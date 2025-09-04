package com.xlzhen.mvvm.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;


import com.xlzhen.mvvm.binding.base.BaseFragmentModel;


public abstract class BaseFragment<K extends ViewDataBinding,V extends BaseFragmentModel> extends Fragment {
    protected K binding;
    protected V model;
    protected Context context;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(this.getClass().getSimpleName(), "..........onCreate..........");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(this.getClass().getSimpleName(), "..........onStart..........");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(this.getClass().getSimpleName(), "..........onResume..........");
        pageLoaded();
    }

    protected abstract void pageLoaded();

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(this.getClass().getSimpleName(), "..........onDestroy..........");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(this.getClass().getSimpleName(), "..........onCreateView..........");
        binding = bindingInflate(inflater,container);
        model = bindingModel();
        binding.setLifecycleOwner(this);
        binding.setVariable(getVariableId(),model);
        initData();
        return binding.getRoot();
    }

    protected abstract int getVariableId();

    protected abstract K bindingInflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup container);

    protected abstract V bindingModel();

    protected abstract void initData();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;

        Log.i(this.getClass().getSimpleName(), "..........onAttach..........");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i(this.getClass().getSimpleName(), "..........onDestroyView..........");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        context = null;

        Log.i(this.getClass().getSimpleName(), "..........onDetach..........");
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        Log.i(this.getClass().getSimpleName(), "..........onHiddenChanged..........");
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.i(this.getClass().getSimpleName(), "..........onLowMemory..........");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(this.getClass().getSimpleName(), "..........onPause..........");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(this.getClass().getSimpleName(), "..........onStop..........");
    }

    public K getBinding() {
        return binding;
    }
}
