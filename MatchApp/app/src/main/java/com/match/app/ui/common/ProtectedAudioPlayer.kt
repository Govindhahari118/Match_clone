package com.match.app.ui.common

import android.media.MediaPlayer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.match.app.ui.i18n.t
import com.match.app.ui.theme.MatreeDesign

/**
 * Privacy-safe profile audio player. Protected Firebase Storage sources are re-authorized by
 * rememberSecureMediaUri before MediaPlayer receives app-private bytes.
 */
@Composable
fun ProtectedAudioPlayer(
    source: String,
    modifier: Modifier = Modifier
) {
    val uri = rememberSecureMediaUri(source)
    var player by remember(uri) { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember(uri) { mutableStateOf(false) }

    DisposableEffect(uri) {
        onDispose {
            runCatching { player?.stop() }
            runCatching { player?.release() }
            player = null
            playing = false
        }
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            enabled = uri != null,
            onClick = {
                if (playing) {
                    player?.pause()
                    playing = false
                } else if (uri != null) {
                    runCatching {
                        val next = MediaPlayer().apply {
                            setDataSource(uri.toString())
                            prepare()
                            setOnCompletionListener { playing = false }
                            start()
                        }
                        player?.release()
                        player = next
                        playing = true
                    }.onFailure {
                        runCatching { player?.release() }
                        player = null
                        playing = false
                    }
                }
            }
        ) {
            Icon(
                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) t("pause", "Pause") else t("play", "Play")
            )
        }
        Spacer(Modifier.width(MatreeDesign.spacing.xs))
        Text(
            if (uri != null) t("voice_bio", "Voice introduction")
            else t("voice_bio_unavailable", "Voice introduction unavailable"),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
