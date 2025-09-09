package com.xlzhen.ifavorites.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xlzhen.ifavorites.R;
import com.xlzhen.ifavorites.model.UIFolder;

import java.util.List;

public class MenuSpinnerAdapter extends ArrayAdapter<UIFolder> {
    private final Context context;
    private final int resource;
    public MenuSpinnerAdapter(Context context, List<UIFolder> data) {
        super(context, R.layout.adapter_menu_spinner, data);
        this.context = context;
        this.resource = R.layout.adapter_menu_spinner;
    }
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // 1. 获取当前位置的 UIFolder 对象
        UIFolder folder = getItem(position);

        // 2. 检查 convertView 是否可以复用
        View folderView = convertView;
        if (folderView == null) {
            // 如果 convertView 为空，则从布局文件加载新的 View
            LayoutInflater inflater = LayoutInflater.from(context);
            folderView = inflater.inflate(resource, parent, false);
        }

        // 3. 查找布局中的 menuTextView
        // 注意：你需要将这里的 R.id.menuTextView 替换为你实际的 TextView ID
        TextView menuTextView = folderView.findViewById(R.id.menuTextView);

        // 4. 将 UIFolder 的 name 字段赋值给 TextView
        if (folder != null && menuTextView != null) {
            // 假设 UIFolder 有一个 public String getName() 方法
            menuTextView.setText(folder.getName());
        }

        return folderView;
    }

    public void setData(List<UIFolder> data){
        clear();
        addAll(data);
        notifyDataSetChanged();
    }
}
