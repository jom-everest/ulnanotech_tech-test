package com.example.slava.myapplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public class MainActivity extends AppCompatActivity {

    private ListView listView;
    private Button button;
    private TextView textView;
    private BluetoothAdapter bluetooth;

    ArrayList<String> rssiList = new ArrayList<String>();
    // adapter for rssiList
    ArrayAdapter<String> adapter;

    // true if scanning for bluetooth devices, false - not scan now
    private boolean isScanning = false;

    // Create a BroadcastReceiver for ACTION_FOUND and ACTION_DISCOVERY_FINISHED and ACTION_STATE_CHANGED
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.

/*
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
*/

                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);

                // from rssi and device name string is formed and added to rssiList
                rssiList.add(String.format("%02d dBm  %s  ", rssi, name));

                // sort rssiList by rssi
                Collections.sort(rssiList, new Comparator<String>() {
                    public int compare(String s1, String s2) {
                        return s1.substring(1,3).compareTo(s2.substring(1, 3));
                    }
                });
                adapter.notifyDataSetChanged();
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // Discovery has finished
                isScanning = false;
                button.setText("Сканировать");
                inform( rssiList.isEmpty() ? "Доступных устройств не обнаружено" : "");
            }
            else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                // Bluetooth state is changed.
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_TURNING_OFF) {
                    // Bluetooth adapter is turned off now
                    if (isScanning) {
                        bluetooth.cancelDiscovery();
                    }
                }

            }
        }
    };

    protected void inform(String s)
    {
        textView.setText(s);
    }

    protected void startDiscoveryBT()
    {
        isScanning = true;
        button.setText("Отменить");
        inform("Идет поиск доступных bluetooth устройств");
        bluetooth.startDiscovery();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final int REQUEST_ENABLE_BT = 1;

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        // Register for broadcasts for when 1. a device is discovered 2. discovery is finished
        // 3. bluetooth adapter is changed its state on turn off
        registerReceiver(mReceiver, filter);

        listView = (ListView) findViewById(R.id.listView);
        button = (Button) findViewById(R.id.button);
        textView = (TextView) findViewById(R.id.textView);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rssiList);
        listView.setAdapter(adapter);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isScanning) {
                    bluetooth.cancelDiscovery();
                }
                else {
                    rssiList.clear();
                    adapter.notifyDataSetChanged();

                    bluetooth = BluetoothAdapter.getDefaultAdapter();
                    // is bluetooth adapter available
                    if (bluetooth == null) {
                        inform("В устройстве отсутствует bluetooth модуль");
                        return;
                    }

                    //
                    if (bluetooth.isEnabled()) {
                        startDiscoveryBT();
                    }
                    else {
                        // Bluetooth is turned off.
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }
                }
            }
        });

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            inform("Bluetooth не включен");
        }
        else {
            startDiscoveryBT();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // unregister the receiver.
        unregisterReceiver(mReceiver);
    }
}
