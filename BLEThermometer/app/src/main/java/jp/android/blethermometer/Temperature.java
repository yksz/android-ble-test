package jp.android.blethermometer;

class Temperature {
    enum Unit {
        UNKNOWN,
        CELSIUS,
        FAHRENHEIT,
    }

    final float value;
    final Unit unit;

    public Temperature(float value, Unit unit) {
        this.value = value;
        this.unit = unit;
    }
}
