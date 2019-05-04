package it.jertlok.screenrecorder.activities

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
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

        // TODO: move to when or something easier to read
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val baseFlags = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            // Marshmallow conditions
            window.decorView.systemUiVisibility = baseFlags
            // If it's higher than O we need to add something else
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                window.decorView.systemUiVisibility = baseFlags or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

            }
            // Set status bar color
            window.statusBarColor = Color.WHITE
        }


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
        // Try to restore the main activity
        main.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
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