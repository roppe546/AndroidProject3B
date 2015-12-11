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
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
    private LineGraphSeries<DataPoint> series2;
    private int counter = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.textView);
        pref = PreferenceManager.getDefaultSharedPreferences(this);

        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (button.getText().equals("Start")) {

                    // Initialize the bluetoothSocket
                    Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
                    Log.i("Pair", "Paired devices: " + pairedDevices.toString());

                    if (pairedDevices.size() == 1) {
                        for (BluetoothDevice dev : pairedDevices) {
                            remote = dev;
                        }
                    }


                    if(remote == null) {
                        Toast.makeText(getApplicationContext(), "No compatible sensor detected!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    downloadDataTask = new DownloadDataTask();
                    downloadDataTask.execute();
                    button.setText("Stop");
                } else {
                    downloadDataTask.cancel(true);
                    new SendDataToServerTask().execute();
                    button.setText("Start");
                }
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

        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            Log.i("Adapter", "Bluetooth adapter found.");
        } else {
            Log.i("Adapter", "Bluetooth adapter NOT found.");
        }


        GraphView graph = (GraphView) findViewById(R.id.graph);
        series = new LineGraphSeries<DataPoint>();
        series2 = new LineGraphSeries<DataPoint>();
        graph.addSeries(series);
        graph.addSeries(series2);
        graph.setTitle("Pulse graph");
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(100);
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
            } catch (IOException e) {

            }
        }
    }

    /**
     * This task downloads data from the sensor device.
     */
    private class DownloadDataTask extends AsyncTask<Void, String, Void> {

        @Override
        protected Void doInBackground(Void... params) {


            // Get BluetoothSocket object
            try {
                socket = remote.createRfcommSocketToServiceRecord(STANDARD_SPP_UUID);
                socket.connect();
            } catch (IOException e) {
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

                    int loopCounter = 0;
                    int msb = 0;

                    while (true) {
                        if (isCancelled())
                            break;

                        loopCounter++;
                        try {
                            // Read a packet
                            buffer = new byte[5];
                            in.read(buffer);

                            // Discard frames where byte 0 != 1
                            if (buffer[0] != 1) {
                                continue;
                            }

                            // Extract the byte representing the pulse (or pleth) value from the byte array
                            int pleth;
                            int pulse;
                            Log.i("PLETH", "" + buffer[2]);
                            Log.i("PULSE", "" + buffer[3]);

                            // Check if frame 1 (sync bit in status = 1)
                            if ((buffer[1] & 0x01) == 1) {
                                Log.i("DOWNLOAD", "SYNC FRAME");
                                loopCounter = 1;

                                msb = unsignedByteToInt(buffer[3]);

                                continue;
                            }
                            else if (loopCounter == 2) {
                                pleth = unsignedByteToInt(buffer[2]);
                                pulse = unsignedByteToInt(buffer[3]);

                                if (msb == 1) {
                                    pulse += 128;
                                }

                                Log.i("DOWNLOAD", "pleth = " + pleth + ", pulse = " + pulse);
                                writeToFile(pleth + " " + pulse + "\n");
                                publishProgress(pulse + "", pleth + "");

                                msb = 0;
                            }
                        }
                        catch (IOException e) {
                            break;
                        }
                    }

                    // Close the Bluetooth socket and the file writer (make sure this always happens)
                    socket.close();
//                    fw.close();
                }
            } catch (IOException e) {
                Log.i("Exception", "Couldn't get input and/or output streams.");
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            series.appendData(new DataPoint(counter++, Double.valueOf(values[0])), true, 100);
            series2.appendData(new DataPoint(counter++, Double.valueOf(values[1])), true, 100);
            textView.setText("Pulse: " + values[0] + "Pleth: " + values[1]);
        }
    }

    /**
     * @param string
     */
    private void writeToFile(String string) {

        if (!isExternalStorageWritable())
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

    private String readFromFile() {

        if (!isExternalStorageWritable())
            System.err.println("isExternalStorageWritable: " + isExternalStorageWritable());

        File root = Environment.getExternalStorageDirectory();

        String filename = pref.getString("filename", "myData.txt");
        File file = new File(root.getAbsolutePath(), filename);

        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;

        try {

            String currentLine;
            br = new BufferedReader(new FileReader(file));

            while ((currentLine = br.readLine()) != null) {
                sb.append(currentLine + "\n");
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

        private class SendDataToServerTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            String data = readFromFile();
            sendDataToServer(data);
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
     * @return an unsigned integer
     */
    private int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }
}
