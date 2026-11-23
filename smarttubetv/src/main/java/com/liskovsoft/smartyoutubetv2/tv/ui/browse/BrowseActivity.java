package com.liskovsoft.smartyoutubetv2.tv.ui.browse;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.common.LeanbackActivity;

import java.lang.reflect.Method;

public class BrowseActivity extends LeanbackActivity {
    private static final String TAG = BrowseActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.fragment_main);
        } catch (NoClassDefFoundError e) {
            // Failed resolution of: Landroidx/lifecycle/ViewTreeLifecycleOwner;
            MessageHelpers.showMessage(this, e.getMessage());
        }
    }

    @Override
    public void onBackPressed() {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_frame);
        if (f != null) {
            try {
                Method m = f.getClass().getMethod("handleBackPress");
                Object handled = m.invoke(f);
                if (Boolean.TRUE.equals(handled)) {
                    return;
                }
            } catch (Exception ignored) {
                // Non-cinema flavors use standard BrowseFragment back behavior
            }
        }
        super.onBackPressed();
    }

    @Override
    protected void initTheme() {
        int browseThemeResId = MainUIData.instance(this).getColorScheme().browseThemeResId;
        if (browseThemeResId > 0) {
            setTheme(browseThemeResId);
        }
    }
}
