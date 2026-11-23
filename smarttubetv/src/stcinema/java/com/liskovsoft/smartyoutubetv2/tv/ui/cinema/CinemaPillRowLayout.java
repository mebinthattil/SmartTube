package com.liskovsoft.smartyoutubetv2.tv.ui.cinema;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Horizontal Apple TV player pill row that routes DPAD_DOWN from the SeekBar directly
 * onto the Pause/Play pill for effortless living-room remote ergonomics.
 */
public class CinemaPillRowLayout extends LinearLayout {

    public CinemaPillRowLayout(Context context) {
        super(context);
    }

    public CinemaPillRowLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CinemaPillRowLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        if (direction == View.FOCUS_DOWN) {
            View playPause = findViewById(R.id.cinema_btn_play_pause);
            if (playPause != null && playPause.getVisibility() == View.VISIBLE
                    && playPause.isFocusable() && playPause.requestFocus()) {
                return true;
            }
        }
        return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
    }
}
