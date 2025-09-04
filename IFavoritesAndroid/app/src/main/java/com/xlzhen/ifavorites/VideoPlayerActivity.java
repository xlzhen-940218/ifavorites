package com.xlzhen.ifavorites;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;

import com.xlzhen.ifavorites.databinding.ActivityVideoPlayerBinding;
import com.xlzhen.ifavorites.viewmodel.VideoPlayerActivityViewModel;
import com.xlzhen.mvvm.activity.BaseActivity;

public class VideoPlayerActivity extends BaseActivity<ActivityVideoPlayerBinding, VideoPlayerActivityViewModel> {
    private ExoPlayer player;
    private long position;

    @Override
    protected int getVariableId() {
        return BR.videoPlayer;
    }

    @Override
    protected VideoPlayerActivityViewModel bindingModel() {
        return new VideoPlayerActivityViewModel(this);
    }

    @Override
    protected void initData() {

    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // 更新当前的 Intent
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri videoUri = intent.getData();
            if (videoUri != null) {
                initializePlayer(videoUri);
            }
        }
    }

    @Override
    protected ActivityVideoPlayerBinding bindingInflate() {
        return ActivityVideoPlayerBinding.inflate(getLayoutInflater());
    }

    private void initializePlayer(Uri videoUri) {
        if (player == null) {
            // 使用 media3 包中的 ExoPlayer
            player = new ExoPlayer.Builder(this).build();
            binding.playerView.setPlayer(player);

            MediaItem mediaItem = MediaItem.fromUri(videoUri);

            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();
            player.seekTo(position);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        handleIntent(getIntent());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // 检查新的屏幕方向
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 当变为横屏时
            // 可以在这里隐藏状态栏、导航栏，进入真正的全屏模式
            // getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (player != null) {
                position = player.getCurrentPosition();
                player.release();
                player = null;
            }
            handleIntent(getIntent());
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // 当变为竖屏时
            // 可以在这里退出全屏
            // getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            if (player != null) {
                position = player.getCurrentPosition();
                player.release();
                player = null;
            }
            handleIntent(getIntent());
        }
    }
}
