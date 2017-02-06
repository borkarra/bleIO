// bleIO_main.java (Originally: DeviceScanActivity.java)

//PR: Notes
//  "//le" prefix reference code from Android Sample BluetoothLeGatt
//  "//t"  prefix reference code from BLE Tutorial

// Keywords:
// final    -   This reference can't be changed after defined (final value)
// volatile -   For variables that may change outside main code, i.e. Hardware Inputs & Interrupt Flags
// @override-   Replaces parent class function (allows detect of misspelling)

// BLE:
//  Tutorial:    https://developer.android.com/guide/topics/connectivity/bluetooth-le.html
//  OriginalApp: https://developer.android.com/samples/BluetoothLeGatt/index.html
//  Connected/Paired/Bonded:
//      Connected:  have a radio channel and can share data
//      Paired:     know each other, have a link key, link can be encrypted (Auto after connect in Android)
//      Bonded:     a retained list of previously paired devices


/*
 *
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
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

// Find bleDevices TODO: Save a list for Quick Connect, Only scan upon command
public class bleIO_main extends ListActivity {
    //private static final String     LOGTAG = "bleIO_main";
    private static final String     LOGTAG = bleIO_main.class.getSimpleName();
    private LeDeviceListAdapter     mLeDeviceListAdapter;
    private BluetoothAdapter        mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private static final int        REQUEST_ENABLE_BT = 1;
    private static final long       SCAN_PERIOD = 10000;    // Stops scanning after 10 seconds.

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(LOGTAG, "onCreate()");

        super.onCreate(savedInstanceState);
        getActionBar().setTitle(R.string.title_devices);
        mHandler = new Handler();

        // Test Debug: Log shows in AndroidStudio EventLog Window during normal running
        Log.i(LOGTAG,   "Log.v Verbose  - Debug Log Test");
        Log.v(LOGTAG,   "Log.d Debug    - Debug Log Test");
        Log.i(LOGTAG,   "Log.i Info     - Debug Log Test");
        Log.w(LOGTAG,   "Log.w Warn     - Debug Log Test");
        Log.e(LOGTAG,   "Log.e Error    - Debug Log Test");
        Log.wtf(LOGTAG, "Log.wtf Assert - Debug Log Test");

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.i(LOGTAG, "onCreate(finish: Bluetooth Not Feature)");
            Toast.makeText(this, R.string.ble_error_NotFeature, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Init BLE, API18+
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Log.i(LOGTAG, "onCreate(finish: Bluetooth Not Supported)");
            Toast.makeText(this, R.string.ble_error_NotSupported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //x // Is Bluetooth Enabled? ==> This exits app without prompting to enable Bluetooth
        //x if (!mBluetoothAdapter.isEnabled()) {
        //x     Toast.makeText(this, "Enable Bluetooth before starting App", Toast.LENGTH_SHORT).show();
        //x     Log.w(LOGTAG, "Enable Bluetooth before starting App");
        //x     finish(); // Clean Exit of App
        //x }

        Log.i(LOGTAG, "onCreate(done)");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(LOGTAG, "onCreateOptionsMenu()");
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                Log.i(LOGTAG, "onOptionsItemSelected(scan)");
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                Log.i(LOGTAG, "onOptionsItemSelected(stopscan)");
                scanLeDevice(false);
                break;
            default:
                Log.i(LOGTAG, "onOptionsItemSelected(**unknown**)");
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(LOGTAG, "onResume()");

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);//Maybe matches to onActivityResult() ??
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) { //From: startActivityForResult()
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            Log.i(LOGTAG, "onActivityResult(User denied Bluetooth)");
            finish();
            return;
        } else {
            Log.i(LOGTAG, "onActivityResult(User Enabled Bluetooth)");
        }
        if(mBluetoothAdapter.isEnabled()) {Log.i(LOGTAG, " BLE Enabled");}

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(LOGTAG, "onPause()");
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Log.i(LOGTAG, "onListItemClick()");
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        final Intent intent = new Intent(this, bleIO_list.class);
        intent.putExtra(bleIO_list.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(bleIO_list.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback); //TODO
            mScanning = false;
        }
        startActivity(intent);  //Spawn a different Activity?
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            Log.i(LOGTAG, "scanLeDevice(enable)");
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback); //TODO
                    invalidateOptionsMenu(); Log.i(LOGTAG, ">invalidateOptionsMenu()"); //Force refresh of ActionBar?
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback); //TODO: --> startScan(List, ScanSettings, ScanCallback)
        } else {
            Log.i(LOGTAG, "scanLeDevice(disable)");
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback); //TODO: --> stopScan(ScanCallback)
        }
        invalidateOptionsMenu(); Log.i(LOGTAG, ">invalidateOptionsMenu()"); //Force refresh of ActionBar?
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            Log.i(LOGTAG, "Class::LeDeviceListAdapter:LeDeviceListAdapter()");
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = bleIO_main.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            Log.i(LOGTAG, "LeDeviceListAdapter:addDevice()");
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            Log.i(LOGTAG, "LeDeviceListAdapter:getDevice()");
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            Log.i(LOGTAG, "LeDeviceListAdapter:getView()");
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }//private class LeDeviceListAdapter extends BaseAdapter

    // Device scan callback.
    //TODO: -->startScan(List, ScanSettings, ScanCallback)
    //TODO: -->stopScan(ScanCallback)
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i(LOGTAG, "mLeScanCallback()");
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}//public class bleIO_main extends ListActivity

// end: // bleIO_main.java (Originally: DeviceScanActivity.java)
