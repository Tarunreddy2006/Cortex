package com.cortex.app.discovery

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import com.cortex.app.database.CortexDatabase
import com.cortex.app.database.entities.AppSelectionEntity
import com.cortex.app.privacy.FinancialExclusionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.util.Locale

class AppDiscoveryService(private val context: Context) {

    private val db = CortexDatabase.getInstance(context)

    suspend fun discoverInstalledApps(): List<AppIntegrationProfile> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installedPackages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
        val savedSelections = db.appSelectionDao().getAllSelections().associateBy { it.packageName }

        val profiles = mutableListOf<AppIntegrationProfile>()

        for (pkgInfo in installedPackages) {
            val appInfo = pkgInfo.applicationInfo ?: continue
            val packageName = pkgInfo.packageName

            // Skip Cortex itself
            if (packageName == context.packageName) continue

            val appName = pm.getApplicationLabel(appInfo).toString()
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            // Categorize app
            val category = categorizeApp(packageName, appInfo)

            // Determine capabilities honestly
            val capabilities = determineCapabilities(packageName, category)

            // Saved state (default enabled for Google/Communication/Education/Productivity if non-financial)
            val isFin = (category == AppCategory.FINANCIAL)
            val saved = savedSelections[packageName]
            val isEnabled = if (isFin) {
                false // NEVER enable financial apps
            } else {
                saved?.isEnabled ?: (category in setOf(
                    AppCategory.GOOGLE,
                    AppCategory.COMMUNICATION,
                    AppCategory.EDUCATION,
                    AppCategory.PRODUCTIVITY
                ) && !isSystemApp)
            }

            // Encode icon as data URL
            val iconBase64 = encodeAppIcon(appInfo.loadIcon(pm))

            val profile = AppIntegrationProfile(
                packageName = packageName,
                appName = appName,
                category = category,
                isSystemApp = isSystemApp,
                isEnabled = isEnabled,
                supportedCapabilities = capabilities,
                iconBase64 = iconBase64,
                versionName = pkgInfo.versionName
            )
            profiles.add(profile)
        }

        // Sort: Non-financial first, then Google, Communication, Education, etc.
        profiles.sortedWith(
            compareBy<AppIntegrationProfile> { it.category == AppCategory.FINANCIAL }
                .thenBy { it.category.ordinal }
                .thenBy { it.appName.lowercase(Locale.ROOT) }
        )
    }

    suspend fun saveSelections(selections: Map<String, Boolean>) = withContext(Dispatchers.IO) {
        val entities = mutableListOf<AppSelectionEntity>()
        val currentSelections = db.appSelectionDao().getAllSelections().associateBy { it.packageName }

        for ((pkg, enabled) in selections) {
            // Strict security check: Financial apps can NEVER be enabled
            val isFinancial = FinancialExclusionPolicy.isFinancialPackage(pkg)
            val effectiveEnabled = if (isFinancial) false else enabled

            val existing = currentSelections[pkg]
            if (existing != null) {
                entities.add(existing.copy(isEnabled = effectiveEnabled, updatedAt = System.currentTimeMillis()))
            } else {
                entities.add(
                    AppSelectionEntity(
                        packageName = pkg,
                        appName = FinancialExclusionPolicy.getKnownAppName(pkg),
                        category = if (isFinancial) AppCategory.FINANCIAL.name else AppCategory.OTHER.name,
                        isEnabled = effectiveEnabled,
                        isSystemApp = false,
                        supportedCapabilitiesJson = "[]"
                    )
                )
            }
        }
        db.appSelectionDao().insertOrUpdateAll(entities)
    }

    private fun categorizeApp(packageName: String, appInfo: ApplicationInfo): AppCategory {
        val pkg = packageName.lowercase(Locale.ROOT)

        // 1. Financial Exclusion Gate (Highest Priority)
        if (FinancialExclusionPolicy.isFinancialPackage(pkg)) {
            return AppCategory.FINANCIAL
        }

        // 2. Google Apps
        if (pkg.startsWith("com.google.android.") || pkg == "com.google.android.gm" || pkg.startsWith("com.google.apps.")) {
            return AppCategory.GOOGLE
        }

        // 3. Communication
        if (pkg in setOf(
                "com.whatsapp", "org.telegram.messenger", "org.thoughtcrime.securesms",
                "com.discord", "com.slack", "com.skype.raider", "com.viber.voip"
            ) || pkg.contains("messenger") || pkg.contains("chat")
        ) {
            return AppCategory.COMMUNICATION
        }

        // 4. Education
        if (pkg.contains("classroom") || pkg.contains("coursera") || pkg.contains("duolingo") ||
            pkg.contains("udemy") || pkg.contains("khanacademy") || pkg.contains("edx") ||
            pkg.contains("moodle") || pkg.contains("canvas") || pkg.contains("blackboard")
        ) {
            return AppCategory.EDUCATION
        }

        // 5. Productivity
        if (pkg.contains("notion") || pkg.contains("obsidian") || pkg.contains("trello") ||
            pkg.contains("asana") || pkg.contains("todoist") || pkg.contains("evernote") ||
            pkg.contains("notes") || pkg.contains("calendar") || pkg.contains("tasks") ||
            pkg.contains("docs") || pkg.contains("sheets") || pkg.contains("office")
        ) {
            return AppCategory.PRODUCTIVITY
        }

        // 6. Social
        if (pkg.contains("instagram") || pkg.contains("twitter") || pkg.contains("reddit") ||
            pkg.contains("linkedin") || pkg.contains("facebook") || pkg.contains("tiktok")
        ) {
            return AppCategory.SOCIAL
        }

        // 7. Browsers
        if (pkg.contains("chrome") || pkg.contains("browser") || pkg.contains("firefox") ||
            pkg.contains("opera") || pkg.contains("brave") || pkg.contains("edge")
        ) {
            return AppCategory.BROWSERS
        }

        return AppCategory.OTHER
    }

    private fun determineCapabilities(packageName: String, category: AppCategory): List<String> {
        val capabilities = mutableListOf<String>()
        val pkg = packageName.lowercase(Locale.ROOT)

        if (category == AppCategory.FINANCIAL) {
            return listOf("EXCLUDED")
        }

        // Official API
        if (pkg in setOf(
                "com.google.android.gm",
                "com.google.android.apps.docs",
                "com.google.android.apps.classroom"
            )
        ) {
            capabilities.add("OFFICIAL_API")
        }

        // System Provider
        if (pkg.contains("calendar") || pkg.contains("contacts")) {
            capabilities.add("SYSTEM_PROVIDER")
        }

        // Notification Integration
        if (category in setOf(AppCategory.COMMUNICATION, AppCategory.SOCIAL, AppCategory.GOOGLE, AppCategory.PRODUCTIVITY)) {
            capabilities.add("NOTIFICATION")
        }

        // Share & Import
        if (category in setOf(AppCategory.BROWSERS, AppCategory.COMMUNICATION, AppCategory.PRODUCTIVITY)) {
            capabilities.add("SHARE_IMPORT")
        }

        if (capabilities.isEmpty()) {
            capabilities.add("UNSUPPORTED")
        }

        return capabilities
    }

    private fun encodeAppIcon(drawable: Drawable): String? {
        return try {
            val bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
                drawable.bitmap
            } else {
                val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
                val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
                val bmp = Bitmap.createBitmap(width.coerceAtMost(96), height.coerceAtMost(96), Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bmp
            }

            val scaled = if (bitmap.width > 96 || bitmap.height > 96) {
                Bitmap.createScaledBitmap(bitmap, 96, 96, true)
            } else {
                bitmap
            }

            val stream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.PNG, 85, stream)
            val bytes = stream.toByteArray()
            "data:image/png;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }
}
