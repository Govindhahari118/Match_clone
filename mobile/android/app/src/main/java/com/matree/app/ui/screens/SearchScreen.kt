package com.matree.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.matree.app.R
import com.matree.app.design.MatreeTheme
import com.matree.app.ui.components.ContentState
import com.matree.app.ui.components.MatreeAdaptiveContent
import com.matree.app.ui.components.MatreeHeroHeader
import com.matree.app.ui.components.MatreeSearchField
import com.matree.app.ui.components.MatreeStateContent

@Composable
fun SearchScreen(
    contentPadding: PaddingValues,
) {
    val tokens = MatreeTheme.tokens
    var query by remember { mutableStateOf("") }

    MatreeAdaptiveContent(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .imePadding(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(tokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
        ) {
            item {
                MatreeHeroHeader(
                    eyebrow = "Discover",
                    title = "Find compatible profiles",
                    body = "Search controls stay consistent across every appearance while the visual atmosphere adapts to your chosen theme.",
                )
            }
            item {
                MatreeSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = stringResource(R.string.search),
                )
            }
            item {
                MatreeStateContent(
                    state = ContentState.Empty,
                    emptyTitle = stringResource(R.string.no_profiles_title),
                    emptyBody = stringResource(R.string.no_profiles_body),
                ) { _: Unit -> }
            }
        }
    }
}
