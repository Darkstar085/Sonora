package com.sipun.sonora.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sipun.sonora.ui.theme.SonoraRed

@Composable
fun SonoraSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        placeholder = { Text("Search Music") },
        shape = OutlinedTextFieldDefaults.shape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SonoraRed,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
        ),
    )
}
