/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.misc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.RestrictTo;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.leanback.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.SeekBarSegment;

import java.util.ArrayList;
import java.util.List;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

/**
 * Apple TV Cinema enhanced SeekBar: smooth frosted pill track, glowing SponsorBlock segments,
 * and always-visible luminous white scrubber orb.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public final class SeekBar extends View {
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public abstract static class AccessibilitySeekListener {
        public abstract boolean onAccessibilitySeekForward();
        public abstract boolean onAccessibilitySeekBackward();
        public abstract boolean onAccessibilitySeekProgress(int progress);
    }

    private static class SeekBarRectangle {
        public final RectF rect = new RectF();
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    private final RectF mProgressRect = new RectF();
    private final RectF mSecondProgressRect = new RectF();
    private final RectF mBackgroundRect = new RectF();
    private final Paint mSecondProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mKnobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mKnobGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int mProgress;
    private int mSecondProgress;
    private int mMax;
    private float mKnobx;

    private int mActiveRadius;
    private int mBarHeight;
    private int mActiveBarHeight;
    private float mDensity;
    private float mFocusFraction = 0f;
    private android.animation.ValueAnimator mFocusAnimator;
    private List<SeekBarSegment> mPendingSegments;
    private final List<SeekBarRectangle> mSeekBarRectangles = new ArrayList<>();

    private AccessibilitySeekListener mAccessibilitySeekListener;

    public SeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        mDensity = context.getResources().getDisplayMetrics().density;
        // Frosted Apple TV Liquid Glass track palette
        mBackgroundPaint.setColor(0x38FFFFFF);
        mSecondProgressPaint.setColor(0x45FFFFFF);
        mProgressPaint.setColor(Color.WHITE);
        mKnobPaint.setColor(Color.WHITE);
        mKnobGlowPaint.setColor(0x52FFFFFF);
        mBarHeight = context.getResources().getDimensionPixelSize(
                R.dimen.lb_playback_transport_progressbar_bar_height);
        mActiveBarHeight = context.getResources().getDimensionPixelSize(
                R.dimen.lb_playback_transport_progressbar_active_bar_height);
        mActiveRadius = context.getResources().getDimensionPixelSize(
                R.dimen.lb_playback_transport_progressbar_active_radius);
    }

    public void setActiveRadius(int radius) {
        mActiveRadius = radius;
        calculate();
    }

    public void setBarHeight(int barHeight) {
        mBarHeight = barHeight;
        calculate();
    }

    public void setActiveBarHeight(int activeBarHeight) {
        mActiveBarHeight = activeBarHeight;
        calculate();
    }

    @Override
    protected void onFocusChanged(boolean gainFocus,
            int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        animateFocusFraction(gainFocus ? 1f : 0f);
    }

    private void animateFocusFraction(float target) {
        if (mFocusAnimator != null) {
            mFocusAnimator.cancel();
        }
        mFocusAnimator = android.animation.ValueAnimator.ofFloat(mFocusFraction, target);
        mFocusAnimator.setDuration(160L);
        mFocusAnimator.setInterpolator(androidx.core.view.animation.PathInterpolatorCompat.create(0.22f, 1.0f, 0.36f, 1.0f));
        mFocusAnimator.addUpdateListener(anim -> {
            mFocusFraction = (float) anim.getAnimatedValue();
            calculate();
        });
        mFocusAnimator.start();
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculate();
        if (mPendingSegments != null) {
            calculateSegments(mPendingSegments);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float barHeight = mBarHeight + (mActiveBarHeight - mBarHeight) * mFocusFraction;
        final float barRadius = barHeight / 2f;

        // 1. Frosted translucent track background
        canvas.drawRoundRect(mBackgroundRect, barRadius, barRadius, mBackgroundPaint);

        // 2. Buffered segment
        if (mSecondProgressRect.right > mSecondProgressRect.left) {
            canvas.drawRoundRect(mSecondProgressRect, barRadius, barRadius, mSecondProgressPaint);
        }

        // 3. Played luminous white fill
        if (mProgressRect.right > mProgressRect.left) {
            canvas.drawRoundRect(mProgressRect, barRadius, barRadius, mProgressPaint);
        }

        // 4. SponsorBlock / Chapter colored segments
        for (SeekBarRectangle rectangle : mSeekBarRectangles) {
            canvas.drawRoundRect(rectangle.rect, barRadius, barRadius, rectangle.paint);
        }

        // 5. Calm resting orb (5.5dp, zero halo) -> blooming focused orb (8.5dp + soft halo)
        final float cy = getHeight() / 2f;
        final float restRadius = 5.5f * mDensity;
        final float knobRadius = restRadius + (mActiveRadius - restRadius) * mFocusFraction;
        if (mFocusFraction > 0.02f) {
            int haloAlpha = Math.round(0x5E * mFocusFraction);
            mKnobGlowPaint.setColor((haloAlpha << 24) | 0x00FFFFFF);
            canvas.drawCircle(mKnobx, cy, knobRadius + (4.2f * mDensity * mFocusFraction), mKnobGlowPaint);
        }
        canvas.drawCircle(mKnobx, cy, knobRadius, mKnobPaint);
    }

    public void setProgress(int progress) {
        if (progress > mMax) {
            progress = mMax;
        } else if (progress < 0) {
            progress = 0;
        }
        mProgress = progress;
        calculate();
    }

    public void setSecondaryProgress(int progress) {
        if (progress > mMax) {
            progress = mMax;
        } else if (progress < 0) {
            progress = 0;
        }
        mSecondProgress = progress;
        calculate();
    }

    public int getProgress() {
        return mProgress;
    }

    public int getSecondProgress() {
        return mSecondProgress;
    }

    public int getMax() {
        return mMax;
    }

    public void setMax(int max) {
        this.mMax = max;
        calculate();
    }

    public void setProgressColor(int color) {
        mProgressPaint.setColor(color);
    }

    public void setSecondaryProgressColor(int color) {
        mSecondProgressPaint.setColor(color);
    }

    public int getSecondaryProgressColor() {
        return mSecondProgressPaint.getColor();
    }

    private void calculate() {
        final float barHeight = mBarHeight + (mActiveBarHeight - mBarHeight) * mFocusFraction;

        final int width = getWidth();
        final int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        final float verticalPadding = (height - barHeight) / 2f;
        final float edgeInset = mActiveRadius;
        final float trackLeft = Math.min(edgeInset, width * 0.1f);
        final float trackRight = Math.max(trackLeft, width - trackLeft);
        final float trackWidth = Math.max(1f, trackRight - trackLeft);

        mBackgroundRect.set(trackLeft, verticalPadding, trackRight, height - verticalPadding);

        final float max = mMax > 0 ? (float) mMax : 1f;
        final float progressPixels = (mProgress / max) * trackWidth;
        mProgressRect.set(trackLeft, verticalPadding, trackLeft + progressPixels, height - verticalPadding);

        final float secondProgressPixels = (mSecondProgress / max) * trackWidth;
        final float secondRight = Math.max(mProgressRect.right, trackLeft + secondProgressPixels);
        mSecondProgressRect.set(mProgressRect.right, verticalPadding, Math.min(trackRight, secondRight), height - verticalPadding);

        mKnobx = trackLeft + progressPixels;

        for (SeekBarRectangle seekBarRectangle : mSeekBarRectangles) {
            seekBarRectangle.rect.set(seekBarRectangle.rect.left, verticalPadding,
                    seekBarRectangle.rect.right, height - verticalPadding);
        }

        invalidate();
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return SeekBar.class.getName();
    }

    public void setAccessibilitySeekListener(AccessibilitySeekListener listener) {
        mAccessibilitySeekListener = listener;
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (mAccessibilitySeekListener != null) {
            switch (action) {
                case AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD:
                    return mAccessibilitySeekListener.onAccessibilitySeekForward();
                case AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD:
                    return mAccessibilitySeekListener.onAccessibilitySeekBackward();
            }
        }
        return super.performAccessibilityAction(action, arguments);
    }

    public void setSegments(List<SeekBarSegment> segments) {
        mPendingSegments = segments;
        calculateSegments(segments);
        invalidate();
    }

    private void calculateSegments(List<SeekBarSegment> segments) {
        mSeekBarRectangles.clear();
        if (segments == null || segments.isEmpty()) {
            return;
        }

        final int width = getWidth();
        final int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        final int barHeight = isFocused() ? mActiveBarHeight : mBarHeight;
        final float verticalPadding = (height - barHeight) / 2f;
        final float trackLeft = mBackgroundRect.left;
        final float trackWidth = Math.max(1f, mBackgroundRect.width());

        for (SeekBarSegment segment : segments) {
            if (segment.endProgress < 0 || segment.startProgress < 0 || segment.startProgress >= 1) {
                continue;
            }

            float endProgress = Math.min(1f, segment.endProgress);
            float leftPixels = trackLeft + segment.startProgress * trackWidth;
            float rightPixels = trackLeft + endProgress * trackWidth;

            float minSegWidth = Math.max(3f * mDensity, mBarHeight / 2f);
            if (rightPixels - leftPixels < minSegWidth) {
                rightPixels = Math.min(mBackgroundRect.right, leftPixels + minSegWidth);
            }

            SeekBarRectangle rect = new SeekBarRectangle();
            rect.rect.set(leftPixels, verticalPadding, rightPixels, height - verticalPadding);
            rect.paint.setColor(segment.color);
            mSeekBarRectangles.add(rect);
        }
    }

    // Touch interceptor
    private int mScaledTouchSlop;
    private int mPaddingLeft;
    private int mPaddingRight;
    private int mTouchProgressOffset;
    private float mTouchDownX;
    private boolean mIsDragging;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchDownX = event.getX();
                startDrag(event);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsDragging) {
                    trackTouchEvent(event);
                } else {
                    final float x = event.getX();
                    if (Math.abs(x - mTouchDownX) > mScaledTouchSlop) {
                        startDrag(event);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                }
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate();
                break;
        }
        return true;
    }

    private void startDrag(MotionEvent event) {
        setPressed(true);
        onStartTrackingTouch();
        trackTouchEvent(event);
        attemptClaimDrag();
    }

    private void setHotspot(float x, float y) {
        if (VERSION.SDK_INT < 21) {
            return;
        }
        final Drawable bg = getBackground();
        if (bg != null) {
            bg.setHotspot(x, y);
        }
    }

    private void trackTouchEvent(MotionEvent event) {
        final float x = event.getX();
        final float trackLeft = mBackgroundRect.left;
        final float trackWidth = Math.max(1f, mBackgroundRect.width());

        final float scale;
        if (x < trackLeft) {
            scale = 0.0f;
        } else if (x > mBackgroundRect.right) {
            scale = 1.0f;
        } else {
            scale = (x - trackLeft) / trackWidth;
        }

        final int range = getMax() - getMin();
        float progress = mTouchProgressOffset + scale * range + getMin();

        setHotspot(x, event.getY());
        setProgress(Math.round(progress));
        if (mAccessibilitySeekListener != null) {
            mAccessibilitySeekListener.onAccessibilitySeekProgress(mProgress);
        }
    }

    private void attemptClaimDrag() {
    }

    void onStartTrackingTouch() {
        mIsDragging = true;
    }

    void onStopTrackingTouch() {
        mIsDragging = false;
    }

    public synchronized int getMin() {
        return 0;
    }
}
