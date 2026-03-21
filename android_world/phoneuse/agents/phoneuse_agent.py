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

"""PhoneUse Agent - Screenshot-based GUI agent with privacy awareness.

Aligned with MobileWorld (MAI) inference pipeline and action space.
"""

import json
import re
import time
import logging
import base64
import io
from typing import Dict, List, Any, Optional, Tuple
import numpy as np
from PIL import Image

from android_world.agents import base_agent
from android_world.env import interface
from android_world.env import json_action
from android_world.env import actuation
from android_world.env import adb_utils
from android_world.phoneuse.agents.llm_wrapper import OpenAIWrapper
from android_world.phoneuse.agents.phoneuse_prompt import build_system_prompt
from android_world.phoneuse.privacy.access_log import AccessLog
from android_world.phoneuse.privacy.request_permission_handler import RequestPermissionHandler
from android_world.phoneuse.privacy.save_profile_handler import SaveProfileHandler
from android_world.phoneuse.privacy.ask_user_handler import AskUserHandler
from android_world.phoneuse.privacy.user_agent import (
    UserAgent, DeterministicUserAgent, LLMUserAgent,
)

# Actions that are privacy-related (results injected into message history)
PRIVACY_ACTIONS = ('request_permission', 'read_profile', 'save_profile', 'ask_user')


# Per-model coordinate strategy.
#
# 'fractional' — model outputs coordinates as fractions of its perceived
#                image dimensions: [x/width, y/height] in range [0, 1].
#                Resolution-independent; no need to know the model's
#                internal image size.  Works for models that honestly
#                self-report their perceived dimensions (Claude, Gemini, GPT).
#
# 'decimal'    — model outputs 0-1 decimal coordinates directly, e.g. 0.556.
#                Similar to fractional but without the "pixel/width" fraction
#                format.  Avoids the extra cognitive step of estimating
#                perceived image dimensions.  Matches OSWorld convention.
#
# ('resize', N) — resize screenshot so long side = N pixels.  Model
#                 outputs pixel coords for the resized image.  Needed for
#                 models that claim the original resolution but output
#                 coords based on their (smaller) internal resolution (Kimi).
#
# int N         — model outputs 0-N normalized coords.
#
_MODEL_COORD_CONFIG = {
    # Fractional: Claude self-reports perceived dims accurately
    'claude':    'fractional',
    # Decimal: Kimi outputs 0-1 coords (OSWorld convention)
    'kimi':      'decimal',
    # Resize: GPT's perceived-dimension denominator is unstable across steps
    # (e.g. width jumps between 828 and 1730), causing wild x-coordinate
    # drift. Pre-resizing the image to a fixed size eliminates this.
    'gpt':       ('resize', 1344),
    # Normalized: Gemini outputs 0-999
    'gemini':    999,
    # Normalized 0-1000: GLM, Doubao/Seed, Qwen all use this convention
    'glm':       1000,
    'doubao':    1000,
    'seed':      1000,
    # Best-effort defaults
    'deepseek':  ('resize', 1024),
    'qwen':      1000,
}


def _get_coord_config(model_name: str):
    """Return (coord_scale, max_long_side) for a model.

    Returns:
        'fractional'      — fractional coordinate mode
        ('resize', int)   — resize mode with max long side
        (int_N, None)     — normalized 0-N mode
    """
    name = model_name.lower()
    for keyword, cfg in _MODEL_COORD_CONFIG.items():
        if keyword in name:
            if cfg in ('fractional', 'decimal'):
                return (cfg, None)
            if isinstance(cfg, tuple):
                return cfg
            return (cfg, None)
    return ('fractional', None)  # default: fractional


class PhoneUseAgent(base_agent.EnvironmentInteractingAgent):
    """Privacy-aware phone use agent with screenshot-based reasoning.

    Inference pipeline aligned with MobileWorld (MAI):
    - Task instruction as standalone user message (always kept)
    - Each step: user(screenshot) + assistant(response) pair
    - Post-hoc image removal: keep only recent max_image_history screenshots
    - Privacy action results injected as user text messages
    """

    # Map package names to their main activity (for am start, bypasses monkey)
    PACKAGE_TO_ACTIVITY = {
        'com.phoneuse.mzocdoc': 'com.phoneuse.mzocdoc/.app.MainActivity',
        'com.phoneuse.imy': 'com.phoneuse.imy/.app.MainActivity',
        'com.phoneuse.mcvspharmacy': 'com.phoneuse.mcvspharmacy/.app.MainActivity',
        'com.phoneuse.mzillow': 'com.phoneuse.mzillow/.app.MainActivity',
        'com.phoneuse.mopentable': 'com.phoneuse.mopentable/.app.MainActivity',
        'com.phoneuse.mbooking': 'com.phoneuse.mbooking/.app.MainActivity',
        'com.phoneuse.mdmv': 'com.phoneuse.mdmv/.app.MainActivity',
        'com.phoneuse.mdoordash': 'com.phoneuse.mdoordash/.app.MainActivity',
        'com.phoneuse.meventbrite': 'com.phoneuse.meventbrite/.app.MainActivity',
        'com.phoneuse.mgeico': 'com.phoneuse.mgeico/.app.MainActivity',
        'com.phoneuse.mthumbtack': 'com.phoneuse.mthumbtack/.app.MainActivity',
    }

    # Map of common app names to Android package names
    APP_NAME_TO_PACKAGE = {
        'mzocdoc': 'com.phoneuse.mzocdoc',
        'zocdoc': 'com.phoneuse.mzocdoc',
        'imy': 'com.phoneuse.imy',
        'mcvs pharmacy': 'com.phoneuse.mcvspharmacy',
        'mcvspharmacy': 'com.phoneuse.mcvspharmacy',
        'mcvs': 'com.phoneuse.mcvspharmacy',
        'cvs pharmacy': 'com.phoneuse.mcvspharmacy',
        'cvs': 'com.phoneuse.mcvspharmacy',
        'mzillow': 'com.phoneuse.mzillow',
        'zillow': 'com.phoneuse.mzillow',
        'mopentable': 'com.phoneuse.mopentable',
        'opentable': 'com.phoneuse.mopentable',
        'mbooking': 'com.phoneuse.mbooking',
        'booking': 'com.phoneuse.mbooking',
        'mdmv': 'com.phoneuse.mdmv',
        'dmv': 'com.phoneuse.mdmv',
        'mdoordash': 'com.phoneuse.mdoordash',
        'doordash': 'com.phoneuse.mdoordash',
        'meventbrite': 'com.phoneuse.meventbrite',
        'eventbrite': 'com.phoneuse.meventbrite',
        'mgeico': 'com.phoneuse.mgeico',
        'geico': 'com.phoneuse.mgeico',
        'mthumbtack': 'com.phoneuse.mthumbtack',
        'thumbtack': 'com.phoneuse.mthumbtack',
        'chrome': 'com.android.chrome',
        'settings': 'com.android.settings',
        'contacts': 'com.google.android.contacts',
        'messages': 'com.google.android.apps.messaging',
        'camera': 'com.android.camera2',
        'calendar': 'com.google.android.calendar',
        'gmail': 'com.google.android.gm',
        'maps': 'com.google.android.apps.maps',
        'photos': 'com.google.android.apps.photos',
    }

    # Maximum actions per LLM response.  Default 10 for models that
    # mostly comply with "one action per turn" (Claude/GPT).
    # Overridden to 1 for Gemini/GLM in __init__ — data shows multi-action
    # is the #1 cause of task failure (20/23 tasks failed due to blind
    # coordinate chaining after screen changes).
    MAX_ACTIONS_PER_STEP = 10
    _SINGLE_ACTION_MODELS = ('gemini', 'glm')

    def __init__(
        self,
        env: interface.AsyncEnv,
        llm_wrapper: OpenAIWrapper,
        user_profile: Dict[str, Any],
        user_decisions: Optional[Dict[str, Dict[str, str]]] = None,
        max_steps: int = 50,
        max_image_history: int = 3,
        name: str = 'PhoneUseAgent',
        enable_personalization: bool = False,
        **kwargs,
    ):
        super().__init__(env, name, transition_pause=2.0)
        self.llm_wrapper = llm_wrapper
        self.user_profile = user_profile
        self.user_decisions = user_decisions or {}
        self.max_steps = max_steps
        self.max_image_history = max_image_history

        # Privacy components
        self.access_log = AccessLog()
        privacy_mode = user_profile.get('privacy_mode', 'full_control')

        # Create UserAgent (switchable via user_agent_mode kwarg)
        user_agent_mode = kwargs.get('user_agent_mode', 'deterministic')
        if user_agent_mode == 'llm':
            user_persona = self.user_decisions.get('user_persona', {})
            self.user_agent = LLMUserAgent(llm_wrapper, user_persona)
        else:
            self.user_agent = DeterministicUserAgent(self.user_decisions)

        # Pass user_agent to handlers
        self.request_permission_handler = RequestPermissionHandler(
            access_log=self.access_log,
            user_agent=self.user_agent,
            user_profile=user_profile,
        )
        self.save_profile_handler = SaveProfileHandler(self.access_log, privacy_mode, env=env)
        self.ask_user_handler = AskUserHandler(user_agent=self.user_agent)

        # State tracking
        self.step_history: List[Dict[str, Any]] = []
        self.screenshots: List[np.ndarray] = []
        self.raw_trajectory: List[Dict[str, Any]] = []
        self._denied_permissions: set = set()  # (intent, resource_id) tuples
        self.coord_scale, self.screenshot_max_long_side = _get_coord_config(self.llm_wrapper.model_name)
        model_lower = self.llm_wrapper.model_name.lower()
        if any(m in model_lower for m in self._SINGLE_ACTION_MODELS):
            self.MAX_ACTIONS_PER_STEP = 1
            logging.info('Model %s: enforcing single-action mode', self.llm_wrapper.model_name)
        # screenshot_size is set on first screenshot (after resize); used
        # by _denormalize_coordinate when coord_scale == 'resize'.
        self.screenshot_size: Optional[Tuple[int, int]] = None
        self.system_prompt = build_system_prompt(
            user_profile, privacy_mode, coord_scale=self.coord_scale,
            enable_personalization=enable_personalization,
        )

    def reset(self, go_home: bool = False):
        """Reset agent state."""
        super().reset(go_home=go_home)
        self.step_history = []
        self.screenshots = []
        self.raw_trajectory = []
        self.access_log.clear()
        self._denied_permissions.clear()

    # ── Main step ──────────────────────────────────────────────

    def step(self, goal: str) -> base_agent.AgentInteractionResult:
        """Perform one step of the agent.

        One step = one LLM call.  The LLM may output multiple <tool_call>
        blocks; they are executed sequentially.  Execution stops early if
        any action fails, is blocked, or signals completion.

        Intermediate screenshots are taken after GUI actions (except the
        last) for trajectory logging, but only the final screenshot is
        fed back to the LLM on the next step.
        """
        if len(self.step_history) >= self.max_steps:
            logging.warning(f'Reached max steps {self.max_steps}')
            return base_agent.AgentInteractionResult(
                done=True,
                data={'status': 'infeasible', 'reason': 'max_steps_reached'},
            )

        # 1. Get current screenshot
        try:
            state = self.get_post_transition_state()
            screenshot = state.pixels
            self.screenshots.append(screenshot.copy())
        except Exception as e:
            logging.warning(f'Could not get state, using mock screenshot: {e}')
            screenshot = np.zeros((2400, 1080, 3), dtype=np.uint8)
            self.screenshots.append(screenshot.copy())
            state = type('MockState', (), {'ui_elements': []})()

        # 2. Build messages (MobileWorld pipeline)
        messages = self._build_messages(goal, screenshot)

        # 3. Call LLM
        try:
            response_text, _, raw_response = self.llm_wrapper.predict_messages(messages)
        except Exception as e:
            logging.error(f'LLM call failed: {e}')
            return base_agent.AgentInteractionResult(
                done=True,
                data={'status': 'infeasible', 'reason': f'llm_error: {e}'},
            )

        # 3b. Merge reasoning_content into response_text for models (e.g. GLM)
        #     that put thinking in a separate API field instead of <thinking> tags.
        #     GLM has 3 output patterns:
        #       A) content has <thinking>+action → no merge needed
        #       B) content has JSON only, reasoning_content has thinking → prepend
        #       C) content is empty, reasoning_content has thinking+action → reconstruct
        #     This ensures: (a) _parse_actions finds thinking, (b) assistant
        #     history messages include <thinking>, reinforcing the output pattern.
        if '<thinking>' not in response_text:
            reasoning_raw = ''
            if raw_response:
                reasoning_raw = (
                    raw_response.get('choices', [{}])[0]
                    .get('message', {}).get('reasoning_content', '') or ''
                )
            if reasoning_raw:
                # Clean reasoning: strip stray <thinking>/</ thinking> tags
                clean = reasoning_raw.replace('</thinking>', '').replace('<thinking>', '').strip()
                # Separate thinking text from any action JSON in reasoning
                action_in_reasoning = re.search(
                    r'\{[^{}]*"action"[^{}]*\}', clean
                )
                thinking_text = (
                    clean[:action_in_reasoning.start()].strip()
                    if action_in_reasoning else clean
                )
                if thinking_text:
                    if response_text.strip():
                        # Pattern B: content has action, reasoning has thinking
                        response_text = (
                            f'<thinking>\n{thinking_text}\n</thinking>\n'
                            + response_text
                        )
                    elif action_in_reasoning:
                        # Pattern C: content empty, reasoning has both
                        response_text = (
                            f'<thinking>\n{thinking_text}\n</thinking>\n'
                            + action_in_reasoning.group(0)
                        )

        # 4. Parse ALL actions from <thinking> + <tool_call> blocks
        thinking, action_list = self._parse_actions(response_text)
        if not action_list:
            logging.error(f'Failed to parse action from: {response_text}')
            return base_agent.AgentInteractionResult(
                done=False,
                data={'status': 'error', 'reason': 'parse_failed', 'raw_response': response_text},
            )

        # Cap actions to avoid runaway execution when LLM outputs 50-100+ blocks
        if len(action_list) > self.MAX_ACTIONS_PER_STEP:
            logging.warning(
                f'LLM returned {len(action_list)} actions, '
                f'truncating to {self.MAX_ACTIONS_PER_STEP}'
            )
            action_list = action_list[:self.MAX_ACTIONS_PER_STEP]

        # 5. Execute actions sequentially, stop on failure
        action_results: List[Tuple[Dict[str, Any], Dict[str, Any]]] = []
        intermediate_screenshots: List[np.ndarray] = []

        for i, action_dict in enumerate(action_list):
            result = self._execute_action(action_dict, state)
            action_results.append((action_dict, result))

            # Stop if action failed, was blocked, or signals completion
            if result.get('status') in ('error', 'blocked', 'complete', 'infeasible'):
                break

            # After a swipe/scroll, stop executing further actions so the
            # LLM gets a fresh screenshot — swipe invalidates all coordinates.
            # Other GUI actions (click, type, system_button) can safely batch
            # since they don't rearrange the screen layout.
            # Privacy actions never change the screen and can always batch.
            action_name = action_dict.get('action', '')
            if action_name in ('swipe', 'drag', 'open'):
                break

        # 6. Build combined result text for privacy/blocked actions
        result_texts: List[str] = []
        for action_dict, result in action_results:
            action_name = action_dict.get('action', '')
            is_privacy = action_name in PRIVACY_ACTIONS
            is_blocked = result.get('status') == 'blocked'
            if is_privacy or is_blocked:
                result_texts.append(
                    self._format_privacy_result(action_name, action_dict, result)
                )
        combined_result_text = '\n'.join(result_texts) if result_texts else None

        # 7. Truncate response to executed actions only.
        # When the LLM outputs N tool_call blocks but only M < N were
        # executed (due to error-break, swipe-break, or MAX_ACTIONS cap),
        # the unexecuted tool_calls must NOT appear in the conversation
        # history — otherwise the model sees "I called request_permission"
        # without a corresponding result and enters an infinite wait loop.
        executed_count = len(action_results)
        total_parsed = len(action_list)
        if executed_count < total_parsed:
            history_response = self._truncate_response(
                response_text, executed_count
            )
            logging.info(
                'Truncated response: %d/%d actions executed, '
                'stripped %d unexecuted tool_call(s) from history',
                executed_count, total_parsed, total_parsed - executed_count,
            )
        else:
            history_response = response_text

        # 8. Store raw trajectory data
        model_reasoning = self.llm_wrapper.extract_reasoning(raw_response)
        raw_step_data = {
            'step': len(self.step_history) + 1,
            'messages': messages,
            'response': {'text': response_text, 'raw': raw_response},
            'thinking': thinking,
            'model_reasoning': model_reasoning,  # DeepSeek/Gemini internal reasoning
            'parsed_actions': [ad for ad, _ in action_results],
            'execution_results': [r for _, r in action_results],
            'intermediate_screenshots_count': len(intermediate_screenshots),
        }
        self.raw_trajectory.append(raw_step_data)

        # 9. Store step history
        step_data = {
            'step': len(self.step_history) + 1,
            'thinking': thinking,
            'parsed_actions': [ad for ad, _ in action_results],
            'raw_response': history_response,
            'results': [r for _, r in action_results],
            'is_privacy_action': bool(combined_result_text),
            'result_text': combined_result_text,
            'intermediate_screenshots': intermediate_screenshots,
            # Backward compat: singular keys for code that accesses them
            'parsed_action': action_results[0][0] if action_results else {},
            'result': action_results[-1][1] if action_results else {},
        }
        self.step_history.append(step_data)

        # 9. Check if done (check last executed action)
        last_action, last_result = action_results[-1]
        done = (
            last_action.get('action') == 'terminate'
            or last_result.get('status') in ('complete', 'infeasible')
        )

        return base_agent.AgentInteractionResult(
            done=done,
            data={
                'actions': [ad for ad, _ in action_results],
                'results': [r for _, r in action_results],
                'access_log': self.access_log.to_dict_list(),
                # Backward compat
                'action': action_results[0][0] if action_results else {},
                'result': action_results[-1][1] if action_results else {},
            },
        )

    # ── Message building (MobileWorld pipeline) ───────────────

    def _build_messages(
        self,
        goal: str,
        current_screenshot: np.ndarray,
    ) -> List[Dict[str, Any]]:
        """Build MobileWorld-style multi-turn messages.

        Structure:
        1. user: system prompt + task instruction (text only, always kept)
        2. user: screenshot_1  →  assistant: response_1  [→ user: result_text_1]
        3. user: screenshot_2  →  assistant: response_2  [→ user: result_text_2]
        ...
        N. user: screenshot_current

        The system prompt is merged into the first user message (not sent as
        role=system) for universal proxy compatibility — some proxies silently
        drop system messages.

        Privacy/tool results are injected as text-only user messages after
        the corresponding assistant response. _hide_history_images only prunes
        image_url user messages, so text-only results are never pruned.
        """
        messages: List[Dict[str, Any]] = []

        # 1. System prompt + task instruction (merged into one user message
        #    to avoid proxies silently dropping role=system messages)
        messages.append({
            'role': 'user',
            'content': self.system_prompt + '\n\n---\n\n## Task\n' + goal,
        })

        # 2. History: screenshot (user) + response (assistant) [+ result (user)]
        for i, step_data in enumerate(self.step_history):
            # User message: screenshot for this step
            if i < len(self.screenshots):
                messages.append({
                    'role': 'user',
                    'content': [{
                        'type': 'image_url',
                        'image_url': {'url': self._image_to_base64(self.screenshots[i])},
                    }],
                })

            # Assistant message: raw LLM response
            raw_resp = step_data.get('raw_response', '')
            if raw_resp:
                messages.append({'role': 'assistant', 'content': raw_resp})

            # Inject privacy/tool result as text-only user message
            if step_data.get('is_privacy_action') and step_data.get('result_text'):
                messages.append({
                    'role': 'user',
                    'content': [{'type': 'text', 'text': step_data['result_text']}],
                })

        # 2b. Detect consecutive-wait loop and inject corrective nudge.
        # Some models (especially Gemini) hallucinate a pending permission
        # request and then loop `wait` indefinitely.  After 3 consecutive
        # waits, inject a user message that breaks the illusion.
        _MAX_CONSECUTIVE_WAITS = 3
        consec_waits = 0
        for sd in reversed(self.step_history):
            actions = sd.get('parsed_actions', [])
            if actions and all(a.get('action') == 'wait' for a in actions):
                consec_waits += 1
            else:
                break
        if consec_waits >= _MAX_CONSECUTIVE_WAITS:
            nudge = (
                "⚠️ You have called `wait` "
                f"{consec_waits} times in a row. "
                "No permission request is currently pending — "
                "all previous requests have already been resolved. "
                "Please look at the current screenshot and continue "
                "with the next action to complete the task."
            )
            messages.append({
                'role': 'user',
                'content': [{'type': 'text', 'text': nudge}],
            })
            logging.warning(
                'Consecutive-wait loop detected (%d waits). '
                'Injecting corrective nudge.', consec_waits
            )

        # 3. Current step: screenshot only
        messages.append({
            'role': 'user',
            'content': [{
                'type': 'image_url',
                'image_url': {'url': self._image_to_base64(current_screenshot)},
            }],
        })

        # 4. Post-hoc: remove old screenshot user messages
        messages = self._hide_history_images(messages)

        return messages

    def _hide_history_images(self, messages: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Remove old image-containing user messages, keeping only the most recent N.

        Aligned with MobileWorld's _hide_history_images.
        Text-only user messages (privacy results) are never removed.
        """
        # Collect indices of user messages that contain images (from back to front)
        image_msg_indices: List[int] = []
        for i in range(len(messages) - 1, -1, -1):
            msg = messages[i]
            if (
                msg.get('role') == 'user'
                and isinstance(msg.get('content'), list)
                and len(msg['content']) > 0
                and msg['content'][0].get('type') == 'image_url'
            ):
                image_msg_indices.append(i)

        # Remove all but the most recent max_image_history images
        indices_to_remove = sorted(image_msg_indices[self.max_image_history:], reverse=True)
        for idx in indices_to_remove:
            del messages[idx]

        return messages

    # ── Response truncation ────────────────────────────────────

    @staticmethod
    def _truncate_response(response_text: str, keep_n: int) -> str:
        """Keep only the first *keep_n* <tool_call> blocks in *response_text*.

        Preserves <thinking> and any text before the first tool_call.
        Removes all tool_call blocks beyond the *keep_n*-th one, preventing
        unexecuted actions from polluting conversation history.
        """
        parts: List[str] = []
        # Everything before the first <tool_call>
        first_tc = response_text.find('<tool_call>')
        if first_tc == -1:
            return response_text
        parts.append(response_text[:first_tc])

        # Collect all <tool_call>...</tool_call> spans
        tc_pattern = re.compile(r'<tool_call>.*?</tool_call>', re.DOTALL)
        matches = list(tc_pattern.finditer(response_text))
        for m in matches[:keep_n]:
            parts.append(m.group(0))

        return '\n'.join(parts).strip()

    # ── Action parsing (MobileWorld format) ───────────────────

    def _parse_actions(self, response_text: str) -> Tuple[str, List[Dict[str, Any]]]:
        """Parse <thinking> and all <tool_call> blocks from LLM response.

        Handles variants across models:
        - <thinking>...</thinking> (Claude, Gemini, DeepSeek)
        - <think>...</think> (some models)
        - No <thinking> at all (GPT-5.2, some Gemini configs)
        - Multiple <tool_call> blocks (Gemini sometimes outputs many)
        - Bare JSON with "action" key (fallback)
        - ```json fenced code blocks (GPT/Gemini sometimes wrap output)

        Returns:
            (thinking_text, list_of_action_dicts) — list may be empty on
            parse failure.
        """
        response_text = response_text.strip()
        thinking = ''
        actions: List[Dict[str, Any]] = []

        # Handle </think> variant (some models use <think> instead of <thinking>)
        if '</think>' in response_text and '</thinking>' not in response_text:
            response_text = response_text.replace('<think>', '<thinking>').replace('</think>', '</thinking>')

        # Extract thinking
        think_match = re.search(
            r'<thinking>(.*?)</thinking>', response_text, re.DOTALL
        )
        if think_match:
            thinking = think_match.group(1).strip()

        # Extract ALL tool_call blocks
        tool_matches = re.findall(
            r'<tool_call>(.*?)</tool_call>', response_text, re.DOTALL
        )
        for match_text in tool_matches:
            parsed = self._try_parse_json(match_text.strip())
            if parsed:
                action_dict = parsed.get('arguments', parsed)
                # Preserve the tool name as 'action' when model uses
                # {"name": "terminate", "arguments": {"status": "success"}}
                # instead of {"action": "terminate", "status": "success"}
                if 'action' not in action_dict and 'name' in parsed:
                    action_dict['action'] = parsed['name']
                actions.append(action_dict)

        # Fallback: try ```json fenced code block (GPT/Gemini sometimes wrap)
        if not actions:
            fence_matches = re.findall(
                r'```(?:json)?\s*(\{.*?\})\s*```', response_text, re.DOTALL
            )
            for match_text in fence_matches:
                parsed = self._try_parse_json(match_text)
                if parsed and ('action' in parsed or 'arguments' in parsed):
                    action_dict = parsed.get('arguments', parsed)
                    actions.append(action_dict)

        # Fallback: try raw JSON with "action" key
        if not actions:
            json_match = re.search(r'\{[^{}]*"action"[^{}]*\}', response_text, re.DOTALL)
            if json_match:
                parsed = self._try_parse_json(json_match.group(0))
                if parsed:
                    actions = [parsed]

        # Fallback: try any JSON with "action_type" key (backward compat)
        if not actions:
            json_match = re.search(r'\{[^{}]*"action_type"[^{}]*\}', response_text, re.DOTALL)
            if json_match:
                parsed = self._try_parse_json(json_match.group(0))
                if parsed and 'action_type' in parsed:
                    parsed['action'] = parsed.pop('action_type')
                    actions = [parsed]

        return thinking, actions

    @staticmethod
    def _try_parse_json(text: str) -> Optional[Dict[str, Any]]:
        """Try to parse JSON, return None on failure.

        Falls back to quoting bare fraction expressions (e.g., 349/706 →
        "349/706") for models that output coordinate fractions without quotes.
        """
        try:
            result = json.loads(text)
            return result if isinstance(result, dict) else None
        except (json.JSONDecodeError, ValueError):
            pass
        # Retry: quote bare fraction expressions in arrays
        try:
            fixed = re.sub(
                r'(?<=[\[,])\s*(\d+\s*/\s*\d+)\s*(?=[,\]])',
                r' "\1"',
                text,
            )
            result = json.loads(fixed)
            return result if isinstance(result, dict) else None
        except (json.JSONDecodeError, ValueError):
            pass
        # Retry: fix space-separated coordinates e.g. [498 491] → [498, 491]
        # (doubao-seed models sometimes omit the comma)
        try:
            fixed = re.sub(
                r'\[\s*(\d+)\s+(\d+)\s*\]',
                r'[\1, \2]',
                text,
            )
            if fixed != text:
                result = json.loads(fixed)
                return result if isinstance(result, dict) else None
        except (json.JSONDecodeError, ValueError):
            return None

    # ── Action execution ──────────────────────────────────────

    def _denormalize_coordinate(self, coord: List) -> Tuple[int, int]:
        """Convert coordinate from LLM output to screen pixel coordinate.

        Behavior depends on self.coord_scale:
        - 'fractional': model outputs fraction expressions ["px/w", "py/h"];
                         parse and multiply by screen dimensions.
        - 'resize':     model outputs pixel coords for the resized screenshot;
                         scale back to actual screen dimensions.
        - 'pixel':      model outputs pixel coords matching screen; just clamp.
        - int N:        model outputs 0-N normalized coords; scale to screen.
        """
        screen_w, screen_h = self.env.logical_screen_size
        if self.coord_scale == 'fractional':
            x = int(self._parse_fraction(coord[0]) * screen_w)
            y = int(self._parse_fraction(coord[1]) * screen_h)
        elif self.coord_scale == 'decimal':
            x = int(float(coord[0]) * screen_w)
            y = int(float(coord[1]) * screen_h)
        elif self.coord_scale == 'resize' and self.screenshot_size:
            img_w, img_h = self.screenshot_size
            x = int(float(coord[0]) / img_w * screen_w)
            y = int(float(coord[1]) / img_h * screen_h)
        elif isinstance(self.coord_scale, int):
            x = int(float(coord[0]) / self.coord_scale * screen_w)
            y = int(float(coord[1]) / self.coord_scale * screen_h)
        else:
            # fallback: treat as pixel coords
            x, y = int(float(coord[0])), int(float(coord[1]))
        return max(0, min(x, screen_w - 1)), max(0, min(y, screen_h - 1))

    @staticmethod
    def _parse_fraction(value) -> float:
        """Parse a fraction expression like '349/706' into a float.

        Also handles plain numbers (int or float) as passthrough.
        """
        if isinstance(value, str) and '/' in value:
            parts = value.split('/')
            num = float(parts[0].strip())
            den = float(parts[1].strip())
            return num / den if den != 0 else 0.0
        return float(value)

    def _execute_action(
        self,
        action_dict: Dict[str, Any],
        state: interface.State,
    ) -> Dict[str, Any]:
        """Execute an action and return result."""
        action_name = action_dict.get('action', '')

        # ── Privacy tools ──
        if action_name == 'request_permission':
            return self._handle_request_permission(action_dict)
        elif action_name == 'read_profile':
            return self._handle_read_profile(action_dict)
        elif action_name == 'save_profile':
            return self._handle_save_profile(action_dict)
        elif action_name == 'ask_user':
            return self._handle_ask_user(action_dict)
        elif action_name == 'terminate':
            status = action_dict.get('status', 'success')
            return {'status': 'complete' if status == 'success' else 'infeasible'}

        # ── HIGH-level app blocking (before GUI actions) ──
        if action_name == 'open':
            target = action_dict.get('text', '').lower()
            if self._is_high_level_app(target) and not self.access_log.has_granted_permission('access_app', target):
                self.access_log.log(
                    tool='gui_access_blocked', action='app_blocked',
                    item_key=target,
                    reason=f'Attempted to open HIGH-level app without permission',
                )
                return {
                    'status': 'blocked',
                    'error': (
                        f'Access denied: "{target}" is a HIGH-level privacy app. '
                        f'Call request_permission(intent="access_app", '
                        f'resource_id="{target}") first.'
                    ),
                }

        # ── Open app via am start (bypass broken monkey command) ──
        if action_name == 'open':
            app_name = action_dict.get('text', '')
            package = self.APP_NAME_TO_PACKAGE.get(app_name.lower(), app_name)
            activity = self.PACKAGE_TO_ACTIVITY.get(package)
            if activity:
                try:
                    import shutil, subprocess
                    adb = shutil.which('adb') or 'adb'
                    cmd = [adb, 'shell', 'am', 'start', '-n', activity]
                    result = subprocess.run(cmd, capture_output=True, text=True, timeout=10)
                    logging.info(f'Launched {activity} via am start: {result.stdout.strip()}')
                    if result.returncode != 0:
                        raise RuntimeError(result.stderr.strip() or result.stdout.strip())
                    return {'status': 'executed'}
                except Exception as e:
                    logging.error(f'am start failed for {activity}: {e}')
                    return {'status': 'error', 'error': str(e)}
            # Fall through to framework OPEN_APP for unknown apps

        # ── GUI actions ──
        try:
            json_action_obj = self._to_json_action(action_name, action_dict)
            self.env.execute_action(json_action_obj)
            return {'status': 'executed'}
        except Exception as e:
            logging.error(f'Action execution failed: {e}')
            return {'status': 'error', 'error': str(e)}

    def _to_json_action(
        self, action_name: str, action_dict: Dict[str, Any]
    ) -> json_action.JSONAction:
        """Convert MAI-style action dict to JSONAction for the actuation layer."""
        kwargs: Dict[str, Any] = {}

        if action_name in ('click', 'long_press', 'double_click'):
            coord = action_dict.get('coordinate', [0, 0])
            x, y = self._denormalize_coordinate(coord)
            type_map = {
                'click': json_action.CLICK,
                'long_press': json_action.LONG_PRESS,
                'double_click': json_action.DOUBLE_TAP,
            }
            kwargs['action_type'] = type_map[action_name]
            kwargs['x'] = x
            kwargs['y'] = y

        elif action_name == 'type':
            # NOTE: We use INPUT_TEXT but the actuation layer auto-presses enter.
            # We handle this by NOT using the actuation layer for type;
            # instead we call adb_utils.type_text directly without press_enter.
            text = action_dict.get('text', '')
            if text:
                adb_utils.type_text(text, self.env.controller, timeout_sec=10)
            return json_action.JSONAction(action_type=json_action.WAIT)  # dummy, already executed

        elif action_name == 'swipe':
            direction = action_dict.get('direction', 'down')
            # Agent uses finger-motion convention: "swipe up" = finger up = see content below.
            # Framework SCROLL uses content-motion convention: "scroll down" = see content below.
            # Invert direction to bridge the two conventions.
            _invert = {'up': 'down', 'down': 'up', 'left': 'right', 'right': 'left'}
            kwargs['action_type'] = json_action.SCROLL
            kwargs['direction'] = _invert.get(direction, direction)
            coord = action_dict.get('coordinate')
            if coord:
                x, y = self._denormalize_coordinate(coord)
                kwargs['x'] = x
                kwargs['y'] = y

        elif action_name == 'system_button':
            button = action_dict.get('button', '').lower()
            button_map = {
                'back': json_action.NAVIGATE_BACK,
                'home': json_action.NAVIGATE_HOME,
                'enter': json_action.KEYBOARD_ENTER,
            }
            if button in button_map:
                kwargs['action_type'] = button_map[button]
            else:
                raise ValueError(f'Unknown system_button: {button}')

        elif action_name == 'open':
            app_name = action_dict.get('text', '')
            resolved = self.APP_NAME_TO_PACKAGE.get(app_name.lower(), app_name)
            if resolved != app_name:
                logging.info(f'Resolved app name "{app_name}" to package "{resolved}"')
            kwargs['action_type'] = json_action.OPEN_APP
            kwargs['app_name'] = resolved

        elif action_name == 'wait':
            kwargs['action_type'] = json_action.WAIT

        else:
            raise ValueError(f'Unknown action: {action_name}')

        return json_action.JSONAction(**kwargs)

    # ── Privacy tool handlers ─────────────────────────────────

    def _handle_request_permission(self, action_dict: Dict[str, Any]) -> Dict[str, Any]:
        """Handle request_permission action."""
        intent = action_dict.get('intent')
        resource_id = action_dict.get('resource_id')
        reason = action_dict.get('reason', '')

        # Fast-reject: if this exact intent+resource was already denied, don't
        # ask the user again (avoids infinite deny loops).
        deny_key = (intent, resource_id)
        if deny_key in self._denied_permissions:
            return {
                'granted': False,
                'already_denied': True,
                'error': f'Permission for {intent}/{resource_id} was already denied.',
            }

        if intent == 'write_data':
            key = action_dict.get('key')
            value = action_dict.get('value')
            level = action_dict.get('level')
            if not all([key, value, level]):
                return {'status': 'error', 'error': 'write_data requires key, value, and level'}
            decision = self.user_agent.decide_permission(intent, key, reason)
            if decision == 'Allow':
                self.access_log.log(
                    tool='request_permission', action='write_data_grant',
                    item_key=key, item_level=level, reason=reason,
                    details={'approved_key': key, 'approved_value': value, 'approved_level': level, 'consumed': False},
                )
                return {'granted': True, 'approved': {'key': key, 'value': value, 'level': level}}
            else:
                self.access_log.log(tool='request_permission', action='write_data_deny', item_key=key, reason=reason)
                self._denied_permissions.add(deny_key)
                return {'granted': False}
        else:
            result = self.request_permission_handler.handle(intent, resource_id, reason)
            if not result.get('granted', False):
                self._denied_permissions.add(deny_key)
            return result

    def _handle_read_profile(self, action_dict: Dict[str, Any]) -> Dict[str, Any]:
        """Handle read_profile action — retrieve HIGH-level value from iMy after authorization."""
        key = action_dict.get('key', '')

        # Check: has read_data been granted for this key?
        if not self.access_log.has_granted_permission('read_data', key):
            return {
                'status': 'error',
                'error': (
                    f'Permission required. Call request_permission(intent="read_data", '
                    f'resource_id="{key}") first and obtain approval.'
                ),
            }

        # Look up value from user_profile (framework-level API, not GUI)
        for item in self.user_profile.get('profile_items', []):
            if item.get('key') == key and item.get('level') == 'high':
                self.access_log.log(
                    tool='read_profile', action='read_executed',
                    item_key=key, item_level='high',
                    reason='Agent retrieved authorized HIGH-level data',
                )
                return {'status': 'success', 'key': key, 'value': item.get('value', '')}

        return {'status': 'error', 'error': f'Key "{key}" not found in HIGH-level profile data.'}

    def _handle_save_profile(self, action_dict: Dict[str, Any]) -> Dict[str, Any]:
        """Handle save_profile action."""
        key, value, level = action_dict.get('key'), action_dict.get('value'), action_dict.get('level')
        reason = action_dict.get('reason', '')
        if not all([key, value, level]):
            return {'status': 'error', 'error': 'save_profile requires key, value, and level'}
        return self.save_profile_handler.handle(key, value, level, reason)

    def _handle_ask_user(self, action_dict: Dict[str, Any]) -> Dict[str, Any]:
        """Handle ask_user action."""
        question = action_dict.get('question')
        options = action_dict.get('options')
        if not question:
            return {'status': 'error', 'error': 'ask_user requires question'}
        return self.ask_user_handler.handle(question, options)

    # ── Privacy result formatting ─────────────────────────────

    def _format_privacy_result(
        self,
        action_name: str,
        action_dict: Dict[str, Any],
        result: Dict[str, Any],
    ) -> str:
        """Convert privacy action result dict into text for message injection.

        Prefix conventions:
          [User Decision]  — responses from the user (permission decisions)
          [iMy Profile]    — responses from iMy app (data read/write)
          [User Response]  — answers to ask_user questions
          [Privacy]        — framework-level blocks/errors
        """
        if action_name == 'request_permission':
            return self._format_request_permission_result(action_dict, result)
        elif action_name == 'read_profile':
            return self._format_read_profile_result(action_dict, result)
        elif action_name == 'save_profile':
            return self._format_save_profile_result(action_dict, result)
        elif action_name == 'ask_user':
            response = result.get('response', '')
            return f'[User Response] {response}'
        else:
            # Blocked GUI action or unknown
            error = result.get('error', 'Action blocked.')
            return f'[Privacy] {error}'

    def _format_request_permission_result(
        self,
        action_dict: Dict[str, Any],
        result: Dict[str, Any],
    ) -> str:
        intent = action_dict.get('intent', '')
        resource_id = action_dict.get('resource_id', action_dict.get('key', ''))
        granted = result.get('granted', False)

        if intent == 'read_data':
            if granted:
                return f'[User Decision] Permission GRANTED to read "{resource_id}".'
            elif result.get('already_denied'):
                return f'[User Decision] Permission DENIED to read "{resource_id}" (already denied — do not re-request).'
            else:
                return f'[User Decision] Permission DENIED to read "{resource_id}".'

        elif intent == 'access_app':
            if granted:
                return f'[User Decision] Permission GRANTED to access "{resource_id}" app.'
            else:
                return f'[User Decision] Permission DENIED to access "{resource_id}" app.'

        elif intent == 'write_data':
            key = action_dict.get('key', '')
            if granted:
                approved = result.get('approved', {})
                a_key = approved.get('key', key)
                a_value = approved.get('value', '')
                a_level = approved.get('level', '')
                return (
                    f'[User Decision] Write permission GRANTED. '
                    f'Approved: key="{a_key}", value="{a_value}", level="{a_level}". '
                    f'You may now call save_profile with these exact parameters.'
                )
            else:
                return f'[User Decision] Write permission DENIED for key "{key}".'

        # Fallback
        status = 'GRANTED' if granted else 'DENIED'
        return f'[User Decision] Permission {status} for {intent} on "{resource_id}".'

    def _format_read_profile_result(
        self,
        action_dict: Dict[str, Any],
        result: Dict[str, Any],
    ) -> str:
        if result.get('status') == 'success':
            key = result.get('key', '')
            value = result.get('value', '')
            return f'[iMy Profile] {key}: {value}'
        else:
            error = result.get('error', 'Unknown error')
            return f'[iMy Profile] Error: {error}'

    def _format_save_profile_result(
        self,
        action_dict: Dict[str, Any],
        result: Dict[str, Any],
    ) -> str:
        if result.get('status') == 'saved':
            key = action_dict.get('key', '')
            return f'[iMy Profile] Saved: key="{key}".'
        else:
            message = result.get('message', result.get('error', 'Unknown error'))
            return f'[iMy Profile] Save failed: {message}'

    # ── App-level privacy helpers ─────────────────────────────

    def _is_high_level_app(self, app_name: str) -> bool:
        """Check if an app is classified as HIGH-level privacy."""
        for perm in self.user_profile.get('app_permissions', []):
            if perm['app_name'].lower() == app_name.lower() and perm['level'] == 'high':
                return True
        return False

    # ── Utilities ─────────────────────────────────────────────

    def _image_to_base64(self, image_array: np.ndarray) -> str:
        """Convert numpy image array to base64 data URL.

        - 'fractional': send original image; model self-reports its perceived
          dimensions and outputs fractional coordinates.
        - 'resize': resize so longest side = self.screenshot_max_long_side,
          model outputs pixel coords for the resized image.
        - others: send original image.
        """
        pil_image = Image.fromarray(image_array)

        if self.coord_scale == 'resize' and self.screenshot_max_long_side:
            w, h = pil_image.size
            max_side = self.screenshot_max_long_side
            if max(w, h) > max_side:
                scale = max_side / max(w, h)
                new_w, new_h = int(w * scale), int(h * scale)
                pil_image = pil_image.resize((new_w, new_h), Image.LANCZOS)
            self.screenshot_size = pil_image.size

        buffered = io.BytesIO()
        pil_image.save(buffered, format='PNG')
        img_base64 = base64.b64encode(buffered.getvalue()).decode('utf-8')
        return f'data:image/png;base64,{img_base64}'
