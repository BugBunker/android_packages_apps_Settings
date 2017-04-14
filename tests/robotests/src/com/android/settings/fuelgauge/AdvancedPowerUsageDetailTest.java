/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.BatteryStats;
import android.os.Bundle;
import android.view.View;

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.applications.AppHeaderController;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AdvancedPowerUsageDetailTest {
    private static final String APP_LABEL = "app label";
    private static final String SUMMARY = "summary";
    private static final String[] PACKAGE_NAME = {"com.android.app"};
    private static final String USAGE_PERCENT = "16";
    private static final int ICON_ID = 123;
    private static final int UID = 1;
    private static final long BACKGROUND_TIME_US = 100 * 1000;
    private static final long FOREGROUND_TIME_US = 200 * 1000;
    private static final long BACKGROUND_TIME_MS = 100;
    private static final long FOREGROUND_TIME_MS = 200;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Activity mActivity;
    @Mock
    private AppHeaderController mAppHeaderController;
    @Mock
    private LayoutPreference mHeaderPreference;
    @Mock
    private ApplicationsState mState;
    @Mock
    private ApplicationsState.AppEntry mAppEntry;
    @Mock
    private Bundle mBundle;
    @Mock
    private BatteryEntry mBatteryEntry;
    @Mock
    private BatterySipper mBatterySipper;
    @Mock
    private BatteryStatsHelper mBatteryStatsHelper;
    @Mock
    private BatteryStats.Uid mUid;
    @Mock
    private PackageManager mPackageManager;
    private AdvancedPowerUsageDetail mFragment;
    private FakeFeatureFactory mFeatureFactory;
    private SettingsActivity mTestActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);

        mFragment = spy(new AdvancedPowerUsageDetail());
        doReturn(mContext).when(mFragment).getContext();
        doReturn(mActivity).when(mFragment).getActivity();
        doReturn(SUMMARY).when(mFragment).getString(anyInt());
        doReturn(APP_LABEL).when(mBundle).getString(anyString());
        doReturn(mBundle).when(mFragment).getArguments();

        doReturn(mAppHeaderController).when(mFeatureFactory.applicationFeatureProvider)
                .newAppHeaderController(any(Fragment.class), any(View.class));
        doReturn(mAppHeaderController).when(mAppHeaderController).setButtonActions(anyInt(),
                anyInt());
        doReturn(mAppHeaderController).when(mAppHeaderController).setIcon(any(Drawable.class));
        doReturn(mAppHeaderController).when(mAppHeaderController).setIcon(any(
                ApplicationsState.AppEntry.class));
        doReturn(mAppHeaderController).when(mAppHeaderController).setLabel(anyString());
        doReturn(mAppHeaderController).when(mAppHeaderController).setLabel(any(
                ApplicationsState.AppEntry.class));
        doReturn(mAppHeaderController).when(mAppHeaderController).setSummary(anyString());


        doReturn(UID).when(mBatterySipper).getUid();
        doReturn(APP_LABEL).when(mBatteryEntry).getLabel();
        doReturn(BACKGROUND_TIME_US).when(mUid).getProcessStateTime(
                eq(BatteryStats.Uid.PROCESS_STATE_BACKGROUND), anyLong(), anyInt());
        doReturn(FOREGROUND_TIME_US).when(mUid).getProcessStateTime(
                eq(BatteryStats.Uid.PROCESS_STATE_FOREGROUND), anyLong(), anyInt());
        ReflectionHelpers.setField(mBatteryEntry, "sipper", mBatterySipper);
        mBatteryEntry.iconId = ICON_ID;
        mBatterySipper.uidObj = mUid;

        mFragment.mHeaderPreference = mHeaderPreference;
        mFragment.mState = mState;
        mAppEntry.info = mock(ApplicationInfo.class);

        mTestActivity = spy(new SettingsActivity());
        doReturn(mPackageManager).when(mTestActivity).getPackageManager();

        final ArgumentCaptor<Bundle> captor = ArgumentCaptor.forClass(Bundle.class);

        Answer<Void> callable = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Exception {
                mBundle = captor.getValue();
                return null;
            }
        };
        doAnswer(callable).when(mTestActivity).startPreferencePanelAsUser(any(), anyString(),
                captor.capture(), anyInt(), any(), any());
    }

    @Test
    public void testInitHeader_NoAppEntry_BuildByBundle() {
        mFragment.mAppEntry = null;
        mFragment.initHeader();

        verify(mAppHeaderController).setIcon(any(Drawable.class));
        verify(mAppHeaderController).setLabel(APP_LABEL);
    }

    @Test
    public void testInitHeader_HasAppEntry_BuildByAppEntry() {
        mFragment.mAppEntry = mAppEntry;
        mFragment.initHeader();

        verify(mAppHeaderController).setIcon(mAppEntry);
        verify(mAppHeaderController).setLabel(mAppEntry);
    }

    @Test
    public void testStartBatteryDetailPage_hasBasicData() {
        AdvancedPowerUsageDetail.startBatteryDetailPage(mTestActivity, null, mBatteryStatsHelper, 0,
                mBatteryEntry, USAGE_PERCENT);

        assertThat(mBundle.getInt(AdvancedPowerUsageDetail.EXTRA_UID)).isEqualTo(UID);
        assertThat(mBundle.getLong(AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME)).isEqualTo(
                BACKGROUND_TIME_MS);
        assertThat(mBundle.getLong(AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME)).isEqualTo(
                FOREGROUND_TIME_MS);
        assertThat(mBundle.getString(AdvancedPowerUsageDetail.EXTRA_POWER_USAGE_PERCENT)).isEqualTo(
                USAGE_PERCENT);
    }

    @Test
    public void testStartBatteryDetailPage_NormalApp() {
        mBatterySipper.mPackages = PACKAGE_NAME;
        AdvancedPowerUsageDetail.startBatteryDetailPage(mTestActivity, null, mBatteryStatsHelper, 0,
                mBatteryEntry, USAGE_PERCENT);

        assertThat(mBundle.getString(AdvancedPowerUsageDetail.EXTRA_PACKAGE_NAME)).isEqualTo(
                PACKAGE_NAME[0]);
    }

    @Test
    public void testStartBatteryDetailPage_SystemApp() {
        mBatterySipper.mPackages = null;
        AdvancedPowerUsageDetail.startBatteryDetailPage(mTestActivity, null, mBatteryStatsHelper, 0,
                mBatteryEntry, USAGE_PERCENT);

        assertThat(mBundle.getString(AdvancedPowerUsageDetail.EXTRA_LABEL)).isEqualTo(APP_LABEL);
        assertThat(mBundle.getInt(AdvancedPowerUsageDetail.EXTRA_ICON_ID)).isEqualTo(ICON_ID);
        assertThat(mBundle.getString(AdvancedPowerUsageDetail.EXTRA_PACKAGE_NAME)).isEqualTo(null);
    }
}
