package com.liskovsoft.smartyoutubetv2.tv.ui.cinema;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.animation.PathInterpolatorCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsItem;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Clean Apple tvOS settings tile adapter (unified monochrome iconography + inline emerald status badge).
 */
public class CinemaSettingsAdapter extends RecyclerView.Adapter<CinemaSettingsAdapter.SettingHolder> {

    private static final Interpolator EASE_OUT = PathInterpolatorCompat.create(0.22f, 1.0f, 0.36f, 1.0f);
    private static final Interpolator EASE_RETURN = PathInterpolatorCompat.create(0.25f, 0.46f, 0.45f, 0.94f);

    public interface OnSettingClickListener {
        void onSettingClicked(@NonNull SettingsItem item);
        void onSettingFocused();
    }

    private final List<SettingsItem> mItems = new ArrayList<>();
    private final OnSettingClickListener mListener;

    public CinemaSettingsAdapter(@NonNull OnSettingClickListener listener) {
        mListener = listener;
    }

    public void setItems(@Nullable List<SettingsItem> items) {
        mItems.clear();
        if (items != null) {
            mItems.addAll(items);
        }
        notifyDataSetChanged();
    }

    public void clear() {
        mItems.clear();
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return mItems.isEmpty();
    }

    @NonNull
    @Override
    public SettingHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cinema_setting_card, parent, false);
        return new SettingHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingHolder holder, int position) {
        holder.bind(mItems.get(position));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    final class SettingHolder extends RecyclerView.ViewHolder {
        private final ImageView iconView;
        private final TextView titleView;
        private final View statusPill;
        private final TextView statusText;

        SettingHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.cinema_setting_icon);
            titleView = itemView.findViewById(R.id.cinema_setting_title);
            statusPill = itemView.findViewById(R.id.cinema_setting_status_pill);
            statusText = itemView.findViewById(R.id.cinema_setting_status_text);
        }

        void bind(@NonNull SettingsItem item) {
            String rawTitle = item.title != null ? item.title : "";
            boolean isDevModeOn = rawTitle.endsWith("(ON)");
            String cleanTitle = isDevModeOn
                    ? rawTitle.substring(0, rawTitle.length() - "(ON)".length()).trim()
                    : rawTitle;
            titleView.setText(cleanTitle);

            if (statusPill != null) {
                statusPill.setVisibility(isDevModeOn ? View.VISIBLE : View.GONE);
            }

            // Use unified monochrome DeArrow glyph so all Settings tiles share one visual weight
            if (cleanTitle.toLowerCase().contains("dearrow")) {
                iconView.setImageResource(R.drawable.ic_cinema_dearrow_mono);
                iconView.setVisibility(View.VISIBLE);
            } else if (item.imageResId > 0) {
                iconView.setImageResource(item.imageResId);
                iconView.setVisibility(View.VISIBLE);
            } else {
                iconView.setVisibility(View.GONE);
            }

            applyFocus(itemView.hasFocus(), false);

            itemView.setOnFocusChangeListener((v, hasFocus) -> {
                applyFocus(hasFocus, true);
                if (hasFocus) {
                    mListener.onSettingFocused();
                }
            });

            itemView.setOnClickListener(v -> mListener.onSettingClicked(item));
        }

        private void applyFocus(boolean focused, boolean animate) {
            int tint = ContextCompat.getColor(
                    itemView.getContext(),
                    focused ? R.color.cinema_text_dark : R.color.cinema_text_primary
            );
            titleView.setTextColor(tint);
            iconView.setImageTintList(ColorStateList.valueOf(tint));

            if (statusText != null) {
                statusText.setTextColor(ContextCompat.getColor(
                        itemView.getContext(),
                        focused ? R.color.cinema_text_dark : R.color.cinema_emerald_soft
                ));
            }

            float scale = focused ? 1.045f : 1.0f;
            if (animate) {
                itemView.animate().cancel();
                itemView.animate()
                        .scaleX(scale)
                        .scaleY(scale)
                        .setDuration(focused ? 190L : 150L)
                        .setInterpolator(focused ? EASE_OUT : EASE_RETURN)
                        .start();
            } else {
                itemView.setScaleX(scale);
                itemView.setScaleY(scale);
            }
        }
    }
}
