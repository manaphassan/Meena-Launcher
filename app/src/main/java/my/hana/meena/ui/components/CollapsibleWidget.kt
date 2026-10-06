package my.hana.meena.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CollapsibleWidget(
    title: String,
    modifier: Modifier = Modifier,
    collapsedSummary: (@Composable () -> Unit)? = null,
    isCollapsed: Boolean = false,
    initiallyExpanded: Boolean = !isCollapsed,
    onToggleCollapse: ((Boolean) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var localCollapsed by remember(isCollapsed, initiallyExpanded) {
        mutableStateOf(if (onToggleCollapse != null) isCollapsed else !initiallyExpanded)
    }
    val effectiveCollapsed = if (onToggleCollapse != null) isCollapsed else localCollapsed
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = my.hana.meena.theme.MeenaSpacing.m)
    ) {
        // Widget Header: tapping title collapses or expands
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    val next = !effectiveCollapsed
                    localCollapsed = next
                    onToggleCollapse?.invoke(next)
                }
                .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.lowercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (effectiveCollapsed && collapsedSummary != null) {
                collapsedSummary()
            }
        }

        // Expandable / Collapsible Body with Smooth Animation
        AnimatedVisibility(
            visible = !effectiveCollapsed,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = my.hana.meena.theme.MeenaSpacing.s)) {
                content()
            }
        }
    }
}

