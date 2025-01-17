package com.bizilabs.streeek.feature.updater

import InAppUpdateManager
import android.app.Activity
import org.koin.dsl.module

val AppUpdaterModule =
    module {
        // Provide InAppUpdateManager as a singleton, passing Activity at runtime
        factory { (activity: Activity) -> InAppUpdateManager(activity) }
    }
