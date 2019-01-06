package com.ckcz123.h5mota.maker;

import android.app.Application;

import com.tencent.bugly.crashreport.CrashReport;

/**
 * Created by castor_v_pollux on 2018/12/17.
 */

public class H5motaApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        CrashReport.initCrashReport(getApplicationContext(), "77e1f685ce", false);
    }

}