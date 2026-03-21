package com.phoneuse.mthumbtack.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phoneuse.mthumbtack.data.ProDemo

@Composable
fun ConfirmationScreen(
    pro: ProDemo,
    appointmentTime: String,
    onDone: () -> Unit,
    onViewRequests: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "✓",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Service Requested!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your request has been sent to ${pro.name}",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Service: ${pro.serviceType}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Scheduled: $appointmentTime",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Done button" }
                .testTag("done_button")
        ) {
            Text("Done")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onViewRequests,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "My Requests button" }
                .testTag("view_requests_button")
        ) {
            Text("My Requests")
        }
    }
}
