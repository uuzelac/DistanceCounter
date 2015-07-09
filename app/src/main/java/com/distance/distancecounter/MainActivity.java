package com.distance.distancecounter;

import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity{

    private static final long MIN_DISTANCE_FOR_UPDATE = 5; // u metrima
    private static final long MIN_TIME_FOR_UPDATE = 5000;  // u ms

    protected boolean mStartStop = false;
    protected boolean mPauseResume = false;

    protected String mLocationProvider;

    protected double mDistance;
    protected double mTraveledDistance;
    protected double mCurrentLat;
    protected double mCurrentLon;
    protected double mPreviousLat;
    protected double mPreviousLon;
    protected long mTimeWhenStopped = 0;

    protected Location mLocation;

    protected MyThread mThread;

    protected Button mStart;
    protected Button mPause;
    protected TextView mDistanceNow;
    protected Spinner mSpinner;

    protected LocationManager mLocationManager;
    protected LocationListener mLocationListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStart = (Button) findViewById(R.id.buttonStart);
        mStart.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
        mPause = (Button) findViewById(R.id.buttonPause);
        mPause.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        mPause.setEnabled(false);

        //Inicijalizacija spinera
        mSpinner = (Spinner) findViewById(R.id.spinner);
        List<String> listOfAlgoritams = new ArrayList<>(2);
        listOfAlgoritams.add("World Geodetic System");
        listOfAlgoritams.add("Haversine formula");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, listOfAlgoritams);
        mSpinner.setAdapter(adapter);

        final Chronometer myChronometer = (Chronometer) findViewById(R.id.chronometer);

        mDistanceNow = (TextView) findViewById(R.id.textViewDistanceNow);
        mDistanceNow.setText("0.0 m");

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //Criteria for network provider
//        Criteria criteria = new Criteria();
//        criteria.setPowerRequirement(Criteria.POWER_LOW);
//        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
//        mLocationProvider = mLocationManager.getBestProvider(criteria, true);


        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                if (!mPauseResume){
                    mDistanceNow.setText(mDistance + " m");
                }
            }

            public void onProviderDisabled(String provider) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
        };

//        mLocationManager.requestLocationUpdates(
//                MIN_TIME_FOR_UPDATE,
//                MIN_DISTANCE_FOR_UPDATE,
//                criteria,
//                mLocationListener,
//                null
//        );

        mLocationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME_FOR_UPDATE,
                MIN_DISTANCE_FOR_UPDATE,
                mLocationListener
        );

        setCurrentLocation();

        mStart.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(mLocation==null){
                    setCurrentLocation();
                }

                if (mLocation!=null && !mStartStop) {
                    mStartStop = true;
                    mPause.setEnabled(true);
                    mSpinner.setEnabled(false);
                    mStart.setText("Stop");
                    mStart.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));

                    mDistance = 0;
                    startThread();
                    myChronometer.setBase(SystemClock.elapsedRealtime());
                    myChronometer.start();

                }else if(mStartStop){

                    if(!mPauseResume){
                        mTimeWhenStopped = myChronometer.getBase() - SystemClock.elapsedRealtime();
                        myChronometer.stop();
                    }

                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                    alertDialog.setTitle("Rezultat merenja")
                            .setMessage("Predjeni put iznosi " + (mPauseResume ? mTraveledDistance : mDistance) + " m. Vreme predjenog " +
                                    "puta iznosi " + myChronometer.getText().toString())
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mStartStop = false;
                                    mStart.setText("Start");
                                    mStart.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                                    mThread.close();
                                    myChronometer.stop();
                                    myChronometer.setBase(SystemClock.elapsedRealtime());
                                    mTimeWhenStopped = 0;
                                    mPause.setEnabled(false);
                                    mSpinner.setEnabled(true);
                                    mPause.setText("Pauza");
                                    mPause.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                                    mDistance = 0;
                                    mDistanceNow.setText("0.00 m");
                                }
                            })
                            .setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (!mPauseResume){
                                        myChronometer.setBase(SystemClock.elapsedRealtime() + mTimeWhenStopped);
                                        myChronometer.start();
                                    }
                                }
                            })
                            .setCancelable(false)
                            .create()
                            .show();
                }else{
                    Toast.makeText(MainActivity.this, "Trenutna lokacija jos uvek nije poznata.",
                            Toast.LENGTH_LONG).show();
                }

            }
        });

        mPause.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLocation!=null && !mPauseResume) {
                    mPauseResume = true;
                    mPause.setText("Nastavi");
                    mPause.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
                    mTraveledDistance = mDistance;
                    mDistance = 0;
                    mTimeWhenStopped = myChronometer.getBase() - SystemClock.elapsedRealtime();
                    myChronometer.stop();
                    mThread.close();

                }else if(mStartStop){
                    mPauseResume = false;
                    mPause.setText("Pauza");
                    mPause.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                    myChronometer.setBase(SystemClock.elapsedRealtime() + mTimeWhenStopped);
                    myChronometer.start();
                    mDistance = mTraveledDistance;
                    startThread();


                }else{
                    Toast.makeText(MainActivity.this, "Trenutna lokacija nije poznata.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setCurrentLocation() {

        mLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//        if (mLocation==null){
//            mLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//        }

        if (mLocation != null) {

            mCurrentLat = mLocation.getLatitude();
            mCurrentLon = mLocation.getLongitude();
            mPreviousLat = mLocation.getLatitude();
            mPreviousLon = mLocation.getLongitude();

        } else {

            if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Toast.makeText(MainActivity.this, "Lokacija jos uvek nije poznata.",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "GPS prijemnik nije ukljucen.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public double getDistanceHaversine(double initialLat, double initialLong, double finalLat, double finalLong) {

        double latA = Math.toRadians(initialLat);
        double lonA = Math.toRadians(initialLong);
        double latB = Math.toRadians(finalLat);
        double lonB = Math.toRadians(finalLong);

        double latDiff = latB - latA;
        double longDiff = lonB - lonA;
        double earthRadius = 6371*1000;

        double distance = 2*earthRadius*Math.asin(Math.sqrt(Math.pow(Math.sin(latDiff/2.0),2)+
                Math.cos(initialLat)*Math.cos(finalLat)*Math.pow(Math.sin(longDiff/2),2)));

        int i = (int) distance;

//        DecimalFormat df = new DecimalFormat("##.00");
//        String formate = df.format(distance);

        return Double.parseDouble(String.valueOf(i));

    }

    private double getDistanceBetween(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        int udaljenost = (int) results[0];
        return (double) udaljenost;
    }

    private void startThread() {
        mThread = new MyThread();
        mThread.start();
    }

    //Nit se pokrece na start dugme i racuna predjenu udaljenost
    private class MyThread extends Thread {

        private boolean mRunning = false;

        @Override
        public void run() {
            mRunning = true;
            while (mRunning) {
                mLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                mCurrentLat = mLocation.getLatitude();
                mCurrentLon = mLocation.getLongitude();
                if (mCurrentLat != mPreviousLat || mCurrentLon != mPreviousLon) {

                    if (mSpinner.getSelectedItem().toString().equals("World Geodetic System")){
                        mDistance += getDistanceBetween(mPreviousLat, mPreviousLon, mCurrentLat, mCurrentLon);
                    }else if(mSpinner.getSelectedItem().toString().equals("Haversine formula")){
                        mDistance += getDistanceHaversine(mPreviousLat, mPreviousLon, mCurrentLat, mCurrentLon);
                    }
                    mPreviousLat = mCurrentLat;
                    mPreviousLon = mCurrentLon;

                }
            }
        }

        public void close() {
            mRunning = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mThread != null){
            mThread.close();
        }
    }
}