package com.scanni.app.ui.common

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/** Creates a ViewModel wired against the [com.scanni.app.di.AppGraph] without a DI framework. */
@Composable
inline fun <reified VM : ViewModel> graphViewModel(
    key: String? = null,
    noinline create: () -> VM,
): VM = viewModel(
    key = key,
    factory = viewModelFactory { initializer { create() } },
)
