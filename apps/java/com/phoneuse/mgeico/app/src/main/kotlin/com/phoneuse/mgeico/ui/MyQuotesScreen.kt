package com.phoneuse.mgeico.ui

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
import com.phoneuse.mgeico.data.InsurancePlan
import android.database.sqlite.SQLiteDatabase
import java.io.File

data class QuoteItem(
    val id: Int,
    val planId: Int,
    val planName: String,
    val coverageType: String,
    val provider: String,
    val vehicleMakeModel: String,
    val coverageNotes: String,
    val applicantName: String
)

@Composable
fun MyQuotesScreen(
    plans: List<InsurancePlan>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var quotes by remember { mutableStateOf<List<QuoteItem>>(emptyList()) }

    LaunchedEffect(plans) {
        try {
            val dbDir = context.getDatabasePath("mgeico.db").parent
            val dbPath = if (dbDir != null) {
                File(dbDir, "mgeico.db")
            } else {
                File("/data/data/com.phoneuse.mgeico/databases/mgeico.db")
            }
            if (dbPath.exists()) {
                val db = SQLiteDatabase.openDatabase(dbPath.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                val cursor = db.rawQuery(
                    "SELECT id, plan_id, applicant_name, vehicle_make_model, coverage_notes FROM quotes ORDER BY id DESC",
                    null
                )
                val quoteList = mutableListOf<QuoteItem>()
                while (cursor.moveToNext()) {
                    val id = cursor.getInt(0)
                    val planId = cursor.getInt(1)
                    val applicantName = cursor.getString(2) ?: ""
                    val vehicleMakeModel = cursor.getString(3) ?: ""
                    val coverageNotes = cursor.getString(4) ?: ""
                    val plan = plans.find { it.id == planId }
                    quoteList.add(
                        QuoteItem(
                            id = id,
                            planId = planId,
                            planName = plan?.planName ?: "Plan #$planId",
                            coverageType = plan?.coverageType ?: "",
                            provider = plan?.provider ?: "",
                            vehicleMakeModel = vehicleMakeModel,
                            coverageNotes = coverageNotes,
                            applicantName = applicantName
                        )
                    )
                }
                cursor.close()
                db.close()
                quotes = quoteList
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
                text = "My Quotes",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (quotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "No quotes yet", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Get a quote to see it here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quotes) { quote ->
                    QuoteCard(
                        quote = quote,
                        onCancel = {
                            try {
                                val dbDir = context.getDatabasePath("mgeico.db").parent
                                val dbPath = if (dbDir != null) {
                                    File(dbDir, "mgeico.db")
                                } else {
                                    File("/data/data/com.phoneuse.mgeico/databases/mgeico.db")
                                }
                                val db = SQLiteDatabase.openDatabase(dbPath.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
                                db.execSQL("DELETE FROM quotes WHERE id = ?", arrayOf(quote.id.toString()))
                                db.close()
                                quotes = quotes.filter { it.id != quote.id }
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
fun QuoteCard(
    quote: QuoteItem,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Quote card: ${quote.planName}, ${quote.vehicleMakeModel}" }
            .testTag("quote_card_${quote.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quote.planName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = quote.coverageType,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = quote.provider, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .semantics { contentDescription = "Cancel quote button" }
                        .testTag("cancel_quote_${quote.id}")
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
                        text = "Vehicle",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = quote.vehicleMakeModel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        text = "Notes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(text = quote.coverageNotes, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
