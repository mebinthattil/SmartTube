package com.liskovsoft.smartyoutubetv2.tv.ui.cinema;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Vertical RecyclerView of horizontal Apple TV+ Cinema video rails with a shared RecycledViewPool.
 */
public class CinemaRowsAdapter extends RecyclerView.Adapter<CinemaRowsAdapter.RowHolder> {

    private static final class RowEntry {
        final int groupId;
        String title;
        final CinemaVideoCardAdapter adapter;

        RowEntry(int groupId, String title, CinemaVideoCardAdapter adapter) {
            this.groupId = groupId;
            this.title = title;
            this.adapter = adapter;
        }
    }

    private final List<RowEntry> mRows = new ArrayList<>();
    private final RecyclerView.RecycledViewPool mSharedPool = new RecyclerView.RecycledViewPool();
    private final CinemaVideoCardAdapter.Listener mCardListener;

    public CinemaRowsAdapter(@NonNull CinemaVideoCardAdapter.Listener cardListener) {
        mCardListener = cardListener;
        mSharedPool.setMaxRecycledViews(0, 24);
    }

    public void update(@NonNull VideoGroup group) {
        int action = group.getAction();

        if (action == VideoGroup.ACTION_REPLACE) {
            if (group.getPosition() == -1) {
                clear();
            } else {
                removeRowById(group.getId());
            }
        } else if (action == VideoGroup.ACTION_REMOVE) {
            RowEntry entry = findRow(group);
            if (entry != null) {
                entry.adapter.removeVideos(group.getVideos());
            }
            return;
        } else if (action == VideoGroup.ACTION_REMOVE_AUTHOR) {
            for (RowEntry entry : mRows) {
                entry.adapter.removeByAuthor(group.getVideos());
            }
            return;
        } else if (action == VideoGroup.ACTION_SYNC) {
            RowEntry entry = findRow(group);
            if (entry != null) {
                entry.adapter.syncVideos(group.getVideos());
            } else {
                for (RowEntry e : mRows) {
                    e.adapter.syncVideos(group.getVideos());
                }
            }
            return;
        }

        if (group.isEmpty()) {
            return;
        }

        RowEntry existing = findRow(group);
        if (existing != null) {
            if (action == VideoGroup.ACTION_PREPEND) {
                existing.adapter.prependVideos(group.getVideos());
            } else {
                existing.adapter.appendVideos(group.getVideos());
            }
            return;
        }

        int insertIndex = (group.getPosition() >= 0 && group.getPosition() <= mRows.size())
                ? group.getPosition()
                : mRows.size();

        CinemaVideoCardAdapter childAdapter = new CinemaVideoCardAdapter(
                new RowBoundListener(group.getId()),
                insertIndex,
                false
        );
        childAdapter.setVideos(group.getVideos());

        String rowTitle = !TextUtils.isEmpty(group.getTitle()) ? group.getTitle() : "";
        mRows.add(insertIndex, new RowEntry(group.getId(), rowTitle, childAdapter));
        notifyItemInserted(insertIndex);
    }

    public void clear() {
        int count = mRows.size();
        for (RowEntry entry : mRows) {
            entry.adapter.clear();
        }
        mRows.clear();
        if (count > 0) {
            notifyItemRangeRemoved(0, count);
        }
    }

    public boolean isEmpty() {
        return mRows.isEmpty();
    }

    @Nullable
    public Video getFirstVideo() {
        for (RowEntry row : mRows) {
            Video v = row.adapter.getItem(0);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private void removeRowById(int groupId) {
        for (int i = 0; i < mRows.size(); i++) {
            if (mRows.get(i).groupId == groupId) {
                mRows.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    @Nullable
    private RowEntry findRow(@NonNull VideoGroup group) {
        for (RowEntry entry : mRows) {
            if (entry.groupId == group.getId()) {
                return entry;
            }
        }
        if (!TextUtils.isEmpty(group.getTitle())) {
            for (RowEntry entry : mRows) {
                if (group.getTitle().equals(entry.title)) {
                    return entry;
                }
            }
        }
        return null;
    }

    private int indexOfRow(int groupId) {
        for (int i = 0; i < mRows.size(); i++) {
            if (mRows.get(i).groupId == groupId) {
                return i;
            }
        }
        return 0;
    }

    private final class RowBoundListener implements CinemaVideoCardAdapter.Listener {
        private final int mGroupId;

        RowBoundListener(int groupId) {
            mGroupId = groupId;
        }

        @Override
        public void onVideoFocused(@NonNull Video video, int ignoredRowIndex, int itemIndex) {
            mCardListener.onVideoFocused(video, indexOfRow(mGroupId), itemIndex);
        }

        @Override
        public void onVideoClicked(@NonNull Video video) {
            mCardListener.onVideoClicked(video);
        }

        @Override
        public void onVideoLongClicked(@NonNull Video video) {
            mCardListener.onVideoLongClicked(video);
        }

        @Override
        public void onScrollEnd(@NonNull Video lastVideo) {
            mCardListener.onScrollEnd(lastVideo);
        }
    }

    @NonNull
    @Override
    public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cinema_row, parent, false);
        return new RowHolder(view, mSharedPool);
    }

    @Override
    public void onBindViewHolder(@NonNull RowHolder holder, int position) {
        RowEntry entry = mRows.get(position);
        holder.bind(entry);
    }

    @Override
    public int getItemCount() {
        return mRows.size();
    }

    static final class RowHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final RecyclerView itemsView;

        RowHolder(@NonNull View itemView, @NonNull RecyclerView.RecycledViewPool pool) {
            super(itemView);
            titleView = itemView.findViewById(R.id.cinema_row_title);
            itemsView = itemView.findViewById(R.id.cinema_row_items);
            itemsView.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
            itemsView.setRecycledViewPool(pool);
            itemsView.setHasFixedSize(false);
            itemsView.setItemAnimator(null);
        }

        void bind(@NonNull RowEntry entry) {
            if (TextUtils.isEmpty(entry.title)) {
                titleView.setVisibility(View.GONE);
            } else {
                titleView.setText(entry.title);
                titleView.setVisibility(View.VISIBLE);
            }
            if (itemsView.getAdapter() != entry.adapter) {
                itemsView.setAdapter(entry.adapter);
            }
        }
    }
}
