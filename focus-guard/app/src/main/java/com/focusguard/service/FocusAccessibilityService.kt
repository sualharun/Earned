package com.focusguard.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.focusguard.session.SessionManager

// Person 3: Accessibility service for app blocking and focus enforcement
class FocusAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val foregroundPackage = event.packageName?.toString() ?: return
        if (!SessionManager.isPackageBlocked(foregroundPackage)) return

        SessionManager.onBlockedAppOpened(foregroundPackage)
        launchFocusGuard()
    }

    override fun onInterrupt() = Unit

    private fun launchFocusGuard() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(launchIntent)
    }
}
