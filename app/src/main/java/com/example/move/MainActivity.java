package com.example.move;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.LinearLayout.LayoutParams;

import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;
import com.tencent.mapsdk.raster.model.LatLng;
import com.tencent.tencentmap.mapsdk.map.MapView;
import com.tencent.tencentmap.mapsdk.map.TencentMap;
import com.tencent.tencentmap.mapsdk.map.UiSettings;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class MainActivity extends AppCompatActivity implements TencentLocationListener {

    MapView mapView = null;
    TencentMap tencentMap = null;
    TencentLocationManager tencentLocationManager;
    TencentLocationRequest tencentLocationRequest;


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
    private double baseSpeed = 0.0000002;
    private double speed = 0.0000002;
    private SeekBar speedSeekBar;
    private Button stopButton;
    private Button northButton;
    private Button eastButton;
    private Button southButton;
    private Button westButton;
    private int isBack = 0;
    private Button backButton;

    private WindowManager windowManager;
    private WindowManager.LayoutParams controllerLayoutParams;
    private View controllerView;
    private View filterView;
    private WindowManager.LayoutParams filterLayoutParams;
    private int isFilter = 0;
    private Button closeFilterButton;
    private LinearLayout headLinearLayout;
    private LayoutParams headLayoutParams;
    private String[] headImages;
    private AssetManager assetManager;
    private Map<Integer, Boolean> petMap;

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
        initFilter();
    }

    // 初始化腾讯地图的一些信息
    private void initMap() {
        tencentMap = mapView.getMap();
        tencentMap.setZoom(20);
        UiSettings uiSettings = mapView.getUiSettings();
        uiSettings.setZoomGesturesEnabled(false);
        uiSettings.setScrollGesturesEnabled(false);
    }

    // 初始化腾讯定位的一些信息
    private void initLocation() {
        tencentLocationManager = TencentLocationManager.getInstance(this);
        tencentLocationManager.setCoordinateType(TencentLocationManager.COORDINATE_TYPE_WGS84);
        tencentLocationRequest = TencentLocationRequest.create();
        tencentLocationRequest.setInterval(100);
        int error = tencentLocationManager.requestLocationUpdates(tencentLocationRequest, this);
    }

    // 初始化模拟定位的一些信息
    private void initMoveManager() {
        random = new Random();
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false,
                true, false, false, true,
                true, true, 0, 5);
        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
    }

    // 这里主要是设置悬浮窗的一些配置
    private void initController() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        controllerLayoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            controllerLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            controllerLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        controllerLayoutParams.format = PixelFormat.RGBA_8888;
        controllerLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        controllerLayoutParams.gravity = Gravity.START | Gravity.TOP;
        // 这是悬浮窗的宽高
        controllerLayoutParams.width = 400;
        controllerLayoutParams.height = 420;
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        // 这是悬浮窗处于屏幕的位置
        controllerLayoutParams.x = metrics.widthPixels;
        controllerLayoutParams.y = metrics.heightPixels / 2 - 50 * 3 / 2;
    }

    private void showController() {
        if (Settings.canDrawOverlays(this)) {
            // 这就是去获取activity_controller，这样才能用它里面的元素
            LayoutInflater layoutInflater = LayoutInflater.from(this);
            controllerView = layoutInflater.inflate(R.layout.activity_controller, null);
            stopButton = (Button) controllerView.findViewById(R.id.stopButton);
            backButton = (Button) controllerView.findViewById(R.id.backButton);
            northButton = (Button) controllerView.findViewById(R.id.northButton);
            eastButton = (Button) controllerView.findViewById(R.id.eastButton);
            southButton = (Button) controllerView.findViewById(R.id.southButton);
            westButton = (Button) controllerView.findViewById(R.id.westButton);
            northButton.setAlpha((float)0.5);
            // 这里是控制移动速度的，有一个基础速度，然后根据速度条的位置增加相应的速度值
            // 因为这里调用了其它layout内的元素，所以需要用LayoutInflater获取到对应的layout再操作
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

            windowManager.addView(controllerView, controllerLayoutParams);
        }
    }

    // 初始化筛选
    private void initFilter() {
        filterLayoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            filterLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            filterLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        filterLayoutParams.format = PixelFormat.RGBA_8888;
        filterLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        filterLayoutParams.gravity = Gravity.START | Gravity.TOP;
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        // 这是悬浮窗的宽高
        filterLayoutParams.width = metrics.widthPixels;
        filterLayoutParams.height = metrics.heightPixels;
        // 这是悬浮窗处于屏幕的位置
        filterLayoutParams.x = 0;
        filterLayoutParams.y = 0;
        headLayoutParams = new LayoutParams(80, 80);
        headLayoutParams.setMargins(5, 5, 5, 5);

        // 获取assets下的小妖头像
        assetManager = this.getResources().getAssets();
        try {
            // 将所有头像都放到assets/heads下，避免其它图片干扰
            headImages = assetManager.list("heads");
        } catch(IOException e) {
            e.printStackTrace();
        }
        // 记录小妖当前是否被选，以小妖id为key，false为未选、true为已选
        petMap = new HashMap<Integer, Boolean>();
    }

    // 显示筛选界面
    private void showFilter() {
        if (Build.VERSION.SDK_INT > 22) {
            if (Settings.canDrawOverlays(this)) {
                LayoutInflater layoutInflater = LayoutInflater.from(this);
                filterView = layoutInflater.inflate(R.layout.activity_filter, null);
                closeFilterButton = (Button) filterView.findViewById(R.id.closeFilterButton);
                headLinearLayout = (LinearLayout) filterView.findViewById(R.id.headLinearLayout);
                windowManager.addView(filterView, filterLayoutParams);
            }
        }
        showPet();
    }
    // 显示小妖头像
    // 这里就是读取assets中的图片，一排显示10个
    private void showPet() {
        InputStream input = null;
        LinearLayout imageLinearLayout = null;
        for (int i = 0; i < headImages.length; i ++) {
            Log.i("images: ", String.valueOf(headImages[i]));
            if (i % 10 == 0) {
                imageLinearLayout = new LinearLayout(this);
                imageLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
                headLinearLayout.addView(imageLinearLayout);
            }
            try {
                input = assetManager.open(headImages[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            final ImageView imgView = new ImageView(this);
            imgView.setLayoutParams(headLayoutParams);
            imgView.setImageBitmap(BitmapFactory.decodeStream(input));
            int imageId = Integer.valueOf(headImages[i].substring(0, headImages[i].indexOf(".")));
            petMap.put(imageId,false);
            imgView.setId(imageId);
            imgView.setAlpha((float)0.2);
            imgView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int imageId = view.getId();
                    Log.i("ID: ", String.valueOf(imageId));
                    Boolean selected = petMap.get(imageId);
                    selected = !selected;
                    petMap.put(imageId, selected);
                    if (selected == true) {
                        view.setAlpha((float)1.0);
                    } else {
                        view.setAlpha((float)0.2);
                    }
                }
            });
            imageLinearLayout.addView(imgView);
        }
    }
    // 隐藏筛选界面
    private void removeFilter() {
        windowManager.removeView(filterView);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.northButton:
                onClickNorth();
                break;
            case R.id.eastButton:
                onClickEast();
                break;
            case R.id.southButton:
                onClickSouth();
                break;
            case R.id.westButton:
                onClickWest();
                break;
            case R.id.stopButton:
                onClickOnOff();
                break;
            case R.id.backButton:
                onClickBack();
                break;
            case R.id.filterButton:
                onClickFilter();
                break;
            case R.id.autoButton:
                onClickAuto();
                break;
            case R.id.nextButton:
                onClickNext();
                break;
            case R.id.closeFilterButton:
                removeFilter();
                break;
        }
    }

    // 控制面板是展开还是收回的方法，由backButton调用
    // 主要就是重新设置悬浮窗的宽度，然后更新悬浮窗
    public void onClickBack() {
        isBack += 1;
        if (isBack % 2 == 1) {
            controllerLayoutParams.width = 100;
            windowManager.updateViewLayout(controllerView, controllerLayoutParams);
            backButton.setText("开");
        } else {
            controllerLayoutParams.width = 400;
            windowManager.updateViewLayout(controllerView, controllerLayoutParams);
            backButton.setText("收");
        }
    }
    // 设置要筛选小妖
    public void onClickFilter() {
        showFilter();
    }
    // 自动到小妖身边
    public void onClickAuto() {
    }
    // 下一个
    public void onClickNext() {
    }
    // 以下四个方法就是控制东南西北的，分别由不同方向按钮调用
    public void onClickNorth() {
        direct = 0;
        northButton.setAlpha((float)0.5);
        eastButton.setAlpha((float)1.0);
        southButton.setAlpha((float)1.0);
        westButton.setAlpha((float)1.0);
    }
    public void onClickEast() {
        direct = 1;
        eastButton.setAlpha((float)0.5);
        northButton.setAlpha((float)1.0);
        southButton.setAlpha((float)1.0);
        westButton.setAlpha((float)1.0);
    }
    public void onClickSouth() {
        direct = 2;
        southButton.setAlpha((float)0.5);
        eastButton.setAlpha((float)1.0);
        northButton.setAlpha((float)1.0);
        westButton.setAlpha((float)1.0);
    }
    public void onClickWest() {
        direct = 3;
        westButton.setAlpha((float)0.5);
        southButton.setAlpha((float)1.0);
        eastButton.setAlpha((float)1.0);
        northButton.setAlpha((float)1.0);
    }
    // 控制走与停的方法，由stopButton调用
    public void onClickOnOff() {
        isRun += 1;
        if (isRun % 2 == 0) {
            stopButton.setText("走");
        } else {
            stopButton.setText("停");
        }
    }

    // 这里tencent定位监听的回调，只要位置发生变化，就会调用此方法
    // 我这里就是让定位获取的坐标始终位于地图中央
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

    // 生成一个区间的浮点数
    private double genDouble(final double min, final double max) {
        return min + ((max - min) * random.nextDouble());
    }

    // 生成一个区间的整数
    private int genInt(final int min, final int max) {
        return random.nextInt(max) % (max-min+1) + min;
    }

    private void setLocation(double longitude, double latitude) {

        // 因为我是在北京的，我把这个海拔设置了一个符合北京的范围，随机生成
        altitude = genDouble(38.0, 50.5);
        // 而定位有精度，GPS精度一般小于15米，我设置在1到15米之间随机
        accuracy = (float)genDouble(1.0, 15.0);

        // 下面就是自己设置定位的位置了
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

    // 持续定位
    public void continueLocation() {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        // 这里是一个死循环，但是我们不可以直接不停的设置定位，那样会把手机卡死
                        // 因此我们这里需要设置两次设置定位的间隔, 但是这个值又不能设置大了，
                        // 因为，两次定位之间间隔长了，在这个时间窗口有其它程序调用定位功能，就会获取到你真实的位置
                        // 从而导致，位置出现跳跃的现象，我这里设置1毫秒
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // isRun是来判断我们是不是要停下来，记住，这里停下来的概念是，你的位置坐标不变的意思，
                    // 因此，要达到这个效果，并不是要把线程停下来，而是要不停的给设置同一位置坐标
                    if (isRun % 2 == 1) {
                        // 这里设置四个方向，北：0、东：1、南：2、西：3
                        // 改变了方向就改变经纬度的变化策略
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
                    }
                    setLocation(longitude, latitude);
                }
            }
        });
        thread.start();
    }
}
