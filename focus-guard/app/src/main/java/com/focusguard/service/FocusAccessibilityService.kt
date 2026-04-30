package com.focusguard.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.focusguard.session.SessionManager

/**
 * Person 3 (Sanjiv) owns this file.
 * Monitors foreground app changes and bounces blacklisted apps during active sessions.
 */
class FocusAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return
        val state = SessionManager.stateFlow.value

        if (!state.isActive) return
        if (pkg == "com.android.dialer") return // always allow emergency calls
        if (pkg == packageName) return // don't block ourselves

        if (pkg in state.blacklistedApps) {
            SessionManager.onBlockedAppOpened(pkg)
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }

    override fun onInterrupt() {}
}
