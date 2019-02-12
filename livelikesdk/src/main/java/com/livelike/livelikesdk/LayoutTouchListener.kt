package com.livelike.livelikesdk

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.ScrollView

/**
 *  Custom touch listener which would recognise the touch event on the view. Since widget view is wrapped around ScrollView
 *  and scrollView disable/override the child layout's touch listener, so we would need a custom touch listener which
 *  would request the parent view to not interfere in it's touch. Downfall here is we would have to manually handle the
 *  touch.
 */
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
            if (deltaX < 0 || deltaX > 0) {
                hideView()
            }
        } else {
            performActionOnViewClicked(viewTouched)
        }
        return true
    }

    private fun performActionOnViewClicked(viewTouched: View?) {
        action(viewTouched)
    }

    // Maybe move this method to better place.
    private fun hideView() { view.visibility = View.INVISIBLE }
}