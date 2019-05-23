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

package it.jertlok.recordie.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.*
import it.jertlok.recordie.R
import it.jertlok.recordie.common.SdkHelper
import it.jertlok.recordie.utils.Utils

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var mSharedPreferences: SharedPreferences

    // User interface
    private lateinit var bitRatePref: ListPreference
    private lateinit var frameRateListPref: ListPreference
    private lateinit var audioRecordingPref: SwitchPreference
    private lateinit var shakeStopPref: SwitchPreference
    private lateinit var screenStopPref: SwitchPreference
    private lateinit var recDelayPref: SeekBarPreference
    private lateinit var themePref: ListPreference
    // Display resolution
    private lateinit var mDisplayRes: String

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Get shared preferences
        mSharedPreferences = preferenceManager.sharedPreferences

        // Get display metrics
        val metrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getRealMetrics(metrics)
        mDisplayRes = Utils.getDisplayResolution(metrics)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Get the various preferences
        bitRatePref = findPreference("bit_rate_pref")!!
        frameRateListPref = findPreference("frame_rate_pref")!!
        audioRecordingPref = findPreference("audio_recording_pref")!!
        shakeStopPref = findPreference("shake_stop_pref")!!
        screenStopPref = findPreference("screen_stop_pref")!!
        recDelayPref = findPreference("rec_delay_pref")!!
        themePref = findPreference("theme_mode_pref")!!

        // If the android version is not at least P, we need to hide the system theme.
        if (!SdkHelper.atleastP()) {
            themePref.entries = arrayOf(getString(R.string.light_theme), getString(R.string.dark_theme))
            themePref.entryValues = arrayOf("LIGHT_THEME", "DARK_THEME")
        }

        // We need to get the shared preferences and change the elements accordingly
        // Bit rate
        bitRatePref.setValueIndex(
            bitRatePref.findIndexOfValue(
                mSharedPreferences.getString("bit_rate", "8192000")
            )
        )
        // Set summary
        bitRatePref.summary = bitRatePref.entry
        // Frame rate
        frameRateListPref.setValueIndex(
            frameRateListPref.findIndexOfValue(
                mSharedPreferences.getString("frame_rate", "30")
            )
        )
        // Set summary
        frameRateListPref.summary = frameRateListPref.entry
        // Other prefs
        audioRecordingPref.isChecked = mSharedPreferences.getBoolean("audio_recording", false)
        shakeStopPref.isChecked = mSharedPreferences.getBoolean("shake_stop", false)
        screenStopPref.isChecked = mSharedPreferences.getBoolean("screen_off_stop", false)
        recDelayPref.value = mSharedPreferences.getInt("rec_delay", 0)
        // Theme preference
        themePref.setValueIndex(themePref.findIndexOfValue(
            mSharedPreferences.getString("theme_mode", "LIGHT_THEME")
            )
        )
        // Set summary
        themePref.summary = themePref.entry

        // On preference change listeners
        bitRatePref.setOnPreferenceChangeListener { _, newValue ->
            mSharedPreferences.edit().putString("bit_rate", newValue as String).apply()
            bitRatePref.summary = bitRatePref.entries[bitRatePref.findIndexOfValue(newValue)]
            true
        }
        frameRateListPref.setOnPreferenceChangeListener { _, newValue ->
            mSharedPreferences.edit().putString("frame_rate", newValue as String).apply()
            frameRateListPref.summary = frameRateListPref.entries[frameRateListPref.findIndexOfValue(newValue)]
            true
        }
        audioRecordingPref.setOnPreferenceChangeListener { _, newValue ->
            mSharedPreferences.edit().putBoolean("audio_recording", newValue as Boolean).apply()
            true
        }
        shakeStopPref.setOnPreferenceChangeListener { _, newValue ->
            mSharedPreferences.edit().putBoolean("shake_stop", newValue as Boolean).apply()
            true
        }
        screenStopPref.setOnPreferenceChangeListener { _, newValue ->
            mSharedPreferences.edit().putBoolean("screen_off_stop", newValue as Boolean).apply()
            true
        }
        recDelayPref.setOnPreferenceChangeListener { _, newValue ->
            mSharedPreferences.edit().putInt("rec_delay", newValue as Int).apply()
            true
        }
        themePref.setOnPreferenceChangeListener { _, newValue ->
            if (newValue != mSharedPreferences.getString("theme_mode", "LIGHT_THEME")) {
                mSharedPreferences.edit().putString("theme_mode", newValue as String).apply()
                activity?.recreate()
            }
            true
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }
}