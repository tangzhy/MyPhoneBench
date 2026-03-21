"""Utilities for interacting with mDoorDash app database."""

import dataclasses
from typing import List, Optional, Dict, Any
from android_world.env import adb_utils
from android_world.env import interface

MDOORDASH_DB_PATH = "/data/data/com.phoneuse.mdoordash/databases/mdoordash.db"

_db_ensured = False


def _ensure_mdoordash_db(env: interface.AsyncEnv) -> None:
    """Ensure mDoorDash DB directory and tables exist (idempotent)."""
    global _db_ensured
    if _db_ensured:
        return
    db_dir = "/data/data/com.phoneuse.mdoordash/databases"
    adb_utils.issue_generic_request(["shell", f"mkdir -p {db_dir}"], env)

    create_stmts = [
        (
            "CREATE TABLE IF NOT EXISTS orders ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            "restaurant_id INTEGER NOT NULL, "
            "customer_name TEXT NOT NULL, "
            "customer_phone TEXT, "
            "customer_dob TEXT, "
            "delivery_address TEXT, "
            "occupation TEXT, "
            "customer_email TEXT, "
            "meal_plan_provider TEXT, "
            "meal_plan_id TEXT, "
            "alternate_contact_name TEXT, "
            "alternate_contact_phone TEXT, "
            "special_instructions TEXT NOT NULL, "
            "order_items TEXT, "
            "order_total REAL, "
            "delivery_time TEXT NOT NULL, "
            "status TEXT DEFAULT 'confirmed', "
            "created_at TEXT DEFAULT (datetime('now')), "
            "priority_phone TEXT, "
            "tracking_email TEXT);"
        ),
        (
            "CREATE TABLE IF NOT EXISTS form_drafts ("
            "restaurant_id INTEGER NOT NULL, "
            "delivery_time TEXT NOT NULL, "
            "customer_name TEXT, "
            "customer_phone TEXT, "
            "customer_dob TEXT, "
            "delivery_address TEXT, "
            "occupation TEXT, "
            "customer_email TEXT, "
            "meal_plan_provider TEXT, "
            "meal_plan_id TEXT, "
            "alternate_contact_name TEXT, "
            "alternate_contact_phone TEXT, "
            "special_instructions TEXT, "
            "priority_phone TEXT, "
            "tracking_email TEXT, "
            "updated_at TEXT, "
            "PRIMARY KEY (restaurant_id, delivery_time));"
        ),
    ]
    for stmt in create_stmts:
        adb_utils.execute_sql_command(MDOORDASH_DB_PATH, stmt, env)

    adb_utils.issue_generic_request(
        ["shell",
         "owner=$(stat -c '%u:%g' /data/data/com.phoneuse.mdoordash) && "
         f"chown -R $owner {db_dir}"],
        env,
    )
    _db_ensured = True


@dataclasses.dataclass(frozen=True)
class Order:
    """Order data class matching the database schema."""
    id: int
    restaurant_id: int
    customer_name: str
    customer_phone: Optional[str] = None
    customer_dob: Optional[str] = None
    delivery_address: Optional[str] = None
    occupation: Optional[str] = None
    customer_email: Optional[str] = None
    meal_plan_provider: Optional[str] = None
    meal_plan_id: Optional[str] = None
    alternate_contact_name: Optional[str] = None
    alternate_contact_phone: Optional[str] = None
    special_instructions: str = ""
    order_items: Optional[str] = None
    order_total: Optional[float] = None
    delivery_time: str = ""
    status: str = "confirmed"
    created_at: str = ""
    priority_phone: Optional[str] = None
    tracking_email: Optional[str] = None


def get_orders(env: interface.AsyncEnv) -> List[Order]:
    """Get all orders from mDoorDash database."""
    query = "SELECT * FROM orders ORDER BY created_at DESC;"
    response = adb_utils.execute_sql_command(MDOORDASH_DB_PATH, query, env)

    orders = []
    if response.output:
        for line in response.output.strip().split('\n'):
            if '|' in line:
                parts = line.split('|')
                if len(parts) >= 16:
                    orders.append(Order(
                        id=int(parts[0].strip()) if parts[0].strip() else 0,
                        restaurant_id=int(parts[1].strip()) if parts[1].strip() else 0,
                        customer_name=parts[2].strip() if parts[2].strip() else "",
                        customer_phone=parts[3].strip() if parts[3].strip() else None,
                        customer_dob=parts[4].strip() if parts[4].strip() else None,
                        delivery_address=parts[5].strip() if parts[5].strip() else None,
                        occupation=parts[6].strip() if parts[6].strip() else None,
                        customer_email=parts[7].strip() if parts[7].strip() else None,
                        meal_plan_provider=parts[8].strip() if parts[8].strip() else None,
                        meal_plan_id=parts[9].strip() if parts[9].strip() else None,
                        alternate_contact_name=parts[10].strip() if parts[10].strip() else None,
                        alternate_contact_phone=parts[11].strip() if parts[11].strip() else None,
                        special_instructions=parts[12].strip() if parts[12].strip() else "",
                        order_items=parts[13].strip() if parts[13].strip() else None,
                        order_total=float(parts[14].strip()) if parts[14].strip() else None,
                        delivery_time=parts[15].strip() if parts[15].strip() else "",
                        status=parts[16].strip() if len(parts) > 16 and parts[16].strip() else "confirmed",
                        created_at=parts[17].strip() if len(parts) > 17 and parts[17].strip() else "",
                        priority_phone=parts[18].strip() if len(parts) > 18 and parts[18].strip() else None,
                        tracking_email=parts[19].strip() if len(parts) > 19 and parts[19].strip() else None,
                    ))
    return orders


def get_orders_by_restaurant(restaurant_id: int, env: interface.AsyncEnv) -> List[Order]:
    """Get orders for a specific restaurant."""
    query = f"SELECT * FROM orders WHERE restaurant_id = {restaurant_id} ORDER BY delivery_time ASC;"
    response = adb_utils.execute_sql_command(MDOORDASH_DB_PATH, query, env)

    orders = []
    if response.output:
        for line in response.output.strip().split('\n'):
            if '|' in line:
                parts = line.split('|')
                if len(parts) >= 16:
                    orders.append(Order(
                        id=int(parts[0].strip()) if parts[0].strip() else 0,
                        restaurant_id=int(parts[1].strip()) if parts[1].strip() else 0,
                        customer_name=parts[2].strip() if parts[2].strip() else "",
                        customer_phone=parts[3].strip() if parts[3].strip() else None,
                        customer_dob=parts[4].strip() if parts[4].strip() else None,
                        delivery_address=parts[5].strip() if parts[5].strip() else None,
                        occupation=parts[6].strip() if parts[6].strip() else None,
                        customer_email=parts[7].strip() if parts[7].strip() else None,
                        meal_plan_provider=parts[8].strip() if parts[8].strip() else None,
                        meal_plan_id=parts[9].strip() if parts[9].strip() else None,
                        alternate_contact_name=parts[10].strip() if parts[10].strip() else None,
                        alternate_contact_phone=parts[11].strip() if parts[11].strip() else None,
                        special_instructions=parts[12].strip() if parts[12].strip() else "",
                        order_items=parts[13].strip() if parts[13].strip() else None,
                        order_total=float(parts[14].strip()) if parts[14].strip() else None,
                        delivery_time=parts[15].strip() if parts[15].strip() else "",
                        status=parts[16].strip() if len(parts) > 16 and parts[16].strip() else "confirmed",
                        created_at=parts[17].strip() if len(parts) > 17 and parts[17].strip() else "",
                        priority_phone=parts[18].strip() if len(parts) > 18 and parts[18].strip() else None,
                        tracking_email=parts[19].strip() if len(parts) > 19 and parts[19].strip() else None,
                    ))
    return orders


def clear_orders(env: interface.AsyncEnv) -> None:
    """Clear all orders from the database."""
    _ensure_mdoordash_db(env)
    query = "DELETE FROM orders;"
    adb_utils.execute_sql_command(MDOORDASH_DB_PATH, query, env)


def clear_form_drafts(env: interface.AsyncEnv) -> None:
    """Clear all form drafts from the database."""
    query = "DELETE FROM form_drafts;"
    try:
        adb_utils.execute_sql_command(MDOORDASH_DB_PATH, query, env)
    except Exception:
        pass


def load_mdoordash_data(data: Dict[str, Any], env: interface.AsyncEnv) -> bool:
    """Load mDoorDash seed data into the device database.

    Clears all existing orders, then inserts any pre-loaded orders
    from data["orders"] (if present).
    """
    try:
        clear_orders(env)
        clear_form_drafts(env)

        for order in data.get('orders', []):
            restaurant_id = order.get('restaurant_id', 0)
            customer_name = order.get('customer_name', '').replace("'", "''")
            customer_phone = order.get('customer_phone', '').replace("'", "''")
            customer_dob = order.get('customer_dob', '').replace("'", "''")
            delivery_address = order.get('delivery_address', '').replace("'", "''")
            occupation = order.get('occupation', '').replace("'", "''")
            customer_email = order.get('customer_email', '').replace("'", "''")
            meal_plan_provider = order.get('meal_plan_provider', '').replace("'", "''")
            meal_plan_id = order.get('meal_plan_id', '').replace("'", "''")
            alternate_contact_name = order.get('alternate_contact_name', '').replace("'", "''")
            alternate_contact_phone = order.get('alternate_contact_phone', '').replace("'", "''")
            special_instructions = order.get('special_instructions', '').replace("'", "''")
            order_items = order.get('order_items', '').replace("'", "''")
            order_total = order.get('order_total', 0.0)
            delivery_time = order.get('delivery_time', '').replace("'", "''")
            status = order.get('status', 'confirmed').replace("'", "''")

            query = (
                "INSERT INTO orders "
                "(restaurant_id, customer_name, customer_phone, customer_dob, "
                "delivery_address, occupation, customer_email, "
                "meal_plan_provider, meal_plan_id, "
                "alternate_contact_name, alternate_contact_phone, "
                "special_instructions, order_items, order_total, "
                "delivery_time, status) VALUES "
                f"({restaurant_id}, '{customer_name}', '{customer_phone}', '{customer_dob}', "
                f"'{delivery_address}', '{occupation}', '{customer_email}', "
                f"'{meal_plan_provider}', '{meal_plan_id}', "
                f"'{alternate_contact_name}', '{alternate_contact_phone}', "
                f"'{special_instructions}', '{order_items}', {order_total}, "
                f"'{delivery_time}', '{status}');"
            )
            adb_utils.execute_sql_command(MDOORDASH_DB_PATH, query, env)

        return True
    except Exception as e:
        print(f"Error loading mDoorDash data: {e}")
        return False
