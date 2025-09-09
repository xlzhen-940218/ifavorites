package com.xlzhen.ifavorites.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.xlzhen.ifavorites.BR;
import com.xlzhen.ifavorites.R;
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
import com.xlzhen.ifavorites.model.UserInfo;
import com.xlzhen.ifavorites.viewmodel.SubFolderFragmentViewModel;
import com.xlzhen.mvvm.fragment.BaseFragment;
import com.xlzhen.mvvm.storage.StorageUtils;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class SubFolderFragment extends BaseFragment<FragmentSubFolderBinding, SubFolderFragmentViewModel> {
    private String currentFolderId;
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
        UserInfo userInfo = null;
        try {
            userInfo = StorageUtils.getData(requireActivity(), "userInfo", UserInfo.class);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
        if (userInfo == null)
            return;
        // 调用封装好的 Kotlin 静态方法
        CompletableFuture<List<Bookmark>> future = BookmarkService.loadBookmarkAsync(String.format("Bearer %s", userInfo.getUserId()), folderId);

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
                try {
                    Toast.makeText(requireActivity(), "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                } catch (Exception ex) {
                    System.err.println(ex.getMessage());
                }
            });
            return null;
        });
    }

    private void getSubFolders(String folderId) {
        UserInfo userInfo = null;
        try {
            userInfo = StorageUtils.getData(requireActivity(), "userInfo", UserInfo.class);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
        if (userInfo == null)
            return;
        // 调用封装好的 Kotlin 静态方法
        CompletableFuture<List<Folder>> future = SubFolderService.loadSubFoldersAsync(String.format("Bearer %s", userInfo.getUserId()), folderId);

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
                                        String text = adapter.getItemText(position);
                                        if (text.length() > 10) {
                                            text = text.substring(0, 10);
                                            text += "...";
                                        }
                                        tab.setText(text);
                                    }
                                }
                        ).attach();
                    } else {
                        viewPagerAdapter.setData(subFolders);
                    }
                    if (requireActivity().getIntent().hasExtra("subId")) {
                        String subId = requireActivity().getIntent().getStringExtra("subId");
                        for (int i = 0; i < subFolders.size(); i++) {
                            if (subFolders.get(i).getId().equals(subId)) {
                                binding.viewPager.setCurrentItem(i);
                                break;
                            }
                        }
                    }
                } else {
                    //Toast.makeText(requireActivity(), "主文件夹加载失败", Toast.LENGTH_SHORT).show();
                }
            });
        }).exceptionally(e -> {
            // 在 CompletableFuture 的默认线程池中处理异常
            // 切换到主线程显示错误信息
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    Toast.makeText(requireActivity(), "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                } catch (Exception ex) {
                    System.err.println(ex.getMessage());
                }
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
        TextInputLayout textInputLayout = (TextInputLayout) ViewGroup.inflate(requireActivity(), R.layout.alert_edittext_layout, null);
        textInputLayout.setHint("请输入菜单名称");
        //var input = new AppCompatEditText(context);
        new AlertDialog.Builder(requireContext())
                .setTitle("新增二级菜单")
                .setView(textInputLayout)
                .setPositiveButton("确定", (dialogInterface, i) -> {
                    TextInputEditText inputEditText = textInputLayout.findViewById(R.id.text_input_edit);
                    if (Objects.requireNonNull(inputEditText.getText()).length() > 0) {
                        String subName = inputEditText.getText().toString();
                        createSubFolder(subName);
                    }
                }).setNegativeButton("取消", null).show();
    }

    private void createSubFolder(String subName) {
        // 调用封装好的 Kotlin 静态方法
        UserInfo userInfo = null;
        try {
            userInfo = StorageUtils.getData(requireActivity(), "userInfo", UserInfo.class);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
        if (userInfo == null)
            return;
        CompletableFuture<String> future = CreateSubFolderService.createSubFoldersAsync(String.format("Bearer %s", userInfo.getUserId()), currentFolderId, subName, userInfo.getUserId());

        future.thenAccept(folderId -> {
            // 在 CompletableFuture 的默认线程池中执行
            // 切换到主线程进行 UI 更新
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!folderId.isEmpty()) {
                    getSubFolders(currentFolderId);
                }
            });
        }).exceptionally(e -> {
            // 在 CompletableFuture 的默认线程池中处理异常
            // 切换到主线程显示错误信息
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    Toast.makeText(requireActivity(), "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                } catch (Exception ex) {
                    System.err.println(ex.getMessage());
                }
            });
            return null;
        });
    }

    public void getProgressStatus(List<String> taskIds, AtomicInteger atomicInteger) {
        UserInfo userInfo = null;
        try {
            userInfo = StorageUtils.getData(requireActivity(), "userInfo", UserInfo.class);
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
                    if (atomicInteger.get() < taskIds.size() - 1) {
                        atomicInteger.addAndGet(1);
                        binding.subFolderTabLayout.postDelayed(() -> {
                            getProgressStatus(taskIds, atomicInteger);
                        }, 3000);
                    }
                    model.successCount.postValue(atomicInteger.get());
                    model.totalCount.postValue(taskIds.size());
                    getBookmarks(currentFolderId);
                } else {
                    binding.subFolderTabLayout.postDelayed(() -> {
                        getProgressStatus(taskIds, atomicInteger);
                    }, 3000);
                }
            });
        }).exceptionally(e -> {
            // 在 CompletableFuture 的默认线程池中处理异常
            // 切换到主线程显示错误信息
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    Toast.makeText(requireActivity(), "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                } catch (Exception ex) {
                    System.err.println(ex.getMessage());
                }
            });
            return null;
        });
    }

    public void recoveryTaskServer() {
        UserInfo userInfo = null;
        try {
            userInfo = StorageUtils.getData(requireActivity(), "userInfo", UserInfo.class);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
        if (userInfo == null)
            return;
        // 调用封装好的 Kotlin 静态方法
        CompletableFuture<List<String>> future = RecoveryTasksUrlService.recoveryTasksUrlAsync(String.format("Bearer %s", userInfo.getUserId()), currentFolderId);

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
                try {
                    Toast.makeText(requireActivity(), "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                } catch (Exception ex) {
                    System.err.println(ex.getMessage());
                }
            });
            return null;
        });
    }

    public void addBookmarkUrlServer(String url, Boolean downloadResource) {
        UserInfo userInfo = null;
        try {
            userInfo = StorageUtils.getData(requireActivity(), "userInfo", UserInfo.class);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
        if (userInfo == null)
            return;
        if (loadingDialog == null) {
            loadingDialog = new LoadingDialog(requireActivity());
        }
        if (!loadingDialog.isShowing()) {
            loadingDialog.show();
        }
        // 调用封装好的 Kotlin 静态方法
        CompletableFuture<List<String>> future = AddBookmarkUrlService.addBookmarkUrlAsync(String.format("Bearer %s", userInfo.getUserId()), currentFolderId, url, downloadResource);

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
                    Toast.makeText(requireActivity(), "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                } catch (Exception ex) {
                    System.err.println(ex.getMessage());
                }
                loadingDialog.dismiss();
            });
            return null;
        });
    }

    public boolean isValidUrl(String urlString) {
        return Patterns.WEB_URL.matcher(urlString).matches();
    }

    public void addBookmarkUrl() {
        try {
            TextInputLayout textInputLayout = (TextInputLayout) ViewGroup.inflate(requireActivity(), R.layout.alert_edittext_layout, null);
            textInputLayout.setHint("请输入收藏夹链接");
            textInputLayout.findViewById(R.id.download_checkbox).setVisibility(View.VISIBLE);
            new AlertDialog.Builder(requireContext())
                    .setTitle("新增收藏夹链接")
                    .setView(textInputLayout)
                    .setPositiveButton("确定", (dialogInterface, i) -> {
                        TextInputEditText input = textInputLayout.findViewById(R.id.text_input_edit);
                        MaterialCheckBox checkBox = textInputLayout.findViewById(R.id.download_checkbox);
                        if (Objects.requireNonNull(input.getText()).length() > 0) {
                            String url = input.getText().toString();
                            Boolean downloadResource = checkBox.isChecked();
                            if (isValidUrl(url)) {
                                addBookmarkUrlServer(url, downloadResource);
                            } else {
                                Toast.makeText(requireActivity(), "url不合法！", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }).setNegativeButton("取消", null).show();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }

    }
}
