package com.example.robin.androidproject3b;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by robin on 14/12/15.
 */
public class ServerWriter extends AsyncTask<Void, Void, Void> {
    private MainActivity activity;
    private SharedPreferences pref;

    public ServerWriter(MainActivity activity) {
        this.activity = activity;
        this.pref = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    @Override
    protected Void doInBackground(Void... params) {
        String data = readFromFile();
        try {
            sendDataToServer(data);
        } catch (Exception e) {
            this.cancel(true);
        }
        return null;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        Toast.makeText(activity.getApplicationContext(), "Unable to send data to server. Make sure server IP and port number are correct.", Toast.LENGTH_LONG).show();
    }

    private void sendDataToServer(String string) throws Exception {

        String host = pref.getString("ipaddress", "localhost");
        int PORT_NUMBER = Integer.parseInt(pref.getString("portnumber", "1337"));

        Socket socket = null;
        PrintWriter out = null;
        try {
            socket = new Socket(host, PORT_NUMBER);
            out = new PrintWriter(socket.getOutputStream(), true);
            out.write(string);
            System.out.println("Sent: " + string);
        } finally {
            if (out != null)
                out.close();
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

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
}
