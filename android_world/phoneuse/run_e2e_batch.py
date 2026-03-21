#!/usr/bin/env python3
# Copyright 2025 PhoneUse Privacy Protocol Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Batch E2E runner for MyPhoneBench — runs all tasks + cross-session pairs.

For each app, runs 25 independent tasks and 5 cross-session pairs, producing
a summary with task success rate, privacy scores, and personalization metrics.
"""

import json
import os
import sys
import copy
import argparse
import logging
import shutil
import subprocess
import time
from pathlib import Path
from typing import Dict, Any, List, Optional, Tuple

sys.path.insert(0, str(Path(__file__).parent.parent.parent.parent))

from android_world.env import env_launcher
from android_world.env import interface
from android_world.phoneuse.agents.llm_wrapper import OpenAIWrapper
from android_world.phoneuse.agents.phoneuse_agent import PhoneUseAgent
from android_world.phoneuse.tasks.task_verifier import TaskVerifier
from android_world.phoneuse.tasks.privacy_evaluator import PrivacyEvaluator
from android_world.phoneuse.mock_apps import imy_utils
from android_world.phoneuse.privacy.seed_generator import load_imy_data
from android_world.phoneuse.generate_final_trajectories import generate_raw_steps
from android_world.phoneuse.app_registry import (
    APP_REGISTRY,
    get_app_config,
    get_all_packages,
    get_load_fn,
    get_push_fn,
    get_seed_path,
    get_tasks_dir,
    get_imy_seed_path,
    build_app_seed,
)

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
)
logger = logging.getLogger(__name__)


# ── Environment helpers ──────────────────────────────────────


def setup_android_env() -> interface.AsyncEnv:
    """Create and return the Android environment (once per batch)."""
    from android_world.env import android_world_controller
    from android_env import loader
    from android_env.components import config_classes

    adb_path = _resolve_adb_path()
    logger.info(f'Using ADB path: {adb_path}')

    config = config_classes.AndroidEnvConfig(
        task=config_classes.FilesystemTaskConfig(
            path=android_world_controller._write_default_task_proto()
        ),
        simulator=config_classes.EmulatorConfig(
            emulator_launcher=config_classes.EmulatorLauncherConfig(
                emulator_console_port=5554,
                adb_port=5555,
                grpc_port=8554,
            ),
            adb_controller=config_classes.AdbControllerConfig(adb_path=adb_path),
        ),
    )
    android_env_instance = loader.load(config)
    controller = android_world_controller.AndroidWorldController(
        android_env_instance,
        install_a11y_forwarding_app=False,
    )
    env = interface.AsyncAndroidEnv(controller)
    env_launcher.setup_env(env, emulator_setup=False, freeze_datetime=True)
    return env


def _resolve_adb_path() -> str:
    """Resolve the adb executable path across platforms."""
    import platform
    path = shutil.which('adb')
    if path:
        return path
    if platform.system() == 'Windows':
        win_default = os.path.join(
            os.environ.get('LOCALAPPDATA', ''),
            'Android', 'Sdk', 'platform-tools', 'adb.exe',
        )
        if os.path.isfile(win_default):
            return win_default
    return '/opt/homebrew/bin/adb'


def force_stop_app(package: str = 'com.phoneuse.mzocdoc') -> None:
    """Force-stop an Android app via ADB."""
    adb_path = _resolve_adb_path()
    try:
        subprocess.run(
            [adb_path, '-s', 'emulator-5554', 'shell', 'am', 'force-stop', package],
            check=False,
            timeout=5,
        )
    except Exception:
        pass


def check_emulator_health() -> bool:
    """Return True if the emulator responds to adb shell."""
    adb_path = _resolve_adb_path()
    try:
        result = subprocess.run(
            [adb_path, '-s', 'emulator-5554', 'shell', 'echo', 'ok'],
            capture_output=True, text=True, timeout=10,
        )
        return result.returncode == 0 and 'ok' in result.stdout
    except Exception:
        return False


def wait_for_emulator(timeout: int = 120) -> bool:
    """Wait until the emulator is reachable, up to *timeout* seconds."""
    adb_path = _resolve_adb_path()
    logger.info(f'Waiting for emulator (up to {timeout}s)...')
    deadline = time.time() + timeout
    while time.time() < deadline:
        if check_emulator_health():
            logger.info('Emulator is healthy.')
            return True
        # Ensure adb server is running
        try:
            subprocess.run([adb_path, 'start-server'],
                           capture_output=True, timeout=10)
        except Exception:
            pass
        time.sleep(5)
    logger.error('Emulator did not become reachable within timeout.')
    return False


def load_existing_report(output_dir: Path, task_id: str) -> Optional[Dict[str, Any]]:
    """Load a previously saved report.json for a task, or return None."""
    report_path = output_dir / 'layer1' / task_id / 'report.json'
    if report_path.exists():
        try:
            with open(report_path, 'r', encoding='utf-8') as f:
                return json.load(f)
        except Exception as e:
            logger.warning(f'Failed to load existing report for {task_id}: {e}')
    return None


# ── Seed data helpers ────────────────────────────────────────


def build_imy_seed(
    base_imy: Dict[str, Any],
    task_override: Optional[Dict[str, Any]],
) -> Dict[str, Any]:
    """Merge base iMy profile with per-task overrides.

    Override supports:
      - extra_profile_items: list of items to append
      - privacy_mode: override default privacy mode
    """
    merged = copy.deepcopy(base_imy)
    if task_override is None:
        return merged

    if 'privacy_mode' in task_override:
        merged['privacy_mode'] = task_override['privacy_mode']

    for item in task_override.get('extra_profile_items', []):
        merged['profile_items'].append(item)

    return merged


def build_mock_app_seed(
    app_key: str,
    task_override: Optional[Dict[str, Any]],
) -> Dict[str, Any]:
    """Build mutable seed for any mock app using the registry.

    For mzocdoc, also includes base doctors from the seed JSON.
    For other apps, only the mutable key (bookings/reservations/etc.) is set.
    """
    return build_app_seed(app_key, None, task_override)


# ── Single-task runner ───────────────────────────────────────


def reset_environment(
    env: interface.AsyncEnv,
    app_key: str,
    imy_seed: Dict[str, Any],
    app_seed: Dict[str, Any],
    app_json_path: Optional[str] = None,
) -> None:
    """Fully reset environment for a new task (registry-driven).

    Args:
        env: Android environment
        app_key: The mock app key (e.g. 'mzocdoc')
        imy_seed: iMy profile seed data
        app_seed: Mutable seed data for the mock app (appointments/bookings/etc.)
        app_json_path: Path to the seed JSON pushed to device (catalog data)
    """
    logger.info('  Resetting iMy profile...')
    load_imy_data(imy_seed, env.controller)

    if app_json_path:
        logger.info(f'  Pushing {app_key} catalog data...')
        push_fn = get_push_fn(app_key)
        push_fn(app_json_path)

    logger.info(f'  Loading {app_key} mutable state...')
    load_fn = get_load_fn(app_key)
    load_fn(app_seed, env.controller)


def run_single_task(
    env: interface.AsyncEnv,
    task_def: Dict[str, Any],
    app_key: str,
    imy_seed: Dict[str, Any],
    app_seed: Dict[str, Any],
    app_json_path: Optional[str],
    output_dir: Path,
    model_name: str = 'claude-opus-4-6',
    max_steps_override: Optional[int] = None,
    enable_personalization: bool = False,
) -> Dict[str, Any]:
    """Run a single task end-to-end with full environment reset.

    Returns:
        Result dict with resolved, privacy_score, etc.
    """
    task_id = task_def.get('task_id', 'unknown')
    task_dir = output_dir / task_id
    task_dir.mkdir(parents=True, exist_ok=True)

    # 1) Force-stop all apps BEFORE clearing DB to prevent autosave race
    cfg = get_app_config(app_key)
    force_stop_app(cfg['package'])
    force_stop_app('com.phoneuse.imy')
    time.sleep(0.5)

    # 2) Reset environment (clear DB tables with apps stopped)
    reset_environment(env, app_key, imy_seed, app_seed, app_json_path)

    # 3) Create fresh agent
    llm_wrapper = OpenAIWrapper(
        model_name=model_name,
        max_retry=5,
        temperature=0.0,
        max_tokens=4096,
    )
    agent = PhoneUseAgent(
        env=env,
        llm_wrapper=llm_wrapper,
        user_profile=imy_seed,
        user_decisions=task_def.get('user_decisions', {}),
        max_steps=max_steps_override or task_def.get('max_steps', 30),
        enable_personalization=enable_personalization,
    )

    # 4) Reset to home screen
    agent.reset(go_home=True)
    time.sleep(1)

    # 4) Run agent
    max_steps = max_steps_override or task_def.get('max_steps', 30)
    step_count = 0
    step_results = []

    while step_count < max_steps:
        step_count += 1
        logger.info(f'  Step {step_count}/{max_steps}')
        result = agent.step(task_def['goal'])
        step_results.append(result.data)
        if result.done:
            logger.info(f'  Agent finished at step {step_count}')
            break

    # 5) Verify
    verifier = TaskVerifier(task_def, env)
    verification = verifier.verify()

    # 6) Privacy evaluation
    resolved = verification.get('resolved', False)
    evaluator = PrivacyEvaluator(agent.access_log, task_def)
    privacy = evaluator.evaluate(env=env, resolved=resolved)

    # 7) Generate trajectory
    try:
        raw_steps = generate_raw_steps(agent, task_def, str(task_dir))
    except Exception as e:
        logger.warning(f'  Trajectory generation failed: {e}')
        raw_steps = []

    # 8) Save per-task report
    report = {
        'task_id': task_id,
        'goal': task_def.get('goal'),
        'difficulty': task_def.get('difficulty', 'unknown'),
        'step_count': step_count,
        'resolved': verification.get('resolved', False),
        'verification': verification,
        'privacy_score': privacy.get('privacy_score'),
        'privacy': privacy,
        'access_log': agent.access_log.to_dict_list(),
    }

    with open(task_dir / 'report.json', 'w', encoding='utf-8') as f:
        json.dump(report, f, indent=2, default=str, ensure_ascii=False)

    if raw_steps:
        with open(task_dir / 'raw_steps.json', 'w', encoding='utf-8') as f:
            json.dump(raw_steps, f, indent=2, default=str, ensure_ascii=False)

    return report


# ── Cross-session pair runner ────────────────────────────────


def run_cross_session_pair(
    env: interface.AsyncEnv,
    pair_def: Dict[str, Any],
    app_key: str,
    base_imy: Dict[str, Any],
    app_json_path: Optional[str],
    output_dir: Path,
    model_name: str = 'claude-opus-4-6',
    max_steps_override: Optional[int] = None,
) -> Dict[str, Any]:
    """Run a cross-session pair (Task A → capture DB → Task B).

    Returns pair result dict.
    """
    pair_id = pair_def.get('pair_id', 'unknown_pair')
    task_a = pair_def['task_a']
    task_b = pair_def['task_b']
    expected_key = pair_def.get('expected_save_key', '')

    pair_dir = output_dir / pair_id
    pair_dir.mkdir(parents=True, exist_ok=True)

    # ── Task A ──
    logger.info(f'  [Pair {pair_id}] Running Task A: {task_a.get("task_id")}')
    imy_a = build_imy_seed(base_imy, task_a.get('seed_data', {}).get('imy_override'))
    app_seed_a = build_mock_app_seed(
        app_key, task_a.get('seed_data', {}).get(f'{app_key}_override')
    )
    report_a = run_single_task(
        env, task_a, app_key, imy_a, app_seed_a, app_json_path,
        pair_dir, model_name,
        max_steps_override=max_steps_override,
        enable_personalization=True,
    )

    # ── Capture iMy DB snapshot ──
    logger.info(f'  [Pair {pair_id}] Reading iMy DB snapshot after Task A...')
    try:
        db_items = imy_utils.read_all_profile_items(env)
    except Exception as e:
        logger.warning(f'  Failed to read iMy DB: {e}')
        db_items = []

    # Build Task B iMy seed from actual DB state
    imy_b_from_db = copy.deepcopy(base_imy)
    imy_b_from_db['profile_items'] = [
        {'key': item['key'], 'value': item['value'], 'level': item['level']}
        for item in db_items
    ]

    # Apply Task B overrides on top
    task_b_imy_override = task_b.get('seed_data', {}).get('imy_override')
    if task_b_imy_override:
        if 'privacy_mode' in task_b_imy_override:
            imy_b_from_db['privacy_mode'] = task_b_imy_override['privacy_mode']
        for item in task_b_imy_override.get('extra_profile_items', []):
            imy_b_from_db['profile_items'].append(item)

    # ── Task B (only reset app DB, keep iMy from Task A) ──
    logger.info(f'  [Pair {pair_id}] Running Task B: {task_b.get("task_id")}')
    app_seed_b = build_mock_app_seed(
        app_key, task_b.get('seed_data', {}).get(f'{app_key}_override')
    )

    # Only reset app DB (not iMy — preserve Task A writes)
    load_fn = get_load_fn(app_key)
    load_fn(app_seed_b, env.controller)

    report_b = run_single_task(
        env, task_b, app_key, imy_b_from_db, app_seed_b, app_json_path,
        pair_dir, model_name,
        max_steps_override=max_steps_override,
        enable_personalization=True,
    )

    # ── Evaluate pair ──
    a_save_score = _evaluate_save(report_a.get('access_log', []), expected_key)
    b_use_score = 1.0 if report_b.get('resolved', False) else 0.0
    pair_score = a_save_score * b_use_score

    pair_result = {
        'pair_id': pair_id,
        'expected_save_key': expected_key,
        'task_a': {
            'task_id': task_a.get('task_id'),
            'resolved': report_a.get('resolved', False),
            'save_score': a_save_score,
        },
        'task_b': {
            'task_id': task_b.get('task_id'),
            'resolved': report_b.get('resolved', False),
            'use_score': b_use_score,
        },
        'db_snapshot_items': len(db_items),
        'pair_score': pair_score,
    }

    with open(pair_dir / 'pair_result.json', 'w', encoding='utf-8') as f:
        json.dump(pair_result, f, indent=2, default=str, ensure_ascii=False)

    return pair_result


def _stem_simple(word: str) -> str:
    """Minimal stemmer for key token matching (handles common plurals)."""
    if word.endswith('ies') and len(word) > 4:
        return word[:-3] + 'y'   # allergies -> allergy
    if word.endswith('es') and len(word) > 4:
        return word[:-2]         # matches -> match
    if word.endswith('s') and len(word) > 3:
        return word[:-1]         # types -> type
    return word


def _token_close(a: str, b: str) -> bool:
    """Check if two tokens are a close match (exact, prefix, or same stem)."""
    if a == b:
        return True
    if a.startswith(b) or b.startswith(a):
        return True
    if _stem_simple(a) == _stem_simple(b):
        return True
    return False


def _fuzzy_key_match(expected_key: str, actual_key: str) -> bool:
    """Check if actual_key is a fuzzy match for expected_key.

    Tokenizes both keys by '_', then checks that every significant token
    (len >= 3) in expected_key has a close match in actual_key's tokens.
    Handles extra words, reordering, prefix matching, and simple plurals.
    """
    if expected_key == actual_key:
        return True
    expected_tokens = set(expected_key.lower().split('_'))
    actual_tokens = set(actual_key.lower().split('_'))
    for et in expected_tokens:
        if len(et) < 3:
            continue
        if not any(_token_close(et, at) for at in actual_tokens):
            return False
    return True


def _evaluate_save(access_log: List[Dict[str, Any]], expected_key: str) -> float:
    """Evaluate if Task A successfully saved the expected key.

    Returns 1.0 if write_executed/write_auto for a fuzzy-matching key found, else 0.0.
    """
    for entry in access_log:
        action = entry.get('action', '')
        item_key = entry.get('item_key', '')
        if action in ('write_executed', 'write_auto') and _fuzzy_key_match(expected_key, item_key):
            return 1.0
    return 0.0


# ── Summary report ───────────────────────────────────────────


def generate_summary(
    layer1_results: List[Dict[str, Any]],
    layer2_results: List[Dict[str, Any]],
) -> Dict[str, Any]:
    """Generate aggregate summary report."""
    # Task success rate
    total_l1 = len(layer1_results)
    resolved_l1 = sum(1 for r in layer1_results if r.get('resolved', False))

    def _infer_eval_status(r: Dict[str, Any]) -> Optional[str]:
        """Infer eval_status from report, handling legacy reports without it."""
        status = r.get('privacy', {}).get('eval_status')
        if status is not None:
            return status
        edims = r.get('privacy', {}).get('evaluable_dims', 0)
        if edims >= 3:
            return 'full'
        if edims > 0:
            return 'access_only'
        if r.get('privacy_score') is not None:
            return 'full'
        return 'unevaluable'

    # Privacy score (average over full-eval tasks only)
    full_privacy_scores = [
        r.get('privacy_score') for r in layer1_results
        if _infer_eval_status(r) == 'full'
        and r.get('privacy_score') is not None
    ]
    avg_privacy = sum(full_privacy_scores) / len(full_privacy_scores) if full_privacy_scores else None

    # Privacy score (resolved tasks only — avoids survivorship bias)
    resolved_privacy_scores = [
        r.get('privacy_score') for r in layer1_results
        if r.get('resolved', False)
        and _infer_eval_status(r) == 'full'
        and r.get('privacy_score') is not None
    ]
    avg_privacy_resolved = (
        sum(resolved_privacy_scores) / len(resolved_privacy_scores)
        if resolved_privacy_scores else None
    )

    # Access-layer score (average over access_only + full tasks)
    access_layer_scores = [
        r.get('privacy', {}).get('access_layer_score')
        for r in layer1_results
        if _infer_eval_status(r) in ('full', 'access_only')
        and r.get('privacy', {}).get('access_layer_score') is not None
    ]
    avg_access_layer = (
        sum(access_layer_scores) / len(access_layer_scores)
        if access_layer_scores else None
    )

    # Privacy dimension averages (skip None values per dimension)
    dim_names = [
        'over_permissioning', 'trap_resistance', 'form_minimization',
    ]
    dim_sums: Dict[str, float] = {d: 0.0 for d in dim_names}
    dim_counts: Dict[str, int] = {d: 0 for d in dim_names}
    for r in layer1_results:
        dims = r.get('privacy', {}).get('dimensions', {})
        for d in dim_names:
            val = dims.get(d)
            if val is not None:
                dim_sums[d] += val
                dim_counts[d] += 1
    avg_dimensions = {
        d: round(dim_sums[d] / dim_counts[d], 4) if dim_counts[d] else None
        for d in dim_names
    }

    # Count eval statuses
    eval_statuses = [_infer_eval_status(r) for r in layer1_results]
    full_eval_count = sum(1 for s in eval_statuses if s == 'full')
    access_only_count = sum(1 for s in eval_statuses if s == 'access_only')
    unevaluable_count = sum(1 for s in eval_statuses if s == 'unevaluable')

    # Per-difficulty stats
    difficulty_stats: Dict[str, Dict[str, int]] = {}
    for r in layer1_results:
        diff = r.get('difficulty', 'unknown')
        if diff not in difficulty_stats:
            difficulty_stats[diff] = {'total': 0, 'resolved': 0}
        difficulty_stats[diff]['total'] += 1
        if r.get('resolved', False):
            difficulty_stats[diff]['resolved'] += 1

    # Proactive save rate (tasks requiring save)
    save_tasks = [r for r in layer1_results if _is_save_task(r)]
    save_successes = sum(1 for r in save_tasks if _has_write(r.get('access_log', [])))
    proactive_save_rate = save_successes / len(save_tasks) if save_tasks else 0.0

    # Preference use rate (tasks with iMy overrides)
    pref_tasks = [r for r in layer1_results if _is_preference_task(r)]
    pref_successes = sum(1 for r in pref_tasks if r.get('resolved', False))
    preference_use_rate = pref_successes / len(pref_tasks) if pref_tasks else 0.0

    # Full loop rate (layer 2)
    pair_scores = [r.get('pair_score', 0.0) for r in layer2_results]
    full_loop_rate = (
        sum(1 for s in pair_scores if s > 0) / len(pair_scores)
        if pair_scores else 0.0
    )

    # Personalization score
    personalization_score = full_loop_rate

    return {
        'task_success_rate': resolved_l1 / total_l1 if total_l1 else 0.0,
        'total_tasks': total_l1,
        'resolved_tasks': resolved_l1,
        'avg_privacy_score': round(avg_privacy, 4) if avg_privacy is not None else None,
        'avg_privacy_score_resolved': round(avg_privacy_resolved, 4) if avg_privacy_resolved is not None else None,
        'privacy_scored_tasks': len(full_privacy_scores),
        'privacy_scored_resolved_tasks': len(resolved_privacy_scores),
        'avg_access_layer_score': avg_access_layer,
        'avg_privacy_dimensions': avg_dimensions,
        'privacy_full_eval_tasks': full_eval_count,
        'privacy_access_only_tasks': access_only_count,
        'privacy_unevaluable_tasks': unevaluable_count,
        'difficulty_stats': difficulty_stats,
        'personalization': {
            'proactive_save_rate': proactive_save_rate,
            'preference_use_rate': preference_use_rate,
            'full_loop_rate': full_loop_rate,
            'personalization_score': personalization_score,
        },
        'layer2_pairs': len(layer2_results),
        'per_task': [
            {
                'task_id': r.get('task_id'),
                'difficulty': r.get('difficulty'),
                'resolved': r.get('resolved'),
                'privacy_score': r.get('privacy_score'),
                'access_layer_score': r.get('privacy', {}).get('access_layer_score'),
                'eval_status': _infer_eval_status(r),
                'interaction_depth': r.get('privacy', {}).get('interaction_depth', 0),
                'privacy_dimensions': r.get('privacy', {}).get('dimensions', {}),
                'steps': r.get('step_count'),
            }
            for r in layer1_results
        ],
    }


def _is_save_task(result: Dict[str, Any]) -> bool:
    """Check if task is one that should trigger proactive save."""
    task_id = result.get('task_id', '')
    return 'save' in task_id or 'write' in task_id


def _is_preference_task(result: Dict[str, Any]) -> bool:
    """Check if task is one that tests preference use."""
    task_id = result.get('task_id', '')
    return 'pref' in task_id or 'preference' in task_id


def _has_write(access_log: List[Dict[str, Any]]) -> bool:
    """Check if access log contains a successful write."""
    return any(
        e.get('action') in ('write_executed', 'write_auto')
        for e in access_log
    )


# ── Main ─────────────────────────────────────────────────────


def main():
    parser = argparse.ArgumentParser(description='Batch E2E runner for MyPhoneBench')
    parser.add_argument(
        '--app',
        type=str,
        required=True,
        choices=list(APP_REGISTRY.keys()),
        help='Which mock app to benchmark',
    )
    parser.add_argument(
        '--tasks-dir',
        type=str,
        default=None,
        help='Directory containing task JSON files (auto-detected from --app if omitted)',
    )
    parser.add_argument(
        '--pairs-dir',
        type=str,
        default=None,
        help='Directory containing cross-session pair JSON files (auto-detected from --app if omitted)',
    )
    parser.add_argument(
        '--app-seed',
        type=str,
        default=None,
        help='Path to app seed JSON (catalog data pushed to device). '
             'Auto-detected from data/apps/<app>/seed.json if omitted.',
    )
    parser.add_argument(
        '--imy-base',
        type=str,
        default=None,
        help='Path to base iMy profile JSON (auto-detected if omitted)',
    )
    parser.add_argument(
        '--output-dir',
        type=str,
        default=None,
        help='Output directory (default: batch_results/<app>/<model>)',
    )
    parser.add_argument(
        '--model',
        type=str,
        default='claude-opus-4-6',
        help='LLM model name',
    )
    parser.add_argument(
        '--max-steps',
        type=int,
        default=None,
        help='Override max_steps from task JSON (e.g. 100)',
    )
    parser.add_argument(
        '--start-from',
        type=str,
        default=None,
        help='Resume from this task prefix (e.g. "007"). '
             'Tasks before it are loaded from existing reports in output-dir.',
    )

    parser.add_argument(
        '--layer',
        type=str,
        choices=['1', '2', 'all'],
        default='all',
        help='Which layer to run: 1 (tasks only), 2 (pairs only), all (default)',
    )

    args = parser.parse_args()
    app_key = args.app

    # Auto-detect paths from registry
    if args.tasks_dir is None:
        td = get_tasks_dir(app_key)
        if td is None:
            logger.error(f'No tasks directory found for {app_key}. Use --tasks-dir.')
            sys.exit(1)
        args.tasks_dir = str(td)

    if args.pairs_dir is None:
        args.pairs_dir = str(Path(args.tasks_dir) / 'pairs')

    if args.imy_base is None:
        args.imy_base = str(get_imy_seed_path())

    if args.output_dir is None:
        args.output_dir = f'batch_results/{app_key}/{args.model}'

    output_dir = Path(args.output_dir).resolve()
    output_dir.mkdir(parents=True, exist_ok=True)

    # Load base iMy seed
    logger.info('Loading base iMy seed data...')
    with open(args.imy_base, 'r', encoding='utf-8') as f:
        base_imy = json.load(f)

    # Load app seed (catalog data) if available
    app_json_path: Optional[str] = None
    if args.app_seed:
        seed_src = Path(args.app_seed)
    else:
        seed_src = get_seed_path(app_key)

    if seed_src and seed_src.exists():
        logger.info(f'Loading {app_key} catalog from {seed_src}...')
        app_json_path = str(output_dir / f'_{app_key}_seed.json')
        with open(seed_src, 'r', encoding='utf-8') as f:
            catalog = json.load(f)
        with open(app_json_path, 'w', encoding='utf-8') as f:
            json.dump(catalog, f, ensure_ascii=False)
        logger.info(f'Wrote {app_key} seed to {app_json_path}')
    else:
        logger.info(f'No external seed for {app_key} (data embedded in app)')

    # Setup environment
    logger.info('Setting up Android environment...')
    env = setup_android_env()

    layer1_results: List[Dict[str, Any]] = []
    layer2_results: List[Dict[str, Any]] = []

    start_from = args.start_from

    # ── Layer 1: Independent tasks (25 per app) ──
    if args.layer in ('1', 'all'):
        tasks_dir = Path(args.tasks_dir)
        if tasks_dir.exists():
            task_files = sorted(tasks_dir.glob('*.json'))
            # Exclude pair files
            task_files = [f for f in task_files if not f.name.startswith('pair_')]

            logger.info(f'Found {len(task_files)} task files')

            for i, task_file in enumerate(task_files):
                with open(task_file, 'r', encoding='utf-8') as f:
                    task_def = json.load(f)

                task_id = task_def.get('task_id', task_file.stem)

                # ── Skip / load existing results for tasks before start_from ──
                if start_from and task_file.stem < start_from:
                    existing = load_existing_report(output_dir, task_id)
                    if existing:
                        logger.info(
                            f'  [SKIP] {task_id}: loaded existing report '
                            f'(resolved={existing.get("resolved")})'
                        )
                        layer1_results.append(existing)
                    else:
                        logger.info(f'  [SKIP] {task_id}: no existing report, recording as failed')
                        layer1_results.append({
                            'task_id': task_id,
                            'resolved': False,
                            'difficulty': task_def.get('difficulty', 'unknown'),
                            'privacy_score': 0.0,
                            'step_count': 0,
                        })
                    continue

                logger.info(f'\n{"="*60}')
                logger.info(f'Task {i+1}/{len(task_files)}: {task_id}')
                logger.info(f'{"="*60}')

                # ── Emulator health check before each task ──
                if not check_emulator_health():
                    logger.warning('Emulator unreachable — attempting recovery...')
                    if not wait_for_emulator(timeout=120):
                        logger.error(f'  Task {task_id} skipped: emulator not recovered')
                        layer1_results.append({
                            'task_id': task_id,
                            'resolved': False,
                            'error': 'emulator_unreachable',
                            'difficulty': task_def.get('difficulty', 'unknown'),
                            'privacy_score': 0.0,
                            'step_count': 0,
                        })
                        continue
                    # Rebuild env connection after recovery
                    try:
                        env = setup_android_env()
                    except Exception as e:
                        logger.error(f'  Failed to reconnect env: {e}')
                        layer1_results.append({
                            'task_id': task_id,
                            'resolved': False,
                            'error': f'env_reconnect_failed: {e}',
                            'difficulty': task_def.get('difficulty', 'unknown'),
                            'privacy_score': 0.0,
                            'step_count': 0,
                        })
                        continue

                try:
                    imy_seed = build_imy_seed(
                        base_imy,
                        task_def.get('seed_data', {}).get('imy_override'),
                    )
                    app_seed = build_mock_app_seed(
                        app_key,
                        task_def.get('seed_data', {}).get(f'{app_key}_override'),
                    )

                    result = run_single_task(
                        env, task_def, app_key, imy_seed, app_seed,
                        app_json_path, output_dir / 'layer1',
                        args.model,
                        max_steps_override=args.max_steps,
                    )
                    layer1_results.append(result)
                    eval_st = result.get('privacy', {}).get('eval_status', '?')
                    ps = result.get('privacy_score')
                    als = result.get('privacy', {}).get('access_layer_score')
                    depth = result.get('privacy', {}).get('interaction_depth', 0)
                    if eval_st == 'full' and ps is not None:
                        score_str = f'privacy={ps:.4f}'
                    elif eval_st == 'access_only' and als is not None:
                        score_str = f'access_layer={als:.4f}'
                    else:
                        score_str = 'privacy=N/A'
                    logger.info(
                        f'  Result: resolved={result.get("resolved")} '
                        f'{score_str} eval={eval_st} depth={depth} '
                        f'steps={result.get("step_count")}'
                    )
                except Exception as e:
                    logger.error(f'  Task {task_id} failed: {e}')
                    layer1_results.append({
                        'task_id': task_id,
                        'resolved': False,
                        'error': str(e),
                        'difficulty': task_def.get('difficulty', 'unknown'),
                        'privacy_score': 0.0,
                        'step_count': 0,
                    })
        else:
            logger.warning(f'Tasks directory not found: {tasks_dir}')

    # ── Layer 2: Cross-session pairs (5 per app) ──
    if args.layer in ('2', 'all'):
        pairs_dir = Path(args.pairs_dir)
        if pairs_dir.exists():
            pair_files = sorted(pairs_dir.glob('pair_*.json'))
            logger.info(f'\nFound {len(pair_files)} cross-session pairs')

            for i, pair_file in enumerate(pair_files):
                with open(pair_file, 'r', encoding='utf-8') as f:
                    pair_def = json.load(f)

                pair_id = pair_def.get('pair_id', pair_file.stem)
                logger.info(f'\n{"="*60}')
                logger.info(f'Pair {i+1}/{len(pair_files)}: {pair_id}')
                logger.info(f'{"="*60}')

                try:
                    result = run_cross_session_pair(
                        env, pair_def, app_key, base_imy,
                        app_json_path, output_dir / 'layer2',
                        args.model,
                        max_steps_override=args.max_steps,
                    )
                    layer2_results.append(result)
                    logger.info(
                        f'  Pair result: score={result.get("pair_score", 0):.2f} '
                        f'A_save={result["task_a"]["save_score"]:.1f} '
                        f'B_use={result["task_b"]["use_score"]:.1f}'
                    )
                except Exception as e:
                    logger.error(f'  Pair {pair_id} failed: {e}')
                    layer2_results.append({
                        'pair_id': pair_id,
                        'pair_score': 0.0,
                        'error': str(e),
                    })
        else:
            logger.warning(f'Pairs directory not found: {pairs_dir}')

    # ── Summary ──
    summary = generate_summary(layer1_results, layer2_results)
    with open(output_dir / 'summary.json', 'w', encoding='utf-8') as f:
        json.dump(summary, f, indent=2, default=str, ensure_ascii=False)

    # Print
    print('\n' + '=' * 70)
    print('BATCH EXECUTION SUMMARY')
    print('=' * 70)
    print(f'Task Success Rate:    {summary["task_success_rate"]:.1%} '
          f'({summary["resolved_tasks"]}/{summary["total_tasks"]})')
    avg_ps = summary["avg_privacy_score"]
    if avg_ps is not None:
        print(f'Avg Privacy Score:    {avg_ps:.4f} '
              f'({summary["privacy_full_eval_tasks"]} full-eval tasks)')
    else:
        print(f'Avg Privacy Score:    N/A (no fully-evaluated tasks)')
    avg_als = summary.get("avg_access_layer_score")
    if avg_als is not None:
        print(f'Avg Access Layer:     {avg_als:.4f} '
              f'({summary["privacy_access_only_tasks"]} access-only + '
              f'{summary["privacy_full_eval_tasks"]} full)')
    print(f'Unevaluable tasks:    {summary["privacy_unevaluable_tasks"]}')
    avg_dims = summary.get('avg_privacy_dimensions', {})
    if avg_dims:
        print(f'Privacy Dimensions:')
        for dim_name, dim_val in avg_dims.items():
            if dim_val is not None:
                print(f'  {dim_name:25s}: {dim_val:.4f}')
            else:
                print(f'  {dim_name:25s}: N/A')
    p = summary['personalization']
    print(f'Personalization:      {p["personalization_score"]:.2f}')
    print(f'  - Proactive Save:   {p["proactive_save_rate"]:.1%}')
    print(f'  - Preference Use:   {p["preference_use_rate"]:.1%}')
    print(f'  - Full Loop:        {p["full_loop_rate"]:.1%}')
    print()
    print('Difficulty breakdown:')
    for diff, stats in sorted(summary.get('difficulty_stats', {}).items()):
        rate = stats['resolved'] / stats['total'] if stats['total'] else 0
        print(f'  {diff:10s}: {stats["resolved"]}/{stats["total"]} ({rate:.0%})')
    print('=' * 70)
    print(f'Full report: {output_dir / "summary.json"}')


if __name__ == '__main__':
    main()
