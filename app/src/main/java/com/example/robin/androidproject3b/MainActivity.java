package com.example.robin.androidproject3b;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.jjoe64.graphview.LegendRenderer;

import java.io.IOException;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter adapter;
    private BluetoothDevice remote;
    private BluetoothSocket socket;

    private DownloadDataTask downloadDataTask;
    private ServerWriter serverWriter;

    private GraphView graph;
    private TextView pulseText;
    private TextView plethText;
    private Button button;
    private Button button2;

    private SharedPreferences pref;
    private MainActivity mainActivity;
//    private PulsePlethMonitor ppm;
//    private LineGraphSeries<DataPoint> series;
//    private LineGraphSeries<DataPoint> series2;
//    private int counter = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        graph = (GraphView) findViewById(R.id.graph);
        graph.setTitle("Pulse and Pleth Graph");
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(50);
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);

        pulseText = (TextView) findViewById(R.id.pulseText);
        plethText = (TextView) findViewById(R.id.plethText);
        mainActivity = this;
        pref = PreferenceManager.getDefaultSharedPreferences(this);

        downloadDataTask = new DownloadDataTask(mainActivity, remote);
        serverWriter = new ServerWriter(this);

        if (downloadDataTask != null) {
            graph.addSeries(downloadDataTask.getSeries());
            graph.addSeries(downloadDataTask.getSeries2());
        }

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

                        if (downloadDataTask != null) {
                            downloadDataTask = new DownloadDataTask(mainActivity, remote);
                        }

                        downloadDataTask.execute();
                        button.setText("Stop");
                    } else {
                        Toast.makeText(getApplicationContext(), "No compatible sensor detected!", Toast.LENGTH_LONG).show();
                        return;
                    }
                } else {
                    downloadDataTask.cancel(true);
                    try {
                        socket.close();
                    } catch (IOException e) {
                    }
                    catch (NullPointerException e) {
                    }

//                    serverWriter.execute();
                    new ServerWriter(mainActivity).execute();
                    button.setText("Start");
                }
            }
        });

        button2 = (Button) findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                serverWriter.execute();
                new ServerWriter(mainActivity).execute();
            }
        });

        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            Log.i("Adapter", "Bluetooth adapter found.");
        } else {
            Log.i("Adapter", "Bluetooth adapter NOT found.");
        }


//        series = new LineGraphSeries<DataPoint>();
//        series2 = new LineGraphSeries<DataPoint>();
//        series2.setColor(Color.RED);

//        graph.addSeries(series);
//        graph.addSeries(series2);


//        series.setTitle("Pulse");
//        series2.setTitle("Pleth");

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
    public void onPause() {
        super.onPause();

        downloadDataTask.cancel(true);
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

    public void updateUI(String pulse, String pleth) {
        pulseText.setText("Pulse: " + pulse);
        plethText.setText("Pleth: " + pleth);
    }
}
