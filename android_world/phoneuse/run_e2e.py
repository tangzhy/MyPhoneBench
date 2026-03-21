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

"""E2E runner for PhoneUse Agent - complete pipeline from seed data to verification."""

import json
import os
import sys
import argparse
import logging
from pathlib import Path
from typing import Dict, Any

# Add project root to path
sys.path.insert(0, str(Path(__file__).parent.parent.parent.parent))

from android_world.env import env_launcher
from android_world.env import interface
from android_world.phoneuse.agents.llm_wrapper import OpenAIWrapper
from android_world.phoneuse.agents.phoneuse_agent import PhoneUseAgent
from android_world.phoneuse.tasks.task_verifier import TaskVerifier
from android_world.phoneuse.tasks.privacy_evaluator import PrivacyEvaluator
from android_world.phoneuse.privacy.seed_generator import load_imy_data
from android_world.phoneuse.generate_final_trajectories import generate_raw_steps
from android_world.phoneuse.app_registry import (
    APP_REGISTRY,
    get_app_config,
    get_load_fn,
    get_push_fn,
    get_seed_path,
    get_imy_seed_path,
    build_app_seed,
)


logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


def load_seed_data(imy_path: str, app_key: str, app_seed_path: str = None) -> tuple[Dict[str, Any], Dict[str, Any]]:
    """Load seed data from JSON files.

    Args:
        imy_path: Path to iMy seed JSON
        app_key: The mock app key (e.g. 'mzocdoc')
        app_seed_path: Path to app seed JSON (optional)

    Returns:
        Tuple of (imy_data, app_data)
    """
    with open(imy_path, 'r', encoding='utf-8') as f:
        imy_data = json.load(f)

    app_data = {}
    if app_seed_path:
        with open(app_seed_path, 'r', encoding='utf-8') as f:
            app_data = json.load(f)

    return imy_data, app_data


def load_task_definition(task_path: str) -> Dict[str, Any]:
    """Load task definition from JSON.
    
    Args:
        task_path: Path to task JSON
        
    Returns:
        Task definition dict
    """
    with open(task_path, 'r', encoding='utf-8') as f:
        return json.load(f)


def setup_environment(imy_data: Dict[str, Any], app_key: str, app_data: Dict[str, Any]) -> interface.AsyncEnv:
    """Setup Android environment and load seed data.

    Args:
        imy_data: iMy seed data
        app_key: The mock app key (e.g. 'mzocdoc')
        app_data: App-specific seed data (may be empty for apps with embedded data)

    Returns:
        Configured Android environment
    """
    logger.info('Setting up Android environment...')

    import shutil
    import platform as _plat
    adb_path = shutil.which('adb')
    if not adb_path and _plat.system() == 'Windows':
        _win = os.path.join(
            os.environ.get('LOCALAPPDATA', ''),
            'Android', 'Sdk', 'platform-tools', 'adb.exe',
        )
        if os.path.isfile(_win):
            adb_path = _win
    if not adb_path:
        adb_path = '/opt/homebrew/bin/adb'
    logger.info(f'Using ADB path: {adb_path}')

    # Build the environment manually to skip re-downloading the accessibility
    # forwarder APK (which hangs if storage.googleapis.com is unreachable).
    # The APK is assumed to be already installed on the emulator.
    from android_world.env import android_world_controller
    from android_env import loader
    from android_env.components import config_classes

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
        install_a11y_forwarding_app=False,  # APK already installed
    )
    env = interface.AsyncAndroidEnv(controller)
    env_launcher.setup_env(env, emulator_setup=False, freeze_datetime=True)

    # Load seed data (adb_utils functions need the controller, not AsyncEnv)
    logger.info('Loading iMy seed data...')
    load_imy_data(imy_data, env.controller)

    # Push app seed JSON to device so the app loads it instead of hardcoded defaults
    app_seed_json = get_seed_path(app_key)
    if app_seed_json and app_seed_json.exists():
        push_fn = get_push_fn(app_key)
        logger.info(f'Pushing {app_key} seed JSON to device: {app_seed_json}')
        push_fn(str(app_seed_json))

    # Load app-specific mutable data (reservations, appointments, etc.) via registry
    load_fn = get_load_fn(app_key)
    if load_fn and app_data:
        logger.info(f'Loading {app_key} mutable data...')
        load_fn(app_data, env.controller)
    else:
        logger.info(f'No mutable seed data for {app_key}')

    logger.info('Environment setup complete')
    return env


def run_agent(
    agent: PhoneUseAgent,
    goal: str,
    max_steps: int = 50,
) -> Dict[str, Any]:
    """Run agent until task completion or max steps.
    
    Args:
        agent: PhoneUse agent instance
        goal: Task goal
        max_steps: Maximum steps
        
    Returns:
        Execution result dict
    """
    logger.info(f'Starting agent execution with goal: {goal}')
    
    step_count = 0
    screenshots = []
    step_results = []
    
    while step_count < max_steps:
        step_count += 1
        logger.info(f'Step {step_count}/{max_steps}')
        
        result = agent.step(goal)
        step_results.append(result.data)
        
        # Save screenshot if available
        if hasattr(agent, 'screenshots') and agent.screenshots:
            screenshots.append(agent.screenshots[-1].copy())
        
        if result.done:
            logger.info(f'Agent finished at step {step_count}')
            break
    
    return {
        'step_count': step_count,
        'screenshots': screenshots,
        'step_results': step_results,
        'access_log': agent.access_log.to_dict_list(),
        'final_status': step_results[-1].get('status') if step_results else 'unknown',
    }


def main():
    """Main E2E execution pipeline."""
    parser = argparse.ArgumentParser(description='Run E2E PhoneUse Agent test')
    parser.add_argument(
        '--app',
        type=str,
        required=True,
        choices=list(APP_REGISTRY.keys()),
        help='Mock app to run (e.g. mzocdoc, mcvspharmacy)'
    )
    parser.add_argument(
        '--imy-seed',
        type=str,
        default=None,
        help='Path to iMy seed JSON (auto-detected from registry if omitted)'
    )
    parser.add_argument(
        '--app-seed',
        type=str,
        default=None,
        help='Path to app seed JSON (auto-detected from registry if omitted)'
    )
    parser.add_argument(
        '--task',
        type=str,
        required=True,
        help='Path to task definition JSON'
    )
    parser.add_argument(
        '--max-steps',
        type=int,
        default=30,
        help='Maximum agent steps (MobileWorld default: 15, we use 30 for complex tasks)'
    )
    parser.add_argument(
        '--output-dir',
        type=str,
        default='./e2e_output',
        help='Output directory for results'
    )
    parser.add_argument(
        '--model',
        type=str,
        default='claude-opus-4-6',
        help='Model name (e.g. claude-opus-4-6, gemini-3-pro-preview, gpt-5.3-codex)'
    )
    
    args = parser.parse_args()
    
    # === Step 0: Clean up output directory ===
    import shutil as shutil_mod
    output_dir = args.output_dir
    if os.path.exists(output_dir):
        logger.info(f'Cleaning up output directory: {output_dir}')
        shutil_mod.rmtree(output_dir)
    os.makedirs(output_dir, exist_ok=True)
    logger.info(f'Output directory ready: {output_dir}')
    
    # Resolve seed paths from registry if not provided
    app_key = args.app
    imy_seed_path = args.imy_seed or get_imy_seed_path()
    app_seed_path = args.app_seed or get_seed_path(app_key)

    # Load data
    logger.info(f'Loading seed data for {app_key}...')
    imy_data, app_data = load_seed_data(imy_seed_path, app_key, app_seed_path)
    task_def = load_task_definition(args.task)

    # Apply imy_override from task definition (e.g. extra_profile_items)
    import copy
    imy_override = task_def.get('seed_data', {}).get('imy_override')
    if imy_override:
        imy_data = copy.deepcopy(imy_data)
        if 'privacy_mode' in imy_override:
            imy_data['privacy_mode'] = imy_override['privacy_mode']
        for item in imy_override.get('extra_profile_items', []):
            imy_data['profile_items'].append(item)
        logger.info(f'Applied imy_override: {list(imy_override.keys())}')

    # Resolve max_steps: task JSON overrides CLI default, but explicit CLI wins
    max_steps = task_def.get('max_steps', args.max_steps)
    logger.info(f'Using max_steps={max_steps} (task JSON: {task_def.get("max_steps")}, CLI: {args.max_steps})')

    # Setup environment
    env = setup_environment(imy_data, app_key, app_data)

    # Create agent
    logger.info('Creating PhoneUse agent...')
    llm_wrapper = OpenAIWrapper(
        model_name=args.model,
        max_retry=3,
        temperature=0.0,
        max_tokens=4096,
    )

    agent = PhoneUseAgent(
        env=env,
        llm_wrapper=llm_wrapper,
        user_profile=imy_data,
        user_decisions=task_def.get('user_decisions', {}),
        max_steps=max_steps,
    )

    # === Reset environment ===
    logger.info('Resetting to home screen for clean start...')
    agent.reset(go_home=True)

    # Force stop target apps to ensure they start fresh
    try:
        import subprocess
        _adb = shutil_mod.which('adb') or '/opt/homebrew/bin/adb'
        from android_world.phoneuse.app_registry import get_all_packages
        for pkg in get_all_packages():
            subprocess.run(
                [_adb, '-s', 'emulator-5554', 'shell', 'am', 'force-stop', pkg],
                check=False, timeout=5,
            )
        logger.info('Target apps force-stopped for clean start')
    except Exception as e:
        logger.warning(f'Could not force-stop apps: {e}')

    # === Run agent ===
    execution_result = run_agent(agent, task_def['goal'], max_steps)
    
    # === Verify task ===
    logger.info('Verifying task completion...')
    verifier = TaskVerifier(task_def, env)
    verification_result = verifier.verify()
    
    # === Evaluate privacy ===
    logger.info('Evaluating privacy behavior...')
    resolved = verification_result.get('resolved', False)
    privacy_evaluator = PrivacyEvaluator(agent.access_log, task_def)
    privacy_result = privacy_evaluator.evaluate(env=env, resolved=resolved)
    
    # === Generate trajectory ===
    logger.info('Generating raw steps trajectory...')
    raw_steps = generate_raw_steps(agent, task_def, output_dir)
    
    # === Generate report ===
    report = {
        'task_id': task_def.get('task_id'),
        'goal': task_def.get('goal'),
        'execution': {
            'step_count': execution_result['step_count'],
            'final_status': execution_result.get('final_status', 'unknown'),
            'access_log': execution_result.get('access_log', []),
        },
        'verification': verification_result,
        'privacy': privacy_result,
    }
    
    # === Save outputs ===
    # Save report
    report_path = os.path.join(output_dir, 'e2e_report.json')
    with open(report_path, 'w', encoding='utf-8') as f:
        json.dump(report, f, indent=2, default=str, ensure_ascii=False)
    logger.info(f'Report saved to {report_path}')
    
    # Save raw steps (per-step LLM call log)
    raw_steps_path = os.path.join(output_dir, 'raw_steps.json')
    with open(raw_steps_path, 'w', encoding='utf-8') as f:
        json.dump(raw_steps, f, indent=2, default=str, ensure_ascii=False)
    logger.info(f'Raw steps saved to {raw_steps_path}')
    
    # === Print summary ===
    print('\n' + '='*60)
    print('E2E EXECUTION SUMMARY')
    print('='*60)
    print(f'Task: {task_def.get("task_id")}')
    print(f'Steps executed: {execution_result["step_count"]}')
    print(f'Task resolved: {verification_result.get("resolved", False)}')
    ps = privacy_result.get('privacy_score')
    als = privacy_result.get('access_layer_score')
    eval_st = privacy_result.get('eval_status', 'unknown')
    depth = privacy_result.get('interaction_depth', 0)
    if ps is not None:
        print(f'Privacy score: {ps:.4f} (full eval)')
    elif als is not None:
        print(f'Privacy score: N/A (access_only), Access-layer: {als:.4f}')
    else:
        print(f'Privacy score: N/A (unevaluable)')
    print(f'Eval status: {eval_st}, Interaction depth: {depth}')
    dims = privacy_result.get('dimensions', {})
    if dims:
        print(f'Privacy dimensions:')
        for dim_name, dim_val in dims.items():
            if dim_val is not None:
                print(f'  {dim_name:25s}: {dim_val:.4f}')
            else:
                print(f'  {dim_name:25s}: N/A')
    print(f'Over-permissioning violations: {len(privacy_result.get("over_permissioning", []))}')
    print(f'SQL trap violations: {len(privacy_result.get("sql_trap_violations", []))}')
    print(f'Output files:')
    print(f'  - {raw_steps_path}')
    print(f'  - {report_path}')
    print('='*60)
    
    return report


if __name__ == '__main__':
    main()

