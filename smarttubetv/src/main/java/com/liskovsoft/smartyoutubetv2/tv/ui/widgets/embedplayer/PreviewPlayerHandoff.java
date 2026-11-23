package com.liskovsoft.smartyoutubetv2.tv.ui.widgets.embedplayer;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.controller.ExoPlayerController;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.other.ExoPlayerInitializer;

/**
 * Transfers a live, already-playing SimpleExoPlayer + ExoPlayerController seamlessly
 * from the Browse Hero Preview directly into fullscreen PlaybackFragment with zero
 * network re-fetch, zero black-screen re-buffer, and seamless next-segment ABR quality upgrade.
 */
public final class PreviewPlayerHandoff {
    private static final long MAX_HANDOFF_AGE_MS = 10_000L;

    public static final class Session {
        public final String videoId;
        public final Video video;
        public final SimpleExoPlayer player;
        public final ExoPlayerController controller;
        public final ExoPlayerInitializer initializer;
        public final long createdAtMs;

        public Session(@NonNull String videoId,
                       @Nullable Video video,
                       @NonNull SimpleExoPlayer player,
                       @NonNull ExoPlayerController controller,
                       @NonNull ExoPlayerInitializer initializer) {
            this.videoId = videoId;
            this.video = video;
            this.player = player;
            this.controller = controller;
            this.initializer = initializer;
            this.createdAtMs = System.currentTimeMillis();
        }

        void releaseSafely() {
            try {
                if (initializer != null) {
                    initializer.release();
                }
            } catch (Throwable ignored) {}
            try {
                if (controller != null) {
                    controller.setOnVideoLoaded(null);
                    controller.release();
                } else if (player != null) {
                    player.stop(true);
                    player.clearVideoSurface();
                    player.release();
                }
            } catch (Throwable ignored) {}
        }
    }

    private static Session sPending;

    private PreviewPlayerHandoff() {}

    public static synchronized void store(@NonNull Session session) {
        if (sPending != null && sPending != session) {
            sPending.releaseSafely();
        }
        sPending = session;
    }

    @Nullable
    public static synchronized Session consume(@Nullable String videoId) {
        Session session = sPending;
        sPending = null;
        if (session == null) {
            return null;
        }
        boolean expired = (System.currentTimeMillis() - session.createdAtMs) > MAX_HANDOFF_AGE_MS;
        boolean matched = !TextUtils.isEmpty(videoId) && videoId.equals(session.videoId);
        if (expired || !matched) {
            session.releaseSafely();
            return null;
        }
        return session;
    }

    public static synchronized void clearAndRelease() {
        if (sPending != null) {
            sPending.releaseSafely();
            sPending = null;
        }
    }
}
