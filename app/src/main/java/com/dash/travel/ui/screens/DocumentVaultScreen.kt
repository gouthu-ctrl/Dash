package com.dash.travel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dash.travel.data.model.DocCategory
import com.dash.travel.data.model.DocumentVaultItem
import com.dash.travel.ui.components.DashCard
import com.dash.travel.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentVaultScreen(
    documents: List<DocumentVaultItem>,
    onNavigateBack: () -> Unit,
    onUploadClick: () -> Unit, // In real app, passes file picker trigger
    onDocumentClick: (DocumentVaultItem) -> Unit
) {
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Secure Vault", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onUploadClick) {
                        Icon(Icons.Default.Add, contentDescription = "Upload")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = OnBackground,
                    navigationIconContentColor = OnBackground,
                    actionIconContentColor = OnBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onUploadClick,
                containerColor = Primary,
                contentColor = OnPrimary
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = "Upload")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Categories
            DocCategory.values().forEach { category ->
                val categoryDocs = documents.filter { it.docType == category }
                if (categoryDocs.isNotEmpty()) {
                    item {
                        Text(
                            category.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleSmall,
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )
                    }
                    items(categoryDocs) { doc ->
                        DocumentItem(doc, onClick = { onDocumentClick(doc) })
                    }
                }
            }
            
            if (documents.isEmpty()) {
                item {
                    EmptyVaultState()
                }
            }
        }
    }
}

@Composable
fun DocumentItem(doc: DocumentVaultItem, onClick: () -> Unit) {
    DashCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Primary.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        getIconForDocType(doc.docType),
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    doc.fileName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (doc.expiryDate != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Expires: ${doc.expiryDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color(0xFFEF4444)
                    )
                }
            }
            
            if (doc.isEncrypted) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Encrypted",
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyVaultState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = OnSurfaceVariant.copy(alpha=0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Your Vault is Empty",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OnSurface
        )
        Text(
            "Upload passports, tickets, and visas for secure access.",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

fun getIconForDocType(type: DocCategory): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        DocCategory.PASSPORT -> Icons.Default.Badge
        DocCategory.VISA -> Icons.Default.Public
        DocCategory.TICKET -> Icons.Default.ConfirmationNumber
        DocCategory.INSURANCE -> Icons.Default.HealthAndSafety
        DocCategory.OTHER -> Icons.Default.Description
    }
}
