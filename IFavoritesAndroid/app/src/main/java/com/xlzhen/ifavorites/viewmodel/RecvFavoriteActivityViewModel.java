package com.xlzhen.ifavorites.viewmodel;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.ListAdapter;

import androidx.lifecycle.MutableLiveData;

import com.xlzhen.ifavorites.RecvFavoriteActivity;
import com.xlzhen.ifavorites.adapter.MenuSpinnerAdapter;
import com.xlzhen.mvvm.binding.base.BaseActivityViewModel;

import java.util.ArrayList;

public class RecvFavoriteActivityViewModel extends BaseActivityViewModel<RecvFavoriteActivity> {
    public MutableLiveData<Boolean> newSubMenu = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> downloadResource = new MutableLiveData<>(false);
    public MenuSpinnerAdapter mainAdapter;
    public MenuSpinnerAdapter subAdapter;
    public MutableLiveData<String> newSubMenuText = new MutableLiveData<>("");
    public MutableLiveData<String> urlText = new MutableLiveData<>("");

    public MutableLiveData<Boolean> downloading = new MutableLiveData<>(false);
    public MutableLiveData<String> progressMessage = new MutableLiveData<>("");
    public MutableLiveData<Integer> successCount = new MutableLiveData<>(0);
    public MutableLiveData<Integer> totalCount = new MutableLiveData<>(0);

    public RecvFavoriteActivityViewModel(RecvFavoriteActivity activity) {
        super(activity);
        mainAdapter = new MenuSpinnerAdapter(activity,new ArrayList<>());
        activity.getBinding().mainMenuSpinner.setAdapter(mainAdapter);
        subAdapter = new MenuSpinnerAdapter(activity,new ArrayList<>());
        activity.getBinding().subMenuSpinner.setAdapter(subAdapter);
    }

    @Override
    public void onResume() {

    }

    @Override
    public void backPage(View view) {
        activity.finish();
    }

    public void favoriteClick(View view){
        activity.favorite();
    }
}
