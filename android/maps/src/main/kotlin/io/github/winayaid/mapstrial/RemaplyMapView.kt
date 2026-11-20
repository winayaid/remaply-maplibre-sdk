package io.github.winayaid.mapstrial

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.core.os.BundleCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import io.github.winayaid.mapstrial.internal.LifecycleStateMachine
import io.github.winayaid.mapstrial.internal.MainThread
import io.github.winayaid.mapstrial.internal.MapSession
import io.github.winayaid.mapstrial.internal.RetainedControllers
import io.github.winayaid.mapstrial.internal.SavedMapState
import io.github.winayaid.mapstrial.internal.SdkLog

/**
 * A map view for classic Android Views.
 *
 * ```
 * val mapView = RemaplyMapView(context, MapStyle.url("https://example.com/style.json"))
 * mapView.listener = object : MapListener {
 *     override fun onMapReady(controller: MapController) { /* add markers */ }
 * }
 * ```
 *
 * **Lifecycle:** handled automatically through the view tree's `LifecycleOwner` (Activities and
 * Fragments). Only hosts without one need to call the lifecycle methods, which are idempotent.
 *
 * **Rotation:** give the view an id (`android:id` or [setId]) and its [controller], with style,
 * camera, and markers, survives configuration changes. Without an id, only style and camera are
 * restored, and markers must be re-added in [MapListener.onMapReady].
 *
 * XML attributes: `app:remaply_styleUrl`, `app:remaply_styleAsset`.
 *
 * Spec: API-030.
 */
@Suppress("TooManyFunctions")
public class RemaplyMapView : FrameLayout {
    private val explicitController: MapController?
    private var initialStyle: MapStyle?
    private var currentOptions: MapOptions
    private var session: MapSession? = null
    private var observedLifecycle: Lifecycle? = null
    private var componentCallbacksRegistered = false

    /** Creates a view with no style. Call `controller.setStyle(...)` to load one. */
    public constructor(context: Context) : this(context, null as AttributeSet?)

    /** Constructor used when inflating from XML. */
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    /** Constructor used when inflating from XML with a default style attribute. */
    public constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
    ) : super(context, attrs, defStyleAttr) {
        explicitController = null
        currentOptions = MapOptions()
        initialStyle = readStyleAttributes(context, attrs, defStyleAttr)
    }

    /**
     * Creates a view in code.
     *
     * @param style Initial style. `null` keeps the controller's stored style, if any.
     * @param options Map options.
     * @param controller The controller to bind. `null` lets the view supply one, retained across
     *   rotation when the view has an id.
     */
    @JvmOverloads
    public constructor(
        context: Context,
        style: MapStyle?,
        options: MapOptions = MapOptions(),
        controller: MapController? = null,
    ) : super(context) {
        explicitController = controller
        currentOptions = options
        initialStyle = style
    }

    /** The controller for this view's style, camera, and markers. */
    public val controller: MapController by lazy(LazyThreadSafetyMode.NONE) { resolveController() }

    /**
     * Map options. Setting this applies every field except [MapOptions.initialCamera]
     * immediately.
     */
    public var options: MapOptions
        get() = currentOptions
        set(value) {
            MainThread.check("RemaplyMapView.options")
            if (lifecycleState == MapLifecycleState.DESTROYED) {
                SdkLog.warn("MAP_DESTROYED: options set after onDestroy")
                return
            }
            currentOptions = value
            session?.applyOptions(value)
        }

    /** Receives map events. Held strongly and released in [onDestroy]. */
    public var listener: MapListener? = null

    private val stateMachine =
        LifecycleStateMachine(
            object : LifecycleStateMachine.Target {
                override fun performCreate() = createSession()

                override fun performStart() = session?.performStart() ?: Unit

                override fun performResume() = session?.performResume() ?: Unit

                override fun performPause() = session?.performPause() ?: Unit

                override fun performStop() = session?.performStop() ?: Unit

                override fun performDestroy() {
                    session?.performDestroy()
                    session = null
                }
            },
        )

    /** Current lifecycle state. Spec: API-010. */
    public val lifecycleState: MapLifecycleState
        get() = stateMachine.state

    // ---- Manual lifecycle (API_SPEC.md §9.1) ----

    /**
     * Creates the native map. [savedInstanceState] restores style and camera saved by
     * [onSaveInstanceState] (Android-only).
     */
    public fun onCreate(savedInstanceState: Bundle?) {
        MainThread.check("onCreate")
        savedInstanceState?.let(::restoreFrom)
        stateMachine.moveUpTo(MapLifecycleState.CREATED)
    }

    /** Starts rendering. */
    public fun onStart() {
        MainThread.check("onStart")
        stateMachine.moveUpTo(MapLifecycleState.STARTED)
    }

    /** Resumes interaction. */
    public fun onResume() {
        MainThread.check("onResume")
        stateMachine.moveUpTo(MapLifecycleState.RESUMED)
    }

    /** Pauses interaction. */
    public fun onPause() {
        MainThread.check("onPause")
        stateMachine.moveDownTo(MapLifecycleState.STARTED)
    }

    /** Stops rendering. */
    public fun onStop() {
        MainThread.check("onStop")
        stateMachine.moveDownTo(MapLifecycleState.CREATED)
    }

    /**
     * Releases the native map. Cancels animations, unbinds the controller (which keeps its state),
     * and clears the listener. Terminal.
     */
    public fun onDestroy() {
        MainThread.check("onDestroy")
        stopObservingLifecycle()
        stateMachine.destroy()
        listener = null
    }

    /** Releases MapLibre caches. */
    public fun onLowMemory() {
        MainThread.check("onLowMemory")
        session?.onLowMemory()
    }

    /** Saves style and camera for process death (Android-only, API-051). Markers are not saved. */
    public fun onSaveInstanceState(outState: Bundle) {
        MainThread.check("onSaveInstanceState")
        SavedMapState.write(outState, controller.style, controller.camera)
    }

    // ---- Automatic lifecycle ----

    private val lifecycleObserver =
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> stateMachine.moveUpTo(MapLifecycleState.CREATED)
                Lifecycle.Event.ON_START -> stateMachine.moveUpTo(MapLifecycleState.STARTED)
                Lifecycle.Event.ON_RESUME -> stateMachine.moveUpTo(MapLifecycleState.RESUMED)
                Lifecycle.Event.ON_PAUSE -> stateMachine.moveDownTo(MapLifecycleState.STARTED)
                Lifecycle.Event.ON_STOP -> stateMachine.moveDownTo(MapLifecycleState.CREATED)
                Lifecycle.Event.ON_DESTROY -> onDestroy()
                Lifecycle.Event.ON_ANY -> Unit
            }
        }

    private val componentCallbacks =
        object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                @Suppress("DEPRECATION")
                if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) session?.onLowMemory()
            }

            override fun onConfigurationChanged(newConfig: Configuration) = Unit

            @Deprecated("Deprecated in Java")
            override fun onLowMemory() {
                session?.onLowMemory()
            }
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isInEditMode || lifecycleState == MapLifecycleState.DESTROYED) return
        if (!componentCallbacksRegistered) {
            context.registerComponentCallbacks(componentCallbacks)
            componentCallbacksRegistered = true
        }
        if (observedLifecycle == null) {
            val owner: LifecycleOwner? = findViewTreeLifecycleOwner()
            if (owner != null) {
                observedLifecycle = owner.lifecycle
                // Replays events up to the owner's current state.
                owner.lifecycle.addObserver(lifecycleObserver)
            } else if (lifecycleState == MapLifecycleState.INITIALIZED) {
                SdkLog.debug("RemaplyMapView has no LifecycleOwner; call its lifecycle methods manually")
            }
        }
    }

    override fun onDetachedFromWindow() {
        if (componentCallbacksRegistered) {
            context.unregisterComponentCallbacks(componentCallbacks)
            componentCallbacksRegistered = false
        }
        super.onDetachedFromWindow()
    }

    private fun stopObservingLifecycle() {
        observedLifecycle?.removeObserver(lifecycleObserver)
        observedLifecycle = null
    }

    // ---- View state (rotation fallback and process death) ----

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable(KEY_SUPER_STATE, super.onSaveInstanceState())
        if (lifecycleState != MapLifecycleState.DESTROYED) {
            SavedMapState.write(bundle, controller.style, controller.camera)
        }
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is Bundle) {
            super.onRestoreInstanceState(state)
            return
        }
        restoreFrom(state)
        super.onRestoreInstanceState(BundleCompat.getParcelable(state, KEY_SUPER_STATE, Parcelable::class.java))
    }

    /** Seeds style and camera from saved state, unless the controller already has its own. */
    private fun restoreFrom(state: Bundle) {
        if (lifecycleState == MapLifecycleState.DESTROYED) return
        val c = controller
        if (c.style == null) SavedMapState.readStyle(state)?.let(c::setStyle)
        if (c.camera == null) {
            SavedMapState.readCamera(state)?.let { saved ->
                c.setCamera(CameraUpdate(saved.center, saved.zoom, saved.bearing, saved.tilt))
            }
        }
    }

    // ---- Touch: any touch cancels a running camera animation (API_SPEC.md §6.4) ----

    /** Cancels a running camera animation on any touch (API_SPEC.md §6.4), then dispatches as usual. */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) session?.cancelAnimation()
        return super.dispatchTouchEvent(ev)
    }

    // ---- Internals ----

    private fun resolveController(): MapController {
        val resolved =
            explicitController
                ?: retainedController()
                ?: MapController()
        initialStyle?.let { style ->
            if (resolved.style != style) resolved.setStyle(style)
        }
        initialStyle = null
        return resolved
    }

    private fun retainedController(): MapController? {
        if (id == NO_ID) {
            SdkLog.debug("RemaplyMapView has no id; its markers will not survive configuration changes")
            return null
        }
        return RetainedControllers.forContext(context)?.getOrCreate("view:$id")
    }

    private fun createSession() {
        if (isInEditMode) return
        session =
            MapSession(
                context = context,
                container = this,
                controller = controller,
                initialOptions = currentOptions,
                sink = eventSink,
            ).also { it.performCreate() }
    }

    private val eventSink =
        object : MapSession.EventSink {
            override fun onMapReady() {
                listener?.onMapReady(controller)
            }

            override fun onMapLoadError(error: MapError) {
                listener?.onMapLoadError(error)
            }

            override fun onStyleLoaded(style: MapStyle) {
                listener?.onStyleLoaded(style)
            }

            override fun onMapTap(coordinate: GeoCoordinate) {
                listener?.onMapTap(coordinate)
            }

            override fun onMarkerTap(marker: Marker) {
                listener?.onMarkerTap(marker)
            }

            override fun onCameraChange(
                camera: CameraPosition,
                reason: CameraChangeReason,
            ) {
                listener?.onCameraChange(camera, reason)
            }

            override fun onCameraIdle(camera: CameraPosition) {
                listener?.onCameraIdle(camera)
            }
        }

    private companion object {
        const val KEY_SUPER_STATE = "remaply.superState"

        fun readStyleAttributes(
            context: Context,
            attrs: AttributeSet?,
            defStyleAttr: Int,
        ): MapStyle? {
            if (attrs == null) return null
            val a = context.obtainStyledAttributes(attrs, R.styleable.RemaplyMapView, defStyleAttr, 0)
            try {
                a.getString(R.styleable.RemaplyMapView_remaply_styleUrl)?.let { return MapStyle.url(it) }
                a.getString(R.styleable.RemaplyMapView_remaply_styleAsset)?.let { return MapStyle.bundled(it) }
                return null
            } finally {
                a.recycle()
            }
        }
    }
}
