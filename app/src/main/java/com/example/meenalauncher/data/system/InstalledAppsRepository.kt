package com.example.meenalauncher.data.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceAppInfo(
    val packageName: String,
    val label: String,
    val firstLetter: Char,
    val version: String,
    val iconBitmap: ImageBitmap? = null,
    val isRealApp: Boolean = true
)

object InstalledAppsRepository {

    suspend fun loadInstalledApps(context: Context): List<DeviceAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = try {
            pm.queryIntentActivities(mainIntent, 0)
        } catch (e: Exception) {
            emptyList()
        }

        val resultList = mutableListOf<DeviceAppInfo>()

        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            // Skip Meena Launcher itself in apps list if desired, or keep it
            val label = try {
                info.loadLabel(pm).toString()
            } catch (e: Exception) {
                pkg
            }

            val letter = label.firstOrNull()?.uppercaseChar()?.let {
                if (it in 'A'..'Z') it else '#'
            } ?: '#'

            var version = "v1.0"
            try {
                val pkgInfo = pm.getPackageInfo(pkg, 0)
                version = "v${pkgInfo.versionName ?: "1.0"}"
            } catch (e: Exception) {
                // Ignore
            }

            var imageBitmap: ImageBitmap? = null
            try {
                val drawable = info.loadIcon(pm)
                val bmp = drawableToBitmap(drawable)
                imageBitmap = bmp.asImageBitmap()
            } catch (e: Exception) {
                // Ignore icon error
            }

            resultList.add(
                DeviceAppInfo(
                    packageName = pkg,
                    label = label,
                    firstLetter = letter,
                    version = version,
                    iconBitmap = imageBitmap,
                    isRealApp = true
                )
            )
        }

        // Sort alphabetically
        resultList.sortBy { it.label.lowercase() }
        resultList
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
