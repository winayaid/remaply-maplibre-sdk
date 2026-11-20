package com.remaply.maps.internal

import android.content.Context
import android.content.ContextWrapper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.remaply.maps.MapController

/**
 * Keeps controllers of `RemaplyMapView`s alive across configuration changes, keyed by view id
 * (API_SPEC.md §9.2). Cleared when the Activity finishes for good.
 */
internal class RetainedControllers : ViewModel() {
    private val controllers = HashMap<String, MapController>()

    fun getOrCreate(key: String): MapController = controllers.getOrPut(key) { MapController() }

    companion object {
        fun forContext(context: Context): RetainedControllers? {
            val owner = context.findViewModelStoreOwner() ?: return null
            return ViewModelProvider(owner)[RetainedControllers::class.java]
        }

        private fun Context.findViewModelStoreOwner(): ViewModelStoreOwner? {
            var c: Context? = this
            while (c != null) {
                if (c is ViewModelStoreOwner) return c
                c = (c as? ContextWrapper)?.baseContext
            }
            return null
        }
    }
}
