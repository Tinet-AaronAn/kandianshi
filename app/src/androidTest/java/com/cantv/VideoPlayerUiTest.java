package com.cantv;

import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import com.cantv.R;
import com.cantv.player.VideoPlayerActivity;

/**
 * 视频播放器 UI 测试 - 基本界面验证
 */
@RunWith(AndroidJUnit4.class)
public class VideoPlayerUiTest {

    @Rule
    public ActivityScenarioRule<VideoPlayerActivity> activityRule =
            new ActivityScenarioRule<>(
                    new Intent(
                            InstrumentationRegistry.getInstrumentation().getTargetContext(),
                            VideoPlayerActivity.class
                    ).putExtra("video_path", "/sdcard/看电视/_nonexistent_/test.mp4")
                     .putExtra("series_name", "测试剧")
                     .putExtra("episode_title", "第1集")
            );

    @Test
    public void testPlayerLaunches() {
        // 播放器界面根布局应该显示
        onView(withId(R.id.player_root)).check(matches(isDisplayed()));
    }
}
