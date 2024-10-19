package io.github.takusan23.androidblesample.ui.screen

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

const val DEFAULT_GATT_SERVER_SERVICE_UUID = "6b9f2474-71de-4528-a1ad-07322d7a28fa" // 適当に UUID.randomUUID() した結果
const val DEFAULT_GATT_SERVER_CHARACTERISTICS_UUID = "fcfeed22-55e4-4e41-aeef-45e10ae4bf3c"

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun GattServerScreen() {
    val context = LocalContext.current

    var bleGattServer = remember<BluetoothGattServer?> { null }
    var bluetoothLeAdvertiser = remember<BluetoothLeAdvertiser?> { null }
    var advertiseCallback = remember<AdvertiseCallback?> { null }

    val connectDeviceList = remember { mutableStateOf(emptyList<BluetoothDevice>()) }
    val serviceUuid = remember { mutableStateOf(DEFAULT_GATT_SERVER_SERVICE_UUID) }
    val characteristicUuid = remember { mutableStateOf(DEFAULT_GATT_SERVER_CHARACTERISTICS_UUID) }
    val readRequestText = remember { mutableStateOf("Hello BLE") }

    fun stopGattServer() {
        bleGattServer?.close()
        bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback ?: return)
    }

    fun startGattServer() {
        stopGattServer()

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        bluetoothLeAdvertiser = bluetoothManager.adapter.bluetoothLeAdvertiser
        bleGattServer = bluetoothManager.openGattServer(context, object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
                super.onConnectionStateChange(device, status, newState)
                device ?: return
                when (newState) {
                    BluetoothProfile.STATE_DISCONNECTED -> connectDeviceList.value -= connectDeviceList.value.first { it.address == device.address }
                    BluetoothProfile.STATE_CONNECTED -> connectDeviceList.value += device
                }
            }

            // readCharacteristic が要求されたら呼ばれる
            override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
                val textToByteArray = readRequestText.value.toByteArray(Charsets.UTF_8)
                bleGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, textToByteArray)
            }
        })
        //サービスとキャラクタリスティックを作る
        val gattService = BluetoothGattService(UUID.fromString(serviceUuid.value), BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val gattCharacteristics = BluetoothGattCharacteristic(
            UUID.fromString(characteristicUuid.value),
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        // サービスに Characteristic を入れる
        gattService.addCharacteristic(gattCharacteristics)
        // GATT サーバーにサービスを追加
        bleGattServer?.addService(gattService)

        // アドバタイジング。これがないと見つけてもらえない
        val advertiseSettings = AdvertiseSettings.Builder().apply {
            setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            setTimeout(0)
        }.build()
        val advertiseData = AdvertiseData.Builder().apply {
            addServiceUuid(ParcelUuid.fromString(serviceUuid.value))
            // setIncludeDeviceName(true) onStartFailure() で 1 になるので呼ばない
        }.build()
        // アドバタイジング開始
        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
                Toast.makeText(context, "onStartSuccess", Toast.LENGTH_SHORT).show()
            }

            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
                Toast.makeText(context, "onStartFailure $errorCode", Toast.LENGTH_SHORT).show()
            }
        }
        bluetoothLeAdvertiser?.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
    }

    // 画面破棄時
    DisposableEffect(key1 = Unit) {
        onDispose { stopGattServer() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(text = "BleGattServer") }) }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            item {
                OutlinedCard(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .fillMaxWidth()
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text(
                            text = "GATT サーバー 設定",
                            fontSize = 20.sp
                        )
                        OutlinedTextField(
                            value = serviceUuid.value,
                            onValueChange = { serviceUuid.value = it },
                            label = { Text(text = "サービス UUID") }
                        )
                        OutlinedTextField(
                            value = characteristicUuid.value,
                            onValueChange = { characteristicUuid.value = it },
                            label = { Text(text = "キャラクタリスティック UUID") }
                        )
                        Button(onClick = { startGattServer() }) {
                            Text(text = "GATT サーバー開始")
                        }
                        Button(onClick = { stopGattServer() }) {
                            Text(text = "GATT サーバー終了")
                        }
                    }
                }
            }

            item {
                OutlinedCard(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .fillMaxWidth()
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text(
                            text = "キャラクタリスティック 設定",
                            fontSize = 20.sp
                        )
                        OutlinedTextField(
                            value = readRequestText.value,
                            onValueChange = { readRequestText.value = it },
                            label = { Text(text = "read で送り返す文字列") }
                        )
                    }
                }
            }

            item {
                OutlinedCard(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .fillMaxWidth()
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text(
                            text = "接続済みデバイス",
                            fontSize = 20.sp
                        )
                        connectDeviceList.value.forEach { device ->
                            Text(text = device.name ?: "不明なデバイス")
                            Text(text = device.address)
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}