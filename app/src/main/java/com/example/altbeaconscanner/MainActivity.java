package com.example.altbeaconscanner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.uber.rxcentralble.Scanner;
import com.uber.rxcentralble.core.CoreScannerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import io.reactivex.disposables.Disposable;

public class MainActivity extends AppCompatActivity {

    public static final int ENTRY_TTL = 20000;
    public static final int UBER_BLUETOOTH_MFG_ID = 0x0415;

    private Disposable scanning;
    private ArrayList<ManufacturerData> dataset;
    private BeaconsListAdapter adapter;

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

        RecyclerView recyclerView = (RecyclerView) this.findViewById(R.id.recycler_view);
        // use a linear layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        dataset = new ArrayList<>();
        adapter = new BeaconsListAdapter(dataset);
        recyclerView.setAdapter(adapter);

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
                                // Received some Bluetooth packet

                                // Take only data placed in the beacon by Uber using Uber's manufacturer code
                                byte[] uberBeaconData = scanData.getParsedAdvertisement().getManufacturerData(UBER_BLUETOOTH_MFG_ID);
                                if (uberBeaconData != null) {
                                    // AltBeacon starts with 0xBEAC
                                    if (uberBeaconData[0] == (byte)0xBE && uberBeaconData[1] == (byte)0xAC) {
                                        updateDataSet(uberBeaconData);
                                    }
                                }
                            },
                            ex -> {
                                Log.e("scan_data", "Error" + ex);
                            }
                    );

        });
        ((Button) this.findViewById(R.id.button_scan_stop)).setOnClickListener(view -> {
            if (scanning != null) {
                scanning.dispose();
                scanning = null;
                Toast.makeText(this, "Stopped scanning", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateDataSet(byte[] beaconData) {
        HashSet<ManufacturerData> set = new HashSet<>(dataset);

        ManufacturerData newManufacturerData = new ManufacturerData(beaconData);
        if (!dataset.contains(newManufacturerData)) {
            Log.e("scan_data", "Detected new: " + newManufacturerData.hex);
        }

        // Remove old entries
        Date secondAgo = new Date(new Date().getTime() - ENTRY_TTL);
        for (Iterator<ManufacturerData> iterator = set.iterator(); iterator.hasNext(); ) {
            ManufacturerData manufacturerData = iterator.next();
            if (manufacturerData.registered.before(secondAgo)) {
                iterator.remove();
            }
        }

        set.remove(newManufacturerData);
        set.add(newManufacturerData);

        dataset.clear();
        dataset.addAll(set);
        Collections.sort(dataset, (i1, i2) -> i1.hex.compareTo(i2.hex));

        adapter.notifyDataSetChanged();
    }
}