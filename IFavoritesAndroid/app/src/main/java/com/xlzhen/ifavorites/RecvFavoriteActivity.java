package com.xlzhen.ifavorites;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayoutMediator;
import com.xlzhen.ifavorites.adapter.ViewPagerAdapter;
import com.xlzhen.ifavorites.api.AddBookmarkUrlService;
import com.xlzhen.ifavorites.api.CreateSubFolderService;
import com.xlzhen.ifavorites.api.Folder;
import com.xlzhen.ifavorites.api.GetProgressService;
import com.xlzhen.ifavorites.api.MainFolderService;
import com.xlzhen.ifavorites.api.Progress;
import com.xlzhen.ifavorites.api.SubFolderService;
import com.xlzhen.ifavorites.databinding.ActivityRecvFavoriteBinding;
import com.xlzhen.ifavorites.dialog.LoadingDialog;
import com.xlzhen.ifavorites.model.UIFolder;
import com.xlzhen.ifavorites.model.UserInfo;
import com.xlzhen.ifavorites.viewmodel.RecvFavoriteActivityViewModel;
import com.xlzhen.mvvm.activity.BaseActivity;
import com.xlzhen.mvvm.storage.StorageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecvFavoriteActivity extends BaseActivity<ActivityRecvFavoriteBinding, RecvFavoriteActivityViewModel> {
    private String selectedMainFolderId;
    private String selectedSubFolderId;
    private LoadingDialog loadingDialog;

    @Override
    protected int getVariableId() {
        return BR.recvFavorite;
    }

    @Override
    protected RecvFavoriteActivityViewModel bindingModel() {
        return new RecvFavoriteActivityViewModel(this);
    }

    private void getSubFolders(String folderId) {

        selectedMainFolderId = folderId;
        UserInfo userInfo = StorageUtils.getData(this, "userInfo", UserInfo.class);

        if (userInfo == null)
            return;
        // 调用封装好的 Kotlin 静态方法
        CompletableFuture<List<Folder>> future = SubFolderService.loadSubFoldersAsync(String.format("Bearer %s", userInfo.getUserId()), folderId);

        future.thenAccept(subFolders -> {
            // 在 CompletableFuture 的默认线程池中执行
            // 切换到主线程进行 UI 更新
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!subFolders.isEmpty()) {
                    List<UIFolder> uiFolders = new ArrayList<>();
                    for (Folder f : subFolders) {
                        uiFolders.add(new UIFolder(f.getId(), f.getName(), f.getSelected()));
                    }
                    model.subAdapter.setData(uiFolders);
                }
            });
        }).exceptionally(e -> {
            // 在 CompletableFuture 的默认线程池中处理异常
            // 切换到主线程显示错误信息
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(this, getString(R.string.network_error) + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
            return null;
        });
    }

    private void getMainFolders() {
        UserInfo userInfo = StorageUtils.getData(this, "userInfo", UserInfo.class);
        if (userInfo == null)
            return;
        // 调用封装好的 Kotlin 静态方法
        CompletableFuture<List<Folder>> future = MainFolderService.loadMainFoldersAsync(String.format("Bearer %s", userInfo.getUserId()));

        future.thenAccept(mainFolders -> {
            // 在 CompletableFuture 的默认线程池中执行
            // 切换到主线程进行 UI 更新
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!mainFolders.isEmpty() && mainFolders.size() >= 6) {
                    List<UIFolder> uiFolders = new ArrayList<>();
                    for (Folder f : mainFolders) {
                        uiFolders.add(new UIFolder(f.getId(), f.getName(), f.getSelected()));
                    }
                    model.mainAdapter.setData(uiFolders);
                } else {
                    Toast.makeText(this, R.string.main_folders_load_failed, Toast.LENGTH_SHORT).show();
                }
            });
        }).exceptionally(e -> {
            // 在 CompletableFuture 的默认线程池中处理异常
            // 切换到主线程显示错误信息
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(this, getString(R.string.network_error) + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
            return null;
        });
    }

    @Override
    protected void initData() {
        getMainFolders();
        binding.mainMenuSpinner.setOnItemClickListener((adapterView, view, position, l) -> getSubFolders(model.mainAdapter.getItem(position).getUId()));
        binding.subMenuSpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectedSubFolderId = model.subAdapter.getItem(i).getUId();
            }
        });
        // 获取启动此 Activity 的 Intent
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        // 确保 Intent 类型是 ACTION_SEND 且 MIME 类型是 text/plain
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                // 从 Intent 中获取文本内容
                handleSharedText(intent);
            }
        }
    }

    private void handleSharedText(Intent intent) {
        // 获取 Intent 中的额外数据
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            // 定义一个简单的URL正则表达式
            String urlRegex = "(http|https)://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/[^\\s]*)?";
            Pattern pattern = Pattern.compile(urlRegex);
            Matcher matcher = pattern.matcher(sharedText);

            List<String> urls = new ArrayList<>();
            while (matcher.find()) {
                urls.add(matcher.group()); // 提取匹配到的URL
            }
            for (String url : urls) {
                model.urlText.postValue(model.urlText.getValue() + url + "\n");
            }
        }
    }

    @Override
    protected ActivityRecvFavoriteBinding bindingInflate() {
        return ActivityRecvFavoriteBinding.inflate(getLayoutInflater());
    }

    public void getProgressStatus(List<String> taskIds, AtomicInteger atomicInteger) {
        UserInfo userInfo = null;
        try {
            userInfo = StorageUtils.getData(this, "userInfo", UserInfo.class);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
        if (userInfo == null)
            return;
        CompletableFuture<Progress> future = GetProgressService.getProgressAsync(String.format("Bearer %s", userInfo.getUserId()), taskIds.get(atomicInteger.get()));
        future.thenAccept(progress -> {
            // 在 CompletableFuture 的默认线程池中执行
            // 切换到主线程进行 UI 更新
            new Handler(Looper.getMainLooper()).post(() -> {
                model.progressMessage.postValue(progress.getMessage());
                if ("COMPLETED".equals(progress.getStatus()) || "FAILED".equals(progress.getStatus())) {
                    model.successCount.postValue(atomicInteger.get());
                    model.totalCount.postValue(taskIds.size());
                    if (atomicInteger.get() < taskIds.size() - 1) {
                        atomicInteger.addAndGet(1);
                        binding.subMenuSpinner.postDelayed(() -> {
                            getProgressStatus(taskIds, atomicInteger);
                        }, 3000);
                    } else {
                        Toast.makeText(this, R.string.favorite_complete, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.putExtra("mainId", selectedMainFolderId);
                        intent.putExtra("subId", selectedSubFolderId);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    binding.subMenuSpinner.postDelayed(() -> {
                        getProgressStatus(taskIds, atomicInteger);
                    }, 3000);
                }
            });
        }).exceptionally(e -> {
            // 在 CompletableFuture 的默认线程池中处理异常
            // 切换到主线程显示错误信息
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    Toast.makeText(this, getString(R.string.network_error) + e.getMessage(), Toast.LENGTH_SHORT).show();
                } catch (Exception ex) {
                    System.err.println(ex.getMessage());
                }
            });
            return null;
        });
    }

    public void addBookmarkUrlServer(String url, String subFolderId, Boolean downloadResource) {
        UserInfo userInfo = null;
        try {
            userInfo = StorageUtils.getData(this, "userInfo", UserInfo.class);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
        if (userInfo == null)
            return;
        if (loadingDialog == null) {
            loadingDialog = new LoadingDialog(this);
        }
        if (!loadingDialog.isShowing()) {
            loadingDialog.show();
        }
        url = url.trim();
        // 调用封装好的 Kotlin 静态方法
        CompletableFuture<List<String>> future = AddBookmarkUrlService.addBookmarkUrlAsync(String.format("Bearer %s", userInfo.getUserId()), subFolderId, url, downloadResource);

        future.thenAccept(taskIds -> {
            // 在 CompletableFuture 的默认线程池中执行
            // 切换到主线程进行 UI 更新
            new Handler(Looper.getMainLooper()).post(() -> {
                loadingDialog.dismiss();
                if (!taskIds.isEmpty()) {
                    //getBookmarks(currentFolderId);
                    //在屏幕左下方放一个textview记录正在进行的任务和已完成的任务
                    model.downloading.postValue(true);
                    model.successCount.postValue(0);
                    model.totalCount.postValue(taskIds.size());
                    AtomicInteger atomicInteger = new AtomicInteger(0);

                    getProgressStatus(taskIds, atomicInteger);
                }
            });
        }).exceptionally(e -> {
            // 在 CompletableFuture 的默认线程池中处理异常
            // 切换到主线程显示错误信息
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    Toast.makeText(this, getString(R.string.network_error) + e.getMessage(), Toast.LENGTH_SHORT).show();
                } catch (Exception ex) {
                    System.err.println(ex.getMessage());
                }
                loadingDialog.dismiss();
            });
            return null;
        });
    }

    private void createSubFolder(String subName, String mainFolderId) {
        // 调用封装好的 Kotlin 静态方法
        UserInfo userInfo = StorageUtils.getData(this, "userInfo", UserInfo.class);


        if (userInfo == null)
            return;
        CompletableFuture<String> future = CreateSubFolderService.createSubFoldersAsync(String.format("Bearer %s", userInfo.getUserId()), mainFolderId, subName, userInfo.getUserId());

        future.thenAccept(folderId -> {
            // 在 CompletableFuture 的默认线程池中执行
            // 切换到主线程进行 UI 更新
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!folderId.isEmpty()) {
                    //继续添加url任务
                    selectedSubFolderId = folderId;
                    addBookmarkUrlServer(model.urlText.getValue(), selectedSubFolderId, model.downloadResource.getValue());
                }
            });
        }).exceptionally(e -> {
            // 在 CompletableFuture 的默认线程池中处理异常
            // 切换到主线程显示错误信息
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    Toast.makeText(this, getString(R.string.network_error) + e.getMessage(), Toast.LENGTH_SHORT).show();
                } catch (Exception ex) {
                    System.err.println(ex.getMessage());
                }
            });
            return null;
        });
    }

    public void favorite() {
        if (model.newSubMenu.getValue()) {
            if (model.newSubMenuText.getValue().isEmpty()) {
                Toast.makeText(this, R.string.sub_menu_cannot_empty, Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, getString(R.string.new_sub_menu) + model.newSubMenuText.getValue(), Toast.LENGTH_SHORT).show();
            createSubFolder(model.newSubMenuText.getValue(), selectedMainFolderId);
        } else {
            addBookmarkUrlServer(model.urlText.getValue(), selectedSubFolderId, model.downloadResource.getValue());
        }
    }
}
