package expo.modules.systemnavigationbar

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import expo.modules.core.ExportedModule
import expo.modules.core.ModuleRegistry
import expo.modules.core.Promise
import expo.modules.core.errors.CurrentActivityNotFoundException
import expo.modules.core.interfaces.ActivityProvider
import expo.modules.core.interfaces.ExpoMethod
import expo.modules.core.interfaces.services.EventEmitter

class SystemNavigationBarModule(context: Context) : ExportedModule(context) {

  private lateinit var mActivityProvider: ActivityProvider
  private lateinit var mEventEmitter: EventEmitter

  override fun getName(): String {
    return NAME
  }

  override fun onCreate(moduleRegistry: ModuleRegistry) {
    mActivityProvider = moduleRegistry.getModule(ActivityProvider::class.java)
        ?: throw IllegalStateException("Could not find implementation for ActivityProvider.")
    mEventEmitter = moduleRegistry.getModule(EventEmitter::class.java) ?: throw IllegalStateException("Could not find implementation for EventEmitter.")
  }

  // Ensure that rejections are passed up to JS rather than terminating the native client.
  private fun safeRunOnUiThread(promise: Promise, block: (activity: Activity) -> Unit) {
    val activity = mActivityProvider.currentActivity
    if (activity == null) {
      promise.reject(CurrentActivityNotFoundException())
      return
    }
    activity.runOnUiThread {
      block(activity);
    }
  }

  @ExpoMethod
  fun setBackgroundColorAsync(color: Int, promise: Promise) {
    safeRunOnUiThread(promise) {
      SystemNavigationBar.setBackgroundColor(it, color, { promise.resolve(null) }, { m -> promise.reject(ERROR_TAG, m) })
    }
  }

  @ExpoMethod
  fun getBackgroundColorAsync(promise: Promise) {
    safeRunOnUiThread(promise) {
      val color = colorToHex(it.window.navigationBarColor)
      promise.resolve(color)
    }
  }

  @ExpoMethod
  fun setBorderColorAsync(color: Int, promise: Promise) {
    safeRunOnUiThread(promise) {
      SystemNavigationBar.setBorderColor(it, color, { promise.resolve(null) }, { m -> promise.reject(ERROR_TAG, m) })
    }
  }

  @ExpoMethod
  fun getBorderColorAsync(promise: Promise) {
    safeRunOnUiThread(promise) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val color = colorToHex(it.window.navigationBarDividerColor)
        promise.resolve(color)
      } else {
        promise.reject(ERROR_TAG, "'getBorderColorAsync' is only available on Android API 28 or higher")
      }
    }
  }

  @ExpoMethod
  fun setAppearanceAsync(appearance: String, promise: Promise) {
    safeRunOnUiThread(promise) {
      SystemNavigationBar.setAppearance(it, appearance, {
        promise.resolve(null)
      }, { m -> promise.reject(ERROR_TAG, m) })
    }
  }

  @ExpoMethod
  fun getAppearanceAsync(promise: Promise) {
    safeRunOnUiThread(promise) {
      WindowInsetsControllerCompat(it.window, it.window.decorView).let { controller ->
        val style = if (controller.isAppearanceLightNavigationBars) "light" else "dark"
        promise.resolve(style)
      }
    }
  }

  @ExpoMethod
  fun setVisibilityAsync(visibility: String, promise: Promise) {
    safeRunOnUiThread(promise) {
      SystemNavigationBar.setVisibility(it, visibility, { promise.resolve(null) }, { m -> promise.reject(ERROR_TAG, m) })
    }
  }

  @ExpoMethod
  fun getVisibilityAsync(promise: Promise) {
    safeRunOnUiThread(promise) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val visibility = if (it.window.decorView.rootWindowInsets.isVisible(WindowInsets.Type.navigationBars())) "visible" else "hidden"
        promise.resolve(visibility)
      } else {
        // TODO: Verify this works
        val visibility = if ((View.SYSTEM_UI_FLAG_HIDE_NAVIGATION and it.window.decorView.systemUiVisibility) == 0) "visible" else "hidden"
        promise.resolve(visibility)
      }
    }
  }

  @ExpoMethod
  fun setPositionAsync(position: String, promise: Promise) {
    safeRunOnUiThread(promise) {
      SystemNavigationBar.setPosition(it, position, { promise.resolve(null)}, { m -> promise.reject(ERROR_TAG, m) })
    }
  }

  @ExpoMethod
  fun getPositionAsync(promise: Promise) {
    safeRunOnUiThread(promise) {
      val position = if (ViewCompat.getFitsSystemWindows(it.window.decorView)) "relative" else "absolute"
      promise.resolve(position)
    }
  }

  @ExpoMethod
  fun setBehaviorAsync(behavior: String, promise: Promise) {
    safeRunOnUiThread(promise) {
      SystemNavigationBar.setBehavior(it, behavior, { promise.resolve(null)}, { m -> promise.reject(ERROR_TAG, m) })
    }
  }

  @ExpoMethod
  fun getBehaviorAsync(promise: Promise) {
    safeRunOnUiThread(promise) {
      WindowInsetsControllerCompat(it.window, it.window.decorView).let { controller ->
        val behavior = when (controller.systemBarsBehavior) {
          // TODO: Maybe relative / absolute
          WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE -> "overlay-swipe"
          WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE -> "inset-swipe"
          // WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH -> "inset-touch"
          else -> "inset-touch"
        }
        promise.resolve(behavior)
      }
    }
  }

  /* Events */

  @ExpoMethod
  fun startObserving(promise: Promise) {
    safeRunOnUiThread(promise) {
      val decorView = it.window.decorView
      decorView.setOnSystemUiVisibilityChangeListener { visibility: Int ->
        var isNavigationBarVisible = (visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0
        var stringVisibility = if (isNavigationBarVisible) "visible" else "hidden";
        mEventEmitter.emit(
            VISIBILITY_EVENT_NAME,
            Bundle().apply {
              putString("visibility", stringVisibility)
              putInt("state", visibility)
            }
        )
      }
      promise.resolve(null);
    }
  }

  @ExpoMethod
  fun stopObserving(promise: Promise) {
    safeRunOnUiThread(promise) {
      val decorView = it.window.decorView
      decorView.setOnSystemUiVisibilityChangeListener(null)
      promise.resolve(null);
    }
  }

  companion object {
    private const val NAME = "ExpoSystemNavigationBar"
    private const val VISIBILITY_EVENT_NAME = "ExpoSystemNavigationBar.didChange"
    private const val ERROR_TAG = "ERR_SYSTEM_NAVIGATION_BAR"

    fun colorToHex(color: Int): String {
      return String.format("#%02x%02x%02x", Color.red(color), Color.green(color), Color.blue(color))
    }
  }
}
