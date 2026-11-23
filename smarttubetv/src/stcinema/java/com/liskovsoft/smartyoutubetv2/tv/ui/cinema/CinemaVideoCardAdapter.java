package com.liskovsoft.smartyoutubetv2.tv.ui.cinema;

import android.graphics.Outline;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.animation.PathInterpolatorCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 60fps Apple tvOS Cinema video card adapter (supports 16:9 rails/grids and 9:14 portrait Shorts).
 */
public class CinemaVideoCardAdapter extends RecyclerView.Adapter<CinemaVideoCardAdapter.VideoHolder> {
    private static final int SCROLL_END_THRESHOLD = 4;
    private static final float FOCUS_SCALE = 1.06f;
    private static final Interpolator FOCUS_IN_INTERP = PathInterpolatorCompat.create(0.22f, 1.0f, 0.36f, 1.0f);
    private static final Interpolator FOCUS_OUT_INTERP = PathInterpolatorCompat.create(0.25f, 0.46f, 0.45f, 0.94f);

    public interface Listener {
        void onVideoFocused(@NonNull Video video, int rowIndex, int itemIndex);
        void onVideoClicked(@NonNull Video video);
        void onVideoLongClicked(@NonNull Video video);
        void onScrollEnd(@NonNull Video lastVideo);
    }

    private final List<Video> mVideos = new ArrayList<>();
    private final Listener mListener;
    private final int mRowIndex;
    private final boolean mIsGridMode;
    private boolean mIsShortsMode = false;
    private int mLastScrollEndSize = -1;

    public CinemaVideoCardAdapter(@NonNull Listener listener, int rowIndex, boolean isGridMode) {
        mListener = listener;
        mRowIndex = rowIndex;
        mIsGridMode = isGridMode;
    }

    public void setShortsMode(boolean isShortsMode) {
        if (mIsShortsMode != isShortsMode) {
            mIsShortsMode = isShortsMode;
            notifyDataSetChanged();
        }
    }

    public void setVideos(@Nullable List<Video> videos) {
        mVideos.clear();
        mLastScrollEndSize = -1;
        if (videos != null) {
            for (Video v : videos) {
                if (v != null) {
                    mVideos.add(v);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void appendVideos(@Nullable List<Video> videos) {
        if (videos == null || videos.isEmpty()) {
            return;
        }
        int start = mVideos.size();
        int added = 0;
        for (Video v : videos) {
            if (v != null && !containsVideo(v)) {
                mVideos.add(v);
                added++;
            }
        }
        if (added > 0) {
            notifyItemRangeInserted(start, added);
        }
    }

    public void prependVideos(@Nullable List<Video> videos) {
        if (videos == null || videos.isEmpty()) {
            return;
        }
        int added = 0;
        for (int i = videos.size() - 1; i >= 0; i--) {
            Video v = videos.get(i);
            if (v != null && !containsVideo(v)) {
                mVideos.add(0, v);
                added++;
            }
        }
        if (added > 0) {
            notifyItemRangeInserted(0, added);
        }
    }

    public void removeVideos(@Nullable List<Video> videos) {
        if (videos == null || videos.isEmpty()) {
            return;
        }
        for (Video target : videos) {
            int idx = indexOf(target);
            if (idx >= 0) {
                mVideos.remove(idx);
                notifyItemRemoved(idx);
            }
        }
    }

    public void removeByAuthor(@Nullable List<Video> videos) {
        if (videos == null || videos.isEmpty()) {
            return;
        }
        String author = null;
        for (Video v : videos) {
            if (v != null && !TextUtils.isEmpty(v.getAuthor())) {
                author = v.getAuthor();
                break;
            }
        }
        if (author == null) {
            return;
        }
        for (int i = mVideos.size() - 1; i >= 0; i--) {
            if (author.equals(mVideos.get(i).getAuthor())) {
                mVideos.remove(i);
                notifyItemRemoved(i);
            }
        }
    }

    public void syncVideos(@Nullable List<Video> videos) {
        if (videos == null || videos.isEmpty()) {
            return;
        }
        for (Video updated : videos) {
            int idx = indexOf(updated);
            if (idx >= 0) {
                mVideos.set(idx, updated);
                notifyItemChanged(idx);
            }
        }
    }

    public void clear() {
        int size = mVideos.size();
        mVideos.clear();
        mLastScrollEndSize = -1;
        if (size > 0) {
            notifyItemRangeRemoved(0, size);
        }
    }

    public int indexOf(@Nullable Video target) {
        if (target == null) {
            return -1;
        }
        for (int i = 0; i < mVideos.size(); i++) {
            Video cur = mVideos.get(i);
            if (cur == target || (cur.videoId != null && cur.videoId.equals(target.videoId))) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    public Video getItem(int position) {
        if (position < 0 || position >= mVideos.size()) {
            return null;
        }
        return mVideos.get(position);
    }

    private boolean containsVideo(@NonNull Video candidate) {
        for (Video existing : mVideos) {
            if (existing == candidate) {
                return true;
            }
            if (candidate.videoId != null && candidate.videoId.equals(existing.videoId)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    @Override
    public VideoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cinema_video_card, parent, false);

        if (mIsGridMode) {
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            view.setLayoutParams(lp);
        }

        return new VideoHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoHolder holder, int position) {
        Video video = mVideos.get(position);
        holder.bind(video, position);
    }

    @Override
    public int getItemCount() {
        return mVideos.size();
    }

    final class VideoHolder extends RecyclerView.ViewHolder {
        private final FrameLayout thumbFrame;
        private final ImageView thumbView;
        private final TextView badgeView;
        private final ProgressBar progressView;
        private final View focusRing;
        private final TextView titleView;
        private final TextView subView;
        private Video mBoundVideo;
        private int mBoundPosition = -1;
        private boolean mAppliedShorts = false;

        VideoHolder(@NonNull View itemView) {
            super(itemView);
            thumbFrame = itemView.findViewById(R.id.cinema_thumb_frame);
            thumbView = itemView.findViewById(R.id.cinema_card_thumb);
            badgeView = itemView.findViewById(R.id.cinema_card_badge);
            progressView = itemView.findViewById(R.id.cinema_card_progress);
            focusRing = itemView.findViewById(R.id.cinema_card_focus_ring);
            titleView = itemView.findViewById(R.id.cinema_card_title);
            subView = itemView.findViewById(R.id.cinema_card_sub);

            applyThumbGeometry(false);
        }

        private void applyThumbGeometry(boolean shorts) {
            mAppliedShorts = shorts;
            float density = itemView.getResources().getDisplayMetrics().density;
            int targetHeightPx = Math.round((shorts ? 226f : 118f) * density);
            ViewGroup.LayoutParams lp = thumbFrame.getLayoutParams();
            if (lp != null && lp.height != targetHeightPx) {
                lp.height = targetHeightPx;
                thumbFrame.setLayoutParams(lp);
            }
            final float radiusPx = density * (shorts ? 14f : 12f);
            thumbFrame.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radiusPx);
                }
            });
            thumbFrame.setClipToOutline(true);
        }

        void bind(@NonNull Video video, int position) {
            mBoundVideo = video;
            mBoundPosition = position;

            if (mAppliedShorts != mIsShortsMode) {
                applyThumbGeometry(mIsShortsMode);
            }

            String title = video.getTitle();
            titleView.setText(title != null ? title : "");

            CharSequence sub = video.getSecondTitle();
            if (TextUtils.isEmpty(sub)) {
                sub = video.getAuthor();
            }
            subView.setText(sub != null ? sub : "");

            // Badge (LIVE / Upcoming / Duration)
            if (video.isLive) {
                badgeView.setText(R.string.cinema_live);
                badgeView.setBackgroundResource(R.drawable.bg_cinema_pill_live);
                badgeView.setVisibility(View.VISIBLE);
            } else if (video.isUpcoming) {
                badgeView.setText(R.string.cinema_upcoming);
                badgeView.setBackgroundResource(R.drawable.bg_cinema_duration_pill);
                badgeView.setVisibility(View.VISIBLE);
            } else if (!TextUtils.isEmpty(video.badge)) {
                badgeView.setText(video.badge);
                badgeView.setBackgroundResource(R.drawable.bg_cinema_duration_pill);
                badgeView.setVisibility(View.VISIBLE);
            } else {
                badgeView.setVisibility(View.GONE);
            }

            // Watched progress bar
            if (video.percentWatched > 0 && !video.isLive) {
                progressView.setProgress(Math.min(100, Math.max(2, Math.round(video.percentWatched))));
                progressView.setVisibility(View.VISIBLE);
            } else {
                progressView.setVisibility(View.GONE);
            }

            // High-speed Glide thumbnail load
            String imgUrl = !TextUtils.isEmpty(video.cardImageUrl) ? video.cardImageUrl : video.bgImageUrl;
            if (!TextUtils.isEmpty(imgUrl)) {
                Glide.with(itemView.getContext().getApplicationContext())
                        .load(imgUrl)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .centerCrop()
                        .into(thumbView);
            } else {
                thumbView.setImageDrawable(null);
            }

            boolean currentlyFocused = itemView.hasFocus();
            applyFocusVisuals(currentlyFocused, false);
            if (currentlyFocused && mBoundVideo != null) {
                mListener.onVideoFocused(mBoundVideo, mRowIndex, mBoundPosition);
            }

            itemView.setOnFocusChangeListener((v, hasFocus) -> {
                applyFocusVisuals(hasFocus, true);
                if (hasFocus) {
                    int adapterPos = getAdapterPosition();
                    Video focused = (adapterPos != RecyclerView.NO_POSITION && adapterPos < mVideos.size())
                            ? mVideos.get(adapterPos)
                            : mBoundVideo;
                    int resolvedPos = (adapterPos != RecyclerView.NO_POSITION) ? adapterPos : mBoundPosition;

                    if (focused != null) {
                        mListener.onVideoFocused(focused, mRowIndex, resolvedPos);

                        if (resolvedPos >= mVideos.size() - SCROLL_END_THRESHOLD
                                && !mVideos.isEmpty()
                                && mVideos.size() != mLastScrollEndSize) {
                            mLastScrollEndSize = mVideos.size();
                            mListener.onScrollEnd(mVideos.get(mVideos.size() - 1));
                        }
                    }
                }
            });

            itemView.setOnClickListener(v -> {
                int adapterPos = getAdapterPosition();
                Video clicked = (adapterPos != RecyclerView.NO_POSITION && adapterPos < mVideos.size())
                        ? mVideos.get(adapterPos)
                        : mBoundVideo;
                if (clicked != null) {
                    mListener.onVideoClicked(clicked);
                }
            });

            itemView.setOnLongClickListener(v -> {
                int adapterPos = getAdapterPosition();
                Video clicked = (adapterPos != RecyclerView.NO_POSITION && adapterPos < mVideos.size())
                        ? mVideos.get(adapterPos)
                        : mBoundVideo;
                if (clicked != null) {
                    mListener.onVideoLongClicked(clicked);
                    return true;
                }
                return false;
            });
        }

        private void applyFocusVisuals(boolean focused, boolean animate) {
            float density = itemView.getResources().getDisplayMetrics().density;
            float targetScale = focused ? FOCUS_SCALE : 1.0f;
            float targetShiftY = focused ? -2f * density : 0f;
            float targetRingAlpha = focused ? 1.0f : 0.0f;
            float targetThumbAlpha = focused ? 1.0f : 0.90f;
            float targetElevation = focused ? 18f * density : 2f;

            itemView.setZ(focused ? 12f : 0f);
            thumbFrame.setElevation(targetElevation);

            titleView.setTextColor(ContextCompat.getColor(
                    itemView.getContext(),
                    focused ? R.color.cinema_text_primary : R.color.cinema_text_secondary
            ));
            subView.setTextColor(ContextCompat.getColor(
                    itemView.getContext(),
                    focused ? R.color.cinema_text_sub_focused : R.color.cinema_text_muted
            ));
            titleView.setAlpha(1.0f);
            subView.setAlpha(1.0f);

            if (animate) {
                Interpolator interp = focused ? FOCUS_IN_INTERP : FOCUS_OUT_INTERP;
                long dur = focused ? 200L : 160L;

                thumbFrame.animate().cancel();
                thumbFrame.animate()
                        .scaleX(targetScale)
                        .scaleY(targetScale)
                        .translationY(targetShiftY)
                        .setDuration(dur)
                        .setInterpolator(interp)
                        .start();

                thumbView.animate().cancel();
                thumbView.animate()
                        .alpha(targetThumbAlpha)
                        .setDuration(dur)
                        .setInterpolator(interp)
                        .start();

                focusRing.animate().cancel();
                focusRing.animate()
                        .alpha(targetRingAlpha)
                        .setDuration(focused ? 160L : 140L)
                        .setInterpolator(interp)
                        .start();
            } else {
                thumbFrame.animate().cancel();
                thumbFrame.setScaleX(targetScale);
                thumbFrame.setScaleY(targetScale);
                thumbFrame.setTranslationY(targetShiftY);
                thumbView.setAlpha(targetThumbAlpha);
                focusRing.setAlpha(targetRingAlpha);
            }
        }
    }
}
