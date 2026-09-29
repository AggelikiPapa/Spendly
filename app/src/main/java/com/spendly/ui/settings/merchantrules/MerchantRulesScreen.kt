package com.spendly.ui.settings.merchantrules

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendly.R

@Composable
fun MerchantRulesScreen(viewModel: MerchantRulesViewModel, contentPadding: PaddingValues, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 16.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
            Text(stringResource(R.string.merchant_rules), style = MaterialTheme.typography.headlineSmall)
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(24.dp)) }
        when {
            state.isLoading -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            state.rows.isEmpty() && state.error == null -> Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.no_merchant_rules), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.no_merchant_rules_hint))
            }
            else -> LazyColumn {
                items(state.rows, key = { it.rule.id }) { row ->
                    val categoryLabel = when {
                        row.categoryName == null -> stringResource(R.string.category_unavailable)
                        row.isCategoryInactive -> stringResource(R.string.category_inactive, row.categoryName)
                        else -> row.categoryName
                    }
                    ListItem(
                        headlineContent = { Text(row.rule.merchantPattern) },
                        supportingContent = { Text(categoryLabel) },
                        trailingContent = {
                            TextButton(onClick = { viewModel.requestDelete(row.rule.id) }, enabled = !state.isSaving,
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                Text(stringResource(R.string.delete))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !state.isSaving) { viewModel.openEditor(row.rule.id) },
                    )
                }
            }
        }
    }

    state.editor?.let { editor ->
        var expanded by remember(editor.ruleId) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = viewModel::dismissEditor,
            title = { Text(stringResource(R.string.change_rule_category)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(editor.merchant)
                    OutlinedButton(onClick = { expanded = true }, enabled = !state.isSaving && state.activeCategories.isNotEmpty()) {
                        Text(state.activeCategories.firstOrNull { it.id == editor.categoryId }?.name ?: stringResource(R.string.choose_category))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        state.activeCategories.forEach { category ->
                            DropdownMenuItem(text = { Text(category.name) }, onClick = {
                                viewModel.selectCategory(category.id)
                                expanded = false
                            })
                        }
                    }
                    if (state.activeCategories.isEmpty()) Text(stringResource(R.string.no_active_categories))
                    editor.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::saveEditor, enabled = !state.isSaving) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissEditor, enabled = !state.isSaving) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (state.deleteRuleId != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text(stringResource(R.string.delete_merchant_rule)) },
            text = { Text(stringResource(R.string.delete_merchant_rule_message)) },
            confirmButton = { TextButton(onClick = viewModel::confirmDelete, enabled = !state.isSaving,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(stringResource(R.string.delete))
            } },
            dismissButton = { TextButton(onClick = viewModel::cancelDelete, enabled = !state.isSaving) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
