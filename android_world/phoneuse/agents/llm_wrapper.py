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

"""Universal LLM wrapper for PhoneUse Agent.

Supports any OpenAI-compatible endpoint with automatic format adaptation:
  - OpenRouter (openrouter.ai): standard OpenAI format, no conversion needed
  - Direct OpenAI: standard format

Usage:
    Set OPENAI_API_KEY and OPENAI_BASE_URL environment variables.
    wrapper = OpenAIWrapper(model_name='anthropic/claude-opus-4.6')
"""

from __future__ import annotations

import json
import os
import re
import time
import base64
import io
import logging
from typing import Optional, Any
import numpy as np
from PIL import Image
import requests

ERROR_CALLING_LLM = 'Error calling LLM'


class LlmWrapper:
    """Abstract interface for (text only) LLM."""

    def predict(self, text_prompt: str) -> tuple[str, Optional[bool], Any]:
        raise NotImplementedError


class MultimodalLlmWrapper:
    """Abstract interface for Multimodal LLM."""

    def predict_mm(
        self, text_prompt: str, images: list[np.ndarray]
    ) -> tuple[str, Optional[bool], Any]:
        raise NotImplementedError


class OpenAIWrapper(LlmWrapper, MultimodalLlmWrapper):
    """Universal OpenAI-compatible API wrapper.

    Works with any OpenAI-compatible endpoint by setting OPENAI_API_KEY
    and OPENAI_BASE_URL. Automatically handles provider-specific quirks:

    - Claude on non-OpenRouter proxies: image format conversion, top-level
      system parameter, no top_p alongside temperature.
    - OpenRouter: standard OpenAI format for all models (OpenRouter handles
      provider conversion internally).
    - Thinking models (Gemini, etc.): content may be in reasoning_tokens;
      caller should use sufficient max_tokens.

    Tested endpoints:
      - https://openrouter.ai/api/v1
      - https://api.openai.com/v1
      - Any OpenAI-compatible endpoint
    """

    def __init__(
        self,
        model_name: str = 'claude-opus-4-6',
        max_retry: int = 3,
        temperature: float = 0.0,
        max_tokens: int = 4096,
        api_key: str | None = None,
        base_url: str | None = None,
        verbose: bool = True,
    ):
        if api_key is None:
            api_key = os.environ.get('OPENAI_API_KEY')
            if not api_key:
                raise ValueError(
                    'API key required. Set OPENAI_API_KEY environment variable. '
                    'If set in ~/.zshrc, ensure the shell sources it before running.'
                )

        if base_url is None:
            raw_url = os.environ.get('OPENAI_BASE_URL')
            if raw_url:
                base_url = raw_url.rstrip('/')
                # Only append /v1 if no version segment already present
                if not re.search(r'/v\d+$', base_url):
                    base_url += '/v1'
            else:
                base_url = 'https://openrouter.ai/api/v1'

        self.api_key = api_key
        self.base_url = base_url.rstrip('/')
        self.model_name = model_name
        self.temperature = temperature
        self.max_tokens = max_tokens
        self.max_retry = min(max(max_retry, 1), 5)
        self.verbose = verbose

    # ── Provider detection ────────────────────────────────

    @property
    def _is_openrouter(self) -> bool:
        """OpenRouter handles all format conversions internally."""
        return 'openrouter' in self.base_url

    @property
    def _is_claude(self) -> bool:
        return 'claude' in self.model_name.lower()

    @property
    def _needs_claude_compat(self) -> bool:
        """Claude on non-OpenRouter proxies needs special handling."""
        return self._is_claude and not self._is_openrouter

    @property
    def _is_kimi(self) -> bool:
        return 'kimi' in self.model_name.lower()

    # ── Public API ────────────────────────────────────────

    def predict(self, text_prompt: str) -> tuple[str, Optional[bool], Any]:
        return self.predict_mm(text_prompt, [])

    def predict_mm(
        self, text_prompt: str, images: list[np.ndarray]
    ) -> tuple[str, Optional[bool], Any]:
        """Convenience method for single-turn text + images."""
        content: list[dict] = [{'type': 'text', 'text': text_prompt}]
        for image in images:
            pil_image = Image.fromarray(image)
            buffered = io.BytesIO()
            pil_image.save(buffered, format='PNG')
            img_b64 = base64.b64encode(buffered.getvalue()).decode('utf-8')
            content.append({
                'type': 'image_url',
                'image_url': {'url': f'data:image/png;base64,{img_b64}'},
            })
        return self.predict_messages([{'role': 'user', 'content': content}])

    def predict_messages(
        self, messages: list[dict[str, Any]]
    ) -> tuple[str, Optional[bool], Any]:
        """Call LLM with multi-turn messages.

        Messages use the standard OpenAI format internally:
        - System: {'role': 'system', 'content': '...'}
        - User text: {'role': 'user', 'content': '...'} or list of items
        - User image: {'type': 'image_url', 'image_url': {'url': 'data:...'}}
        - Assistant: {'role': 'assistant', 'content': '...'}

        Format conversions are applied automatically based on model/endpoint.
        """
        counter = self.max_retry
        retry_delay = 1.0

        headers = {
            'Authorization': f'Bearer {self.api_key}',
            'Content-Type': 'application/json',
        }
        # OpenRouter optional headers
        if self._is_openrouter:
            headers['HTTP-Referer'] = 'https://github.com/myphonebench'
            headers['X-Title'] = 'MyPhoneBench'

        while counter > 0:
            try:
                payload = self._build_payload(messages)
                if self.verbose:
                    self._log_request(payload)

                response = requests.post(
                    f'{self.base_url}/chat/completions',
                    headers=headers,
                    json=payload,
                    timeout=300,
                )

                if response.status_code == 200:
                    result = response.json()
                    if self.verbose:
                        self._log_response(result)

                    text = self._extract_content(result)
                    if not text:
                        reasoning = result.get('usage', {}).get(
                            'completion_tokens_details', {}
                        ).get('reasoning_tokens', 0)
                        if reasoning > 0:
                            logging.warning(
                                f'Empty content with {reasoning} reasoning_tokens. '
                                f'Increase max_tokens (currently {self.max_tokens}) '
                                f'for thinking models.'
                            )
                    return text, None, result

                # Handle errors
                error_msg = f'HTTP {response.status_code}: {response.text[:300]}'
                if response.status_code == 429 and counter > 1:
                    time.sleep(retry_delay * 2)
                    retry_delay *= 2
                    counter -= 1
                    continue
                raise requests.HTTPError(error_msg)

            except Exception as e:
                counter -= 1
                logging.warning(
                    f'LLM call failed (retries left: {counter}): {e}'
                )
                if counter > 0:
                    time.sleep(retry_delay)
                    retry_delay *= 2
                else:
                    return ERROR_CALLING_LLM, None, None

        return ERROR_CALLING_LLM, None, None

    # ── Payload building ──────────────────────────────────

    def _build_payload(self, messages: list[dict]) -> dict:
        """Build the API request payload with all necessary format conversions."""
        api_messages = []
        system_parts = []

        for msg in messages:
            role = msg.get('role')
            content = msg.get('content')

            if role == 'system':
                system_parts.append(
                    content if isinstance(content, str) else str(content)
                )
            elif role == 'user':
                if isinstance(content, list):
                    api_content = []
                    for item in content:
                        if item.get('type') == 'text':
                            api_content.append({
                                'type': 'text',
                                'text': item.get('text', ''),
                            })
                        elif item.get('type') == 'image_url':
                            api_content.append({
                                'type': 'image_url',
                                'image_url': item.get('image_url', {}),
                            })
                    api_messages.append({'role': 'user', 'content': api_content})
                else:
                    api_messages.append({
                        'role': 'user',
                        'content': content if isinstance(content, str) else str(content),
                    })
            elif role == 'assistant':
                api_messages.append({
                    'role': 'assistant',
                    'content': content if isinstance(content, str) else str(content),
                })

        # ── Merge system prompt into the first user message ──
        # Avoids proxy compatibility issues: some proxies silently drop
        # top-level `system` or `role: 'system'` messages. Putting the
        # system prompt as text in the first user message is universal.
        if system_parts and api_messages:
            system_text = '\n\n'.join(system_parts)
            first = api_messages[0]
            if first.get('role') == 'user':
                content = first.get('content')
                if isinstance(content, list):
                    # Prepend system prompt as first text item
                    api_messages[0] = {
                        'role': 'user',
                        'content': [{'type': 'text', 'text': system_text}] + content,
                    }
                elif isinstance(content, str):
                    api_messages[0] = {
                        'role': 'user',
                        'content': system_text + '\n\n' + content,
                    }
            else:
                # First message isn't user — insert a user message at the start
                api_messages.insert(0, {'role': 'user', 'content': system_text})

        # ── Claude-specific conversions (non-OpenRouter proxies only) ──
        if self._needs_claude_compat:
            api_messages = self._convert_images_for_claude(api_messages)

        # ── Ensure no empty content (some proxies reject empty messages) ──
        api_messages = self._ensure_non_empty_content(api_messages)

        payload: dict[str, Any] = {
            'model': self.model_name,
            'messages': api_messages,
            'max_tokens': self.max_tokens,
            'temperature': self.temperature,
        }

        # Kimi: only temperature=1 is accepted on some proxies;
        # disable thinking mode for faster responses (thinking is on by default)
        if self._is_kimi:
            payload['temperature'] = 1
            payload['thinking'] = {'type': 'disabled'}

        # Claude: don't send top_p alongside temperature
        # Non-Claude: top_p is fine
        if not self._is_claude:
            payload['top_p'] = 0.95

        return payload

    def _convert_images_for_claude(self, messages: list[dict]) -> list[dict]:
        """Convert OpenAI image_url → Claude image+source format.

        Only needed for Claude on non-OpenRouter proxies.
        OpenRouter handles this conversion internally.
        """
        converted = []
        for msg in messages:
            content = msg.get('content')
            if msg.get('role') == 'user' and isinstance(content, list):
                new_content = []
                for item in content:
                    if item.get('type') == 'image_url':
                        url = item.get('image_url', {}).get('url', '')
                        if url.startswith('data:'):
                            header, raw_b64 = url.split(',', 1)
                            media_type = header.split(':')[1].split(';')[0]
                            new_content.append({
                                'type': 'image',
                                'source': {
                                    'type': 'base64',
                                    'media_type': media_type,
                                    'data': raw_b64,
                                },
                            })
                        else:
                            new_content.append(item)
                    else:
                        new_content.append(item)
                converted.append({'role': msg['role'], 'content': new_content})
            else:
                converted.append(msg)
        return converted

    @staticmethod
    def _ensure_non_empty_content(messages: list[dict]) -> list[dict]:
        """Ensure every message has non-empty content.

        Some proxies reject messages with empty
        content strings or content lists that contain only images without
        text. This adds a minimal text placeholder where needed.
        """
        result = []
        for msg in messages:
            content = msg.get('content')
            if content is None or content == '' or content == []:
                # Skip completely empty messages
                continue
            if isinstance(content, list):
                has_text = any(
                    item.get('type') == 'text' and item.get('text', '').strip()
                    for item in content
                )
                if not has_text:
                    # Image-only message: prepend a minimal text item
                    content = [{'type': 'text', 'text': '[screenshot]'}] + content
                    msg = {**msg, 'content': content}
            result.append(msg)
        return result

    # ── Response extraction ───────────────────────────────

    def _extract_content(self, result: dict) -> str:
        """Extract text content from API response.

        Checks `content` first. If empty, falls back to `reasoning_content`
        (DeepSeek) or `reasoning` (OpenRouter/Gemini) in case the model put
        our <thinking>/<tool_call> tags there instead.
        """
        choices = result.get('choices', [])
        if not choices:
            return ''

        msg = choices[0].get('message', {})
        content = msg.get('content', '') or ''

        # If content is non-empty, use it (normal case for all tested models)
        if content.strip():
            return content

        # Fallback: check reasoning_content (DeepSeek-R1 style)
        reasoning = msg.get('reasoning_content', '') or ''
        if reasoning.strip() and ('<tool_call>' in reasoning or '"action"' in reasoning):
            return reasoning

        # Fallback: check reasoning (OpenRouter/Gemini style)
        reasoning2 = msg.get('reasoning', '') or ''
        if reasoning2.strip() and ('<tool_call>' in reasoning2 or '"action"' in reasoning2):
            return reasoning2

        return content  # return empty content as-is

    @staticmethod
    def extract_reasoning(raw_response: dict) -> str:
        """Extract model's internal reasoning/thinking (separate from our <thinking> tag).

        DeepSeek-R1: reasoning_content field
        OpenRouter/Gemini: reasoning field
        """
        if not raw_response:
            return ''
        msg = raw_response.get('choices', [{}])[0].get('message', {})
        return (
            msg.get('reasoning_content', '')
            or msg.get('reasoning', '')
            or ''
        )

    @staticmethod
    def extract_token_usage(raw_response: dict) -> dict:
        """Extract unified token usage from any endpoint's response.

        Different proxies put token details in different fields:
          - Some use usage.input_tokens_details for image_tokens
          - Some use usage.prompt_tokens_details
          - OpenRouter: cost info in usage.cost_details

        Returns a flat dict with best-effort values:
          prompt_tokens, completion_tokens, total_tokens,
          image_tokens (0 if not reported), text_tokens, reasoning_tokens, cost
        """
        usage = raw_response.get('usage', {}) if raw_response else {}

        # Try input_tokens_details first, then prompt_tokens_details
        input_details = usage.get('input_tokens_details') or {}
        prompt_details = usage.get('prompt_tokens_details') or {}
        completion_details = usage.get('completion_tokens_details') or {}

        image_tokens = (
            input_details.get('image_tokens')
            or prompt_details.get('image_tokens')
            or 0
        )
        text_tokens = (
            input_details.get('text_tokens')
            or prompt_details.get('text_tokens')
            or 0
        )

        return {
            'prompt_tokens': usage.get('prompt_tokens', 0),
            'completion_tokens': usage.get('completion_tokens', 0),
            'total_tokens': usage.get('total_tokens', 0),
            'image_tokens': image_tokens,
            'text_tokens': text_tokens,
            'reasoning_tokens': completion_details.get('reasoning_tokens', 0),
            'cached_tokens': (
                input_details.get('cached_tokens')
                or prompt_details.get('cached_tokens')
                or 0
            ),
            'cost': usage.get('cost'),  # OpenRouter only
        }

    # ── Logging ───────────────────────────────────────────

    def _log_request(self, payload: dict) -> None:
        print(f'\n{"="*80}')
        print(f'LLM REQUEST: model={payload["model"]} '
              f'max_tokens={payload["max_tokens"]} temp={payload["temperature"]}')
        print(f'Endpoint: {self.base_url}')
        msgs = payload.get('messages', [])
        print(f'Messages: {len(msgs)}', end='')
        if 'system' in payload:
            print(f' + system ({len(payload["system"])} chars)', end='')
        print()
        for i, msg in enumerate(msgs):
            role = msg.get('role', '?')
            content = msg.get('content', '')
            if isinstance(content, list):
                types = [it.get('type', '?') for it in content]
                print(f'  [{i}] {role}: {types}')
            else:
                print(f'  [{i}] {role}: {str(content)[:120]}{"..." if len(str(content)) > 120 else ""}')
        print(f'{"="*80}\n')

    def _log_response(self, result: dict) -> None:
        text = self._extract_content(result)
        token_usage = self.extract_token_usage(result)
        print(f'\n{"="*80}')
        print(f'LLM RESPONSE: {len(text)} chars')
        print(f'Usage: prompt={token_usage["prompt_tokens"]} '
              f'completion={token_usage["completion_tokens"]} '
              f'total={token_usage["total_tokens"]}')
        if token_usage['image_tokens']:
            print(f'  image_tokens={token_usage["image_tokens"]} '
                  f'text_tokens={token_usage["text_tokens"]}')
        if token_usage['reasoning_tokens']:
            print(f'  reasoning_tokens={token_usage["reasoning_tokens"]}')
        if text:
            print(f'Content: {text[:200]}{"..." if len(text) > 200 else ""}')
        else:
            print(f'Content: (empty) reasoning_tokens={token_usage["reasoning_tokens"]}')
        print(f'{"="*80}\n')


class CUAWrapper(LlmWrapper, MultimodalLlmWrapper):
    """GPT-5.4 Computer Use Agent wrapper using Responses API.

    Uses the ``/v1/responses`` endpoint with ``{"type": "computer"}`` and
    function tools (privacy actions).  The model returns structured
    ``computer_call`` / ``function_call`` output items which are converted
    to the ``<thinking>...<tool_call>...</tool_call>`` text format expected
    by ``PhoneUseAgent._parse_actions``.

    Because some endpoints do **not** support ``previous_response_id``, each
    request sends only the task instruction + the latest screenshot + any
    pending tool results.
    """

    # Privacy / control function tools exposed to CUA
    _FUNCTION_TOOLS = [
        {
            'type': 'function',
            'name': 'request_permission',
            'description': (
                'Request user permission to access private data or a high-level app. '
                'Returns grant/deny status only — does NOT return the data itself.'
            ),
            'parameters': {
                'type': 'object',
                'properties': {
                    'intent': {
                        'type': 'string',
                        'enum': ['read_data', 'access_app', 'write_data'],
                    },
                    'resource_id': {'type': 'string'},
                    'reason': {'type': 'string'},
                    'key': {'type': 'string'},
                    'value': {'type': 'string'},
                    'level': {'type': 'string', 'enum': ['low', 'high']},
                },
                'required': ['intent', 'resource_id', 'reason'],
            },
        },
        {
            'type': 'function',
            'name': 'read_profile',
            'description': 'Read a profile value from the iMy app after permission is granted.',
            'parameters': {
                'type': 'object',
                'properties': {'key': {'type': 'string'}},
                'required': ['key'],
            },
        },
        {
            'type': 'function',
            'name': 'save_profile',
            'description': 'Save a value to the user\'s iMy profile.',
            'parameters': {
                'type': 'object',
                'properties': {
                    'key': {'type': 'string'},
                    'value': {'type': 'string'},
                    'level': {'type': 'string', 'enum': ['low', 'high']},
                    'reason': {'type': 'string'},
                },
                'required': ['key', 'value', 'level'],
            },
        },
        {
            'type': 'function',
            'name': 'ask_user',
            'description': 'Ask the user a question and wait for an answer.',
            'parameters': {
                'type': 'object',
                'properties': {
                    'question': {'type': 'string'},
                    'options': {
                        'type': 'array',
                        'items': {'type': 'string'},
                    },
                },
                'required': ['question'],
            },
        },
        {
            'type': 'function',
            'name': 'terminate',
            'description': 'Signal that the task is complete or infeasible.',
            'parameters': {
                'type': 'object',
                'properties': {
                    'status': {
                        'type': 'string',
                        'enum': ['success', 'fail'],
                    },
                },
                'required': ['status'],
            },
        },
        {
            'type': 'function',
            'name': 'open_app',
            'description': 'Open an installed Android app by name.',
            'parameters': {
                'type': 'object',
                'properties': {'app_name': {'type': 'string'}},
                'required': ['app_name'],
            },
        },
    ]

    def __init__(
        self,
        model_name: str = 'gpt-5.4',
        max_retry: int = 3,
        temperature: float = 0.0,
        max_tokens: int = 4096,
        api_key: str | None = None,
        base_url: str | None = None,
        verbose: bool = True,
    ):
        if api_key is None:
            api_key = os.environ.get('OPENAI_API_KEY')
            if not api_key:
                raise ValueError('API key required. Set OPENAI_API_KEY.')
        if base_url is None:
            raw_url = os.environ.get('OPENAI_BASE_URL')
            if raw_url:
                base_url = raw_url.rstrip('/')
                # Only append /v1 if no version segment already present
                if not re.search(r'/v\d+$', base_url):
                    base_url += '/v1'
            else:
                base_url = 'https://api.openai.com/v1'
        self.api_key = api_key
        self.base_url = base_url.rstrip('/')
        self.model_name = model_name
        self.temperature = temperature
        self.max_tokens = max_tokens
        self.max_retry = min(max(max_retry, 1), 5)
        self.verbose = verbose
        # Accumulate pending tool results to send in next request
        self._pending_tool_results: list[dict] = []

    # ── Public API ────────────────────────────────────────

    def predict(self, text_prompt: str) -> tuple[str, Optional[bool], Any]:
        return self.predict_messages([{'role': 'user', 'content': text_prompt}])

    def predict_messages(
        self, messages: list[dict[str, Any]]
    ) -> tuple[str, Optional[bool], Any]:
        """Convert PhoneUse messages → Responses API call → text output.

        The returned text follows the ``<thinking>...<tool_call>...</tool_call>``
        format so that ``PhoneUseAgent._parse_actions`` can handle it unchanged.
        """
        cua_input = self._build_cua_input(messages)
        tools = [{'type': 'computer'}] + self._FUNCTION_TOOLS

        payload = {
            'model': self.model_name,
            'input': cua_input,
            'tools': tools,
        }

        headers = {
            'Authorization': f'Bearer {self.api_key}',
            'Content-Type': 'application/json',
        }

        counter = self.max_retry
        retry_delay = 1.0

        while counter > 0:
            try:
                if self.verbose:
                    self._log_cua_request(payload)

                response = requests.post(
                    f'{self.base_url}/responses',
                    headers=headers,
                    json=payload,
                    timeout=300,
                )

                if response.status_code == 200:
                    result = response.json()
                    if self.verbose:
                        self._log_cua_response(result)
                    text = self._convert_cua_output(result)
                    return text, None, result

                error_msg = f'HTTP {response.status_code}: {response.text[:500]}'
                if response.status_code == 429 and counter > 1:
                    time.sleep(retry_delay * 2)
                    retry_delay *= 2
                    counter -= 1
                    continue
                raise requests.HTTPError(error_msg)

            except Exception as e:
                counter -= 1
                logging.warning(f'CUA call failed (retries left: {counter}): {e}')
                if counter > 0:
                    time.sleep(retry_delay)
                    retry_delay *= 2
                else:
                    return ERROR_CALLING_LLM, None, None

        return ERROR_CALLING_LLM, None, None

    # ── Input building ────────────────────────────────────

    def _build_cua_input(self, messages: list[dict]) -> list[dict]:
        """Build Responses API ``input`` from PhoneUse multi-turn messages.

        Since ``previous_response_id`` is not available, we send:
        1. The task instruction (from first user message text)
        2. Any privacy tool result context from recent history
        3. The latest screenshot as an image input
        """
        input_items: list[dict] = []

        task_instruction = ''
        latest_screenshot_url = ''
        tool_context_parts: list[str] = []

        for msg in messages:
            role = msg.get('role')
            content = msg.get('content')

            if role == 'user':
                if isinstance(content, str):
                    # Text-only user message — either system+task or tool result
                    if not task_instruction:
                        task_instruction = content
                    else:
                        tool_context_parts.append(content)
                elif isinstance(content, list):
                    for item in content:
                        if item.get('type') == 'image_url':
                            # Always take the latest screenshot
                            latest_screenshot_url = item['image_url']['url']
                        elif item.get('type') == 'text':
                            text = item.get('text', '').strip()
                            if text:
                                tool_context_parts.append(text)

        # Build the text instruction with tool context
        full_text = task_instruction
        if tool_context_parts:
            full_text += '\n\n## Recent Tool Results\n' + '\n'.join(tool_context_parts)

        # Add pending tool results from previous function calls
        if self._pending_tool_results:
            results_text = '\n'.join(
                f'[{r["name"]}] {r["output"]}' for r in self._pending_tool_results
            )
            full_text += '\n\n## Pending Function Results\n' + results_text
            self._pending_tool_results.clear()

        input_items.append({
            'role': 'user',
            'content': [{'type': 'input_text', 'text': full_text}],
        })

        # Add latest screenshot
        if latest_screenshot_url:
            # Extract base64 data from data URL
            if latest_screenshot_url.startswith('data:'):
                input_items.append({
                    'role': 'user',
                    'content': [{
                        'type': 'input_image',
                        'image_url': latest_screenshot_url,
                    }],
                })
            else:
                input_items.append({
                    'role': 'user',
                    'content': [{
                        'type': 'input_image',
                        'image_url': latest_screenshot_url,
                    }],
                })

        return input_items

    # ── Output conversion ─────────────────────────────────

    def _convert_cua_output(self, result: dict) -> str:
        """Convert Responses API output items to <thinking>/<tool_call> text.

        Processes all output items in order:
        - reasoning → thinking text
        - message → commentary
        - computer_call → GUI action tool_call
        - function_call → privacy/control action tool_call
        """
        output_items = result.get('output', [])
        thinking_parts: list[str] = []
        tool_calls: list[str] = []

        for item in output_items:
            item_type = item.get('type', '')

            if item_type == 'reasoning':
                # Extract reasoning summary
                summary_parts = []
                for s in item.get('summary', []):
                    if s.get('type') == 'summary_text':
                        summary_parts.append(s.get('text', ''))
                if summary_parts:
                    thinking_parts.append('\n'.join(summary_parts))

            elif item_type == 'message':
                # Text message from the model — proxy embeds <thinking> and
                # <tool_call> tags in the message text.  We extract thinking
                # here; actions are extracted in the deduplication pass below.
                content = item.get('content', [])
                for c in content:
                    if c.get('type') in ('output_text', 'text'):
                        text = c.get('text', '').strip()
                        if not text:
                            continue
                        # Extract <thinking> content
                        think_match = re.search(
                            r'<thinking>(.*?)</thinking>', text, re.DOTALL
                        )
                        if think_match:
                            thinking_parts.append(think_match.group(1).strip())
                        elif '<tool_call>' not in text and '"action"' not in text:
                            # Pure commentary text (no action content)
                            coord_match = re.match(
                                r'^(\d+)\s*,\s*(\d+)$', text
                            )
                            if not coord_match:
                                thinking_parts.append(text)

            elif item_type == 'computer_call':
                action = item.get('action', {})
                tc = self._convert_computer_action(action)
                if tc:
                    tool_calls.append(tc)

            elif item_type == 'function_call':
                # Handled in deduplication pass below
                pass

        # The proxy embeds <thinking> + <tool_call> tags in message text AND
        # may also return separate function_call items.  The message text is
        # the model's primary intent; function_call items are often duplicates
        # or proxy artifacts.  Strategy:
        # - If message text had actions, use those; ignore function_call GUI
        #   actions but keep privacy function_calls
        # - If message text had NO actions, use function_call items
        msg_actions_list: list[str] = []
        fc_actions_list: list[str] = []
        for item in output_items:
            item_type = item.get('type', '')
            if item_type == 'message':
                content = item.get('content', [])
                for c in content:
                    if c.get('type') in ('output_text', 'text'):
                        text = c.get('text', '').strip()
                        if '<tool_call>' in text:
                            tc_matches = re.findall(
                                r'<tool_call>(.*?)</tool_call>', text, re.DOTALL
                            )
                            for tc_text in tc_matches:
                                try:
                                    parsed = json.loads(tc_text.strip())
                                    msg_actions_list.append(json.dumps(parsed))
                                except json.JSONDecodeError:
                                    pass
                        elif '"action"' in text:
                            json_match = re.search(
                                r'\{[^{}]*"action"[^{}]*\}', text
                            )
                            if json_match:
                                try:
                                    parsed = json.loads(json_match.group(0))
                                    msg_actions_list.append(json.dumps(parsed))
                                except json.JSONDecodeError:
                                    pass
                        else:
                            coord_match = re.match(r'^(\d+)\s*,\s*(\d+)$', text)
                            if coord_match:
                                x = int(coord_match.group(1))
                                y = int(coord_match.group(2))
                                msg_actions_list.append(json.dumps(
                                    {'action': 'click', 'coordinate': [x, y]}
                                ))
            elif item_type == 'function_call':
                tc = self._convert_function_call(item)
                if tc:
                    fc_actions_list.append(tc)
            elif item_type == 'computer_call':
                action = item.get('action', {})
                tc = self._convert_computer_action(action)
                if tc:
                    fc_actions_list.append(tc)

        # Decide which actions to use:
        if msg_actions_list:
            # Message text had actions — use those as primary
            # Only add function_call actions that are privacy tools (not GUI)
            privacy_fc = {'request_permission', 'read_profile', 'save_profile',
                          'ask_user'}
            final_actions = list(msg_actions_list)
            for fc in fc_actions_list:
                try:
                    parsed = json.loads(fc)
                    if parsed.get('action') in privacy_fc:
                        final_actions.append(fc)
                except (json.JSONDecodeError, TypeError):
                    pass
            tool_calls = final_actions
        else:
            # No actions in message text — use function_call items
            tool_calls = fc_actions_list if fc_actions_list else tool_calls

        # Build final text
        parts: list[str] = []
        if thinking_parts:
            parts.append(f'<thinking>\n{chr(10).join(thinking_parts)}\n</thinking>')
        else:
            parts.append('<thinking>\nCUA action step.\n</thinking>')

        for tc in tool_calls:
            parts.append(f'<tool_call>\n{tc}\n</tool_call>')

        # If no tool calls found, check for pending_safety_review or similar
        if not tool_calls:
            parts.append('<tool_call>\n{"action": "wait"}\n</tool_call>')

        return '\n'.join(parts)

    def _convert_computer_action(self, action: dict) -> str | None:
        """Convert a single CUA computer action to PhoneUse JSON string."""
        action_type = action.get('type', '')

        if action_type == 'click':
            x, y = action.get('x', 0), action.get('y', 0)
            return json.dumps({'action': 'click', 'coordinate': [x, y]})

        elif action_type == 'double_click':
            x, y = action.get('x', 0), action.get('y', 0)
            return json.dumps({'action': 'double_click', 'coordinate': [x, y]})

        elif action_type == 'type':
            text = action.get('text', '')
            return json.dumps({'action': 'type', 'text': text})

        elif action_type == 'scroll':
            x, y = action.get('x', 0), action.get('y', 0)
            scroll_x = action.get('scroll_x', 0)
            scroll_y = action.get('scroll_y', 0)
            # Negative scroll_y = scroll down (content moves up)
            if abs(scroll_y) >= abs(scroll_x):
                direction = 'down' if scroll_y < 0 else 'up'
            else:
                direction = 'right' if scroll_x < 0 else 'left'
            return json.dumps({
                'action': 'swipe', 'direction': direction, 'coordinate': [x, y],
            })

        elif action_type == 'keypress':
            keys = action.get('keys', [])
            if 'Enter' in keys or 'Return' in keys:
                return json.dumps({'action': 'system_button', 'button': 'enter'})
            elif 'Escape' in keys:
                return json.dumps({'action': 'system_button', 'button': 'back'})
            elif 'Backspace' in keys:
                return json.dumps({'action': 'system_button', 'button': 'back'})
            elif 'Home' in keys:
                return json.dumps({'action': 'system_button', 'button': 'home'})
            else:
                # Other key combos — try typing as text
                return json.dumps({'action': 'type', 'text': '+'.join(keys)})

        elif action_type == 'screenshot':
            # CUA requests a new screenshot — skip, next step auto-sends one
            return None

        elif action_type == 'wait':
            return json.dumps({'action': 'wait'})

        elif action_type == 'drag':
            start_x = action.get('start_x', action.get('x', 0))
            start_y = action.get('start_y', action.get('y', 0))
            end_x = action.get('end_x', 0)
            end_y = action.get('end_y', 0)
            path = action.get('path', [])
            if path and len(path) >= 2:
                start_x, start_y = path[0]
                end_x, end_y = path[-1]
            return json.dumps({
                'action': 'drag',
                'start_coordinate': [start_x, start_y],
                'end_coordinate': [end_x, end_y],
            })

        else:
            logging.warning(f'Unknown CUA action type: {action_type}')
            return None

    def _convert_function_call(self, item: dict) -> str | None:
        """Convert a CUA function_call output to PhoneUse JSON string."""
        name = item.get('name', '')
        args_str = item.get('arguments', '{}')
        try:
            args = json.loads(args_str) if isinstance(args_str, str) else args_str
        except json.JSONDecodeError:
            args = {}

        if name == 'open_app':
            return json.dumps({'action': 'open', 'text': args.get('app_name', '')})

        elif name == 'terminate':
            return json.dumps({'action': 'terminate', 'status': args.get('status', 'fail')})

        elif name in ('request_permission', 'read_profile', 'save_profile', 'ask_user'):
            action_dict = {'action': name}
            action_dict.update(args)
            return json.dumps(action_dict)

        else:
            logging.warning(f'Unknown CUA function call: {name}')
            return None

    def add_tool_result(self, name: str, output: str) -> None:
        """Queue a function call result to include in the next request."""
        self._pending_tool_results.append({'name': name, 'output': output})

    # ── Token usage (compatible with OpenAIWrapper) ───────

    @staticmethod
    def extract_token_usage(raw_response: dict) -> dict:
        usage = raw_response.get('usage', {}) if raw_response else {}
        return {
            'prompt_tokens': usage.get('input_tokens', 0),
            'completion_tokens': usage.get('output_tokens', 0),
            'total_tokens': usage.get('total_tokens', 0),
            'image_tokens': 0,
            'text_tokens': 0,
            'reasoning_tokens': 0,
            'cached_tokens': 0,
            'cost': None,
        }

    @staticmethod
    def extract_reasoning(raw_response: dict) -> str:
        if not raw_response:
            return ''
        for item in raw_response.get('output', []):
            if item.get('type') == 'reasoning':
                parts = []
                for s in item.get('summary', []):
                    if s.get('type') == 'summary_text':
                        parts.append(s.get('text', ''))
                if parts:
                    return '\n'.join(parts)
        return ''

    # ── Logging ───────────────────────────────────────────

    def _log_cua_request(self, payload: dict) -> None:
        print(f'\n{"="*80}')
        print(f'CUA REQUEST: model={payload["model"]}')
        print(f'Endpoint: {self.base_url}/responses')
        input_items = payload.get('input', [])
        print(f'Input items: {len(input_items)}')
        tools = payload.get('tools', [])
        tool_types = [t.get('type', t.get('name', '?')) for t in tools]
        print(f'Tools: {tool_types}')
        for i, item in enumerate(input_items):
            role = item.get('role', '?')
            content = item.get('content', [])
            if isinstance(content, list):
                types = [c.get('type', '?') for c in content]
                print(f'  [{i}] {role}: {types}')
            else:
                print(f'  [{i}] {role}: {str(content)[:120]}')
        print(f'{"="*80}\n')

    def _log_cua_response(self, result: dict) -> None:
        output_items = result.get('output', [])
        usage = result.get('usage', {})
        print(f'\n{"="*80}')
        print(f'CUA RESPONSE: {len(output_items)} output items')
        print(f'Usage: input={usage.get("input_tokens", 0)} '
              f'output={usage.get("output_tokens", 0)}')
        for i, item in enumerate(output_items):
            itype = item.get('type', '?')
            if itype == 'computer_call':
                action = item.get('action', {})
                print(f'  [{i}] {itype}: {action.get("type")} '
                      f'({json.dumps(action)[:120]})')
            elif itype == 'function_call':
                print(f'  [{i}] {itype}: {item.get("name")} '
                      f'({item.get("arguments", "")[:120]})')
            elif itype == 'reasoning':
                summaries = item.get('summary', [])
                text = ' '.join(s.get('text', '') for s in summaries)[:120]
                print(f'  [{i}] {itype}: {text}')
            elif itype == 'message':
                content = item.get('content', [])
                text = ' '.join(c.get('text', '') for c in content)[:120]
                print(f'  [{i}] {itype}: {text}')
            else:
                print(f'  [{i}] {itype}')
        print(f'{"="*80}\n')

