package com.xlzhen.ifavorites.api;

import android.content.Context;

import com.xlzhen.ifavorites.model.ServerUrl;
import com.xlzhen.mvvm.storage.StorageUtils;

public class ServerConfig {
    public static String BASE_URL = "http://75.127.13.9:5000/";
    public static void updateUrl(Context context, String url){
        if(!url.endsWith("/")){
            url += "/";
        }
        BASE_URL = url;
        StorageUtils.saveData(context,"server",new ServerUrl(url));
    }


}
