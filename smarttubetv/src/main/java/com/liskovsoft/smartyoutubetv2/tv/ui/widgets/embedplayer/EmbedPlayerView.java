package com.liskovsoft.smartyoutubetv2.tv.ui.widgets.embedplayer;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.video.VideoListener;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.ChatReceiver;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.SeekBarSegment;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.base.BasePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.controller.ExoPlayerController;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.other.ExoPlayerInitializer;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.renderer.CustomOverridesRenderersFactory;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.selector.RestoreTrackSelector;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.selector.StagedUpgradeTrackSelection;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;

import java.io.InputStream;
import java.util.List;

/**
 * https://chatgpt.com/c/6806b729-1ab0-8010-94f0-56f6b71cdbfb
 */
public class EmbedPlayerView extends PlayerView implements PlaybackView {
    private static final String TAG = EmbedPlayerView.class.getSimpleName();
    public static final int QUALITY_LOW = 0;
    public static final int QUALITY_NORMAL = 1;
    private SimpleExoPlayer mPlayer;
    private ExoPlayerInitializer mPlayerInitializer;
    private ExoPlayerController mExoPlayerController;
    private PlaybackPresenter mPlaybackPresenter;
    private Video mVideo;
    private boolean mIsMute;
    private final Runnable mShowView = this::showView;
    private final Runnable mStopPlayback = this::finish;
    private int mQuality;
    private float mPercentWatched;
    private boolean mUseSmoothFade;
    private long mMaxPreviewDurationMs = 5 * 60 * 1_000;
    private boolean mSkipWatchSync;
    private OnPreviewStateListener mPreviewListener;
    private boolean mFirstFrameShown;
    private VideoListener mPreviewVideoListener;
    private Player.EventListener mPreviewPlayerEventListener;

    public interface OnPreviewStateListener {
        void onFirstFrameRendered();
        void onPreviewError();
        void onPreviewStopped();
    }

    public void setPreviewStateListener(@Nullable OnPreviewStateListener listener) {
        mPreviewListener = listener;
    }

    public void setSmoothFade(boolean smoothFade) {
        mUseSmoothFade = smoothFade;
    }

    public void setMaxMuteDurationMs(long durationMs) {
        mMaxPreviewDurationMs = durationMs;
    }

    public void setSkipWatchSync(boolean skip) {
        mSkipWatchSync = skip;
    }

    public EmbedPlayerView(Context context) {
        super(context);
        hideView();
    }

    public EmbedPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        hideView();
    }

    public EmbedPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        hideView();
    }

    private void hideView() {
        animate().cancel();
        setAlpha(0f);
        mFirstFrameShown = false;
    }

    private void showView() {
        if (mUseSmoothFade) {
            animate().cancel();
            animate().alpha(1f).setDuration(420).start();
        } else {
            setAlpha(1f);
        }
    }

    @Override
    public void updateSuggestions(VideoGroup group) {

    }

    @Override
    public void removeSuggestions(VideoGroup group) {

    }

    @Override
    public int getSuggestionsIndex(VideoGroup group) {
        return 0;
    }

    @Override
    public VideoGroup getSuggestionsByIndex(int index) {
        return null;
    }

    @Override
    public void focusSuggestedItem(int index) {

    }

    @Override
    public void focusSuggestedItem(Video video) {

    }

    @Override
    public void resetSuggestedPosition() {

    }

    @Override
    public boolean isSuggestionsEmpty() {
        return false;
    }

    @Override
    public void clearSuggestions() {

    }

    @Override
    public void showOverlay(boolean show) {

    }

    @Override
    public boolean isOverlayShown() {
        return false;
    }

    @Override
    public void showSuggestions(boolean show) {

    }

    @Override
    public boolean isSuggestionsShown() {
        return false;
    }

    @Override
    public void showControls(boolean show) {

    }

    @Override
    public boolean isControlsShown() {
        return false;
    }

    @Override
    public int getButtonState(int buttonId) {
        return -1;
    }

    @Override
    public void setButtonState(int buttonId, int buttonState) {

    }

    @Override
    public void setChannelIcon(String iconUrl) {

    }

    @Override
    public void setSeekPreviewTitle(String title) {

    }

    @Override
    public void setNextTitle(Video nextVideo) {

    }

    @Override
    public void showDebugInfo(boolean show) {

    }

    @Override
    public void showSubtitles(boolean show) {

    }

    @Override
    public void loadStoryboard() {

    }

    @Override
    public void setTitle(String title) {

    }

    @Override
    public void showProgressBar(boolean show) {
        
    }

    @Override
    public void setSeekBarSegments(List<SeekBarSegment> segments) {

    }

    @Override
    public void updateEndingTime() {

    }

    @Override
    public void setChatReceiver(ChatReceiver chatReceiver) {

    }

    @Override
    public void setVideo(Video item) {
        mVideo = item;

        if (mExoPlayerController != null) {
            mExoPlayerController.setVideo(mVideo);
        }
    }

    @Override
    public Video getVideo() {
        return mVideo;
    }

    @Override
    public void finish() {
        destroyPlayerObjects();
    }

    @Override
    public void finishReally() {
        finish();
    }

    @Override
    public void showBackground(String url) {

    }

    @Override
    public void showBackgroundColor(int colorResId) {

    }

    @Override
    public void resetPlayerState() {

    }

    @Override
    public boolean isEmbed() {
        return true;
    }

    @Override
    public void openSabr(MediaItemFormatInfo formatInfo) {
        mExoPlayerController.openSabr(formatInfo);
    }

    @Override
    public void openDash(MediaItemFormatInfo formatInfo) {
        mExoPlayerController.openDash(formatInfo);
    }

    @Override
    public void openDash(InputStream dashManifest) {
        mExoPlayerController.openDash(dashManifest);
    }

    @Override
    public void openDashUrl(String dashManifestUrl) {
        mExoPlayerController.openDashUrl(dashManifestUrl);
    }

    @Override
    public void openHlsUrl(String hlsPlaylistUrl) {
        mExoPlayerController.openHlsUrl(hlsPlaylistUrl);
    }

    @Override
    public void openUrlList(List<String> urlList) {
        mExoPlayerController.openUrlList(urlList);
    }

    @Override
    public void openMerged(MediaItemFormatInfo formatInfo, String hlsPlaylistUrl) {
        mExoPlayerController.openMerged(formatInfo, hlsPlaylistUrl);
    }

    @Override
    public void openMerged(InputStream dashManifest, String hlsPlaylistUrl) {
        mExoPlayerController.openMerged(dashManifest, hlsPlaylistUrl);
    }

    @Override
    public long getPositionMs() {
        return mExoPlayerController.getPositionMs();
    }

    @Override
    public void setPositionMs(long positionMs) {
        mExoPlayerController.setPositionMs(positionMs);
    }

    @Override
    public long getDurationMs() {
        return mExoPlayerController.getDurationMs();
    }

    @Override
    public void setPlayWhenReady(boolean play) {
        mExoPlayerController.setPlayWhenReady(play);
    }

    @Override
    public boolean getPlayWhenReady() {
        return mExoPlayerController.getPlayWhenReady();
    }

    @Override
    public boolean isPlaying() {
        return mExoPlayerController.isPlaying();
    }

    @Override
    public boolean isLoading() {
        return mExoPlayerController.isLoading();
    }

    @Override
    public List<FormatItem> getVideoFormats() {
        return mExoPlayerController.getVideoFormats();
    }

    @Override
    public List<FormatItem> getAudioFormats() {
        return mExoPlayerController.getAudioFormats();
    }

    @Override
    public List<FormatItem> getSubtitleFormats() {
        return mExoPlayerController.getSubtitleFormats();
    }

    @Override
    public void setFormat(FormatItem option) {
        // NOP. Use internal selection
    }

    @Override
    public FormatItem getVideoFormat() {
        return mExoPlayerController.getVideoFormat();
    }

    @Override
    public FormatItem getAudioFormat() {
        return mExoPlayerController.getAudioFormat();
    }

    @Override
    public FormatItem getSubtitleFormat() {
        return mExoPlayerController.getSubtitleFormat();
    }

    @Override
    public boolean isEngineInitialized() {
        return mPlayer != null;
    }

    @Override
    public void restartEngine() {

    }

    @Override
    public void reloadPlayback() {

    }

    @Override
    public void blockEngine(boolean block) {

    }

    @Override
    public boolean isEngineBlocked() {
        return false;
    }

    @Override
    public boolean isInPIPMode() {
        return false;
    }

    @Override
    public boolean containsMedia() {
        if (mExoPlayerController == null) {
            return false;
        }

        return mExoPlayerController.containsMedia();
    }

    @Override
    public void setSpeed(float speed) {
        if (mExoPlayerController != null) {
            mExoPlayerController.setSpeed(speed);
        }
    }

    @Override
    public float getSpeed() {
        if (mExoPlayerController == null) {
            return 1f;
        }

        return mExoPlayerController.getSpeed();
    }

    @Override
    public void setPitch(float pitch) {
        if (mExoPlayerController != null) {
            mExoPlayerController.setPitch(pitch);
        }
    }

    @Override
    public float getPitch() {
        if (mExoPlayerController == null) {
            return 1f;
        }

        return mExoPlayerController.getPitch();
    }

    @Override
    public void setVolume(float volume) {
        if (!mIsMute && mExoPlayerController != null) {
            mExoPlayerController.setVolume(volume);
        }
    }

    @Override
    public float getVolume() {
        if (mExoPlayerController == null) {
            return 1f;
        }

        return mExoPlayerController.getVolume();
    }

    @Override
    public void setZoomPercents(int percents) {

    }

    @Override
    public int getResizeMode() {
        return super.getResizeMode();
    }

    @Override
    public void setAspectRatio(float ratio) {

    }

    @Override
    public void setRotationAngle(int angle) {

    }

    @Override
    public void setVideoFlipEnabled(boolean enabled) {

    }

    @Override
    public void setVideoGravity(int gravity) {
        
    }

    @Override
    public Throwable getUnthrownError() {
        return mExoPlayerController.getUnthrownError();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            finish();
        } finally {
            super.finalize();
        }
    }

    public void setQuality(int quality) {
        mQuality = quality;
    }

    public void openVideo(String videoId) {
        openVideo(Video.from(videoId));
    }

    public void openVideo(@Nullable Video video) {
        if (video == null) {
            return;
        }

        if (mPlaybackPresenter == null) {
            mPlaybackPresenter = getPlaybackPresenter();
        }

        // Fullscreen playback is running. Skipping
        // Fix Android TV 12 playing on Home (top view check)
        PlaybackView view = mPlaybackPresenter.getView();
        if (view == null || view instanceof EmbedPlayerView || (!mPlaybackPresenter.isEngineInitialized() && getViewManager().getTopView() != PlaybackView.class)) {
            if (!isEngineInitialized()) {
                initPlayer();
                // Queue target video BEFORE createPlayerObjects()->onEngineInitialized() so
                // VideoLoaderController loads the preview stream ONCE instead of double-loading!
                mPlaybackPresenter.onNewVideo(video);
                createPlayerObjects();
            } else {
                initPlayer();
                createPlayerObjects();
                mPlaybackPresenter.onNewVideo(video);
            }
            mPercentWatched = video.percentWatched;
        }
    }

    private void initPlayer() {
        if (isEngineInitialized()) {
            mPlaybackPresenter.setView(this);
            return;
        }

        mPlayerInitializer = new ExoPlayerInitializer(getContext());
        mPlaybackPresenter.setView(this);
        mExoPlayerController = new ExoPlayerController(getContext(), mPlaybackPresenter);
        mExoPlayerController.setOnVideoLoaded(this::onVideoLoaded);
        mPlaybackPresenter.onViewInitialized(); // init all controllers

        setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM); // Fix unfilled borders
    }

    private void createPlayerObjects() {
        if (isEngineInitialized()) {
            setPlayer(mPlayer);
            return;
        }

        // Staged two-tier track selection: starts Hero preview rapidly on 720p (same codec as user's
        // saved 1080p/4K target), caps preview ahead-buffer to 5.5s, and seamlessly upgrades to full
        // quality on the VERY NEXT buffer chunk upon handoff with ZERO buffer flush or spinner!
        StagedUpgradeTrackSelection.Factory stagedFactory = new StagedUpgradeTrackSelection.Factory();
        DefaultTrackSelector trackSelector = new RestoreTrackSelector(stagedFactory);
        mExoPlayerController.setTrackSelector(trackSelector);
        mExoPlayerController.setStagedTrackFactory(stagedFactory);
        mExoPlayerController.setStagedFastStartEnabled(mQuality != QUALITY_LOW);

        mPlayerInitializer.setPreviewBufferCapMs(5_500L);
        DefaultRenderersFactory renderersFactory = new CustomOverridesRenderersFactory(getContext());
        mPlayer = mPlayerInitializer.createPlayer(getContext(), renderersFactory, trackSelector);
        mPlayer.setPlayWhenReady(true);
        //mPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);

        mPreviewVideoListener = new VideoListener() {
            @Override
            public void onRenderedFirstFrame() {
                if (!mFirstFrameShown) {
                    mFirstFrameShown = true;
                    Utils.removeCallbacks(mShowView);
                    showView();
                    if (mPreviewListener != null) {
                        mPreviewListener.onFirstFrameRendered();
                    }
                }
            }
        };
        mPlayer.addVideoListener(mPreviewVideoListener);

        mPreviewPlayerEventListener = new Player.EventListener() {
            @Override
            public void onPlayerError(ExoPlaybackException error) {
                hideView();
                if (mPreviewListener != null) {
                    mPreviewListener.onPreviewError();
                }
            }
        };
        mPlayer.addListener(mPreviewPlayerEventListener);

        mExoPlayerController.setPlayer(mPlayer);
        if (mQuality == QUALITY_LOW) {
            mExoPlayerController.selectFormatTransient(FormatItem.VIDEO_SUB_SD_AVC_30);
        }
        // Don't use subs during preview! Not efficient. High cpu load. Cause input lags.
        mExoPlayerController.selectFormatTransient(FormatItem.SUBTITLE_NONE);
        if (mIsMute) {
            mExoPlayerController.setVolume(0);
        }

        setPlayer(mPlayer);

        mPlaybackPresenter.onEngineInitialized(); // start playback
    }

    public boolean canHandoff(@Nullable Video video) {
        return video != null
                && mVideo != null
                && video.videoId != null
                && video.videoId.equals(mVideo.videoId)
                && mPlayer != null
                && mExoPlayerController != null
                && mPlayerInitializer != null
                && mExoPlayerController.containsMedia();
    }

    /**
     * Detaches the live ExoPlayer without stopping or releasing it, placing it in
     * {@link PreviewPlayerHandoff} so PlaybackFragment resumes immediately at the
     * exact frame and smoothly upgrades to full quality on the next DASH segment.
     */
    public boolean detachForHandoff() {
        if (!isEngineInitialized() || mVideo == null || mVideo.videoId == null
                || mExoPlayerController == null || mPlayerInitializer == null
                || !mExoPlayerController.containsMedia()) {
            return false;
        }
        Utils.removeCallbacks(mShowView);
        Utils.removeCallbacks(mStopPlayback);

        if (mPreviewVideoListener != null) {
            mPlayer.removeVideoListener(mPreviewVideoListener);
            mPreviewVideoListener = null;
        }
        if (mPreviewPlayerEventListener != null) {
            mPlayer.removeListener(mPreviewPlayerEventListener);
            mPreviewPlayerEventListener = null;
        }

        mExoPlayerController.setOnVideoLoaded(null);
        mExoPlayerController.setVolume(1f);
        // Freeze the media clock for the ~100ms Activity launch while keeping decoded buffers hot
        // so audio never races ahead of video while PlaybackFragment's SurfaceView attaches!
        mPlayer.setPlayWhenReady(false);
        setPlayer(null); // detaches preview TextureView without stopping/flushing ExoPlayer

        android.util.Log.d("CinemaHandoff", "Detaching live preview for handoff: videoId=" + mVideo.videoId
                + " state=" + mPlayer.getPlaybackState()
                + " posMs=" + mPlayer.getCurrentPosition()
                + " bufMs=" + mPlayer.getBufferedPosition());

        PreviewPlayerHandoff.store(new PreviewPlayerHandoff.Session(
                mVideo.videoId,
                mVideo,
                mPlayer,
                mExoPlayerController,
                mPlayerInitializer
        ));

        // Transfer ownership so destroyPlayerObjects() / finish() becomes a safe no-op
        mPlayer = null;
        mExoPlayerController = null;
        mPlayerInitializer = null;
        hideView();
        return true;
    }

    private void destroyPlayerObjects() {
        if (isEngineInitialized()) {
            Utils.removeCallbacks(mShowView);
            Utils.removeCallbacks(mStopPlayback);
            // Don't replace main player!
            if (mPlaybackPresenter.getView() == null || mPlaybackPresenter.getView() == this) {
                mPlaybackPresenter.onEngineReleased();
            }
            mPlayerInitializer.release();
            mExoPlayerController.setOnVideoLoaded(null);
            // Fix access calls when player isn't initialized
            mExoPlayerController.release();
            mPlayer = null;
            setPlayer(null);
            hideView();
            syncPositionIfNeeded();
            if (mPreviewListener != null) {
                mPreviewListener.onPreviewStopped();
            }
        }
    }

    private void syncPositionIfNeeded() {
        if (!mSkipWatchSync && !mIsMute && isPositionChanged()) {
            BasePresenter<?> presenter = getViewManager().getTopPresenter();
            if (presenter != null) {
                presenter.syncItem(mVideo);
            }
        }
    }

    private boolean isPositionChanged() {
        return mVideo != null && Math.abs(mPercentWatched - mVideo.percentWatched) > 20;
    }

    private void onVideoLoaded() {
        // Fallback if onRenderedFirstFrame does not fire promptly on some devices
        if (!mFirstFrameShown) {
            Utils.postDelayed(mShowView, mUseSmoothFade ? 650 : 1_000);
        }
        if ((mIsMute || mSkipWatchSync) && mMaxPreviewDurationMs > 0) {
            Utils.postDelayed(mStopPlayback, mMaxPreviewDurationMs);
        }
    }

    public void setMute(boolean mute) {
        mIsMute = mute;

        if (mExoPlayerController != null) {
            mExoPlayerController.setVolume(mute ? 0 : 1f);
        }
    }

    private ViewManager getViewManager() {
        return ViewManager.instance(getContext());
    }

    private PlaybackPresenter getPlaybackPresenter() {
        return PlaybackPresenter.instance(getContext());
    }
}
