package com.example.robin.androidproject3b;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Date;
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
    private Button button2;

    private SharedPreferences pref;
    private LineGraphSeries<DataPoint> series;
    private int counter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.textView);
        button = (Button) findViewById(R.id.button);

        pref = PreferenceManager.getDefaultSharedPreferences(this);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("clicked button1");
                if (button.getText().equals("Start"))
                    button.setText("Stop");
                else
                    button.setText("Start");

                //TODO implement download logic
            }
        });

        button2 = (Button) findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("clicked button2!");
                new SendDataToServerTask().execute();
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

        GraphView graph = (GraphView) findViewById(R.id.graph);
        series = new LineGraphSeries<DataPoint>();
        graph.addSeries(series);
        graph.setTitle("Pulse graph");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.settingsButton) {
            Log.i("ActionMenu", "Selected Settings in menu");
            startActivityForResult(new Intent(this, SettingsActivity.class), 200);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (socket != null) {
            try {
                socket.close();
            }
            catch (IOException e) {

            }
        }
    }

    /**
     * This task downloads data from the sensor device.
     */
    private class DownloadDataTask extends AsyncTask<Void, String, Void> {

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
                socket.connect();
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
                byte[] bytes = {0x02, 0x70, 0x04, 0x02, 0x02, 0x00, (byte) 0x78, 0x03};
                out.write(bytes);
                out.flush();

                // Read one byte from the input stream
                byte[] buffer = new byte[1];
                in.read(buffer);

                // If the reply equals ACK
                if (buffer[0] == ACK_BYTE) {
                    // Create a FileWriter using the external file path Write a date stamp to the first line of the file
                    writeToFile(new Date().toString());

                    // While not interrupted
                    int loopCounter = 0;

                    while (true) {
                        loopCounter++;
                        try {
                            // Read a packet
                            buffer = new byte[5];
                            in.read(buffer);

                            // Discard frames where byte 0 != 1
                            if (buffer[0] != 1) {
                                continue;
                            }

                            // TEST CODE
                            String test = Arrays.toString(buffer);
                            Log.i("BLYAT", "arr: " + test);

                            // Extract the byte representing the pulse (or pleth) value from the byte array
                            // TODO: Implement comment above
                            if(loopCounter == 2) {
                                Log.i("PULSE_LSB", "" + buffer[3]);
                                publishProgress(buffer[3] + "");
                            }

                            // Check if frame 1 (sync bit in status = 1)
                            if ((buffer[1] & 0x01) != 1) {
                                loopCounter = 1;
                                Log.i("DOWNLOAD", "SYNC FRAME");
                                continue;
                            }

                            Log.i("PLETH", "" + buffer[2]);
                            Log.i("PULSE", "" + buffer[3]);

                            int pleth = unsignedByteToInt(buffer[2]);
                            int pulse = unsignedByteToInt(buffer[3]);
                            Log.i("DOWNLOAD", "pleth = " + pleth + ", pulse = " + pulse);

                            // Write the pleth data to the file
                            writeToFile(pleth + " " + pulse + "\n");

                            // Display the pulse data
//                            publishProgress(pulse + "");
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

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            series.appendData(new DataPoint(counter++, Double.valueOf(values[0])), true, 100);
            textView.setText("Pulse: " + values[0]);
        }
    }

    /**
     *
     *
     * @param string
     */
    private void writeToFile(String string) {

        if(!isExternalStorageWritable())
            System.err.println("isExternalStorageWritable: " + isExternalStorageWritable());

        File root = Environment.getExternalStorageDirectory();

        String filename = pref.getString("filename", "myData.txt");
        File file = new File(root.getAbsolutePath(), filename);
        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(file, true);
            fileWriter.append(string);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class SendDataToServerTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            sendDataToServer("Hello World!");
            return null;
        }
    }

    private void sendDataToServer(String string) {

        String host = pref.getString("ipaddress", "localhost");
        int PORT_NUMBER = Integer.parseInt(pref.getString("portnumber", "1337"));

        Socket socket = null;
        try {
            socket = new Socket(host, PORT_NUMBER);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.write(string);
            out.close();
            System.out.println("Sent: " + string);
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
     * Convert an signed integer to an unsigned integer.
     *
     * @param b byte holding the integer
     * @return  an unsigned integer
     */
    private int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }
}
