package com.matree.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.matree.app.design.MatreeTheme

@Composable
fun MatreeSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
) {
    val tokens = MatreeTheme.tokens
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = tokens.colors.textSecondary,
            )
        },
        placeholder = {
            Text(
                text = placeholder,
                color = tokens.colors.textSecondary,
            )
        },
        shape = RoundedCornerShape(tokens.radii.large),
    )
}

@Composable
fun MatreeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val tokens = MatreeTheme.tokens
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        label = { Text(label) },
        supportingText = supportingText?.let {
            {
                Text(
                    text = it,
                    style = tokens.typography.caption,
                )
            }
        },
        enabled = enabled,
        isError = isError,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(tokens.radii.medium),
    )
}

@Composable
fun MatreeOtpField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
) {
    MatreeTextField(
        value = value,
        onValueChange = { candidate ->
            if (candidate.length <= length && candidate.all(Char::isDigit)) {
                onValueChange(candidate)
            }
        },
        label = "Verification code",
        modifier = modifier,
        supportingText = "${value.length} of $length digits",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
    )
}

@Composable
fun MatreeChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val tokens = MatreeTheme.tokens
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        label = {
            Text(
                text = label,
                style = tokens.typography.label,
            )
        },
        shape = RoundedCornerShape(tokens.radii.medium),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = tokens.colors.borderDefault,
            selectedBorderColor = tokens.colors.actionPrimary,
        ),
    )
}

@Composable
fun MatreeFilterRow(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MatreeTheme.tokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
    ) {
        options.forEach { option ->
            MatreeChoiceChip(
                label = option,
                selected = option in selected,
                onClick = { onToggle(option) },
            )
        }
    }
}

@Composable
fun MatreeTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (labels.isEmpty()) return

    val tokens = MatreeTheme.tokens
    val safeSelectedIndex = selectedIndex.coerceIn(labels.indices)

    TabRow(
        selectedTabIndex = safeSelectedIndex,
        modifier = modifier.fillMaxWidth(),
        containerColor = tokens.colors.surfacePrimary,
        contentColor = tokens.colors.actionPrimary,
        divider = {},
    ) {
        labels.forEachIndexed { index, label ->
            Tab(
                selected = safeSelectedIndex == index,
                onClick = { onSelected(index) },
                modifier = Modifier.heightIn(min = 48.dp),
                text = {
                    Text(
                        text = label,
                        style = tokens.typography.label,
                    )
                },
            )
        }
    }
}

@Composable
fun MatreeSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val tokens = MatreeTheme.tokens
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = tokens.colors.textOnAccent,
            checkedTrackColor = tokens.colors.actionPrimary,
            uncheckedThumbColor = tokens.colors.textSecondary,
            uncheckedTrackColor = tokens.colors.surfaceSubtle,
        ),
    )
}

@Composable
fun MatreeCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
fun MatreeRadio(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    RadioButton(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
fun MatreeMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    items: List<String>,
    onItemSelected: (String) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        items.forEach { item ->
            DropdownMenuItem(
                text = { Text(item) },
                onClick = { onItemSelected(item) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatreeTooltip(
    text: String,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(text) } },
        state = rememberTooltipState(),
    ) {
        content()
    }
}
