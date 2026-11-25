package com.cortex.app.permissions

data class PermissionState(
    val notificationListener: Boolean,
    val usageStats: Boolean,
    val readCalendar: Boolean,
    val readContacts: Boolean,
    val postNotifications: Boolean
) {
    fun areAllGranted(): Boolean =
        notificationListener && usageStats && readCalendar && readContacts && postNotifications

    fun getPendingCount(): Int {
        var count = 0
        if (!notificationListener) count++
        if (!usageStats) count++
        if (!readCalendar) count++
        if (!readContacts) count++
        if (!postNotifications) count++
        return count
    }
}
