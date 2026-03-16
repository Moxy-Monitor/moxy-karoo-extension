package com.fortioridesign.moxykaroo.screens

import android.app.Activity
import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryUnknown
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.DisabledByDefault
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fortioridesign.moxykaroo.ble.BleManager
import com.fortioridesign.moxykaroo.KarooSystemServiceProvider
import com.fortioridesign.moxykaroo.MoxyMonitorConnectionState
import com.fortioridesign.moxykaroo.R
import com.fortioridesign.moxykaroo.SensorBodyPosition
import com.fortioridesign.moxykaroo.getBatteryLevelLabel
import io.hammerhead.karooext.models.BatteryStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.time.Instant

fun getDeviceIndexLabel(index: Int): String {
    return when (index) {
        0 -> "1st"
        1 -> "2nd"
        2 -> "3rd"
        3 -> "4th"
        else -> "${index + 1}th"
    }
}

@Composable
fun getBatteryIcon(batteryStatus: BatteryStatus?): androidx.compose.ui.graphics.vector.ImageVector {
    return when (batteryStatus) {
        BatteryStatus.NEW -> Icons.Default.BatteryFull
        BatteryStatus.GOOD -> Icons.Default.Battery6Bar
        BatteryStatus.OK -> Icons.Default.Battery4Bar
        BatteryStatus.LOW -> Icons.Default.Battery2Bar
        BatteryStatus.CRITICAL -> Icons.Default.Battery1Bar
        else -> Icons.Default.BatteryUnknown
    }
}

fun getBatteryColor(batteryStatus: BatteryStatus?): Color {
    return when (batteryStatus) {
        BatteryStatus.NEW, BatteryStatus.GOOD -> Color.Green
        BatteryStatus.OK -> Color.Yellow
        BatteryStatus.LOW -> Color(0xFFFF9800) // Orange
        BatteryStatus.CRITICAL -> Color.Red
        else -> Color.Gray
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(activity: Activity, onFinish: () -> Unit) {
    val fontSize = 18.sp
    val ctx = LocalContext.current
    val karooSystem = koinInject<KarooSystemServiceProvider>()
    var karooSystemConnectionPeriodElapsed by remember { mutableStateOf(false) }
    val bleManager = koinInject<BleManager>()
    val karooConnected by karooSystem.connectionStateFlow.collectAsStateWithLifecycle(false)
    val connectionState by bleManager.deviceConnectionStatus.collectAsStateWithLifecycle(null)
    val hasBlePermissions by bleManager.observeBlePermissions().collectAsStateWithLifecycle(false)

    // Animation states for each item
    val logoAlpha = remember { Animatable(0f) }
    val logoOffsetY = remember { Animatable(50f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(50f) }
    val buttonAlpha = remember { Animatable(0f) }
    val buttonOffsetY = remember { Animatable(50f) }

    @Composable
    fun ScanDeviceButton(button: SensorButton) {
        OutlinedButton(
            onClick = {
                // Navigate to scan screen
                ctx.startActivity(
                    Intent().setClassName(
                        "io.hammerhead.sensorsapp",
                        button.className
                    )
                )
            },
            border = BorderStroke(1.dp, Color.White),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            modifier = Modifier
                .alpha(buttonAlpha.value)
                .offset(y = buttonOffsetY.value.dp)
                .fillMaxWidth(),
            content = { Text(button.buttonText, fontSize = fontSize) }
        )
    }

    val appState by remember { derivedStateOf {
        if (!karooSystemConnectionPeriodElapsed || connectionState == null) {
            AppState.LOADING
        } else if (!karooConnected) {
            AppState.KAROO_NOT_CONNECTED
        } else if (!hasBlePermissions) {
            AppState.PERMISSIONS_NOT_GRANTED
        } else if (connectionState?.isEmpty() == true) {
            AppState.NONE_PAIRED
        } else {
            AppState.RUNNING
        }
    } }

    LaunchedEffect(Unit) {
        delay(500L)
        karooSystemConnectionPeriodElapsed = true
    }

    // Trigger animations on composition
    LaunchedEffect(Unit) {
        launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, delayMillis = 0)
            )
        }
        launch {
            logoOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 600, delayMillis = 0)
            )
        }
        launch {
            textAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, delayMillis = 150)
            )
        }
        launch {
            textOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 600, delayMillis = 150)
            )
        }
        launch {
            buttonAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, delayMillis = 300)
            )
        }
        launch {
            buttonOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 600, delayMillis = 300)
            )
        }
    }

    Box(Modifier
        .fillMaxSize()
        .background(colorResource(R.color.background))) {
        if (appState != AppState.RUNNING) {
            Image(
                painter = painterResource(id = R.drawable.moxydevice),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                contentScale = ContentScale.FillWidth
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                colorResource(id = R.color.background)
                            ),
                            startY = with(LocalDensity.current) { 210.dp.toPx() },
                            endY = with(LocalDensity.current) { 240.dp.toPx() }
                        )
                    )
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(start = 10.dp, end = 10.dp, top = 220.dp, bottom = 10.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top)
            ) {

                @Composable
                fun TextLabel(text: String) {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = fontSize,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .alpha(textAlpha.value)
                    )
                }

                when (appState) {
                    AppState.NONE_PAIRED -> {
                        TextLabel("No Moxy Monitors paired.")

                        ScanDeviceButton(SensorButton.SCAN)
                    }

                    AppState.PERMISSIONS_NOT_GRANTED -> {
                        TextLabel("Missing permissions for Bluetooth communication.")

                        OutlinedButton(
                            onClick = {
                                bleManager.requestPermissions(activity)
                            },
                            border = BorderStroke(1.dp, Color.White),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier
                                .alpha(buttonAlpha.value)
                                .offset(y = buttonOffsetY.value.dp),
                            content = { Text("Grant", fontSize = fontSize) }
                        )
                    }

                    AppState.LOADING -> {
                        CircularProgressIndicator(modifier = Modifier.padding(top = 10.dp))
                    }

                    AppState.KAROO_NOT_CONNECTED -> {
                        TextLabel("Failed to connect to Karoo System.")
                    }

                    else -> {}
                }
            }

        } else {
            val coroutineScope = rememberCoroutineScope()

            Column(Modifier
                .fillMaxSize()
                .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 10.dp)
                .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top)) {
                Text("Sensors", color = Color.White, fontSize = fontSize)

                connectionState?.entries?.sortedBy { (_, device) -> device.deviceIndex }?.forEach { (scanResult, device) ->
                    key(scanResult.address) {
                        val deviceName = device.name ?: "Unknown Device"

                        val borderColor = when (device.connectionState) {
                            MoxyMonitorConnectionState.CONNECTED -> Color.Green
                            MoxyMonitorConnectionState.CONNECTING, MoxyMonitorConnectionState.SEARCHING -> Color.Yellow
                            MoxyMonitorConnectionState.DISCONNECTED -> Color.Red
                            MoxyMonitorConnectionState.DISABLED -> Color.Gray
                        }

                        var menuExpanded by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(1.dp, borderColor), shape = RoundedCornerShape(20.dp))
                                .padding(start = 20.dp, top = 10.dp, bottom = 10.dp, end = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Top row with device info
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Connection state icon on the left
                                    val (connectionIcon, iconColor) = when (device.connectionState) {
                                        MoxyMonitorConnectionState.CONNECTED -> Icons.Default.BluetoothConnected to Color.Green
                                        MoxyMonitorConnectionState.SEARCHING -> Icons.Default.Refresh to Color.Yellow
                                        MoxyMonitorConnectionState.CONNECTING -> Icons.Default.Refresh to Color.Yellow
                                        MoxyMonitorConnectionState.DISCONNECTED -> Icons.Default.Error to Color.Red
                                        MoxyMonitorConnectionState.DISABLED -> Icons.Default.DisabledByDefault to Color.Gray
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(0.dp),
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .border(
                                                BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        val indexLabel = getDeviceIndexLabel(device.deviceIndex)

                                        Text(
                                            text = indexLabel,
                                            color = Color.White,
                                            fontSize = fontSize,
                                        )

                                        Icon(
                                            imageVector = connectionIcon,
                                            contentDescription = "Connection Status",
                                            tint = iconColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = deviceName,
                                            color = Color.White,
                                            fontSize = fontSize,
                                        )

                                        Text(
                                            text = device.bodyPosition.displayName,
                                            color = Color.White,
                                            fontSize = fontSize,
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        IconButton(
                                            onClick = { menuExpanded = true }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Select Position",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }

                                // Bottom row with battery, smo2, and thb values
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Battery level column
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = getBatteryIcon(device.batteryLevel),
                                            contentDescription = "Battery Status",
                                            tint = getBatteryColor(device.batteryLevel),
                                            modifier = Modifier.size(20.dp).rotate(90f)
                                        )
                                        Text(
                                            text = getBatteryLevelLabel(device.batteryLevel),
                                            color = Color.White,
                                            fontSize = fontSize
                                        )
                                    }

                                    val hasRecentData = device.lastSensorDataReceivedAt?.isAfter(Instant.now().minusSeconds(10)) == true

                                    // SMO2 value column
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "SmO₂",
                                            color = Color.Gray,
                                            fontSize = 12.sp,
                                            modifier = Modifier.height(20.dp)
                                        )
                                        Text(
                                            text = if (hasRecentData && device.smo2 != null) "%.1f%%".format(device.smo2) else "---",
                                            color = Color.White,
                                            fontSize = fontSize
                                        )
                                    }

                                    // THB value column
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "tHb",
                                            color = Color.Gray,
                                            fontSize = 12.sp,
                                            modifier = Modifier.height(20.dp)
                                        )
                                        Text(
                                            text = if (hasRecentData && device.thb != null) "%.1f".format(device.thb) else "---",
                                            color = Color.White,
                                            fontSize = fontSize
                                        )
                                    }
                                }
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(colorResource(R.color.background))
                            ) {
                                SensorBodyPosition.entries.forEach { position ->
                                    DropdownMenuItem(
                                        text = { Text(position.displayName, color = Color.White) },
                                        onClick = {
                                            coroutineScope.launch {
                                                bleManager.updateDeviceState(scanResult) { it?.copy(bodyPosition = position) }
                                                menuExpanded = false
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                ScanDeviceButton(SensorButton.SETTINGS)
            }
        }

        Image(
            painter = painterResource(id = R.drawable.back),
            contentDescription = "Back",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 10.dp)
                .size(54.dp)
                .clickable {
                    onFinish()
                }
        )
    }
}
