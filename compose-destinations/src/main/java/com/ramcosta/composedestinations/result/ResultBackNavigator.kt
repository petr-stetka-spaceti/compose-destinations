package com.ramcosta.composedestinations.result

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.ramcosta.composedestinations.spec.DestinationSpec
import kotlinx.serialization.encodeToString
import kotlin.reflect.KType
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

class ResultBackNavigator<R>(
    val navController: NavController,
    private val navBackStackEntry: NavBackStackEntry,
    resultOriginType: Class<out DestinationSpec<*>>,
    resultType: Class<R>
) {
    val resultKey = resultKey(resultOriginType, resultType)
    val canceledKey = canceledKey(resultOriginType, resultType)

    val isResumed: Boolean
        get() = navBackStackEntry.lifecycle.currentState == Lifecycle.State.RESUMED

    fun navigateBack(
        result: R,
        onlyIfResumed: Boolean = false
    ) {
        if (onlyIfResumed && !isResumed) {
            return
        }

        setResult(result)
        navigateBack()
    }

    inline fun <reified R> navigateBackSerializable(
        result: R,
        onlyIfResumed: Boolean = false
    ) {
        if (onlyIfResumed && !isResumed) {
            return
        }

        setResultSerializable(result)
        navigateBack()
    }

    fun setResult(result: R) {
        navController.previousBackStackEntry?.savedStateHandle?.let {
            it[canceledKey] = false
            it[resultKey] = result
        }
    }

    inline fun <reified R> setResultSerializable(result: R) {
        navController.previousBackStackEntry?.savedStateHandle?.let {
            val json = Json.encodeToString(result)

            it[canceledKey] = false
            it[resultKey] = json
        }
    }

    fun navigateBack(onlyIfResumed: Boolean = false) {
        if (onlyIfResumed && !isResumed) {
            return
        }

        navController.navigateUp()
    }

    @SuppressLint("ComposableNaming")
    @Composable
    fun handleCanceled() {
        val currentNavBackStackEntry = remember { navController.currentBackStackEntry } ?: return

        DisposableEffect(key1 = Unit) {
            val observer = object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            val savedStateHandle =
                                navController.previousBackStackEntry?.savedStateHandle ?: return

                            if (!savedStateHandle.contains(canceledKey)) {
                                // We set canceled to true when this destination becomes visible
                                // When a value to be returned is set, we will put the canceled to `false`
                                savedStateHandle[canceledKey] = true
                                currentNavBackStackEntry.lifecycle.removeObserver(this)
                            }
                        }

                        else -> Unit
                    }
                }
            }

            currentNavBackStackEntry.lifecycle.addObserver(observer)

            onDispose {
                currentNavBackStackEntry.lifecycle.removeObserver(observer)
            }
        }
    }
}
