package com.alex.ai.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AlexAccessibilityService : AccessibilityService() {

    private val tag = "AlexVisualAgent"

    companion object {
        var instance: AlexAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val info = serviceInfo ?: android.accessibilityservice.AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.feedbackType = android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = info.flags or
            android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
            android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            android.accessibilityservice.AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        serviceInfo = info
        Log.d(tag, "Alex Visual Agent Accessibility Service connected & configured")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Monitors screen transitions in Instagram, Facebook, WhatsApp, etc.
    }

    override fun onInterrupt() {
        Log.d(tag, "Alex Visual Agent interrupted")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    /**
     * Inspect screen: Scans current active window nodes and extracts actionable elements
     */
    fun dumpScreenHierarchy(): List<String> {
        val root = rootInActiveWindow ?: return emptyList()
        val textList = mutableListOf<String>()
        traverseNodes(root, textList)
        return textList
    }

    private fun traverseNodes(node: AccessibilityNodeInfo?, result: MutableList<String>) {
        if (node == null) return
        val text = node.text?.toString() ?: node.contentDescription?.toString()
        if (!text.isNullOrBlank()) {
            result.add(text)
        }
        for (i in 0 until node.childCount) {
            traverseNodes(node.getChild(i), result)
        }
    }

    /**
     * Finds a clickable node matching text and performs a tap
     */
    fun clickText(targetText: String, requireSafetyCheck: Boolean = false): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(targetText)

        for (node in nodes) {
            if (node.isClickable) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            // Check parent if node itself isn't directly clickable
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                parent = parent.parent
            }
        }
        return false
    }

    /**
     * Type text into currently focused or first available editable field
     */
    fun typeText(textToType: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

        if (focused != null && focused.isEditable) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType)
            }
            return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }

        // Search for editable nodes
        val editable = findFirstEditableNode(root)
        if (editable != null) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType)
            }
            return editable.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }
        return false
    }

    private fun findFirstEditableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val res = findFirstEditableNode(node.getChild(i))
            if (res != null) return res
        }
        return null
    }

    /**
     * Dispatch touch click at explicit screen coordinate
     */
    fun tapCoordinates(x: Float, y: Float, onComplete: (() -> Unit)? = null) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 80)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onComplete?.invoke()
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.e(tag, "Tap gesture cancelled")
            }
        }, null)
    }

    /**
     * Global Navigation Actions: Back, Home, Recents, Notifications
     */
    fun goBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun goHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun openRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun openNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    fun openQuickSettings(): Boolean = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)

    /**
     * Robust Element Clicker (Matches Text, Content Description, View ID or Screen Bounds)
     */
    fun clickElement(target: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val cleanTarget = target.lowercase().trim()
        if (cleanTarget.isBlank()) return false

        val matchingNodes = mutableListOf<AccessibilityNodeInfo>()
        findMatchingNodes(root, cleanTarget, matchingNodes)

        for (node in matchingNodes) {
            if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true
                }
                parent = parent.parent
            }
            // Tap center coordinates using gesture fallback
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (!rect.isEmpty && rect.width() > 0 && rect.height() > 0) {
                tapCoordinates(rect.centerX().toFloat(), rect.centerY().toFloat())
                return true
            }
        }
        return false
    }

    private fun findMatchingNodes(node: AccessibilityNodeInfo?, target: String, list: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        val text = (node.text?.toString() ?: "").lowercase()
        val desc = (node.contentDescription?.toString() ?: "").lowercase()
        val viewId = (node.viewIdResourceName ?: "").lowercase()

        if (text.contains(target) || desc.contains(target) || viewId.contains(target)) {
            list.add(node)
        }
        for (i in 0 until node.childCount) {
            findMatchingNodes(node.getChild(i), target, list)
        }
    }

    /**
     * Universal Screen Scrolling & Swiping Gestures (Works in TikTok, Reels, Shorts, Feed, Web)
     */
    fun scrollDown(): Boolean {
        val root = rootInActiveWindow
        val nodeScrolled = root?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) ?: false
        if (nodeScrolled) return true

        val displayMetrics = resources.displayMetrics
        val startX = displayMetrics.widthPixels / 2f
        val startY = displayMetrics.heightPixels * 0.75f
        val endY = displayMetrics.heightPixels * 0.25f
        return swipeGesture(startX, startY, startX, endY)
    }

    fun scrollUp(): Boolean {
        val root = rootInActiveWindow
        val nodeScrolled = root?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) ?: false
        if (nodeScrolled) return true

        val displayMetrics = resources.displayMetrics
        val startX = displayMetrics.widthPixels / 2f
        val startY = displayMetrics.heightPixels * 0.25f
        val endY = displayMetrics.heightPixels * 0.75f
        return swipeGesture(startX, startY, startX, endY)
    }

    fun swipeLeft(): Boolean {
        val displayMetrics = resources.displayMetrics
        val startX = displayMetrics.widthPixels * 0.85f
        val endX = displayMetrics.widthPixels * 0.15f
        val y = displayMetrics.heightPixels / 2f
        return swipeGesture(startX, y, endX, y)
    }

    fun swipeRight(): Boolean {
        val displayMetrics = resources.displayMetrics
        val startX = displayMetrics.widthPixels * 0.15f
        val endX = displayMetrics.widthPixels * 0.85f
        val y = displayMetrics.heightPixels / 2f
        return swipeGesture(startX, y, endX, y)
    }

    private fun swipeGesture(startX: Float, startY: Float, endX: Float, endY: Float): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 300)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }
}
