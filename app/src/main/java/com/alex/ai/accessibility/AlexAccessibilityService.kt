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
        Log.d(tag, "Alex Visual Agent Accessibility Service connected")
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
     * Scroll down on active list
     */
    fun scrollDown(): Boolean {
        val root = rootInActiveWindow ?: return false
        return root.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }
}
