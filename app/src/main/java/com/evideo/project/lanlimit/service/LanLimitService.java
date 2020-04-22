package com.evideo.project.lanlimit.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.evideo.project.lanlimit.manager.LanLimitManager;
import com.evideostb.component.logger.EvLog;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * @ProjectName: evideo-lan-limit
 * @Package: com.evideo.project.lanlimit.service
 * @ClassName: LanLimitService
 * @Description: java类作用描述
 * @Author: chentao
 * @CreateDate: 2020/4/13 18:00
 * @Version: 1.0
 */
public class LanLimitService extends Service {

    private static final String TAG = LanLimitService.class.getSimpleName();

    private Disposable disposable;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        EvLog.i(TAG, "onStartCommand()");
        disposable = Observable.intervalRange(0, Integer.MAX_VALUE, 0, 90, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        LanLimitManager.getInstance().searchDevices();
                    }
                });

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (disposable != null) {
            disposable.dispose();
            disposable = null;
        }
    }
}
