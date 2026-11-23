package com.liskovsoft.smartyoutubetv2.tv.ui.cinema;

import android.graphics.Outline;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Horizontal pill strip of subscribed channels for SmartTube's Channels (TYPE_MULTI_GRID) section.
 */
public class CinemaChannelStripAdapter extends RecyclerView.Adapter<CinemaChannelStripAdapter.ChannelHolder> {

    public interface Listener {
        void onChannelFocused(@NonNull Video channel, int position);
        void onChannelClicked(@NonNull Video channel);
        void onChannelLongClicked(@NonNull Video channel);
        void onChannelScrollEnd(@NonNull Video lastChannel);
    }

    private final List<Video> mChannels = new ArrayList<>();
    private final Listener mListener;
    private int mLastScrollEndSize = -1;

    public CinemaChannelStripAdapter(@NonNull Listener listener) {
        mListener = listener;
    }

    public void setChannels(@Nullable List<Video> channels) {
        mChannels.clear();
        mLastScrollEndSize = -1;
        if (channels != null) {
            for (Video v : channels) {
                if (v != null) {
                    mChannels.add(v);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void appendChannels(@Nullable List<Video> channels) {
        if (channels == null || channels.isEmpty()) {
            return;
        }
        int start = mChannels.size();
        int added = 0;
        for (Video v : channels) {
            if (v != null) {
                mChannels.add(v);
                added++;
            }
        }
        if (added > 0) {
            notifyItemRangeInserted(start, added);
        }
    }

    public void clear() {
        mChannels.clear();
        mLastScrollEndSize = -1;
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return mChannels.isEmpty();
    }

    @NonNull
    @Override
    public ChannelHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cinema_channel_pill, parent, false);
        return new ChannelHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChannelHolder holder, int position) {
        holder.bind(mChannels.get(position), position);
    }

    @Override
    public int getItemCount() {
        return mChannels.size();
    }

    final class ChannelHolder extends RecyclerView.ViewHolder {
        private final ImageView avatarView;
        private final View newDotView;
        private final TextView nameView;
        private Video mBoundChannel;
        private int mBoundPosition = -1;

        ChannelHolder(@NonNull View itemView) {
            super(itemView);
            avatarView = itemView.findViewById(R.id.cinema_channel_avatar);
            newDotView = itemView.findViewById(R.id.cinema_channel_new_dot);
            nameView = itemView.findViewById(R.id.cinema_channel_name);

            avatarView.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });
            avatarView.setClipToOutline(true);
        }

        void bind(@NonNull Video channel, int position) {
            mBoundChannel = channel;
            mBoundPosition = position;
            String label = !TextUtils.isEmpty(channel.getTitle()) ? channel.getTitle() : channel.getAuthor();
            nameView.setText(label != null ? label : "");
            newDotView.setVisibility(channel.hasNewContent ? View.VISIBLE : View.GONE);

            if (!TextUtils.isEmpty(channel.cardImageUrl)) {
                Glide.with(itemView.getContext().getApplicationContext())
                        .load(channel.cardImageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .centerCrop()
                        .into(avatarView);
            } else {
                avatarView.setImageDrawable(null);
            }

            applyFocus(itemView.hasFocus());

            itemView.setOnFocusChangeListener((v, hasFocus) -> {
                applyFocus(hasFocus);
                if (hasFocus) {
                    int pos = getAdapterPosition();
                    Video focused = (pos != RecyclerView.NO_POSITION && pos < mChannels.size())
                            ? mChannels.get(pos) : mBoundChannel;
                    int resolvedPos = (pos != RecyclerView.NO_POSITION) ? pos : mBoundPosition;
                    if (focused != null) {
                        mListener.onChannelFocused(focused, resolvedPos);
                        if (resolvedPos >= mChannels.size() - 4 && !mChannels.isEmpty()
                                && mChannels.size() != mLastScrollEndSize) {
                            mLastScrollEndSize = mChannels.size();
                            mListener.onChannelScrollEnd(mChannels.get(mChannels.size() - 1));
                        }
                    }
                }
            });

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                Video clicked = (pos != RecyclerView.NO_POSITION && pos < mChannels.size())
                        ? mChannels.get(pos) : mBoundChannel;
                if (clicked != null) {
                    mListener.onChannelClicked(clicked);
                }
            });

            itemView.setOnLongClickListener(v -> {
                int pos = getAdapterPosition();
                Video clicked = (pos != RecyclerView.NO_POSITION && pos < mChannels.size())
                        ? mChannels.get(pos) : mBoundChannel;
                if (clicked != null) {
                    mListener.onChannelLongClicked(clicked);
                    return true;
                }
                return false;
            });
        }

        private void applyFocus(boolean focused) {
            nameView.setTextColor(ContextCompat.getColor(
                    itemView.getContext(),
                    focused ? R.color.cinema_text_dark : R.color.cinema_text_primary
            ));
            itemView.animate().cancel();
            itemView.animate()
                    .scaleX(focused ? 1.05f : 1.0f)
                    .scaleY(focused ? 1.05f : 1.0f)
                    .setDuration(focused ? 190L : 150L)
                    .setInterpolator(androidx.core.view.animation.PathInterpolatorCompat.create(0.22f, 1.0f, 0.36f, 1.0f))
                    .start();
        }
    }
}
