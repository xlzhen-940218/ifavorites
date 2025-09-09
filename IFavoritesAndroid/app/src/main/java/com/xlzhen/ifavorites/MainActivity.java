package com.xlzhen.ifavorites;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;
import com.xlzhen.ifavorites.adapter.ViewPagerAdapter;
import com.xlzhen.ifavorites.api.Folder;
import com.xlzhen.ifavorites.api.MainFolderService;
import com.xlzhen.ifavorites.api.ServerConfig;
import com.xlzhen.ifavorites.databinding.ActivityMainBinding;
import com.xlzhen.ifavorites.model.ServerUrl;
import com.xlzhen.ifavorites.model.UserInfo;
import com.xlzhen.ifavorites.viewmodel.MainActivityViewModel;
import com.xlzhen.mvvm.activity.BaseActivity;
import com.xlzhen.mvvm.storage.StorageUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MainActivity extends BaseActivity<ActivityMainBinding, MainActivityViewModel> {
    private ViewPagerAdapter viewPagerAdapter;

    @Override
    protected int getVariableId() {
        return BR.main;
    }

    @Override
    protected MainActivityViewModel bindingModel() {
        return new MainActivityViewModel(this);
    }

    @Override
    protected void initData() {
        if (binding.viewPager.getAdapter() != null) {
            return;
        }
        UserInfo userInfo = StorageUtils.getData(this, "userInfo", UserInfo.class);
        if (userInfo == null)
            return;

        ServerUrl server = StorageUtils.getData(this, "server", ServerUrl.class);
        if (server == null)
            return;
        ServerConfig.updateUrl(this, server.getUrl());
        // 调用封装好的 Kotlin 静态方法
        CompletableFuture<List<Folder>> future = MainFolderService.loadMainFoldersAsync(String.format("Bearer %s", userInfo.getUserId()));

        future.thenAccept(mainFolders -> {
            // 在 CompletableFuture 的默认线程池中执行
            // 切换到主线程进行 UI 更新
            new Handler(Looper.getMainLooper()).post(() -> {
                if (mainFolders.size() >= 6) {
                    // 创建适配器并设置给 ViewPager2
                    if (viewPagerAdapter == null) {
                        viewPagerAdapter = new ViewPagerAdapter(MainActivity.this, mainFolders, true);
                        binding.viewPager.setAdapter(viewPagerAdapter);
                        new TabLayoutMediator(binding.mainFolderTabLayout, binding.viewPager,
                                (tab, position) -> {
                                    // 在这里设置每个 Tab 的文本，通常与适配器中的数据对应
                                    var adapter = (ViewPagerAdapter) binding.viewPager.getAdapter();
                                    if (adapter != null) {
                                        tab.setText(adapter.getItemText(position));
                                    }
                                }
                        ).attach();
                    } else {
                        viewPagerAdapter.setData(mainFolders);
                    }
                    if (getIntent().hasExtra("mainId")) {
                        String mainId = getIntent().getStringExtra("mainId");
                        for (int i = 0; i < mainFolders.size(); i++) {
                            if (mainFolders.get(i).getId().equals(mainId)) {
                                binding.viewPager.setCurrentItem(i);
                                break;
                            }
                        }
                    }
                } else {
                    Toast.makeText(MainActivity.this, R.string.main_folders_load_failed, Toast.LENGTH_SHORT).show();
                }
            });
        }).exceptionally(e -> {
            // 在 CompletableFuture 的默认线程池中处理异常
            // 切换到主线程显示错误信息
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(MainActivity.this, getString(R.string.network_error) + e.getMessage(), Toast.LENGTH_SHORT).show();
                if ("retrofit2.HttpException: HTTP 401 UNAUTHORIZED".equals(e.getMessage())) {
                    StorageUtils.removeData(MainActivity.this, "userInfo");
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                }
            });
            return null;
        });
    }

    @Override
    protected ActivityMainBinding bindingInflate() {
        return ActivityMainBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onResume() {
        super.onResume();
        UserInfo userInfo = StorageUtils.getData(this, "userInfo", UserInfo.class);
        ServerUrl server = StorageUtils.getData(this, "server", ServerUrl.class);

        if (userInfo == null || server == null) {
            Toast.makeText(this, R.string.please_first_login, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            initData();
        }
    }
}
