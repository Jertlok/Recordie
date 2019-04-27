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

    // Display metrics
    private lateinit var mDisplayMetrics: DisplayMetrics
    // Display resolution
    private lateinit var mDisplayRes: String

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        mSharedPreferences = preferenceManager.sharedPreferences

        // Get display metrics
        val mDisplayMetrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getRealMetrics(mDisplayMetrics)
        mDisplayRes = Utils.getDisplayResolution(mDisplayMetrics)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // TODO: we also need to set the descriptions.
        videoListPref = findPreference("video_resolution_pref") as ListPreference
        frameRateListPref = findPreference("frame_rate_pref") as ListPreference
        audioRecordingPref = findPreference("audio_recording_pref") as SwitchPreference

        // We need to trim down the videoListPref, so I need to create the new arrays that will
        // take place
        val fromIndex = videoListPref.findIndexOfValue(mDisplayRes)
        val supportedVideoEntries = Arrays.copyOfRange(videoListPref.entries, fromIndex,
                videoListPref.entries.size)
        val supportedVideoEntryValues = Arrays.copyOfRange(videoListPref.entryValues, fromIndex,
                videoListPref.entryValues.size)
        videoListPref.entries = supportedVideoEntries
        videoListPref.entryValues = supportedVideoEntryValues

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