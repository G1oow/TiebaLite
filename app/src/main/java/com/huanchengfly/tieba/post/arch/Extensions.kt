package com.huanchengfly.tieba.post.arch

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty1

fun <T> Flow<T>.collectIn(
    lifecycleOwner: LifecycleOwner,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    action: (T) -> Unit
): Job = lifecycleOwner.lifecycleScope.launch {
    flowWithLifecycle(lifecycleOwner.lifecycle, minActiveState).collect(action)
}

@Composable
inline fun <reified T : UiState, A> Flow<T>.collectPartialAsState(
    prop1: KProperty1<T, A>,
    initial: A,
): State<A> {
    return produceState(
        initialValue = initial,
        key1 = this,
        key2 = prop1,
        key3 = initial
    ) {
        this@collectPartialAsState
            .map {
                prop1.get(it)
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .collect {
                value = it
            }
    }
}

@Composable
inline fun <reified Event : UiEvent> Flow<UiEvent>.onEvent(
    noinline listener: suspend (Event) -> Unit
) {
    val currentListener by rememberUpdatedState(listener)
    LaunchedEffect(this) {
        this@onEvent.filterIsInstance<Event>().collect { event ->
            launch { currentListener(event) }
        }
    }
}

@Composable
inline fun <reified Event : UiEvent> BaseViewModel<*, *, *, *>.onEvent(
    noinline listener: suspend (Event) -> Unit
) {
    uiEventFlow.onEvent<Event>(listener)
}

@Composable
inline fun <reified VM : ViewModel> hiltViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    key: String? = null,
): VM = androidx.hilt.navigation.compose.hiltViewModel(viewModelStoreOwner, key)

@Composable
inline fun <reified VM : BaseViewModel<*, *, *, *>> pageViewModel(
    key: String? = null,
): VM {
    return hiltViewModel<VM>(key = key).apply {
        val context = LocalContext.current
        if (context is BaseComposeActivity) {
            val coroutineScope = rememberCoroutineScope()

            DisposableEffect(key1 = uiEventFlow) {
                with(coroutineScope) {
                    val job =
                        uiEventFlow
                            .filterIsInstance<CommonUiEvent>()
                            .cancellable()
                            .flowOn(Dispatchers.IO)
                            .collectIn(context) {
                                context.handleCommonEvent(it)
                            }

                    onDispose {
                        Log.i("pageViewModel", "onDispose")
                        job.cancel()
                    }
                }
            }
        }
    }
}

@Composable
inline fun <INTENT : UiIntent, reified VM : BaseViewModel<INTENT, *, *, *>> pageViewModel(
    initialIntent: List<INTENT> = emptyList(),
    key: String? = null,
): VM {
    return pageViewModel<VM>(key = key).apply {
        if (initialIntent.isNotEmpty()) {
            LaunchedEffect(key1 = initialized) {
                if (!initialized) {
                    initialized = true
                    initialIntent.asFlow()
                        .onEach(this@apply::send)
                        .flowOn(Dispatchers.IO)
                        .launchIn(viewModelScope)
                }
            }
        }
    }
}