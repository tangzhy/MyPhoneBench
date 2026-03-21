#!/usr/bin/env python3
"""Generate final trajectories after E2E execution.

Produces two output files:
1. raw_steps.json  - Per-step LLM call log (messages + response), screenshots as file paths
2. readable_trajectory.json - Simple step-by-step summary for quick reading
"""

import json
import numpy as np
from pathlib import Path
from PIL import Image


def _save_screenshot(image_array: np.ndarray, path: Path) -> None:
    """Save a numpy image array as PNG file."""
    if image_array is None:
        return
    Image.fromarray(image_array).save(path)


def _save_all_screenshots(agent, output_dir: Path) -> dict:
    """Save all screenshots to disk.  Returns index -> relative path mapping.

    Also saves intermediate screenshots (taken between multi-action steps)
    as step_N_action_M.png.
    """
    screenshots_dir = output_dir / 'screenshots'
    screenshots_dir.mkdir(parents=True, exist_ok=True)

    mapping: dict[int, str] = {}
    for i, screenshot in enumerate(agent.screenshots):
        step_num = i + 1
        rel_path = f'screenshots/step_{step_num}.png'
        _save_screenshot(screenshot, output_dir / rel_path)
        mapping[i] = rel_path

        # Save intermediate screenshots for this step
        if i < len(agent.step_history):
            step_data = agent.step_history[i]
            for j, inter_ss in enumerate(step_data.get('intermediate_screenshots', [])):
                inter_path = f'screenshots/step_{step_num}_action_{j+1}.png'
                _save_screenshot(inter_ss, output_dir / inter_path)

    return mapping


def _replace_base64_with_paths(messages: list, step_num: int,
                                max_image_history: int,
                                total_steps: int) -> list:
    """Replace base64 image URLs in *messages* with file paths.

    After ``_hide_history_images`` the messages contain at most
    ``max_image_history`` image-bearing user messages.  They appear in
    chronological order so we can simply assign step numbers with a counter.

    The images that survive ``_hide_history_images`` are the most recent ones.
    For step N (1-indexed) with max_image_history=H the surviving screenshots
    are: step max(1, N-H+1) … step N.
    """
    # Compute which step indices have surviving screenshots
    first_surviving = max(1, step_num - max_image_history + 1)
    surviving_steps = list(range(first_surviving, step_num + 1))

    img_counter = 0
    new_messages = []
    for msg in messages:
        role = msg.get('role')
        content = msg.get('content')

        if role == 'user' and isinstance(content, list):
            new_content = []
            for item in content:
                if item.get('type') == 'image_url':
                    url = item.get('image_url', {}).get('url', '')
                    if url.startswith('data:image'):
                        # Map this image to the correct step
                        if img_counter < len(surviving_steps):
                            path = f'screenshots/step_{surviving_steps[img_counter]}.png'
                        else:
                            path = 'screenshots/unknown.png'
                        img_counter += 1
                        new_content.append({
                            'type': 'image_url',
                            'image_url': {'url': path},
                        })
                    else:
                        new_content.append(item)
                else:
                    new_content.append(item)
            new_messages.append({'role': role, 'content': new_content})
        else:
            new_messages.append(msg)

    return new_messages


# ── Public API ────────────────────────────────────────────────

def generate_raw_steps(agent, task_def, output_dir=None):
    """Per-step raw LLM call log.

    Each element: full messages (screenshots as file paths) + LLM response.
    """
    output_dir = Path(output_dir) if output_dir else Path('e2e_output')
    _save_all_screenshots(agent, output_dir)

    total_steps = len(agent.raw_trajectory)
    max_img = getattr(agent, 'max_image_history', 3)

    raw_steps = []
    for step_data in agent.raw_trajectory:
        step_num = step_data.get('step', 0)
        messages = step_data.get('messages', [])
        response = step_data.get('response', {})

        clean_messages = _replace_base64_with_paths(
            messages, step_num, max_img, total_steps,
        )

        entry = {
            'step': step_num,
            'messages': clean_messages,
            'response': response,
        }
        # Include multi-action data when available
        if 'parsed_actions' in step_data:
            entry['parsed_actions'] = step_data['parsed_actions']
        if 'execution_results' in step_data:
            entry['execution_results'] = step_data['execution_results']
        if 'thinking' in step_data:
            entry['thinking'] = step_data['thinking']
        if step_data.get('intermediate_screenshots_count'):
            entry['intermediate_screenshots_count'] = step_data['intermediate_screenshots_count']
        raw_steps.append(entry)

    return raw_steps


def generate_readable_trajectory(agent, task_def, output_dir=None):
    """Simple human-readable trajectory: thought + action + screenshot per step."""
    output_dir = Path(output_dir) if output_dir else Path('e2e_output')
    screenshot_map = _save_all_screenshots(agent, output_dir)

    readable = {
        'task_id': task_def.get('task_id'),
        'goal': task_def.get('goal'),
        'total_steps': len(agent.step_history),
        'steps': [],
    }

    for i, step_data in enumerate(agent.step_history):
        step_num = step_data.get('step', i + 1)
        thinking = step_data.get('thinking', '')

        # Support multi-action steps (parsed_actions) with fallback to
        # single-action (parsed_action) for backward compatibility.
        actions_list = step_data.get('parsed_actions')
        if actions_list is None:
            action_dict = step_data.get('parsed_action', {})
            actions_list = [action_dict] if action_dict else []
        # Clean 'thought' key from each action (backward compat)
        actions_clean = [
            {k: v for k, v in ad.items() if k != 'thought'}
            for ad in actions_list
        ]

        results_list = step_data.get('results')
        if results_list is None:
            single_result = step_data.get('result')
            results_list = [single_result] if single_result else []

        readable['steps'].append({
            'step': step_num,
            'thought': thinking,
            'actions': actions_clean,
            'results': results_list,
            'screenshot': screenshot_map.get(i, f'screenshots/step_{step_num}.png'),
        })

    return readable
