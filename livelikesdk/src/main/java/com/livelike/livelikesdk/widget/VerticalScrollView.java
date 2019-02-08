package com.livelike.livelikesdk.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewParent;
import android.widget.ScrollView;
import com.livelike.livelikesdk.LayoutTouchListener;

class VerticalScrollView extends ScrollView {
    Context context;
    public VerticalScrollView(Context context) {
        super(context);
        this.context = context;
    }

    public VerticalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public VerticalScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        switch (action)
        {
            case MotionEvent.ACTION_DOWN:
                super.onTouchEvent(ev);
                ViewParent p0 = getParent();
                if (p0 != null)
                    p0.requestDisallowInterceptTouchEvent(true);
                //this.setOnTouchListener(new LayoutTouchListener(context, this));
                return false; // redirect MotionEvents to

            case MotionEvent.ACTION_MOVE:
                super.onInterceptTouchEvent(ev);
                ViewParent p = getParent();
                if (p != null)
                    p.requestDisallowInterceptTouchEvent(true);
                //this.setOnTouchListener(new LayoutTouchListener(context, this));
                return false; // redirect MotionEvents to ourself

            case MotionEvent.ACTION_CANCEL:
                super.onTouchEvent(ev);
                break;

            case MotionEvent.ACTION_UP:
                super.onInterceptTouchEvent(ev);
                ViewParent p1 = getParent();
                if (p1 != null)
                    p1.requestDisallowInterceptTouchEvent(true);
                return false;

            default:  break;
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);
        Log.i("VerticalScrollview", "onTouchEvent. action: " + ev.getAction() );
        return true;
    }
}
