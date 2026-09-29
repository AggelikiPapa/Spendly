package com.spendly.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.spendly.R
import com.spendly.SpendlyApplication
import com.spendly.ui.theme.AccentColor
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SpendlyBudgetWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(DpSize(120.dp, 110.dp), DpSize(250.dp, 110.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as SpendlyApplication
        val state = withContext(Dispatchers.IO) {
            try {
                BudgetWidgetDataProvider(app.transactionRepository, app.monthlyBudgetRepository).load()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                BudgetWidgetState.Error(YearMonth.now())
            }
        }
        val accent = app.accentStore.selected.value
        val darkTheme = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        provideContent { BudgetWidgetContent(state, accent, darkTheme) }
    }
}

@Composable
private fun BudgetWidgetContent(state: BudgetWidgetState, accent: AccentColor, darkTheme: Boolean) {
    val context = LocalContext.current
    val medium = LocalSize.current.width >= 250.dp
    Column(
        modifier = GlanceModifier.fillMaxSize()
            .background(ColorProvider(R.color.widget_background))
            .clickable(actionStartActivity(BudgetWidgetActions.dashboardIntent(context)))
            .padding(12.dp),
    ) {
        when (state) {
            is BudgetWidgetState.Ready -> if (medium) MediumBudget(state, accent, darkTheme)
                else SmallBudget(state, accent, darkTheme)
            is BudgetWidgetState.NoBudget -> {
                Heading("Spendly")
                Spacer(GlanceModifier.height(8.dp))
                Body(if (medium) "No budget set for ${BudgetWidgetStateFactory.monthLabel(state.month)}" else "No budget set")
                if (medium) {
                    Spacer(GlanceModifier.height(6.dp))
                    Secondary("Tap to configure")
                }
            }
            is BudgetWidgetState.Error -> {
                Heading("Spendly")
                Spacer(GlanceModifier.height(8.dp))
                Body("Unable to update")
            }
        }
    }
}

@Composable
private fun SmallBudget(state: BudgetWidgetState.Ready, accent: AccentColor, darkTheme: Boolean) {
    Heading(BudgetWidgetStateFactory.monthLabel(state.month))
    Spacer(GlanceModifier.height(4.dp))
    Emphasis(state.percentageLabel, state, accent, darkTheme, large = true)
    if (state.percentageUsed != null) Secondary("spent")
    Spacer(GlanceModifier.height(4.dp))
    Secondary(state.smallBalanceLabel)
}

@Composable
private fun MediumBudget(state: BudgetWidgetState.Ready, accent: AccentColor, darkTheme: Boolean) {
    Heading(BudgetWidgetStateFactory.monthLabel(state.month))
    Spacer(GlanceModifier.height(4.dp))
    Body(state.spentLimitLabel)
    Spacer(GlanceModifier.height(8.dp))
    LinearProgressIndicator(
        progress = state.visualProgress,
        modifier = GlanceModifier.fillMaxWidth(),
        color = ColorProvider(WidgetEmphasisColor.resolve(accent, state.isOverBudget, darkTheme)),
        backgroundColor = ColorProvider(R.color.widget_track),
    )
    Spacer(GlanceModifier.height(6.dp))
    Emphasis(if (state.percentageUsed == null) state.percentageLabel else "${state.percentageLabel} spent",
        state, accent, darkTheme)
    Secondary(state.balanceLabel)
}

@Composable
private fun Emphasis(value: String, state: BudgetWidgetState.Ready, accent: AccentColor, darkTheme: Boolean,
    large: Boolean = false) {
    Text(value, style = TextStyle(
        color = ColorProvider(WidgetEmphasisColor.resolve(accent, state.isOverBudget, darkTheme)),
        fontSize = if (state.percentageUsed == null) 16.sp else if (large) 28.sp else 24.sp,
        fontWeight = FontWeight.Bold,
    ))
}

@Composable
private fun Heading(value: String) {
    Text(value, style = TextStyle(color = ColorProvider(R.color.widget_text), fontSize = 15.sp, fontWeight = FontWeight.Bold))
}

@Composable
private fun Body(value: String) {
    Text(value, style = TextStyle(color = ColorProvider(R.color.widget_text), fontSize = 14.sp))
}

@Composable
private fun Secondary(value: String) {
    Text(value, style = TextStyle(color = ColorProvider(R.color.widget_secondary), fontSize = 12.sp))
}
