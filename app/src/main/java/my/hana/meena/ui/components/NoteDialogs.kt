package my.hana.meena.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import my.hana.meena.R
import my.hana.meena.data.system.NoteRules
import my.hana.meena.theme.MeenaBlack
import my.hana.meena.theme.MeenaSpacing
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextWhite

/**
 * A note as written: list lines get a small square mark (drawn, not a text symbol), other lines
 * show as they are. [maxLines] limits how many lines show; the rest are counted, not shown.
 */
@Composable
fun NoteText(note: String, color: Color, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) {
    val lines = remember(note) { NoteRules.lines(note) }
    val shown = lines.take(maxLines)
    val markColor = MaterialTheme.colorScheme.primary
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MeenaSpacing.s)) {
        shown.forEach { (text, isListLine) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isListLine) {
                    Box(
                        modifier = Modifier
                            .padding(end = MeenaSpacing.m)
                            .size(6.dp)
                            .background(markColor)
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                    maxLines = if (maxLines == Int.MAX_VALUE) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        val hidden = lines.size - shown.size
        if (hidden > 0) {
            Text(
                text = stringResource(R.string.notes_more_lines, hidden),
                style = MaterialTheme.typography.labelSmall,
                color = MeenaTextMuted
            )
        }
    }
}

/** What can be done with one note. Each action closes the popup; remove asks first (in the caller). */
@Composable
fun NoteActionsDialog(
    note: String,
    onEdit: () -> Unit,
    onToggleList: () -> Unit,
    onRemind: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val isList = remember(note) { NoteRules.isList(note) }
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MeenaBlack)
                .padding(MeenaSpacing.l)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(MeenaSpacing.m)) {
                Text(
                    text = stringResource(R.string.notes_note_title),
                    style = MaterialTheme.typography.displayMedium,
                    fontSize = 24.sp,
                    color = MeenaTextWhite
                )
                NoteText(
                    note = note,
                    color = MeenaTextWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                )
                Column(modifier = Modifier.padding(top = MeenaSpacing.s)) {
                    NoteAction(stringResource(R.string.notes_edit), null, MeenaTextWhite, onEdit)
                    NoteAction(
                        text = stringResource(if (isList) R.string.notes_make_text else R.string.notes_make_list),
                        hint = stringResource(if (isList) R.string.notes_make_text_hint else R.string.notes_make_list_hint),
                        color = MeenaTextWhite,
                        onClick = onToggleList
                    )
                    NoteAction(
                        text = stringResource(R.string.notes_remind),
                        hint = stringResource(R.string.notes_remind_hint),
                        color = MeenaTextWhite,
                        onClick = onRemind
                    )
                    NoteAction(stringResource(R.string.action_remove), null, MaterialTheme.colorScheme.error, onRemove)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        text = stringResource(R.string.action_close),
                        style = MaterialTheme.typography.labelSmall,
                        color = MeenaTextMuted,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .metroClickable(onClick = onDismiss)
                            .padding(vertical = MeenaSpacing.m, horizontal = MeenaSpacing.s)
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteAction(text: String, hint: String?, color: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .metroClickable(onClick = onClick)
            .padding(vertical = MeenaSpacing.s),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = color)
        if (hint != null) {
            Text(text = hint, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
        }
    }
}

/**
 * Edits one note. Several lines are fine; list lines keep their "- " start so the list stays a list.
 * [onSave] returns false when the text can't be saved (empty), and the editor stays open.
 */
@Composable
fun NoteEditDialog(initial: String, onSave: (String) -> Boolean, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    val accent = MaterialTheme.colorScheme.primary
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MeenaBlack)
                .padding(MeenaSpacing.l)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(MeenaSpacing.m)) {
                Text(
                    text = stringResource(R.string.notes_edit_title),
                    style = MaterialTheme.typography.displayMedium,
                    fontSize = 24.sp,
                    color = MeenaTextWhite
                )
                BasicTextField(
                    value = value,
                    onValueChange = { value = it.copy(text = it.text.take(NoteRules.MAX_LENGTH)) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MeenaTextWhite),
                    cursorBrush = SolidColor(accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp, max = 240.dp)
                        .focusRequester(focusRequester)
                        .drawBehind {
                            val y = size.height - 1.dp.toPx()
                            drawLine(accent, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                        }
                        .padding(vertical = MeenaSpacing.m)
                )
                Text(
                    text = stringResource(R.string.notes_edit_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MeenaSpacing.m),
                    horizontalArrangement = Arrangement.spacedBy(MeenaSpacing.m)
                ) {
                    val canSave = value.text.isNotBlank()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .background(if (canSave) my.hana.meena.theme.MeenaAccentFill else my.hana.meena.theme.MeenaTextDisabled)
                            .metroClickable(enabled = canSave) { if (onSave(value.text)) onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.action_save),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (canSave) my.hana.meena.theme.MeenaOnAccent else MeenaTextWhite
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .background(my.hana.meena.theme.MeenaTextDisabled)
                            .metroClickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MeenaTextWhite
                        )
                    }
                }
            }
        }
    }
}
