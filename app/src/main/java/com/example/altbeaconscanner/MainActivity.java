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

    private Disposable scanning;
    private ArrayList<ManufacturerData> dataset;
    private BeaconsListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }

        RecyclerView recyclerView = (RecyclerView) this.findViewById(R.id.recycler_view);
        // use a linear layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        dataset = new ArrayList<>();
        adapter = new BeaconsListAdapter(dataset);
        recyclerView.setAdapter(adapter);

        ((Button) this.findViewById(R.id.button_scan_start)).setOnClickListener(view -> {
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

                                // Get data by EIR type. "manufacturer data" is FF
                                // https://www.bluetooth.com/specifications/assigned-numbers/generic-access-profile/
                                byte[] beaconData = scanData.getParsedAdvertisement().getEIRData(0xFF);
                                if (beaconData != null) {
                                    updateDataSet(beaconData);
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