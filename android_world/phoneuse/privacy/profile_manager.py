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

"""Profile manager for reading/writing user profile data via ADB sqlite3."""

import json
from typing import Dict, List, Optional
from android_world.env import adb_utils
from android_world.env import interface

PROFILE_DB_PATH = "/data/data/com.phoneuse.imy/databases/profile.db"


def get_low_items(env: interface.AsyncEnv) -> Dict[str, str]:
    """Get all LOW-level profile items for system prompt injection.
    
    Args:
        env: The Android environment.
        
    Returns:
        Dictionary mapping key to value for all LOW-level items.
    """
    query = "SELECT key, value FROM profile_items WHERE level = 'low';"
    response = adb_utils.execute_sql_command(PROFILE_DB_PATH, query, env)
    
    items = {}
    if response.output:
        for line in response.output.strip().split('\n'):
            if '|' in line:
                key, value = line.split('|', 1)
                items[key.strip()] = value.strip()
    
    return items


def get_high_item(key: str, env: interface.AsyncEnv) -> Optional[str]:
    """Get a HIGH-level profile item (simulates request_permission).
    
    Args:
        key: The profile item key to retrieve.
        env: The Android environment.
        
    Returns:
        The value if found, None otherwise.
    """
    query = f"SELECT value FROM profile_items WHERE level = 'high' AND key = '{key}' LIMIT 1;"
    response = adb_utils.execute_sql_command(PROFILE_DB_PATH, query, env)
    
    if response.output and response.output.strip():
        return response.output.strip()
    return None


def get_all_profile_items(env: interface.AsyncEnv) -> List[Dict[str, str]]:
    """Get all profile items (for debugging).
    
    Args:
        env: The Android environment.
        
    Returns:
        List of dictionaries with 'level', 'key', 'value' keys.
    """
    query = "SELECT level, key, value FROM profile_items;"
    response = adb_utils.execute_sql_command(PROFILE_DB_PATH, query, env)
    
    items = []
    if response.output:
        for line in response.output.strip().split('\n'):
            if '|' in line:
                parts = line.split('|')
                if len(parts) >= 3:
                    items.append({
                        'level': parts[0].strip(),
                        'key': parts[1].strip(),
                        'value': parts[2].strip()
                    })
    
    return items

