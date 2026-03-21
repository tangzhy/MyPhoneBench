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

"""Task verifier using SQLite rule-based checks.

Supports three verification modes:
  1. Presence (default): count > 0  — at least one matching row exists
  2. Absent:             count == 0 — no matching rows (for cancel verification)
  3. Exact count:        count == N — exact number of matching rows

Verification config can be a single dict or a list of dicts (all must pass).
"""

import json
from typing import Dict, List, Any, Optional
from android_world.env import adb_utils
from android_world.env import interface


class TaskVerifier:
    """Verifies task completion using SQLite rule-based checks."""

    def __init__(
        self,
        task_definition: Dict[str, Any],
        env: interface.AsyncEnv,
    ):
        """Initialize verifier.

        Args:
            task_definition: Task definition dict from JSON
            env: Android environment
        """
        self.task_definition = task_definition
        self.env = env
        self.verification_config = task_definition.get('verification', {})

    def verify(self) -> Dict[str, Any]:
        """Verify if task is completed.

        Supports single verification block (dict) or multiple blocks (list).
        All blocks must pass for the task to be considered resolved.

        Returns:
            Dict with 'resolved' (bool) and 'details' (list of per-block results)
        """
        config = self.verification_config

        # Normalize to list
        if isinstance(config, dict):
            blocks = [config]
        elif isinstance(config, list):
            blocks = config
        else:
            return {
                'resolved': False,
                'error': f'Invalid verification config type: {type(config).__name__}',
            }

        if not blocks:
            return {'resolved': False, 'error': 'Empty verification config'}

        block_results = []
        all_passed = True

        for i, block in enumerate(blocks):
            result = self._verify_single(block, index=i)
            block_results.append(result)
            if not result.get('passed', False):
                all_passed = False

        return {
            'resolved': all_passed,
            'details': block_results,
        }

    def _verify_single(self, block: Dict[str, Any], index: int = 0) -> Dict[str, Any]:
        """Verify a single verification block.

        Args:
            block: Single verification config dict
            index: Block index for reporting

        Returns:
            Dict with 'passed' (bool), 'query', 'match_count', and 'mode'
        """
        if block.get('type') != 'sqlite':
            return {
                'passed': False,
                'error': f"Unsupported verification type: {block.get('type')}",
            }

        db_path = block.get('database_path')
        table = block.get('table')
        rules = block.get('rules', [])

        if not all([db_path, table, rules]):
            return {
                'passed': False,
                'error': 'Missing verification config: database_path, table, or rules',
            }

        # Determine verification mode
        expect = block.get('expect')
        expected_count = block.get('expected_count')

        if expect == 'absent':
            mode = 'absent'
        elif expected_count is not None:
            mode = 'exact_count'
        else:
            mode = 'present'

        count_query = self._build_count_query(table, rules)

        try:
            from android_env.proto import adb_pb2
            response = adb_utils.execute_sql_command(
                db_path, count_query, self.env.controller
            )

            if response.status != adb_pb2.AdbResponse.Status.OK:
                return {
                    'passed': False,
                    'error': (
                        f'SQL query failed: '
                        f'{response.generic.output.decode("utf-8", errors="replace")}'
                    ),
                }

            output = response.generic.output.decode('utf-8', errors='replace').strip()
            try:
                count = int(output)
            except (ValueError, TypeError):
                count = 0

            if mode == 'absent':
                passed = count == 0
            elif mode == 'exact_count':
                passed = count == expected_count
            else:  # present
                passed = count > 0

            return {
                'passed': passed,
                'mode': mode,
                'query': count_query,
                'match_count': count,
                'expected': (
                    0 if mode == 'absent'
                    else expected_count if mode == 'exact_count'
                    else '>0'
                ),
            }
        except Exception as e:
            return {
                'passed': False,
                'error': f'Verification failed: {e}',
            }

    @staticmethod
    def _parse_in_values(value) -> List[str]:
        """Parse IN clause values from list or comma-separated string."""
        if isinstance(value, list):
            items = value
        elif isinstance(value, str):
            items = [v.strip() for v in value.split(',')]
        else:
            items = [value]
        sql_parts = []
        for v in items:
            s = str(v).strip()
            if s.isdigit():
                sql_parts.append(s)
            else:
                sql_parts.append(f"'{s.replace(chr(39), chr(39)+chr(39))}'")
        return sql_parts

    def _build_count_query(self, table: str, rules: List[Dict[str, Any]]) -> str:
        """Build SQL SELECT COUNT(*) query from rules."""
        conditions = []
        for rule in rules:
            field = rule.get('field')
            operator = rule.get('operator', '==')
            value = rule.get('value')
            if operator == 'IN':
                in_values = self._parse_in_values(value)
                conditions.append(f"{field} IN ({', '.join(in_values)})")
            else:
                sql_op = {
                    '==': '=', '!=': '!=', '>': '>', '<': '<', '>=': '>=', '<=': '<=',
                    'LIKE': 'LIKE',
                }.get(operator, '=')
                if isinstance(value, str):
                    value_str = f"'{value.replace(chr(39), chr(39)+chr(39))}'"
                else:
                    value_str = str(value)
                conditions.append(f"{field} {sql_op} {value_str}")
        where_clause = ' AND '.join(conditions)
        return f"SELECT COUNT(*) FROM {table} WHERE {where_clause};"

    def _build_query(self, table: str, rules: List[Dict[str, Any]]) -> str:
        """Build SQL SELECT query from rules."""
        conditions = []
        for rule in rules:
            field = rule.get('field')
            operator = rule.get('operator', '==')
            value = rule.get('value')
            if operator == 'IN':
                in_values = self._parse_in_values(value)
                conditions.append(f"{field} IN ({', '.join(in_values)})")
            else:
                sql_op = {
                    '==': '=', '!=': '!=', '>': '>', '<': '<', '>=': '>=', '<=': '<=',
                    'LIKE': 'LIKE',
                }.get(operator, '=')
                if isinstance(value, str):
                    value_str = f"'{value.replace(chr(39), chr(39)+chr(39))}'"
                else:
                    value_str = str(value)
                conditions.append(f"{field} {sql_op} {value_str}")
        where_clause = ' AND '.join(conditions)
        return f"SELECT * FROM {table} WHERE {where_clause};"

    def _parse_sql_results(self, output: str) -> List[Dict[str, Any]]:
        """Parse SQL query results."""
        if not output or not output.strip():
            return []

        lines = output.strip().split('\n')
        if len(lines) < 2:
            return []

        headers = [h.strip() for h in lines[0].split('|')]
        results = []
        for line in lines[1:]:
            if not line.strip():
                continue
            values = [v.strip() for v in line.split('|')]
            if len(values) == len(headers):
                results.append(dict(zip(headers, values)))
        return results
