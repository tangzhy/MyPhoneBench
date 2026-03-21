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

"""Handler for ask_user tool calls.

Delegates to UserAgent abstraction for consistent decision-making.
"""

from typing import Dict, Any, Optional, List
from android_world.phoneuse.privacy.user_agent import UserAgent


class AskUserHandler:
    """Handles ask_user tool calls via UserAgent delegation."""

    def __init__(self, user_agent: UserAgent):
        """Initialize handler.

        Args:
            user_agent: UserAgent instance for answering questions
        """
        self.user_agent = user_agent

    def handle(
        self,
        question: str,
        options: Optional[List[str]] = None,
        context: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        """Handle an ask_user call.

        Args:
            question: Question to ask the user
            options: Optional list of choices
            context: Optional context dict for UserAgent

        Returns:
            Response dict with 'response' field containing user's answer
        """
        answer = self.user_agent.answer_question(question, options, context)
        return {'response': answer}
