package com.phoneuse.mdoordash.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phoneuse.mdoordash.data.DataLoader
import com.phoneuse.mdoordash.data.RestaurantDemo
import android.database.sqlite.SQLiteDatabase
import java.io.File

data class OrderItem(
    val id: Int,
    val restaurantId: Int,
    val restaurantName: String,
    val cuisine: String,
    val neighborhood: String,
    val deliveryTime: String,
    val specialInstructions: String,
    val customerName: String,
    val orderItems: String
)

@Composable
fun MyOrdersScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var orders by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    var restaurants by remember { mutableStateOf<List<RestaurantDemo>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            restaurants = DataLoader.loadAllFromJson(context)
        } catch (e: Exception) {
            restaurants = DataLoader.getDefaultRestaurants()
        }
    }

    LaunchedEffect(restaurants) {
        try {
            val dbDir = context.getDatabasePath("mdoordash.db").parent
            val dbPath = if (dbDir != null) {
                File(dbDir, "mdoordash.db")
            } else {
                File("/data/data/com.phoneuse.mdoordash/databases/mdoordash.db")
            }
            if (dbPath.exists()) {
                val db = SQLiteDatabase.openDatabase(dbPath.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                val cursor = db.rawQuery(
                    "SELECT id, restaurant_id, customer_name, special_instructions, delivery_time, order_items FROM orders ORDER BY delivery_time ASC",
                    null
                )

                val orderList = mutableListOf<OrderItem>()
                while (cursor.moveToNext()) {
                    val id = cursor.getInt(0)
                    val restaurantId = cursor.getInt(1)
                    val customerName = cursor.getString(2) ?: ""
                    val specialInstructions = cursor.getString(3) ?: ""
                    val deliveryTime = cursor.getString(4) ?: ""
                    val orderItems = cursor.getString(5) ?: ""

                    val restaurant = restaurants.find { it.id == restaurantId }
                    if (restaurant != null) {
                        orderList.add(
                            OrderItem(
                                id = id,
                                restaurantId = restaurantId,
                                restaurantName = restaurant.name,
                                cuisine = restaurant.cuisine,
                                neighborhood = restaurant.neighborhood,
                                deliveryTime = deliveryTime,
                                specialInstructions = specialInstructions,
                                customerName = customerName,
                                orderItems = orderItems
                            )
                        )
                    }
                }
                cursor.close()
                db.close()
                orders = orderList
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = "Back button" }
            ) {
                Text("\u2190")
            }
            Text(
                text = "My Orders",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No orders yet",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Place an order to see it here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(orders) { order ->
                    OrderCard(
                        order = order,
                        onCancel = {
                            try {
                                val dbDir = context.getDatabasePath("mdoordash.db").parent
                                val dbPath = if (dbDir != null) {
                                    File(dbDir, "mdoordash.db")
                                } else {
                                    File("/data/data/com.phoneuse.mdoordash/databases/mdoordash.db")
                                }
                                val db = SQLiteDatabase.openDatabase(dbPath.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
                                db.execSQL("DELETE FROM orders WHERE id = ?", arrayOf(order.id.toString()))
                                db.close()
                                orders = orders.filter { it.id != order.id }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun OrderCard(
    order: OrderItem,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Order card: ${order.restaurantName}, ${order.deliveryTime}" }
            .testTag("order_card_${order.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = order.restaurantName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = order.cuisine,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = order.neighborhood,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .semantics { contentDescription = "Cancel order button" }
                        .testTag("cancel_order_${order.id}")
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.error)
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Delivery Time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = order.deliveryTime,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        text = "Instructions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = order.specialInstructions,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
