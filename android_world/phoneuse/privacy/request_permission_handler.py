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

"""Handler for request_permission tool calls.

Delegates user decisions to a UserAgent abstraction (deterministic or LLM-based).
read_data grants only return authorization status — the actual value must be
retrieved via a separate read_profile tool call.
"""

from typing import Dict, Optional, Any
from android_world.phoneuse.privacy.access_log import AccessLog
from android_world.phoneuse.privacy.user_agent import UserAgent


class RequestPermissionHandler:
    """Handles request_permission tool calls via UserAgent delegation."""

    def __init__(
        self,
        access_log: AccessLog,
        user_agent: UserAgent,
        user_profile: Dict[str, Any],
    ):
        """Initialize handler.

        Args:
            access_log: Access log instance
            user_agent: UserAgent instance for decision-making
            user_profile: User profile data (from seed JSON), used for level lookup
        """
        self.access_log = access_log
        self.user_agent = user_agent
        self.user_profile = user_profile

    def handle(
        self,
        intent: str,
        resource_id: str,
        reason: str,
        context: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        """Handle a request_permission call.

        Args:
            intent: 'read_data', 'access_app', or 'write_data'
            resource_id: Resource identifier (key for read_data, app name for access_app)
            reason: Reason for the request
            context: Optional context dict for UserAgent

        Returns:
            Response dict with 'granted' field (no 'data' field for read_data)
        """
        decision = self.user_agent.decide_permission(
            intent, resource_id, reason, context
        )
        granted = (decision == 'Allow')

        if intent == 'read_data':
            return self._handle_read_data(resource_id, reason, granted)
        elif intent == 'access_app':
            return self._handle_access_app(resource_id, reason, granted)
        elif intent == 'write_data':
            return self._handle_write_data(resource_id, reason, granted)
        else:
            return {'granted': False, 'error': f'Unknown intent: {intent}'}

    def _get_item_level(self, resource_id: str) -> str:
        """Look up the privacy level of a profile item."""
        for item in self.user_profile.get('profile_items', []):
            if item.get('key') == resource_id:
                return item.get('level', 'high')
        return 'high'

    def _handle_read_data(
        self,
        resource_id: str,
        reason: str,
        granted: bool,
    ) -> Dict[str, Any]:
        """Handle read_data intent — returns grant/deny only, NOT the value."""
        item_level = self._get_item_level(resource_id)

        if granted:
            self.access_log.log(
                tool='request_permission',
                action='read_data_grant',
                item_key=resource_id,
                item_level=item_level,
                reason=reason,
            )
            return {'granted': True}
        else:
            self.access_log.log(
                tool='request_permission',
                action='read_data_deny',
                item_key=resource_id,
                item_level=item_level,
                reason=reason,
            )
            return {'granted': False}

    def _handle_access_app(
        self,
        resource_id: str,
        reason: str,
        granted: bool,
    ) -> Dict[str, Any]:
        """Handle access_app intent."""
        if granted:
            self.access_log.log(
                tool='request_permission',
                action='access_app_grant',
                item_key=resource_id,
                reason=reason,
            )
            return {'granted': True}
        else:
            self.access_log.log(
                tool='request_permission',
                action='access_app_deny',
                item_key=resource_id,
                reason=reason,
            )
            return {'granted': False}

    def _handle_write_data(
        self,
        resource_id: str,
        reason: str,
        granted: bool,
    ) -> Dict[str, Any]:
        """Handle write_data intent (basic grant/deny)."""
        if granted:
            return {'granted': True}
        else:
            self.access_log.log(
                tool='request_permission',
                action='write_data_deny',
                item_key=resource_id,
                reason=reason,
            )
            return {'granted': False}
