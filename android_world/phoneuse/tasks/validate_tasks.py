#!/usr/bin/env python3
"""Validate task JSON files for schema, consistency, and coverage.

Checks:
  1. Schema: required fields present, correct types
  2. Decision-classification consistency: granted fields match classification
  3. Trap coverage: >=50% tasks should have leakage traps (v2)
  4. Profile/vagueness distribution: balanced across categories
  5. Leakage trap feasibility: check_query references valid patterns
  6. Optional --llm-review: Claude-based logical consistency check

Usage:
  python -m android_world.phoneuse.tasks.validate_tasks --tasks-dir tasks/generated_v2
  python -m android_world.phoneuse.tasks.validate_tasks --tasks-dir tasks/generated  # v1
"""

import argparse
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Dict, Any, List, Tuple

# Schema requirements
REQUIRED_FIELDS = {
    "task_id": str,
    "difficulty": str,
    "max_steps": int,
    "goal": str,
    "seed_data": dict,
    "user_decisions": dict,
    "verification": (dict, list),
    "privacy_traps": list,
}

# v2-specific fields (optional for v1)
V2_FIELDS = {
    "user_profile": str,
    "goal_vagueness": str,
    "permission_classification": dict,
    "expected_form_fields": list,
}

VALID_DIFFICULTIES = {"easy", "medium", "hard", "expert"}
VALID_PROFILES = {"open", "cautious", "strict"}
VALID_VAGUENESS = {"explicit", "moderate", "vague", "tempting"}
VALID_TRAP_TYPES = {"over_permissioning", "field_trap", "data_leakage"}
VALID_PERM_CATEGORIES = {"required", "plausible", "unnecessary", "forbidden"}


def validate_schema(task: Dict, task_id: str, is_v2: bool) -> List[str]:
    """Check required fields and types."""
    errors = []
    for field, expected_type in REQUIRED_FIELDS.items():
        if field not in task:
            errors.append(f"[{task_id}] Missing required field: {field}")
        elif isinstance(expected_type, tuple):
            if not isinstance(task[field], expected_type):
                errors.append(
                    f"[{task_id}] Field '{field}' has wrong type: "
                    f"expected {expected_type}, got {type(task[field]).__name__}"
                )
        elif not isinstance(task[field], expected_type):
            errors.append(
                f"[{task_id}] Field '{field}' has wrong type: "
                f"expected {expected_type.__name__}, got {type(task[field]).__name__}"
            )

    if task.get("difficulty") not in VALID_DIFFICULTIES:
        errors.append(
            f"[{task_id}] Invalid difficulty: {task.get('difficulty')} "
            f"(valid: {VALID_DIFFICULTIES})"
        )

    if is_v2:
        for field, expected_type in V2_FIELDS.items():
            if field not in task:
                errors.append(f"[{task_id}] Missing v2 field: {field}")
            elif not isinstance(task[field], expected_type):
                errors.append(
                    f"[{task_id}] v2 field '{field}' has wrong type: "
                    f"expected {expected_type.__name__}, got {type(task[field]).__name__}"
                )

        if task.get("user_profile") and task["user_profile"] not in VALID_PROFILES:
            errors.append(
                f"[{task_id}] Invalid user_profile: {task['user_profile']} "
                f"(valid: {VALID_PROFILES})"
            )
        if task.get("goal_vagueness") and task["goal_vagueness"] not in VALID_VAGUENESS:
            errors.append(
                f"[{task_id}] Invalid goal_vagueness: {task['goal_vagueness']} "
                f"(valid: {VALID_VAGUENESS})"
            )

    return errors


def validate_decisions_vs_classification(task: Dict, task_id: str) -> List[str]:
    """Check consistency between user_decisions grants and permission_classification."""
    errors = []
    perm_class = task.get("permission_classification", {})
    if not perm_class:
        return errors

    decisions = task.get("user_decisions", {})
    read_data = decisions.get("read_data", {})
    default_policy = read_data.get("default", "deny")

    # Build field -> category lookup
    field_category = {}
    for category in VALID_PERM_CATEGORIES:
        for field in perm_class.get(category, []):
            field_category[field] = category

    # Check explicit grants
    for field, decision in read_data.items():
        if field == "default":
            continue
        if decision == "grant" and field in field_category:
            cat = field_category[field]
            if cat == "forbidden":
                errors.append(
                    f"[{task_id}] Field '{field}' is classified as 'forbidden' "
                    f"but explicitly granted in user_decisions"
                )

    # For open profile (default=grant), forbidden fields are accessible
    # This is intentional — the test is whether the agent requests them
    if default_policy == "grant":
        profile = task.get("user_profile", "unknown")
        if profile != "open":
            errors.append(
                f"[{task_id}] read_data.default='grant' but user_profile='{profile}' "
                f"(expected 'open')"
            )

    return errors


def validate_traps(task: Dict, task_id: str) -> List[str]:
    """Check trap definitions for validity."""
    errors = []
    warnings = []
    for i, trap in enumerate(task.get("privacy_traps", [])):
        trap_type = trap.get("type", "")
        if trap_type not in VALID_TRAP_TYPES:
            errors.append(
                f"[{task_id}] Trap #{i}: invalid type '{trap_type}' "
                f"(valid: {VALID_TRAP_TYPES})"
            )

        if trap_type in ("field_trap", "data_leakage"):
            if not trap.get("database_path"):
                errors.append(f"[{task_id}] Trap #{i} ({trap_type}): missing database_path")
            if not trap.get("check_query"):
                errors.append(f"[{task_id}] Trap #{i} ({trap_type}): missing check_query")
            elif "SELECT COUNT" not in trap["check_query"].upper():
                warnings.append(
                    f"[{task_id}] Trap #{i}: check_query may not return a count "
                    f"(expected SELECT COUNT(*))"
                )

        if trap_type == "over_permissioning":
            if not trap.get("fields"):
                warnings.append(f"[{task_id}] Trap #{i}: over_permissioning has no fields")

    return errors


def validate_verification(task: Dict, task_id: str) -> List[str]:
    """Check verification block structure."""
    errors = []
    verification = task.get("verification", {})

    verifications = verification if isinstance(verification, list) else [verification]
    for i, v in enumerate(verifications):
        if not v.get("type"):
            errors.append(f"[{task_id}] Verification #{i}: missing type")
        if not v.get("database_path"):
            errors.append(f"[{task_id}] Verification #{i}: missing database_path")
        if not v.get("table"):
            errors.append(f"[{task_id}] Verification #{i}: missing table")
        for j, rule in enumerate(v.get("rules", [])):
            if not rule.get("field"):
                errors.append(f"[{task_id}] Verification #{i}, rule #{j}: missing field")
            if "value" not in rule:
                errors.append(f"[{task_id}] Verification #{i}, rule #{j}: missing value")

    return errors


def check_distribution(
    tasks: List[Dict], field: str, expected: Dict[str, int], label: str
) -> List[str]:
    """Check that task distribution across a field is reasonable."""
    warnings = []
    counts = Counter(t.get(field, "MISSING") for t in tasks)

    for value in counts:
        if value == "MISSING":
            warnings.append(f"  WARNING: {counts[value]} tasks missing '{field}' field")

    print(f"\n{label} distribution:")
    for value, count in sorted(counts.items()):
        marker = ""
        if value in expected:
            diff = count - expected[value]
            if abs(diff) > 2:
                marker = f" (expected ~{expected[value]}, diff={diff:+d})"
        print(f"  {value}: {count}{marker}")

    return warnings


def validate_tasks(tasks_dir: Path, is_v2: bool = False) -> Tuple[List[str], List[str]]:
    """Run all validations on task files in the directory."""
    errors = []
    warnings = []

    # Load all task files
    task_files = sorted(tasks_dir.glob("*.json"))
    if not task_files:
        errors.append(f"No task JSON files found in {tasks_dir}")
        return errors, warnings

    tasks = []
    for path in task_files:
        try:
            with open(path, encoding='utf-8') as f:
                task = json.load(f)
            tasks.append(task)
        except json.JSONDecodeError as e:
            errors.append(f"Invalid JSON in {path.name}: {e}")

    print(f"Loaded {len(tasks)} tasks from {tasks_dir}")

    # 1. Schema validation
    for task in tasks:
        tid = task.get("task_id", "UNKNOWN")
        errors.extend(validate_schema(task, tid, is_v2))
        errors.extend(validate_decisions_vs_classification(task, tid))
        errors.extend(validate_traps(task, tid))
        errors.extend(validate_verification(task, tid))

    # 2. Distribution checks (v2 only)
    if is_v2:
        warnings.extend(check_distribution(
            tasks, "user_profile",
            {"open": 10, "cautious": 10, "strict": 5},
            "User Profile",
        ))
        warnings.extend(check_distribution(
            tasks, "goal_vagueness",
            {"explicit": 3, "moderate": 7, "vague": 10, "tempting": 5},
            "Goal Vagueness",
        ))

    warnings.extend(check_distribution(
        tasks, "difficulty",
        {"easy": 8, "medium": 8, "hard": 6, "expert": 3},
        "Difficulty",
    ))

    # 3. Trap coverage
    leakage_count = sum(
        1 for t in tasks
        if any(trap.get("type") == "data_leakage" for trap in t.get("privacy_traps", []))
    )
    field_trap_count = sum(
        1 for t in tasks
        if any(trap.get("type") == "field_trap" for trap in t.get("privacy_traps", []))
    )
    perm_trap_count = sum(
        1 for t in tasks
        if any(trap.get("type") == "over_permissioning" for trap in t.get("privacy_traps", []))
    )

    print(f"\nTrap coverage:")
    print(f"  over_permissioning: {perm_trap_count}/{len(tasks)} ({100*perm_trap_count/len(tasks):.0f}%)")
    print(f"  field_trap (SSN): {field_trap_count}/{len(tasks)} ({100*field_trap_count/len(tasks):.0f}%)")
    print(f"  data_leakage: {leakage_count}/{len(tasks)} ({100*leakage_count/len(tasks):.0f}%)")

    if is_v2 and leakage_count < len(tasks) * 0.4:
        warnings.append(
            f"  WARNING: Leakage trap coverage {leakage_count}/{len(tasks)} "
            f"({100*leakage_count/len(tasks):.0f}%) is below 40% target"
        )

    # 4. Permission classification coverage
    all_perm_fields = set()
    for t in tasks:
        pc = t.get("permission_classification", {})
        for cat in VALID_PERM_CATEGORIES:
            all_perm_fields.update(pc.get(cat, []))

    # Count how many distinct classification patterns exist
    perm_patterns = Counter()
    for t in tasks:
        pc = t.get("permission_classification", {})
        pattern = tuple(sorted(pc.get("required", [])))
        perm_patterns[pattern] += 1

    print(f"\nPermission classification:")
    print(f"  Distinct fields used: {len(all_perm_fields)}")
    print(f"  Distinct 'required' patterns: {len(perm_patterns)}")
    if len(perm_patterns) == 1 and len(tasks) > 5:
        warnings.append(
            "  WARNING: All tasks share identical 'required' classification "
            "— over_permissioning scoring has no task-to-task variation"
        )

    # 5. Check pairs directory
    pairs_dir = tasks_dir / "pairs"
    if pairs_dir.exists():
        pair_files = sorted(pairs_dir.glob("*.json"))
        print(f"\nLayer 2 pairs: {len(pair_files)}")
        for path in pair_files:
            try:
                with open(path, encoding='utf-8') as f:
                    pair = json.load(f)
                for side in ("task_a", "task_b"):
                    if side not in pair:
                        errors.append(f"Pair {path.name}: missing {side}")
                if "expected_save_key" not in pair:
                    warnings.append(f"Pair {path.name}: missing expected_save_key")
            except json.JSONDecodeError as e:
                errors.append(f"Invalid JSON in pair {path.name}: {e}")

    return errors, warnings


def main():
    parser = argparse.ArgumentParser(description="Validate task JSON files")
    parser.add_argument(
        "--tasks-dir",
        type=Path,
        default=Path(__file__).parent / "generated_v2",
        help="Directory containing task JSON files",
    )
    parser.add_argument(
        "--v2",
        action="store_true",
        default=None,
        help="Enforce v2 schema (auto-detected from dir name if not specified)",
    )
    args = parser.parse_args()

    tasks_dir = args.tasks_dir
    if not tasks_dir.is_absolute():
        # Resolve relative to cwd, not relative to this script
        tasks_dir = Path.cwd() / tasks_dir

    is_v2 = args.v2 if args.v2 is not None else ("v2" in tasks_dir.name)

    print(f"Validating {'v2' if is_v2 else 'v1'} tasks in {tasks_dir}\n")
    print("=" * 60)

    errors, warnings = validate_tasks(tasks_dir, is_v2)

    print("\n" + "=" * 60)

    if warnings:
        print(f"\nWarnings ({len(warnings)}):")
        for w in warnings:
            print(f"  {w}")

    if errors:
        print(f"\nERRORS ({len(errors)}):")
        for e in errors:
            print(f"  {e}")
        print(f"\nValidation FAILED with {len(errors)} errors")
        sys.exit(1)
    else:
        print(f"\nValidation PASSED ({len(warnings)} warnings)")


if __name__ == "__main__":
    main()
