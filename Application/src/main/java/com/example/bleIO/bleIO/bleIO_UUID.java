// bleIO_UUID.java (originally: SampleGattAttributes.java)

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

import android.util.Log;

import java.util.HashMap;
import java.util.UUID;

//==================== BLE UUID ====================
// This class includes GATT attributes by bleIO device
// Specifically listed UUID are used for Selecting Services and Characteristics for special support/decoding
public class bleIO_UUID { //bleIO_uuid
    private final static String LOGTAG = bleIO_UUID.class.getSimpleName();
    private static HashMap<String, String> attributes = new HashMap();
    public static String cUUID_ClientChar       = "00002902-0000-1000-8000-00805f9b34fb"; //CLIENT_CHARACTERISTIC_CONFIG (HRM, HTM, BATT, bleIO...)

    //HRM:
    private static String cUUID_HRM_Rate        = "00002a37-0000-1000-8000-00805f9b34fb"; //HEART_RATE_MEASUREMENT
    public final static UUID UUID_HRM_Rate = UUID.fromString(bleIO_UUID.cUUID_HRM_Rate);
    //HTM:
    private static String cUUID_HTM_Temp        = "00002a1c-0000-1000-8000-00805f9b34fb"; //Temperature Measurement
    public final static UUID UUID_HTM_Temp = UUID.fromString(bleIO_UUID.cUUID_HTM_Temp);
    //BATTERY:
    private static String cUUID_BATT_Level      = "00002a19-0000-1000-8000-00805f9b34fb"; //Battery Level
    public final static UUID UUID_BATT_Level = UUID.fromString(bleIO_UUID.cUUID_BATT_Level);

    //bleIO:
    //private static String cUUID_bleIO_base    = "4d3281c0-86d1-11e4-b084-0002a5d5c51b"; // May use as a mask for group
    private static String cUUID_bleIO_service   = "4d3281c0-86d1-11e4-b084-0002a5d5c510";
    private static String cUUID_bleIO_w8        = "4d3281c0-86d1-11e4-b084-0002a5d5c511";
    private static String cUUID_bleIO_r4        = "4d3281c0-86d1-11e4-b084-0002a5d5c512";
    private static String cUUID_bleIO_n4        = "4d3281c0-86d1-11e4-b084-0002a5d5c513";
    //public final static UUID UUID_bleIO_service = UUID.fromString(bleIO_UUID.cUUID_bleIO_service);
    public final static UUID UUID_bleIO_w8 = UUID.fromString(bleIO_UUID.cUUID_bleIO_w8);
    public final static UUID UUID_bleIO_r4 = UUID.fromString(bleIO_UUID.cUUID_bleIO_r4);
    public final static UUID UUID_bleIO_n4 = UUID.fromString(bleIO_UUID.cUUID_bleIO_n4);

    static { // This list is for simply looking up a readable description of the UUID

        //bleIO Service and Characteristics
        //attributes.put(cUUID_bleIO_base,    "bleIO base"); // Notes in Device's code
        attributes.put(cUUID_bleIO_service, "bleIO service");
        attributes.put(cUUID_bleIO_w8,      "bleIO w8");
        attributes.put(cUUID_bleIO_r4,      "bleIO r4");
        attributes.put(cUUID_bleIO_n4,      "bleIO n4");

        //x attributes.put("1bc5d5a5-0200-84b0-e411-d186c081324d", "bleIO rev base");
        //x attributes.put("10c5d5a5-0200-84b0-e411-d186c081324d", "bleIO rev Service");
        //x attributes.put("11c5d5a5-0200-84b0-e411-d186c081324d", "bleIO rev w8");
        //x attributes.put("12c5d5a5-0200-84b0-e411-d186c081324d", "bleIO rev r4");
        //x attributes.put("13c5d5a5-0200-84b0-e411-d186c081324d", "bleIO rev n4");

        // TODO: Add DFU for FOTA, Possibly add UARTService for Wireless Debug
        //x GattService::UUID_DFU,   //0x1530 - See UARTServiceShortUUID in BLE_API:DFUService.cpp  //
        //x GattService::UARTService,//0x0001~0x0003 - See DFUServiceShortUUID  in BLE_API:UARTService.cpp //

        //Standard Services and Characteristics (used only for testing bleIO)
        attributes.put("00000000-0000-1000-8000 00805F9B34FB", "Base for UUID16");

        attributes.put("00001800-0000-1000-8000-00805f9b34fb", "Generic Access");
        attributes.put("00001801-0000-1000-8000-00805f9b34fb", "Generic Attribute");
        attributes.put("00001809-0000-1000-8000-00805f9b34fb", "Health Thermometer");
          attributes.put(cUUID_HTM_Temp,                       "Temperature Measurement");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
          attributes.put(cUUID_HRM_Rate,                       "Heart Rate Measurement");
        attributes.put("0000180f-0000-1000-8000-00805f9b34fb", "Battery Service");
          attributes.put(cUUID_BATT_Level,                     "Battery Level");

        attributes.put("00001811-0000-1000-8000-00805f9b34fb", "Alert Notification");
        attributes.put("00001805-0000-1000-8000-00805f9b34fb", "Current Time");
        attributes.put("00001812-0000-1000-8000-00805f9b34fb", "HID");
        attributes.put("00001802-0000-1000-8000-00805f9b34fb", "Immediate Alert");
        attributes.put("00001803-0000-1000-8000-00805f9b34fb", "Link Loss");
        attributes.put("0000180e-0000-1000-8000-00805f9b34fb", "Phone Alert");
        attributes.put("00001806-0000-1000-8000-00805f9b34fb", "Reference Time");
        attributes.put("00001813-0000-1000-8000-00805f9b34fb", "Scan Parameters");
        attributes.put("0000a010-0000-1000-8000-00805f9b34fb", "Accelerometer");
        attributes.put("0000a011-0000-1000-8000-00805f9b34fb", "x-axis");
        attributes.put("0000a012-0000-1000-8000-00805f9b34fb", "y-axis");
        attributes.put("0000a013-0000-1000-8000-00805f9b34fb", "z-axis");
    }

    public static String lookup(String uuid) {
        String name = attributes.get(uuid);
        //if(name==null){ name = bleIO_main.getResources().getString(R.string.unknown_UUID);}
        if(name==null){ name = "Unknown UUID";}
        Log.v(LOGTAG, "lookup("+uuid+")==["+name+"]");
        return name;
    }
}

// end: bleIO_UUID.java (originally: SampleGattAttributes.java)


