package com.liskovsoft.smartyoutubetv2.tv.ui.cinema;

import android.content.res.ColorStateList;
import android.graphics.Outline;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import androidx.core.view.animation.PathInterpolatorCompat;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.mediaserviceinterfaces.oauth.Account;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.errors.ErrorFragmentData;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers.VideoLoaderController;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.AccountSelectionPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.AccountSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.BrowseView;
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager;
import com.liskovsoft.smartyoutubetv2.common.misc.CrashRestorer;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;
import com.liskovsoft.smartyoutubetv2.common.prefs.SponsorBlockData;
import com.liskovsoft.smartyoutubetv2.common.utils.SimpleEditDialog;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.embedplayer.EmbedPlayerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Apple TV+ Cinema redesign of SmartTube's main Browse UI.
 * Implements SmartTube's BrowseView contract directly so all upstream feeds, accounts,
 * SponsorBlock, DeArrow, playlists, channels, search, and settings work seamlessly.
 */
public class CinemaBrowseFragment extends Fragment
        implements BrowseView, MediaServiceManager.AccountChangeListener {

    private static final long HERO_PREFETCH_DELAY_MS = 300L;
    private static final long HERO_PREVIEW_DELAY_MS = 900L;
    private static final long HERO_PREVIEW_MAX_MS = 90_000L;
    private static final int GRID_COLS_STANDARD = 4;
    private static final int GRID_COLS_SHORTS = 5;
    private static final Interpolator APPLE_EASE_OUT = PathInterpolatorCompat.create(0.22f, 1.0f, 0.36f, 1.0f);
    private static final Interpolator APPLE_EASE_RETURN = PathInterpolatorCompat.create(0.25f, 0.46f, 0.45f, 0.94f);

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final List<BrowseSection> mSections = new ArrayList<>();
    private final List<TextView> mSectionTabViews = new ArrayList<>();
    private final android.util.SparseArray<BrowseSection> mPresenterIndexToSection = new android.util.SparseArray<>();

    private BrowsePresenter mBrowsePresenter;
    private CrashRestorer mCrashRestorer;
    private boolean mIsFragmentCreated;

    // Hero stage views
    private ImageView mHeroBackdropA;
    private ImageView mHeroBackdropB;
    private boolean mUseSlotA = true;
    private String mCurrentBackdropUrl;
    private EmbedPlayerView mHeroPreviewPlayer;
    private LinearLayout mHeroInfoContainer;
    private TextView mHeroChannel;
    private TextView mHeroStats;
    private TextView mHeroLiveBadge;
    private TextView mHeroPreviewBadge;
    private TextView mHeroTitle;
    private TextView mHeroSubtitle;
    private View mSettingsHeader;
    private TextView mSettingsModeBadge;
    private boolean mHeroCondensed = false;

    // Top bar views
    private HorizontalScrollView mNavScroll;
    private LinearLayout mNavTabsContainer;
    private TextView mPipPill;
    private LinearLayout mSearchBtn;
    private ImageView mSearchIcon;
    private TextView mSearchLabel;
    private View mSettingsBtn;
    private ImageView mSettingsIcon;
    private LinearLayout mAccountBtn;
    private ImageView mAccountAvatar;
    private ImageView mAccountGuestIcon;
    private TextView mAccountInitial;
    private TextView mAccountName;
    private View mContentHost;

    // Content views & adapters
    private RecyclerView mChannelStripRecycler;
    private RecyclerView mRowsRecycler;
    private RecyclerView mGridRecycler;
    private RecyclerView mSettingsRecycler;
    private View mErrorContainer;
    private TextView mErrorMessage;
    private TextView mErrorAction;
    private ProgressBar mLoadingView;

    private CinemaRowsAdapter mRowsAdapter;
    private CinemaVideoCardAdapter mGridAdapter;
    private CinemaSettingsAdapter mSettingsAdapter;
    private CinemaChannelStripAdapter mChannelStripAdapter;
    private GridLayoutManager mGridLayoutManager;

    private int mSelectedSectionIndex = 0;
    private int mLastFocusedRowIndex = 0;
    private BrowseSection mActiveSection;
    private BrowseSection mSettingsSection;
    private boolean mInSettingsMode = false;
    private String mLastAnimatedHeroKey;
    private boolean mPendingFocusOnContent = true;
    private Video mHeroVideo;
    private Video mPendingPreviewVideo;
    private Video mPendingPrefetchVideo;
    private boolean mIsPreviewActive = false;

    private final Runnable mPrefetchFormatRunnable = this::runScheduledFormatPrefetch;
    private final Runnable mStartPreviewRunnable = this::startScheduledHeroPreview;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(null);
        if (getContext() == null) {
            return;
        }
        mCrashRestorer = new CrashRestorer(getContext(), savedInstanceState);
        mIsFragmentCreated = true;
        mBrowsePresenter = BrowsePresenter.instance(getContext());
        mBrowsePresenter.setView(this);
        MediaServiceManager.instance().addAccountListener(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_cinema_browse, container, false);
        bindViews(root);
        setupAdapters();
        setupTopBarActions();
        setupHeroPreviewPlayer();
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mBrowsePresenter == null) {
            return;
        }
        mBrowsePresenter.onViewInitialized();
        updateAccountPill();
        updateSponsorBlockBadge();
        updatePipPill();

        if (mCrashRestorer != null) {
            mCrashRestorer.restoreHeader((idx, video) -> {
                selectSection(idx, true);
                selectSectionItem(video);
            });
            mCrashRestorer.restorePlayback();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mCrashRestorer != null && mBrowsePresenter != null) {
            mCrashRestorer.persistHeaderIndex(outState, mSelectedSectionIndex);
            mCrashRestorer.persistVideo(outState, mBrowsePresenter.getCurrentVideo());
        }
    }

    private void bindViews(@NonNull View root) {
        mHeroBackdropA = root.findViewById(R.id.hero_backdrop_a);
        mHeroBackdropB = root.findViewById(R.id.hero_backdrop_b);
        mHeroPreviewPlayer = root.findViewById(R.id.hero_preview_player);
        mHeroInfoContainer = root.findViewById(R.id.cinema_hero_info);
        mHeroChannel = root.findViewById(R.id.hero_channel);
        mHeroStats = root.findViewById(R.id.hero_stats);
        mHeroLiveBadge = root.findViewById(R.id.hero_live_badge);
        mHeroPreviewBadge = root.findViewById(R.id.hero_preview_badge);
        mHeroTitle = root.findViewById(R.id.hero_title);
        mHeroSubtitle = root.findViewById(R.id.hero_subtitle);
        mSettingsHeader = root.findViewById(R.id.cinema_settings_header);
        mSettingsModeBadge = root.findViewById(R.id.cinema_settings_mode_badge);

        mNavScroll = root.findViewById(R.id.cinema_nav_scroll);
        mNavScroll.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), view.getHeight() / 2f);
            }
        });
        mNavScroll.setClipToOutline(true);
        mNavTabsContainer = root.findViewById(R.id.cinema_nav_tabs);
        mPipPill = root.findViewById(R.id.cinema_pip_pill);
        mSearchBtn = root.findViewById(R.id.cinema_search_btn);
        mSearchIcon = root.findViewById(R.id.cinema_search_icon);
        mSearchLabel = root.findViewById(R.id.cinema_search_label);
        mSettingsBtn = root.findViewById(R.id.cinema_settings_btn);
        mSettingsIcon = root.findViewById(R.id.cinema_settings_icon);
        mAccountBtn = root.findViewById(R.id.cinema_account_btn);
        mAccountAvatar = root.findViewById(R.id.cinema_account_avatar);
        mAccountGuestIcon = root.findViewById(R.id.cinema_account_guest_icon);
        mAccountInitial = root.findViewById(R.id.cinema_account_initial);
        mAccountName = root.findViewById(R.id.cinema_account_name);
        mContentHost = root.findViewById(R.id.cinema_content_host);

        mAccountAvatar.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        mAccountAvatar.setClipToOutline(true);

        mChannelStripRecycler = root.findViewById(R.id.cinema_channel_strip);
        mRowsRecycler = root.findViewById(R.id.cinema_rows_recycler);
        mGridRecycler = root.findViewById(R.id.cinema_grid_recycler);
        mSettingsRecycler = root.findViewById(R.id.cinema_settings_recycler);
        mErrorContainer = root.findViewById(R.id.cinema_error_container);
        mErrorMessage = root.findViewById(R.id.cinema_error_message);
        mErrorAction = root.findViewById(R.id.cinema_error_action);
        mLoadingView = root.findViewById(R.id.cinema_loading);

        // Smart Back key: lift focus from content up to Top Liquid Glass Nav Bar
        root.setFocusableInTouchMode(false);
    }

    private void setupAdapters() {
        CinemaVideoCardAdapter.Listener cardListener = new CinemaVideoCardAdapter.Listener() {
            @Override
            public void onVideoFocused(@NonNull Video video, int rowIndex, int itemIndex) {
                boolean isRowMode = mActiveSection != null && mActiveSection.getType() == BrowseSection.TYPE_ROW;
                boolean shouldCondense = mActiveSection != null
                        && (isRowMode ? rowIndex > 0 : itemIndex >= getActiveGridCols());
                setHeroCondensed(shouldCondense);
                mLastFocusedRowIndex = rowIndex;

                onHeroVideoFocused(video);
                if (mBrowsePresenter != null) {
                    mBrowsePresenter.onVideoItemSelected(video);
                }
            }

            @Override
            public void onVideoClicked(@NonNull Video video) {
                // Seamless handoff: transfer live playing preview ExoPlayer straight into fullscreen
                // so playback continues instantaneously while the next DASH chunk upgrades to 1080p/4K.
                if (mHeroPreviewPlayer != null && mHeroPreviewPlayer.canHandoff(video)
                        && mHeroPreviewPlayer.detachForHandoff()) {
                    mMainHandler.removeCallbacks(mStartPreviewRunnable);
                    mPendingPreviewVideo = null;
                    mIsPreviewActive = false;
                    if (mHeroPreviewBadge != null) {
                        mHeroPreviewBadge.setVisibility(View.GONE);
                    }
                } else {
                    stopHeroPreview(true);
                }
                if (mBrowsePresenter != null) {
                    mBrowsePresenter.onVideoItemClicked(video);
                }
            }

            @Override
            public void onVideoLongClicked(@NonNull Video video) {
                stopHeroPreview(true);
                if (mBrowsePresenter != null) {
                    mBrowsePresenter.onVideoItemLongClicked(video);
                }
            }

            @Override
            public void onScrollEnd(@NonNull Video lastVideo) {
                if (mBrowsePresenter != null) {
                    mBrowsePresenter.onScrollEnd(lastVideo);
                }
            }
        };

        mRowsAdapter = new CinemaRowsAdapter(cardListener);
        mRowsRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false) {
            @Override
            public boolean requestChildRectangleOnScreen(@NonNull RecyclerView parent, @NonNull View child,
                                                         @NonNull android.graphics.Rect rect, boolean immediate,
                                                         boolean focusedChildVisible) {
                int dy = child.getTop() - parent.getPaddingTop();
                if (dy != 0) {
                    parent.stopScroll();
                    if (immediate) {
                        parent.scrollBy(0, dy);
                    } else {
                        parent.smoothScrollBy(0, dy);
                    }
                    return true;
                }
                return false;
            }
        });
        mRowsRecycler.setAdapter(mRowsAdapter);
        mRowsRecycler.setItemAnimator(null);

        mGridAdapter = new CinemaVideoCardAdapter(cardListener, 0, true);
        mGridLayoutManager = new GridLayoutManager(getContext(), GRID_COLS_STANDARD);
        mGridRecycler.setLayoutManager(mGridLayoutManager);
        mGridRecycler.setAdapter(mGridAdapter);
        mGridRecycler.setItemAnimator(null);

        mSettingsAdapter = new CinemaSettingsAdapter(new CinemaSettingsAdapter.OnSettingClickListener() {
            @Override
            public void onSettingClicked(@NonNull SettingsItem item) {
                stopHeroPreview(true);
                handleSettingClick(item);
            }

            @Override
            public void onSettingFocused() {
                stopHeroPreview(true);
            }
        });
        mSettingsRecycler.setLayoutManager(new GridLayoutManager(getContext(), 4));
        mSettingsRecycler.setAdapter(mSettingsAdapter);

        mChannelStripAdapter = new CinemaChannelStripAdapter(new CinemaChannelStripAdapter.Listener() {
            @Override
            public void onChannelFocused(@NonNull Video channel, int position) {
                stopHeroPreview(true);
                setHeroCondensed(true);
                if (mBrowsePresenter != null) {
                    mBrowsePresenter.onVideoItemSelected(channel);
                }
            }

            @Override
            public void onChannelClicked(@NonNull Video channel) {
                stopHeroPreview(true);
                if (mBrowsePresenter != null) {
                    mBrowsePresenter.onVideoItemClicked(channel);
                }
            }

            @Override
            public void onChannelLongClicked(@NonNull Video channel) {
                stopHeroPreview(true);
                if (mBrowsePresenter != null) {
                    mBrowsePresenter.onVideoItemLongClicked(channel);
                }
            }

            @Override
            public void onChannelScrollEnd(@NonNull Video lastChannel) {
                if (mBrowsePresenter != null) {
                    mBrowsePresenter.onScrollEnd(lastChannel);
                }
            }
        });
        mChannelStripRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        mChannelStripRecycler.setAdapter(mChannelStripAdapter);
    }

    private void setupTopBarActions() {
        mSearchBtn.setOnClickListener(v -> {
            stopHeroPreview(true);
            if (getContext() != null) {
                SearchPresenter.instance(getContext()).startSearch(null);
            }
        });
        mSearchBtn.setOnFocusChangeListener((v, hasFocus) -> {
            styleTopPillFocus(v, hasFocus);
            int color = ContextCompat.getColor(v.getContext(),
                    hasFocus ? R.color.cinema_text_dark : R.color.cinema_text_primary);
            mSearchLabel.setTextColor(color);
            mSearchIcon.setImageTintList(ColorStateList.valueOf(color));
            if (hasFocus) {
                stopHeroPreview(true);
            }
        });

        if (mSettingsBtn != null) {
            mSettingsBtn.setOnClickListener(v -> openSettingsSection(true));
            mSettingsBtn.setOnFocusChangeListener((v, hasFocus) -> {
                styleTopPillFocus(v, hasFocus);
                int color = ContextCompat.getColor(v.getContext(),
                        hasFocus ? R.color.cinema_text_dark : R.color.cinema_text_primary);
                if (mSettingsIcon != null) {
                    mSettingsIcon.setImageTintList(ColorStateList.valueOf(color));
                }
                if (hasFocus) {
                    stopHeroPreview(true);
                }
            });
        }

        mAccountBtn.setOnClickListener(v -> {
            stopHeroPreview(true);
            if (getContext() != null) {
                AccountSelectionPresenter.instance(getContext()).nextAccountOrDialog();
            }
        });
        mAccountBtn.setOnLongClickListener(v -> {
            stopHeroPreview(true);
            if (getContext() != null) {
                AccountSettingsPresenter.instance(getContext()).show();
            }
            return true;
        });
        mAccountBtn.setOnFocusChangeListener((v, hasFocus) -> {
            styleTopPillFocus(v, hasFocus);
            int color = ContextCompat.getColor(v.getContext(),
                    hasFocus ? R.color.cinema_text_dark : R.color.cinema_text_primary);
            mAccountName.setTextColor(color);
            if (mAccountGuestIcon != null) {
                mAccountGuestIcon.setImageTintList(ColorStateList.valueOf(color));
            }
            if (hasFocus) {
                stopHeroPreview(true);
            }
        });

        mPipPill.setOnClickListener(v -> {
            stopHeroPreview(true);
            if (getContext() != null) {
                ViewManager.instance(getContext()).startView(PlaybackView.class);
            }
        });
        mPipPill.setOnFocusChangeListener((v, hasFocus) -> {
            styleTopPillFocus(v, hasFocus);
            mPipPill.setTextColor(ContextCompat.getColor(v.getContext(),
                    hasFocus ? R.color.cinema_text_dark : R.color.cinema_text_primary));
            if (hasFocus) {
                stopHeroPreview(true);
            }
        });

        mErrorAction.setOnFocusChangeListener((v, hasFocus) -> {
            styleTopPillFocus(v, hasFocus);
            mErrorAction.setTextColor(ContextCompat.getColor(v.getContext(),
                    hasFocus ? R.color.cinema_text_dark : R.color.cinema_text_primary));
        });
    }

    private void setupHeroPreviewPlayer() {
        mHeroPreviewPlayer.setUseController(false);
        mHeroPreviewPlayer.setMute(false); // Unmuted live preview by default
        mHeroPreviewPlayer.setSkipWatchSync(true); // Previews never pollute Watch History
        mHeroPreviewPlayer.setQuality(EmbedPlayerView.QUALITY_NORMAL);
        mHeroPreviewPlayer.setSmoothFade(true);
        mHeroPreviewPlayer.setMaxMuteDurationMs(HERO_PREVIEW_MAX_MS);
        mHeroPreviewPlayer.setPreviewStateListener(new EmbedPlayerView.OnPreviewStateListener() {
            @Override
            public void onFirstFrameRendered() {
                mMainHandler.post(() -> {
                    if (mIsPreviewActive && mHeroPreviewBadge != null) {
                        mHeroPreviewBadge.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onPreviewError() {
                mMainHandler.post(() -> {
                    mIsPreviewActive = false;
                    if (mHeroPreviewBadge != null) {
                        mHeroPreviewBadge.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onPreviewStopped() {
                mMainHandler.post(() -> {
                    mIsPreviewActive = false;
                    if (mHeroPreviewBadge != null) {
                        mHeroPreviewBadge.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    // -------------------------------------------------------------------------
    // HERO STAGE & NETFLIX-STYLE 1.6s AUTO-PREVIEW ENGINE
    // -------------------------------------------------------------------------

    private void onHeroVideoFocused(@Nullable Video video) {
        if (video == null) {
            return;
        }
        boolean isSameVideo = mHeroVideo != null && (mHeroVideo == video
                || (!TextUtils.isEmpty(mHeroVideo.videoId) && mHeroVideo.videoId.equals(video.videoId)));
        mHeroVideo = video;

        // Always update metadata immediately (handles DeArrow / ACTION_SYNC updates too)
        updateHeroMetadata(video);
        crossfadeHeroBackdrop(!TextUtils.isEmpty(video.bgImageUrl) ? video.bgImageUrl : video.cardImageUrl);

        if (isSameVideo && (mIsPreviewActive || mPendingPreviewVideo == video)) {
            return;
        }

        mMainHandler.removeCallbacks(mPrefetchFormatRunnable);
        mMainHandler.removeCallbacks(mStartPreviewRunnable);
        stopHeroPreview(true);

        if (isPreviewEligible(video)) {
            mPendingPrefetchVideo = video;
            mPendingPreviewVideo = video;
            mMainHandler.postDelayed(mPrefetchFormatRunnable, HERO_PREFETCH_DELAY_MS);
            mMainHandler.postDelayed(mStartPreviewRunnable, HERO_PREVIEW_DELAY_MS);
        }
    }

    private void runScheduledFormatPrefetch() {
        Video target = mPendingPrefetchVideo;
        mPendingPrefetchVideo = null;
        if (target == null || target != mHeroVideo || !isPreviewEligible(target) || isFocusInTopBar()) {
            return;
        }
        if (getContext() != null && !TextUtils.isEmpty(target.videoId)) {
            boolean allowRestricted = !GeneralData.instance(getContext()).isChildModeEnabled();
            VideoLoaderController.prefetchFormatInfo(target.videoId, allowRestricted);
        }
    }

    private boolean isPreviewEligible(@Nullable Video video) {
        if (video == null || !isResumed() || getContext() == null) {
            return false;
        }
        if (!video.hasVideo() || TextUtils.isEmpty(video.videoId)) {
            return false;
        }
        if (video.isUpcoming || video.isUnplayable || video.isChapter) {
            return false;
        }
        if (mActiveSection != null && (mActiveSection.getType() == BrowseSection.TYPE_SETTINGS_GRID
                || mActiveSection.getType() == BrowseSection.TYPE_ERROR)) {
            return false;
        }
        // Never interrupt active background music or PiP playback
        if (PlaybackPresenter.instance(getContext()).isRunningInBackground()) {
            return false;
        }
        return true;
    }

    private void startScheduledHeroPreview() {
        Video target = mPendingPreviewVideo;
        mPendingPreviewVideo = null;
        if (target == null || target != mHeroVideo || !isPreviewEligible(target)) {
            return;
        }
        // Only start preview if focus is still inside the video content area (not Top Bar)
        if (isFocusInTopBar()) {
            return;
        }
        mIsPreviewActive = true;
        mHeroPreviewPlayer.setMute(false);
        mHeroPreviewPlayer.openVideo(target);
    }

    private void stopHeroPreview(boolean immediate) {
        mMainHandler.removeCallbacks(mPrefetchFormatRunnable);
        mMainHandler.removeCallbacks(mStartPreviewRunnable);
        mPendingPrefetchVideo = null;
        mPendingPreviewVideo = null;
        if (mHeroPreviewBadge != null) {
            mHeroPreviewBadge.setVisibility(View.GONE);
        }
        if (mHeroPreviewPlayer != null && (mIsPreviewActive || mHeroPreviewPlayer.isEngineInitialized())) {
            mIsPreviewActive = false;
            if (immediate) {
                mHeroPreviewPlayer.finish();
            } else {
                mHeroPreviewPlayer.setMute(true);
                mHeroPreviewPlayer.finish();
            }
        }
    }

    private void updateHeroMetadata(@NonNull Video video) {
        mHeroInfoContainer.setVisibility(View.VISIBLE);

        String author = video.getAuthor();
        mHeroChannel.setText(!TextUtils.isEmpty(author) ? author : "");
        mHeroChannel.setVisibility(!TextUtils.isEmpty(author) ? View.VISIBLE : View.GONE);

        CharSequence second = video.getSecondTitle();
        String stats = second != null ? second.toString() : "";
        if (!TextUtils.isEmpty(author) && stats.startsWith(author)) {
            stats = stats.substring(author.length()).replaceFirst("^\\s*[•·\\-]+\\s*", "");
        }
        mHeroStats.setText(stats);
        mHeroStats.setVisibility(!TextUtils.isEmpty(stats) ? View.VISIBLE : View.GONE);

        mHeroLiveBadge.setVisibility(video.isLive ? View.VISIBLE : View.GONE);

        String title = video.getTitleFull();
        if (TextUtils.isEmpty(title)) {
            title = video.getTitle();
        }
        mHeroTitle.setText(title != null ? title : "");

        String desc = !TextUtils.isEmpty(video.description) ? video.description : "";
        if (TextUtils.isEmpty(desc) && !TextUtils.isEmpty(video.category)) {
            desc = video.category;
        }
        mHeroSubtitle.setText(desc);
        boolean showSub = !TextUtils.isEmpty(desc) && !mHeroCondensed;
        mHeroSubtitle.setVisibility(showSub ? View.VISIBLE : View.GONE);
        mHeroSubtitle.setAlpha(showSub ? 1f : 0f);

        // Silky Apple tvOS Hero typography lift + fade whenever switching to a new video
        String heroKey = video.videoId + "|" + title;
        if (!heroKey.equals(mLastAnimatedHeroKey) && mHeroInfoContainer != null) {
            mLastAnimatedHeroKey = heroKey;
            float shiftPx = 8f * getResources().getDisplayMetrics().density;
            mHeroInfoContainer.animate().cancel();
            mHeroInfoContainer.setAlpha(0.55f);
            mHeroInfoContainer.setTranslationY(shiftPx);
            mHeroInfoContainer.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(210)
                    .setInterpolator(APPLE_EASE_OUT)
                    .start();
        }
    }

    private void crossfadeHeroBackdrop(@Nullable String imageUrl) {
        if (getContext() == null || TextUtils.isEmpty(imageUrl) || imageUrl.equals(mCurrentBackdropUrl)) {
            return;
        }
        mCurrentBackdropUrl = imageUrl;

        final ImageView next = mUseSlotA ? mHeroBackdropA : mHeroBackdropB;
        final ImageView prev = mUseSlotA ? mHeroBackdropB : mHeroBackdropA;
        mUseSlotA = !mUseSlotA;

        next.animate().cancel();
        prev.animate().cancel();
        next.setAlpha(0f);

        Glide.with(getContext().getApplicationContext())
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .centerCrop()
                .into(next);

        next.animate().alpha(1f).setDuration(420).setInterpolator(new DecelerateInterpolator()).start();
        prev.animate().alpha(0f).setDuration(420).setInterpolator(new DecelerateInterpolator()).start();
    }

    private void clearHeroBackdrop() {
        mCurrentBackdropUrl = null;
        if (mHeroBackdropA != null) {
            mHeroBackdropA.animate().cancel();
            mHeroBackdropA.animate().alpha(0f).setDuration(260).start();
        }
        if (mHeroBackdropB != null) {
            mHeroBackdropB.animate().cancel();
            mHeroBackdropB.animate().alpha(0f).setDuration(260).start();
        }
    }

    private void clearHeroMetadata() {
        mHeroVideo = null;
        if (mHeroChannel != null) mHeroChannel.setText("");
        if (mHeroStats != null) mHeroStats.setText("");
        if (mHeroTitle != null) mHeroTitle.setText("");
        if (mHeroSubtitle != null) {
            mHeroSubtitle.setText("");
            mHeroSubtitle.setVisibility(View.GONE);
        }
        if (mHeroLiveBadge != null) mHeroLiveBadge.setVisibility(View.GONE);
        if (mHeroPreviewBadge != null) mHeroPreviewBadge.setVisibility(View.GONE);
    }

    private void setHeroCondensed(boolean condensed) {
        if (mHeroCondensed == condensed || mHeroInfoContainer == null) {
            return;
        }
        mHeroCondensed = condensed;

        mHeroTitle.setMaxLines(condensed ? 1 : 2);
        boolean hasSub = !TextUtils.isEmpty(mHeroSubtitle.getText());
        if (hasSub) {
            mHeroSubtitle.animate().cancel();
            if (condensed) {
                mHeroSubtitle.animate()
                        .alpha(0f)
                        .setDuration(150)
                        .setInterpolator(APPLE_EASE_RETURN)
                        .withEndAction(() -> {
                            if (mHeroCondensed && mHeroSubtitle != null) {
                                mHeroSubtitle.setVisibility(View.GONE);
                            }
                        })
                        .start();
            } else {
                mHeroSubtitle.setAlpha(0f);
                mHeroSubtitle.setVisibility(View.VISIBLE);
                mHeroSubtitle.animate()
                        .alpha(1f)
                        .setDuration(180)
                        .setInterpolator(APPLE_EASE_OUT)
                        .start();
            }
        }
    }

    /**
     * Apple TV ergonomics: pressing Back from deep in the video rails lifts focus to the
     * floating top Liquid Glass bar first; pressing Back from the top bar exits normally.
     */
    public boolean handleBackPress() {
        if (mInSettingsMode) {
            stopHeroPreview(true);
            if (!isFocusInTopBar() && mSettingsBtn != null) {
                mSettingsBtn.requestFocus();
                return true;
            }
            if (!mSections.isEmpty()) {
                selectSectionInternal(0, true);
                return true;
            }
        }
        if (!isFocusInTopBar() && mSelectedSectionIndex >= 0 && mSelectedSectionIndex < mSectionTabViews.size()) {
            stopHeroPreview(true);
            setHeroCondensed(false);
            mSectionTabViews.get(mSelectedSectionIndex).requestFocus();
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // FLOATING LIQUID GLASS TOP NAVIGATION TABS
    // -------------------------------------------------------------------------

    private void rebuildNavTabs() {
        if (getContext() == null || mNavTabsContainer == null) {
            return;
        }
        mNavTabsContainer.removeAllViews();
        mSectionTabViews.clear();

        float density = getResources().getDisplayMetrics().density;
        int padH = Math.round(14f * density);
        int padV = Math.round(6f * density);
        int marginH = Math.round(2f * density);

        for (int i = 0; i < mSections.size(); i++) {
            final int index = i;
            final BrowseSection section = mSections.get(i);

            TextView tab = new TextView(getContext());
            tab.setId(View.generateViewId());
            tab.setText(section.getTitle());
            tab.setTextSize(12.5f);
            tab.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
            tab.setGravity(Gravity.CENTER);
            tab.setSingleLine(true);
            tab.setFocusable(true);
            tab.setPadding(padH, padV, padH, padV);
            tab.setBackgroundResource(R.drawable.bg_cinema_nav_tab);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Math.round(32f * density)
            );
            lp.setMarginStart(marginH);
            lp.setMarginEnd(marginH);
            tab.setLayoutParams(lp);

            tab.setSelected(i == mSelectedSectionIndex);
            updateTabColors(tab, false, i == mSelectedSectionIndex);

            tab.setOnFocusChangeListener((v, hasFocus) -> {
                updateTabColors(tab, hasFocus, index == mSelectedSectionIndex);
                styleTopPillFocus(tab, hasFocus);
                if (hasFocus) {
                    stopHeroPreview(true);
                    scrollTabIntoView(tab);
                    if (index != mSelectedSectionIndex) {
                        selectSectionInternal(index, false);
                    }
                }
            });

            tab.setOnClickListener(v -> {
                selectSectionInternal(index, true);
            });

            tab.setOnLongClickListener(v -> {
                if (mBrowsePresenter != null) {
                    mBrowsePresenter.onSectionLongPressed(section.getId());
                }
                return true;
            });

            mNavTabsContainer.addView(tab);
            mSectionTabViews.add(tab);
        }

        // Explicitly wire horizontal focus chain across top Liquid Glass bar so
        // pressing Right on the last tab (e.g. Settings) cleanly enters Search -> Sign In.
        wireTopBarFocusChain();
    }

    private void wireTopBarFocusChain() {
        if (mSectionTabViews.isEmpty()) {
            return;
        }
        int firstTabId = mSectionTabViews.get(0).getId();
        int lastTabId = mSectionTabViews.get(mSectionTabViews.size() - 1).getId();
        int rightEntryId = (mPipPill != null && mPipPill.getVisibility() == View.VISIBLE)
                ? R.id.cinema_pip_pill
                : R.id.cinema_settings_btn;

        // Left-to-right top bar: [Search] <-> [Home | Shorts | History] <-> [Pip?] <-> [Settings Gear] <-> [Account]
        if (mSearchBtn != null) {
            mSearchBtn.setNextFocusLeftId(R.id.cinema_search_btn);
            mSearchBtn.setNextFocusRightId(firstTabId);
            mSearchBtn.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && !mSectionTabViews.isEmpty()) {
                    mSectionTabViews.get(0).requestFocus();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    focusOnContent();
                    return true;
                }
                return false;
            });
        }

        for (int i = 0; i < mSectionTabViews.size(); i++) {
            final int tabIdx = i;
            TextView tab = mSectionTabViews.get(i);
            tab.setNextFocusLeftId(i > 0 ? mSectionTabViews.get(i - 1).getId() : R.id.cinema_search_btn);
            tab.setNextFocusRightId(i < mSectionTabViews.size() - 1 ? mSectionTabViews.get(i + 1).getId() : rightEntryId);

            // Bypass HorizontalScrollView's internal FocusFinder scope so LEFT on Home cleanly enters
            // Search and RIGHT on History cleanly enters Settings Gear -> Sign In.
            tab.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && tabIdx == 0 && mSearchBtn != null) {
                    mSearchBtn.requestFocus();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && tabIdx == mSectionTabViews.size() - 1) {
                    View target = (mPipPill != null && mPipPill.getVisibility() == View.VISIBLE)
                            ? mPipPill : mSettingsBtn;
                    if (target != null) {
                        target.requestFocus();
                        return true;
                    }
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    focusOnContent();
                    return true;
                }
                return false;
            });
        }

        if (mPipPill != null) {
            mPipPill.setNextFocusLeftId(lastTabId);
            mPipPill.setNextFocusRightId(R.id.cinema_settings_btn);
            mPipPill.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && !mSectionTabViews.isEmpty()) {
                    mSectionTabViews.get(mSectionTabViews.size() - 1).requestFocus();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && mSettingsBtn != null) {
                    mSettingsBtn.requestFocus();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    focusOnContent();
                    return true;
                }
                return false;
            });
        }

        if (mSettingsBtn != null) {
            mSettingsBtn.setNextFocusLeftId(
                    (mPipPill != null && mPipPill.getVisibility() == View.VISIBLE)
                            ? R.id.cinema_pip_pill : lastTabId);
            mSettingsBtn.setNextFocusRightId(R.id.cinema_account_btn);
            mSettingsBtn.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    if (mPipPill != null && mPipPill.getVisibility() == View.VISIBLE) {
                        mPipPill.requestFocus();
                    } else if (!mSectionTabViews.isEmpty()) {
                        mSectionTabViews.get(mSectionTabViews.size() - 1).requestFocus();
                    }
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && mAccountBtn != null) {
                    mAccountBtn.requestFocus();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    focusOnContent();
                    return true;
                }
                return false;
            });
        }

        if (mAccountBtn != null) {
            mAccountBtn.setNextFocusLeftId(R.id.cinema_settings_btn);
            mAccountBtn.setNextFocusRightId(R.id.cinema_account_btn);
            mAccountBtn.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && mSettingsBtn != null) {
                    mSettingsBtn.requestFocus();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    focusOnContent();
                    return true;
                }
                return false;
            });
        }
    }

    private void openSettingsSection(boolean focusContent) {
        if (mSettingsSection == null) {
            return;
        }
        mInSettingsMode = true;
        mActiveSection = mSettingsSection;
        mPendingFocusOnContent = focusContent;
        for (int i = 0; i < mSectionTabViews.size(); i++) {
            TextView tv = mSectionTabViews.get(i);
            tv.setSelected(false);
            updateTabColors(tv, tv.hasFocus(), false);
        }
        if (mSettingsBtn != null) {
            mSettingsBtn.setSelected(true);
        }
        stopHeroPreview(true);
        configureContentModeForSection(mSettingsSection);
        if (mBrowsePresenter != null) {
            mBrowsePresenter.onSectionFocused(mSettingsSection.getId());
        }
    }

    private void updateTabColors(@NonNull TextView tab, boolean focused, boolean selected) {
        if (getContext() == null) {
            return;
        }
        int colorRes = focused ? R.color.cinema_text_dark
                : (selected ? R.color.cinema_text_primary : R.color.cinema_text_secondary);
        tab.setTextColor(ContextCompat.getColor(getContext(), colorRes));
    }

    private void styleTopPillFocus(@NonNull View view, boolean focused) {
        view.animate().cancel();
        if (focused) {
            view.setAlpha(0.86f);
        }
        view.animate()
                .scaleX(focused ? 1.05f : 1.0f)
                .scaleY(focused ? 1.05f : 1.0f)
                .alpha(1.0f)
                .setDuration(focused ? 190L : 150L)
                .setInterpolator(focused ? APPLE_EASE_OUT : APPLE_EASE_RETURN)
                .start();
    }

    private void scrollTabIntoView(@NonNull View tab) {
        if (mNavScroll == null) {
            return;
        }
        int targetX = tab.getLeft() - (mNavScroll.getWidth() - tab.getWidth()) / 2;
        mNavScroll.smoothScrollTo(Math.max(0, targetX), 0);
    }

    private boolean isFocusInTopBar() {
        View root = getView();
        if (root == null) {
            return false;
        }
        View focused = root.findFocus();
        if (focused == null) {
            return false;
        }
        View topBar = root.findViewById(R.id.cinema_top_bar);
        View p = focused;
        while (p != null) {
            if (p == topBar) {
                return true;
            }
            if (p.getParent() instanceof View) {
                p = (View) p.getParent();
            } else {
                break;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // SECTION SWITCHING & BROWSEVIEW CONTRACT
    // -------------------------------------------------------------------------

    private void selectSectionInternal(int index, boolean focusContent) {
        if (mSections.isEmpty()) {
            return;
        }
        mInSettingsMode = false;
        if (mSettingsBtn != null) {
            mSettingsBtn.setSelected(false);
        }
        int clamped = Math.max(0, Math.min(index, mSections.size() - 1));
        mSelectedSectionIndex = clamped;
        mActiveSection = mSections.get(clamped);
        mPendingFocusOnContent = focusContent;

        for (int i = 0; i < mSectionTabViews.size(); i++) {
            TextView tv = mSectionTabViews.get(i);
            boolean sel = (i == clamped);
            tv.setSelected(sel);
            updateTabColors(tv, tv.hasFocus(), sel);
        }
        if (clamped < mSectionTabViews.size()) {
            final View selectedTab = mSectionTabViews.get(clamped);
            selectedTab.post(() -> scrollTabIntoView(selectedTab));
        }

        stopHeroPreview(true);
        configureContentModeForSection(mActiveSection);

        if (mActiveSection.getType() == BrowseSection.TYPE_ERROR && mActiveSection.getData() instanceof ErrorFragmentData) {
            showError((ErrorFragmentData) mActiveSection.getData());
        } else if (mBrowsePresenter != null) {
            mBrowsePresenter.onSectionFocused(mActiveSection.getId());
        }
    }

    private void configureContentModeForSection(@NonNull BrowseSection section) {
        mErrorContainer.setVisibility(View.GONE);
        mRowsRecycler.setVisibility(View.GONE);
        mGridRecycler.setVisibility(View.GONE);
        mSettingsRecycler.setVisibility(View.GONE);
        mChannelStripRecycler.setVisibility(View.GONE);
        if (mSettingsHeader != null) {
            mSettingsHeader.setVisibility(View.GONE);
        }

        mRowsAdapter.clear();
        mGridAdapter.clear();
        mSettingsAdapter.clear();
        mChannelStripAdapter.clear();
        mLastFocusedRowIndex = 0;

        // Clear stale hero metadata & dim old backdrop when entering a fresh section
        clearHeroMetadata();

        boolean isShorts = section.getType() == BrowseSection.TYPE_SHORTS_GRID;
        mGridAdapter.setShortsMode(isShorts);

        switch (section.getType()) {
            case BrowseSection.TYPE_ROW:
                mHeroInfoContainer.setVisibility(View.VISIBLE);
                setHeroCondensed(false);
                mRowsRecycler.setVisibility(View.VISIBLE);
                break;
            case BrowseSection.TYPE_GRID:
                mHeroInfoContainer.setVisibility(View.VISIBLE);
                setHeroCondensed(false);
                mGridLayoutManager.setSpanCount(GRID_COLS_STANDARD);
                mGridRecycler.setVisibility(View.VISIBLE);
                break;
            case BrowseSection.TYPE_SHORTS_GRID:
                mHeroInfoContainer.setVisibility(View.VISIBLE);
                setHeroCondensed(true);
                mGridLayoutManager.setSpanCount(GRID_COLS_SHORTS);
                mGridRecycler.setVisibility(View.VISIBLE);
                break;
            case BrowseSection.TYPE_MULTI_GRID:
                mHeroInfoContainer.setVisibility(View.VISIBLE);
                setHeroCondensed(true);
                mGridLayoutManager.setSpanCount(GRID_COLS_STANDARD);
                mChannelStripRecycler.setVisibility(View.VISIBLE);
                mGridRecycler.setVisibility(View.VISIBLE);
                break;
            case BrowseSection.TYPE_SETTINGS_GRID:
                clearHeroBackdrop();
                mHeroInfoContainer.setVisibility(View.GONE);
                updateSettingsHeaderBadge();
                if (mSettingsHeader != null) {
                    mSettingsHeader.setVisibility(View.VISIBLE);
                }
                mSettingsRecycler.setVisibility(View.VISIBLE);
                break;
            case BrowseSection.TYPE_ERROR:
                clearHeroBackdrop();
                mHeroInfoContainer.setVisibility(View.GONE);
                mErrorContainer.setVisibility(View.VISIBLE);
                break;
        }

        // Silky Apple tvOS section transition: subtle upward glide + fade when switching feeds
        if (mContentHost != null && getContext() != null) {
            float shiftPx = 18f * getResources().getDisplayMetrics().density;
            mContentHost.animate().cancel();
            mContentHost.setAlpha(0f);
            mContentHost.setTranslationY(shiftPx);
            mContentHost.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(240)
                    .setInterpolator(APPLE_EASE_OUT)
                    .start();
        }
    }

    private void updateSettingsHeaderBadge() {
        if (mSettingsModeBadge == null || getContext() == null) {
            return;
        }
        boolean devMode = GeneralData.instance(getContext()).isDeveloperModeEnabled();
        if (devMode) {
            mSettingsModeBadge.setText("Developer Mode");
            mSettingsModeBadge.setBackgroundResource(R.drawable.bg_cinema_pill_emerald);
            mSettingsModeBadge.setTextColor(ContextCompat.getColor(getContext(), R.color.cinema_emerald_soft));
        } else {
            mSettingsModeBadge.setText("Simple Mode");
            mSettingsModeBadge.setBackgroundResource(R.drawable.bg_cinema_hud_status_pill);
            mSettingsModeBadge.setTextColor(ContextCompat.getColor(getContext(), R.color.cinema_text_secondary));
        }
    }

    private int getActiveGridCols() {
        return mGridLayoutManager != null ? mGridLayoutManager.getSpanCount() : GRID_COLS_STANDARD;
    }

    private void handleSettingClick(@NonNull SettingsItem item) {
        if (getContext() == null) {
            return;
        }
        String password = GeneralData.instance(getContext()).getSettingsPassword();
        if (password == null) {
            item.onClick.run();
        } else {
            SimpleEditDialog.showPassword(
                    getContext(),
                    getContext().getString(R.string.enter_settings_password),
                    null,
                    newValue -> {
                        if (Utils.passwordMatch(password, newValue)) {
                            item.onClick.run();
                            return true;
                        }
                        return false;
                    }
            );
        }
    }

    private void updateAccountPill() {
        if (getContext() == null || mAccountName == null) {
            return;
        }
        Account account = MediaServiceManager.instance().getSelectedAccount();
        if (account != null && !TextUtils.isEmpty(account.getName())) {
            String name = account.getName().trim();
            mAccountName.setText(name);
            mAccountInitial.setText(name.substring(0, 1).toUpperCase());
            if (mAccountGuestIcon != null) {
                mAccountGuestIcon.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(account.getAvatarImageUrl())) {
                mAccountAvatar.setVisibility(View.VISIBLE);
                mAccountInitial.setVisibility(View.GONE);
                Glide.with(getContext().getApplicationContext())
                        .load(account.getAvatarImageUrl())
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .centerCrop()
                        .into(mAccountAvatar);
            } else {
                mAccountAvatar.setVisibility(View.GONE);
                mAccountInitial.setVisibility(View.VISIBLE);
            }
        } else {
            mAccountName.setText(R.string.cinema_sign_in);
            mAccountAvatar.setVisibility(View.GONE);
            mAccountInitial.setVisibility(View.GONE);
            if (mAccountGuestIcon != null) {
                mAccountGuestIcon.setVisibility(View.VISIBLE);
                int iconColor = ContextCompat.getColor(getContext(),
                        (mAccountBtn != null && mAccountBtn.hasFocus())
                                ? R.color.cinema_text_dark : R.color.cinema_text_primary);
                mAccountGuestIcon.setImageTintList(ColorStateList.valueOf(iconColor));
            }
        }
    }

    private void updateSponsorBlockBadge() {
        // SponsorBlock indicator intentionally removed from top bar for ultra-clean Apple TV minimalism
    }

    private void updatePipPill() {
        if (getContext() == null || mPipPill == null) {
            return;
        }
        boolean runningBg = PlaybackPresenter.instance(getContext()).isRunningInBackground();
        mPipPill.setVisibility(runningBg ? View.VISIBLE : View.GONE);
        wireTopBarFocusChain();
    }

    // -------------------------------------------------------------------------
    // BrowseView INTERFACE IMPLEMENTATION
    // -------------------------------------------------------------------------

    @Override
    public void addSection(int index, BrowseSection section) {
        if (section == null) {
            return;
        }
        mPresenterIndexToSection.put(index, section);
        int id = section.getId();
        // Store Settings section for our dedicated top-right gear icon pill
        if (id == MediaGroup.TYPE_SETTINGS || section.getType() == BrowseSection.TYPE_SETTINGS_GRID) {
            mSettingsSection = section;
            return;
        }
        // Keep the floating top Liquid Glass bar ultra-clean: Home, Shorts, and History ONLY
        if (id != MediaGroup.TYPE_HOME && id != MediaGroup.TYPE_SHORTS && id != MediaGroup.TYPE_HISTORY) {
            return;
        }
        for (int i = 0; i < mSections.size(); i++) {
            if (mSections.get(i).getId() == id) {
                mSections.set(i, section);
                rebuildNavTabs();
                return;
            }
        }
        // Guarantee consistent Apple TV order: Home -> Shorts -> History
        int desiredOrder = (id == MediaGroup.TYPE_HOME) ? 0 : (id == MediaGroup.TYPE_SHORTS ? 1 : 2);
        int insertAt = 0;
        for (int i = 0; i < mSections.size(); i++) {
            int curId = mSections.get(i).getId();
            int curOrder = (curId == MediaGroup.TYPE_HOME) ? 0 : (curId == MediaGroup.TYPE_SHORTS ? 1 : 2);
            if (curOrder < desiredOrder) {
                insertAt = i + 1;
            }
        }
        mSections.add(insertAt, section);
        rebuildNavTabs();
    }

    @Override
    public void removeSection(BrowseSection category) {
        if (category == null) {
            return;
        }
        for (int i = 0; i < mSections.size(); i++) {
            if (mSections.get(i).getId() == category.getId()) {
                mSections.remove(i);
                break;
            }
        }
        if (mSelectedSectionIndex >= mSections.size()) {
            mSelectedSectionIndex = Math.max(0, mSections.size() - 1);
        }
        rebuildNavTabs();
    }

    @Override
    public void removeAllSections() {
        mPresenterIndexToSection.clear();
        mSections.clear();
        mSelectedSectionIndex = 0;
        rebuildNavTabs();
    }

    @Override
    public void selectSection(int index, boolean focusOnContent) {
        if (index < 0 || mSections.isEmpty()) {
            return;
        }
        BrowseSection mapped = mPresenterIndexToSection.get(index);
        if (mapped != null) {
            int id = mapped.getId();
            if (id == MediaGroup.TYPE_SETTINGS || mapped.getType() == BrowseSection.TYPE_SETTINGS_GRID) {
                openSettingsSection(focusOnContent);
                return;
            }
            for (int i = 0; i < mSections.size(); i++) {
                if (mSections.get(i).getId() == id) {
                    selectSectionInternal(i, focusOnContent);
                    return;
                }
            }
            // Hidden section (e.g. restored from previous state) -> fall back cleanly to Home
            selectSectionInternal(0, focusOnContent);
            return;
        }
        selectSectionInternal(index, focusOnContent);
    }

    @Override
    public void updateSection(VideoGroup group) {
        if (group == null || mActiveSection == null) {
            return;
        }
        mErrorContainer.setVisibility(View.GONE);

        int type = mActiveSection.getType();
        if (type == BrowseSection.TYPE_ROW) {
            mRowsRecycler.setVisibility(View.VISIBLE);
            boolean wasEmpty = mRowsAdapter.isEmpty();
            mRowsAdapter.update(group);
            if (wasEmpty && !mRowsAdapter.isEmpty()) {
                Video first = mRowsAdapter.getFirstVideo();
                if (first != null && mHeroVideo == null) {
                    updateHeroMetadata(first);
                    crossfadeHeroBackdrop(!TextUtils.isEmpty(first.bgImageUrl) ? first.bgImageUrl : first.cardImageUrl);
                }
                if (mPendingFocusOnContent) {
                    mPendingFocusOnContent = false;
                    mMainHandler.post(this::focusOnContent);
                }
            }
        } else if (type == BrowseSection.TYPE_MULTI_GRID) {
            if (group.getPosition() == 0) {
                mChannelStripRecycler.setVisibility(View.VISIBLE);
                if (group.getAction() == VideoGroup.ACTION_REPLACE) {
                    mChannelStripAdapter.setChannels(group.getVideos());
                } else {
                    mChannelStripAdapter.appendChannels(group.getVideos());
                }
                if (mPendingFocusOnContent && !mChannelStripAdapter.isEmpty()) {
                    mPendingFocusOnContent = false;
                    mMainHandler.post(() -> mChannelStripRecycler.requestFocus());
                }
            } else {
                applyGridUpdate(group);
            }
        } else {
            mGridRecycler.setVisibility(View.VISIBLE);
            boolean wasEmpty = mGridAdapter.getItemCount() == 0;
            applyGridUpdate(group);
            if (wasEmpty && mGridAdapter.getItemCount() > 0) {
                Video first = mGridAdapter.getItem(0);
                if (first != null) {
                    updateHeroMetadata(first);
                    crossfadeHeroBackdrop(!TextUtils.isEmpty(first.bgImageUrl) ? first.bgImageUrl : first.cardImageUrl);
                }
                if (mPendingFocusOnContent) {
                    mPendingFocusOnContent = false;
                    mMainHandler.post(this::focusOnContent);
                }
            }
        }
    }

    private void applyGridUpdate(@NonNull VideoGroup group) {
        int action = group.getAction();
        if (action == VideoGroup.ACTION_REPLACE) {
            mGridAdapter.setVideos(group.getVideos());
        } else if (action == VideoGroup.ACTION_REMOVE) {
            mGridAdapter.removeVideos(group.getVideos());
        } else if (action == VideoGroup.ACTION_REMOVE_AUTHOR) {
            mGridAdapter.removeByAuthor(group.getVideos());
        } else if (action == VideoGroup.ACTION_SYNC) {
            mGridAdapter.syncVideos(group.getVideos());
        } else if (action == VideoGroup.ACTION_PREPEND) {
            mGridAdapter.prependVideos(group.getVideos());
        } else {
            mGridAdapter.appendVideos(group.getVideos());
        }
    }

    @Override
    public void updateSection(SettingsGroup group) {
        if (group == null) {
            return;
        }
        mErrorContainer.setVisibility(View.GONE);
        mHeroInfoContainer.setVisibility(View.GONE);
        updateSettingsHeaderBadge();
        if (mSettingsHeader != null) {
            mSettingsHeader.setVisibility(View.VISIBLE);
        }
        mSettingsRecycler.setVisibility(View.VISIBLE);
        mSettingsAdapter.setItems(group.getItems());

        if (mPendingFocusOnContent && !mSettingsAdapter.isEmpty()) {
            mPendingFocusOnContent = false;
            mMainHandler.post(() -> mSettingsRecycler.requestFocus());
        }
    }

    @Override
    public void clearSection(BrowseSection section) {
        stopHeroPreview(true);
        mHeroVideo = null;
        if (mRowsAdapter != null) {
            mRowsAdapter.clear();
        }
        if (mGridAdapter != null) {
            mGridAdapter.clear();
        }
        if (mSettingsAdapter != null) {
            mSettingsAdapter.clear();
        }
        if (mChannelStripAdapter != null) {
            mChannelStripAdapter.clear();
        }
    }

    @Override
    public void selectSectionItem(int index) {
        if (index < 0 || mActiveSection == null) {
            return;
        }
        if (mActiveSection.getType() == BrowseSection.TYPE_ROW && mRowsRecycler != null) {
            mRowsRecycler.scrollToPosition(index);
        } else if (mGridRecycler != null) {
            mGridRecycler.scrollToPosition(index);
        }
    }

    @Override
    public void selectSectionItem(Video item) {
        if (item == null || mGridAdapter == null) {
            return;
        }
        int idx = mGridAdapter.indexOf(item);
        if (idx >= 0 && mGridRecycler != null) {
            mGridRecycler.scrollToPosition(idx);
        }
    }

    @Override
    public void showError(ErrorFragmentData data) {
        stopHeroPreview(true);
        clearHeroBackdrop();
        if (data == null || mErrorContainer == null) {
            return;
        }
        mRowsRecycler.setVisibility(View.GONE);
        mGridRecycler.setVisibility(View.GONE);
        mSettingsRecycler.setVisibility(View.GONE);
        mChannelStripRecycler.setVisibility(View.GONE);
        mHeroInfoContainer.setVisibility(View.GONE);

        mErrorMessage.setText(data.getMessage() != null ? data.getMessage() : "");
        String actionText = data.getActionText();
        if (!TextUtils.isEmpty(actionText)) {
            mErrorAction.setText(actionText);
            mErrorAction.setVisibility(View.VISIBLE);
            mErrorAction.setOnClickListener(v -> data.onAction());
        } else {
            mErrorAction.setText("Retry");
            mErrorAction.setVisibility(View.VISIBLE);
            mErrorAction.setOnClickListener(v -> {
                if (mBrowsePresenter != null) {
                    mBrowsePresenter.refresh();
                }
            });
        }
        mErrorContainer.setVisibility(View.VISIBLE);
        if (mErrorAction.getVisibility() == View.VISIBLE) {
            mPendingFocusOnContent = false;
            mMainHandler.post(() -> mErrorAction.requestFocus());
        }
    }

    @Override
    public void showProgressBar(boolean show) {
        mMainHandler.post(() -> {
            if (mLoadingView != null) {
                mLoadingView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public boolean isProgressBarShowing() {
        return mLoadingView != null && mLoadingView.getVisibility() == View.VISIBLE;
    }

    @Override
    public void focusOnContent() {
        if (mActiveSection == null) {
            return;
        }
        if (mErrorContainer.getVisibility() == View.VISIBLE && mErrorAction.getVisibility() == View.VISIBLE) {
            mErrorAction.requestFocus();
            return;
        }
        switch (mActiveSection.getType()) {
            case BrowseSection.TYPE_ROW:
                mRowsRecycler.requestFocus();
                break;
            case BrowseSection.TYPE_SETTINGS_GRID:
                mSettingsRecycler.requestFocus();
                break;
            case BrowseSection.TYPE_MULTI_GRID:
                if (!mChannelStripAdapter.isEmpty()) {
                    mChannelStripRecycler.requestFocus();
                } else {
                    mGridRecycler.requestFocus();
                }
                break;
            default:
                mGridRecycler.requestFocus();
                break;
        }
    }

    @Override
    public boolean isEmpty() {
        if (mActiveSection == null) {
            return true;
        }
        switch (mActiveSection.getType()) {
            case BrowseSection.TYPE_ROW:
                return mRowsAdapter == null || mRowsAdapter.isEmpty();
            case BrowseSection.TYPE_SETTINGS_GRID:
                return mSettingsAdapter == null || mSettingsAdapter.isEmpty();
            case BrowseSection.TYPE_MULTI_GRID:
                return (mChannelStripAdapter == null || mChannelStripAdapter.isEmpty())
                        && (mGridAdapter == null || mGridAdapter.getItemCount() == 0);
            default:
                return mGridAdapter == null || mGridAdapter.getItemCount() == 0;
        }
    }

    @Override
    public void updateBadge() {
        updateAccountPill();
        updateSponsorBlockBadge();
        updatePipPill();
    }

    @Override
    public void onAccountChanged(Account account) {
        mMainHandler.post(this::updateAccountPill);
    }

    // -------------------------------------------------------------------------
    // LIFECYCLE
    // -------------------------------------------------------------------------

    @Override
    public void onResume() {
        super.onResume();
        updateAccountPill();
        updateSponsorBlockBadge();
        updatePipPill();
        if (!mIsFragmentCreated && mBrowsePresenter != null) {
            mBrowsePresenter.onViewResumed();
        }
        mIsFragmentCreated = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        stopHeroPreview(true);
        if (!mIsFragmentCreated && mBrowsePresenter != null) {
            mBrowsePresenter.onViewPaused();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        stopHeroPreview(true);
    }

    @Override
    public void onDestroyView() {
        stopHeroPreview(true);
        mMainHandler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        MediaServiceManager.instance().removeAccountListener(this);
        if (mBrowsePresenter != null) {
            mBrowsePresenter.onViewDestroyed();
        }
        super.onDestroy();
    }
}
