package com.spendly.ui.settings.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendly.R
import com.spendly.domain.model.Category

@Composable
fun CategoryManagementScreen(
    viewModel: CategoryManagementViewModel,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val busy = state.busyCategoryId != null || state.editor?.isSaving == true
    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Text(stringResource(R.string.categories), style = MaterialTheme.typography.headlineSmall)
            }
            Button(onClick = viewModel::startAdd, enabled = !busy && !state.isLoading && state.loadError == null) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(stringResource(R.string.add_category))
            }
        }
        state.operationError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 24.dp))
        }
        when {
            state.isLoading -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            state.loadError != null -> Text(
                state.loadError.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(24.dp),
            )
            else -> {
                val active = state.categories.filter { it.isActive }.sortedBy { it.name.lowercase() }
                val inactive = state.categories.filterNot { it.isActive }.sortedBy { it.name.lowercase() }
                LazyColumn {
                    item { SectionHeading(stringResource(R.string.active_categories)) }
                    if (active.isEmpty()) item { EmptySection() }
                    items(active, key = { it.id }) { category ->
                        CategoryRow(category, busy, viewModel::startRename, viewModel::toggleActive)
                    }
                    item { SectionHeading(stringResource(R.string.inactive_categories)) }
                    if (inactive.isEmpty()) item { EmptySection() }
                    items(inactive, key = { it.id }) { category ->
                        CategoryRow(category, busy, viewModel::startRename, viewModel::toggleActive)
                    }
                }
            }
        }
    }

    state.editor?.let { editor ->
        AlertDialog(
            onDismissRequest = viewModel::dismissEditor,
            title = { Text(stringResource(if (editor.categoryId == null) R.string.add_category else R.string.rename_category)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editor.nameInput,
                        onValueChange = viewModel::changeName,
                        label = { Text(stringResource(R.string.category_name)) },
                        singleLine = true,
                        isError = editor.nameError != null,
                        enabled = !editor.isSaving,
                    )
                    editor.nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    editor.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::saveEditor, enabled = !editor.isSaving) {
                    Text(stringResource(if (editor.isSaving) R.string.saving else R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissEditor, enabled = !editor.isSaving) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionHeading(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun EmptySection() {
    Text(stringResource(R.string.no_categories_in_section), modifier = Modifier.padding(horizontal = 24.dp))
}

@Composable
private fun CategoryRow(
    category: Category,
    busy: Boolean,
    onRename: (Long) -> Unit,
    onToggle: (Long) -> Unit,
) {
    ListItem(
        headlineContent = { Text(category.name) },
        supportingContent = {
            Text(stringResource(if (category.isSystem) R.string.system_category else R.string.custom_category))
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!category.isSystem) {
                    TextButton(onClick = { onRename(category.id) }, enabled = !busy) {
                        Text(stringResource(R.string.rename))
                    }
                }
                Switch(
                    checked = category.isActive,
                    onCheckedChange = { onToggle(category.id) },
                    enabled = !busy,
                )
            }
        },
    )
}
