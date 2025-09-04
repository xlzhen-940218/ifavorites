package com.xlzhen.mvvm.activity;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.ViewDataBinding;

import com.xlzhen.mvvm.binding.base.BaseActivityViewModel;


public abstract class BaseActivity<K extends ViewDataBinding, V extends BaseActivityViewModel> extends AppCompatActivity {
    protected K binding;
    protected V model;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(null);
        Log.i(this.getClass().getSimpleName(), "..........onCreate..........");
        EdgeToEdge.enable(this);
        binding = bindingInflate();
        setContentView(binding.getRoot());
        model = bindingModel();
        binding.setLifecycleOwner(this);
        binding.setVariable(getVariableId(), model);
        initData();
    }

    protected abstract int getVariableId();

    protected abstract V bindingModel();

    protected abstract void initData();

    protected abstract K bindingInflate();

    public K getBinding() {
        return binding;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(this.getClass().getSimpleName(), "..........onSaveInstanceState..........");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(this.getClass().getSimpleName(), "..........onRestart..........");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(this.getClass().getSimpleName(), "..........onStart..........");

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(this.getClass().getSimpleName(), "..........onStop..........");

    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.i(this.getClass().getSimpleName(), "..........onPause..........");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(this.getClass().getSimpleName(), "..........onResume..........");
        model.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(this.getClass().getSimpleName(), "..........onDestroy..........");

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        hideSystemUI();
        Log.i(this.getClass().getSimpleName(), "..........onPostResume..........");
    }

    protected void hideSystemUI() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        if (isNotificationBarTextBlack()) {
            window.getDecorView().setSystemUiVisibility(
                    (hideNotification() ? View.SYSTEM_UI_FLAG_FULLSCREEN : View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            window.getDecorView().setSystemUiVisibility(
                    (hideNotification() ? View.SYSTEM_UI_FLAG_FULLSCREEN : View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        hideSystemUI();
    }

    protected boolean hideNotification() {
        return false;
    }

    protected boolean isNotificationBarTextBlack() {
        return true;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.i(this.getClass().getSimpleName(), "..........onLowMemory..........");
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Log.i(this.getClass().getSimpleName(), "..........onTrimMemory..........>" + level);
    }
    private int originOrientation = -1;
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        if(newConfig.orientation!=originOrientation){
            originOrientation = newConfig.orientation;
            binding = bindingInflate();
            setContentView(binding.getRoot());
            model = bindingModel();
            binding.setLifecycleOwner(this);
            binding.setVariable(getVariableId(), model);
            initData();
        }
        super.onConfigurationChanged(newConfig);
    }
}
