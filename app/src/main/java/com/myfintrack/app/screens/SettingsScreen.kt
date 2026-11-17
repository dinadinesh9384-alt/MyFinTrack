package com.myfintrack.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myfintrack.app.theme.*

@Composable
fun SettingsScreen(
    isBalanceMasked: Boolean,
    onToggleBalanceMask: () -> Unit,
    onActionToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var currency by remember { mutableStateOf("INR (₹)") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "App Settings",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage your preferences & privacy",
                    color = FinTextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Section: Privacy & Security
        item {
            Text("PRIVACY & SECURITY", color = FinTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        item {
            SettingSwitchTile(
                icon = Icons.Default.VisibilityOff,
                title = "Mask Balance by Default",
                subtitle = "Hide total balance figures on startup",
                checked = isBalanceMasked,
                onCheckedChange = {
                    onToggleBalanceMask()
                }
            )
        }

        // Section: Preferences
        item {
            Text("PREFERENCES", color = FinTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        item {
            SettingSwitchTile(
                icon = Icons.Default.Notifications,
                title = "Push Notifications",
                subtitle = "Receive daily financial summaries & alerts",
                checked = notificationsEnabled,
                onCheckedChange = {
                    notificationsEnabled = it
                    onActionToast(if (it) "Notifications Enabled" else "Notifications Disabled")
                }
            )
        }

        item {
            SettingActionTile(
                icon = Icons.Default.AttachMoney,
                title = "Currency Format",
                subtitle = currency,
                onClick = { onActionToast("Currency set to INR (₹)") }
            )
        }

        item {
            SettingActionTile(
                icon = Icons.Default.Download,
                title = "Export Financial Statement",
                subtitle = "Download CSV or PDF transaction history",
                onClick = { onActionToast("Export feature coming soon") }
            )
        }
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
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FinCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, FinBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(FinCardHover, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = FinAccent, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = FinTextSecondary, fontSize = 10.sp)
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = FinAccent,
                    uncheckedThumbColor = FinTextSecondary,
                    uncheckedTrackColor = FinCardHover
                )
            )
        }
    }
}

@Composable
private fun SettingActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FinCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, FinBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(FinCardHover, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = FinAccent, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = FinTextSecondary, fontSize = 10.sp)
                }
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = FinTextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}
