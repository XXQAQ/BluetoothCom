package com.xq.bluetoothcom;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class BluetoothCom {

    //一般蓝牙最大连接超时为30秒，当出现30秒才回调的时候是不需要再重连的，因为此时蓝牙设备大概率已经不在了。
    private static final int CONNECT_TIME_OUT = 30*1000-1;

    private final Context context;

    private final SerialWorkManager connectWorkManager = new SerialWorkManager();

    public BluetoothCom(Context context) {
        this.context = context;
    }

    private final Map<String, AtomicReference<Record>> recordMap = new HashMap<>();

//    public void connectWithoughtSearch(String mac, final int discoverDelay, final int retryCount, final OnConnectListener onConnectListener){
//        connect(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac),discoverDelay,retryCount,onConnectListener);
//    }

    public void connect(final BluetoothDevice bluetoothDevice , final int discoverDelay, final int retryCount, final OnConnectListener onConnectListener){

        final String device = bluetoothDevice.getAddress();

        AtomicReference<Record> reference = new AtomicReference<>();
        synchronized (recordMap){
            recordMap.put(device,reference);
        }

        final Supplier<Boolean> containWithRemove = new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                if (recordMap.containsKey(device)){
                    synchronized (recordMap){
                        if (recordMap.containsKey(device)){
                            recordMap.remove(device);
                            return true;
                        }
                    }
                }
                return false;
            }
        };
        connectRetry(reference, device, bluetoothDevice, discoverDelay, retryCount, 0, new OnConnectListener() {
            @Override
            public void onSuccess(BluetoothChannel bluetoothChannel) {
                onConnectListener.onSuccess(bluetoothChannel);
            }

            @Override
            public void onError(String info, String code) {
                if (containWithRemove.get()) {
                    onConnectListener.onError(info, code);
                }
            }
        }, new BluetoothChannel.OnCloseListener() {
            @Override
            public void onClose() {
                containWithRemove.get();
            }
        });
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private void connectRetry(final AtomicReference<Record> reference, final String device, final BluetoothDevice bluetoothDevice, final int discoverDelay, final int maxCount, final int curCount, final OnConnectListener onConnectListener, final BluetoothChannel.OnCloseListener onCloseListener){
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (recordMap.containsKey(device)){
                    final long overTime = System.currentTimeMillis() + CONNECT_TIME_OUT;
                    reallyConnect(reference, bluetoothDevice, discoverDelay, new OnConnectListener() {
                        @Override
                        public void onSuccess(BluetoothChannel bluetoothChannel) {
                            onConnectListener.onSuccess(bluetoothChannel);
                        }

                        @Override
                        public void onError(String info, String code) {
                            if (curCount >= maxCount-1 || overTime >= System.currentTimeMillis()){
                                onConnectListener.onError(info,code);
                            } else {
                                connectRetry(reference,device,bluetoothDevice,discoverDelay,maxCount,curCount+1,onConnectListener,onCloseListener);
                            }
                        }
                    },onCloseListener);
                }
            }
        },curCount == 0? 0 : (int)(Math.pow(2,curCount%3))*1000L);
    }

    public void reallyConnect(AtomicReference<Record> reference, final BluetoothDevice bluetoothDevice, final int discoverDelay, final OnConnectListener onConnectListener, final BluetoothChannel.OnCloseListener onCloseListener){

        final Record record = new Record();
        record.connectWorkRunnable = connectWorkManager.new WorkRunnable() {
            @Override
            public void run() {

                final GattCallbackDispatcher gattCallbackDispatcher = new GattCallbackDispatcher(bluetoothDevice.getAddress()) {

                    private final AtomicBoolean firstConnect = new AtomicBoolean(true);

                    //注意：这个方法可能会多次回调
                    @Override
                    public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
                        super.onConnectionStateChange(gatt, status, newState);

                        leave();

                        //初次连接
                        if (firstConnect.get()){
                            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                                //单纯的连接成功不能算成功，要等到发现服务的时候才能算成功
                                record.discoverHandler = createDiscoverHandler(gatt);
                                record.discoverHandler.sendEmptyMessageDelayed(0, discoverDelay);
                            } else {
                                firstConnect.set(false);
                                gatt.disconnect();
                                gatt.close();
                                onConnectListener.onError("onConnectionStateChange fail", status+"-"+newState);
                            }
                        }
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        super.onServicesDiscovered(gatt, status);

                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            firstConnect.set(false);
                            record.bluetoothChannel = new BluetoothChannel(gatt, this, onCloseListener);
                            onConnectListener.onSuccess(record.bluetoothChannel);
                        } else {
                            firstConnect.set(false);
                            gatt.disconnect();
                            gatt.close();
                            onConnectListener.onError("onServicesDiscovered fail", String.valueOf(status));
                        }
                    }
                };
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    record.bluetoothGatt = bluetoothDevice.connectGatt(context, false,gattCallbackDispatcher,BluetoothDevice.TRANSPORT_LE);
                } else {
                    record.bluetoothGatt = bluetoothDevice.connectGatt(context, false,gattCallbackDispatcher);
                }
            }
        };
        reference.set(record);

        connectWorkManager.joinWork(record.connectWorkRunnable);
    }

    private Handler createDiscoverHandler(final BluetoothGatt gatt){
        return new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == 0){
                    //这个方法一定要延迟调用，刚链接上的时候大概率无法discoverServices
                    gatt.discoverServices();
                    return true;
                }
                return false;
            }
        });
    }

    public boolean disconnect(String device){
        Pair<Boolean, AtomicReference<Record>> pair = containWithRemoveDevice(device);
        if (pair.first){
            if (pair.second == null){
                //一般走到这里的时候，意味着正在重连中，因为我们删掉了这个设备的Key，所以重连不会发生
            } else {
                Record record = pair.second.get();
                if(record.bluetoothChannel != null){
                    record.bluetoothChannel.close();
                } else {
                    //
                    if (connectWorkManager.containWork(record.connectWorkRunnable)){
                        record.connectWorkRunnable.leave();
                    }
                    //如果还没调用discoverServices就赶紧取消该消息
                    if (record.discoverHandler != null && record.discoverHandler.hasMessages(0)){
                        record.discoverHandler.removeMessages(0);
                    }
                    //
                    if (record.bluetoothGatt != null){
                        record.bluetoothGatt.disconnect();
                        record.bluetoothGatt.close();
                    }
                }
            }
        }
        return pair.first;
    }

    private List<String> getAllConnected(){
        return new ArrayList<>(recordMap.keySet());
    }

    private Pair<Boolean,AtomicReference<Record>> containWithRemoveDevice(String device){
        if (recordMap.containsKey(device)){
            synchronized (recordMap){
                if (recordMap.containsKey(device)){
                    return new Pair<>(true,recordMap.remove(device));
                }
            }
        }
        return new Pair<>(false,null);
    }

    private class Record {
        //连接前阶段
        //
        private SerialWorkManager.WorkRunnable connectWorkRunnable;
        //
        private BluetoothGatt bluetoothGatt;
        //回调onConnectionStateChange后，会调用discoverServices，这个时候未回调onServicesDiscovered
        private Handler discoverHandler;
        //连接后阶段
        //以上流程完成后都使用它来 中止连接
        private BluetoothChannel bluetoothChannel;
    }

    interface Supplier<T> {
        T get();
    }

    public interface OnConnectListener{
        void onSuccess(BluetoothChannel bluetoothChannel);
        void onError(String info,String code);
    }

}
