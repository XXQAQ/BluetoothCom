package com.xq.bluetoothcom;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

class GattCallbackDispatcher extends BluetoothGattCallback {

    private final String tag;

    public GattCallbackDispatcher(String mac) {
        tag = "GattCallbackDispatcher" + "-" + mac;
    }

    private final Set<BluetoothGattCallback> gattCallbacks = new LinkedHashSet<>();

    public boolean registerGattCallback(BluetoothGattCallback gattCallback){
        synchronized (gattCallbacks){
            return gattCallbacks.add(gattCallback);
        }
    }

    public boolean unregisterGattCallback(BluetoothGattCallback gattCallback){
        synchronized (gattCallbacks){
            return gattCallbacks.remove(gattCallback);
        }
    }

    public void clearGattCallback(){
        gattCallbacks.clear();
    }

    private List<BluetoothGattCallback> syncGetAllGattCallback(){
        synchronized (gattCallbacks){
            return new ArrayList<>(gattCallbacks);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
        super.onPhyUpdate(gatt, txPhy, rxPhy, status);
        Log.i(tag, String.format("onPhyUpdate: txPhy=%s, rxPhy=%s, status=%s",txPhy,rxPhy,status));
        for (BluetoothGattCallback gattCallback : syncGetAllGattCallback()){
            gattCallback.onPhyUpdate(gatt, txPhy, rxPhy, status);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
        super.onPhyRead(gatt, txPhy, rxPhy, status);
        Log.i(tag, String.format("onPhyRead: txPhy=%s, rxPhy=%s, status=%s",txPhy,rxPhy,status));
        for (BluetoothGattCallback gattCallback : syncGetAllGattCallback()){
            gattCallback.onPhyRead(gatt, txPhy, rxPhy, status);
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        Log.d(tag, String.format("onConnectionStateChange: status=%s , newState=%s", status, newState));
        for (BluetoothGattCallback gattCallback : syncGetAllGattCallback()){
            gattCallback.onConnectionStateChange(gatt, status, newState);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        Log.d(tag, String.format("onServicesDiscovered: status=%s", status));
        for (BluetoothGattCallback gattCallback : syncGetAllGattCallback()){
            gattCallback.onServicesDiscovered(gatt, status);
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        Log.i(tag, String.format("onCharacteristicRead: status=%s, values=%s",status,bytes2HexString(characteristic.getValue())));
        for (BluetoothGattCallback gattCallback : syncGetAllGattCallback()){
            gattCallback.onCharacteristicRead(gatt, characteristic, status);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value, int status) {
        super.onCharacteristicRead(gatt, characteristic, value, status);
        for (BluetoothGattCallback gattCallback : syncGetAllGattCallback()){
            gattCallback.onCharacteristicRead(gatt, characteristic, value, status);
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        Log.i(tag, String.format("onCharacteristicWrite: status=%s, values=%s",status,bytes2HexString(characteristic.getValue())));
        for (BluetoothGattCallback gattCallback : syncGetAllGattCallback()){
            gattCallback.onCharacteristicWrite(gatt, characteristic, status);
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        Log.i(tag,String.format("onCharacteristicChanged : values=%s", bytes2HexString(characteristic.getValue())));
        for (BluetoothGattCallback gattCallback : syncGetAllGattCallback()){
            gattCallback.onCharacteristicChanged(gatt, characteristic);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        super.onCharacteristicChanged(gatt, characteristic, value);
        for (BluetoothGattCallback gattCallback : syncGetAllGattCallback()){
            gattCallback.onCharacteristicChanged(gatt, characteristic, value);
        }
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorRead(gatt, descriptor, status);
        Log.i(tag, String.format("onDescriptorRead: status=%s, values=%s",status,bytes2HexString(descriptor.getValue())));
        for (BluetoothGattCallback gattCallback : syncGetAllGattCallback()){
            gattCallback.onDescriptorRead(gatt, descriptor, status);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status, byte[] value) {
        super.onDescriptorRead(gatt, descriptor, status, value);
        for (BluetoothGattCallback gattCallback : syncGetAllGattCallback()){
            gattCallback.onDescriptorRead(gatt, descriptor, status, value);
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        Log.i(tag, String.format("onDescriptorWrite: status=%s, values = %s",status,bytes2HexString(descriptor.getValue())));
        for (BluetoothGattCallback gattCallback : syncGetAllGattCallback()){
            gattCallback.onDescriptorWrite(gatt, descriptor, status);
        }
    }

    @Override
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
        super.onReliableWriteCompleted(gatt, status);
        Log.i(tag, String.format("onReliableWriteCompleted: status=%s",status));
        for (BluetoothGattCallback gattCallback : syncGetAllGattCallback()){
            gattCallback.onReliableWriteCompleted(gatt, status);
        }
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        super.onReadRemoteRssi(gatt, rssi, status);
        Log.i(tag, String.format("onReadRemoteRssi: rssi=%s, status=%s",rssi,status));
        for (BluetoothGattCallback gattCallback : syncGetAllGattCallback()){
            gattCallback.onReadRemoteRssi(gatt, rssi, status);
        }
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        super.onMtuChanged(gatt, mtu, status);
        Log.i(tag, String.format("onMtuChanged: mtu=%s, status=%s",mtu,status));
        for (BluetoothGattCallback gattCallback : syncGetAllGattCallback()){
            gattCallback.onMtuChanged(gatt, mtu, status);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onServiceChanged(BluetoothGatt gatt) {
        super.onServiceChanged(gatt);
        Log.i(tag, "onServiceChanged: ");
        for (BluetoothGattCallback gattCallback : syncGetAllGattCallback()){
            gattCallback.onServiceChanged(gatt);
        }
    }

    private static final char HEX_DIGITS[] =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String bytes2HexString(final byte[] bytes) {
        if (bytes == null) return "";
        int len = bytes.length;
        if (len <= 0) return "";
        char[] ret = new char[len << 1];
        for (int i = 0, j = 0; i < len; i++) {
            ret[j++] = HEX_DIGITS[bytes[i] >> 4 & 0x0f];
            ret[j++] = HEX_DIGITS[bytes[i] & 0x0f];
        }
        return new String(ret);
    }
}
