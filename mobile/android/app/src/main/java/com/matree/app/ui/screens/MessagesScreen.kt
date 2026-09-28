package com.matree.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.matree.app.R
import com.matree.app.design.MatreeTheme
import com.matree.app.ui.components.ContentState
import com.matree.app.ui.components.MatreeAdaptiveContent
import com.matree.app.ui.components.MatreeHeroHeader
import com.matree.app.ui.components.MatreeStateContent

@Composable
fun MessagesScreen(
    contentPadding: PaddingValues,
) {
    val tokens = MatreeTheme.tokens

    MatreeAdaptiveContent(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(tokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
        ) {
            item {
                MatreeHeroHeader(
                    title = "Messages",
                    body = "Conversation surfaces inherit the same tokens and interaction behavior in every appearance.",
                )
            }
            item {
                MatreeStateContent(
                    state = ContentState.Empty,
                    emptyTitle = stringResource(R.string.no_messages_title),
                    emptyBody = stringResource(R.string.no_messages_body),
                ) { _: Unit -> }
            }
        }
    }
}
