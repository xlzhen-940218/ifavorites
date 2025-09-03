package com.xlzhen.mvvm.binding.base;


import android.content.Context;

import androidx.databinding.ViewDataBinding;

import com.xlzhen.mvvm.adapter.SimpleViewBindingAdapter;


public abstract class BaseAdapterViewModel <K extends SimpleViewBindingAdapter,T extends ViewDataBinding,V extends BaseViewBindingModel> {
    public K adapter;

    public BaseAdapterViewModel(K adapter) {
        this.adapter = adapter;
    }

    public abstract void updateUI(Context context, T binding, V item);
}
