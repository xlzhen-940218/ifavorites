package com.xlzhen.mvvm.binding.base;


import com.xlzhen.mvvm.fragment.BaseFragment;

public class BaseFragmentModel <T extends BaseFragment> extends BaseUIViewModel {
    protected T fragment;

    public BaseFragmentModel(T fragment) {
        this.fragment = fragment;
    }


}
