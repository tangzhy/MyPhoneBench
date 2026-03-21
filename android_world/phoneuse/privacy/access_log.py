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

"""In-memory access log for privacy evaluation."""

import time
from typing import List, Dict, Optional
from dataclasses import dataclass, field


@dataclass
class AccessLogEntry:
    """Single entry in the access log."""
    timestamp: str
    tool: str  # 'request_permission' | 'save_profile' | 'ask_user'
    action: str  # e.g., 'read_data_grant', 'write_executed', etc.
    item_key: Optional[str] = None
    item_level: Optional[str] = None
    reason: Optional[str] = None
    details: Optional[Dict] = None


class AccessLog:
    """In-memory access log for tracking privacy-related operations."""
    
    def __init__(self):
        self.entries: List[AccessLogEntry] = []
    
    def log(
        self,
        tool: str,
        action: str,
        item_key: Optional[str] = None,
        item_level: Optional[str] = None,
        reason: Optional[str] = None,
        details: Optional[Dict] = None,
    ):
        """Log a privacy-related operation.
        
        Args:
            tool: Tool name ('request_permission', 'save_profile', 'ask_user')
            action: Action type (e.g., 'read_data_grant', 'write_executed')
            item_key: Profile item key (if applicable)
            item_level: Privacy level ('low' or 'high')
            reason: Reason for the operation
            details: Additional context as dict
        """
        entry = AccessLogEntry(
            timestamp=time.strftime('%Y-%m-%d %H:%M:%S'),
            tool=tool,
            action=action,
            item_key=item_key,
            item_level=item_level,
            reason=reason,
            details=details or {},
        )
        self.entries.append(entry)
    
    def get_entries(
        self,
        tool: Optional[str] = None,
        action: Optional[str] = None,
        item_key: Optional[str] = None,
    ) -> List[AccessLogEntry]:
        """Get filtered access log entries.
        
        Args:
            tool: Filter by tool name
            action: Filter by action type
            item_key: Filter by item key
            
        Returns:
            List of matching entries
        """
        result = self.entries
        if tool:
            result = [e for e in result if e.tool == tool]
        if action:
            result = [e for e in result if e.action == action]
        if item_key:
            result = [e for e in result if e.item_key == item_key]
        return result
    
    def has_granted_permission(
        self,
        intent: str,
        resource_id: str,
    ) -> bool:
        """Check if a permission was granted.
        
        Args:
            intent: 'read_data', 'access_app', or 'write_data'
            resource_id: Resource identifier (key or app name)
            
        Returns:
            True if permission was granted
        """
        if intent == 'read_data':
            action = 'read_data_grant'
        elif intent == 'access_app':
            action = 'access_app_grant'
        elif intent == 'write_data':
            action = 'write_data_grant'
        else:
            return False
        
        entries = self.get_entries(
            tool='request_permission',
            action=action,
            item_key=resource_id,
        )
        return len(entries) > 0
    
    def get_write_permission(
        self,
        key: str,
        value: str,
        level: str,
    ) -> Optional[AccessLogEntry]:
        """Get matching write permission entry for save_profile validation.
        
        Args:
            key: Profile key
            value: Profile value
            level: Privacy level
            
        Returns:
            Matching permission entry or None
        """
        entries = self.get_entries(
            tool='request_permission',
            action='write_data_grant',
        )
        
        for entry in entries:
            if entry.item_key == key:
                # Check if details match
                details = entry.details or {}
                if (details.get('approved_key') == key and
                    details.get('approved_value') == value and
                    details.get('approved_level') == level):
                    # Check if not yet consumed
                    if not details.get('consumed', False):
                        return entry
        return None
    
    def mark_permission_consumed(self, entry: AccessLogEntry):
        """Mark a write permission as consumed (used by save_profile).
        
        Args:
            entry: The permission entry to mark
        """
        if entry.details is None:
            entry.details = {}
        entry.details['consumed'] = True
    
    def clear(self):
        """Clear all log entries."""
        self.entries = []
    
    def to_dict_list(self) -> List[Dict]:
        """Convert to list of dictionaries for serialization."""
        return [
            {
                'timestamp': e.timestamp,
                'tool': e.tool,
                'action': e.action,
                'item_key': e.item_key,
                'item_level': e.item_level,
                'reason': e.reason,
                'details': e.details,
            }
            for e in self.entries
        ]

