package com.xlzhen.mvvm.binding.viewadapter;

import android.view.View;

import androidx.databinding.BindingAdapter;

public class ViewAdapter {
    @BindingAdapter("selected")
    public static void setSelected(View view, boolean selected){
        view.setSelected(selected);
    }

    @BindingAdapter("visibility")
    public static void setVisibility(View view,int visibility){
        view.setVisibility(visibility);
    }

    @BindingAdapter("enabled")
    public static void setEnabled(View view,boolean enabled){
        view.setEnabled(enabled);
    }

    @BindingAdapter("tag")
    public static void setTag(View view,Object tag){
        view.setTag(tag);
    }
}
