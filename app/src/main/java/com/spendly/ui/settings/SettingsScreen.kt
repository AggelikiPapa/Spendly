package com.spendly.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.spendly.R

@Composable
fun SettingsScreen(contentPadding: PaddingValues) {
    Text(
        text = stringResource(R.string.settings_placeholder),
        modifier = Modifier.padding(contentPadding).padding(24.dp),
    )
}
