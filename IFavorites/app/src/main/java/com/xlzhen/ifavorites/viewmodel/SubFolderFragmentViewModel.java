package com.xlzhen.ifavorites.viewmodel;

import android.content.Intent;
import android.net.Uri;
import android.view.View;

import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.xlzhen.ifavorites.adapter.BookmarkAdapter;
import com.xlzhen.ifavorites.api.Bookmark;
import com.xlzhen.ifavorites.api.RetrofitClient;
import com.xlzhen.ifavorites.fragment.SubFolderFragment;
import com.xlzhen.mvvm.binding.base.BaseFragmentModel;

import java.util.List;
import java.util.Objects;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class SubFolderFragmentViewModel extends BaseFragmentModel<SubFolderFragment> {
    public GridLayoutManager gridLayoutManager;
    public BookmarkAdapter bookmarkAdapter;
    public MutableLiveData<Boolean> subFolder = new MutableLiveData<>(false);

    public SubFolderFragmentViewModel(SubFolderFragment fragment) {
        super(fragment);

        gridLayoutManager = new GridLayoutManager(fragment.getActivity(), 2, LinearLayoutManager.VERTICAL, false);
    }

    public void setBookmarkData(List<Bookmark> bookmarkData) {
        bookmarkAdapter = new BookmarkAdapter(bookmarkData, bookmark -> {
            if(bookmark.getFilepath() != null){
                var filePathUrl = RetrofitClient.BASE_URL + Objects.requireNonNull(bookmark.getFilepath()).replace("\\", "/");

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(filePathUrl));
                intent.setPackage(fragment.requireActivity().getPackageName());
                intent.setDataAndType(Uri.parse(filePathUrl), "video/*"); // 同时设置数据和类型
                fragment.startActivity(intent);
            }else {
                fragment.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.getLink())));
            }
            return Unit.INSTANCE;
        });
        fragment.getBinding().bookmarkRecyclerView.setLayoutManager(gridLayoutManager);
        fragment.getBinding().bookmarkRecyclerView.setAdapter(bookmarkAdapter);
    }

    public void addFolderClick(View view) {
        fragment.addFolder();
    }

    public void addBookmarkClick(View view) {
        fragment.addBookmarkUrl();
    }
}
