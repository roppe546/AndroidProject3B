package com.example.robin.androidproject3b;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.UUID;

/**
 * This task downloads data from the sensor device.
 */
public class DownloadDataTask extends AsyncTask<Void, String, Void> {
    private final UUID STANDARD_SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final byte ACK_BYTE = 0x06;

    private PulsePlethMonitor ppm;
    private BluetoothDevice remote;
    private BluetoothSocket socket;

    private MainActivity activity;
    private SharedPreferences pref;

    private double counter = 0;
    private LineGraphSeries<DataPoint> series;
    private LineGraphSeries<DataPoint> series2;

    public DownloadDataTask(MainActivity activity) {
        this.ppm = new PulsePlethMonitor();
        this.activity = activity;
        this.pref = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());

        this.remote = activity.getRemote();

        series = new LineGraphSeries<>();
        series2 = new LineGraphSeries<>();
        series.setTitle("Pulse");
        series2.setTitle("Pleth");
        series2.setColor(Color.RED);
    }

    @Override
    protected Void doInBackground(Void... params) {
        // Get BluetoothSocket object
        try {
            socket = remote.createRfcommSocketToServiceRecord(STANDARD_SPP_UUID);
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
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
                writeToFile(new Date().toString() + "\n");
                writeToFile("Pleth Pulse\n");

                int loopCounter = 0;

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
                        byte status = buffer[1];
                        Log.i("PLETH", "" + buffer[2]);
                        Log.i("PULSE", "" + buffer[3]);

                        // Check if frame 1 (sync bit in status = 1)
                        if ((status & 0x01) == 1) {
                            Log.i("DOWNLOAD", "SYNC FRAME");
                            loopCounter = 1;

                            ppm.setMsb(unsignedByteToInt(buffer[3]));

                            continue;
                        } else if (loopCounter == 2) {
                            ppm.setPulse(unsignedByteToInt(buffer[2]));
                            ppm.setPleth(unsignedByteToInt(buffer[3]));

                            if (ppm.getMsb() == 1) {
                                ppm.setPulse(ppm.getPulse() + 128);
                            }

                            Log.i("DOWNLOAD", "pleth = " + ppm.getPleth() + ", pulse = " + ppm.getPulse());
                            writeToFile(ppm.getPleth() + " " + ppm.getPulse() + "\n");
                            publishProgress(ppm.getPulse() + "", ppm.getPleth() + "");

                            ppm.setMsb(0);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }

                // Close the Bluetooth socket and the file writer (make sure this always happens)
//                socket.close();
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

        System.out.println("got here");

        counter = counter + (double) 1/3;
        Log.i("pulse2", "" + values[1]);
        Log.i("pleth2", "" + values[0]);
        Log.i("counter", "" + counter);

        series.appendData(new DataPoint(counter, Double.valueOf(values[1])), false, 10001);
        series2.appendData(new DataPoint(counter, Double.valueOf(values[0])), false, 10001);

        activity.updateUI(values[1], values[0]);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes a given String to file.
     * @param string to write to file.
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

    public LineGraphSeries<DataPoint> getSeries() {
        return series;
    }

    public LineGraphSeries<DataPoint> getSeries2() {
        return series2;
    }

    public void closeSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}