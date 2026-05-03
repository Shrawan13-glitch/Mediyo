package com.shrawan.mediyo.music.ui.player

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.PlaybackException
import com.shrawan.mediyo.R

@Composable
fun PlaybackError(
    error: PlaybackException,
    retry: () -> Unit,
) {
    val details = buildString {
        append(error.message?.takeIf { it.isNotBlank() } ?: stringResource(R.string.error_unknown))
        if (error.errorCodeName.isNotBlank()) {
            append(" [")
            append(error.errorCodeName)
            append("]")
        }
        error.cause?.message?.takeIf { it.isNotBlank() }?.let { causeMessage ->
            append("\n")
            append(causeMessage)
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { retry() }
            )
        }
    ) {
        Icon(
            painter = painterResource(R.drawable.info),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )

        Text(
            text = details,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}
