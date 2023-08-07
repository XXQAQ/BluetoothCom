package com.xq.bluetoothcom;

import android.os.Handler;
import android.os.Looper;
import android.util.Pair;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class SerialWorkManager {

    private final ExecutorService workThread = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());

    private final Map<WorkRunnable, Future<?>> futureMap = new LinkedHashMap<>();

    public boolean joinWork(WorkRunnable workRunnable){
        return joinWork(Integer.MAX_VALUE,workRunnable);
    }

    public boolean joinWork(final int outTime, final WorkRunnable workRunnable){
        synchronized (futureMap){
            try {
                futureMap.put(workRunnable,workThread.submit(new Runnable() {

                    @Override
                    public void run() {
                        //所有回调都需要执行在主线程
                        Handler handler = new Handler(Looper.getMainLooper());
                        //
                        dispatchRun(handler,workRunnable);
                        //
                        try {
                            Thread.sleep(outTime);
                            //上面的倒计时结束了还没被中断，说明超时了，那么直接回调超时事件
                            if (containWithRemove()){
                                dispatchOnTimeout(handler,workRunnable);
                            }
                        } catch (InterruptedException e) {
                            //
                            handler.removeCallbacksAndMessages(null);
                        }
                    }

                    private void dispatchRun(Handler handler,final WorkRunnable workRunnable){
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                workRunnable.run();
                            }
                        });
                    }

                    private void dispatchOnTimeout(Handler handler,final WorkRunnable workRunnable){
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                workRunnable.onTimeout();
                            }
                        });
                    }

                    private boolean containWithRemove(){
                        if (futureMap.containsKey(workRunnable)){
                            synchronized (futureMap){
                                if (futureMap.containsKey(workRunnable)){
                                    futureMap.remove(workRunnable);
                                    return true;
                                }
                            }
                        }
                        return false;
                    }
                }));
                return true;
            } catch (RejectedExecutionException e){
                e.printStackTrace();
                return false;
            }
        }
    }

    public boolean leaveWork(WorkRunnable workRunnable){
        Pair<Boolean, Future<?>> pair = containWithRemoveWork(workRunnable);
        if (pair.first){
            pair.second.cancel(true);
        }
        return pair.first;
    }

    public boolean containWork(WorkRunnable workRunnable){
        return futureMap.containsKey(workRunnable);
    }

    private Pair<Boolean,Future<?>> containWithRemoveWork(WorkRunnable workRunnable){
        if (futureMap.containsKey(workRunnable)){
            synchronized (futureMap){
                if (futureMap.containsKey(workRunnable)){
                    return new Pair<Boolean,Future<?>>(true,futureMap.get(workRunnable));
                }
            }
        }
        return new Pair<>(false,null);
    }

    public void clearAllWork(){
        for (Future<?> future : futureMap.values()){
            future.cancel(true);
        }
        futureMap.clear();
    }

    public void close(){
        workThread.shutdownNow();
        futureMap.clear();
    }

    public abstract class WorkRunnable implements Runnable{

        public void onTimeout(){

        }

        public boolean leave(){
            return leaveWork(this);
        }
    }

}
