package com.burnout.rooms

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun CustomDialog(
  icon: ImageVector,
  heading: String,

  dismissText: String = "Dismiss",
  confirmText: String = "Confirm",

  enableDismiss: Boolean = true,
  enableConfirm: Boolean = true,

  onDismiss: () -> Unit,
  onConfirm: () -> Unit,

  content: @Composable (() -> Unit)
) {
  CustomDialog(
    heading = {
      Row {
        Icon(icon, null, Modifier.padding(end = 8.dp))
        Text(heading)
      }
    },
    onDismiss = onDismiss,
    onConfirm = onConfirm,
    content = content
  )
}

@Composable
fun CustomDialog(
  heading: @Composable (() -> Unit),

  dismissText: String = "Dismiss",
  confirmText: String = "Confirm",

  enableDismiss: Boolean = true,
  enableConfirm: Boolean = true,

  onDismiss: () -> Unit,
  onConfirm: () -> Unit,

  content: @Composable (() -> Unit)
) {
  AlertDialog(
    icon = heading,
    text = content,

    confirmButton = {
      TextButton(
        content = { Text(confirmText) },
        onClick = onConfirm,
        enabled = enableConfirm
      )
    },
    onDismissRequest = onDismiss,
    dismissButton = {
      TextButton(
        content = { Text(dismissText) },
        onClick = onDismiss,
        enabled = enableDismiss
      )
    }
  )
}