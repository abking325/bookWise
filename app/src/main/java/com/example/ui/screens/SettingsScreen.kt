package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.importer.BackupInspection
import com.example.localization.UiStrings
import com.example.model.AppLanguage
import com.example.model.AppSettings
import com.example.model.AppThemeMode
import com.example.model.DuplicateResolution
import com.example.model.NavigationStyle
import com.example.model.ReaderFont
import com.example.model.ReaderTheme
import com.example.tts.OfflineVoiceInfo
import com.example.ui.theme.PlayfairDisplayFamily
import com.example.viewmodel.StorageStats

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  settings: AppSettings,
  offlineVoices: List<OfflineVoiceInfo>,
  storageStats: StorageStats,
  pendingBackup: BackupInspection?,
  strings: UiStrings,
  onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
  onPreviewVoice: (voiceName: String, languageCode: String) -> Unit,
  onOpenVoiceDataSetup: () -> Unit,
  onExportLibraryZip: (android.net.Uri) -> Unit,
  onExportSettingsJson: (android.net.Uri) -> Unit,
  onInspectBackupUri: (android.net.Uri) -> Unit,
  onConfirmRestoreBackup: (DuplicateResolution, Boolean) -> Unit,
  onDismissPendingBackup: () -> Unit,
  onOpenImportBook: () -> Unit,
  onClearCache: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val exportLibraryLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
      if (uri != null) onExportLibraryZip(uri)
    }
  val exportSettingsLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
      if (uri != null) onExportSettingsJson(uri)
    }
  val restoreLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) onInspectBackupUri(uri)
    }

  val hasAmharicVoice = remember(offlineVoices) { offlineVoices.any { it.localeTag.startsWith("am", true) } }

  LazyColumn(
    modifier = modifier.fillMaxSize().testTag("settings_screen"),
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 104.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Text(
        text = strings.navSettings,
        fontFamily = PlayfairDisplayFamily,
        style = MaterialTheme.typography.headlineMedium,
      )
    }

    // Uninstall Backup Reminder Banner
    item {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            Icons.Filled.WarningAmber,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = strings.backupReminderBanner,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
          )
        }
      }
    }

    // 1. APPEARANCE
    item {
      SettingsSectionCard(title = strings.settingsAppearance) {
        Text("Theme", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          AppThemeMode.entries.forEach { mode ->
            FilterChip(
              selected = settings.themeMode == mode,
              onClick = { onUpdateSettings { it.copy(themeMode = mode) } },
              label = { Text(mode.labelEn) },
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Navigation Style", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          NavigationStyle.entries.forEach { style ->
            FilterChip(
              selected = settings.navigationStyle == style,
              onClick = { onUpdateSettings { it.copy(navigationStyle = style) } },
              label = { Text(style.labelEn) },
              modifier = Modifier.testTag("nav_style_${style.id}"),
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))
        SettingsSwitchRow(
          title = "3D Carousel Perspective Depth",
          checked = settings.carouselDepthEnabled,
          onCheckedChange = { v -> onUpdateSettings { it.copy(carouselDepthEnabled = v) } },
        )
        SettingsSwitchRow(
          title = "Realistic Book Page Thickness",
          checked = settings.carouselShowSpineThickness,
          onCheckedChange = { v -> onUpdateSettings { it.copy(carouselShowSpineThickness = v) } },
        )
      }
    }

    // 2. READING
    item {
      SettingsSectionCard(title = strings.settingsReading) {
        Text("Default Reader Font", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ReaderFont.entries.forEach { rf ->
            FilterChip(
              selected = settings.readerFont == rf,
              onClick = { onUpdateSettings { it.copy(readerFont = rf) } },
              label = { Text(rf.labelEn) },
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Default Reading Theme", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ReaderTheme.entries.forEach { rt ->
            FilterChip(
              selected = settings.readerTheme == rt,
              onClick = { onUpdateSettings { it.copy(readerTheme = rt) } },
              label = { Text(rt.labelEn) },
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
          "Default Font Size: ${settings.readerFontSizeSp.toInt()}sp",
          style = MaterialTheme.typography.labelLarge,
        )
        Slider(
          value = settings.readerFontSizeSp,
          onValueChange = { v -> onUpdateSettings { it.copy(readerFontSizeSp = v) } },
          valueRange = 14f..28f,
        )

        Text(
          "Line Spacing: ${String.format("%.2f", settings.readerLineHeightMultiplier)}x",
          style = MaterialTheme.typography.labelLarge,
        )
        Slider(
          value = settings.readerLineHeightMultiplier,
          onValueChange = { v -> onUpdateSettings { it.copy(readerLineHeightMultiplier = v) } },
          valueRange = 1.3f..2.1f,
        )

        Text(
          "Page Margins: ${settings.readerMarginDp.toInt()}dp",
          style = MaterialTheme.typography.labelLarge,
        )
        Slider(
          value = settings.readerMarginDp,
          onValueChange = { v -> onUpdateSettings { it.copy(readerMarginDp = v) } },
          valueRange = 10f..40f,
        )

        SettingsSwitchRow(
          title = "Auto-Save Reading Position",
          checked = settings.autoSaveReadingPosition,
          onCheckedChange = { v -> onUpdateSettings { it.copy(autoSaveReadingPosition = v) } },
        )
      }
    }

    // 3. AUDIO (OFFLINE TTS)
    item {
      SettingsSectionCard(title = strings.settingsAudio) {
        Text(
          text = "Only verified offline voices (no network required) are listed below.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (offlineVoices.isEmpty()) {
          Text(
            text = "No offline voices reported yet by Android TTS engine.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            offlineVoices.take(8).forEach { voice ->
              val selected = settings.ttsVoiceName == voice.name
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                  Modifier.fillMaxWidth()
                    .clickable { onUpdateSettings { it.copy(ttsVoiceName = voice.name) } }
                    .padding(vertical = 4.dp),
              ) {
                RadioButton(
                  selected = selected,
                  onClick = { onUpdateSettings { it.copy(ttsVoiceName = voice.name) } },
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${voice.displayLanguage} (${voice.genderHint})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                  )
                  Text(
                    text = "${voice.localeTag} • ${voice.qualityLabel} • Offline",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                OutlinedButton(
                  onClick = { onPreviewVoice(voice.name, voice.localeTag) },
                  modifier = Modifier.height(36.dp),
                ) {
                  Icon(Icons.Filled.PlayArrow, contentDescription = "Preview", modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Preview", style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }
        }

        if (!hasAmharicVoice) {
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "Amharic offline voice not detected on this device.",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = "Install offline speech packs via Android Text-to-Speech settings to enable Amharic audio.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              TextButton(onClick = onOpenVoiceDataSetup) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Install Offline Voice Data")
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
          "Reading Speed: ${String.format("%.2f", settings.ttsSpeechRate)}x",
          style = MaterialTheme.typography.labelLarge,
        )
        Slider(
          value = settings.ttsSpeechRate,
          onValueChange = { v -> onUpdateSettings { it.copy(ttsSpeechRate = v) } },
          valueRange = 0.6f..2.0f,
        )

        Text(
          "Voice Pitch: ${String.format("%.2f", settings.ttsPitch)}x",
          style = MaterialTheme.typography.labelLarge,
        )
        Slider(
          value = settings.ttsPitch,
          onValueChange = { v -> onUpdateSettings { it.copy(ttsPitch = v) } },
          valueRange = 0.7f..1.4f,
        )

        SettingsSwitchRow(
          title = "Double-Tap Paragraph to Read",
          checked = settings.ttsDoubleTapEnabled,
          onCheckedChange = { v -> onUpdateSettings { it.copy(ttsDoubleTapEnabled = v) } },
        )
        SettingsSwitchRow(
          title = "Background & Screen-Off Playback",
          checked = settings.ttsBackgroundPlaybackEnabled,
          onCheckedChange = { v -> onUpdateSettings { it.copy(ttsBackgroundPlaybackEnabled = v) } },
        )
      }
    }

    // 4. LANGUAGE
    item {
      SettingsSectionCard(title = strings.settingsLanguage) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          AppLanguage.entries.forEach { lang ->
            FilterChip(
              selected = settings.appLanguage == lang,
              onClick = { onUpdateSettings { it.copy(appLanguage = lang) } },
              label = { Text(lang.labelEn) },
              modifier = Modifier.testTag("app_lang_${lang.code}"),
            )
          }
        }
      }
    }

    // 5. DATA MANAGEMENT
    item {
      SettingsSectionCard(title = strings.settingsData) {
        val libMb = String.format("%.2f MB", storageStats.libraryBytes / (1024f * 1024f))
        val cacheKb = String.format("%.1f KB", storageStats.cacheBytes / 1024f)
        Text(
          text = "Offline Library Storage: $libMb • Temporary Cache: $cacheKb",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = { exportLibraryLauncher.launch("bookwise-library-backup.zip") },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_backup_library_button"),
          ) {
            Icon(Icons.Filled.Archive, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Backup Entire Library (ZIP)")
          }
          OutlinedButton(
            onClick = {
              restoreLauncher.launch(
                arrayOf("application/zip", "application/json", "text/html", "*/*")
              )
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_restore_backup_button"),
          ) {
            Icon(Icons.Filled.Restore, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Restore Backup / Migrate BookWise")
          }
          OutlinedButton(
            onClick = { exportSettingsLauncher.launch("bookwise-settings.json") },
            modifier = Modifier.fillMaxWidth().height(48.dp),
          ) {
            Icon(Icons.Filled.Download, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Export Settings (JSON)")
          }
          OutlinedButton(
            onClick = onOpenImportBook,
            modifier = Modifier.fillMaxWidth().height(48.dp),
          ) {
            Text("Import New Book (TXT / EPUB / PDF)")
          }
          OutlinedButton(
            onClick = onClearCache,
            modifier = Modifier.fillMaxWidth().height(48.dp),
          ) {
            Icon(Icons.Filled.CleaningServices, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Clear Temporary Cache")
          }
        }
      }
    }
  }

  // Backup Validation & Duplicate Conflict Resolution Dialog
  pendingBackup?.let { inspection ->
    var resolution by remember { mutableStateOf(DuplicateResolution.SKIP) }
    var restoreSettings by remember { mutableStateOf(inspection.settings != null) }

    AlertDialog(
      onDismissRequest = onDismissPendingBackup,
      title = { Text("Restore BookWise Backup") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            "Found ${inspection.entries.size} book(s) in archive.",
            style = MaterialTheme.typography.bodyMedium,
          )
          if (inspection.duplicateCount > 0) {
            Text(
              "${inspection.duplicateCount} book(s) already exist in your library. Choose how to handle duplicates:",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.secondary,
            )
            DuplicateResolution.entries.forEach { opt ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { resolution = opt },
              ) {
                RadioButton(selected = resolution == opt, onClick = { resolution = opt })
                Text(
                  when (opt) {
                    DuplicateResolution.SKIP -> "Skip existing books"
                    DuplicateResolution.REPLACE -> "Replace existing books"
                    DuplicateResolution.KEEP_BOTH -> "Keep both copies"
                  },
                  style = MaterialTheme.typography.bodyMedium,
                )
              }
            }
          }
          if (inspection.settings != null) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth().clickable { restoreSettings = !restoreSettings },
            ) {
              Checkbox(checked = restoreSettings, onCheckedChange = { restoreSettings = it })
              Text("Also restore application settings", style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      },
      confirmButton = {
        Button(onClick = { onConfirmRestoreBackup(resolution, restoreSettings) }) {
          Text("Restore Now")
        }
      },
      dismissButton = {
        TextButton(onClick = onDismissPendingBackup) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun SettingsSectionCard(
  title: String,
  content: @Composable () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = title,
        fontFamily = PlayfairDisplayFamily,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Spacer(modifier = Modifier.height(6.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
      Spacer(modifier = Modifier.height(10.dp))
      content()
    }
  }
}

@Composable
private fun SettingsSwitchRow(
  title: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(text = title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}
