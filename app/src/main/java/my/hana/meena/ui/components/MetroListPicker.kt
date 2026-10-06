package my.hana.meena.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import my.hana.meena.theme.MeenaBlack
import my.hana.meena.theme.MeenaBorder
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextSecondary
import my.hana.meena.theme.MeenaTextWhite

/**
 * Item specification for MetroListPicker.
 */
data class MetroPickerItem(
    val key: String,
    val title: String,
    val subtitle: String? = null
)

/**
 * Windows Phone Metro full-screen list picker (§2.2 #74):
 * - Replaces segmented buttons and inline dropdowns
 * - Pure black full-screen overlay with 0dp corners
 * - Large light typography (42sp)
 * - Minimum 48dp touch targets on every row
 * - Filterable search bar when option count > 6
 */
@Composable
fun MetroListPicker(
    title: String,
    items: List<MetroPickerItem>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    enableSearch: Boolean = items.size > 6
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = remember(searchQuery, items) {
        if (searchQuery.isBlank()) {
            items
        } else {
            val q = searchQuery.lowercase().trim()
            items.filter { it.title.lowercase().contains(q) || (it.subtitle?.lowercase()?.contains(q) == true) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MeenaBlack)
                .padding(horizontal = my.hana.meena.theme.MeenaSpacing.l, vertical = my.hana.meena.theme.MeenaSpacing.l)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Row with Back icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .metroClickable(onClick = onDismiss)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back",
                            tint = MeenaTextMuted,
                            modifier = Modifier.size(my.hana.meena.theme.MeenaSpacing.l)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "back",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MeenaTextMuted
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "close",
                            tint = MeenaTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = title.lowercase(),
                    style = MaterialTheme.typography.displayLarge,
                    color = MeenaTextWhite
                )

                // Search Bar (if enabled)
                if (enableSearch) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .border(1.dp, MeenaBorder)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = my.hana.meena.theme.MeenaSpacing.m),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "search",
                                tint = MeenaTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MeenaTextWhite),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "type to search...",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MeenaTextMuted
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .metroClickable { searchQuery = "" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "clear search",
                                        tint = MeenaTextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Items list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
                ) {
                    items(filteredItems, key = { it.key }) { item ->
                        val isSelected = item.key == selectedKey

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .metroClickable(
                                    onClick = {
                                        onSelect(item.key)
                                        onDismiss()
                                    }
                                )
                                .padding(vertical = my.hana.meena.theme.MeenaSpacing.m),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left selection indicator (accent bar)
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(28.dp)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title.lowercase(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontSize = 18.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MeenaTextWhite
                                )
                                if (item.subtitle != null) {
                                    Text(
                                        text = item.subtitle.lowercase(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MeenaTextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Setting row that displays current selection and triggers MetroListPicker on click.
 */
@Composable
fun MetroListPickerRow(
    title: String,
    currentValue: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .metroClickable(onClick = onClick)
            .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
        verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
    ) {
        Text(
            text = title.lowercase(),
            style = MaterialTheme.typography.bodyMedium,
            color = MeenaTextMuted
        )
        Text(
            text = currentValue.lowercase(),
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
        )
        if (description != null) {
            Text(
                text = description.lowercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MeenaTextSecondary
            )
        }
    }
}
