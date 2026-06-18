package com.example.operatingsystempraticalclass;

import android.content.Context;
import android.content.SharedPreferences;

public class CacheManager {
    private static  final  String PRE_NAME = "cache_prefs";
    private  static  final String KEY_CACHE_TIMESTAMP = "cache_timestamp";

    public  static  void saveCacheTimeStamp(Context context, long timestamp){
        SharedPreferences prefs = context.getSharedPreferences(PRE_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_CACHE_TIMESTAMP, timestamp).apply();

    }

    public  static  long  getCacheTimestamp(Context context){
        return context.getSharedPreferences(PRE_NAME, Context.MODE_PRIVATE).getLong(KEY_CACHE_TIMESTAMP, 0);
    }
}
