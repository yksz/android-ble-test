package jp.android.blethermometer;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

class BluetoothLeUtils {
    private static final String TAG = BluetoothLeUtils.class.getSimpleName();

    public static boolean changeGattMtu(BluetoothGatt gatt, int mtu) {
        int retry = 5;
        boolean ok = false;
        while (!ok && retry > 0) {
            ok = gatt.requestMtu(mtu);
            retry--;
        }
        return ok;
    }

    public static boolean enableNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        return setNotification(gatt, characteristic, true);
    }

    public static boolean disableNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        return setNotification(gatt, characteristic, false);
    }

    private static boolean setNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                           boolean enabled) {
        if (gatt == null) {
            throw new NullPointerException("gatt must not be null");
        }
        if (characteristic == null) {
            throw new NullPointerException("characteristic must not be null");
        }

        gatt.setCharacteristicNotification(characteristic, enabled);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                GattAttributes.CLIENT_CHARACTERISTIC_CONFIGURATION);
        if (descriptor == null) {
            Log.w(TAG, "Descriptor not found: uuid=" + GattAttributes.CLIENT_CHARACTERISTIC_CONFIGURATION);
            return false;
        }
        if (enabled) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        return gatt.writeDescriptor(descriptor);
    }

    public static boolean enableIndication(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        return setIndication(gatt, characteristic, true);
    }

    public static boolean disableIndication(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        return setIndication(gatt, characteristic, false);
    }

    private static boolean setIndication(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                         boolean enabled) {
        if (gatt == null) {
            throw new NullPointerException("gatt must not be null");
        }
        if (characteristic == null) {
            throw new NullPointerException("characteristic must not be null");
        }

        gatt.setCharacteristicNotification(characteristic, enabled);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                GattAttributes.CLIENT_CHARACTERISTIC_CONFIGURATION);
        if (descriptor == null) {
            Log.w(TAG, "Descriptor not found: uuid=" + GattAttributes.CLIENT_CHARACTERISTIC_CONFIGURATION);
            return false;
        }
        if (enabled) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        } else {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        return gatt.writeDescriptor(descriptor);
    }
}
