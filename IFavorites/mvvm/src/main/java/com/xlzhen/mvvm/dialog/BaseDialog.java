package com.xlzhen.mvvm.dialog;

import android.app.Dialog;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.xlzhen.mvvm.binding.base.BaseDialogViewModel;


public abstract class BaseDialog<K extends ViewDataBinding,V extends BaseDialogViewModel> extends Dialog implements LifecycleOwner{
    protected K binding;
    protected V model;

    private final LifecycleRegistry lifecycleRegistry;
    public BaseDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        lifecycleRegistry = new LifecycleRegistry(this);
        binding = bindingInflate();
        setContentView(binding.getRoot());
        model = bindingModel();
        binding.setLifecycleOwner(this);
        binding.setVariable(getVariableId(),model);
        setCanceledOnTouchOutside(false);

        initDialog();
        binding.getRoot().postDelayed(this::initData, 500);
    }

    protected abstract void initData();

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }

    public K getBinding() {
        return binding;
    }

    protected abstract int getVariableId();

    protected abstract V bindingModel();

    protected abstract K bindingInflate();

    protected abstract void initDialog();

    @Override
    protected void onStart() {
        super.onStart();
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
    }

    @Override
    protected void onStop() {
        super.onStop();
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
    }
}
