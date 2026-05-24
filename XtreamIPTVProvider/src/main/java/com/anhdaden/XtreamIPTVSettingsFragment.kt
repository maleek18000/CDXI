package com.anhdaden

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class XtreamIPTVSettingsFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ctx = requireContext()
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

        return AlertDialog.Builder(ctx)
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
            .create()
    }
}
