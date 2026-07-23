package com.profilepilot.autofill;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public final class SyncWorker extends Worker {
    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters parameters) {
        super(context, parameters);
    }

    @NonNull @Override public Result doWork() {
        return SyncClient.flushBlocking(getApplicationContext()) ? Result.success() : Result.retry();
    }
}
