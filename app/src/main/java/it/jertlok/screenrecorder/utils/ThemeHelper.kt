package it.jertlok.screenrecorder.utils

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.os.Build
import android.preference.PreferenceManager
import android.view.View
import android.view.WindowManager

import java.lang.ref.WeakReference
import java.util.*

class ThemeHelper {

    companion object {

        /**
         * Sets the styles conditionally
         */
        fun setTheme(context: Activity, lightTheme: Int, darkTheme: Int) {
            // Weak reference for activity - garbage collector friendly.
            val activityRef: WeakReference<Activity> = WeakReference(context)
            val activity = activityRef.get() ?: return
            // We get the UIModeManager
            val uiModeManager = activity.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

            // Implement dark override
            val darkOverride = PreferenceManager.getDefaultSharedPreferences(activity.applicationContext)
                .getBoolean("dark_mode", false)

            if (darkOverride && Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                activity.setTheme(darkTheme)
                // Workaround for SystemUI visibility toggle
                activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                return
            }

            when (uiModeManager.nightMode) {
                UiModeManager.MODE_NIGHT_AUTO -> {
                    val calendar = Calendar.getInstance()
                    val timeOfDay = calendar.get(Calendar.HOUR_OF_DAY)
                    // Set theme according to time
                    if (timeOfDay in 0..18)
                        setLightTheme(activity, lightTheme)
                    else
                        activity.setTheme(darkTheme)
                }
                // Set light theme
                UiModeManager.MODE_NIGHT_NO -> setLightTheme(activity, lightTheme)
                // Set dark theme
                UiModeManager.MODE_NIGHT_YES -> activity.setTheme(darkTheme)
            }
        }

        private fun setLightTheme(activity: Activity, theme: Int) {
            // Set light theme
            activity.setTheme(theme)
            // TODO: move to when or something easier to read
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val baseFlags = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                // Marshmallow conditions
                activity.window.decorView.systemUiVisibility = baseFlags
                // If it's higher than O we need to add something else
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    activity.window.decorView.systemUiVisibility = baseFlags or
                            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

                }
            }
        }
    }
}
