package com.example.altbeaconscanner;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class BeaconsListAdapter extends RecyclerView.Adapter<BeaconsListAdapter.MyViewHolder> {
    public static final int ENTRY_TTL = 20000;

    List<ManufacturerData> dataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public LinearLayout layout;

        public MyViewHolder(LinearLayout v) {
            super(v);
            layout = v;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public BeaconsListAdapter(List<ManufacturerData> dataset) {
        this.dataset = dataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public BeaconsListAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        LinearLayout layout = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new MyViewHolder(layout);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        TextView textView = holder.layout.findViewById(R.id.hexText);
        textView.setText(dataset.get(position).hex);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return dataset.size();
    }

    void newScannedItem(byte[] beaconData) {
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

        notifyDataSetChanged();
    }
}