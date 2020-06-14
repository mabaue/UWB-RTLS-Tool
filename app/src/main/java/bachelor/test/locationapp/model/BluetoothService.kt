package bachelor.test.locationapp.model

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import java.util.*

private const val TAG_MAC = "F0:74:2F:98:DE:90"
private const val GET_LOCATION_CHARACTERISTIC = "003BBDF2-C634-4B3D-AB56-7EC889B89A37"
private const val SET_LOCATION_MODE_CHARACTERISTIC = "A02B947E-DF97-4516-996A-1882521E0EAD"
private const val DESCRIPTOR = "00002902-0000-1000-8000-00805F9B34FB"
private const val MTU_SIZE = 33
private val POSITION_MODE = byteArrayOf(0x00)
private val DISTANCE_MODE = byteArrayOf(0x01)
private val POSITION_DISTANCE_MODE = byteArrayOf(0x02)

class BluetoothService(private val model: ModelImpl) {

    private var tagConnection: BluetoothGatt? = null
    private var tagIsConnected = true
    private var locationMode = DISTANCE_MODE

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = model.context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    fun initialize(){
        // Ensures Bluetooth is available on the device and it is enabled.
        bluetoothAdapter?.takeIf { it.isDisabled }?.apply {
            model.onBluetoothNotEnabled()
            bluetoothAdapter?.enable()
            return
        }
        scanLeDevice()
    }

    fun terminate(): Boolean{
        if (tagIsConnected){
            tagConnection?.disconnect()
            tagConnection?.close()
            tagConnection = null
            return true
        }
        return false
    }

    private fun scanLeDevice() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner

        println("Scan started")
        scanner?.startScan(object: ScanCallback(){
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                val device = result?.device
                if (device?.address == TAG_MAC){
                    println("Found tag")
                    scanner.stopScan(this)
                    device.connectGatt(model.context, false, gattCallback)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                println("Scan failed")
                println(errorCode.toString())
            }
        })
    }

    // Various callback methods defined by the BLE API.
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    println("Connected to tag")
                    tagConnection = gatt
                    tagIsConnected = true
                    tagConnection!!.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    tagIsConnected = false
                    tagConnection = null
                    model.onDisconnectionSuccess(true)
                }
            }
        }

        // New services discovered
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    println("Services discovered")
                    // Set connection MTU to 30 bytes (default is 20 only)
                    gatt.requestMtu(MTU_SIZE)
                }
                else -> {
                    // Unsuccessful service discovery
                    model.onConnectionSuccess(false)
                    println("No services discovered")
                }
            }
        }

        // Check if the position mode set in 'onServicesDiscovered' was successful
        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int){
            if (characteristic?.uuid == UUID.fromString(SET_LOCATION_MODE_CHARACTERISTIC) && status == BluetoothGatt.GATT_SUCCESS){
                if (characteristic?.value!!.contentEquals(locationMode)) {
                    model.onConnectionSuccess(true)
                }
            }
        }

        // Remote characteristic changes handling
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            model.onCharacteristicChange(characteristic!!.value)
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            if(mtu == MTU_SIZE && status == BluetoothGatt.GATT_SUCCESS){
                println("Success")
                val setLocationModeCharacteristic = gatt!!.services[2].getCharacteristic(UUID.fromString(SET_LOCATION_MODE_CHARACTERISTIC))
                // Set Position Mode to retrieve 14 byte array of position data
                setLocationModeCharacteristic.value = POSITION_MODE
                // Set Distance Mode to retrieve 30 byte array of distance data (when 4 anchors available)
                //setLocationModeCharacteristic.value = DISTANCE_MODE
                // Set Position and Distance Mode to retrieve big byte array of data
                //setLocationModeCharacteristic.value = POSITION_DISTANCE_MODE

                locationMode = setLocationModeCharacteristic.value
                val success = gatt.writeCharacteristic(setLocationModeCharacteristic)
                if (!success){
                    model.onConnectionSuccess(false)
                }
            }
        }
    }

    fun enableNotifications(){
        if (tagIsConnected){
            val characteristic = tagConnection!!.services[2].getCharacteristic(UUID.fromString(GET_LOCATION_CHARACTERISTIC))
            tagConnection?.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(UUID.fromString(DESCRIPTOR))
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            tagConnection?.writeDescriptor(descriptor)
        }
        else{
            initialize()
            enableNotifications()
        }
    }

    fun disableNotifications(){
        if (tagIsConnected){
            val characteristic = tagConnection!!.services[2].getCharacteristic(UUID.fromString(GET_LOCATION_CHARACTERISTIC))
            tagConnection?.setCharacteristicNotification(characteristic, false)
            val descriptor = characteristic.getDescriptor(UUID.fromString(DESCRIPTOR))
            descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            tagConnection?.writeDescriptor(descriptor)
        }
    }
}