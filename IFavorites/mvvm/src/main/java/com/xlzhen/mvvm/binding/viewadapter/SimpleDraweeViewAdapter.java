package com.xlzhen.mvvm.binding.viewadapter;

import androidx.databinding.BindingAdapter;

import com.facebook.drawee.view.SimpleDraweeView;

public class SimpleDraweeViewAdapter {
    @BindingAdapter("imageUri")
    public static void setImageUri(SimpleDraweeView view,String url){
        view.setImageURI(url);
    }
}
