package com.xlzhen.ifavorites.viewmodel;

import android.view.View;

import com.xlzhen.ifavorites.VideoPlayerActivity;
import com.xlzhen.mvvm.binding.base.BaseActivityViewModel;

public class VideoPlayerActivityViewModel extends BaseActivityViewModel<VideoPlayerActivity> {
    public VideoPlayerActivityViewModel(VideoPlayerActivity activity) {
        super(activity);
    }

    @Override
    public void onResume() {

    }

    @Override
    public void backPage(View view) {
        activity.finish();
    }
}
