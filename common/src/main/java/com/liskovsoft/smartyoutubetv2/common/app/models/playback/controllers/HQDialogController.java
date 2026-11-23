package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.BasePlayerController;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;
import com.liskovsoft.smartyoutubetv2.common.utils.AppDialogUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class HQDialogController extends BasePlayerController {
    private static final String TAG = HQDialogController.class.getSimpleName();
    private static final int VIDEO_FORMATS_ID = 132;
    private static final int AUDIO_FORMATS_ID = 133;
    // NOTE: using map, because same item could be changed time to time
    private final Map<Integer, OptionCategory> mCategories = new LinkedHashMap<>();
    private final Map<Integer, OptionCategory> mCategoriesInt = new LinkedHashMap<>();
    private final Set<Runnable> mHideListeners = new HashSet<>();
    private AppDialogPresenter mAppDialogPresenter;

    @Override
    public void onInit() {
        mAppDialogPresenter = AppDialogPresenter.instance(getContext());
    }

    @Override
    public void onViewResumed() {
        //updateBackgroundPlayback();
    }

    @Override
    public void onButtonClicked(int buttonId, int buttonState) {
        if (buttonId == R.id.lb_control_high_quality) {
            onHighQualityClicked();
        }
    }

    private void onHighQualityClicked() {
        fitVideoIntoDialog();

        // Simple family-friendly YouTube quality picker by default; full fine-grained controls in Developer Mode
        if (!GeneralData.instance(getContext()).isDeveloperModeEnabled()) {
            showSimpleQualityDialog();
            return;
        }

        addQualityCategories();
        addAudioLanguage();
        addPresetsCategory();
        addVideoZoomCategory();
        addNetworkEngine();
        addVideoBufferCategory();
        addAudioDelayCategory();
        addPitchEffectCategory();
        addSleepTimerCategory();
        //addBackgroundPlaybackCategory();

        appendOptions(mCategoriesInt);
        appendOptions(mCategories);

        mAppDialogPresenter.showDialog(getContext().getString(R.string.playback_settings), this::onDialogHide);
    }

    /**
     * Familiar YouTube-style resolution selector (Auto, 4K, 1440p, 1080p, 720p, 480p, 360p)
     * with one crisp entry per resolution — automatically selecting the best codec/fps under the hood.
     */
    private void showSimpleQualityDialog() {
        if (getPlayer() == null) {
            return;
        }
        List<FormatItem> allFormats = getPlayer().getVideoFormats();
        FormatItem currentFormat = getPlayer().getVideoFormat();
        int currentHeight = currentFormat != null ? currentFormat.getHeight() : -1;
        FormatItem savedFormat = getPlayerData().getFormat(FormatItem.TYPE_VIDEO);
        FormatItem defaultDeviceFormat = getPlayerData().getDefaultVideoFormat();
        boolean isAutoSaved = savedFormat == null || savedFormat.isDefault() || savedFormat.getHeight() <= 0
                || (defaultDeviceFormat != null && savedFormat.equals(defaultDeviceFormat));

        // Deduplicate by resolution height (highest first), preferring highest fps & best codec (VP9 > AVC > AV1)
        TreeMap<Integer, FormatItem> bestByHeight = new TreeMap<>(Collections.reverseOrder());
        if (allFormats != null) {
            for (FormatItem item : allFormats) {
                if (item == null || item.isDefault() || item.getHeight() < 240) {
                    continue;
                }
                int bucketHeight = normalizeQualityBucket(item.getHeight());
                FormatItem existing = bestByHeight.get(bucketHeight);
                if (existing == null || isBetterSimpleFormat(item, existing)) {
                    bestByHeight.put(bucketHeight, item);
                }
            }
        }

        List<OptionItem> options = new ArrayList<>();
        String autoLabel = currentHeight > 0
                ? ("Auto (" + normalizeQualityBucket(currentHeight) + "p)")
                : "Auto (Recommended)";
        options.add(UiOptionItem.from(autoLabel, opt -> {
            FormatItem autoTarget = defaultDeviceFormat != null
                    ? defaultDeviceFormat
                    : (bestByHeight.isEmpty() ? FormatItem.VIDEO_FHD_VP9_60 : bestByHeight.firstEntry().getValue());
            applySimpleVideoFormat(autoTarget, autoTarget);
        }, isAutoSaved));

        for (Map.Entry<Integer, FormatItem> entry : bestByHeight.entrySet()) {
            int h = entry.getKey();
            FormatItem target = entry.getValue();
            boolean selected = !isAutoSaved && currentHeight > 0
                    && normalizeQualityBucket(currentHeight) == h;
            String label = formatSimpleHeight(h);
            options.add(UiOptionItem.from(label, opt -> applySimpleVideoFormat(target, target), selected));
        }

        mAppDialogPresenter.appendRadioCategory("Video Quality", options);
        mAppDialogPresenter.showDialog("Quality", this::onDialogHide);
    }

    private void applySimpleVideoFormat(FormatItem playFormat, FormatItem persistFormat) {
        if (getPlayer() == null) {
            return;
        }
        if (playFormat != null) {
            getPlayer().setFormat(playFormat);
        }
        if (persistFormat != null) {
            getPlayerData().setFormat(persistFormat);
        }
        mAppDialogPresenter.closeDialog();
        getPlayer().showOverlay(false);
    }

    private static int normalizeQualityBucket(int height) {
        if (height >= 2000) return 2160;
        if (height >= 1350) return 1440;
        if (height >= 1000) return 1080;
        if (height >= 680) return 720;
        if (height >= 440) return 480;
        if (height >= 320) return 360;
        return 240;
    }

    private static String formatSimpleHeight(int height) {
        int bucket = normalizeQualityBucket(height);
        if (bucket >= 2160) return "2160p (4K)";
        if (bucket == 1440) return "1440p (HD)";
        if (bucket == 1080) return "1080p (Full HD)";
        if (bucket == 720) return "720p (HD)";
        return bucket + "p";
    }

    private static boolean isBetterSimpleFormat(FormatItem candidate, FormatItem existing) {
        float cFps = candidate.getFrameRate();
        float eFps = existing.getFrameRate();
        if (Math.abs(cFps - eFps) > 5f) {
            return cFps > eFps;
        }
        String cTitle = candidate.getTitle() != null ? candidate.getTitle().toString().toLowerCase() : "";
        String eTitle = existing.getTitle() != null ? existing.getTitle().toString().toLowerCase() : "";
        int cScore = cTitle.contains("vp9") ? 3 : (cTitle.contains("avc") ? 2 : 1);
        int eScore = eTitle.contains("vp9") ? 3 : (eTitle.contains("avc") ? 2 : 1);
        return cScore > eScore;
    }

    private void addQualityCategories() {
        if (getPlayer() == null) {
            return;
        }

        List<FormatItem> videoFormats = getPlayer().getVideoFormats();
        String videoFormatsTitle = getContext().getString(R.string.title_video_formats);

        List<FormatItem> audioFormats = getPlayer().getAudioFormats();
        String audioFormatsTitle = getContext().getString(R.string.title_audio_formats);

        addCategoryInt(OptionCategory.from(
                VIDEO_FORMATS_ID,
                OptionCategory.TYPE_RADIO_LIST,
                videoFormatsTitle,
                UiOptionItem.from(videoFormats, this::selectFormatOption, getContext().getString(R.string.option_disabled))));
        addCategoryInt(OptionCategory.from(
                AUDIO_FORMATS_ID,
                OptionCategory.TYPE_RADIO_LIST,
                audioFormatsTitle,
                UiOptionItem.from(audioFormats, this::selectFormatOption, getContext().getString(R.string.option_disabled))));
    }

    private void selectFormatOption(OptionItem option) {
        if (getPlayer() == null) {
            return;
        }

        FormatItem formatItem = UiOptionItem.toFormat(option);
        getPlayer().setFormat(formatItem);
        persistFormat(formatItem);

        if (getPlayerData().getFormat(formatItem.getType()).isPreset()) {
            // Preset currently active. Show warning about format reset.
            MessageHelpers.showMessage(getContext(), R.string.video_preset_enabled);
        }

        if (!getPlayer().containsMedia()) {
            getPlayer().reloadPlayback();
        }

        // Make result easily be spotted by the user
        if (formatItem.getType() == FormatItem.TYPE_VIDEO) {
            getPlayer().showOverlay(false);
        }
    }

    private void persistFormat(FormatItem formatItem) {
        if (formatItem.getType() == FormatItem.TYPE_VIDEO) {
            if (!getPlayerData().getFormat(FormatItem.TYPE_VIDEO).isPreset()) {
                getPlayerData().setFormat(formatItem);
            } else {
                getPlayerData().setTempVideoFormat(formatItem);
            }
        } else {
            getPlayerData().setFormat(formatItem);
        }
    }

    private void addVideoBufferCategory() {
        if (getPlayer() == null) {
            return;
        }
        addCategoryInt(AppDialogUtil.createVideoBufferCategory(getContext(),
                () -> getPlayer().restartEngine()));
    }

    private void addAudioDelayCategory() {
        if (getPlayer() == null) {
            return;
        }
        addCategoryInt(AppDialogUtil.createAudioDelayCategory(getContext(),
                () -> getPlayer().restartEngine()));
    }

    private void addPitchEffectCategory() {
        addCategoryInt(AppDialogUtil.createPitchEffectCategory(getContext()));
    }

    private void addSleepTimerCategory() {
        addCategoryInt(AppDialogUtil.createSleepTimerCategory(getContext()));
    }

    private void addAudioLanguage() {
        if (getPlayer() == null) {
            return;
        }
        addCategoryInt(AppDialogUtil.createAudioLanguageCategory(getContext(),
                () -> getPlayer().restartEngine()));
    }

    private void addNetworkEngine() {
        if (getPlayer() == null) {
            return;
        }
        addCategoryInt(AppDialogUtil.createNetworkEngineCategory(getContext(),
                () -> getPlayer().restartEngine()));
    }

    private void onDialogHide() {
        //updateBackgroundPlayback();

        for (Runnable listener : mHideListeners) {
            listener.run();
        }

        mHideListeners.clear();
        mCategories.clear();
        mCategoriesInt.clear();
    }

    //private void updateBackgroundPlayback() {
    //    ViewManager.instance(getContext()).blockTop(null);
    //
    //    if (getPlayer() != null) {
    //        getPlayer().setBackgroundMode(getPlayerData().getBackgroundMode());
    //    }
    //}

    //private void addBackgroundPlaybackCategory() {
    //    OptionCategory category =
    //            AppDialogUtil.createBackgroundPlaybackCategory(getContext(), getPlayerData(), GeneralData.instance(getContext()), this::updateBackgroundPlayback);
    //
    //    addCategoryInt(category);
    //}

    private void addPresetsCategory() {
        addCategoryInt(AppDialogUtil.createVideoPresetsCategory(
                getContext(), () -> {
                    if (getPlayer() == null) {
                        return;
                    }

                    FormatItem format = getPlayerData().getFormat(FormatItem.TYPE_VIDEO);
                    getPlayer().setFormat(format);

                    if (!getPlayer().containsMedia()) {
                        getPlayer().reloadPlayback();
                    }

                    // Make result easily be spotted by the user
                    getPlayer().showOverlay(false);
                }
        ));
    }

    private void addVideoZoomCategory() {
        if (getPlayer() == null) {
            return;
        }

        addCategoryInt(AppDialogUtil.createVideoZoomCategory(
                getContext(), () -> {
                    getPlayer().setResizeMode(getPlayerData().getResizeMode());
                    getPlayer().setZoomPercents(getPlayerData().getZoomPercents());

                    // Make result easily be spotted by the user
                    getPlayer().showOverlay(false);
                }));
    }

    private void removeCategoryInt(int id) {
        mCategoriesInt.remove(id);
    }

    private void addCategoryInt(OptionCategory category) {
        mCategoriesInt.put(category.id, category);
    }

    public void removeCategory(int id) {
        mCategories.remove(id);
    }

    public void addCategory(OptionCategory category) {
        mCategories.put(category.id, category);
    }

    public void addOnDialogHide(Runnable listener) {
        mHideListeners.add(listener);
    }

    public void removeOnDialogHide(Runnable listener) {
        mHideListeners.remove(listener);
    }

    private void appendOptions(Map<Integer, OptionCategory> categories) {
        for (OptionCategory category : categories.values()) {
            mAppDialogPresenter.appendCategory(category);
        }
    }
}
