package com.xlzhen.ifavorites.viewmodel;

import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.xlzhen.ifavorites.MainActivity;
import com.xlzhen.ifavorites.api.Folder;
import com.xlzhen.mvvm.binding.base.BaseActivityViewModel;

import java.util.ArrayList;
import java.util.List;

public class MainActivityViewModel extends BaseActivityViewModel<MainActivity> {

    public MainActivityViewModel(MainActivity activity) {
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
