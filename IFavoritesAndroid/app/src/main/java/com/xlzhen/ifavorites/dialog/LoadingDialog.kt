package com.xlzhen.ifavorites.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.Window
import com.xlzhen.ifavorites.R

class LoadingDialog(context: Context) : Dialog(context) {

    init {
        // 设置无标题
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        // 设置弹窗背景为透明，以便看到布局中的圆角背景
        window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        // 设置点击外部区域不可取消
        setCanceledOnTouchOutside(false)
        setCancelable(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.request_dialog_layout)
    }
}