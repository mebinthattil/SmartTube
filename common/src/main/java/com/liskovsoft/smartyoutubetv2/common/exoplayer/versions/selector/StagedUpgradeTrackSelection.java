package com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.selector;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.chunk.MediaChunk;
import com.google.android.exoplayer2.source.chunk.MediaChunkIterator;
import com.google.android.exoplayer2.trackselection.BaseTrackSelection;
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionUtil;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.liskovsoft.sharedutils.mylogger.Log;

import java.util.List;

/**
 * Seamless two-stage ABR TrackSelection inside a single codec TrackGroup:
 *   index 0 = fast-start 720p track (renders preview & Frame 1 rapidly)
 *   index 1 = user's full target quality (1080p / 4K, same codec & TrackGroup)
 *
 * Calling {@link #promoteToFullQuality()} flips the active index without calling
 * TrackSelector.setParameters(), so ExoPlayer NEVER flushes already-buffered chunks,
 * NEVER tears down the decoder, and NEVER shows a rebuffering spinner — the very next
 * network buffer segment is fetched at full quality seamlessly.
 */
public class StagedUpgradeTrackSelection extends BaseTrackSelection {
    private static final String TAG = "CinemaHandoff";

    private volatile boolean mPromoted;
    private int mSelectedIndex;
    private int mSelectionReason = C.SELECTION_REASON_INITIAL;

    public StagedUpgradeTrackSelection(TrackGroup group, int[] tracks) {
        super(group, tracks);
        // NOTE: BaseTrackSelection sorts `tracks` in DESCENDING bitrate order!
        // So index (length - 1) is the fast-start lower-bitrate 720p track,
        // and index 0 is the highest-bitrate full-quality (1080p/4K) track!
        mSelectedIndex = tracks.length > 1 ? (tracks.length - 1) : 0;
        mPromoted = tracks.length <= 1;
    }

    public void promoteToFullQuality() {
        if (!mPromoted) {
            mPromoted = true;
            Log.d(TAG, "StagedUpgradeTrackSelection promoted: next chunk will fetch " + getFormat(0));
        }
    }

    public boolean isPromoted() {
        return mPromoted;
    }

    public Format getFastStartFormat() {
        return getFormat(length() - 1);
    }

    public Format getTargetFormat() {
        return getFormat(0);
    }

    @Override
    public void updateSelectedTrack(
            long playbackPositionUs,
            long bufferedDurationUs,
            long availableDurationUs,
            List<? extends MediaChunk> queue,
            MediaChunkIterator[] mediaChunkIterators) {
        if (mPromoted && mSelectedIndex != 0) {
            mSelectedIndex = 0;
            mSelectionReason = C.SELECTION_REASON_ADAPTIVE;
        }
    }

    @Override
    public int getSelectedIndex() {
        return mSelectedIndex;
    }

    @Override
    public int getSelectionReason() {
        return mSelectionReason;
    }

    @Nullable
    @Override
    public Object getSelectionData() {
        return null;
    }

    public static final class Factory implements TrackSelection.Factory {
        private volatile StagedUpgradeTrackSelection mActiveSelection;
        private volatile boolean mPendingPromote;

        public void promoteActiveSelection() {
            mPendingPromote = true;
            StagedUpgradeTrackSelection current = mActiveSelection;
            if (current != null) {
                current.promoteToFullQuality();
            }
        }

        public void resetForNewSource() {
            mPendingPromote = false;
            mActiveSelection = null;
        }

        @Nullable
        public StagedUpgradeTrackSelection getActiveSelection() {
            return mActiveSelection;
        }

        @Override
        public TrackSelection[] createTrackSelections(
                Definition[] definitions, BandwidthMeter bandwidthMeter) {
            return TrackSelectionUtil.createTrackSelectionsForDefinitions(
                    definitions,
                    definition -> {
                        if (definition.tracks.length == 1) {
                            return new FixedTrackSelection(
                                    definition.group,
                                    definition.tracks[0],
                                    definition.reason,
                                    definition.data);
                        }
                        StagedUpgradeTrackSelection staged =
                                new StagedUpgradeTrackSelection(definition.group, definition.tracks);
                        if (mPendingPromote) {
                            staged.promoteToFullQuality();
                        }
                        mActiveSelection = staged;
                        return staged;
                    });
        }
    }
}
