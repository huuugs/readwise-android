package com.readwise.ai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.readwise.ai.model.AIProvider
import com.readwise.ai.viewmodel.AIConfigViewModel
import com.readwise.core.database.entity.AIConfigEntity

/**
 * AI Configuration Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIConfigScreen(
    viewModel: AIConfigViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val allConfigs by viewModel.allConfigs.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedProvider by remember { mutableStateOf(AIProvider.OPENAI) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Configuration") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Config")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Default Provider Section
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Default Provider",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        AIProvider.values().forEach { provider ->
                            ProviderOptionRow(
                                provider = provider,
                                isSelected = uiState.defaultProvider == provider,
                                isConfigured = provider in uiState.configuredProviders,
                                onClick = { viewModel.setDefaultProvider(provider) }
                            )
                        }
                    }
                }
            }

            // Saved Configurations
            item {
                Text(
                    text = "Saved Configurations",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (allConfigs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No configurations yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add a configuration to get started",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(allConfigs) { config ->
                    ConfigurationCard(
                        config = config,
                        isDefault = uiState.defaultProvider.name == config.service,
                        onDelete = { viewModel.deleteConfig(config) },
                        onSetDefault = { viewModel.setAsDefault(config) }
                    )
                }
            }
        }
    }

    // Add Configuration Dialog
    if (showAddDialog) {
        AddConfigDialog(
            onDismiss = { showAddDialog = false },
            onSave = { provider, apiKey ->
                viewModel.saveApiKey(provider, apiKey)
                showAddDialog = false
            }
        )
    }

    // Test Connection Result
    LaunchedEffect(uiState.testSuccess) {
        if (uiState.testSuccess == true) {
            // Show success message
        }
    }
}

@Composable
fun ProviderOptionRow(
    provider: AIProvider,
    isSelected: Boolean,
    isConfigured: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = provider.displayName,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.weight(1f))
        if (isConfigured) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Configured",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.AddCircleOutline,
                contentDescription = "Not configured",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ConfigurationCard(
    config: AIConfigEntity,
    isDefault: Boolean,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = config.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${config.service} - ${config.model}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isDefault) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "DEFAULT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!isDefault) {
                    TextButton(onClick = onSetDefault) {
                        Text("Set as Default")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
fun AddConfigDialog(
    onDismiss: () -> Unit,
    onSave: (AIProvider, String) -> Unit
) {
    var selectedProvider by remember { mutableStateOf(AIProvider.OPENAI) }
    var apiKey by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }
    var testSuccess by remember { mutableStateOf<Boolean?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add AI Configuration") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Provider:")
                Spacer(modifier = Modifier.height(8.dp))

                AIProvider.values().forEach { provider ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProvider = provider }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedProvider == provider,
                            onClick = { selectedProvider = provider }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(provider.displayName)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        testSuccess = null
                    },
                    label = { Text("API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (testSuccess == true) {
                    Text(
                        text = "API key is valid!",
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (testSuccess == false) {
                    Text(
                        text = "API key is invalid",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedProvider, apiKey) },
                enabled = apiKey.isNotEmpty()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
