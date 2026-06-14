package com.scanni.app.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.scanni.app.BuildConfig
import com.scanni.app.R
import com.scanni.app.di.AppGraph
import com.scanni.app.domain.model.AppSettings
import com.scanni.app.domain.model.OcrScript
import com.scanni.app.domain.model.ThemeMode
import com.scanni.app.domain.repo.SettingsRepository
import com.scanni.app.ui.common.graphViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }

    fun setDynamicColor(enabled: Boolean) =
        viewModelScope.launch { repository.setDynamicColor(enabled) }

    fun setAutoCapture(enabled: Boolean) =
        viewModelScope.launch { repository.setAutoCapture(enabled) }

    fun setOcrScript(script: OcrScript) =
        viewModelScope.launch { repository.setOcrScript(script) }
}

@Composable
fun SettingsScreen(
    graph: AppGraph,
    onBack: () -> Unit,
) {
    val viewModel = graphViewModel { SettingsViewModel(graph.settingsRepository) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back))
                }
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionLabel(stringResource(R.string.settings_section_appearance))
            RadioRow(
                label = stringResource(R.string.settings_theme_system),
                selected = settings.themeMode == ThemeMode.SYSTEM,
            ) { viewModel.setThemeMode(ThemeMode.SYSTEM) }
            RadioRow(
                label = stringResource(R.string.settings_theme_light),
                selected = settings.themeMode == ThemeMode.LIGHT,
            ) { viewModel.setThemeMode(ThemeMode.LIGHT) }
            RadioRow(
                label = stringResource(R.string.settings_theme_dark),
                selected = settings.themeMode == ThemeMode.DARK,
            ) { viewModel.setThemeMode(ThemeMode.DARK) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchRow(
                    label = stringResource(R.string.settings_dynamic_color),
                    description = stringResource(R.string.settings_dynamic_color_desc),
                    checked = settings.dynamicColor,
                    onToggle = viewModel::setDynamicColor,
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionLabel(stringResource(R.string.settings_section_scanning))
            SwitchRow(
                label = stringResource(R.string.settings_auto_capture),
                description = stringResource(R.string.settings_auto_capture_desc),
                checked = settings.autoCapture,
                onToggle = viewModel::setAutoCapture,
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionLabel(stringResource(R.string.settings_section_ocr))
            RadioRow(
                label = stringResource(R.string.settings_ocr_latin),
                description = stringResource(R.string.settings_ocr_latin_desc),
                selected = settings.ocrScript == OcrScript.LATIN,
            ) { viewModel.setOcrScript(OcrScript.LATIN) }
            RadioRow(
                label = stringResource(R.string.settings_ocr_arabic),
                description = stringResource(R.string.settings_ocr_arabic_desc),
                selected = settings.ocrScript == OcrScript.ARABIC,
            ) { viewModel.setOcrScript(OcrScript.ARABIC) }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionLabel(stringResource(R.string.settings_section_about))
            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME))
                },
                supportingContent = { Text(stringResource(R.string.settings_about_blurb)) },
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    description: String? = null,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = description?.let { { Text(it) } },
        trailingContent = { RadioButton(selected = selected, onClick = onClick) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun SwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(description) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onToggle) },
        modifier = Modifier.clickable { onToggle(!checked) },
    )
}
