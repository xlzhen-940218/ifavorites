package com.xlzhen.ifavorites.model;

import androidx.annotation.NonNull;

public class UIFolder {
    private String uId;
    private String name;
    private boolean selected;

    public UIFolder(String uId, String name, boolean selected) {
        this.uId = uId;
        this.name = name;
        this.selected = selected;
    }

    public UIFolder() {
    }

    public String getUId() {
        return uId;
    }

    public void setUId(String uId) {
        this.uId = uId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
