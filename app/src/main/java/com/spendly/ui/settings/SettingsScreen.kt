package com.spendly.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.spendly.R

@Composable
fun SettingsScreen(contentPadding: PaddingValues, onOpenCategories: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Text(
            text = stringResource(R.string.settings),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(24.dp),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.categories)) },
            supportingContent = { Text(stringResource(R.string.manage_categories)) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenCategories),
        )
    }
}
