package com.example.myapplication.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AppExecutors {

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Executor MAIN = new MainThreadExecutor();

    private AppExecutors() {
    }

    /** Executor dyal l'khedma f background (DB, network...). */
    public static ExecutorService io() {
        return IO;
    }

    /** Executor dyal l'main thread (UI). */
    public static Executor main() {
        return MAIN;
    }

    private static final class MainThreadExecutor implements Executor {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            handler.post(command);
        }
    }
}
