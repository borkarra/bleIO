// bleIO_list.java (originally: DeviceControlActivity.java)

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

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SeekBar;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code bleIO_service}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class bleIO_list extends Activity {
    private final static String LOGTAG = bleIO_list.class.getSimpleName();
    private Context mContextList = null;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private TextView mDataField1;
    private TextView mDataField2;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private bleIO_service m_bleIO_service; // Holds link to bleIO_service
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    public  static BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mCharacteristic_w8= null;
    private byte[] pbCharacteristicValue_w8 = {10, 20, 01, (byte) 0xA0, 02, (byte) 0xB0, 03, (byte) 0xC0}; // Set Both LEDs to Dim (Note High bytes last)

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    //private SeekBar mSeekBarL1; //widget id: seekBarL1
    //private SeekBar mSeekBarL2; //widget id: seekBarL2

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            m_bleIO_service = ((bleIO_service.LocalBinder) service).getService();
            if (!m_bleIO_service.initialize()) {
                Log.e(LOGTAG, "onServiceConnected(Unable to initialize Bluetooth)");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            m_bleIO_service.connect(mDeviceAddress);
            Log.i(LOGTAG, "onServiceConnected(ok)");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(LOGTAG, "onServiceDisconnected(ok)");
            m_bleIO_service = null;
        }
    };// private final ServiceConnection mServiceConnection = new ServiceConnection()

    // If not listed here then won't be received by BroadcastReceiver()==mGattUpdateReceiver()
    private static IntentFilter makeGattUpdateIntentFilter() {
        Log.i(LOGTAG, "makeGattUpdateIntentFilter()");
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(bleIO_service.ACTION_GATT_CONNECTED);
        intentFilter.addAction(bleIO_service.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(bleIO_service.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(bleIO_service.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(bleIO_service.ACTION_HRM_AVAILABLE);
        intentFilter.addAction(bleIO_service.ACTION_w8_AVAILABLE);
        intentFilter.addAction(bleIO_service.ACTION_r4_AVAILABLE);
        intentFilter.addAction(bleIO_service.ACTION_n4_AVAILABLE);
        return intentFilter;
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read or notification operations.
    // ACTION_***_AVAILABLE: receive custom read or notify data
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.i(LOGTAG, "BroadcastReceiver:onReceive()[" + action + "]");

            //TODO Switch (action)
            switch (action) {
                case bleIO_service.ACTION_GATT_CONNECTED:
                    mConnected = true;
                    updateConnectionState(R.string.connected);
                    invalidateOptionsMenu();
                    Log.i(LOGTAG, " >invalidateOptionsMenu()"); //Force refresh of ActionBar?
                    break;

                case bleIO_service.ACTION_GATT_DISCONNECTED:
                    mConnected = false;
                    updateConnectionState(R.string.disconnected);
                    invalidateOptionsMenu();
                    Log.i(LOGTAG, " >invalidateOptionsMenu()"); //Force refresh of ActionBar?
                    clearUI();
                    break;

                case bleIO_service.ACTION_GATT_SERVICES_DISCOVERED:
                    //Log.i(LOGTAG, "BroadcastReceiver:onReceive(ACTION_GATT_SERVICES_DISCOVERED)");
                    // Show all the supported services and characteristics on the user interface.
                    displayGattServices(m_bleIO_service.getSupportedGattServices());
                    break;

                case bleIO_service.ACTION_DATA_AVAILABLE:
                    //Log.i(LOGTAG, "BroadcastReceiver:onReceive(ACTION_DATA_AVAILABLE)");
                    displayData(intent.getStringExtra(bleIO_service.EXTRA_DATA));
                    break;

                case bleIO_service.ACTION_HRM_AVAILABLE:
                    //Log.i(LOGTAG, "BroadcastReceiver:onReceive(ACTION_HRM_AVAILABLE)");
                    displayData(intent.getStringExtra(bleIO_service.EXTRA_DATA));
                    break;

                case bleIO_service.ACTION_w8_AVAILABLE:
                    //Log.i(LOGTAG, "BroadcastReceiver:onReceive(ACTION_w8_AVAILABLE)");
                    //displayData(intent.getStringExtra(bleIO_service.EXTRA_DATA));
                    display_w8(intent.getByteArrayExtra(bleIO_service.EXTRA_DATA));
                    break;

                case bleIO_service.ACTION_r4_AVAILABLE:
                    //Log.i(LOGTAG, "BroadcastReceiver:onReceive(ACTION_r4_AVAILABLE)");
                    //displayData(intent.getStringExtra(bleIO_service.EXTRA_DATA));
                    display_r4(intent.getByteArrayExtra(bleIO_service.EXTRA_DATA));
                    break;

                case bleIO_service.ACTION_n4_AVAILABLE:
                    //Log.i(LOGTAG, "BroadcastReceiver:onReceive(ACTION_n4_AVAILABLE)");
                    //displayData(intent.getStringExtra(bleIO_service.EXTRA_DATA));
                    display_n4(intent.getByteArrayExtra(bleIO_service.EXTRA_DATA));
                    break;

                default:
                    Log.i(LOGTAG, "BroadcastReceiver:onReceive(**Unknown Action**)[" + action + "]");
                    break;
            }
        }
    };// private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver()

    //######################### Read&Set Multiple Characteristics?
    // Ref: http://stackoverflow.com/questions/27136904/subscribe-to-a-characteristic-and-catch-the-value-android
    //  // Add in Device Activity Class...
    //  // make sure this method is called when there is more than one characteristic to read & set
    //  private void read_Characteristic()
    //  {
    //      BluetoothLeService.read(mWriteCharacteristic.element());
    //      BluetoothLeService.set(mWriteCharacteristic.element(),true);
    //      mWriteCharacteristic.remove();
    //  };
    //##########################

    // Select Characteristic, appropriately process Read and Notify properties, TODO: Process Write property(s)
    // Reference: http://developer.android.com/reference/android/bluetooth/BluetoothGatt.html
    // return(xx): the return value doesn't appear to make much difference in calling function.
    private final ExpandableListView.OnChildClickListener servicesListClickListner = new ExpandableListView.OnChildClickListener() {
        @Override
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
            if (mGattCharacteristics == null) {
                Log.i(LOGTAG, "OnChildClickListener:onChildClick(no characteristics)");
                return false; //Click not processed since no characteristic
            }

            //Log.d(LOGTAG, "OnChildClickListener:onChildClick(with characteristics)");
            final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(groupPosition).get(childPosition);
            final int charaProp = characteristic.getProperties();

            if ((charaProp & (BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY)) > 0) { //Characteristic has Read or Notify?
                //Log.i(LOGTAG, "=Characteristic.(Read|Notify)["+charaProp+"d] Mask:[" + (BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY) + "d]");

                // Disable Notification on any previous different selection with Read or Notify so not mixed with new selection
                // **BUT** Some applications may desire multiple simultaneous Notifications enabled
                if (mNotifyCharacteristic != null && mNotifyCharacteristic != characteristic) { // Was there a different Characteristic with Notify Enabled?
                    Log.i(LOGTAG, "OnChildClickListener:onChildClick()=Clearing previous Notify when enabling new Read or Notify");
                    //m_bleIO_service.setCharacteristicNotificationDisable(mNotifyCharacteristic); // Disable Notify on Previous selection
                    m_bleIO_service.setCharacteristicNotification(mNotifyCharacteristic, false);
                    mNotifyCharacteristic = null;

                    //Don't enable further action till Notify Disable is completed, User can click again.
                    if (mContextList != null) {
                        Toast.makeText(mContextList, "Disabling Previous Selection, Please Click Again", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(LOGTAG, "**mContextList==null while Clearing previous Notify when enabling new Read or Notify**");
                    }

                    return (true);// Processed click, but user needs to click again since this action consumed disabling previous Notify Selection
                }
            }

            //TODO: Note: Moved in front of READ only as a test till button/slider created
            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) { //TODO: Process Write Characteristics
                Log.i(LOGTAG, "OnChildClickListener:onChildClick()=Characteristic.PropertyWrite==TODO[" + charaProp + "] Mask:[" + BluetoothGattCharacteristic.PROPERTY_WRITE + "]");//TODO: Write
                //Log.i(LOGTAG, "OnChildClickListener:onChildClick()=Characteristic.PropertyWrite==TODO[0x%02x] Mask:[0x%02x]", charaProp, BluetoothGattCharacteristic.PROPERTY_WRITE );//TODO: Write

                {//####TODO:  This writes device ok! Temporary test: When click on Writable Characteristic, if is w8 then send preset test data:
                    byte[] pw8 = {10, 20, 01, (byte) 0xA0, 02, (byte) 0xB0, 03, (byte) 0xC0}; // Set Both LEDs to Dim (Note High bytes last)
                    characteristic.setValue(pw8);    //w8 should be 8bytes or long64bit
                    m_bleIO_service.writeCharacteristic(characteristic); // this function displays its own debug before going to parent class function
                    Log.i(LOGTAG, "OnChildClickListener:onChildClick(Set w8, will see return on read w8)");//TODO: Write
                    //display_w8(pw8);
                    //Fallthrough to allow Read?
                }
            }

            //** Note special handling when Read & Notify both set. Some devices set only Notify, while Others set for Immediate Read, doing both same time can cause one to be lost (overwritten or already busy)

            // Process Read Property
            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) { //Readable?  // TODO: Report Bug of | vs &
                Log.i(LOGTAG, "OnChildClickListener:onChildClick()=Characteristic.PropertyRead[" + charaProp + "d] Mask:[" + BluetoothGattCharacteristic.PROPERTY_READ + "d]");
                m_bleIO_service.readCharacteristic(characteristic);
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) { //Notify still need to be set?
                    //x mNotifyCharacteristicToSet = characteristic;//Save for Enabling Notify when receive Read Reply.
                    Log.i(LOGTAG, "OnChildClickListener:onChildClick(Notify Queued till receive Read Reply)=Characteristic.PropertyRead[" + charaProp + "d] Mask:[" + BluetoothGattCharacteristic.PROPERTY_READ + "d]");
                }
                return (true);// Processed click, but need to check if Notify setting when get read response (If do Read+Notify same time then second one can get lost)
            }

            //Process Notify right now if Read Property wasn't set (If try doing both read&notify at same time the second will be flakey)
            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) { //Notify-able?   // Enable Notify on new selection // Bug of | vs &, see #2 at: https://code.google.com/p/android/issues/detail?id=58979
                Log.i(LOGTAG, "OnChildClickListener:onChildClick()=Characteristic.PropertyNotify[" + charaProp + "d] Mask:[" + BluetoothGattCharacteristic.PROPERTY_NOTIFY + "d]");
                m_bleIO_service.setCharacteristicNotification(characteristic, true);
                mNotifyCharacteristic = characteristic; //Log that Notify has been set for this characteristic

                //TODO: Let this complete before starting next action
                return (true);// Processed click
            }

            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) { //TODO: Process Write Characteristics
                Log.i(LOGTAG, "OnChildClickListener:onChildClick()=Characteristic.PROPERTY_WRITE_NO_RESPONSE==TODO[" + charaProp + "] Mask:[" + BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE + "]");//TODO: Write
            }

            //String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
            //String pMessage = (mNotifyCharacteristicActive==null)?"-none-":(bleIO_UUID.lookup(mNotifyCharacteristicActive.getUuid().toString(),unknownCharaString));
            //Log.i(LOGTAG, "**Notify==["+(pMessage)+"]**");

            Log.w(LOGTAG, "**OnChildClickListener:onChildClick(**NothingProcessed??)**");

            //return true; //Processed click
            return false; //Nothing Processed
        }
    };//    private final ExpandableListView.OnChildClickListener servicesListClickListner = new ExpandableListView.OnChildClickListener()

    private void clearUI() {
        Log.i(LOGTAG, "clearUI()");
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField1.setText(R.string.no_data);
        mDataField2.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(LOGTAG, "onCreate(List)");
        mContextList = this;
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField1 = (TextView) findViewById(R.id.data_value1);
        mDataField2 = (TextView) findViewById(R.id.data_value2);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        Intent gattServiceIntent = new Intent(this, bleIO_service.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE); //TODO: What's this?

        final SeekBar mSeekBarL1 = (SeekBar) findViewById(R.id.seekBarL1); //Find seekBarL1, Max=100=InXML
        final TextView mSeekBarL1value = (TextView) findViewById(R.id.valueL1);
        //mSeekBarL1.setMax(100); // Set in xml
        //mSeekBarL1.setEnabled(false); //TODO: Disable bar movement in code until BLE Stabilized
        mSeekBarL1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { //Ref: http://android-helper.blogspot.ca/2011/05/android-simple-seekbar-example.html
            //x private int iProgress=10;
            public void onProgressChanged(SeekBar pSeekBar, int iProgress, boolean bFromUser) {//TODO: Consider moving into onStopTrackingTouch() for less communications
                //if(iProgressL1 != iProgress){}
                if (mCharacteristic_w8 == null){Log.e(LOGTAG, "**setOnSeekBarChangeListener.onProgressChanged(L1=null)**"); return;}
                mSeekBarL1value.setText(String.format("%d", iProgress).toString());
                pbCharacteristicValue_w8[0] = (byte) iProgress;  //L1 = seekBar Value
                mCharacteristic_w8.setValue(pbCharacteristicValue_w8);    //w8 should be 8bytes or long64bit
                m_bleIO_service.writeCharacteristic(mCharacteristic_w8); // this function displays its own debug before going to parent class function
                Log.i(LOGTAG, "setOnSeekBarChangeListener:onProgressChanged(w8:L1=" + iProgress + ")");
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
        });

        final SeekBar mSeekBarL2 = (SeekBar) findViewById(R.id.seekBarL2); //Find seekBarL2, Max=100=InXML
        final TextView mSeekBarL2value = (TextView) findViewById(R.id.valueL2);
        //mSeekBarL2.setMax(100); // Set in xml
        //mSeekBarL2.setEnabled(false); //TODO: Disable bar movement in code until BLE Stabilized
        mSeekBarL2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { //Ref: http://android-helper.blogspot.ca/2011/05/android-simple-seekbar-example.html
            //x private int iProgress=10;
            public void onProgressChanged(SeekBar pSeekBar, int iProgress, boolean bFromUser) { //TODO: Consider moving into onStopTrackingTouch() for less communications
                //if(iProgressL2 != iProgress){}
                if (mCharacteristic_w8 == null){Log.e(LOGTAG, "**setOnSeekBarChangeListener.onProgressChanged(L2=null)**"); return;}
                mSeekBarL2value.setText(String.format("%d", iProgress).toString());
                pbCharacteristicValue_w8[1] = (byte) iProgress;  //L2 = seekBar Value
                mCharacteristic_w8.setValue(pbCharacteristicValue_w8);    //w8 should be 8bytes or long64bit
                m_bleIO_service.writeCharacteristic(mCharacteristic_w8); // this function displays its own debug before going to parent class function
                Log.i(LOGTAG, "setOnSeekBarChangeListener:onProgressChanged(w8:L2=" + iProgress + ")");
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
        });

        // Option: Set initial UI state
    }//    public void onCreate(Bundle savedInstanceState) { //


    @Override
    protected void onResume() {
        super.onResume();
        Log.i(LOGTAG, "onResume()");
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (m_bleIO_service != null) {
            final boolean result = m_bleIO_service.connect(mDeviceAddress);
            Log.i(LOGTAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(LOGTAG, "onPause()");
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(LOGTAG, "onDestroy()");
        unbindService(mServiceConnection);
        m_bleIO_service = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            Log.i(LOGTAG, "onCreateOptionsMenu(Connected)");
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            Log.i(LOGTAG, "onCreateOptionsMenu(Disconnected)");
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(LOGTAG, "onOptionsItemSelected()[" + item + "]");
        switch (item.getItemId()) {
            case R.id.menu_connect:
                Log.i(LOGTAG, "onOptionsItemSelected(Connect)");
                m_bleIO_service.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                Log.i(LOGTAG, "onOptionsItemSelected(Disconnect)");
                m_bleIO_service.disconnect();
                return true;
            case android.R.id.home:
                Log.i(LOGTAG, "onOptionsItemSelected(Home)");
                onBackPressed();
                return true;
        }
        Log.i(LOGTAG, "onOptionsItemSelected(unknown or standard)");
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        Log.i(LOGTAG, "updateConnectionState(resourceId)[" + resourceId + "]");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
        Log.i(LOGTAG, "displayData()[" + data + "]");
        if (data != null) {
            mDataField1.setText(data);
        }
        mDataField2.setText(R.string.no_data);
    }

    private void display_w8(byte[] pbytes) { //In case w8 had Read property enabled
        mDataField1.setText("w8");
        if (pbytes == null) {
            Log.w(LOGTAG, "display_w8:pbytes==NULL");
            mDataField2.setText("w8:Data2==NULL");
            return;
        }
        if (pbytes.length == 0) {
            Log.w(LOGTAG, "display_w8:pbytes.length==0");
            mDataField2.setText("w8:Data2==0Len");
            return;
        }

        Log.i(LOGTAG, "display_w8: pbytes=>Hex");
        final StringBuilder sString2 = new StringBuilder(pbytes.length);
        for (byte pbyteN : pbytes) {
            sString2.append(String.format("%02X ", pbyteN));
        }
        mDataField2.setText(pbytes.length + "[" + sString2.toString() + "]");

        // Combine to 64bit so easier to separate out parts (20141228PR: I'm sure I'll learn a better way to do this as I become better with Java)
        // Android: Long=64bit, int=32bit, short=16bit
        long lData = (((long) pbytes[7] & 0xFF) << 56 | ((long) pbytes[6] & 0xFF) << 48 | ((long) pbytes[5] & 0xFF) << 40 | ((long) pbytes[4] & 0xFF << 32)
                | ((long) pbytes[3] & 0xFF) << 24 | ((long) pbytes[2] & 0xFF) << 16 | ((long) pbytes[1] & 0xFF) << 8 | ((long) pbytes[0] & 0xFF)); // Extract 64bit in forced byte order to match hardware (With sign extension suppression)
        final StringBuilder sString1 = new StringBuilder(100); //Ensure enough length
        sString1.append(String.format("w8: 0x%016x", lData));
        mDataField1.setText(sString1);

        // No "unsigned" types in Java, so be careful of sign extension, ">>>>" == shift without sign extension, Use L on constants to force as Long so no signed extension on any bits (B7,B15,B23)
        int iL1pwm100 = (int) ((lData >> 0) & 0x7FL);      // LED1 Brightness 0~100% (7bit)
        int iL2pwm255 = (int) ((lData >> 8) & 0xFFL);      // LED2 Brightness 0~255  (8bit)
        int iX0 = (int) ((lData >> 16) & 0xFFFFL);   // Spare (16bit)
        int iX1 = (int) ((lData >> 32) & 0xFFFFL);   // Spare (16bit)
        int iX2 = (int) ((lData >> 48) & 0xFFFFL);   // Spare (16bit)
        // Warning: Depending on Interrupt settings in device the w8 status may be old, as Read Action doesn't trigger a fresh port read in device
        sString1.append(String.format("\nL1.%d  L2.%d\nSpare.%04x.%04x.%04x", iL1pwm100, iL2pwm255, iX0, iX1, iX2));
        mDataField1.setText(sString1);
        //TODO: Add Exception Catch here for String.format conversion errors/typos
        /*{ try {
                String cmdString = "led2 " + progress / SLIDER_MAX;
                value = cmdString.getBytes("UTF-8");
                mService.writeRXCharacteristic(value);
            } catch (UnsupportedEncodingException e) {
                handle error by outputting alternate dummy string;
            }
        }*/
    }

    private void display_r4(byte[] pbytes) {
        mDataField1.setText("r4");
        if (pbytes == null) {
            Log.w(LOGTAG, "display_r4:pbytes==NULL");
            mDataField2.setText("r4:Data2==NULL");
            return;
        }
        if (pbytes.length == 0) {
            Log.w(LOGTAG, "display_r4:pbytes.length==0");
            mDataField2.setText("r4:Data2==0Len");
            return;
        }

        Log.i(LOGTAG, "display_r4: pbytes=>Hex");
        final StringBuilder sString2 = new StringBuilder(pbytes.length);
        for (byte pbyteN : pbytes) {
            sString2.append(String.format("%02X ", pbyteN));
        }
        mDataField2.setText(pbytes.length + "[" + sString2.toString() + "]");

        // Combine to 64bit so easier to separate out parts (20141228PR: I'm sure I'll learn a better way to do this as I become better with Java)
        // Android: Long=64bit, int=32bit, short=16bit
        long lData = (((long) pbytes[3] & 0xFF) << 24 | ((long) pbytes[2] & 0xFF) << 16 | ((long) pbytes[1] & 0xFF) << 8 | ((long) pbytes[0] & 0xFF)); // Extract 64bit in forced byte order to match hardware (With sign extension suppression)
        final StringBuilder sString1 = new StringBuilder(100); //Ensure enough length
        sString1.append(String.format("r4: 0x%08x", lData));
        mDataField1.setText(sString1);

        // No "unsigned" types in Java, so be careful of sign extension, ">>>>" == shift without sign extension, Use L on constants to force as Long so no signed extension on any bits (B7,B15,B23)
        boolean bB1p = (((lData & 0x01L) > 0));          //Button 1 Status (1=Pressed?)
        boolean bB2p = (((lData & 0x02L) > 0));          //Button 2 Status (1=Pressed?)
        int iB1p = (int) ((lData >> 2) & 0x3FL);      //Count Button1 Presses (6bit)
        int iB1r = (int) ((lData >> 8) & 0xFFL);      //Count Button1 Releases (8bit)
        int iB2p = (int) ((lData >> 16) & 0xFFL);     //Count Button2 Presses  (8bit)
        int iB2r = (int) ((lData >> 24) & 0xFFL);     //Count Button2 Releases (8bit)
        // Warning: Depending on Interrupt settings in device the r4 Button status may be old, as Read Action doesn't trigger a fresh button port read in device
        sString1.append(String.format("\n[spr] B1.%c.%d.%d  B2.%c.%d.%d", (bB1p ? 'p' : 'r'), iB1p, iB1r, (bB2p ? 'p' : 'r'), iB2p, iB2r));// To debug conversion errors split to separate String.format per element
        mDataField1.setText(sString1);
        //TODO: Add Exception Catch here for String.format conversion errors/typos
    }

    private void display_n4(byte[] pbytes) {
        mDataField1.setText("n4");
        if (pbytes == null) {
            Log.w(LOGTAG, "display_n4:pbytes==NULL");
            mDataField2.setText("n4:Data2==NULL");
            return;
        }
        if (pbytes.length == 0) {
            Log.w(LOGTAG, "display_n4:pbytes.length==0");
            mDataField2.setText("n4:Data2==0Len");
            return;
        }

        Log.i(LOGTAG, "display_n4: pbytes=>Hex");
        final StringBuilder sString2 = new StringBuilder(pbytes.length);
        for (byte pbyteN : pbytes) {
            sString2.append(String.format("%02X ", pbyteN));
        }
        mDataField2.setText(pbytes.length + "[" + sString2.toString() + "]");

        // Combine to 64bit so easier to separate out parts (20141228PR: I'm sure I'll learn a better way to do this as I become better with Java)
        // Android: Long=64bit, int=32bit, short=16bit
        long lData = (((long) pbytes[3] & 0xFF) << 24 | ((long) pbytes[2] & 0xFF) << 16 | ((long) pbytes[1] & 0xFF) << 8 | ((long) pbytes[0] & 0xFF)); // Extract 64bit in forced byte order to match hardware (With sign extension suppression)
        final StringBuilder sString1 = new StringBuilder(100); //Ensure enough length
        sString1.append(String.format("n4: 0x%08x", lData));
        mDataField1.setText(sString1);

        // No "unsigned" types in Java, so be careful of sign extension, ">>>>" == shift without sign extension, Use L on constants to force as Long so no signed extension on any bits (B7,B15,B23)
        boolean bB1p = (((lData & 0x01L) > 0));          //Button 1 Status (1=Pressed?)
        boolean bB2p = (((lData & 0x02L) > 0));          //Button 2 Status (1=Pressed?)
        int iB1p = (int) ((lData >> 2) & 0x3FL);      //Count Button1 Presses (6bit)
        int iB2r = (int) ((lData >> 8) & 0xFFL);      //Count Button2 Releases (8bit)
        int iTempC100 = (int) ((lData >> 16) & 0xFFFFL);   // Temperature in hundreths of 'C (i.e. float = iTempC10/100)
        float fTempC = (iTempC100);
        fTempC /= 100.0;   // Temperature in 'C
        // Warning: Direct read of Notify Characteristic may have old data, as first Read Action doesn't trigger a fresh button port read in device (while Notifies from device should be fresh data)
        sString1.append(String.format("\nB1.%c B2.%c B1p.%d B2r.%d T:%.2f'C", (bB1p ? 'p' : 'r'), (bB2p ? 'p' : 'r'), iB1p, iB2r, fTempC));// To debug conversion errors split to separate String.format per element
        mDataField1.setText(sString1);
        //TODO: Add Exception Catch here for String.format conversion errors/typos
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        Log.i(LOGTAG, "displayGattServices(...)");
        if (gattServices == null) return;
        String uuid = null;
        //String unknownServiceString = getResources().getString(R.string.unknown_service);
        //String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            Log.i(LOGTAG, "displayGattServices(service...)");
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME, bleIO_UUID.lookup(uuid));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                Log.i(LOGTAG, "displayGattServices(characteristics...)");
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(LIST_NAME, bleIO_UUID.lookup(uuid));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);

                {   // Record specific characteristics for later writing to: i.e. w8
                    if (bleIO_UUID.UUID_bleIO_w8.equals(gattCharacteristic.getUuid()))  { mCharacteristic_w8=gattCharacteristic; }//Save for later writing to
                    //Alternate Option: Directly get the specific Characteristics:
                    // BluetoothGattService bse = gatt.getService(TRANSFER_SERVICE_UUID);
                    // BluetoothGattCharacteristic bgc = bse.getCharacteristic(TRANSFER_CHARACTERISTIC_UUID);
                }
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(//Build the list for output like a printf??
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2},
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2}
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }
}
// end: bleIO_list.java (originally: DeviceControlActivity.java)
