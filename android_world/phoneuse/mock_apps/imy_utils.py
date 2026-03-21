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

"""Utilities for interacting with iMy app database."""

from android_world.env import adb_utils
from android_world.env import interface

IMY_DB_PATH = "/data/data/com.phoneuse.imy/databases/profile.db"

_db_ensured = False


def _ensure_imy_db(env: interface.AsyncEnv) -> None:
    """Ensure iMy DB directory and profile_items table exist (idempotent)."""
    global _db_ensured
    if _db_ensured:
        return
    db_dir = "/data/data/com.phoneuse.imy/databases"
    adb_utils.issue_generic_request(["shell", f"mkdir -p {db_dir}"], env.controller)
    adb_utils.execute_sql_command(
        IMY_DB_PATH,
        "CREATE TABLE IF NOT EXISTS profile_items "
        "(id INTEGER PRIMARY KEY AUTOINCREMENT, "
        "level TEXT, key TEXT, value TEXT, "
        "created_at TEXT DEFAULT '', updated_at TEXT DEFAULT '');",
        env.controller,
    )
    # Fix ownership: mkdir/sqlite3 run as root, but the app needs write access.
    adb_utils.issue_generic_request(
        ["shell",
         "owner=$(stat -c '%u:%g' /data/data/com.phoneuse.imy) && "
         f"chown -R $owner {db_dir}"],
        env.controller,
    )
    _db_ensured = True


def upsert_profile_item(
    env: interface.AsyncEnv,
    key: str,
    value: str,
    level: str,
) -> None:
    """Insert or update a profile item in iMy SQLite DB.

    Args:
        env: Android environment (needs .controller for ADB access)
        key: Profile key
        value: Profile value
        level: Privacy level ('low' or 'high')
    """
    _ensure_imy_db(env)
    escaped_key = key.replace("'", "''")
    escaped_value = value.replace("'", "''")
    escaped_level = level.replace("'", "''")
    sql = (
        f"INSERT OR REPLACE INTO profile_items (key, value, level) "
        f"VALUES ('{escaped_key}', '{escaped_value}', '{escaped_level}');"
    )
    adb_utils.execute_sql_command(IMY_DB_PATH, sql, env.controller)


def read_all_profile_items(env: interface.AsyncEnv) -> list[dict]:
    """Read all profile items from iMy SQLite DB.

    Args:
        env: Android environment

    Returns:
        List of dicts with 'key', 'value', 'level' fields
    """
    sql = "SELECT key, value, level FROM profile_items;"
    response = adb_utils.execute_sql_command(IMY_DB_PATH, sql, env.controller)
    output = response.generic.output.decode('utf-8', errors='replace').strip()

    items = []
    if output:
        for line in output.split('\n'):
            parts = line.split('|')
            if len(parts) >= 3:
                items.append({
                    'key': parts[0].strip(),
                    'value': parts[1].strip(),
                    'level': parts[2].strip(),
                })
    return items
