package com.example.altbeaconscanner;

import java.util.Date;
import java.util.Objects;
import java.util.Random;

public class ManufacturerData {
    Date registered;
    String hex;

    public ManufacturerData(byte[] beaconData) {
        this.registered = new Date();
        this.hex = byteArrayToHex(beaconData);
    }

    public static String byteArrayToHex(byte[] array) {
        StringBuilder sb = new StringBuilder(array.length * 3);
        for (byte b : array) {
            sb.append(String.format("%02x ", b));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ManufacturerData manufacturerData = (ManufacturerData) o;
        return Objects.equals(hex, manufacturerData.hex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hex);
    }
}
