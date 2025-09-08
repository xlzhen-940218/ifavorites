package com.xlzhen.ifavorites.viewmodel;

import android.view.View;

import androidx.lifecycle.MutableLiveData;

import com.xlzhen.ifavorites.LoginActivity;
import com.xlzhen.mvvm.binding.base.BaseActivityViewModel;

public class LoginActivityViewModel extends BaseActivityViewModel<LoginActivity> {
    public MutableLiveData<String> email = new MutableLiveData<>("");
    public MutableLiveData<String> password = new MutableLiveData<>("");
    public MutableLiveData<Boolean> loginOrRegister = new MutableLiveData<>(true);
    public LoginActivityViewModel(LoginActivity activity) {
        super(activity);
    }

    @Override
    public void onResume() {

    }

    @Override
    public void backPage(View view) {
        activity.finish();
    }

    public void changeLoginRegister(View view){
        loginOrRegister.postValue(!loginOrRegister.getValue());
    }

    public void loginClick(View view){
        activity.loginClick();
    }
}
