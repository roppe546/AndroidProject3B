package com.example.robin.androidproject3b;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private UUID STANDARD_SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter adapter;
    private BluetoothDevice remote;
    private BluetoothSocket socket;

    private DownloadDataTask downloadDataTask;

    private TextView textView;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.textView);
        button = (Button) findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (button.getText().equals("Start"))
                    button.setText("Stop");
                else
                    button.setText("Start");

                //TODO implement download logic
            }
        });

        // Bluetooth stuff
        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            Log.i("Adapter", "Bluetooth adapter found.");
        } else {
            Log.i("Adapter", "Bluetooth adapter NOT found.");
        }

        // TODO: This code should/could probably be elsewhere
        // TODO: Is this needed?
        // If adapter is disabled, try to enable it
//        if (adapter.isEnabled() == false) {
//            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(intent, REQUEST_ENABLE_BT);
//        }

        // Start download task
        downloadDataTask = new DownloadDataTask();
        downloadDataTask.execute();
    }

    /**
     * This task downloads data from the sensor device.
     */
    private class DownloadDataTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            // Initialize the bluetoothSocket
            Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
            Log.i("Pair", "Paired devices: " + pairedDevices.toString());

            if (pairedDevices.size() == 1) {
                for (BluetoothDevice dev : pairedDevices) {
                    remote = dev;
                }
            }

            // Get BluetoothSocket object
            try {
                socket = remote.createRfcommSocketToServiceRecord(STANDARD_SPP_UUID);
            }
            catch (IOException e) {
                Log.i("Exception", "Couldn't create BluetoothSocket");
            }

            // Retrieve the input and output streams from the socket
            // Write the byte sequence representing the data format to the sensor (and flush the stream)
            // Read one byte from the input stream
            // If the reply equals ACK
            //     Create a FileWriter using the external file path Write a date stamp to the first line of the file
            //     While not interrupted
            //         Read a packet
            //         Extract the byte representing the pulse (or pleth) value from the byte array
            //         Write the pleth data to the file
            //         Display the pulse data
            // Close the Bluetooth socket and the file writer (make sure this always happens)

            return null;
        }
    }
}


