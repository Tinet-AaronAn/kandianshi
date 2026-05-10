package com.cantv;

import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;

import com.cantv.R;
import com.cantv.ui.MainActivity;

/**
 * UI 仪器测试 - 验证主界面基本元素和交互
 *
 * 注意：运行前需确保设备已授予存储权限
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityUiTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testAppLaunches() {
        // App 能启动不崩溃
        onView(withId(R.id.tv_title))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testTitleDisplayed() {
        onView(withId(R.id.tv_title))
                .check(matches(withText(containsString("看电视"))));
    }

    @Test
    public void testEmptyStateOrGridDisplayed() {
        // 要么显示空提示，要么显示网格列表
        // 二者应该有且只有一个可见
        try {
            onView(withId(R.id.tv_empty)).check(matches(isDisplayed()));
        } catch (Exception e) {
            onView(withId(R.id.grid_series)).check(matches(isDisplayed()));
        }
    }
}
