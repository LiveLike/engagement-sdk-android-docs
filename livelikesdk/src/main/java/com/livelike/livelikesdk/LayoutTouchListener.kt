package com.livelike.livelikesdk

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.ScrollView

class LayoutTouchListener(private val view: View,
                          private val scrollview: ScrollView) : View.OnTouchListener {

    private var action : (View?) -> Unit = {}
    constructor(view: View,
                scrollview: ScrollView,
                action: (View?) -> Unit) : this(view, scrollview) {
        this.action = action
    }

    private var downX : Float = 0.0f
    private var downY : Float = 0.0f
    private var upX : Float = 0.0f
    private var upY : Float = 0.0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                scrollview.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                scrollview.requestDisallowInterceptTouchEvent(false)

                upX = event.x
                upY = event.y

                val deltaX = downX - upX
                return swipeHorizontally(deltaX, v)
            }
            MotionEvent.ACTION_CANCEL -> {
                scrollview.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP -> {
                scrollview.requestDisallowInterceptTouchEvent(true)
            }
        }
        return false
    }

    private fun swipeHorizontally(deltaX: Float, viewTouched: View?): Boolean {
        if (Math.abs(deltaX) > 100) {
            if (deltaX < 0) {
                hideView()
                return true
            }
            if (deltaX > 0) {
                hideView()
                return true
            }
            return true
        } else {
            action(viewTouched)
        }
        return true
    }

    // Maybe move this method to better place.
    private fun hideView() { view.visibility = View.INVISIBLE }
}