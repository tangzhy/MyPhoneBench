#!/usr/bin/env python3
"""Simplified E2E runner that calls real LLM API but doesn't require full Android environment."""

import json
import os
import sys
import logging
import numpy as np
from pathlib import Path
from typing import Dict, Any

# Add project root to path
sys.path.insert(0, str(Path(__file__).parent.parent.parent.parent))

from android_world.phoneuse.agents.llm_wrapper import OpenAIWrapper
from android_world.phoneuse.agents.phoneuse_prompt import build_system_prompt
from android_world.phoneuse.tasks.privacy_evaluator import PrivacyEvaluator
from android_world.phoneuse.privacy.access_log import AccessLog
from android_world.phoneuse.privacy.request_permission_handler import RequestPermissionHandler
from android_world.phoneuse.privacy.save_profile_handler import SaveProfileHandler
from android_world.phoneuse.privacy.ask_user_handler import AskUserHandler
from android_world.phoneuse.generate_final_trajectories import (
    generate_readable_trajectory,
    generate_raw_trajectory,
)

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class SimplePhoneUseAgent:
    """Simplified agent that calls real LLM API but uses mock execution."""
    
    def __init__(self, llm_wrapper, user_profile, user_decisions, max_steps=20):
        self.llm_wrapper = llm_wrapper
        self.user_profile = user_profile
        self.user_decisions = user_decisions
        self.max_steps = max_steps
        
        # Privacy components
        self.access_log = AccessLog()
        privacy_mode = user_profile.get('privacy_mode', 'full_control')
        
        self.request_permission_handler = RequestPermissionHandler(
            self.access_log,
            user_profile,
            user_decisions,
        )
        self.save_profile_handler = SaveProfileHandler(
            self.access_log,
            privacy_mode,
        )
        self.ask_user_handler = AskUserHandler(
            user_decisions.get('ask_user', {}),
        )
        
        # State tracking
        self.step_history: list[Dict[str, Any]] = []
        self.screenshots: list[np.ndarray] = []
        self.raw_trajectory: list[Dict[str, Any]] = []
        self.readable_trajectory: list[Dict[str, Any]] = []
        self.system_prompt = build_system_prompt(user_profile, privacy_mode)
    
    def step(self, goal: str):
        """Execute one step."""
        if len(self.step_history) >= self.max_steps:
            return {'done': True, 'status': 'max_steps_reached'}
        
        # Create mock screenshot
        screenshot = np.zeros((2400, 1080, 3), dtype=np.uint8)
        # Add some visual content to make it more realistic
        screenshot[100:200, 100:200] = [255, 0, 0]  # Red square
        self.screenshots.append(screenshot.copy())
        
        # Build prompt
        from android_world.phoneuse.agents.phoneuse_prompt import build_action_prompt
        action_prompt = build_action_prompt(
            goal,
            self.step_history,
            len(self.step_history) + 1,
        )
        
        full_prompt = f"{self.system_prompt}\n\n{action_prompt}"
        
        # Call real LLM API
        try:
            logger.info(f'Calling LLM API for step {len(self.step_history) + 1}...')
            response_text, _, raw_response = self.llm_wrapper.predict_mm(
                full_prompt,
                [screenshot],
            )
            logger.info(f'LLM response: {response_text[:100]}...')
        except Exception as e:
            logger.error(f'LLM call failed: {e}')
            return {'done': True, 'status': 'llm_error', 'error': str(e)}
        
        # Store raw data
        raw_step_data = {
            'step': len(self.step_history) + 1,
            'llm_input': {
                'system_prompt': self.system_prompt,
                'action_prompt': action_prompt,
                'has_screenshot': True,
                'screenshot_shape': screenshot.shape,
                'full_prompt_with_history': full_prompt,
            },
            'llm_output': {
                'response_text': response_text,
                'raw_response': raw_response,
                'has_reasoning': False,
                'reasoning_content': None,
            }
        }
        
        # Parse action
        action_dict = self._parse_action(response_text)
        if action_dict is None:
            logger.error(f'Failed to parse action: {response_text}')
            return {'done': False, 'status': 'parse_failed'}
        
        # Execute action (mock)
        result = self._execute_action(action_dict)
        
        # Store step data
        step_data = {
            'step': len(self.step_history) + 1,
            'raw_response': response_text,
            'parsed_action': action_dict,
            'result': result,
        }
        self.step_history.append(step_data)
        
        # Generate summary
        step_summary = self._format_step_summary(action_dict, result)
        
        # Store readable entry
        readable_entry = {
            'step': len(self.readable_trajectory) + 1,
            'input': {
                'goal': goal,
                'previous_steps': [s.get('raw_response', '') for s in self.step_history[:-1]],
                'current_step_num': len(self.step_history),
            },
            'action': action_dict,
            'result': result,
            'summary': step_summary,
        }
        self.readable_trajectory.append(readable_entry)
        
        # Store raw trajectory
        # History is from PREVIOUS steps only (not including current step)
        # Step 1 has no history, Step 2 has Step 1's response, etc.
        current_step_num = len(self.step_history) + 1
        history_for_raw = []
        if current_step_num > 1:
            # Get raw responses from previous steps (up to 3 steps back)
            # step_history contains steps 0 to current_step_num-2
            start_idx = max(0, current_step_num - 4)  # Go back up to 3 steps
            end_idx = current_step_num - 1  # Up to (but not including) current step
            for j in range(start_idx, end_idx):
                if 0 <= j < len(self.step_history):
                    hist_step = self.step_history[j]
                    if isinstance(hist_step, dict):
                        history_for_raw.append(hist_step.get('raw_response', ''))
                    else:
                        history_for_raw.append(str(hist_step))
        
        # Get recent screenshots (last 3, including current)
        recent_screenshots = self.screenshots[-3:] if len(self.screenshots) >= 3 else self.screenshots
        
        raw_step_data['llm_input']['history_raw_responses'] = history_for_raw
        raw_step_data['llm_input']['recent_screenshots_count'] = len(recent_screenshots)
        raw_step_data['parsed_action'] = action_dict
        raw_step_data['execution_result'] = result
        self.raw_trajectory.append(raw_step_data)
        
        # Check if done
        done = (
            action_dict.get('action_type') == 'status' or
            result.get('status') in ['complete', 'infeasible']
        )
        
        return {'done': done, 'status': result.get('status', 'executed')}
    
    def _parse_action(self, response_text: str):
        """Parse JSON action from response."""
        import re
        json_match = re.search(r'\{[^{}]*"action_type"[^{}]*\}', response_text, re.DOTALL)
        if json_match:
            json_str = json_match.group(0)
        else:
            json_match = re.search(r'\{.*\}', response_text, re.DOTALL)
            if json_match:
                json_str = json_match.group(0)
            else:
                return None
        
        try:
            action = json.loads(json_str)
            if 'coordinate' in action:
                coord = action['coordinate']
                if isinstance(coord, list) and len(coord) >= 2:
                    action['x'] = int(coord[0])
                    action['y'] = int(coord[1])
                    del action['coordinate']
            return action
        except:
            return None
    
    def _execute_action(self, action_dict: Dict[str, Any]):
        """Execute action (mock)."""
        action_type = action_dict.get('action_type')
        
        if action_type == 'request_permission':
            return self.request_permission_handler.handle(
                action_dict.get('intent'),
                action_dict.get('resource_id'),
                action_dict.get('reason', ''),
            )
        elif action_type == 'save_profile':
            return self.save_profile_handler.handle(
                action_dict.get('key'),
                action_dict.get('value'),
                action_dict.get('level'),
                action_dict.get('reason', ''),
            )
        elif action_type == 'ask_user':
            return self.ask_user_handler.handle(
                action_dict.get('question'),
                action_dict.get('options'),
            )
        elif action_type == 'status':
            return {'status': action_dict.get('goal_status', 'complete')}
        else:
            # Mock GUI action execution
            return {'status': 'executed'}
    
    def _format_step_summary(self, action_dict, result):
        """Format step summary."""
        action_type = action_dict.get('action_type', 'unknown')
        
        if action_type == 'click':
            return f"Clicked at ({action_dict.get('x')}, {action_dict.get('y')})"
        elif action_type == 'input_text':
            text = action_dict.get('text', '')
            return f"Input text: {text[:20]}..." if len(text) > 20 else f"Input text: {text}"
        elif action_type == 'request_permission':
            return f"Requested permission: {action_dict.get('intent')} for {action_dict.get('resource_id')} - {result.get('granted', False)}"
        elif action_type == 'save_profile':
            return f"Saved profile: {action_dict.get('key')} = {result.get('status', 'error')}"
        elif action_type == 'status':
            return f"Task {action_dict.get('goal_status', 'complete')}"
        else:
            return f"{action_type}: {result.get('status', 'executed')}"


def load_data():
    """Load seed data and task definition."""
    base_path = Path(__file__).parent
    
    with open(base_path / 'data' / 'e2e_imy_seed.json', 'r', encoding='utf-8') as f:
        imy_data = json.load(f)
    
    with open(base_path / 'data' / 'e2e_mzocdoc_seed.json', 'r', encoding='utf-8') as f:
        mzocdoc_data = json.load(f)
    
    with open(base_path / 'tasks' / 'mzocdoc_book_appointment.json', 'r', encoding='utf-8') as f:
        task_def = json.load(f)
    
    return imy_data, mzocdoc_data, task_def


def main():
    """Main execution."""
    logger.info('='*60)
    logger.info('E2E Test - Simplified Version (Real LLM API)')
    logger.info('='*60)
    
    # Load data
    logger.info('Loading data...')
    imy_data, mzocdoc_data, task_def = load_data()
    
    # Create LLM wrapper
    logger.info('Creating LLM wrapper...')
    llm_wrapper = OpenAIWrapper(
        model_name='claude-opus-4-6',
        max_retry=3,
        temperature=0.0,
        top_p=0.95,
        max_tokens=4096,
    )
    
    # Create agent
    logger.info('Creating agent...')
    agent = SimplePhoneUseAgent(
        llm_wrapper=llm_wrapper,
        user_profile=imy_data,
        user_decisions=task_def.get('user_decisions', {}),
        max_steps=15,  # Limit for testing
    )
    
    # Run agent
    logger.info('Running agent...')
    goal = task_def['goal']
    step_count = 0
    max_steps = 15  # Full run
    
    while step_count < max_steps:
        step_count += 1
        logger.info(f'\n--- Step {step_count}/{max_steps} ---')
        
        result = agent.step(goal)
        
        if result.get('done'):
            logger.info(f'Agent finished at step {step_count}')
            break
        
        # Stop early if we get a status action
        if result.get('status') in ['complete', 'infeasible']:
            break
    
    logger.info(f'\nTotal steps: {step_count}')
    
    # Generate trajectories
    logger.info('Generating trajectories...')
    output_dir = Path(__file__).parent.parent.parent / 'e2e_output'
    readable_traj = generate_readable_trajectory(agent, task_def, output_dir)
    raw_traj = generate_raw_trajectory(agent, task_def)
    
    # Privacy evaluation
    logger.info('Evaluating privacy...')
    privacy_evaluator = PrivacyEvaluator(agent.access_log, task_def)
    privacy_result = privacy_evaluator.evaluate()
    
    # Save outputs
    output_dir = Path(__file__).parent.parent.parent / 'e2e_output'
    output_dir.mkdir(exist_ok=True)
    
    # Save readable trajectory
    readable_path = output_dir / 'readable_trajectory.json'
    with open(readable_path, 'w', encoding='utf-8') as f:
        json.dump(readable_traj, f, indent=2, default=str, ensure_ascii=False)
    logger.info(f'✅ Readable trajectory saved to {readable_path}')
    
    # Save raw trajectory
    raw_path = output_dir / 'raw_trajectory_sft.json'
    with open(raw_path, 'w', encoding='utf-8') as f:
        json.dump(raw_traj, f, indent=2, default=str, ensure_ascii=False)
    logger.info(f'✅ Raw trajectory (SFT) saved to {raw_path}')
    
    # Save report
    report = {
        'task_id': task_def.get('task_id'),
        'goal': task_def.get('goal'),
        'execution': {
            'step_count': step_count,
            'final_status': result.get('status', 'unknown'),
        },
        'verification': {
            'note': 'Verification requires real database - see task_verifier.py for logic',
            'rules': task_def.get('verification', {}).get('rules', []),
        },
        'privacy': privacy_result,
    }
    report_path = output_dir / 'e2e_report.json'
    with open(report_path, 'w', encoding='utf-8') as f:
        json.dump(report, f, indent=2, default=str, ensure_ascii=False)
    logger.info(f'✅ Report saved to {report_path}')
    
    # Print summary
    print('\n' + '='*60)
    print('E2E EXECUTION SUMMARY')
    print('='*60)
    print(f'Task: {task_def.get("task_id")}')
    print(f'Steps executed: {step_count}')
    print(f'Final status: {result.get("status", "unknown")}')
    print(f'Privacy score: {privacy_result.get("privacy_score", 0.0):.2f}')
    print(f'Over-permissioning violations: {len(privacy_result.get("over_permissioning", []))}')
    print('='*60)
    print(f'\n✅ Trajectories generated:')
    print(f'   - Readable: {readable_path}')
    print(f'   - Raw (SFT): {raw_path}')
    print(f'   - Report: {report_path}')


if __name__ == '__main__':
    main()

