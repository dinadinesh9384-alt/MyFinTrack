package com.myfintrack.app.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myfintrack.app.components.*
import com.myfintrack.app.data.SettingsRepository
import com.myfintrack.app.model.AppSettings
import com.myfintrack.app.model.ThemeMode
import com.myfintrack.app.theme.*
import java.time.LocalDate

@Composable
fun SettingsScreen(
    settings: AppSettings,
    transactionCount: Int,
    onHideBalancesChange: (Boolean) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onExportCsv: (android.net.Uri) -> Unit,
    onDeleteAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editName by remember { mutableStateOf(false) }
    var editTheme by remember { mutableStateOf(false) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    // Storage Access Framework: the user picks where to save. No storage permission needed.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> if (uri != null) onExportCsv(uri) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Settings",
                color = FinTextPrimary,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
        }

        item { GroupLabel("Profile & Appearance") }
        item {
            SettingActionTile(
                icon = Icons.Filled.Person,
                title = "Display name",
                subtitle = settings.displayName.ifBlank { "Not set, used for your greeting" },
                onClick = { editName = true }
            )
        }
        item {
            SettingActionTile(
                icon = Icons.Filled.Palette,
                title = "App theme",
                subtitle = settings.themeMode.label,
                onClick = { editTheme = true }
            )
        }

        item { GroupLabel("Privacy") }
        item {
            SettingSwitchTile(
                icon = Icons.Filled.VisibilityOff,
                title = "Hide amounts",
                subtitle = "Mask balances and amounts on every screen. Saved between launches.",
                checked = settings.hideBalances,
                onCheckedChange = onHideBalancesChange
            )
        }
        item {
            InfoTile(
                icon = Icons.Filled.Lock,
                title = "Stored on this device",
                subtitle = "Your transactions, accounts, budgets and goals are saved locally and never sent to any server."
            )
        }

        item { GroupLabel("Data") }
        item {
            SettingActionTile(
                icon = Icons.Filled.Download,
                title = "Export to CSV",
                subtitle = if (transactionCount == 0) "Nothing to export yet"
                else "Save all $transactionCount transactions as a spreadsheet file",
                enabled = transactionCount > 0,
                onClick = { exportLauncher.launch("myfintrack-${LocalDate.now()}.csv") }
            )
        }
        item {
            SettingActionTile(
                icon = Icons.Filled.DeleteForever,
                title = "Delete all data",
                subtitle = "Permanently erase transactions, accounts, budgets, goals and settings",
                tint = FinExpense,
                onClick = { confirmDeleteAll = true }
            )
        }

        item { GroupLabel("About") }
        item {
            InfoTile(
                icon = Icons.Filled.AttachMoney,
                title = "Currency",
                subtitle = "Indian Rupee (\u20b9), Indian number grouping"
            )
        }
    }

    if (editName) {
        var draft by remember { mutableStateOf(settings.displayName) }
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text("Display name", color = FinTextPrimary) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= SettingsRepository.MAX_NAME_LENGTH) draft = it },
                    singleLine = true,
                    placeholder = { Text("Your name") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = finTextFieldColors(FinAccent)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDisplayNameChange(draft)
                    editName = false
                }) { Text("Save", color = FinAccent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { editName = false }) { Text("Cancel", color = FinTextSecondary) }
            },
            containerColor = FinCard
        )
    }

    if (editTheme) {
        AlertDialog(
            onDismissRequest = { editTheme = false },
            title = { Text("App theme", color = FinTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onThemeModeChange(mode)
                                    editTheme = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = settings.themeMode == mode,
                                onClick = {
                                    onThemeModeChange(mode)
                                    editTheme = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = FinAccent)
                            )
                            Text(mode.label, color = FinTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { editTheme = false }) { Text("Cancel", color = FinTextSecondary) }
            },
            containerColor = FinCard
        )
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("Delete all data?", color = FinTextPrimary) },
            text = {
                Text(
                    "This permanently removes all $transactionCount transactions, your accounts, budgets, goals and settings from this device. " +
                        "It cannot be undone. Export a CSV first if you want a copy.",
                    color = FinTextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteAll = false
                    onDeleteAllData()
                }) { Text("Delete everything", color = FinExpense, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) { Text("Cancel", color = FinTextSecondary) }
            },
            containerColor = FinCard
        )
    }
}

@Composable
private fun GroupLabel(text: String) {
    OverlineLabel(text, modifier = Modifier.padding(top = 10.dp, start = 4.dp))
}

@Composable
private fun TileIcon(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(tint.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun SettingSwitchTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FinCardShape).background(FinCard)
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TileIcon(icon, FinAccent)
        Column(Modifier.weight(1f)) {
            Text(title, color = FinTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = FinTextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF052E16),
                checkedTrackColor = FinAccent,
                uncheckedThumbColor = FinTextSecondary,
                uncheckedTrackColor = FinCardHover,
                uncheckedBorderColor = FinBorderLight
            )
        )
    }
}

@Composable
private fun SettingActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = FinAccent
) {
    val alpha = if (enabled) 1f else 0.45f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FinCardShape).background(FinCard)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TileIcon(icon, tint.copy(alpha = alpha))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = if (tint == FinExpense) FinExpense else FinTextPrimary.copy(alpha = alpha),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(subtitle, color = FinTextSecondary.copy(alpha = alpha), fontSize = 11.sp)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = FinTextMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun InfoTile(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FinCardShape).background(FinCard)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TileIcon(icon, FinTextSecondary)
        Column(Modifier.weight(1f)) {
            Text(title, color = FinTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = FinTextSecondary, fontSize = 11.sp)
        }
    }
}
