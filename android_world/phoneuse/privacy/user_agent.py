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

"""User Agent abstraction for privacy decision-making.

Provides structured "Allow"/"Deny" decisions for permission requests
and answers for ask_user questions, with both deterministic and LLM-based modes.
"""

import logging
from abc import ABC, abstractmethod
from typing import Dict, List, Optional, Any

logger = logging.getLogger(__name__)


class UserAgent(ABC):
    """Abstract base class for user agents that make privacy decisions."""

    @abstractmethod
    def decide_permission(
        self,
        intent: str,
        resource_id: str,
        reason: str,
        context: Optional[Dict[str, Any]] = None,
    ) -> str:
        """Decide whether to allow or deny a permission request.

        Args:
            intent: Permission intent ('read_data', 'access_app', 'write_data')
            resource_id: Resource identifier (key or app name)
            reason: Reason for the request
            context: Optional context dict (e.g., {'goal': '...'})

        Returns:
            'Allow' or 'Deny'
        """

    @abstractmethod
    def answer_question(
        self,
        question: str,
        options: Optional[List[str]] = None,
        context: Optional[Dict[str, Any]] = None,
    ) -> str:
        """Answer an ask_user question.

        Args:
            question: Question to answer
            options: Optional list of choices
            context: Optional context dict

        Returns:
            The answer string
        """


class DeterministicUserAgent(UserAgent):
    """Uses pre-defined user_decisions dict from task JSON."""

    def __init__(self, user_decisions: Optional[Dict[str, Any]] = None):
        """Initialize with user decisions.

        Args:
            user_decisions: Pre-defined decisions dict from task JSON.
                Format: {
                    'read_data': {'phone_number': 'grant', 'default': 'deny'},
                    'access_app': {'mzocdoc': 'grant', 'default': 'deny'},
                    'write_data': {'default': 'grant'},
                    'ask_user': {'Which doctor?': 'Dr. Smith'},
                }
        """
        self.user_decisions = user_decisions or {}

    def decide_permission(
        self,
        intent: str,
        resource_id: str,
        reason: str,
        context: Optional[Dict[str, Any]] = None,
    ) -> str:
        decisions = self.user_decisions.get(intent, {})

        # Check specific resource_id
        if resource_id in decisions:
            raw = decisions[resource_id]
        elif 'default' in decisions:
            raw = decisions['default']
        else:
            raw = 'deny'

        # Map 'grant' -> 'Allow', 'deny' -> 'Deny'
        if raw == 'grant':
            return 'Allow'
        return 'Deny'

    def answer_question(
        self,
        question: str,
        options: Optional[List[str]] = None,
        context: Optional[Dict[str, Any]] = None,
    ) -> str:
        ask_user_decisions = self.user_decisions.get('ask_user', {})

        if question in ask_user_decisions:
            return ask_user_decisions[question]

        # Fallback: first option or 'Yes'
        if options and len(options) > 0:
            return options[0]
        return 'Yes'


class LLMUserAgent(UserAgent):
    """Uses an LLM to simulate a user with a persona/script from task definition."""

    def __init__(self, llm_wrapper, user_persona: Optional[Dict[str, Any]] = None):
        """Initialize with LLM wrapper and persona.

        Args:
            llm_wrapper: LLM wrapper instance (e.g., OpenAIWrapper)
            user_persona: Persona dict from task definition, e.g.:
                {'name': 'John', 'description': 'A cautious user who ...'}
        """
        self.llm_wrapper = llm_wrapper
        self.user_persona = user_persona or {}

    def decide_permission(
        self,
        intent: str,
        resource_id: str,
        reason: str,
        context: Optional[Dict[str, Any]] = None,
    ) -> str:
        persona_desc = self.user_persona.get(
            'description', 'a typical smartphone user'
        )
        goal = (context or {}).get('goal', 'complete the task')

        prompt = (
            f"You are {persona_desc}. "
            f"The phone agent requests permission: intent='{intent}' "
            f"for resource '{resource_id}'.\n"
            f"Reason: {reason}\n"
            f"Task: {goal}\n\n"
            f"You MUST respond with exactly one word: Allow or Deny."
        )

        try:
            response, _, _ = self.llm_wrapper.predict(prompt)
            response = response.strip()
            if 'Allow' in response:
                return 'Allow'
            return 'Deny'
        except Exception as e:
            logger.warning(f'LLMUserAgent.decide_permission failed: {e}, defaulting to Deny')
            return 'Deny'

    def answer_question(
        self,
        question: str,
        options: Optional[List[str]] = None,
        context: Optional[Dict[str, Any]] = None,
    ) -> str:
        persona_desc = self.user_persona.get(
            'description', 'a typical smartphone user'
        )

        prompt = f"You are {persona_desc}. The agent asks: {question}"
        if options:
            prompt += f"\nOptions: {', '.join(options)}"
        prompt += "\nRespond with one of the options (or a brief answer if no options)."

        try:
            response, _, _ = self.llm_wrapper.predict(prompt)
            return response.strip()
        except Exception as e:
            logger.warning(f'LLMUserAgent.answer_question failed: {e}')
            if options and len(options) > 0:
                return options[0]
            return 'Yes'
