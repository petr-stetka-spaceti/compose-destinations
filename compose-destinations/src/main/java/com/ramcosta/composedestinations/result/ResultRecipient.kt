@file:SuppressLint("ComposableNaming")
@file:Suppress("UNCHECKED_CAST")

package com.ramcosta.composedestinations.result

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.spec.DestinationSpec
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class ResultRecipient<D : DestinationSpec<*>, R>(
    val navBackStackEntry: NavBackStackEntry,
    resultOriginType: Class<D>,
    resultType: Class<R>,
) {

    val resultKey = resultKey(resultOriginType, resultType)
    val canceledKey = canceledKey(resultOriginType, resultType)

    @Composable
    fun onNavResult(listener: (NavResult<R>) -> Unit) {
        val currentListener by rememberUpdatedState(listener)

        DisposableEffect(key1 = Unit) {
            val observer = object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    when (event) {
                        Lifecycle.Event.ON_START,
                        Lifecycle.Event.ON_RESUME -> {
                            handleResultIfPresent(currentListener)
                        }

                        Lifecycle.Event.ON_DESTROY -> {
                            navBackStackEntry.lifecycle.removeObserver(this)
                        }

                        else -> Unit
                    }
                }
            }

            navBackStackEntry.lifecycle.addObserver(observer)

            onDispose {
                navBackStackEntry.lifecycle.removeObserver(observer)
            }
        }
    }

    @Composable
    inline fun <reified R> onNavResultSerializable(noinline listener: (NavResult<R>) -> Unit) {
        val currentListener by rememberUpdatedState(listener)

        DisposableEffect(key1 = Unit) {
            val observer = object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    when (event) {
                        Lifecycle.Event.ON_START,
                        Lifecycle.Event.ON_RESUME -> {
                            handleResultIfPresentSerializable(currentListener)
                        }

                        Lifecycle.Event.ON_DESTROY -> {
                            navBackStackEntry.lifecycle.removeObserver(this)
                        }

                        else -> Unit
                    }
                }
            }

            navBackStackEntry.lifecycle.addObserver(observer)

            onDispose {
                navBackStackEntry.lifecycle.removeObserver(observer)
            }
        }
    }

    private fun handleResultIfPresent(listener: (NavResult<R>) -> Unit) {
        if (!hasAnyResult()) {
            return
        }

        val canceled = navBackStackEntry.savedStateHandle.remove<Boolean>(canceledKey)

        if (canceled == true) {
            listener(NavResult.Canceled)
        } else if (navBackStackEntry.savedStateHandle.contains(resultKey)) {
            listener(
                NavResult.Value(
                    navBackStackEntry.savedStateHandle.remove<R>(resultKey) as R
                )
            )
        }
    }

    inline fun <reified R> handleResultIfPresentSerializable(listener: (NavResult<R>) -> Unit) {
        if (!hasAnyResult()) {
            return
        }

        val canceled = navBackStackEntry.savedStateHandle.remove<Boolean>(canceledKey)

        if (canceled == true) {
            listener(NavResult.Canceled)
        } else if (navBackStackEntry.savedStateHandle.contains(resultKey)) {
            listener(
                NavResult.Value(
                    Json.decodeFromString(navBackStackEntry.savedStateHandle.remove<String>(resultKey)!!)
                )
            )
        }
    }

    fun hasAnyResult(): Boolean {
        return navBackStackEntry.savedStateHandle.contains(canceledKey) ||
                navBackStackEntry.savedStateHandle.contains(resultKey)
    }

    @Suppress("OVERRIDE_DEPRECATION", "OverridingDeprecatedMember")
    @Composable
    fun onResult(listener: (R) -> Unit) {
        val currentListener by rememberUpdatedState(listener)

        DisposableEffect(key1 = Unit) {
            val observer = object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            if (navBackStackEntry.savedStateHandle.contains(resultKey)) {
                                currentListener(navBackStackEntry.savedStateHandle.remove<R>(resultKey) as R)
                            }
                        }

                        Lifecycle.Event.ON_DESTROY -> {
                            navBackStackEntry.lifecycle.removeObserver(this)
                        }

                        else -> Unit
                    }
                }
            }

            navBackStackEntry.lifecycle.addObserver(observer)

            onDispose {
                navBackStackEntry.lifecycle.removeObserver(observer)
            }
        }
    }
}