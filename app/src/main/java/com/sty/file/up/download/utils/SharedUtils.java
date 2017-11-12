package com.sty.file.up.download.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Shi Tianyi on 2017/11/9/0009.
 */

public class SharedUtils {

    public static int getLastPosition(Context context, int threadId){
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return defaultSharedPreferences.getInt("lastPosition" + threadId, -1);
    }

    public static void setLastPosition(Context context, int threadId, int position){
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        defaultSharedPreferences.edit().putInt("lastPosition" + threadId, position).commit();
    }

    public static void deleteLastPosition(Context context, int threadId){
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        defaultSharedPreferences.edit().putInt("lastPosition" + threadId, -2).commit();
    }

    public static void deleteSharedPreferences(Context context){
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        defaultSharedPreferences.edit().clear().commit();
    }
}
