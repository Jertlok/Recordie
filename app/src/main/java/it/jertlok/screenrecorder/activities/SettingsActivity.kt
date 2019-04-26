package it.jertlok.screenrecorder.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import it.jertlok.screenrecorder.R
import it.jertlok.screenrecorder.fragments.SettingsFragment

class SettingsActivity: AppCompatActivity() {

    companion object {
        private const val TAG = "SettingsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                .addToBackStack(TAG)
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