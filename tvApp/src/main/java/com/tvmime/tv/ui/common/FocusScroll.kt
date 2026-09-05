package com.tvmime.tv.ui.common

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged

@Composable
fun KeepFocusedChildVisibleScrollBehavior(content: @Composable () -> Unit) {
    content()
}

@Composable
fun InheritedFocusScrollBehavior(
    behavior: Any?,
    content: @Composable () -> Unit,
) {
    content()
}

@Composable
fun KeepFocusedChildVisibleColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier) {
        content()
    }
}

@Composable
fun KeepFocusedChildVisibleLazyColumn(
    state: LazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyListScope.(inheritedBehavior: Any?) -> Unit,
) {
    LazyColumn(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
    ) {
        content(null)
    }
}

@Composable
fun Modifier.scrollsToTopWhenFocused(
    offset: () -> Int,
    scrollToTop: suspend () -> Unit,
): Modifier {
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(focused) {
        if (!focused) return@LaunchedEffect
        withFrameNanos { }
        if (offset() != 0) scrollToTop()
    }
    return onFocusChanged { focused = it.hasFocus }.focusGroup()
}
