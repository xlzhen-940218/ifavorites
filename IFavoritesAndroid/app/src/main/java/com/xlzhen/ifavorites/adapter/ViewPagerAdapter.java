package com.xlzhen.ifavorites.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.xlzhen.ifavorites.api.Folder;
import com.xlzhen.ifavorites.fragment.SubFolderFragment;

import java.util.ArrayList;
import java.util.List;

public class ViewPagerAdapter extends FragmentStateAdapter {

    private final List<Folder> folders = new ArrayList<>();
    private final boolean mainFolder;

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity, List<Folder> folders, boolean mainFolder) {
        super(fragmentActivity);
        this.mainFolder = mainFolder;
        this.folders.addAll(folders);
    }

    public void updateData(List<Folder> newFolders) {
        folders.addAll(newFolders);
        notifyItemRangeChanged(0, folders.size());
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // 根据位置创建并返回对应的 Fragment 实例
        return SubFolderFragment.newInstance(folders.get(position), mainFolder);
    }

    @Override
    public int getItemCount() {
        // 返回页面总数
        return folders.size();
    }

    public String getItemText(int position) {
        return folders.get(position).getName();
    }

    public void setData(List<Folder> subFolders) {
        folders.clear();
        folders.addAll(subFolders);
        notifyItemRangeChanged(0, folders.size());
    }
}