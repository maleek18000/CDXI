package com.anhdaden

import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class XtreamIPTVPlugin : Plugin() {

    companion object {
        private var _plugin: XtreamIPTVPlugin? = null

        fun getPlugin(): XtreamIPTVPlugin? = _plugin

        fun getLinks(): Array<Link> {
            return try {
                CloudStreamApp.getKey("xtream_iptv_links") ?: emptyArray()
            } catch (e: Exception) {
                emptyArray()
            }
        }

        fun saveLinks(links: Array<Link>) {
            try {
                CloudStreamApp.setKey("xtream_iptv_links", links)
            } catch (e: Exception) {
                // Fallback - ignore
            }
        }

        fun addLink(link: Link) {
            val links = getLinks().toMutableList()
            links.add(link)
            saveLinks(links.toTypedArray())
        }

        fun removeLink(name: String) {
            val links = getLinks().filter { it.name != name }
            saveLinks(links.toTypedArray())
        }
    }

    fun reload() {
        val savedLinks = getLinks()
        savedLinks.forEach { link ->
            registerMainAPI(XtreamIPTVProvider(link.mainUrl, link.name, link.username, link.password))
        }
    }

    override fun load(context: Context) {
        _plugin = this
        reload()
    }

    init {
        openSettings = { ctx ->
            showMainSettings(ctx)
        }
    }

    private fun showMainSettings(ctx: Context) {
        val links = getLinks()

        val message = if (links.isEmpty()) {
            "No accounts added yet.\n\nTap 'Add Account' to add your IPTV provider."
        } else {
            "Your accounts:\n\n" + links.joinToString("\n") { "• ${it.name}\n  ${it.mainUrl}" }
        }

        AlertDialog.Builder(ctx)
            .setTitle("Xtream IPTV")
            .setMessage(message)
            .setPositiveButton("Add Account") { _, _ ->
                showAddDialog(ctx)
            }
            .setNegativeButton("Manage", null)
            .setNeutralButton("Close", null)
            .show()
            .also { dialog ->
                // Override Manage button
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setOnClickListener {
                    if (links.isEmpty()) {
                        Toast.makeText(ctx, "No accounts to manage", Toast.LENGTH_SHORT).show()
                    } else {
                        showManageDialog(ctx)
                    }
                }
            }
    }

    private fun showAddDialog(ctx: Context) {
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val nameInput = EditText(ctx).apply {
            hint = "Account Name (e.g., My IPTV)"
            setSingleLine()
        }

        val serverInput = EditText(ctx).apply {
            hint = "Server URL (e.g., http://server:port)"
            setSingleLine()
        }

        val userInput = EditText(ctx).apply {
            hint = "Username"
            setSingleLine()
        }

        val passInput = EditText(ctx).apply {
            hint = "Password"
            setSingleLine()
        }

        layout.addView(nameInput)
        layout.addView(serverInput)
        layout.addView(userInput)
        layout.addView(passInput)

        AlertDialog.Builder(ctx)
            .setTitle("Add IPTV Account")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text.toString().trim()
                var server = serverInput.text.toString().trim().trimEnd('/')
                val user = userInput.text.toString().trim()
                val pass = passInput.text.toString().trim()

                // Normalize URL scheme
                if (server.startsWith("HTTP://", ignoreCase = true)) {
                    server = "http://" + server.substring(7)
                } else if (server.startsWith("HTTPS://", ignoreCase = true)) {
                    server = "https://" + server.substring(8)
                }

                // Validate
                if (name.isEmpty()) {
                    Toast.makeText(ctx, "Name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (!server.startsWith("http")) {
                    Toast.makeText(ctx, "Server must start with http:// or https://", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (user.isEmpty()) {
                    Toast.makeText(ctx, "Username is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (pass.isEmpty()) {
                    Toast.makeText(ctx, "Password is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Check unique name
                if (getLinks().any { it.name == name }) {
                    Toast.makeText(ctx, "Account '$name' already exists", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Save and reload
                addLink(Link(name, server, user, pass))
                reload()

                Toast.makeText(ctx, "Account '$name' saved! Restart CloudStream to see it.", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showManageDialog(ctx: Context) {
        val links = getLinks()
        if (links.isEmpty()) {
            Toast.makeText(ctx, "No accounts to manage", Toast.LENGTH_SHORT).show()
            return
        }

        val names = links.map { "${it.name} - ${it.mainUrl}" }.toTypedArray()

        AlertDialog.Builder(ctx)
            .setTitle("Manage Accounts")
            .setItems(names) { _, which ->
                val link = links[which]
                AlertDialog.Builder(ctx)
                    .setTitle(link.name)
                    .setMessage("Server: ${link.mainUrl}\nUsername: ${link.username}")
                    .setPositiveButton("Delete") { _, _ ->
                        removeLink(link.name)
                        Toast.makeText(ctx, "Removed '${link.name}'. Restart CloudStream.", Toast.LENGTH_LONG).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Close", null)
            .show()
    }
}
