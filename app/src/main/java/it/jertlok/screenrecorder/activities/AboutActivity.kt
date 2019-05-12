package it.jertlok.screenrecorder.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import it.jertlok.screenrecorder.R
import it.jertlok.screenrecorder.utils.ThemeHelper
import me.jfenn.attribouter.Attribouter

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set theme - yes, Settings compatible :)
        ThemeHelper.setTheme(this, R.style.AppTheme_Settings, R.style.AppTheme_Settings_Dark)

        // Set action bar title
        supportActionBar?.title = "About"
        // Set the back key on ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, Attribouter.from(this).toFragment())
            .addToBackStack("About")
            .commit()
    }

    override fun onBackPressed() {
        val main = Intent(this, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
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