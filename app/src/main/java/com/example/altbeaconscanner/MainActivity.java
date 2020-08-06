package com.example.altbeaconscanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.uber.rxcentralble.Scanner;
import com.uber.rxcentralble.core.CoreScannerFactory;

import java.util.ArrayList;

import io.reactivex.disposables.Disposable;

public class MainActivity extends AppCompatActivity {

    public static final int UBER_BLUETOOTH_MFG_ID = 0x0415;

    private Disposable scanning;
    private BeaconsListAdapter allBluetoothPackets;
    private BeaconsListAdapter uberAltBeacons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of proper permission request:
        // https://altbeacon.github.io/android-beacon-library/requesting_permission.html
        // I just request both FINE and COARSE location
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }
        }

        allBluetoothPackets = initRecyclerView(R.id.bluetooth_packets_list);
        uberAltBeacons = initRecyclerView(R.id.uber_alt_beacons_list);

        this.findViewById(R.id.button_scan_start).setOnClickListener(view -> {
            if (scanning != null) {
                Toast.makeText(this, "Already scanning", Toast.LENGTH_SHORT).show();
                return;
            }
            Scanner scanner = new CoreScannerFactory().produce();

            // Use the scanner to scan for advertising peripherals.
            scanning = scanner
                    .scan()
                    .subscribe(
                            scanData -> {
                                // Received some Bluetooth packet, show it on UI
                                byte[] manufacturerCodeAndData = scanData.getParsedAdvertisement().getRawAdvertisement();
                                allBluetoothPackets.newScannedItem(manufacturerCodeAndData);

                                // Check if packet is Uber's AltBeacon packet using Uber's manufacturer code and AltBeacon header
                                // 1. Take only the data placed in the beacon by Uber using Uber's manufacturer code
                                byte[] uberBeaconData = scanData.getParsedAdvertisement().getManufacturerData(UBER_BLUETOOTH_MFG_ID);
                                if (uberBeaconData != null) {
                                    // 2. AltBeacons start with 0xBEAC
                                    if (uberBeaconData[0] == (byte) 0xBE && uberBeaconData[1] == (byte) 0xAC) {
                                        uberAltBeacons.newScannedItem(uberBeaconData);
                                        // Just parse the byte array, here is the AltBeacon format:
                                        // https://stackoverflow.com/a/35869431/2424926
                                        // Note: format there starts with "m:2-3=beac" because bytes 0 and 1 are for manufacturer code
                                        // RxBLE strips the manufacturer code, so just subtract 2:
                                        // m:0-1=BEAC - header
                                        // i:2-17 - UUID
                                        // i:18-19 - major
                                        // i:20-21 - minor
                                        // p:24-24 - power level in 1 meter from device
                                        // d:25-25 - not used by us
                                    }
                                }
                            },
                            ex -> {
                                Log.e("scan_data", "Error" + ex);
                            }
                    );

        });
        this.findViewById(R.id.button_scan_stop).setOnClickListener(view -> {
            if (scanning != null) {
                scanning.dispose();
                scanning = null;
                Toast.makeText(this, "Stopped scanning", Toast.LENGTH_SHORT).show();
            }
        });
    }

    BeaconsListAdapter initRecyclerView(int id) {
        RecyclerView recyclerView = this.findViewById(id);
        // use a linear layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        ArrayList<ManufacturerData> dataset = new ArrayList<>();
        BeaconsListAdapter adapter = new BeaconsListAdapter(dataset);
        recyclerView.setAdapter(adapter);

        return adapter;
    }
}