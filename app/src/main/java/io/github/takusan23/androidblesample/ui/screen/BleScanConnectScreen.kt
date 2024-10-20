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
import android.os.Build
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleScanConnectScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val bluetoothGatt = remember { mutableStateOf<BluetoothGatt?>(null) }
    val serviceUuid = remember { mutableStateOf(DEFAULT_GATT_SERVER_SERVICE_UUID) }
    val characteristicUuid = remember { mutableStateOf(DEFAULT_GATT_SERVER_CHARACTERISTICS_UUID) }
    val readRequestTextList = remember { mutableStateOf(emptyList<String>()) }
    val writeRequestText = remember { mutableStateOf(Build.MODEL) }

    fun findDeviceAndConnectService() {
        scope.launch {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

            // BluetoothDevice が見つかるまで一時停止
            val bluetoothDevice: BluetoothDevice? = suspendCoroutine { continuation ->
                val bluetoothLeScanner = bluetoothManager.adapter.bluetoothLeScanner
                val bleScanCallback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult?) {
                        super.onScanResult(callbackType, result)
                        // 見つけたら返して、スキャンも終了させる
                        continuation.resume(result?.device)
                        bluetoothLeScanner.stopScan(this)
                    }

                    override fun onScanFailed(errorCode: Int) {
                        super.onScanFailed(errorCode)
                        continuation.resume(null)
                    }
                }

                // GATT サーバーのサービス UUID を指定して検索を始める
                val scanFilter = ScanFilter.Builder().apply {
                    setServiceUuid(ParcelUuid.fromString(serviceUuid.value))
                }.build()
                bluetoothLeScanner.startScan(
                    listOf(scanFilter),
                    ScanSettings.Builder().build(),
                    bleScanCallback
                )
            }

            if (bluetoothDevice == null) {
                Toast.makeText(context, "デバイスが見つかりませんでした", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "デバイスを見つけました", Toast.LENGTH_SHORT).show()
            }

            // BLE デバイスを見つけたら、GATT サーバーへ接続
            bluetoothDevice?.connectGatt(context, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                    super.onConnectionStateChange(gatt, status, newState)
                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> gatt?.discoverServices() // 接続できたらサービスを探す
                        BluetoothProfile.STATE_DISCONNECTED -> bluetoothGatt.value = null // なくなった
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                    super.onServicesDiscovered(gatt, status)
                    // サービスが見つかったら GATT サーバーに対して操作ができるはず
                    // サービスとキャラクタリスティックを探して、read する
                    bluetoothGatt.value = gatt
                }

                // onCharacteristicReadRequest で送られてきたデータを受け取る
                override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
                    super.onCharacteristicRead(gatt, characteristic, value, status)
                    readRequestTextList.value += value.toString(Charsets.UTF_8)
                }
            })
        }
    }

    fun readCharacteristic() {
        val gatt = bluetoothGatt.value ?: return
        // GATT サーバーへ狙ったサービス内にあるキャラクタリスティックへ read を試みる
        val findService = gatt.services?.first { it.uuid == UUID.fromString(serviceUuid.value) }
        val findCharacteristic = findService?.characteristics?.first { it.uuid == UUID.fromString(characteristicUuid.value) }
        // 結果は onCharacteristicRead で
        gatt.readCharacteristic(findCharacteristic)
    }

    fun writeCharacteristic() {
        val gatt = bluetoothGatt.value ?: return
        // GATT サーバーへ狙ったサービス内にあるキャラクタリスティックへ read を試みる
        val findService = gatt.services?.first { it.uuid == UUID.fromString(serviceUuid.value) } ?: return
        val findCharacteristic = findService.characteristics?.first { it.uuid == UUID.fromString(characteristicUuid.value) } ?: return
        // 結果は onCharacteristicWriteRequest で
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(findCharacteristic, writeRequestText.value.toByteArray(Charsets.UTF_8), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        }
    }

    fun closeGatt() {
        bluetoothGatt.value?.close()
        bluetoothGatt.value = null
    }

    DisposableEffect(key1 = Unit) {
        onDispose { closeGatt() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(text = "BLE Scan + Connect") }) }
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
                            text = "GATT サーバーへ接続する",
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
                        Button(onClick = { findDeviceAndConnectService() }) {
                            Text(text = "BLE スキャン+サービス+キャラクタリスティック 開始")
                        }
                        Button(onClick = { closeGatt() }) {
                            Text(text = "終了")
                        }
                    }
                }
            }

            // gatt 無いなら return
            val gatt = bluetoothGatt.value ?: return@LazyColumn

            item {
                OutlinedCard(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .fillMaxWidth()
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text(
                            text = "GATT サーバー情報",
                            fontSize = 20.sp
                        )
                        Text(text = gatt.device.address)
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
                            text = "送信するデータ",
                            fontSize = 20.sp
                        )
                        OutlinedTextField(
                            value = writeRequestText.value,
                            onValueChange = { writeRequestText.value = it },
                            label = { Text(text = "送信する文字列") }
                        )
                        Button(onClick = { writeCharacteristic() }) {
                            Text(text = "WRITE")
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
                            text = "受信したデータ",
                            fontSize = 20.sp
                        )
                        Button(onClick = { readCharacteristic() }) {
                            Text(text = "READ")
                        }
                        HorizontalDivider()
                        readRequestTextList.value.forEach {
                            Text(text = it)
                        }
                    }
                }
            }
        }
    }
}