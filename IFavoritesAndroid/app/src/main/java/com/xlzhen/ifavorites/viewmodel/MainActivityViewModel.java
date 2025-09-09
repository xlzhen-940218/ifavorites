package com.xlzhen.ifavorites.viewmodel;

import android.content.res.Configuration;
import android.view.View;

import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.xlzhen.ifavorites.MainActivity;
import com.xlzhen.ifavorites.api.Folder;
import com.xlzhen.mvvm.binding.base.BaseActivityViewModel;

import java.util.ArrayList;
import java.util.List;

public class MainActivityViewModel extends BaseActivityViewModel<MainActivity> {
    public MutableLiveData<Integer> titleTopPadding = new MutableLiveData<>(50);
    public MainActivityViewModel(MainActivity activity) {
        super(activity);
        int dpTop = 10;
        Configuration configuration = activity.getResources().getConfiguration();
        if(configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            dpTop = 50;
        }
        titleTopPadding.postValue((int) ( dpTop * activity.getResources().getDisplayMetrics().density));
    }


    @Override
    public void onResume() {

    }

    @Override
    public void backPage(View view) {
        activity.finish();
    }
}
