package com.anhdaden

import android.content.Context
import android.content.SharedPreferences
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class XtreamIPTVPlugin : Plugin() {

    companion object {
        var pluginContext: Context? = null

        fun getPrefs(): SharedPreferences? {
            return pluginContext?.getSharedPreferences("xtream_prefs", Context.MODE_PRIVATE)
        }
    }

    override fun load(context: Context) {
        pluginContext = context
        registerMainAPI(XtreamIPTVProvider())
    }

    init {
        openSettings = { ctx ->
            val prefs = ctx.getSharedPreferences("xtream_prefs", Context.MODE_PRIVATE)

            val layout = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 40, 50, 10)
            }

            val serverInput = EditText(ctx).apply {
                hint = "Server URL (e.g., http://server:port)"
                setText(prefs.getString("server_url", ""))
                setSingleLine()
            }

            val userInput = EditText(ctx).apply {
                hint = "Username"
                setText(prefs.getString("username", ""))
                setSingleLine()
            }

            val passInput = EditText(ctx).apply {
                hint = "Password"
                setText(prefs.getString("password", ""))
                setSingleLine()
            }

            layout.addView(serverInput)
            layout.addView(userInput)
            layout.addView(passInput)

            AlertDialog.Builder(ctx)
                .setTitle("Xtream IPTV Settings")
                .setView(layout)
                .setPositiveButton("Save") { _, _ ->
                    val server = serverInput.text.toString().trimEnd('/')
                    val user = userInput.text.toString()
                    val pass = passInput.text.toString()

                    prefs.edit()
                        .putString("server_url", server)
                        .putString("username", user)
                        .putString("password", pass)
                        .apply()

                    Toast.makeText(ctx, "Settings saved! Restart CloudStream to apply.", Toast.LENGTH_LONG).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
