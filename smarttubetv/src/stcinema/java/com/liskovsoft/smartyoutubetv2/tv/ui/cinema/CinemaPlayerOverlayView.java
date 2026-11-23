package com.liskovsoft.smartyoutubetv2.tv.ui.cinema;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.animation.PathInterpolatorCompat;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerUI;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Full-screen Apple tvOS Cinema Player Overlay (Row 0 of PlaybackFragment).
 * Places Channel + Bold Title top-left, one quiet technical badge top-right,
 * unobstructed video in the middle, and a concentric 26dp glass capsule at the bottom.
 */
public class CinemaPlayerOverlayView extends FrameLayout {

    private static final long SYNC_INTERVAL_MS = 450L;
    private static final Interpolator EASE_OUT = PathInterpolatorCompat.create(0.22f, 1.0f, 0.36f, 1.0f);
    private static final Interpolator EASE_IN = PathInterpolatorCompat.create(0.4f, 0.0f, 1.0f, 1.0f);

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    // Scrims & main sections
    private View mScrimTop;
    private View mScrimBottom;
    private View mTopHeader;
    private View mTransportRow;
    private TextView mEyebrowView;
    private TextView mTitleView;
    private TextView mQualityPill;
    private TextView mSpeedPill;
    private TextView mSponsorBlockPill;

    // Hidden Leanback bridge views
    private TextView mLegacyQualityInfo;
    private View mThumbsWrapper;

    // Bottom Pill Control Buttons
    private View mPillRow;
    private LinearLayout mBtnRewind;
    private ImageView mIconRewind;
    private TextView mLabelRewind;
    private LinearLayout mBtnPlayPause;
    private ImageView mIconPlayPause;
    private TextView mLabelPlayPause;
    private LinearLayout mBtnForward;
    private ImageView mIconForward;
    private TextView mLabelForward;
    private LinearLayout mBtnUpNext;
    private ImageView mIconUpNext;
    private TextView mLabelUpNext;
    private LinearLayout mBtnCaptions;
    private ImageView mIconCaptions;
    private TextView mLabelCaptions;
    private LinearLayout mBtnQuality;
    private ImageView mIconQuality;
    private TextView mLabelQuality;
    private LinearLayout mBtnMore;
    private ImageView mIconMore;
    private TextView mLabelMore;

    private String mLastVideoId;
    private String mLastParsedQuality;
    private boolean mLastPlayingState = true;
    private boolean mSyncRunning = false;

    private final Runnable mSyncRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mSyncRunning || !isAttachedToWindow()) {
                return;
            }
            syncHudState();
            mHandler.postDelayed(this, SYNC_INTERVAL_MS);
        }
    };

    public CinemaPlayerOverlayView(@NonNull Context context) {
        super(context);
    }

    public CinemaPlayerOverlayView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CinemaPlayerOverlayView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int exactHeightSpec = MeasureSpec.makeMeasureSpec(screenHeight, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, exactHeightSpec);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mScrimTop = findViewById(R.id.cinema_player_scrim_top);
        mScrimBottom = findViewById(R.id.cinema_player_scrim_bottom);
        mTopHeader = findViewById(R.id.cinema_player_top_header);
        mTransportRow = findViewById(R.id.transport_row);
        mEyebrowView = findViewById(R.id.cinema_hud_eyebrow);
        mTitleView = findViewById(R.id.cinema_hud_title);
        mQualityPill = findViewById(R.id.cinema_pill_quality);
        mSpeedPill = findViewById(R.id.cinema_pill_speed);
        mSponsorBlockPill = findViewById(R.id.cinema_pill_sb);

        mLegacyQualityInfo = findViewById(R.id.quality_info);
        mThumbsWrapper = findViewById(R.id.thumbs_row_wrapper);

        mPillRow = findViewById(R.id.cinema_pill_controls_row);

        mBtnRewind = findViewById(R.id.cinema_btn_rewind);
        mIconRewind = findViewById(R.id.cinema_icon_rewind);
        mLabelRewind = findViewById(R.id.cinema_label_rewind);

        mBtnPlayPause = findViewById(R.id.cinema_btn_play_pause);
        mIconPlayPause = findViewById(R.id.cinema_icon_play_pause);
        mLabelPlayPause = findViewById(R.id.cinema_label_play_pause);

        mBtnForward = findViewById(R.id.cinema_btn_forward);
        mIconForward = findViewById(R.id.cinema_icon_forward);
        mLabelForward = findViewById(R.id.cinema_label_forward);

        mBtnUpNext = findViewById(R.id.cinema_btn_upnext);
        mIconUpNext = findViewById(R.id.cinema_icon_upnext);
        mLabelUpNext = findViewById(R.id.cinema_label_upnext);

        mBtnCaptions = findViewById(R.id.cinema_btn_captions);
        mIconCaptions = findViewById(R.id.cinema_icon_captions);
        mLabelCaptions = findViewById(R.id.cinema_label_captions);

        mBtnQuality = findViewById(R.id.cinema_btn_quality);
        mIconQuality = findViewById(R.id.cinema_icon_quality);
        mLabelQuality = findViewById(R.id.cinema_label_quality);

        mBtnMore = findViewById(R.id.cinema_btn_more);
        mIconMore = findViewById(R.id.cinema_icon_more);
        mLabelMore = findViewById(R.id.cinema_label_more);

        setupPill(mBtnRewind, mIconRewind, mLabelRewind, v -> seekByOffset(-1));
        setupPill(mBtnPlayPause, mIconPlayPause, mLabelPlayPause, v -> togglePlayPause());
        setupPill(mBtnForward, mIconForward, mLabelForward, v -> seekByOffset(1));
        setupPill(mBtnUpNext, mIconUpNext, mLabelUpNext, v -> openUpNext());
        setupPill(mBtnCaptions, mIconCaptions, mLabelCaptions, v -> openCaptions());
        setupPill(mBtnQuality, mIconQuality, mLabelQuality, v -> openQualityDialog());
        setupPill(mBtnMore, mIconMore, mLabelMore, v -> openMoreMenu());

        if (mLegacyQualityInfo != null) {
            mLegacyQualityInfo.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    parseQualityString(s != null ? s.toString() : "");
                }
            });
        }

        // Route DPAD_DOWN from SeekBar directly onto Pause pill & DPAD_UP from pills back to SeekBar
        View seekBar = findViewById(R.id.playback_progress);
        if (seekBar != null && mBtnPlayPause != null) {
            seekBar.setNextFocusDownId(R.id.cinema_btn_play_pause);
            seekBar.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == android.view.KeyEvent.ACTION_DOWN
                        && keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {
                    mBtnPlayPause.requestFocus();
                    return true;
                }
                return false;
            });
        }
        View[] allPills = {mBtnRewind, mBtnPlayPause, mBtnForward, mBtnUpNext, mBtnCaptions, mBtnQuality, mBtnMore};
        for (View p : allPills) {
            if (p != null) {
                p.setNextFocusUpId(R.id.playback_progress);
            }
        }

        if (mSponsorBlockPill != null) {
            mSponsorBlockPill.setVisibility(View.GONE);
        }

        syncHudState();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mSyncRunning = true;
        mHandler.removeCallbacks(mSyncRunnable);
        mHandler.post(mSyncRunnable);
    }

    @Override
    protected void onDetachedFromWindow() {
        mSyncRunning = false;
        mHandler.removeCallbacks(mSyncRunnable);
        super.onDetachedFromWindow();
    }

    /**
     * Silky Apple tvOS entrance glide when Player HUD opens.
     */
    public void animateHudEntrance() {
        float density = getResources().getDisplayMetrics().density;
        if (mScrimTop != null) {
            mScrimTop.animate().cancel();
            mScrimTop.setAlpha(0f);
            mScrimTop.animate().alpha(1f).setDuration(220L).setInterpolator(EASE_OUT).start();
        }
        if (mScrimBottom != null) {
            mScrimBottom.animate().cancel();
            mScrimBottom.setAlpha(0f);
            mScrimBottom.animate().alpha(1f).setDuration(220L).setInterpolator(EASE_OUT).start();
        }
        if (mTopHeader != null && mTopHeader.getVisibility() == View.VISIBLE) {
            mTopHeader.animate().cancel();
            mTopHeader.setAlpha(0f);
            mTopHeader.setTranslationY(-14f * density);
            mTopHeader.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(220L)
                    .setInterpolator(EASE_OUT)
                    .setListener(null)
                    .start();
        }
        if (mTransportRow != null) {
            mTransportRow.animate().cancel();
            mTransportRow.setAlpha(0f);
            mTransportRow.setTranslationY(20f * density);
            mTransportRow.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(240L)
                    .setInterpolator(EASE_OUT)
                    .setListener(null)
                    .start();
        }
    }

    /**
     * Silky Apple tvOS exit glide when Player HUD dismisses (eliminates abrupt 0ms pop-out).
     */
    public void animateHudExit(@Nullable final Runnable onEnd) {
        float density = getResources().getDisplayMetrics().density;
        if (mScrimTop != null) {
            mScrimTop.animate().cancel();
            mScrimTop.animate().alpha(0f).setDuration(170L).setInterpolator(EASE_IN).start();
        }
        if (mScrimBottom != null) {
            mScrimBottom.animate().cancel();
            mScrimBottom.animate().alpha(0f).setDuration(170L).setInterpolator(EASE_IN).start();
        }
        if (mTopHeader != null) {
            mTopHeader.animate().cancel();
            mTopHeader.animate()
                    .alpha(0f)
                    .translationY(-10f * density)
                    .setDuration(170L)
                    .setInterpolator(EASE_IN)
                    .setListener(null)
                    .start();
        }
        if (mTransportRow != null) {
            mTransportRow.animate().cancel();
            mTransportRow.animate()
                    .alpha(0f)
                    .translationY(14f * density)
                    .setDuration(180L)
                    .setInterpolator(EASE_IN)
                    .setListener(new AnimatorListenerAdapter() {
                        private boolean mHandled = false;
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (!mHandled) {
                                mHandled = true;
                                mTransportRow.animate().setListener(null);
                                if (onEnd != null) {
                                    onEnd.run();
                                }
                            }
                        }
                        @Override
                        public void onAnimationCancel(Animator animation) {
                            mHandled = true;
                            mTransportRow.animate().setListener(null);
                        }
                    })
                    .start();
        } else if (onEnd != null) {
            onEnd.run();
        }
    }

    private void setupPill(@Nullable View pill, @Nullable ImageView icon, @Nullable TextView label,
                           @NonNull OnClickListener clickListener) {
        if (pill == null) {
            return;
        }
        pill.setOnClickListener(v -> {
            resetUiTimeout();
            clickListener.onClick(v);
        });
        pill.setOnFocusChangeListener((v, hasFocus) -> {
            resetUiTimeout();
            applyPillVisualState(v, icon, label, hasFocus, true);
        });
        applyPillVisualState(pill, icon, label, pill.hasFocus(), false);
    }

    private void applyPillVisualState(@NonNull View pill, @Nullable ImageView icon,
                                      @Nullable TextView label, boolean focused, boolean animate) {
        int color = ContextCompat.getColor(getContext(),
                focused ? R.color.cinema_text_dark : R.color.cinema_text_primary);
        if (icon != null) {
            icon.setImageTintList(ColorStateList.valueOf(color));
        }
        if (label != null) {
            label.setTextColor(color);
        }
        pill.animate().cancel();
        float targetScale = focused ? 1.05f : 1.0f;
        if (animate) {
            if (focused) {
                pill.setAlpha(0.86f);
            }
            pill.animate()
                    .scaleX(targetScale)
                    .scaleY(targetScale)
                    .alpha(1.0f)
                    .setDuration(focused ? 180L : 150L)
                    .setInterpolator(EASE_OUT)
                    .start();
        } else {
            pill.setScaleX(targetScale);
            pill.setScaleY(targetScale);
            pill.setAlpha(1.0f);
        }
    }

    private void syncHudState() {
        Context ctx = getContext();
        if (ctx == null) {
            return;
        }
        PlaybackPresenter presenter = PlaybackPresenter.instance(ctx);
        PlaybackView player = presenter != null ? presenter.getPlayer() : null;
        Video video = presenter != null ? presenter.getVideo() : null;

        // 1. Sync compact seek-mode visibility (hide pills during storyboard scrubbing)
        boolean inSeekThumbs = mThumbsWrapper != null && mThumbsWrapper.getVisibility() == View.VISIBLE;
        if (mPillRow != null) {
            int desired = inSeekThumbs ? View.GONE : View.VISIBLE;
            if (mPillRow.getVisibility() != desired) {
                mPillRow.setVisibility(desired);
            }
        }
        if (mTopHeader != null) {
            int desiredHeader = inSeekThumbs ? View.INVISIBLE : View.VISIBLE;
            if (mTopHeader.getVisibility() != desiredHeader) {
                mTopHeader.setVisibility(desiredHeader);
            }
        }

        // 2. Sync Top-Left Channel Eyebrow & Title
        if (video != null) {
            String vidKey = video.videoId + "|" + video.getTitleFull() + "|" + video.getAuthor();
            if (!vidKey.equals(mLastVideoId)) {
                mLastVideoId = vidKey;
                updateTopLeftMetadata(video);
            }
        }

        // 3. Sync Seek increment labels (-10s / +10s)
        long seekSec = Math.max(5L, PlayerData.instance(ctx).getSeekIncrementMs() / 1000L);
        if (mLabelRewind != null) {
            String rew = "-" + seekSec + "s";
            if (!rew.equals(mLabelRewind.getText().toString())) {
                mLabelRewind.setText(rew);
            }
        }
        if (mLabelForward != null) {
            String fwd = "+" + seekSec + "s";
            if (!fwd.equals(mLabelForward.getText().toString())) {
                mLabelForward.setText(fwd);
            }
        }

        // 4. Sync Play / Pause pill state
        boolean isPlaying = player == null || player.getPlayWhenReady();
        if (isPlaying != mLastPlayingState) {
            mLastPlayingState = isPlaying;
            if (mIconPlayPause != null) {
                mIconPlayPause.setImageResource(isPlaying ? R.drawable.ic_cinema_pause : R.drawable.ic_cinema_play);
                applyPillVisualState(mBtnPlayPause, mIconPlayPause, mLabelPlayPause,
                        mBtnPlayPause != null && mBtnPlayPause.hasFocus(), false);
            }
            if (mLabelPlayPause != null) {
                mLabelPlayPause.setText(isPlaying ? "Pause" : "Play");
            }
        }

        // 5. Sync Speed pill — ONLY visible when playback speed != 1.0x (Apple visual restraint)
        float speed = player != null ? player.getSpeed() : 1.0f;
        if (speed <= 0f) {
            speed = 1.0f;
        }
        if (mSpeedPill != null) {
            boolean isNormalSpeed = Math.abs(speed - 1.0f) < 0.05f;
            if (isNormalSpeed) {
                mSpeedPill.setVisibility(View.GONE);
            } else {
                String speedStr = (Math.abs(speed - Math.round(speed * 10f) / 10f) < 0.01f)
                        ? String.format(Locale.US, "%.1f×", speed)
                        : String.format(Locale.US, "%.2f×", speed);
                if (!speedStr.equals(mSpeedPill.getText().toString())) {
                    mSpeedPill.setText(speedStr);
                }
                mSpeedPill.setVisibility(View.VISIBLE);
            }
        }

        // 6. Keep SponsorBlock badge hidden in HUD (timeline segments & 5s popup communicate it cleanly)
        if (mSponsorBlockPill != null && mSponsorBlockPill.getVisibility() != View.GONE) {
            mSponsorBlockPill.setVisibility(View.GONE);
        }

        // 7. Re-sync quality text if missed before watcher attached
        if (mLegacyQualityInfo != null && mLegacyQualityInfo.getText() != null) {
            String rawQ = mLegacyQualityInfo.getText().toString();
            if (!TextUtils.isEmpty(rawQ) && !rawQ.equals(mLastParsedQuality)) {
                parseQualityString(rawQ);
            }
        }
    }

    private void updateTopLeftMetadata(@NonNull Video video) {
        if (mEyebrowView != null) {
            StringBuilder eyebrow = new StringBuilder();
            String author = video.getAuthor();
            if (!TextUtils.isEmpty(author)) {
                eyebrow.append(author.trim().toUpperCase(Locale.US));
            }
            if (video.getSecondTitle() != null) {
                String second = video.getSecondTitle().toString().trim();
                if (!TextUtils.isEmpty(author) && second.startsWith(author)) {
                    second = second.substring(author.length()).replaceFirst("^\\s*[•·\\-]+\\s*", "");
                }
                // Normalize all bullets/separators to one consistent thin middot ("  ·  ")
                second = second.replaceAll("\\s*[•·]\\s*", "  ·  ").trim();
                if (!TextUtils.isEmpty(second)) {
                    if (eyebrow.length() > 0) {
                        eyebrow.append("  ·  ");
                    }
                    eyebrow.append(second.toUpperCase(Locale.US));
                }
            }
            mEyebrowView.setText(eyebrow.toString());
            mEyebrowView.setVisibility(eyebrow.length() > 0 ? View.VISIBLE : View.GONE);
        }

        if (mTitleView != null) {
            String title = video.getTitleFull();
            if (TextUtils.isEmpty(title)) {
                title = video.getTitle();
            }
            mTitleView.setText(title != null ? title : "");
        }
    }

    /**
     * Parses SmartTube's quality label (e.g., "4K/24/VP9", "1080/60/AVC", "720/30/VP9 HDR")
     * into ONE quiet top-right technical badge ("1080p · 60fps" or "4K HDR · 60fps").
     */
    private void parseQualityString(@Nullable String raw) {
        if (TextUtils.isEmpty(raw)) {
            if (mQualityPill != null) mQualityPill.setVisibility(View.GONE);
            return;
        }
        mLastParsedQuality = raw;
        String clean = raw.trim();
        String[] parts = clean.split("/");

        String res = parts.length > 0 ? parts[0].trim() : "";
        String fps = parts.length > 1 ? parts[1].trim() : "";
        String codec = parts.length > 2 ? parts[2].trim().split("\\s+")[0] : "";

        if (res.matches("\\d+")) {
            res = res + "p";
        }
        String hdr = clean.toUpperCase(Locale.US).contains("HDR") ? " HDR" : "";
        String fpsDigits = fps.replaceAll("[^0-9]", "");
        boolean devMode = getContext() != null && GeneralData.instance(getContext()).isDeveloperModeEnabled();

        if (mQualityPill != null) {
            StringBuilder qText = new StringBuilder();
            if (!TextUtils.isEmpty(res)) {
                qText.append(res).append(hdr);
            }
            if (!TextUtils.isEmpty(fpsDigits)) {
                if (qText.length() > 0) qText.append(" · ");
                qText.append(fpsDigits).append("fps");
            }
            if (devMode && !TextUtils.isEmpty(codec)) {
                if (qText.length() > 0) qText.append(" · ");
                qText.append(codec.toUpperCase(Locale.US));
            }
            if (qText.length() > 0) {
                mQualityPill.setText(qText.toString());
                mQualityPill.setVisibility(View.VISIBLE);
            } else {
                mQualityPill.setVisibility(View.GONE);
            }
        }
    }

    // -------------------------------------------------------------------------
    // PILL BUTTON ACTIONS
    // -------------------------------------------------------------------------

    private void dismissHudForSheet() {
        PlaybackPresenter presenter = PlaybackPresenter.instance(getContext());
        PlaybackView player = presenter != null ? presenter.getPlayer() : null;
        if (player != null) {
            player.showOverlay(false);
        }
    }

    private void resetUiTimeout() {
        PlaybackPresenter presenter = PlaybackPresenter.instance(getContext());
        if (presenter != null) {
            presenter.onKeyDown(-1);
        }
    }

    private void seekByOffset(int direction) {
        Context ctx = getContext();
        if (ctx == null) return;
        PlaybackPresenter presenter = PlaybackPresenter.instance(ctx);
        PlaybackView player = presenter != null ? presenter.getPlayer() : null;
        if (player == null) return;

        long delta = PlayerData.instance(ctx).getSeekIncrementMs();
        if (delta <= 0) delta = 10_000L;
        long cur = player.getPositionMs();
        long dur = player.getDurationMs();
        long target = cur + (direction * delta);
        if (target < 0) target = 0;
        if (dur > 0 && target > dur) target = dur;
        player.setPositionMs(target);
    }

    private void togglePlayPause() {
        PlaybackPresenter presenter = PlaybackPresenter.instance(getContext());
        PlaybackView player = presenter != null ? presenter.getPlayer() : null;
        if (presenter == null || player == null) return;

        if (player.getPlayWhenReady()) {
            presenter.onPauseClicked();
            mLastPlayingState = false;
        } else {
            presenter.onPlayClicked();
            mLastPlayingState = true;
        }
        if (mIconPlayPause != null) {
            mIconPlayPause.setImageResource(mLastPlayingState ? R.drawable.ic_cinema_pause : R.drawable.ic_cinema_play);
            applyPillVisualState(mBtnPlayPause, mIconPlayPause, mLabelPlayPause,
                    mBtnPlayPause != null && mBtnPlayPause.hasFocus(), false);
        }
        if (mLabelPlayPause != null) {
            mLabelPlayPause.setText(mLastPlayingState ? "Pause" : "Play");
        }
    }

    private void openUpNext() {
        PlaybackPresenter presenter = PlaybackPresenter.instance(getContext());
        PlaybackView player = presenter != null ? presenter.getPlayer() : null;
        if (player != null) {
            player.showSuggestions(true);
        }
    }

    private void openCaptions() {
        PlaybackPresenter presenter = PlaybackPresenter.instance(getContext());
        PlaybackView player = presenter != null ? presenter.getPlayer() : null;
        if (presenter == null || player == null) return;
        dismissHudForSheet();
        presenter.onButtonLongClicked(R.id.lb_control_closed_captioning,
                player.getButtonState(R.id.lb_control_closed_captioning));
    }

    private void openQualityDialog() {
        PlaybackPresenter presenter = PlaybackPresenter.instance(getContext());
        if (presenter != null) {
            dismissHudForSheet();
            presenter.onButtonClicked(R.id.lb_control_high_quality, PlayerUI.BUTTON_OFF);
        }
    }

    private void openMoreMenu() {
        Context ctx = getContext();
        PlaybackPresenter presenter = PlaybackPresenter.instance(ctx);
        PlaybackView player = presenter != null ? presenter.getPlayer() : null;
        if (ctx == null || presenter == null || player == null) return;

        dismissHudForSheet();

        boolean devMode = GeneralData.instance(ctx).isDeveloperModeEnabled();
        AppDialogPresenter dialog = AppDialogPresenter.instance(ctx);
        List<OptionItem> items = new ArrayList<>();

        items.add(UiOptionItem.from("Playback Speed", opt -> {
            if (devMode) {
                presenter.onButtonClicked(R.id.action_video_speed, player.getButtonState(R.id.action_video_speed));
            } else {
                openSimpleSpeedMenu(ctx, player);
            }
        }));
        items.add(UiOptionItem.from("Like Video", opt -> {
            dialog.closeDialog();
            presenter.onButtonClicked(R.id.action_thumbs_up, player.getButtonState(R.id.action_thumbs_up));
        }));
        items.add(UiOptionItem.from("Dislike Video", opt -> {
            dialog.closeDialog();
            presenter.onButtonClicked(R.id.action_thumbs_down, player.getButtonState(R.id.action_thumbs_down));
        }));
        items.add(UiOptionItem.from("Open Channel", opt -> {
            dialog.closeDialog();
            presenter.onButtonClicked(R.id.action_channel, PlayerUI.BUTTON_OFF);
        }));
        items.add(UiOptionItem.from("Subscribe / Unsubscribe", opt -> {
            dialog.closeDialog();
            presenter.onButtonClicked(R.id.action_subscribe, player.getButtonState(R.id.action_subscribe));
        }));
        items.add(UiOptionItem.from("Add to Playlist", opt -> {
            presenter.onButtonClicked(R.id.action_playlist_add, PlayerUI.BUTTON_OFF);
        }));
        items.add(UiOptionItem.from("Video Info & Comments", opt -> {
            presenter.onButtonClicked(R.id.action_info, PlayerUI.BUTTON_OFF);
        }));

        if (devMode) {
            items.add(UiOptionItem.from("Playback Mode / Repeat", opt -> {
                presenter.onButtonLongClicked(R.id.action_repeat, player.getButtonState(R.id.action_repeat));
            }));
            items.add(UiOptionItem.from("SponsorBlock Settings", opt -> {
                presenter.onButtonLongClicked(R.id.action_content_block, player.getButtonState(R.id.action_content_block));
            }));
            items.add(UiOptionItem.from("Picture-in-Picture", opt -> {
                dialog.closeDialog();
                presenter.onButtonClicked(R.id.action_pip, PlayerUI.BUTTON_OFF);
            }));
            items.add(UiOptionItem.from("Stats for Nerds", opt -> {
                dialog.closeDialog();
                presenter.onButtonClicked(R.id.action_video_stats, player.getButtonState(R.id.action_video_stats));
            }));
        }

        dialog.appendStringsCategory("Options", items);
        dialog.showDialog("More");
    }

    private void openSimpleSpeedMenu(@NonNull Context ctx, @NonNull PlaybackView player) {
        float[] speeds = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
        String[] labels = {"0.5×", "0.75×", "Normal (1.0×)", "1.25×", "1.5×", "2.0×"};
        float cur = player.getSpeed();
        if (cur <= 0f) cur = 1.0f;

        AppDialogPresenter dialog = AppDialogPresenter.instance(ctx);
        List<OptionItem> speedOpts = new ArrayList<>();
        for (int i = 0; i < speeds.length; i++) {
            final float targetSpeed = speeds[i];
            boolean selected = Math.abs(cur - targetSpeed) < 0.05f;
            speedOpts.add(UiOptionItem.from(labels[i], opt -> {
                player.setSpeed(targetSpeed);
                dialog.closeDialog();
            }, selected));
        }
        dialog.appendRadioCategory("Playback Speed", speedOpts);
        dialog.showDialog("Speed");
    }
}
