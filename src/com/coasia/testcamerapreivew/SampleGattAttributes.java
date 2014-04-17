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

package com.coasia.testcamerapreivew;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String UUID_SERVICE_GAP = "00001800-0000-1000-8000-00805f9b34fb";
    public static String UUID_SERVICE_GATT = "00001801-0000-1000-8000-00805f9b34fb";
    public static String UUID_SERVICE_HELLO = "1b7e8251-2877-41c3-b46e-cf057c562023";
    public static String UUID_SERVICE_BATTERY = "0000180f-0000-1000-8000-00805f9b34fb";
    public static String UUID_SERVICE_DEVICE_INFOMATION = "0000180a-0000-1000-8000-00805f9b34fb";
    
    public static String UUID_CHARACTERISTIC_DEVICE_NAME = "00002a00-0000-1000-8000-00805f9b34fb";
    public static String UUID_CHARACTERISTIC_APPEARANCE = "00002a01-0000-1000-8000-00805f9b34fb";
    
    public static String UUID_CHARACTERISTIC_HELLO_NOTIFY = "8ac32d3f-5cb9-4d44-bec2-ee689169f626";
    public static String UUID_CHARACTERISTIC_HELLO_CONFIG = "5e9bf2a8-f93f-4481-a67e-3b2f4a07891a";
    
    public static String UUID_CHARACTERISTIC_MANUFACTURE_NAME= "00002a29-0000-1000-8000-00805f9b34fb";
    public static String UUID_CHARACTERISTIC_MODEL_NUMBER= "00002a24-0000-1000-8000-00805f9b34fb";
    public static String UUID_CHARACTERISTIC_SYSTEM_ID= "00002a23-0000-1000-8000-00805f9b34fb";
    public static String UUID_CHARACTERISTIC_BATTERY_LEVEL= "00002a19-0000-1000-8000-00805f9b34fb";
    
    public static String UUID_DESCRIPTOR_HELLO_CONFIG= "00002902-0000-1000-8000-00805f9b34fb";

    static {
        // Sample Services.
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        
        attributes.put(UUID_SERVICE_GAP, "GAP service");
        attributes.put(UUID_SERVICE_GATT, "GATT service");
        attributes.put(UUID_SERVICE_HELLO, "Hello sensor service");
        attributes.put(UUID_SERVICE_BATTERY, "Battery service");        
        attributes.put(UUID_SERVICE_DEVICE_INFOMATION, "Device Information Service");
        // Sample Characteristics.
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        attributes.put(UUID_CHARACTERISTIC_MANUFACTURE_NAME, "Manufacturer Name String");
        
        attributes.put(UUID_CHARACTERISTIC_DEVICE_NAME, "Device name");
        attributes.put(UUID_CHARACTERISTIC_APPEARANCE, "Appearance");
        attributes.put(UUID_CHARACTERISTIC_HELLO_NOTIFY, "Hello sensor notify");
        attributes.put(UUID_CHARACTERISTIC_HELLO_CONFIG, "Hello sensor config");
        attributes.put(UUID_CHARACTERISTIC_MODEL_NUMBER, "Model");
        attributes.put(UUID_CHARACTERISTIC_SYSTEM_ID, "System Id");
        attributes.put(UUID_CHARACTERISTIC_BATTERY_LEVEL, "Battery LV");
        attributes.put(UUID_DESCRIPTOR_HELLO_CONFIG, "Hello config descriptor");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
