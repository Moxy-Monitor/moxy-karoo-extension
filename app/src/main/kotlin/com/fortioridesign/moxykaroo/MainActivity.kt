package com.fortioridesign.moxykaroo

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.fortioridesign.moxykaroo.ble.BleManager
import com.fortioridesign.moxykaroo.screens.MainScreen
import com.fortioridesign.moxykaroo.theme.AppTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val bleManager: BleManager by inject()

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bleManager.requestPermissions(this)

        setContent {
            AppTheme {
                MainScreen(this) {
                    finish()
                }
            }
        }
    }
}
