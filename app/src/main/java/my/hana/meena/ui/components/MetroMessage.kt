package my.hana.meena.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import my.hana.meena.theme.MeenaTextWhite

object MetroMessage {
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    fun show(msg: String) {
        _message.value = msg
    }

    fun clear() {
        _message.value = null
    }
}

@Composable
fun MetroMessageContainer() {
    val message by MetroMessage.message.collectAsState()

    if (message != null) {
        LaunchedEffect(message) {
            delay(3000)
            MetroMessage.clear()
        }
    }

    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(my.hana.meena.theme.MeenaSpacing.l),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = message?.lowercase() ?: "",
                color = MeenaTextWhite,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
