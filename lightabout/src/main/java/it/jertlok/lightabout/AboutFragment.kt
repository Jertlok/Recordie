package it.jertlok.lightabout

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

/**
 * Created by Prasad Shirvandkar on 24/06/16.
 *
 * Converted to AndroidX and Kotlin by Jertlok
 * A lot of files have been removed as not relevant for
 * my app scopes.
 */

class AboutFragment : Fragment(), View.OnClickListener {

    private lateinit var rate: LinearLayout
    private lateinit var github: LinearLayout
    private lateinit var twitter: LinearLayout
    private lateinit var bugs: LinearLayout
    private lateinit var donate: LinearLayout
    private lateinit var version: String
    private lateinit var versionText: TextView


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.about_fragment, container, false)

        try {
            val packageInfo = view.context.packageManager.getPackageInfo(
                view.context.packageName,
                PackageManager.GET_ACTIVITIES
            )
            version = packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        versionText = view.findViewById(R.id.app_version)
        versionText.text = version

        rate = view.findViewById(R.id.rate)
        github = view.findViewById(R.id.github)
        twitter = view.findViewById(R.id.twitter)
        bugs = view.findViewById(R.id.bugs)
        donate = view.findViewById(R.id.donate)

        rate.setOnClickListener(this)
        github.setOnClickListener(this)
        twitter.setOnClickListener(this)
        bugs.setOnClickListener(this)
        donate.setOnClickListener(this)

        // Return the constructed view
        return view
    }

    override fun onClick(v: View) {

        when (v.id) {

            R.id.rate -> startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + v.context.packageName)
                )
            )

            R.id.github -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
                "https://github.com/Jertlok")))

            R.id.twitter -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/Jertlok")))

            R.id.donate -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://paypal.me/Jertlok")))
            }

            R.id.bugs -> {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/Jertlok/Recordie/issues")))
            }
        }
    }
}
