package com.xlzhen.mvvm.adapter;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;
import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;


import com.xlzhen.mvvm.binding.base.BaseAdapterViewModel;
import com.xlzhen.mvvm.binding.base.BaseViewBindingModel;

import java.util.List;

public abstract class SimpleViewBindingAdapter<K extends ViewDataBinding, V extends BaseAdapterViewModel
        , T extends BaseViewBindingModel>
        extends BaseAdapter implements LifecycleOwner {
    protected Context context;
    private List<T> data;

    private final LifecycleRegistry lifecycleRegistry;

    public SimpleViewBindingAdapter(Context context, List<T> data) {
        this.context = context;
        this.data = data;
        lifecycleRegistry = new LifecycleRegistry(this);
    }

    public SimpleViewBindingAdapter(Context context) {
        this.context = context;
        lifecycleRegistry = new LifecycleRegistry(this);
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }

    @Override
    public T getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return data.get(position).getId();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    public void addData(T bean) {
        data.add(bean);
        notifyDataSetChanged();
    }

    public void addDataList(List<T> dataList) {
        data.addAll(dataList);
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        K binding = bindingInflate(parent);
        convertView = binding.getRoot();
        V model = bindingModel();
        binding.setLifecycleOwner(this);
        binding.setVariable(getVariableId(), model);

        model.updateUI(context, binding, getItem(position));
        return convertView;
    }

    protected abstract int getVariableId();

    protected abstract V bindingModel();

    protected abstract K bindingInflate(ViewGroup parent);

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        super.registerDataSetObserver(observer);
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        super.unregisterDataSetObserver(observer);
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
    }
}
