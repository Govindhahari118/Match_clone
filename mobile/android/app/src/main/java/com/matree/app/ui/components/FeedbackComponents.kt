package com.matree.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.matree.app.design.MatreeTheme

sealed interface ContentState<out T> {
    data object Loading : ContentState<Nothing>
    data object Empty : ContentState<Nothing>
    data object Offline : ContentState<Nothing>
    data class Error(val message: String? = null) : ContentState<Nothing>
    data class Data<T>(val value: T) : ContentState<T>
}

@Composable
fun <T> MatreeStateContent(
    state: ContentState<T>,
    emptyTitle: String,
    emptyBody: String,
    onRetry: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    when (state) {
        ContentState.Loading -> MatreeLoadingSkeleton()
        ContentState.Empty -> MatreeEmptyState(
            title = emptyTitle,
            body = emptyBody,
        )
        ContentState.Offline -> MatreeErrorState(
            title = "You are offline",
            body = "Reconnect to the internet and try again.",
            onRetry = onRetry,
        )
        is ContentState.Error -> MatreeErrorState(
            title = "Something went wrong",
            body = state.message ?: "We could not load this content.",
            onRetry = onRetry,
        )
        is ContentState.Data -> content(state.value)
    }
}

@Composable
fun MatreeLoadingSkeleton(
    modifier: Modifier = Modifier,
) {
    val tokens = MatreeTheme.tokens
    val transition = rememberInfiniteTransition(label = "matree-skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "matree-skeleton-alpha",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .alpha(alpha)
                    .background(
                        tokens.colors.surfaceSubtle,
                        RoundedCornerShape(tokens.radii.card),
                    ),
            )
        }
    }
}

@Composable
fun MatreeEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MatreeTheme.tokens
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(tokens.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        Text(
            text = title,
            style = tokens.typography.heading3,
            color = tokens.colors.textPrimary,
        )
        Text(
            text = body,
            style = tokens.typography.bodySecondary,
            color = tokens.colors.textSecondary,
        )
    }
}

@Composable
fun MatreeErrorState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val tokens = MatreeTheme.tokens
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(tokens.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        Text(
            text = title,
            style = tokens.typography.heading3,
            color = tokens.colors.textPrimary,
        )
        Text(
            text = body,
            style = tokens.typography.bodySecondary,
            color = tokens.colors.textSecondary,
        )
        if (onRetry != null) {
            MatreeSecondaryButton(
                text = "Retry",
                onClick = onRetry,
            )
        }
    }
}

@Composable
fun MatreeProgress(
    modifier: Modifier = Modifier,
) {
    val tokens = MatreeTheme.tokens
    CircularProgressIndicator(
        modifier = modifier,
        color = tokens.colors.actionPrimary,
    )
}

@Composable
fun MatreeConfirmationDialog(
    title: String,
    body: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            MatreeTextButton(
                text = confirmLabel,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            MatreeTextButton(
                text = dismissLabel,
                onClick = onDismiss,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatreeBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val tokens = MatreeTheme.tokens
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = tokens.colors.surfaceElevated,
        contentColor = tokens.colors.textPrimary,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = tokens.spacing.md,
                    end = tokens.spacing.md,
                    bottom = tokens.spacing.xl,
                ),
        ) {
            content()
        }
    }
}

@Composable
fun MatreeSnackbarHost(
    hostState: SnackbarHostState,
) {
    SnackbarHost(hostState = hostState)
}
