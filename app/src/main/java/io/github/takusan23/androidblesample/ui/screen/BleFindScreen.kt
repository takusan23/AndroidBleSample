package io.github.takusan23.androidblesample.ui.screen

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleFindScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val scanDeviceList = remember { mutableStateOf(emptyList<BluetoothDevice>()) }

    DisposableEffect(key1 = Unit) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothLeScanner = bluetoothManager.adapter.bluetoothLeScanner
        val bleScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                scanDeviceList.value += result?.device ?: return
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Toast.makeText(context, "onScanFailed", Toast.LENGTH_SHORT).show()
            }
        }

        // UUID 指定して検索
        val scanFilter = ScanFilter.Builder().apply {
            setServiceUuid(ParcelUuid.fromString(GATT_SERVER_SERVICE_UUID))
        }.build()

        // 指定時間後にスキャン停止。電池がもったいない
        scope.launch {
            bluetoothLeScanner.startScan(
                listOf(scanFilter),
                ScanSettings.Builder().build(),
                bleScanCallback
            )
            delay(10_000)
            bluetoothLeScanner.stopScan(bleScanCallback)
        }
        onDispose { bluetoothLeScanner.stopScan(bleScanCallback) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(text = "BLE 検索") }) }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            LazyColumn {
                items(scanDeviceList.value) { device ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                device.connectGatt(context, false, object : BluetoothGattCallback() {

                                    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                                        super.onConnectionStateChange(gatt, status, newState)
                                        println("onConnectionStateChange status=$newState")
                                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                                            // 接続できたらサービスを探す
                                            gatt?.discoverServices()
                                        }
                                    }

                                    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                                        super.onServicesDiscovered(gatt, status)
                                        println("onServicesDiscovered")
                                        val findService = gatt?.services?.first { it.uuid == UUID.fromString(GATT_SERVER_SERVICE_UUID) }
                                        val findCharacteristic = findService?.characteristics?.first { it.uuid == UUID.fromString(GATT_SERVER_CHARACTERISTICS_UUID) }
                                        // GATT サーバーへ狙ったサービス内にあるキャラクタリスティックへ read を試みる
                                        gatt?.readCharacteristic(findCharacteristic)
                                    }

                                    override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
                                        super.onCharacteristicRead(gatt, characteristic, value, status)
                                        println(value.toString(Charsets.UTF_8))
                                        // 閉じる
                                        gatt.close()
                                    }
                                })
                            }
                    ) {
                        Text(text = device.name ?: "不明な名前", fontSize = 20.sp)
                        Text(text = device.address ?: "不明なアドレス")
                        Text(text = device.uuids?.joinToString(separator = " / ") { it.uuid.toString() } ?: "不明なUUID")
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}