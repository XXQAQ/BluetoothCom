package com.xq.bluetoothcom;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BluetoothChannel {

    private final BluetoothGatt bluetoothGatt;

    private final GattCallbackDispatcher gattCallbackDispatcher;

    private final SerialWorkManager serialWorkManager = new SerialWorkManager();

    private final OnCloseListener onCloseListener;

    public BluetoothChannel(final BluetoothGatt bluetoothGatt, final GattCallbackDispatcher gattCallbackDispatcher, final OnCloseListener onCloseListener) {
        this.bluetoothGatt = bluetoothGatt;
        this.gattCallbackDispatcher = gattCallbackDispatcher;
        this.onCloseListener = onCloseListener;
        this.gattCallbackDispatcher.registerGattCallback(new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (newState == BluetoothProfile.STATE_DISCONNECTED){
                    //
                    gattCallbackDispatcher.clearGattCallback();
                    notifyCallbackMap.clear();
                    //
                    serialWorkManager.close();
                    //
                    bluetoothGatt.close();
                    //
                    onCloseListener.onClose();
                    //
                    onDisconnectedListener.onDisconnected();
                }
            }
        });
    }

    private OnDisconnectedListener onDisconnectedListener;
    public void setOnDisconnectedListener(OnDisconnectedListener onDisconnectedListener) {
        this.onDisconnectedListener = onDisconnectedListener;
    }

    public void close(){
        //
        gattCallbackDispatcher.clearGattCallback();
        notifyCallbackMap.clear();
        //
        serialWorkManager.close();
        //
        bluetoothGatt.disconnect();
        bluetoothGatt.close();
        //
        onCloseListener.onClose();
    }

    public void writeCharacteristic(final String serviceUUID, final String characterUUID, final byte[] bytes,final OnActionCallback callback){
        serialWorkManager.joinWork(serialWorkManager.new WorkRunnable(){
            @Override
            public void run() {
                if (writeCharacteristic(serviceUUID, characterUUID, bytes)) {
                    gattCallbackDispatcher.registerGattCallback(new BluetoothGattCallback() {
                        @Override
                        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                            super.onCharacteristicWrite(gatt, characteristic, status);
                            gattCallbackDispatcher.unregisterGattCallback(this);
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                callback.onSuccess();
                            } else {
                                callback.onError("onCharacteristicWrite fail", String.valueOf(status));
                            }
                            leave();
                        }
                    });
                } else {
                    callback.onError("writeCharacteristic error", "");
                    leave();
                }
            }
        });
    }

    public void readCharacteristic(final String serviceUUID, final String characterUUID,final OnReadCallback callback){
        serialWorkManager.joinWork(serialWorkManager.new WorkRunnable() {
            @Override
            public void run() {
                if (readCharacteristic(serviceUUID, characterUUID)) {
                    gattCallbackDispatcher.registerGattCallback(new BluetoothGattCallback() {
                        @Override
                        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                            super.onCharacteristicRead(gatt, characteristic, status);
                            gattCallbackDispatcher.unregisterGattCallback(this);
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                callback.onRead(characteristic.getValue());
                            } else {
                                callback.onError("onCharacteristicRead fail", String.valueOf(status));
                            }
                            leave();
                        }
                    });
                } else {
                    callback.onError("readCharacteristic error", "");
                    leave();
                }
            }
        });
    }

    public void writeDescriptor(final String serviceUUID, final String characterUUID, final String descriptorUUID, final byte[] bytes,final OnActionCallback callback){
        serialWorkManager.joinWork(serialWorkManager.new WorkRunnable() {
            @Override
            public void run() {
                if (writeDescriptor(serviceUUID, characterUUID, descriptorUUID, bytes)) {
                    gattCallbackDispatcher.registerGattCallback(new BluetoothGattCallback() {
                        @Override
                        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                            super.onDescriptorWrite(gatt, descriptor, status);
                            gattCallbackDispatcher.unregisterGattCallback(this);
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                callback.onSuccess();
                            } else {
                                callback.onError("onDescriptorWrite fail", String.valueOf(status));
                            }
                            leave();
                        }
                    });
                } else {
                    callback.onError("writeDescriptor error", "");
                    leave();
                }
            }
        });
    }

    public void readDescriptor(final String serviceUUID, final String characterUUID, final String descriptorUUID, final OnReadCallback callback){
        serialWorkManager.joinWork(serialWorkManager.new WorkRunnable() {
            @Override
            public void run() {
                if (readDescriptor(serviceUUID, characterUUID, descriptorUUID)) {
                    gattCallbackDispatcher.registerGattCallback(new BluetoothGattCallback() {
                        @Override
                        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                            super.onDescriptorRead(gatt, descriptor, status);
                            gattCallbackDispatcher.unregisterGattCallback(this);
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                callback.onRead(descriptor.getValue());
                            } else {
                                callback.onError("onDescriptorRead fail", String.valueOf(status));
                            }
                            leave();
                        }
                    });
                } else {
                    callback.onError("readDescriptor error", "");
                    leave();
                }
            }
        });
    }

    private final Map<String,BluetoothGattCallback> notifyCallbackMap = new HashMap<>();

    public void startNotify(final String serviceUUID, final String characterUUID, final NotifyType notifyType, final OnActionCallback callback, final OnReceiveListener onReceiveListener){
        serialWorkManager.joinWork(serialWorkManager.new WorkRunnable() {
            @Override
            public void run() {
                if (setCharacteristicNotificationEnable(serviceUUID, characterUUID,notifyType)) {
                    gattCallbackDispatcher.registerGattCallback(new BluetoothGattCallback() {
                        @Override
                        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                            super.onDescriptorWrite(gatt, descriptor, status);
                            gattCallbackDispatcher.unregisterGattCallback(this);
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                if (Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) || Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)){
                                    String key = getNotifyKey(serviceUUID,characterUUID);
                                    BluetoothGattCallback gattCallback = createNotifyGattCallback(onReceiveListener);
                                    gattCallbackDispatcher.registerGattCallback(gattCallback);
                                    notifyCallbackMap.put(key,gattCallback);
                                    callback.onSuccess();
                                }
                            } else {
                                callback.onError("onDescriptorWrite fail", String.valueOf(status));
                            }
                            leave();
                        }
                    });
                } else {
                    callback.onError("setCharacteristicNotification error", "");
                    leave();
                }
            }
        });
    }

    public void stopNotify(final String serviceUUID, final String characterUUID,final OnActionCallback callback){
        serialWorkManager.joinWork(serialWorkManager.new WorkRunnable() {
            @Override
            public void run() {
                if (setCharacteristicNotificationDisable(serviceUUID, characterUUID)) {
                    gattCallbackDispatcher.registerGattCallback(new BluetoothGattCallback() {
                        @Override
                        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                            super.onDescriptorWrite(gatt, descriptor, status);
                            gattCallbackDispatcher.unregisterGattCallback(this);
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                if (Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)){
                                    String key = getNotifyKey(serviceUUID,characterUUID);
                                    if (notifyCallbackMap.containsKey(key)){
                                        gattCallbackDispatcher.unregisterGattCallback(notifyCallbackMap.get(key));
                                    }
                                    callback.onSuccess();
                                }
                            } else {
                                callback.onError("onDescriptorWrite fail", String.valueOf(status));
                            }
                            leave();
                        }
                    });
                } else {
                    callback.onError("setCharacteristicNotification error", "");
                    leave();
                }
            }
        });
    }


    private BluetoothGattCallback createNotifyGattCallback(final OnReceiveListener listener){
        return new BluetoothGattCallback() {
            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                listener.onReceive(characteristic.getValue());
            }
        };
    }

    private String getNotifyKey(final String serviceUUID, final String characterUUID){
        return "/"+serviceUUID+"/"+characterUUID;
    }

    private boolean writeCharacteristic(String serviceUUID,String characterUUID,byte[] bytes){
        try {
            BluetoothGattCharacteristic characteristic = bluetoothGatt
                    .getService(UUID.fromString(serviceUUID))
                    .getCharacteristic(UUID.fromString(characterUUID));
            return characteristic.setValue(bytes) && bluetoothGatt.writeCharacteristic(characteristic);
        } catch (NullPointerException e){
            return false;
        }
    }

    private boolean writeDescriptor(String serviceUUID,String characterUUID,String descriptorUUID,byte[] bytes){
        try {
            BluetoothGattDescriptor descriptor = bluetoothGatt
                    .getService(UUID.fromString(serviceUUID))
                    .getCharacteristic(UUID.fromString(characterUUID))
                    .getDescriptor(UUID.fromString(descriptorUUID));
            return descriptor.setValue(bytes) && bluetoothGatt.writeDescriptor(descriptor);
        } catch (NullPointerException e){
            return false;
        }
    }

    private boolean readCharacteristic(String serviceUUID,String characterUUID){
        try {
            BluetoothGattCharacteristic characteristic = bluetoothGatt
                    .getService(UUID.fromString(serviceUUID))
                    .getCharacteristic(UUID.fromString(characterUUID));
            return bluetoothGatt.readCharacteristic(characteristic);
        } catch (NullPointerException e){
            return false;
        }
    }

    private boolean readDescriptor(String serviceUUID,String characterUUID,String descriptorUUID){
        try {
            BluetoothGattDescriptor descriptor = bluetoothGatt
                    .getService(UUID.fromString(serviceUUID))
                    .getCharacteristic(UUID.fromString(characterUUID))
                    .getDescriptor(UUID.fromString(descriptorUUID));
            return bluetoothGatt.readDescriptor(descriptor);
        } catch (NullPointerException e){
            return false;
        }
    }

    private boolean setCharacteristicNotificationEnable(String serviceUUID,String characterUUID,NotifyType notifyType){
        try {
            BluetoothGattCharacteristic characteristic = bluetoothGatt
                    .getService(UUID.fromString(serviceUUID))
                    .getCharacteristic(UUID.fromString(characterUUID));
            //标准预设通知的Descriptor
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            //
            return bluetoothGatt.setCharacteristicNotification(characteristic, true)
                    && descriptor.setValue((notifyType == NotifyType.Indication?BluetoothGattDescriptor.ENABLE_INDICATION_VALUE:BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE))
                    && bluetoothGatt.writeDescriptor(descriptor);
        } catch (NullPointerException e){
            return false;
        }
    }

    private boolean setCharacteristicNotificationDisable(String serviceUUID,String characterUUID){
        try {
            BluetoothGattCharacteristic characteristic = bluetoothGatt
                    .getService(UUID.fromString(serviceUUID))
                    .getCharacteristic(UUID.fromString(characterUUID));
            //标准预设通知的Descriptor
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            //
            return bluetoothGatt.setCharacteristicNotification(characteristic, false)
                    && descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
                    && bluetoothGatt.writeDescriptor(descriptor);
        } catch (NullPointerException e){
            return false;
        }
    }

    interface OnCloseListener{
        void onClose();
    }

    public interface OnReceiveListener {
        void onReceive(byte[] bytes);
    }

    public interface OnActionCallback {
        void onSuccess();
        void onError(String info,String code);
    }

    public interface OnReadCallback {
        void onRead(byte[] bytes);
        void onError(String info,String code);
    }

    public interface OnDisconnectedListener {
        void onDisconnected();
    }

    public enum NotifyType{
        Notify,
        Indication,
    }

}
