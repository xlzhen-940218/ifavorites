package com.xlzhen.ifavorites.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;

import com.google.android.material.tabs.TabLayoutMediator;
import com.xlzhen.ifavorites.BR;
import com.xlzhen.ifavorites.adapter.ViewPagerAdapter;
import com.xlzhen.ifavorites.api.AddBookmarkUrlService;
import com.xlzhen.ifavorites.api.Bookmark;
import com.xlzhen.ifavorites.api.BookmarkService;
import com.xlzhen.ifavorites.api.CreateSubFolderService;
import com.xlzhen.ifavorites.api.Folder;
import com.xlzhen.ifavorites.api.GetProgressService;
import com.xlzhen.ifavorites.api.Progress;
import com.xlzhen.ifavorites.api.RecoveryTasksUrlService;
import com.xlzhen.ifavorites.api.SubFolderService;
import com.xlzhen.ifavorites.databinding.FragmentSubFolderBinding;
import com.xlzhen.ifavorites.dialog.LoadingDialog;
import com.xlzhen.ifavorites.viewmodel.SubFolderFragmentViewModel;
import com.xlzhen.mvvm.fragment.BaseFragment;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class SubFolderFragment extends BaseFragment<FragmentSubFolderBinding, SubFolderFragmentViewModel> {
    private String currentFolderId;
    private final String userId = "9f51807c-b0db-4d34-b807-c979b199cedc";
    private ViewPagerAdapter viewPagerAdapter;
    private LoadingDialog loadingDialog;

    @Override
    protected void pageLoaded() {

    }

    @Override
    protected int getVariableId() {
        return BR.subfolderFragment;
    }

    @Override
    protected FragmentSubFolderBinding bindingInflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentSubFolderBinding.inflate(getLayoutInflater(), container, false);
    }

    @Override
    protected SubFolderFragmentViewModel bindingModel() {
        return new SubFolderFragmentViewModel(this);
    }

    private void getBookmarks(String folderId) {
        // 调用封装好的 Kotlin 静态方法
        CompletableFuture<List<Bookmark>> future = BookmarkService.loadBookmarkAsync("Bearer 9f51807c-b0db-4d34-b807-c979b199cedc", folderId);

        future.thenAccept(bookmarks -> {
            // 在 CompletableFuture 的默认线程池中执行
            // 切换到主线程进行 UI 更新
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!bookmarks.isEmpty()) {
                    model.setBookmarkData(bookmarks);
                } else {
                    //Toast.makeText(requireActivity(), "主文件夹加载失败", Toast.LENGTH_SHORT).show();
                }
            });
        }).exceptionally(e -> {
            // 在 CompletableFuture 的默认线程池中处理异常
            // 切换到主线程显示错误信息
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(requireActivity(), "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
            return null;
        });
    }

    private void getSubFolders(String folderId) {
        // 调用封装好的 Kotlin 静态方法
        CompletableFuture<List<Folder>> future = SubFolderService.loadSubFoldersAsync("Bearer 9f51807c-b0db-4d34-b807-c979b199cedc", folderId);

        future.thenAccept(subFolders -> {
            // 在 CompletableFuture 的默认线程池中执行
            // 切换到主线程进行 UI 更新
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!subFolders.isEmpty()) {
                    // 创建适配器并设置给 ViewPager2
                    if (viewPagerAdapter == null) {
                        viewPagerAdapter = new ViewPagerAdapter(requireActivity(), subFolders, false);
                        binding.viewPager.setAdapter(viewPagerAdapter);

                        new TabLayoutMediator(binding.subFolderTabLayout, binding.viewPager,
                                (tab, position) -> {
                                    // 在这里设置每个 Tab 的文本，通常与适配器中的数据对应
                                    var adapter = (ViewPagerAdapter) binding.viewPager.getAdapter();
                                    if (adapter != null) {
                                        tab.setText(adapter.getItemText(position));
                                    }
                                }
                        ).attach();
                    } else {
                        viewPagerAdapter.setData(subFolders);
                    }

                } else {
                    //Toast.makeText(requireActivity(), "主文件夹加载失败", Toast.LENGTH_SHORT).show();
                }
            });
        }).exceptionally(e -> {
            // 在 CompletableFuture 的默认线程池中处理异常
            // 切换到主线程显示错误信息
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(requireActivity(), "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
            return null;
        });
    }

    @Override
    protected void initData() {
        if (getArguments() != null) {
            currentFolderId = getArguments().getString("folder_id");
            String folderName = getArguments().getString("folder_name");
            boolean subFolder = getArguments().getBoolean("sub_folder");
            model.subFolder.postValue(subFolder);
            if (currentFolderId == null)
                return;
            if (subFolder) {
                getBookmarks(currentFolderId);
                recoveryTaskServer();
            } else {

                getSubFolders(currentFolderId);
            }
        }
    }

    public static SubFolderFragment newInstance(Folder folder, boolean mainFolder) {
        SubFolderFragment fragment = new SubFolderFragment();
        Bundle args = new Bundle();
        args.putString("folder_id", folder.getId());
        args.putString("folder_name", folder.getName());
        args.putBoolean("sub_folder", !mainFolder);
        fragment.setArguments(args);
        return fragment;
    }

    public void addFolder() {
        var input = new AppCompatEditText(context);
        new AlertDialog.Builder(requireContext())
                .setTitle("新增二级菜单")
                .setView(input)
                .setPositiveButton("确定", (dialogInterface, i) -> {
                    if (Objects.requireNonNull(input.getText()).length() > 0) {
                        String subName = input.getText().toString();
                        createSubFolder(subName);
                    }
                }).setNegativeButton("取消", null).show();
    }

    private void createSubFolder(String subName) {
        // 调用封装好的 Kotlin 静态方法
        CompletableFuture<Boolean> future = CreateSubFolderService.createSubFoldersAsync("Bearer 9f51807c-b0db-4d34-b807-c979b199cedc", currentFolderId, subName, userId);

        future.thenAccept(success -> {
            // 在 CompletableFuture 的默认线程池中执行
            // 切换到主线程进行 UI 更新
            new Handler(Looper.getMainLooper()).post(() -> {
                if (success) {
                    getSubFolders(currentFolderId);
                }
            });
        }).exceptionally(e -> {
            // 在 CompletableFuture 的默认线程池中处理异常
            // 切换到主线程显示错误信息
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(requireActivity(), "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
            return null;
        });
    }

    public void getProgressStatus(List<String> taskIds, AtomicInteger atomicInteger) {
        CompletableFuture<Progress> future = GetProgressService.getProgressAsync("Bearer 9f51807c-b0db-4d34-b807-c979b199cedc", taskIds.get(atomicInteger.get()));
        future.thenAccept(progress -> {
            // 在 CompletableFuture 的默认线程池中执行
            // 切换到主线程进行 UI 更新
            new Handler(Looper.getMainLooper()).post(() -> {
                model.progressMessage.postValue(progress.getMessage());
                if ("COMPLETED".equals(progress.getStatus()) || "FAILED".equals(progress.getStatus())) {
                    atomicInteger.addAndGet(1);
                    model.successCount.postValue(atomicInteger.get());
                    model.totalCount.postValue(taskIds.size());
                    getBookmarks(currentFolderId);
                }
                binding.subFolderTabLayout.postDelayed(()->{
                    getProgressStatus(taskIds, atomicInteger);
                },3000);
            });
        }).exceptionally(e -> {
            // 在 CompletableFuture 的默认线程池中处理异常
            // 切换到主线程显示错误信息
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(requireActivity(), "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
            return null;
        });
    }

    public void recoveryTaskServer(){
        // 调用封装好的 Kotlin 静态方法
        CompletableFuture<List<String>> future = RecoveryTasksUrlService.recoveryTasksUrlAsync("Bearer 9f51807c-b0db-4d34-b807-c979b199cedc", currentFolderId);

        future.thenAccept(taskIds -> {
            // 在 CompletableFuture 的默认线程池中执行
            // 切换到主线程进行 UI 更新
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!taskIds.isEmpty()) {
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
                Toast.makeText(requireActivity(), "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
            return null;
        });
    }

    public void addBookmarkUrlServer(String url) {
        if (loadingDialog == null) {
            loadingDialog = new LoadingDialog(requireActivity());
        }
        if (!loadingDialog.isShowing()) {
            loadingDialog.show();
        }
        // 调用封装好的 Kotlin 静态方法
        CompletableFuture<List<String>> future = AddBookmarkUrlService.addBookmarkUrlAsync("Bearer 9f51807c-b0db-4d34-b807-c979b199cedc", currentFolderId, url);

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
                Toast.makeText(requireActivity(), "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                loadingDialog.dismiss();
            });
            return null;
        });
    }

    public boolean isValidUrl(String urlString) {
        return Patterns.WEB_URL.matcher(urlString).matches();
    }

    public void addBookmarkUrl() {
        var input = new AppCompatEditText(context);
        new AlertDialog.Builder(requireContext())
                .setTitle("新增收藏夹链接")
                .setView(input)
                .setPositiveButton("确定", (dialogInterface, i) -> {
                    if (Objects.requireNonNull(input.getText()).length() > 0) {
                        String url = input.getText().toString();
                        if (isValidUrl(url)) {
                            addBookmarkUrlServer(url);
                        } else {
                            Toast.makeText(requireActivity(), "url不合法！", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).setNegativeButton("取消", null).show();
    }
}
