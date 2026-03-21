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

"""Seed data generator and loader for iMy, mZocdoc, and mCVS Pharmacy apps."""

import json
import os
import platform
import shutil
import subprocess
from pathlib import Path
from typing import Optional, Dict, Any
from android_world.env import interface
from android_world.env import adb_utils


def _resolve_adb() -> str:
    """Resolve adb path across platforms (macOS / Linux / Windows)."""
    path = shutil.which("adb")
    if path:
        return path
    if platform.system() == "Windows":
        win_default = os.path.join(
            os.environ.get("LOCALAPPDATA", ""),
            "Android", "Sdk", "platform-tools", "adb.exe",
        )
        if os.path.isfile(win_default):
            return win_default
    return "/opt/homebrew/bin/adb"


def _adb_run(args: list, **kwargs) -> subprocess.CompletedProcess:
    """Run an adb command using the resolved adb path."""
    adb = _resolve_adb()
    return subprocess.run([adb] + args, **kwargs)


def push_imy_data(json_path: str) -> bool:
    """Push iMy data JSON file to device."""
    try:
        target_path = "/sdcard/imy_data.json"
        _adb_run(
            ["push", json_path, target_path],
            capture_output=True, text=True, check=True,
        )
        return True
    except subprocess.CalledProcessError as e:
        print(f"Error pushing iMy data: {e}")
        return False


def _get_app_uid(package: str) -> Optional[str]:
    """Get the Linux UID for an Android package."""
    try:
        r = _adb_run(
            ["shell", f"stat -c '%u' /data/data/{package}"],
            capture_output=True, text=True,
        )
        uid = r.stdout.strip().strip("'")
        if uid and uid.isdigit():
            return uid
    except subprocess.CalledProcessError:
        pass
    try:
        r = _adb_run(
            ["shell",
             f"dumpsys package {package} | grep userId= | head -1"],
            capture_output=True, text=True,
        )
        for token in r.stdout.split():
            if token.startswith("userId="):
                uid = token.split("=")[1]
                if uid.isdigit():
                    return uid
    except subprocess.CalledProcessError:
        pass
    return None


def _push_app_data(
    json_path: str,
    package: str,
    filename: str,
) -> bool:
    """Push a JSON data file to the device's internal app storage.

    After ``pm clear`` the app data directory is removed.  This function
    recreates it with correct ownership (looked up via ``dumpsys package``)
    **before** pushing, so that the app can read the file.  Falls back to
    ``/sdcard/`` if the internal push still fails.
    """
    files_dir = f"/data/data/{package}/files"
    internal_path = f"{files_dir}/{filename}"
    sdcard_path = f"/sdcard/{filename}"

    uid = _get_app_uid(package)

    try:
        _adb_run(
            ["shell", f"mkdir -p {files_dir}"],
            capture_output=True, text=True, check=True,
        )
        if uid:
            _adb_run(
                ["shell", f"chown -R {uid}:{uid} /data/data/{package}"],
                capture_output=True, text=True, check=True,
            )
            _adb_run(
                ["shell", f"restorecon -R /data/data/{package}"],
                capture_output=True, text=True,
            )
        _adb_run(
            ["push", json_path, internal_path],
            capture_output=True, text=True, check=True,
        )
        if uid:
            _adb_run(
                ["shell", f"chown {uid}:{uid} {internal_path}"],
                capture_output=True, text=True,
            )
        return True
    except subprocess.CalledProcessError:
        pass

    try:
        _adb_run(
            ["push", json_path, sdcard_path],
            capture_output=True, text=True, check=True,
        )
        print(f"  Pushed {filename} to {sdcard_path} (internal push failed)")
        return True
    except subprocess.CalledProcessError as e:
        print(f"Error pushing {filename} for {package}: {e}")
        return False


def push_mzocdoc_data(json_path: str) -> bool:
    """Push mZocdoc data JSON file to device."""
    return _push_app_data(json_path, "com.phoneuse.mzocdoc", "mzocdoc_data.json")


def push_mcvspharmacy_data(json_path: str) -> bool:
    """Push mCVS Pharmacy data JSON file to device."""
    return _push_app_data(
        json_path, "com.phoneuse.mcvspharmacy", "mcvspharmacy_data.json",
    )


def push_mopentable_data(json_path: str) -> bool:
    """Push mOpenTable data JSON file to device."""
    return _push_app_data(
        json_path, "com.phoneuse.mopentable", "mopentable_data.json",
    )


def push_mzillow_data(json_path: str) -> bool:
    """Push mZillow data JSON file to device."""
    return _push_app_data(
        json_path, "com.phoneuse.mzillow", "mzillow_data.json",
    )


def push_mbooking_data(json_path: str) -> bool:
    """Push mBooking data JSON file to device."""
    return _push_app_data(
        json_path, "com.phoneuse.mbooking", "mbooking_data.json",
    )


def push_mdmv_data(json_path: str) -> bool:
    """Push mDMV data JSON file to device."""
    return _push_app_data(
        json_path, "com.phoneuse.mdmv", "mdmv_data.json",
    )


def push_mdoordash_data(json_path: str) -> bool:
    """Push mDoorDash data JSON file to device."""
    return _push_app_data(
        json_path, "com.phoneuse.mdoordash", "mdoordash_data.json",
    )


def push_meventbrite_data(json_path: str) -> bool:
    """Push mEventbrite data JSON file to device."""
    return _push_app_data(
        json_path, "com.phoneuse.meventbrite", "meventbrite_data.json",
    )


def push_mgeico_data(json_path: str) -> bool:
    """Push mGEICO data JSON file to device."""
    return _push_app_data(
        json_path, "com.phoneuse.mgeico", "mgeico_data.json",
    )


def push_mthumbtack_data(json_path: str) -> bool:
    """Push mThumbtack data JSON file to device."""
    return _push_app_data(
        json_path, "com.phoneuse.mthumbtack", "mthumbtack_data.json",
    )


def _ensure_imy_db(env: interface.AsyncEnv) -> None:
    """Ensure iMy database directory and tables exist.

    After ``pm clear com.phoneuse.imy`` the entire
    ``/data/data/com.phoneuse.imy/databases/`` directory is deleted.
    We need to recreate it and the required tables before any SQL
    INSERT/DELETE operations.
    """
    db_dir = "/data/data/com.phoneuse.imy/databases"
    db_path = f"{db_dir}/profile.db"

    # Ensure directory exists
    adb_utils.issue_generic_request(
        ["shell", f"mkdir -p {db_dir}"], env
    )

    # Create tables if they don't exist (matches Room schema)
    create_stmts = [
        (
            "CREATE TABLE IF NOT EXISTS profile_items "
            "(id INTEGER PRIMARY KEY AUTOINCREMENT, "
            "level TEXT, key TEXT, value TEXT, "
            "created_at TEXT DEFAULT '', updated_at TEXT DEFAULT '');"
        ),
        (
            "CREATE TABLE IF NOT EXISTS app_permissions "
            "(app_name TEXT PRIMARY KEY, level TEXT);"
        ),
        (
            "CREATE TABLE IF NOT EXISTS write_permissions "
            "(id INTEGER PRIMARY KEY AUTOINCREMENT, "
            "timestamp TEXT, key TEXT, value TEXT, level TEXT, "
            "status TEXT, approved_key TEXT, approved_value TEXT, "
            "approved_level TEXT, consumed INTEGER DEFAULT 0);"
        ),
        (
            "CREATE TABLE IF NOT EXISTS access_log "
            "(id INTEGER PRIMARY KEY AUTOINCREMENT, "
            "timestamp TEXT, tool TEXT, action TEXT, "
            "item_key TEXT, item_level TEXT, reason TEXT, "
            "source TEXT, details TEXT);"
        ),
    ]
    for stmt in create_stmts:
        adb_utils.execute_sql_command(db_path, stmt, env)

    # Fix ownership: mkdir/sqlite3 run as root, but the app needs write access.
    adb_utils.issue_generic_request(
        ["shell",
         "owner=$(stat -c '%u:%g' /data/data/com.phoneuse.imy) && "
         f"chown -R $owner {db_dir}"],
        env,
    )


def load_imy_data(data: Dict[str, Any], env: interface.AsyncEnv) -> bool:
    """Load iMy data into the device database.

    Args:
        data: iMy data dict from JSON
        env: Android environment

    Returns:
        True if successful
    """
    try:
        # Push data to device (app will load it)
        # For now, we'll write directly to DB via SQL
        db_path = "/data/data/com.phoneuse.imy/databases/profile.db"

        # Ensure DB and tables exist (survives pm clear)
        _ensure_imy_db(env)

        # Clear ALL tables to prevent cross-task contamination
        adb_utils.execute_sql_command(db_path, "DELETE FROM profile_items;", env)
        adb_utils.execute_sql_command(db_path, "DELETE FROM app_permissions;", env)
        adb_utils.execute_sql_command(db_path, "DELETE FROM access_log;", env)
        adb_utils.execute_sql_command(db_path, "DELETE FROM write_permissions;", env)
        
        # Insert profile items
        profile_items = data.get('profile_items', [])
        for item in profile_items:
            key = item.get('key', '').replace("'", "''")
            value = item.get('value', '').replace("'", "''")
            level = item.get('level', 'low')
            query = f"INSERT INTO profile_items (key, value, level) VALUES ('{key}', '{value}', '{level}');"
            adb_utils.execute_sql_command(db_path, query, env)
        
        # Insert app permissions
        app_permissions = data.get('app_permissions', [])
        for perm in app_permissions:
            app_name = perm.get('app_name', '').replace("'", "''")
            level = perm.get('level', 'low')
            query = f"INSERT INTO app_permissions (app_name, level) VALUES ('{app_name}', '{level}');"
            adb_utils.execute_sql_command(db_path, query, env)
        
        return True
    except Exception as e:
        print(f"Error loading iMy data: {e}")
        return False

