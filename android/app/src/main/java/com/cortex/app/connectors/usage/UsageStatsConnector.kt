package com.cortex.app.connectors.usage

import android.app.usage.UsageStatsManager
import android.content.Context
import com.cortex.app.database.CortexDatabase
import com.cortex.app.privacy.FinancialExclusionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

data class AppUsageSummary(
    val packageName: String,
    val appName: String,
    val dailyMinutes: Long,
    val weeklyMinutes: Long,
    val lastTimeUsedMs: Long
)

class UsageStatsConnector(private val context: Context) {

    private val db = CortexDatabase.getInstance(context)

    suspend fun getUsageSummaries(): List<AppUsageSummary> = withContext(Dispatchers.IO) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return@withContext emptyList()

        val enabledApps = db.appSelectionDao().getEnabledSelections().associateBy { it.packageName }
        val userExclusions = db.financialExclusionDao().getAllExcludedPackages().toSet()

        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        // Daily interval (start of today)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfToday = calendar.timeInMillis

        // Weekly interval (7 days ago)
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startOfWeek = calendar.timeInMillis

        val dailyStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startOfToday,
            now
        ) ?: emptyList()

        val weeklyStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_WEEKLY,
            startOfWeek,
            now
        ) ?: emptyList()

        val weeklyMap = weeklyStats.groupBy { it.packageName }

        val summaries = mutableListOf<AppUsageSummary>()

        for (stat in dailyStats) {
            val pkg = stat.packageName

            // 1. Check financial exclusion: ALWAYS EXCLUDE
            if (FinancialExclusionPolicy.isFinancialPackage(pkg, userExclusions)) {
                continue
            }

            // 2. Check user allowlist: Only track user-selected apps
            val appEntity = enabledApps[pkg] ?: continue

            val dailyMinutes = stat.totalTimeInForeground / (1000 * 60)
            val weeklyMinutes = weeklyMap[pkg]?.sumOf { it.totalTimeInForeground }?.div(1000 * 60) ?: dailyMinutes

            summaries.add(
                AppUsageSummary(
                    packageName = pkg,
                    appName = appEntity.appName,
                    dailyMinutes = dailyMinutes,
                    weeklyMinutes = weeklyMinutes,
                    lastTimeUsedMs = stat.lastTimeUsed
                )
            )
        }

        summaries.sortedByDescending { it.dailyMinutes }
    }
}
