package com.xlzhen.mvvm.binding.base;

import com.xlzhen.mvvm.dialog.BaseDialog;

public class BaseDialogViewModel<T extends BaseDialog> extends BaseViewBindingModel{
    protected T dialog;

    public BaseDialogViewModel(T dialog) {
        this.dialog = dialog;
    }
}
