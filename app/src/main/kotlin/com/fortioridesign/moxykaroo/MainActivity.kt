/*
 * Copyright 2026 Fortiori Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
