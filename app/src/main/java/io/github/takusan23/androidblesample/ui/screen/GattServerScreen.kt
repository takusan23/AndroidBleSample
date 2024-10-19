package io.github.takusan23.androidblesample.ui.screen

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.util.UUID

const val GATT_SERVER_SERVICE_UUID = "6b9f2474-71de-4528-a1ad-07322d7a28fa" // 適当に UUID.randomUUID() した結果
const val GATT_SERVER_CHARACTERISTICS_UUID = "fcfeed22-55e4-4e41-aeef-45e10ae4bf3c"

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun GattServerScreen() {
    val context = LocalContext.current

    DisposableEffect(key1 = Unit) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        var bleGattServer: BluetoothGattServer? = null
        bleGattServer = bluetoothManager.openGattServer(context, object : BluetoothGattServerCallback() {

            override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
                super.onConnectionStateChange(device, status, newState)
                Toast.makeText(context, "onConnectionStateChange device=$device / status=$status / newState=$newState", Toast.LENGTH_SHORT).show()
            }

            // read 要求がされたら返す
            override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
                bleGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, "Hello World".toByteArray(Charsets.UTF_8))
            }

        })
        //サービスとキャラクタリスティックを作る
        val gattService = BluetoothGattService(UUID.fromString(GATT_SERVER_SERVICE_UUID), BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val gattCharacteristics = BluetoothGattCharacteristic(
            UUID.fromString(GATT_SERVER_CHARACTERISTICS_UUID),
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        // サービスに Characteristic を入れる
        gattService.addCharacteristic(gattCharacteristics)
        // GATT サーバーにサービスを追加
        bleGattServer.addService(gattService)

        // アドバタイジング。これがないと見つけてもらえない
        val advertiseSettings = AdvertiseSettings.Builder().apply {
            setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            setTimeout(0)
        }.build()
        val advertiseData = AdvertiseData.Builder().apply {
            addServiceUuid(ParcelUuid.fromString(GATT_SERVER_SERVICE_UUID))
            // setIncludeDeviceName(true) onStartFailure() で 1 になる
        }.build()
        // アドバタイジング開始
        val bleAdvertiser = bluetoothManager.adapter.bluetoothLeAdvertiser
        val advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
                Toast.makeText(context, "onStartSuccess", Toast.LENGTH_SHORT).show()
            }

            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
                ADVERTISE_FAILED_DATA_TOO_LARGE
                Toast.makeText(context, "onStartFailure $errorCode", Toast.LENGTH_SHORT).show()
            }
        }
        bleAdvertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)

        // 破棄時
        onDispose {
            bleGattServer.close()
            bleAdvertiser.stopAdvertising(advertiseCallback)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(text = "BleGattServer") }) }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            Text(text = "GATT サーバーです")
        }
    }
}