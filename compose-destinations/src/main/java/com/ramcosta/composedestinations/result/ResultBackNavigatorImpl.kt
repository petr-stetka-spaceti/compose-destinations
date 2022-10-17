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
import kotlin.reflect.KType
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

internal class ResultBackNavigatorImpl<R>(
    private val navController: NavController,
    private val navBackStackEntry: NavBackStackEntry,
    resultOriginType: Class<out DestinationSpec<*>>,
    resultType: Class<R>
) : ResultBackNavigator<R> {

    private val resultKey = resultKey(resultOriginType, resultType)
    private val canceledKey = canceledKey(resultOriginType, resultType)

    override fun navigateBack(
        result: R,
        onlyIfResumed: Boolean,
        type: KType?
    ) {
        if (onlyIfResumed && navBackStackEntry.lifecycle.currentState != Lifecycle.State.RESUMED) {
            return
        }

        setResult(result, type)
        navigateBack()
    }

    override fun setResult(
        result: R,
        type: KType?
    ) {
        navController.previousBackStackEntry?.savedStateHandle?.let {
            it[canceledKey] = false
            it[resultKey] = trySerializeWithKotlinX(result, type) ?: result
        }
    }

    override fun navigateBack() {
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

    private fun trySerializeWithKotlinX(result: R, type: KType?): String? {
        val serializer = type?.let { serializer(it) }
        return serializer?.let { Json.encodeToString(it, result) }
    }
}