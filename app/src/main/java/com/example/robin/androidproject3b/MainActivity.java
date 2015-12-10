package com.example.robin.androidproject3b;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final UUID STANDARD_SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final byte ACK_BYTE = 0x06;

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

        writeToFile("Test1");
        writeToFile("Test2");
        writeToFile("Test3");
    }

    private void writeToFile(String string) {

        if(!isExternalStorageWritable())
            System.err.println("isExternalStorageWritable: " + isExternalStorageWritable());

        File root = Environment.getExternalStorageDirectory();
        File file = new File(root.getAbsolutePath(), "myData.txt");
        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(file, true);
            fileWriter.append(string);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
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
            InputStream in;
            OutputStream out;

            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();

                // Write the byte sequence representing the data format to the sensor (and flush the stream)
                byte[] bytes = {0x02, 0x70, 0x04, 0x02, 0x02, 0x00, 0x78, 0x03};
                out.write(bytes);
                out.flush();

                // Read one byte from the input stream
                byte[] buffer = new byte[1024];
                in.read(buffer);

                // If the reply equals ACK
                if (buffer[0] == ACK_BYTE) {
                    // Create a FileWriter using the external file path Write a date stamp to the first line of the file
//                    FileWriter fw = new FileWriter();

                    // While not interrupted
                    while (true) {
                        try {
                            // Read a packet
                            in.read(buffer);

                            // Extract the byte representing the pulse (or pleth) value from the byte array
                            // TODO: Implement comment above

                            // Write the pleth data to the file
                            // TODO: Implement comment above

                            // Display the pulse data
                            // TODO: Implement comment above
                        }
                        catch (IOException e) {
                            break;
                        }
                    }

                    // Close the Bluetooth socket and the file writer (make sure this always happens)
                    socket.close();
//                    fw.close();
                }
            }
            catch (IOException e) {
                Log.i("Exception", "Couldn't get input and/or output streams.");
            }

            return null;
        }
    }
}
