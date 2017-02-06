// bleIO_service.java (Originally: DeviceScanActivity.java)


/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package com.example.bleIO.bleIO;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class bleIO_service extends Service {
    private final static String LOGTAG = bleIO_service.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =            "com.example.bleio.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =         "com.example.bleio.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =  "com.example.bleio.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =            "com.example.bleio.ACTION_DATA_AVAILABLE";
    public final static String ACTION_HRM_AVAILABLE =             "com.example.bleio.ACTION_HRM_AVAILABLE";
    public final static String EXTRA_DATA =                       "com.example.bleio.EXTRA_DATA";
    public final static String ACTION_w8_AVAILABLE =              "com.example.bleio.ACTION_w8_AVAILABLE";
    public final static String ACTION_r4_AVAILABLE =              "com.example.bleio.ACTION_r4_AVAILABLE";
    public final static String ACTION_n4_AVAILABLE =              "com.example.bleio.ACTION_n4_AVAILABLE";

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(LOGTAG, "BluetoothGattCallback:onConnectionStateChange(Connected)");
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate_action(intentAction);
                Log.i(LOGTAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(LOGTAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(LOGTAG, "BluetoothGattCallback:onConnectionStateChange(Disconnected)");
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(LOGTAG, "Disconnected from GATT server.");
                broadcastUpdate_action(intentAction);
            } else {
                Log.i(LOGTAG, "BluetoothGattCallback:onConnectionStateChange(unknown)");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(LOGTAG, "BluetoothGattCallback:onServicesDiscovered(Success)");
                broadcastUpdate_action(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.i(LOGTAG, "BluetoothGattCallback:onServicesDiscovered(xSuccess)" + status);
                Log.w(LOGTAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {//==BLE:READ
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(LOGTAG, "onCharacteristicRead(Success)->ACTION_*_AVAILABLE"); //TODO: Process by UUID?
                //broadcastUpdate_CharData(ACTION_DATA_AVAILABLE, characteristic);
                broadcastUpdate_CharData(broadcastUpdate_action_by_uuid(characteristic), characteristic);
            } else {
                Log.i(LOGTAG, "onCharacteristicRead(xSuccess)[" + status +"]");
            }

            //Whatever the result of the Read was, process any pending Notify enable
            //if((bleIO_list.mNotifyCharacteristic == null)||(bleIO_list.mNotifyCharacteristic != characteristic)){   // Notify already enabled for this Characteristic?
            if(bleIO_list.mNotifyCharacteristic == null){   // Notify already enabled? (Should be this characteristic)
                final int charaProp = characteristic.getProperties();
                if( (charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) { //Notify still Requested/Available for this characteristic?
                    Log.i(LOGTAG, "onCharacteristicRead(Handling a pending Notify Enable)=Characteristic.PropertyRead[" + charaProp + "d] Mask:[" + BluetoothGattCharacteristic.PROPERTY_NOTIFY + "d]");
                    setCharacteristicNotification(characteristic, true);    // Enable Notify for this Characteristic
                    bleIO_list.mNotifyCharacteristic = characteristic;      // Log that Notify has been Enabled for this Characteristic
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) { //==BLE:NOTIFY
            Log.i(LOGTAG, "onCharacteristicChanged()->ACTION_****_AVAILABLE");
            //broadcastUpdate_CharData(ACTION_DATA_AVAILABLE, characteristic);
            broadcastUpdate_CharData(broadcastUpdate_action_by_uuid(characteristic), characteristic);
        }
    };

    private void broadcastUpdate_action(final String action) {
        final Intent intent = new Intent(action);
        Log.i(LOGTAG, "broadcastUpdate(action)->intent)[" + action + "]");
        sendBroadcast(intent);
    }

    private String broadcastUpdate_action_by_uuid(BluetoothGattCharacteristic characteristic)
    {
        if (bleIO_UUID.UUID_HRM_Rate.equals(characteristic.getUuid()))  { return(ACTION_HRM_AVAILABLE); }
        if (bleIO_UUID.UUID_bleIO_w8.equals(characteristic.getUuid()))  { return(ACTION_w8_AVAILABLE);   }
        if (bleIO_UUID.UUID_bleIO_r4.equals(characteristic.getUuid()))  { return(ACTION_r4_AVAILABLE);   }
        if (bleIO_UUID.UUID_bleIO_n4.equals(characteristic.getUuid()))  { return(ACTION_n4_AVAILABLE);   }
        return(ACTION_DATA_AVAILABLE); // Generic Processing
    }

    // http://developer.android.com/reference/android/bluetooth/BluetoothGattCharacteristic.html
    private void broadcastUpdate_CharData(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        Log.i(LOGTAG, "broadcastUpdate(action,characteristic)->intent[" + action + "][" + characteristic +"] UUID:["+ characteristic.getUuid()+"]");

        switch(action){
            case ACTION_HRM_AVAILABLE:
                int flag = characteristic.getProperties();
                int format = -1;
                if ((flag & 0x01) != 0) {
                    format = BluetoothGattCharacteristic.FORMAT_UINT16;
                    Log.d(LOGTAG, "Heart rate format UINT16.");
                } else {
                    format = BluetoothGattCharacteristic.FORMAT_UINT8;
                    Log.d(LOGTAG, "Heart rate format UINT8.");
                }
                final int heartRate = characteristic.getIntValue(format, 1);
                //Log.d(LOGTAG, String.format("Received heart rate: %d", heartRate)); //Notify is working
                Log.i(LOGTAG, "Received heart rate: "+ heartRate); //Notify is working
                intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
                break;

            case ACTION_w8_AVAILABLE:
            case ACTION_r4_AVAILABLE:
            case ACTION_n4_AVAILABLE:
                Log.i(LOGTAG, "bleIO format[" + action + "]");
                final byte[] pdataIO = characteristic.getValue();
                if (pdataIO == null || pdataIO.length == 0) {
                    //TODO: Add an error code for displaying on the phone and putting in log file.
                    Log.e(LOGTAG, "Action Error, No Data [" + action + "]");
                } else {
                    intent.putExtra(EXTRA_DATA, characteristic.getValue());
                }
                break;

            default:
                // For all other profiles, writes the data formatted in HEX.
                final byte[] pdataU = characteristic.getValue();
                if (pdataU != null && pdataU.length > 0) {
                    boolean bReadable =true;
                    Log.i(LOGTAG, "Unknown["+action+"]=>HexData"+pdataU.length);
                    final StringBuilder pBuilder = new StringBuilder(pdataU.length);
                    for(byte byteChar : pdataU) {
                        pBuilder.append(String.format("%02X ", byteChar));
                        if (byteChar < ' ' || byteChar > '~') { bReadable = false; }
                    }
                    if (!bReadable){ intent.putExtra(EXTRA_DATA, pBuilder.toString());}
                    else { intent.putExtra(EXTRA_DATA, new String(pdataU) + "\n" + pBuilder.toString());}
                } else {
                    Log.i(LOGTAG, "Unknown["+action+"](NoData)");
                }
                break;
        }

        Log.i(LOGTAG, "sendBroadcast(intent)");
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        bleIO_service getService() {
            return bleIO_service.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    // Init BLE, Return result
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(LOGTAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(LOGTAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(LOGTAG, "connect(BluetoothAdapter not initialized or unspecified address)");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            if (mBluetoothGatt.connect()) {
                Log.d(LOGTAG, "connect(Successfully using existing mBluetoothGatt for connection)");//TODO: This appears to fail, best to clear and make fresh.
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                Log.w(LOGTAG, "connect(Failed to use an existing mBluetoothGatt for connection)");
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(LOGTAG, "connect(Device not found.  Unable to connect)");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(LOGTAG, "connect(Trying to create a new connection)");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.i(LOGTAG, "disconnect(ble==null)");
            Log.w(LOGTAG, "BluetoothAdapter not initialized");
            return;
        }
        Log.i(LOGTAG, "disconnect(ble)");
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            Log.i(LOGTAG, "close(ble==null)");
            return;
        }
        Log.i(LOGTAG, "close(ble)");
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    //Write a Characteristic (i.e. w8), values in the passed characteristic
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) { //checked in parent function
            Log.w(LOGTAG, "writeCharacteristic(Attempt to Write while BluetoothAdapter not initialized), UUID[" + characteristic.getUuid() +"]");
            //Log.w(LOGTAG, "writeCharacteristic(BluetoothAdapter not initialized)");
            return false;
        }
        if(!mBluetoothGatt.writeCharacteristic(characteristic))  // checked in parent function
        {
            Log.e(LOGTAG, "writeCharacteristic(Error), UUID[" + characteristic.getUuid() + "]"); return false;
        }
        Log.i(LOGTAG, "writeCharacteristic(ok) UUID[" + characteristic.getUuid() +"]");
        return true;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)} callback.
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) { // Checked in parent
            Log.w(LOGTAG, "readCharacteristic(Attempt to Read while BluetoothAdapter not initialized), UUID[" + characteristic.getUuid() +"]");
            //Log.w(LOGTAG, "readCharacteristic(BluetoothAdapter not initialized)");
            return;
        }
        if(!mBluetoothGatt.readCharacteristic(characteristic)) { Log.e(LOGTAG, "readCharacteristic(Error), UUID[" + characteristic.getUuid() + "]");}
        Log.i(LOGTAG, "readCharacteristic(ok) UUID[" + characteristic.getUuid() +"]");
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean bEnabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(LOGTAG, "**setCharacteristicNotification("+bEnabled+", Attempt to change Notify while BluetoothAdapter not initialized), UUID[" + characteristic.getUuid() +"]**");
            return;
        }
        if(!mBluetoothGatt.setCharacteristicNotification(characteristic, bEnabled)){Log.e(LOGTAG, "**ERROR**: setCharacteristicNotification()");} // Set Characteristic's Notify as Requested[Enable/Disabled]
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(bleIO_UUID.cUUID_ClientChar));//CLIENT_CHARACTERISTIC_CONFIG (HRM, HTM, BATT, bleIO...)
        if(descriptor==null){
            Log.e(LOGTAG, "**ERROR**: descriptor==null");
            return;
        }
        //descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE); //TODO: DISABLE_NOTIFICATION_VALUE??
        //descriptor.setValue((bEnabled?BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE:BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE));//TODO: Report this as bug fix?
        //descriptor.setValue(bEnabled?BluetoothGattDescriptor.ENABLE_INDICATION_VALUE:BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);//TODO: Report this as bug fix?
        if(bEnabled) {  if(!descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {Log.e(LOGTAG, "**ERROR**: setValue(ENABLE_NOTIFICATION_VALUE)"); }
        } else {        if(!descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)){Log.e(LOGTAG, "**ERROR**: setValue(ENABLE_NOTIFICATION_VALUE)"); } }
        if(!mBluetoothGatt.writeDescriptor(descriptor)){Log.e(LOGTAG, "**ERROR**: writeDescriptor()["+descriptor+"]");}

        Log.i(LOGTAG, "setCharacteristicNotification("+bEnabled+", "+bleIO_UUID.lookup(characteristic.getUuid().toString())+")");

        //ref:
        // TODO: http://e2e.ti.com/support/wireless_connectivity/f/538/p/312382/1103175
        //      This is also sometimes required (e.g. for heart rate monitors) to enable notifications/indications
        //      see: https://developer.bluetooth.org/gatt/descriptors/Pages/DescriptorViewer.aspx?u=org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml
        // TODO: Using Queues: http://stackoverflow.com/questions/17910322/android-ble-api-gatt-notification-not-received
        // TODO: http://stackoverflow.com/questions/17910322/android-ble-api-gatt-notification-not-received
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
}
// end: bleIO_service.java (Originally: DeviceScanActivity.java)
