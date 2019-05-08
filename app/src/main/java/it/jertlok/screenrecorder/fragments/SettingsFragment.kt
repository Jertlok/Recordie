package it.jertlok.screenrecorder.fragments

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.*
import it.jertlok.screenrecorder.R
import it.jertlok.screenrecorder.utils.Utils

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var mSharedPreferences: SharedPreferences

    // User interface
    private lateinit var bitRatePref: ListPreference
    private lateinit var frameRateListPref: ListPreference
    private lateinit var audioRecordingPref: SwitchPreference
    private lateinit var shakeStopPref: SwitchPreference
    private lateinit var screenStopPref: SwitchPreference
    private lateinit var recDelayPref: SeekBarPreference
    private lateinit var darkModePref: SwitchPreference
    private lateinit var themeCategory: PreferenceCategory
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
        darkModePref = findPreference("dark_mode_pref")!!
        themeCategory = findPreference("theme_category")!!

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            darkModePref.isVisible = false
            themeCategory.isVisible = false
        }

        // We need to get the shared preferences and change the elements accordingly
        bitRatePref.setValueIndex(
            bitRatePref.findIndexOfValue(
                mSharedPreferences.getString("bit_rate", "16384000")
            )
        )
        frameRateListPref.setValueIndex(
            frameRateListPref.findIndexOfValue(
                mSharedPreferences.getString("frame_rate", "30")
            )
        )
        audioRecordingPref.isChecked = mSharedPreferences.getBoolean("audio_recording", false)
        shakeStopPref.isChecked = mSharedPreferences.getBoolean("shake_stop", false)
        screenStopPref.isChecked =  mSharedPreferences.getBoolean("screen_off_stop", false)
        recDelayPref.value = mSharedPreferences.getInt("rec_delay", 3)
        darkModePref.isChecked = mSharedPreferences.getBoolean("dark_mode", false)

        // On preference change listeners
        bitRatePref.setOnPreferenceChangeListener { _, newValue ->
            mSharedPreferences.edit().putString("bit_rate", newValue as String).apply()
            true
        }
        frameRateListPref.setOnPreferenceChangeListener { _, newValue ->
            mSharedPreferences.edit().putString("frame_rate", newValue as String).apply()
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
        darkModePref.setOnPreferenceChangeListener { _, newValue ->
            mSharedPreferences.edit().putBoolean("dark_mode", newValue as Boolean).apply()
            activity?.recreate()
            true
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }
}