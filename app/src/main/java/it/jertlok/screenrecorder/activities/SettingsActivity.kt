package it.jertlok.screenrecorder.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import it.jertlok.screenrecorder.R
import it.jertlok.screenrecorder.fragments.SettingsFragment
import it.jertlok.screenrecorder.utils.ThemeHelper

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set theme
        ThemeHelper.setTheme(this, R.style.AppTheme_Settings, R.style.AppTheme_Settings_Dark)

        // Set contents
        setContentView(R.layout.settings_base)

        // Set action bar title
        supportActionBar?.title = getString(R.string.settings_activity_name)
        // Set the back key on ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Let's inflate the settings fragment
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_content, SettingsFragment())
            .addToBackStack("Settings")
            .commit()
    }

    override fun onBackPressed() {
        val main = Intent(this, MainActivity::class.java)
        startActivity(main)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }
}