package de.tadris.fitness.ui.settings.export

import android.app.Activity
import android.app.AlertDialog
import android.widget.TextView
import de.tadris.fitness.R
import de.tadris.fitness.util.isUrl

class ConfigureHttpPostDialog(val activity: Activity, val onSubmit: (url: String) -> Unit) {

    private val dialog = AlertDialog.Builder(activity)
        .setView(R.layout.dialog_configure_http_post)
        .setTitle(R.string.configureHttpPostRequest)
        .setPositiveButton(R.string.okay, null)
        .create()!!

    init {
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                submit()
            }
        }
        dialog.show()
    }

    private val urlTextView = dialog.findViewById<TextView>(R.id.httpPostUrl)!!

    private fun submit() {
        val url = urlTextView.text.toString()
        if (url.isUrl()) {
            dialog.dismiss()
            onSubmit(url)
        } else {
            urlTextView.error = activity.getString(R.string.enterValidUrl)
        }
    }

}