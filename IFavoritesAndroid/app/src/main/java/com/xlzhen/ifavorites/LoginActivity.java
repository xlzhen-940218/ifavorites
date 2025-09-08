package com.xlzhen.ifavorites;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.xlzhen.ifavorites.api.LoginRegisterService;
import com.xlzhen.ifavorites.databinding.ActivityLoginBinding;
import com.xlzhen.ifavorites.model.UserInfo;
import com.xlzhen.ifavorites.viewmodel.LoginActivityViewModel;
import com.xlzhen.mvvm.activity.BaseActivity;
import com.xlzhen.mvvm.storage.StorageUtils;

public class LoginActivity extends BaseActivity<ActivityLoginBinding, LoginActivityViewModel> {

    public void loginClick() {
        String email = model.email.getValue();
        String password = model.password.getValue();
        boolean loginOrRegister = model.loginOrRegister.getValue();
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "请输入邮箱", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password == null || password.isEmpty()) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
            return;
        }
        LoginRegisterService.loginRegisterAsync(email, password, loginOrRegister).thenAccept(userId -> {
            new Handler(Looper.getMainLooper()).post(() -> {
                assert userId != null;
                if (!userId.isEmpty()) {
                    Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
                    StorageUtils.saveData(this, "userInfo", new UserInfo(email, userId));
                    finish();
                } else {
                    Toast.makeText(this, "登录失败", Toast.LENGTH_SHORT).show();
                }
            });
        }).exceptionally(e -> {
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(this, "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
            return null;
        });
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
