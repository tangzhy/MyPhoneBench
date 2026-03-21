"""Central app configuration registry for all MyPhoneBench mock apps.

Each entry maps an app key (e.g. 'mzocdoc') to its package name, DB path,
seed filename, mutable table key, and the module containing its utils.

Usage:
    from android_world.phoneuse.app_registry import APP_REGISTRY, get_app_config
    cfg = get_app_config('mzocdoc')
    load_fn = cfg['load_fn']  # callable(data, env) -> bool
"""

import importlib
from pathlib import Path
from typing import Any, Callable, Dict, List, Optional

APP_REGISTRY: Dict[str, Dict[str, Any]] = {
    'mzocdoc': {
        'package': 'com.phoneuse.mzocdoc',
        'db_path': '/data/data/com.phoneuse.mzocdoc/databases/mzocdoc.db',
        'seed_filename': 'mzocdoc_data.json',
        'mutable_key': 'appointments',
        'utils_module': 'android_world.phoneuse.mock_apps.mzocdoc_utils',
        'load_fn_name': 'load_mzocdoc_data',
        'display_name': 'mZocdoc',
    },
    'mcvspharmacy': {
        'package': 'com.phoneuse.mcvspharmacy',
        'db_path': '/data/data/com.phoneuse.mcvspharmacy/databases/mcvspharmacy.db',
        'seed_filename': 'mcvspharmacy_data.json',
        'mutable_key': 'bookings',
        'utils_module': 'android_world.phoneuse.mock_apps.mcvspharmacy_utils',
        'load_fn_name': 'load_mcvspharmacy_data',
        'display_name': 'mCVS Pharmacy',
    },
    'mopentable': {
        'package': 'com.phoneuse.mopentable',
        'db_path': '/data/data/com.phoneuse.mopentable/databases/mopentable.db',
        'seed_filename': 'mopentable_data.json',
        'mutable_key': 'reservations',
        'utils_module': 'android_world.phoneuse.mock_apps.mopentable_utils',
        'load_fn_name': 'load_mopentable_data',
        'display_name': 'mOpenTable',
    },
    'mzillow': {
        'package': 'com.phoneuse.mzillow',
        'db_path': '/data/data/com.phoneuse.mzillow/databases/mzillow.db',
        'seed_filename': 'mzillow_data.json',
        'mutable_key': 'viewing_appointments',
        'utils_module': 'android_world.phoneuse.mock_apps.mzillow_utils',
        'load_fn_name': 'load_mzillow_data',
        'display_name': 'mZillow',
    },
    'mbooking': {
        'package': 'com.phoneuse.mbooking',
        'db_path': '/data/data/com.phoneuse.mbooking/databases/mbooking.db',
        'seed_filename': 'mbooking_data.json',
        'mutable_key': 'reservations',
        'utils_module': 'android_world.phoneuse.mock_apps.mbooking_utils',
        'load_fn_name': 'load_mbooking_data',
        'display_name': 'mBooking',
    },
    'mdmv': {
        'package': 'com.phoneuse.mdmv',
        'db_path': '/data/data/com.phoneuse.mdmv/databases/mdmv.db',
        'seed_filename': 'mdmv_data.json',
        'mutable_key': 'appointments',
        'utils_module': 'android_world.phoneuse.mock_apps.mdmv_utils',
        'load_fn_name': 'load_mdmv_data',
        'display_name': 'mDMV',
    },
    'mdoordash': {
        'package': 'com.phoneuse.mdoordash',
        'db_path': '/data/data/com.phoneuse.mdoordash/databases/mdoordash.db',
        'seed_filename': 'mdoordash_data.json',
        'mutable_key': 'orders',
        'utils_module': 'android_world.phoneuse.mock_apps.mdoordash_utils',
        'load_fn_name': 'load_mdoordash_data',
        'display_name': 'mDoorDash',
    },
    'meventbrite': {
        'package': 'com.phoneuse.meventbrite',
        'db_path': '/data/data/com.phoneuse.meventbrite/databases/meventbrite.db',
        'seed_filename': 'meventbrite_data.json',
        'mutable_key': 'registrations',
        'utils_module': 'android_world.phoneuse.mock_apps.meventbrite_utils',
        'load_fn_name': 'load_meventbrite_data',
        'display_name': 'mEventbrite',
    },
    'mgeico': {
        'package': 'com.phoneuse.mgeico',
        'db_path': '/data/data/com.phoneuse.mgeico/databases/mgeico.db',
        'seed_filename': 'mgeico_data.json',
        'mutable_key': 'quotes',
        'utils_module': 'android_world.phoneuse.mock_apps.mgeico_utils',
        'load_fn_name': 'load_mgeico_data',
        'display_name': 'mGEICO',
    },
    'mthumbtack': {
        'package': 'com.phoneuse.mthumbtack',
        'db_path': '/data/data/com.phoneuse.mthumbtack/databases/mthumbtack.db',
        'seed_filename': 'mthumbtack_data.json',
        'mutable_key': 'bookings',
        'utils_module': 'android_world.phoneuse.mock_apps.mthumbtack_utils',
        'load_fn_name': 'load_mthumbtack_data',
        'display_name': 'mThumbtack',
    },
}

# iMy is special — it's the privacy manager, not a mock app
IMY_PACKAGE = 'com.phoneuse.imy'


def get_app_config(app_key: str) -> Dict[str, Any]:
    """Get config for an app by key. Raises KeyError if not found."""
    if app_key not in APP_REGISTRY:
        raise KeyError(
            f"Unknown app '{app_key}'. Available: {list(APP_REGISTRY.keys())}"
        )
    return APP_REGISTRY[app_key]


def get_all_app_keys() -> List[str]:
    """Return all registered app keys."""
    return list(APP_REGISTRY.keys())


def get_all_packages() -> List[str]:
    """Return all registered Android package names (including iMy)."""
    pkgs = [cfg['package'] for cfg in APP_REGISTRY.values()]
    pkgs.append(IMY_PACKAGE)
    return pkgs


def get_load_fn(app_key: str) -> Callable:
    """Dynamically import and return the load_<app>_data function."""
    cfg = get_app_config(app_key)
    mod = importlib.import_module(cfg['utils_module'])
    return getattr(mod, cfg['load_fn_name'])


def get_push_fn(app_key: str) -> Callable:
    """Dynamically import and return the push_<app>_data function from seed_generator."""
    from android_world.phoneuse.privacy import seed_generator
    fn_name = f"push_{app_key}_data"
    if hasattr(seed_generator, fn_name):
        return getattr(seed_generator, fn_name)
    # Fallback: use generic _push_app_data
    cfg = get_app_config(app_key)
    def _push(json_path: str) -> bool:
        return seed_generator._push_app_data(
            json_path, cfg['package'], cfg['seed_filename'],
        )
    return _push


def get_data_dir() -> Path:
    """Return the data/ directory path."""
    return Path(__file__).resolve().parent / 'data'


def get_seed_path(app_key: str) -> Optional[Path]:
    """Return the seed JSON path for an app, or None if not found."""
    data_dir = get_data_dir()
    # New layout: data/apps/<app>/seed.json
    p = data_dir / 'apps' / app_key / 'seed.json'
    if p.exists():
        return p
    # Legacy layout: data/e2e_<app>_seed.json
    p = data_dir / f'e2e_{app_key}_seed.json'
    if p.exists():
        return p
    return None


def get_form_schema_path(app_key: str) -> Optional[Path]:
    """Return the form schema path for an app, or None if not found."""
    data_dir = get_data_dir()
    # New layout: data/apps/<app>/form_schema.json
    p = data_dir / 'apps' / app_key / 'form_schema.json'
    if p.exists():
        return p
    return None


def get_tasks_dir(app_key: str) -> Optional[Path]:
    """Return the tasks directory for an app, or None if not found."""
    data_dir = get_data_dir()
    p = data_dir / 'tasks' / app_key
    if p.exists():
        return p
    return None


def get_imy_seed_path() -> Path:
    """Return the iMy base profile seed path."""
    return get_data_dir() / 'e2e_imy_seed.json'


def build_app_seed(
    app_key: str,
    base_catalog: Optional[Dict[str, Any]],
    task_override: Optional[Dict[str, Any]],
) -> Dict[str, Any]:
    """Build mutable seed for an app from base catalog + task overrides.

    The mutable_key from the registry determines which key holds the
    pre-existing records (appointments, bookings, reservations, etc.).
    """
    cfg = get_app_config(app_key)
    mutable_key = cfg['mutable_key']
    seed: Dict[str, Any] = {mutable_key: []}
    if task_override and mutable_key in task_override:
        seed[mutable_key] = task_override[mutable_key]
    return seed
