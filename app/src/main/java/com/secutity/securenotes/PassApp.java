package com.secutity.securenotes;

import android.annotation.SuppressLint;
import android.app.Application;

import net.sqlcipher.database.SQLiteDatabase;

@SuppressLint("Registered")
public class PassApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SQLiteDatabase.loadLibs(this);
    }
}
