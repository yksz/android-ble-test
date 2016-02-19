package jp.android.blethermometer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final long SCAN_PERIOD = 10000; // Stops scanning after 10 seconds.
    private static final String SUPPORTED_DEVICE_NAME = "Thermometer";

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private ConnectionStatus mConnectionStatus = ConnectionStatus.DISCONNECTED;
    private GattStatus mGattStatus = GattStatus.NOT_AVAILABLE;
    private boolean mScanning;

    // View
    private TextView mTemperatureText;
    private TextView mConnectionStatusText;
    private TextView mGattStatusText;
    private Button mScanButton;
    private Button mUpdateButton;

    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();

        if (!isBleSupported()) {
            finish();
            return;
        }

        initView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScanningLeDevice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectBle();
    }

    private boolean isBleSupported() {
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.msg_error_ble_not_supported, Toast.LENGTH_SHORT).show();
            return false;
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.msg_error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void initView() {
        mTemperatureText = (TextView) findViewById(R.id.txtTemperature);
        mConnectionStatusText = (TextView) findViewById(R.id.txtConnectionStatus);
        mGattStatusText = (TextView) findViewById(R.id.txtGattStatus);
        mScanButton = (Button) findViewById(R.id.btnScan);
        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanLeDevice();
            }
        });
        mUpdateButton = (Button) findViewById(R.id.btnUpdate);
        mUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTemperature();
            }
        });
        mUpdateButton.setEnabled(false);
    }

    private void scanLeDevice() {
        if (mScanning) {
            stopScanningLeDevice();
        } else {
            startScanningLeDevice();
        }
    }

    private void startScanningLeDevice() {
        if (mScanning) {
            return;
        }

        // Stops scanning after a pre-defined scan period.
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScanningLeDevice();
            }
        }, SCAN_PERIOD);

        mScanning = true;
        mBluetoothAdapter.startLeScan(mLeScanCallback);
        mScanButton.setText(R.string.view_btn_scan_scanning);
    }

    private void stopScanningLeDevice() {
        if (!mScanning) {
            return;
        }

        mScanning = false;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        mScanButton.setText(R.string.view_btn_scan);
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.d(TAG, "Device: name=" + device.getName() + ", address=" + device.getAddress());
            if (device.getName() != null && device.getName().equals(SUPPORTED_DEVICE_NAME)) {
                stopScanningLeDevice();
                mBluetoothGatt = device.connectGatt(getApplicationContext(), false, mGattCallback);
                setConnectionStatus(ConnectionStatus.CONNECTING);
            }
        }
    };

    private void disconnectBle() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    private void setConnectionStatus(final ConnectionStatus status) {
        mConnectionStatus = status;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                switch (status) {
                    case CONNECTING:
                        mScanButton.setEnabled(false);
                        break;
                    case CONNECTED:
                        mScanButton.setEnabled(false);
                        mUpdateButton.setEnabled(true);
                        break;
                    case DISCONNECTED:
                        mScanButton.setEnabled(true);
                        mUpdateButton.setEnabled(false);
                        break;
                }
                mScanButton.setText(R.string.view_btn_scan);
                mConnectionStatusText.setText(status.toString());
            }
        });
    }

    private void updateTemperature() {
        if (mConnectionStatus == ConnectionStatus.DISCONNECTED || mGattStatus == GattStatus.NOT_AVAILABLE) {
            return;
        }

        if (mGattStatus == GattStatus.NOTIFICATION_ENABLED || mGattStatus == GattStatus.INDICATION_ENABLED) {
            stopUpdateTemperature();
        } else {
            startUpdateTemperature();
        }
    }

    private void startUpdateTemperature() {
        BluetoothGattCharacteristic characteristic = findCharacteristic();
        if (characteristic == null) {
            return;
        }
        int props = characteristic.getProperties();
        if (hasCharacteristicProperty(props, BluetoothGattCharacteristic.PROPERTY_NOTIFY)) {
            BluetoothLeUtils.enableNotification(mBluetoothGatt, characteristic);
            setGattStatus(GattStatus.NOTIFICATION_ENABLED);
        } else if (hasCharacteristicProperty(props, BluetoothGattCharacteristic.PROPERTY_INDICATE)) {
            BluetoothLeUtils.enableIndication(mBluetoothGatt, characteristic);
            setGattStatus(GattStatus.INDICATION_ENABLED);
        }
    }

    private void stopUpdateTemperature() {
        BluetoothGattCharacteristic characteristic = findCharacteristic();
        if (characteristic == null) {
            return;
        }
        int props = characteristic.getProperties();
        if (hasCharacteristicProperty(props, BluetoothGattCharacteristic.PROPERTY_NOTIFY)) {
            BluetoothLeUtils.disableNotification(mBluetoothGatt, characteristic);
        } else if (hasCharacteristicProperty(props, BluetoothGattCharacteristic.PROPERTY_INDICATE)) {
            BluetoothLeUtils.disableIndication(mBluetoothGatt, characteristic);
        }
        setGattStatus(GattStatus.SERVICES_DISCOVERED);
    }

    private boolean hasCharacteristicProperty(int properties, int property) {
        return (properties & property) == property;
    }

    private BluetoothGattCharacteristic findCharacteristic() {
        UUID serviceUuid = GattAttributes.HEALTH_THERMOMETER_SERVICE;
        UUID characteristicUuid = GattAttributes.HEALTH_THERMOMETER_MEASUREMENT;

        BluetoothGattService service = mBluetoothGatt.getService(serviceUuid);
        if (service == null) {
            Log.w(TAG, "Service not found: uuid=" + serviceUuid);
            return null;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
        if (characteristic == null) {
            Log.w(TAG, "Characteristic not found: uuid=" + characteristicUuid);
            return null;
        }
        return characteristic;
    }

    private void setGattStatus(final GattStatus status) {
        mGattStatus = status;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                switch (status) {
                    case NOTIFICATION_ENABLED:
                    case INDICATION_ENABLED:
                        mUpdateButton.setText(R.string.view_btn_update_stop);
                        break;
                    default:
                        mUpdateButton.setText(R.string.view_btn_update);
                        break;
                }
                mGattStatusText.setText(status.toString());
            }
        });
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
                setConnectionStatus(ConnectionStatus.CONNECTED);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
                setConnectionStatus(ConnectionStatus.DISCONNECTED);
                setGattStatus(GattStatus.NOT_AVAILABLE);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Changes the MTU size to 512 in case LOLLIPOP and above devices
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    BluetoothLeUtils.changeGattMtu(gatt, 512);
                }
                setGattStatus(GattStatus.SERVICES_DISCOVERED);
                logGattServices(gatt);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        private void logGattServices(BluetoothGatt gatt) {
            for (BluetoothGattService service : gatt.getServices()) {
                String serviceName = GattAttributes.lookup(service.getUuid(), "Unknown");
                Log.d(TAG, "+ Service: name=" + serviceName + ", uuid=" + service.getUuid());
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    String characteristicName = GattAttributes.lookup(characteristic.getUuid(), "Unknown");
                    Log.d(TAG, "- Characteristic: name=" + characteristicName + ", uuid=" + characteristic.getUuid());
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristic(characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            handleCharacteristic(characteristic);
        }

        private void handleCharacteristic(BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(GattAttributes.HEALTH_THERMOMETER_MEASUREMENT)) {
                Temperature temp = parseHealthThermometerMeasurement(characteristic);
                setTemperature(temp);
            }
        }
    };

    private Temperature parseHealthThermometerMeasurement(BluetoothGattCharacteristic characteristic) {
        float value = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 1);
        Temperature.Unit unit = Temperature.Unit.UNKNOWN;
        byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            byte flagByte = data[0];
            if ((flagByte & 0x01) != 0) {
                unit = Temperature.Unit.FAHRENHEIT;
            } else {
                unit = Temperature.Unit.CELSIUS;
            }
        }
        return new Temperature(value, unit);
    }

    private void setTemperature(final Temperature temp) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String unit = "";
                switch (temp.unit) {
                    case CELSIUS:
                        unit = getString(R.string.unit_celsius);
                        break;
                    case FAHRENHEIT:
                        unit = getString(R.string.unit_fahrenheit);
                        break;
                }
                mTemperatureText.setText(String.format("%3.1f", temp.value) + unit);
            }
        });
    }
}
