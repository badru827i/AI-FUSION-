package com.aifusion.app.integration

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import java.util.Locale

/**
 * Local-first bridge from AI-FUSION to Android's supported app intents.
 *
 * This connector does not inspect another app's private data, bypass Android
 * permissions, click UI controls, or send messages automatically. The share
 * action always presents Android's chooser so the user selects the destination.
 */
object UniversalAppConnector {
    data class LaunchableApp(
        val label: String,
        val packageName: String
    )

    data class ActionResult(
        val success: Boolean,
        val message: String
    )

    enum class SettingsPage {
        GENERAL,
        WIFI,
        BLUETOOTH
    }

    private val packageAliases = mapOf(
        "youtube" to "com.google.android.youtube",
        "whatsapp" to "com.whatsapp",
        "chrome" to "com.android.chrome",
        "gmail" to "com.google.android.gm",
        "telegram" to "org.telegram.messenger",
        "tiktok" to "com.zhiliaoapp.musically",
        "instagram" to "com.instagram.android",
        "facebook" to "com.facebook.katana",
        "spotify" to "com.spotify.music",
        "google maps" to "com.google.android.apps.maps",
        "maps" to "com.google.android.apps.maps"
    )

    fun installedApps(context: Context): List<LaunchableApp> {
        val appContext = context.applicationContext
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return appContext.packageManager.queryIntentActivities(launcherIntent, 0)
            .mapNotNull { info ->
                val activity = info.activityInfo ?: return@mapNotNull null
                val label = info.loadLabel(appContext.packageManager)?.toString()?.trim()
                    .orEmpty()
                if (label.isBlank() || activity.packageName.isBlank()) null
                else LaunchableApp(label, activity.packageName)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.ROOT) }
    }

    fun launchApp(context: Context, requestedName: String): ActionResult {
        val query = requestedName.trim()
        if (query.isBlank()) {
            return ActionResult(false, "Nama aplikasi belum diberikan.")
        }

        val apps = installedApps(context)
        val normalized = normalize(query)
        val aliasPackage = packageAliases[normalized]
        val aliasMatch = aliasPackage?.let { pkg ->
            apps.firstOrNull { it.packageName == pkg }
        }
        val exactMatch = apps.firstOrNull { normalize(it.label) == normalized }
        val partialMatches = apps.filter {
            val label = normalize(it.label)
            label.contains(normalized) || normalized.contains(label)
        }

        val selected = aliasMatch ?: exactMatch ?: partialMatches.singleOrNull()
        if (selected == null) {
            val choices = partialMatches.take(5).joinToString { it.label }
            return if (partialMatches.size > 1) {
                ActionResult(false, "Nama aplikasi tidak unik. Pilihan yang hampir sepadan: $choices.")
            } else {
                ActionResult(false, "Aplikasi '$query' tidak ditemui. Semak nama aplikasi atau minta senarai aplikasi.")
            }
        }

        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(selected.packageName)
                ?: Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setPackage(selected.packageName)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.applicationContext.startActivity(launchIntent)
            ActionResult(true, "Membuka ${selected.label}.")
        } catch (_: ActivityNotFoundException) {
            ActionResult(false, "Android tidak menemui skrin utama untuk ${selected.label}.")
        } catch (error: SecurityException) {
            ActionResult(false, "Android menyekat pembukaan aplikasi: ${error.message ?: "kebenaran ditolak"}.")
        }
    }

    fun openUrl(context: Context, rawUrl: String): ActionResult {
        val input = rawUrl.trim()
        if (input.isBlank()) return ActionResult(false, "Pautan belum diberikan.")
        val candidate = if (input.startsWith("http://", true) || input.startsWith("https://", true)) {
            input
        } else {
            "https://$input"
        }
        val uri = runCatching { Uri.parse(candidate) }.getOrNull()
            ?: return ActionResult(false, "Format pautan tidak sah.")
        if (uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) {
            return ActionResult(false, "Hanya pautan HTTP atau HTTPS dibenarkan.")
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.applicationContext.startActivity(intent)
            ActionResult(true, "Pautan dibuka dalam aplikasi yang tersedia.")
        } catch (_: ActivityNotFoundException) {
            ActionResult(false, "Tiada aplikasi tersedia untuk membuka pautan ini.")
        } catch (error: SecurityException) {
            ActionResult(false, "Android menyekat pautan ini: ${error.message ?: "kebenaran ditolak"}.")
        }
    }

    fun shareText(context: Context, text: String): ActionResult {
        val content = text.trim()
        if (content.isBlank()) return ActionResult(false, "Tiada teks untuk dikongsi.")
        return try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
            }
            val chooser = Intent.createChooser(sendIntent, "Kongsi melalui aplikasi")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.applicationContext.startActivity(chooser)
            ActionResult(true, "Menu perkongsian dibuka. Pilih aplikasi dan sahkan sendiri sebelum menghantar.")
        } catch (_: ActivityNotFoundException) {
            ActionResult(false, "Tiada aplikasi yang boleh menerima teks untuk dikongsi.")
        } catch (error: SecurityException) {
            ActionResult(false, "Android menyekat perkongsian: ${error.message ?: "kebenaran ditolak"}.")
        }
    }

    fun openSettings(context: Context, page: SettingsPage): ActionResult {
        val action = when (page) {
            SettingsPage.GENERAL -> Settings.ACTION_SETTINGS
            SettingsPage.WIFI -> Settings.ACTION_WIFI_SETTINGS
            SettingsPage.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
        }
        return try {
            context.applicationContext.startActivity(
                Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            val label = when (page) {
                SettingsPage.GENERAL -> "Tetapan Android"
                SettingsPage.WIFI -> "Tetapan Wi-Fi"
                SettingsPage.BLUETOOTH -> "Tetapan Bluetooth"
            }
            ActionResult(true, "$label dibuka. Perubahan tetapan dibuat sendiri oleh pengguna.")
        } catch (_: ActivityNotFoundException) {
            ActionResult(false, "Halaman tetapan itu tidak tersedia pada peranti ini.")
        } catch (error: SecurityException) {
            ActionResult(false, "Android menyekat pembukaan tetapan: ${error.message ?: "kebenaran ditolak"}.")
        }
    }

    private fun normalize(value: String): String =
        value.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
}
