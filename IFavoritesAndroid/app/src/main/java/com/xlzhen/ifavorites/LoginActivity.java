package com.xlzhen.ifavorites;

import com.xlzhen.ifavorites.databinding.ActivityLoginBinding;
import com.xlzhen.ifavorites.viewmodel.LoginActivityViewModel;
import com.xlzhen.mvvm.activity.BaseActivity;

public class LoginActivity extends BaseActivity<ActivityLoginBinding, LoginActivityViewModel> {

    public void loginClick() {

    }

    @Override
    protected int getVariableId() {
        return BR.loginActivity;
    }

    @Override
    protected LoginActivityViewModel bindingModel() {
        return new LoginActivityViewModel(this);
    }

    @Override
    protected void initData() {

    }

    @Override
    protected ActivityLoginBinding bindingInflate() {
        return ActivityLoginBinding.inflate(getLayoutInflater());
    }
}
