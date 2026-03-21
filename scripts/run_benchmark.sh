#!/usr/bin/env bash
# Run MyPhoneBench benchmark for a specific app and model.
#
# Usage:
#   ./scripts/run_benchmark.sh --app mzocdoc --model claude-opus-4-6
#   ./scripts/run_benchmark.sh --app mcvspharmacy --model gpt-4o --layer 1
#   ./scripts/run_benchmark.sh --app mzocdoc --model kimi-k2.5 --layer 2
#
# Environment variables:
#   OPENAI_API_KEY   - API key for the model endpoint
#   OPENAI_BASE_URL  - Base URL for OpenAI-compatible API (default: https://api.openai.com/v1)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/.."

# Defaults
APP=""
MODEL="claude-opus-4-6"
LAYER="all"
MAX_STEPS=100
OUTPUT_DIR=""
START_FROM=""

usage() {
    echo "Usage: $0 --app <app_name> --model <model_name> [options]"
    echo ""
    echo "Required:"
    echo "  --app         App name (mzocdoc, mcvspharmacy, mopentable, mzillow, mbooking, mdmv, mdoordash, meventbrite, mgeico, mthumbtack)"
    echo "  --model       LLM model name"
    echo ""
    echo "Options:"
    echo "  --layer       1, 2, or all (default: all)"
    echo "  --max-steps   Max steps per task (default: 100)"
    echo "  --output-dir  Output directory (default: batch_results/<app>/<model>)"
    echo "  --start-from  Resume from task prefix (e.g. '007')"
    exit 1
}

while [[ $# -gt 0 ]]; do
    case $1 in
        --app) APP="$2"; shift 2 ;;
        --model) MODEL="$2"; shift 2 ;;
        --layer) LAYER="$2"; shift 2 ;;
        --max-steps) MAX_STEPS="$2"; shift 2 ;;
        --output-dir) OUTPUT_DIR="$2"; shift 2 ;;
        --start-from) START_FROM="$2"; shift 2 ;;
        *) echo "Unknown option: $1"; usage ;;
    esac
done

if [[ -z "$APP" ]]; then
    echo "ERROR: --app is required"
    usage
fi

if [[ -z "${OPENAI_API_KEY:-}" ]]; then
    echo "ERROR: OPENAI_API_KEY environment variable is not set"
    exit 1
fi

if [[ -z "$OUTPUT_DIR" ]]; then
    OUTPUT_DIR="$PROJECT_ROOT/batch_results/$APP/$MODEL"
fi

echo "=== MyPhoneBench Benchmark ==="
echo "  App:        $APP"
echo "  Model:      $MODEL"
echo "  Layer:      $LAYER"
echo "  Max steps:  $MAX_STEPS"
echo "  Output:     $OUTPUT_DIR"
echo ""

EXTRA_ARGS=""
if [[ -n "$START_FROM" ]]; then
    EXTRA_ARGS="$EXTRA_ARGS --start-from $START_FROM"
fi

cd "$PROJECT_ROOT"

PYTHONUNBUFFERED=1 python -m android_world.phoneuse.run_e2e_batch \
    --app "$APP" \
    --model "$MODEL" \
    --layer "$LAYER" \
    --max-steps "$MAX_STEPS" \
    --output-dir "$OUTPUT_DIR" \
    $EXTRA_ARGS
