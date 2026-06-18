package com.scanni.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/** Collects one-shot events only while the screen is at least STARTED. */
@Composable
fun <T> EventEffect(events: Flow<T>, onEvent: (T) -> Unit) {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(events, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            events.collect(onEvent)
        }
    }
}
