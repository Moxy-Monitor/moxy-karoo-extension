package com.fortioridesign.moxykaroo.screens

enum class SensorButton(val className: String, val buttonText: String) {
    SCAN(
        "io.hammerhead.sensorsapp.sensorSearch.SensorSearchActivity",
        "Scan"
    ),
    SETTINGS(
        "io.hammerhead.sensorsapp.sensorList.SensorListActivity",
        "Open Sensor List"
    )
}