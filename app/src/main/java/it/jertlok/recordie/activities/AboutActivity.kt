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

package it.jertlok.recordie.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import it.jertlok.lightabout.AboutFragment
import it.jertlok.recordie.R
import it.jertlok.recordie.utils.ThemeHelper

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set theme - yes, Settings compatible :)
        ThemeHelper.setTheme(this, R.style.AppTheme_Settings, R.style.AppTheme_Settings_Dark)

        // Set action bar title
        supportActionBar?.title = getString(R.string.about_activity_name)
        // Set the back key on ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, AboutFragment())
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