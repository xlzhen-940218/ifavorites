package com.xlzhen.mvvm.binding.base;


import android.view.View;

import com.xlzhen.mvvm.activity.BaseActivity;

public abstract class BaseActivityViewModel<T extends BaseActivity> extends BaseUIViewModel {
    protected T activity;

    public BaseActivityViewModel(T activity) {
        this.activity = activity;
    }

    public abstract void onResume();

    public abstract void backPage(View view);
}

