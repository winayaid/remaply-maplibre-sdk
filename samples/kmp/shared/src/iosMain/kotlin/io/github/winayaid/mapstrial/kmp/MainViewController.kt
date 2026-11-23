package io.github.winayaid.mapstrial.kmp

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** Entry point for the iOS app. In Swift: `MainViewControllerKt.MainViewController(mapFactory: ...)`. */
@Suppress("FunctionName", "unused")
fun MainViewController(mapFactory: NativeMapFactory): UIViewController =
    ComposeUIViewController {
        CompositionLocalProvider(LocalNativeMapFactory provides mapFactory) {
            App()
        }
    }
