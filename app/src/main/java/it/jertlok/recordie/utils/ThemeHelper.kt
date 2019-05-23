/*
 *     This file is part of Recordie.
 *
 *     Recordie is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Recordie is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Recordie.  If not, see <https://www.gnu.org/licenses/>.
 */

package it.jertlok.recordie.utils

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.preference.PreferenceManager
import java.lang.ref.WeakReference
import java.util.*

class ThemeHelper {

    companion object {

        /**
         * Sets the styles conditionally
         */
        fun setTheme(context: Activity, lightTheme: Int, darkTheme: Int) : String? {
            // Weak reference for activity - garbage collector friendly.
            val activityRef: WeakReference<Activity> = WeakReference(context)
            val activity = activityRef.get() ?: return null
            // We get the UIModeManager
            val uiModeManager = activity.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

            when (PreferenceManager.getDefaultSharedPreferences(activity.applicationContext)
                .getString("theme_mode", "LIGHT_THEME")) {
                "SYSTEM_THEME" -> {
                    when (uiModeManager.nightMode) {
                        UiModeManager.MODE_NIGHT_AUTO -> {
                            val calendar = Calendar.getInstance()
                            val timeOfDay = calendar.get(Calendar.HOUR_OF_DAY)
                            // Set theme according to time
                            // From 6am to 6pm use light theme
                            // TODO: remove these hardcoded values
                            if (timeOfDay in 6..18)
                                activity.setTheme(lightTheme)
                            else
                                activity.setTheme(darkTheme)

                            return "SYSTEM_THEME"
                        }
                        // Set light theme
                        UiModeManager.MODE_NIGHT_NO -> {
                            activity.setTheme(lightTheme)
                            return "LIGHT_THEME"
                        }
                        // Set dark theme
                        UiModeManager.MODE_NIGHT_YES -> {
                            activity.setTheme(darkTheme)
                            return "DARK_THEME"
                        }
                    }
                }
                "LIGHT_THEME" -> {
                    activity.setTheme(lightTheme)
                    return "LIGHT_THEME"
                }
                "DARK_THEME" -> {
                    activity.setTheme(darkTheme)
                    return "DARK_THEME"
                }
            }

            // The activity was either broken or not available.
            return null
        }
    }
}
