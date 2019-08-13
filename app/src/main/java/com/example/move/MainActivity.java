package com.example.move;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;

import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;
import com.tencent.mapsdk.raster.model.LatLng;
import com.tencent.tencentmap.mapsdk.map.MapView;
import com.tencent.tencentmap.mapsdk.map.TencentMap;
import com.tencent.tencentmap.mapsdk.map.UiSettings;

import com.example.move.service.MoveService;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements TencentLocationListener {

    MapView mapView = null;
    TencentMap tencentMap = null;
    TencentLocationManager tencentLocationManager;
    TencentLocationRequest tencentLocationRequest;
    TencentLocationListener tencentLocationListener;


    private Random random;
    private LocationManager locationManager;
    private Thread thread;
    private int isRun = 0;
    private final Object lock = new Object();
    private double longitude = 0;
    private double latitude = 0;
    private double altitude;
    private float accuracy;
    private int direct = 0;
    private double baseSpeed = 0.00001;
    private double speed = 0.00001;
    private int step = 1000;
    private int count = 0;
    private SeekBar speedSeekBar;

    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private View controllerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        init();
        continueLocation();
        showController();
    }

    private void init() {
        initMap();
        initLocation();
        initMoveManager();
        initController();
    }

    private void initMap() {
        tencentMap = mapView.getMap();
        tencentMap.setZoom(20);
        UiSettings uiSettings = mapView.getUiSettings();
        uiSettings.setZoomGesturesEnabled(false);
        uiSettings.setScrollGesturesEnabled(false);
    }

    private void initLocation() {
        tencentLocationManager = TencentLocationManager.getInstance(this);
        tencentLocationManager.setCoordinateType(TencentLocationManager.COORDINATE_TYPE_WGS84);
        tencentLocationRequest = TencentLocationRequest.create();
        tencentLocationRequest.setInterval(100);
        int error = tencentLocationManager.requestLocationUpdates(tencentLocationRequest, this);
    }

    private void initMoveManager() {
        random = new Random();
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false,
                true, false, false, true,
                true, true, 0, 5);
        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
    }

    private void initController() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.gravity = Gravity.START | Gravity.TOP;
        layoutParams.width = 600;
        layoutParams.height = 600;
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        layoutParams.x = metrics.widthPixels - 10;
        layoutParams.y = metrics.heightPixels / 2;
    }

    private void showController() {
        if (Settings.canDrawOverlays(this)) {
            LayoutInflater layoutInflater = LayoutInflater.from(this);
            controllerView = layoutInflater.inflate(R.layout.activity_controller, null);

            speedSeekBar = (SeekBar) controllerView.findViewById(R.id.speedSeekBar);
            speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    speed = baseSpeed + baseSpeed * (i / 10);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            windowManager.addView(controllerView, layoutParams);
        }
    }

    public void onClickNorth(View view) {
        direct = 0;
    }
    public void onClickEast(View view) {
        direct = 1;
    }
    public void onClickSouth(View view) {
        direct = 2;
    }
    public void onClickWest(View view) {
        direct = 3;
    }
    public void onClickOnOff(View view) {
        isRun += 1;
        Log.i("isRun", String.valueOf(isRun));
        if (isRun % 2 == 1) {

            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }

    @Override
    public void onLocationChanged(TencentLocation location, int error, String reason) {
        if (latitude == 0 && longitude == 0) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }
        tencentMap.setCenter(new LatLng(latitude, longitude));
    }

    @Override
    public void onStatusUpdate(String name, int status, String desc) {

    }

    private double genDouble(final double min, final double max) {
        return min + ((max - min) * random.nextDouble());
    }

    private int genInt(final int min, final int max) {
        return random.nextInt(max) % (max-min+1) + min;
    }

    private void setLocation(double longitude, double latitude) {

        altitude = genDouble(38.0, 50.5);
        accuracy = (float)genDouble(1.0, 15.0);

        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setTime(System.currentTimeMillis());
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        // 北京海拔大概范围，随机生成
        location.setAltitude(altitude);
        // GPS定位精度范围，随机生成
        location.setAccuracy(accuracy);
        if (Build.VERSION.SDK_INT > 16) {
            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }
        locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, location);
    }

    public void stopLocation() {
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void continueLocation() {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    while (isRun % 2 == 0) {
                        stopLocation();
                    }
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    switch (direct % 4) {
                        case 0:
                            latitude += speed;
                            break;
                        case 1:
                            longitude += speed;
                            break;
                        case 2:
                            latitude -= speed;
                            break;
                        case 3:
                            longitude -= speed;
                            break;
                    }
                    setLocation(longitude, latitude);
                }
            }
        });
        thread.start();
    }
}
