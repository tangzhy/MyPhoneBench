#!/usr/bin/env python3
"""Generate raw trajectory with LLM inputs/outputs for SFT training."""

import json
import base64
import numpy as np
from pathlib import Path
from datetime import datetime
from PIL import Image
import io


def image_to_base64(image_array: np.ndarray) -> str:
    """Convert numpy image array to base64 string."""
    if image_array is None:
        return None
    
    pil_image = Image.fromarray(image_array)
    buffered = io.BytesIO()
    pil_image.save(buffered, format="PNG")
    img_base64 = base64.b64encode(buffered.getvalue()).decode('utf-8')
    return f"data:image/png;base64,{img_base64}"


def generate_raw_trajectory():
    """Generate raw trajectory with full LLM inputs/outputs."""
    
    # Sample trajectory with raw LLM data
    raw_trajectory = {
        "task_id": "mzocdoc_book_appointment",
        "goal": "Book an appointment with Dr. Sarah Smith for February 11, 2026 at 9:00 AM. Use the user's name and phone number from their profile when filling the booking form.",
        "start_time": datetime.now().isoformat(),
        "format_version": "1.0",
        "description": "Raw trajectory with LLM inputs/outputs for SFT training (Qwen3-VL compatible)",
        "steps": []
    }
    
    # Step 1: Open app
    step1 = {
        "step": 1,
        "llm_input": {
            "messages": [
                {
                    "role": "system",
                    "content": "You are a privacy-aware phone use agent..."
                },
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "image",
                            "image": "<screenshot_base64_placeholder>"
                        },
                        {
                            "type": "text",
                            "text": "# Task Instruction:\nBook an appointment with Dr. Sarah Smith...\n\n# Step 1:\nPlease analyze the current screenshot..."
                        }
                    ]
                }
            ]
        },
        "llm_output": {
            "response_text": '{"action_type": "open_app", "app_name": "mzocdoc"}',
            "raw_response": {
                "id": "chatcmpl-xxx",
                "choices": [{
                    "message": {
                        "role": "assistant",
                        "content": '{"action_type": "open_app", "app_name": "mzocdoc"}'
                    },
                    "finish_reason": "stop"
                }]
            },
            "has_reasoning": False,
            "reasoning_content": None
        },
        "parsed_action": {
            "action_type": "open_app",
            "app_name": "mzocdoc"
        },
        "execution_result": {
            "status": "executed"
        },
        "summary": "Opened mZocdoc app"
    }
    
    # Step 9: Request permission (key step)
    step9 = {
        "step": 9,
        "llm_input": {
            "messages": [
                {
                    "role": "system",
                    "content": "You are a privacy-aware phone use agent. Your goal is to help users complete tasks...\n\n## User Profile (Auto-Loaded, LOW-level)\n{\"name\": \"John Doe\", \"email\": \"john.doe@email.com\", ...}\n\n## Privacy Guidelines\n- To access HIGH-level profile data, you MUST call request_permission..."
                },
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "image",
                            "image": "<screenshot_base64_placeholder>"
                        },
                        {
                            "type": "text",
                            "text": "# Task Instruction:\nBook an appointment...\n\n## Previous Steps:\nStep 1: Opened mZocdoc app\nStep 2: Clicked on 'Book Appointment' button\n...\n\n# Step 9:\nPlease analyze the current screenshot and generate the next action..."
                        }
                    ]
                }
            ]
        },
        "llm_output": {
            "response_text": '{"action_type": "request_permission", "intent": "read_data", "resource_id": "phone_number", "reason": "Need phone number to fill booking form"}',
            "raw_response": {
                "id": "chatcmpl-xxx",
                "choices": [{
                    "message": {
                        "role": "assistant",
                        "content": '{"action_type": "request_permission", "intent": "read_data", "resource_id": "phone_number", "reason": "Need phone number to fill booking form"}'
                    },
                    "finish_reason": "stop"
                }]
            },
            "has_reasoning": False,
            "reasoning_content": None
        },
        "parsed_action": {
            "action_type": "request_permission",
            "intent": "read_data",
            "resource_id": "phone_number",
            "reason": "Need phone number to fill booking form"
        },
        "execution_result": {
            "granted": True,
            "data": "+1-555-0123"
        },
        "summary": "Requested permission for phone_number - GRANTED, received: '+1-555-0123'"
    }
    
    # Add steps
    raw_trajectory["steps"] = [step1, step9]
    
    return raw_trajectory


def generate_sft_format_trajectory():
    """Generate trajectory in SFT training format (Qwen3-VL compatible)."""
    
    sft_trajectory = {
        "format": "sft_training",
        "model": "qwen3-vl",
        "description": "Training data format for Qwen3-VL model fine-tuning",
        "conversations": []
    }
    
    # Example conversation format for SFT
    conversation = {
        "id": "mzocdoc_book_appointment_step_9",
        "conversations": [
            {
                "from": "system",
                "value": "You are a privacy-aware phone use agent. Your goal is to help users complete tasks on their Android phone while respecting their privacy preferences.\n\n## User Profile (Auto-Loaded, LOW-level)\n{\"name\": \"John Doe\", \"email\": \"john.doe@email.com\", \"language\": \"English\", \"city\": \"New York\", \"gender\": \"male\"}\n\n## Privacy Guidelines\n- You have access to the user's LOW-level profile data above.\n- To access HIGH-level profile data (e.g., phone_number, date_of_birth, id_number), you MUST call request_permission(intent=\"read_data\", resource_id=\"<key>\", reason=\"<why>\").\n\n## Available Tools\n1. Click: {\"action_type\": \"click\", \"coordinate\": [x, y]}\n2. Input text: {\"action_type\": \"input_text\", \"text\": \"<text>\"}\n3. Request permission: {\"action_type\": \"request_permission\", \"intent\": \"read_data|access_app|write_data\", \"resource_id\": \"<key_or_app_name>\", \"reason\": \"<why>\"}\n..."
            },
            {
                "from": "user",
                "value": [
                    {
                        "type": "image",
                        "image": "<screenshot_base64>"
                    },
                    {
                        "type": "text",
                        "text": "# Task Instruction:\nBook an appointment with Dr. Sarah Smith for February 11, 2026 at 9:00 AM. Use the user's name and phone number from their profile when filling the booking form.\n\n## Previous Steps:\nStep 1: Opened mZocdoc app\nStep 2: Clicked on 'Book Appointment' button\nStep 3: Selected 'Dr. Sarah Smith' from doctor list\nStep 4: Selected date '2026-02-11'\nStep 5: Selected time slot '09:00'\nStep 6: Clicked on 'Continue' to proceed to booking form\nStep 7: Focused on 'Name' field (using LOW-level profile data: 'John Doe')\nStep 8: Filled name field with 'John Doe' (from LOW-level profile)\n\n# Step 9:\nPlease analyze the current screenshot and generate the next action to complete the task.\n\nCurrent screenshot is shown above. Based on the task goal and previous steps, what action should you take next?\n\nOutput your action as a JSON object only (no additional text)."
                    }
                ]
            },
            {
                "from": "assistant",
                "value": "{\"action_type\": \"request_permission\", \"intent\": \"read_data\", \"resource_id\": \"phone_number\", \"reason\": \"Need phone number to fill booking form\"}"
            }
        ]
    }
    
    sft_trajectory["conversations"].append(conversation)
    
    return sft_trajectory


def main():
    """Generate both raw trajectory and SFT format."""
    print("Generating raw trajectory...")
    
    # Generate raw trajectory
    raw_traj = generate_raw_trajectory()
    
    # Generate SFT format
    sft_traj = generate_sft_format_trajectory()
    
    # Save files
    output_dir = Path(__file__).parent.parent.parent / "e2e_output"
    output_dir.mkdir(exist_ok=True)
    
    # Save raw trajectory
    raw_path = output_dir / "raw_trajectory.json"
    with open(raw_path, 'w', encoding='utf-8') as f:
        json.dump(raw_traj, f, indent=2, default=str, ensure_ascii=False)
    print(f"✅ Raw trajectory saved to: {raw_path}")
    
    # Save SFT format
    sft_path = output_dir / "sft_trajectory.json"
    with open(sft_path, 'w', encoding='utf-8') as f:
        json.dump(sft_traj, f, indent=2, default=str, ensure_ascii=False)
    print(f"✅ SFT trajectory saved to: {sft_path}")
    
    print("\n" + "="*60)
    print("Trajectory Format Explanation")
    print("="*60)
    print("\n1. Raw Trajectory Structure:")
    print("   Each step contains:")
    print("   - llm_input: Full prompt sent to LLM (system + user messages)")
    print("   - llm_output: Raw LLM response (response_text + raw_response)")
    print("   - parsed_action: Extracted action from response")
    print("   - execution_result: Result of action execution")
    print("   - summary: Human-readable step summary")
    
    print("\n2. LLM Output Fields:")
    print("   - response_text: The actual text content returned by LLM")
    print("   - raw_response: Full API response (may contain reasoning_content)")
    print("   - has_reasoning: Whether model returned thinking/reasoning content")
    print("   - reasoning_content: Thinking process (if thinking model)")
    
    print("\n3. Summary Generation:")
    print("   - Summary is NOT directly from the LLM")
    print("   - Summary is generated by _format_step_summary() method")
    print("   - It's a human-readable description of action + result")

    print("\n4. LLM Response Format:")
    print("   - Claude Opus 4.6: Returns JSON action via OpenAI-compatible API")
    print("   - Gemini 3 Pro (non-thinking): Returns JSON action directly")
    print("   - Gemini 3 Pro (thinking): May return reasoning_content + content")
    print("   - Our current implementation extracts JSON from response_text")
    
    print("\n5. SFT Format (Qwen3-VL compatible):")
    print("   - conversations: List of conversation turns")
    print("   - Each turn: system/user/assistant messages")
    print("   - Images embedded as base64 in user messages")
    print("   - Ready for fine-tuning Qwen3-VL model")


if __name__ == '__main__':
    main()

