package jp.android.blethermometer;

import java.util.HashMap;
import java.util.UUID;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
class GattAttributes {
    private static final HashMap<UUID, String> attributes = new HashMap();

    // Services.
    public static final UUID GENERIC_ACCESS_SERVICE = UUID
            .fromString("00001800-0000-1000-8000-00805f9b34fb");
    public static final UUID GENERIC_ATTRIBUTE_SERVICE = UUID
            .fromString("00001801-0000-1000-8000-00805f9b34fb");
    public static final UUID HEALTH_THERMOMETER_SERVICE = UUID
            .fromString("00001809-0000-1000-8000-00805f9b34fb");

    // Characteristics.
    public static final UUID HEALTH_THERMOMETER_MEASUREMENT = UUID
            .fromString("00002a1c-0000-1000-8000-00805f9b34fb");

    // Descriptors.
    public static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");

    static {
        // Services.
        attributes.put(GENERIC_ACCESS_SERVICE, "Generic Access Service");
        attributes.put(GENERIC_ATTRIBUTE_SERVICE, "Generic Attribute Service");
        attributes.put(HEALTH_THERMOMETER_SERVICE, "Health Thermometer Service");

        // Characteristics.
        attributes.put(HEALTH_THERMOMETER_MEASUREMENT, "Health Thermometer Measurement");

        // Descriptors.
        attributes.put(CLIENT_CHARACTERISTIC_CONFIGURATION, "Client Characteristic Configuration");
    }

    public static String lookup(UUID uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}