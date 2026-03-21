# MyPhoneBench

A privacy-aware benchmark for evaluating LLM-based phone agents on realistic mobile tasks.

MyPhoneBench measures how well AI agents complete everyday phone tasks — booking appointments, ordering food, managing insurance — while respecting user privacy preferences. It features 10 mock Android apps spanning healthcare, dining, real estate, government services, and more, each with carefully designed privacy traps and dark patterns that test whether agents over-share personal information.

## Key Features

- **10 Mock Apps** covering diverse real-world scenarios (see table below)
- **250 Independent Tasks** (25 per app): form-filling tasks with privacy-sensitive fields
- **50 Cross-Session Pairs** (5 per app): consistency tests where the agent must remember prior decisions
- **3-Dimensional Privacy Scoring**: Over-Permissioning (OP), Transparency (TR), and Form-filling Minimization (FM)
- **Dark Pattern Traps**: pre-checked checkboxes, misleading opt-in/out toggles, hidden data sharing clauses
- **iMy Privacy Manager**: a companion app that stores user privacy preferences the agent should consult

## Mock App Coverage

| App Key | Display Name | Domain | Task Type |
|---------|-------------|--------|-----------|
| `mzocdoc` | mZocdoc | Healthcare | Doctor appointment booking |
| `mcvspharmacy` | mCVS Pharmacy | Pharmacy | Prescription & vaccination booking |
| `mopentable` | mOpenTable | Dining | Restaurant reservation |
| `mzillow` | mZillow | Real Estate | Property viewing appointment |
| `mbooking` | mBooking | Travel | Hotel reservation |
| `mdmv` | mDMV | Government | DMV appointment scheduling |
| `mdoordash` | mDoorDash | Food Delivery | Food ordering |
| `meventbrite` | mEventbrite | Events | Event registration |
| `mgeico` | mGEICO | Insurance | Insurance quote request |
| `mthumbtack` | mThumbtack | Home Services | Service provider booking |

## Prerequisites

- Python 3.11+
- Android SDK with `adb` and `emulator` in PATH
- Android emulator (API 34, x86_64) with Google APIs
- An OpenAI-compatible API endpoint and key

## Installation

```bash
# Clone the repo
git clone https://github.com/anonymous/MyPhoneBench.git
cd MyPhoneBench

# Install Python dependencies
pip install -r requirements.txt
pip install -e .

# Set up the Android emulator
./scripts/setup_emulator.sh

# Install all mock app APKs
./scripts/install_apks.sh
```

## Configuration

MyPhoneBench uses an OpenAI-compatible API interface. Set these environment variables:

```bash
export OPENAI_API_KEY="your-api-key"
export OPENAI_BASE_URL="https://api.openai.com/v1"  # or any compatible endpoint
```

Supported models include any that work with the OpenAI chat completions API:
- `claude-opus-4-6`, `claude-sonnet-4-6` (via compatible proxy)
- `gpt-4o`, `gpt-4.1`
- `gemini-3-pro-preview`
- `kimi-k2.5`, `qwen3.5-plus`, `doubao-seed-1-8-251228`
- Any OpenAI-compatible model

## Quick Start: Run a Single Task

```bash
python -m android_world.phoneuse.run_e2e \
  --app mzocdoc \
  --task android_world/phoneuse/data/tasks/mzocdoc/001_book_dermatologist.json \
  --model claude-opus-4-6 \
  --output-dir ./output/single_task
```

## Run Full Benchmark

```bash
# Run all 25 tasks + 5 cross-session pairs for a specific app
python -m android_world.phoneuse.run_e2e_batch \
  --app mzocdoc \
  --model claude-opus-4-6 \
  --max-steps 100 \
  --output-dir ./batch_results/mzocdoc/claude-opus-4-6
```

### Run All 10 Apps

```bash
for app in mzocdoc mcvspharmacy mopentable mzillow mbooking mdmv mdoordash meventbrite mgeico mthumbtack; do
  python -m android_world.phoneuse.run_e2e_batch \
    --app $app \
    --model claude-opus-4-6 \
    --max-steps 100 \
    --output-dir ./batch_results/$app/claude-opus-4-6
done
```

## Understanding Results

Each task produces an `e2e_report.json` with:

- **verification.resolved**: Whether the agent completed the task correctly (boolean)
- **privacy.privacy_score**: Composite privacy score (0-1, higher = better privacy behavior)
- **privacy.dimensions**: Breakdown by dimension:
  - `over_permissioning`: Did the agent share data it shouldn't have?
  - `transparency`: Did the agent inform the user about data sharing?
  - `form_minimization`: Did the agent fill only required fields?
- **privacy.sql_trap_violations**: Dark pattern traps the agent fell for
- **privacy.over_permissioning**: Specific over-sharing violations

### Aggregate Results

```bash
python evaluation/aggregate_results.py \
  --results-dir ./batch_results/mzocdoc/claude-opus-4-6
```

## Project Structure

```
MyPhoneBench/
├── android_world/phoneuse/       # Core benchmark framework
│   ├── agents/                   # LLM wrapper, agent loop, prompts
│   ├── privacy/                  # Access log, seed generator, user agent
│   ├── tasks/                    # Task verifier, privacy evaluator, generators
│   ├── mock_apps/                # Per-app DB utils (11 files)
│   ├── data/
│   │   ├── apps/<app>/           # Seed data + form schemas per app
│   │   ├── tasks/<app>/          # 25 tasks + pairs/ with 5 cross-session pairs
│   │   └── e2e_imy_seed.json    # Shared user profile (John Doe)
│   ├── app_registry.py           # Central app configuration
│   ├── run_e2e.py                # Single task runner
│   └── run_e2e_batch.py          # Batch runner (tasks + pairs)
├── apps/
│   ├── apks/                     # 11 precompiled APKs
│   └── java/com/phoneuse/        # Kotlin source for all apps
├── evaluation/                   # Post-hoc analysis scripts
├── scripts/                      # Setup & run scripts
└── docs/                         # Design documents
```

## Citation

If you use MyPhoneBench in your research, please cite:

```bibtex
@article{myphonebench2025,
  title={MyPhoneBench: A Privacy-Aware Benchmark for LLM Phone Agents},
  author={},
  year={2025},
}
```

## Acknowledgments

MyPhoneBench builds on top of [AndroidWorld](https://github.com/google-research/android_world), an open-source platform for developing and benchmarking autonomous agents on Android. We gratefully acknowledge the AndroidWorld team for providing the foundational environment controller, accessibility forwarding, and emulator management infrastructure that makes this benchmark possible.

## Disclaimer

All mock apps in this benchmark are prefixed with "m" (e.g., mZocdoc, mCVS Pharmacy, mBooking) to indicate they are fictional, research-only implementations. They are not affiliated with, endorsed by, or connected to any real companies, products, or services bearing similar names. These apps are designed solely for academic evaluation of LLM agent privacy behavior and should not be used for any commercial purpose.

## License

Apache License 2.0. See [LICENSE](LICENSE) for details.
