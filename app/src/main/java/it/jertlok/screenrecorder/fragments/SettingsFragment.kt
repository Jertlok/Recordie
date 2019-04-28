package it.jertlok.screenrecorder.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import it.jertlok.screenrecorder.R
import it.jertlok.screenrecorder.utils.Utils
import java.util.Arrays

class SettingsFragment: PreferenceFragmentCompat() {
    // TODO: Make the resolutions available according to the
    // TODO: main display resolution.
    companion object {
        private const val TAG = "SettingsFragment"
    }

    private lateinit var mSharedPreferences: SharedPreferences

    // User interface
    private lateinit var videoListPref: ListPreference
    private lateinit var frameRateListPref: ListPreference
    private lateinit var audioRecordingPref: SwitchPreference

    // Display resolution
    private lateinit var mDisplayRes: String

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        mSharedPreferences = preferenceManager.sharedPreferences

        // Get display metrics
        val metrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getRealMetrics(metrics)
        mDisplayRes = Utils.getDisplayResolution(metrics)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // TODO: we also need to set the descriptions.
        videoListPref = findPreference("video_resolution_pref") as ListPreference
        frameRateListPref = findPreference("frame_rate_pref") as ListPreference
        audioRecordingPref = findPreference("audio_recording_pref") as SwitchPreference

        // TODO: Let's remove the XML arrays and put programmatically arrays
        // TODO: down here or some sort of Settings controller.
        // TODO: as it doesn't make any sense to play this lil' game.
        // We need to inject to the videoListPreference the mDisplayRes
        if (!videoListPref.entryValues.contains(mDisplayRes)) {
            videoListPref.entryValues[0] = mDisplayRes
        } else {
            // Exclude "Device screen resolution" as it's a supported one, which means
            // that it's inside our list.
            val supportedVideoEntries = Arrays.copyOfRange(videoListPref.entries, 1,
                    videoListPref.entries.size)
            val supportedVideoEntryValues = Arrays.copyOfRange(videoListPref.entryValues, 1,
                    videoListPref.entryValues.size)
            // Let's set the values
            videoListPref.entries = supportedVideoEntries
            videoListPref.entryValues = supportedVideoEntryValues
        }

        // We need to get the shared preferences and change the elements accordingly
        videoListPref.setValueIndex(videoListPref.findIndexOfValue(
                mSharedPreferences.getString("video_resolution", mDisplayRes)))
        frameRateListPref.setValueIndex(frameRateListPref.findIndexOfValue(
                mSharedPreferences.getString("frame_rate", "30")))
        audioRecordingPref.isChecked =
                mSharedPreferences.getBoolean("audio_recording", false)

        // Set on list preference change listeners, that will write into the SharedPreferences
        videoListPref.setOnPreferenceChangeListener { _, newValue ->
            mSharedPreferences.edit().putString("video_resolution", newValue as String).apply()
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

        return super.onCreateView(inflater, container, savedInstanceState)
    }
}