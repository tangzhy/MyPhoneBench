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

"""Handler for save_profile tool calls."""

from typing import Dict, Any, Optional
from android_world.phoneuse.privacy.access_log import AccessLog


class SaveProfileHandler:
    """Handles save_profile tool calls with privacy mode enforcement."""

    def __init__(
        self,
        access_log: AccessLog,
        privacy_mode: str = 'full_control',
        env=None,
    ):
        """Initialize handler.

        Args:
            access_log: Access log instance
            privacy_mode: 'full_control' or 'semi_control'
            env: Android environment for actual DB writes (optional)
        """
        self.access_log = access_log
        self.privacy_mode = privacy_mode
        self.env = env

    def _persist_to_db(self, key: str, value: str, level: str) -> None:
        """Write profile item to iMy SQLite DB if env is available."""
        if self.env is not None:
            from android_world.phoneuse.mock_apps import imy_utils
            imy_utils.upsert_profile_item(self.env, key, value, level)

    def handle(
        self,
        key: str,
        value: str,
        level: str,
        reason: str,
    ) -> Dict[str, Any]:
        """Handle a save_profile call.

        Args:
            key: Profile key
            value: Profile value
            level: Privacy level ('low' or 'high')
            reason: Reason for saving

        Returns:
            Response dict with 'status' ('saved' or 'error') and optional 'message'
        """
        if self.privacy_mode == 'full_control':
            return self._handle_full_control(key, value, level, reason)
        else:  # semi_control
            return self._handle_semi_control(key, value, level, reason)

    def _handle_full_control(
        self,
        key: str,
        value: str,
        level: str,
        reason: str,
    ) -> Dict[str, Any]:
        """Handle save_profile in full_control mode (requires permission)."""
        permission_entry = self.access_log.get_write_permission(key, value, level)

        if permission_entry is None:
            self.access_log.log(
                tool='save_profile',
                action='write_rejected',
                item_key=key,
                item_level=level,
                reason=reason,
                details={'error': 'no_permission'},
            )
            return {
                'status': 'error',
                'message': (
                    'Write permission required. Call request_permission('
                    'intent="write_data", key="...", value="...", level="...") first.'
                )
            }

        # Check parameter consistency
        details = permission_entry.details or {}
        approved_key = details.get('approved_key')
        approved_value = details.get('approved_value')
        approved_level = details.get('approved_level')

        if (approved_key != key or approved_value != value or approved_level != level):
            self.access_log.log(
                tool='save_profile',
                action='write_rejected',
                item_key=key,
                item_level=level,
                reason=reason,
                details={'error': 'parameter_mismatch'},
            )
            return {
                'status': 'error',
                'message': (
                    'Parameters do not match approved write. '
                    f'Expected: key={approved_key}, value={approved_value}, level={approved_level}'
                )
            }

        # Mark permission as consumed
        self.access_log.mark_permission_consumed(permission_entry)

        # Log and persist
        self.access_log.log(
            tool='save_profile',
            action='write_executed',
            item_key=key,
            item_level=level,
            reason=reason,
        )
        self._persist_to_db(key, value, level)

        return {'status': 'saved'}

    def _handle_semi_control(
        self,
        key: str,
        value: str,
        level: str,
        reason: str,
    ) -> Dict[str, Any]:
        """Handle save_profile in semi_control mode (direct write)."""
        self.access_log.log(
            tool='save_profile',
            action='write_auto',
            item_key=key,
            item_level=level,
            reason=reason,
        )
        self._persist_to_db(key, value, level)

        return {'status': 'saved'}
