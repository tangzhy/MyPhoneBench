#!/usr/bin/env python3
"""Run E2E test in mock mode (without real Android environment) for trajectory generation."""

import json
import os
import sys
import logging
from pathlib import Path

# Add project root to path
sys.path.insert(0, str(Path(__file__).parent.parent.parent.parent))

from android_world.phoneuse.agents.llm_wrapper import OpenAIWrapper
from android_world.phoneuse.agents.phoneuse_agent import PhoneUseAgent
from android_world.phoneuse.tasks.task_verifier import TaskVerifier
from android_world.phoneuse.tasks.privacy_evaluator import PrivacyEvaluator
from android_world.phoneuse.generate_final_trajectories import (
    generate_readable_trajectory,
    generate_raw_trajectory,
)

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


def load_data():
    """Load seed data and task definition."""
    base_path = Path(__file__).parent
    
    # Load seed data
    with open(base_path / 'data' / 'e2e_imy_seed.json', 'r', encoding='utf-8') as f:
        imy_data = json.load(f)
    
    with open(base_path / 'data' / 'e2e_mzocdoc_seed.json', 'r', encoding='utf-8') as f:
        mzocdoc_data = json.load(f)
    
    # Load task definition
    with open(base_path / 'tasks' / 'mzocdoc_book_appointment.json', 'r', encoding='utf-8') as f:
        task_def = json.load(f)
    
    return imy_data, mzocdoc_data, task_def


def create_mock_agent(imy_data, task_def):
    """Create agent with mock environment."""
    # Create LLM wrapper
    llm_wrapper = OpenAIWrapper(
        model_name='claude-opus-4-6',
        max_retry=3,
        temperature=0.0,
        top_p=0.95,
        max_tokens=4096,
    )
    
    # Create mock environment (will be None, agent will handle it)
    class MockEnv:
        def get_state(self, wait_to_stabilize=False):
            class MockState:
                pixels = None
                ui_elements = []
            return MockState()
        
        def reset(self, go_home=False):
            pass
        
        controller = None
    
    mock_env = MockEnv()
    
    # Create agent
    agent = PhoneUseAgent(
        env=mock_env,
        llm_wrapper=llm_wrapper,
        user_profile=imy_data,
        user_decisions=task_def.get('user_decisions', {}),
        max_steps=20,  # Limit steps for testing
    )
    
    return agent


def run_mock_execution(agent, goal, max_steps=20):
    """Run agent in mock mode."""
    logger.info(f'Starting mock execution with goal: {goal}')
    
    step_count = 0
    step_results = []
    
    while step_count < max_steps:
        step_count += 1
        logger.info(f'Step {step_count}/{max_steps}')
        
        try:
            result = agent.step(goal)
            step_results.append(result.data)
            
            if result.done:
                logger.info(f'Agent finished at step {step_count}')
                break
        except Exception as e:
            logger.error(f'Step {step_count} failed: {e}')
            break
    
    return {
        'step_count': step_count,
        'step_results': step_results,
        'access_log': agent.access_log.to_dict_list(),
        'final_status': step_results[-1].get('status') if step_results else 'unknown',
    }


def main():
    """Main execution."""
    logger.info('Loading data...')
    imy_data, mzocdoc_data, task_def = load_data()
    
    logger.info('Creating mock agent...')
    agent = create_mock_agent(imy_data, task_def)
    
    logger.info('Running mock execution...')
    execution_result = run_mock_execution(agent, task_def['goal'], max_steps=20)
    
    logger.info('Generating trajectories...')
    readable_traj = generate_readable_trajectory(agent, task_def)
    raw_traj = generate_raw_trajectory(agent, task_def)
    
    # Mock verification (since we don't have real DB)
    verification_result = {
        'resolved': False,
        'note': 'Mock mode - verification skipped (no real database)',
        'details': {
            'query': 'SELECT * FROM appointments WHERE ...',
            'results': [],
            'match_count': 0,
        }
    }
    
    # Privacy evaluation
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
        'execution': execution_result,
        'verification': verification_result,
        'privacy': privacy_result,
    }
    report_path = output_dir / 'e2e_report.json'
    with open(report_path, 'w', encoding='utf-8') as f:
        json.dump(report, f, indent=2, default=str, ensure_ascii=False)
    logger.info(f'✅ Report saved to {report_path}')
    
    # Print summary
    print('\n' + '='*60)
    print('MOCK EXECUTION SUMMARY')
    print('='*60)
    print(f'Task: {task_def.get("task_id")}')
    print(f'Steps executed: {execution_result["step_count"]}')
    print(f'Privacy score: {privacy_result.get("privacy_score", 0.0):.2f}')
    print(f'Over-permissioning violations: {len(privacy_result.get("over_permissioning", []))}')
    print('='*60)
    print(f'\n✅ Trajectories generated:')
    print(f'   - Readable: {readable_path}')
    print(f'   - Raw (SFT): {raw_path}')


if __name__ == '__main__':
    main()

