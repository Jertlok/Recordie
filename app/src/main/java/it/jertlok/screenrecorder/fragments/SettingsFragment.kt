package it.jertlok.screenrecorder.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import it.jertlok.screenrecorder.R

class SettingsFragment: PreferenceFragmentCompat() {
    // TODO: Make the resolutions available according to the
    // TODO: main display resolution.
    companion object {
        private const val TAG = "SettingsFragment"
    }

    private lateinit var mSharedPreferences: SharedPreferences

    // User interface
    private lateinit var mListPreference: ListPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        mSharedPreferences = preferenceManager.sharedPreferences
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mListPreference = findPreference("video_resolutions") as ListPreference

        // We need to get the shared preferences and change the elements accordingly
        mListPreference.setValueIndex(mListPreference.findIndexOfValue(
                mSharedPreferences.getString("video_resolutions", "-1")))

        // Set on list preference change listener, that will write into the SharedPreferences
        mListPreference.setOnPreferenceChangeListener { _, newValue ->
            mSharedPreferences.edit().putString("video_resolution", newValue.toString()).apply()
            true
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }
}