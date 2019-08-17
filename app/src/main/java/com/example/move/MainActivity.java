package com.example.move;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.Toast;

import com.rabtman.wsmanager.WsManager;
import com.rabtman.wsmanager.listener.WsStatusListener;
import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;
import com.tencent.mapsdk.raster.model.LatLng;
import com.tencent.tencentmap.mapsdk.map.MapView;
import com.tencent.tencentmap.mapsdk.map.TencentMap;
import com.tencent.tencentmap.mapsdk.map.UiSettings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;


public class MainActivity extends AppCompatActivity implements TencentLocationListener {
    private double pi = 3.1415926535897932384626;
    public double a = 6378245.0;
    public double ee = 0.00669342162296594323;
    // 腾讯地图
    MapView mapView = null;
    TencentMap tencentMap = null;
    // 腾讯定位
    TencentLocationManager tencentLocationManager;
    TencentLocationRequest tencentLocationRequest;


    private Random random;
    // 模拟定位
    private LocationManager locationManager;
    // 持续模拟设置定位线程
    private Thread thread;
    // 设置移动还是停下
    private int isRun = 0;
    // 记录当前设置的定位值
    private double longtitude = 0;
    private double latitude = 0;
    // 设置的当前定位海拔
    private double altitude;
    // 设置的当前定位精度
    private float accuracy;
    // 当前行走方向
    private int direct = 0;
    // 基础移动速度
    private double baseSpeed = 0.0000002;
    // 移动速度
    private double speed = 0.0000002;
    // 控制器上的功能组件
    private SeekBar speedSeekBar;
    private Button stopButton;
    private Button northButton;
    private Button eastButton;
    private Button southButton;
    private Button westButton;
    private Button backButton;
    // 判断控制器是收起还是展开
    private int isBack = 0;

    // 悬浮效果控制器
    private WindowManager windowManager;
    private WindowManager.LayoutParams controllerLayoutParams;
    // 控制器页面
    private View controllerView;
    // 筛选器页面
    private View filterView;
    private WindowManager.LayoutParams filterLayoutParams;
    private LinearLayout headLinearLayout;
    private LayoutParams headLayoutParams;
    // 存储妖灵头像图片
    private String[] headImages;
    // 用于获取assets目录下的图片
    private AssetManager assetManager;
    // 筛选器中选中的妖灵
    private Set<String> petSet;
    private SharedPreferences petSharedPreferences;
    private SharedPreferences.Editor editor;
    // 腾讯提供的获取坐标位置附近妖灵的websocket
    private String wssHost = "wss://publicld.gwgo.qq.com?account_value=0&account_type=1&appid=0&token=0";
    // websocket管理，引用的第三方库
    private WsManager wsManager;
    // 用于向websocket发送数据
    private JSONObject jsonObject;
    // 记录websocket返回的妖灵列表
    private JSONArray jsonArray;
    // 记录当前查找到jsonArray中的哪个妖灵
    private int currentIndex = 0;

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
        initWebsocket();
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

    // 初始化websocket
    private void initWebsocket() {
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .pingInterval(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        wsManager = new WsManager.Builder(this)
                .wsUrl(wssHost)
                .needReconnect(true)
                .client(okHttpClient)
                .build();
        wsManager.setWsStatusListener(new WsStatusListener() {
            @Override
            public void onOpen(Response response) {
                super.onOpen(response);
            }

            @Override
            public void onMessage(String text) {
                super.onMessage(text);
                Log.i("Message", text);
            }

            @Override
            public void onMessage(ByteString bytes) {
                super.onMessage(bytes);
                Log.i("RECV Message", bytes.toString());
                byte[] bs = bytes.toByteArray();
                byte[] buffer = new byte[bs.length-4];
                System.arraycopy(bytes.toByteArray(), 4, buffer, 0, bs.length-4);
                String j = new String(buffer);
                Log.i("Message buffer", j);
                try {
                    JSONObject json = new JSONObject(j);
                    jsonArray = json.getJSONArray("sprite_list");
                    currentIndex = jsonArray.length() - 1;
                    Toast toast = Toast.makeText(getApplicationContext(), "来了", Toast.LENGTH_SHORT);
                    toast.show();
                    onClickNext();
                    Log.i("jsonArray", jsonArray.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onReconnect() {
                super.onReconnect();
            }

            @Override
            public void onClosing(int code, String reason) {
                super.onClosing(code, reason);
                onReconnect();
            }

            @Override
            public void onClosed(int code, String reason) {
                super.onClosed(code, reason);
                onReconnect();
            }

            @Override
            public void onFailure(Throwable t, Response response) {
                super.onFailure(t, response);
                onReconnect();
            }
        });
        wsManager.startConnect();
    }

    private void getPets() {
        try {
            jsonObject = new JSONObject();
            jsonObject.put("request_type", "1001");
            jsonObject.put("latitude", (int)(latitude*1000*1000));
            jsonObject.put("longtitude", (int)(longtitude*1000*1000));
            jsonObject.put("platform", 0);
            long times = System.currentTimeMillis();
            jsonObject.put("requestid", 7782306);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i("json: ", jsonObject.toString());
        String json = jsonObject.toString();
        int length = json.length();
        byte[] jsonByte = json.getBytes();
        Log.i("jsonbyte", jsonByte.toString());
        byte[] buffer = new byte[4+length];
        length += 4;
        buffer[0] = (byte)(length & 0xFF000000);
        buffer[1] = (byte)(length & 0xFF0000);
        buffer[2] = (byte)(length & 0xFF00);
        buffer[3] = (byte)(length & 0xFF);
        Log.i("string length", String.valueOf(length));
        Log.i("json byte length", String.valueOf(jsonByte.length));
        System.arraycopy(jsonByte, 0, buffer, 4, jsonByte.length);
        Log.i("buffer length", String.valueOf(buffer.length));

        Log.i("buffer", Arrays.toString(buffer));
        ByteString bytes = ByteString.of(buffer);
        Log.i("bytes", bytes.toString());
        wsManager.sendMessage(bytes);
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
        petSharedPreferences = getSharedPreferences("pet", this.MODE_PRIVATE);
        editor = petSharedPreferences.edit();
        petSet = new HashSet<String>(petSharedPreferences.getStringSet("selected", new HashSet<String>()));
        Log.i("petSet", petSet.toString());
    }

    // 显示筛选界面
    private void showFilter() {
        if (Build.VERSION.SDK_INT > 22) {
            if (Settings.canDrawOverlays(this)) {
                LayoutInflater layoutInflater = LayoutInflater.from(this);
                filterView = layoutInflater.inflate(R.layout.activity_filter, null);
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
            imgView.setId(imageId);
            if (petSet.contains(String.valueOf(imageId))) {
                imgView.setAlpha((float)1.0);
            } else {
                imgView.setAlpha((float)0.3);
            }
            imgView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int imageId = view.getId();
                    if (petSet.contains(String.valueOf(imageId))) {
                        petSet.remove(String.valueOf(imageId));
                        view.setAlpha((float)0.3);
                    } else {
                        petSet.add(String.valueOf(imageId));
                        view.setAlpha((float)1.0);
                    }
                    Log.i("petSet 2", petSet.toString());
                    editor.putStringSet("selected", petSet);
                    editor.commit();
                    Log.i("petSet 3", petSharedPreferences.getStringSet("selected", new HashSet<String>()).toString());
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
        getPets();
    }
    // 下一个
    public void onClickNext() {
        // 我这里逆序查找，因为，一般后台的妖灵都比较好
        if (jsonArray != null && jsonArray.length() > 0 && currentIndex > 0) {
            currentIndex --;
            try {
                JSONObject currentPet = jsonArray.getJSONObject(currentIndex);
                double  tmpNextLatitude = (double)currentPet.getInt("latitude") / (1000 * 1000);
                double tmpNextLongtitude = (double)currentPet.getInt("longtitude") / (1000 * 1000);
                GPS gps = gcj2gps84(tmpNextLatitude, tmpNextLongtitude);
                final double nextLatitude = gps.getLat();
                final double nextLongtitude = gps.getLon();
                final int sprite_id = currentPet.getInt("sprite_id");
                int gentime = currentPet.getInt("gentime");
                int lifetime = currentPet.getInt("lifetime");
                long currentTime = System.currentTimeMillis() / 1000;
                Log.i("gentime", String.valueOf(gentime));
                Log.i("currentTime", String.valueOf(currentTime));
                if (petSet.contains(String.valueOf(sprite_id)) && (gentime + lifetime) > (currentTime + 2)) {
                    Log.i("ID:", String.valueOf(sprite_id));
                    Thread moveThread = new Thread(new Runnable() {
                                                       @Override
                                                       public void run() {
                            final double latitudeStep = (nextLatitude - latitude) / 500;
                            final double longtitudeStep = (nextLongtitude - longtitude) / 500;
                            for (int i = 0; i < 500; i ++) {
                                try {
                                    Thread.sleep(1);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                latitude += latitudeStep;
                            }
                            for (int i = 0; i < 500; i ++) {
                                try {
                                    Thread.sleep(1);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                longtitude += longtitudeStep;
                            }
                            latitude = nextLatitude;
                            longtitude = nextLongtitude;
                    }
                    });
                    moveThread.start();
                } else {
                    onClickNext();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            getPets();
        }
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
        if (latitude == 0 && longtitude == 0) {
            latitude = location.getLatitude();
            longtitude = location.getLongitude();
            Toast toast = Toast.makeText(getApplicationContext(), "定位成功", Toast.LENGTH_SHORT);
            toast.show();
        }
        tencentMap.setCenter(new LatLng(latitude, longtitude));
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

    private void setLocation(double longtitude, double latitude) {

        // 因为我是在北京的，我把这个海拔设置了一个符合北京的范围，随机生成
        altitude = genDouble(38.0, 50.5);
        // 而定位有精度，GPS精度一般小于15米，我设置在1到15米之间随机
        accuracy = (float)genDouble(1.0, 15.0);

        // 下面就是自己设置定位的位置了
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setTime(System.currentTimeMillis());
        location.setLatitude(latitude);
        location.setLongitude(longtitude);
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
                                longtitude += speed;
                                break;
                            case 2:
                                latitude -= speed;
                                break;
                            case 3:
                                longtitude -= speed;
                                break;
                        }
                    }
                    setLocation(longtitude, latitude);
                }
            }
        });
        thread.start();
    }

    private class GPS {
        private double lat;
        private double lon;
        public GPS(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }
        public double getLat() {
            return lat;
        }
        public double getLon() {
            return lon;
        }
        public void setLat(double lat) {
            this.lat = lat;
        }
        public void setLon(double lon) {
            this.lon = lon;
        }
    }

    // GCJ02坐标转GPS84
    private GPS gcj2gps84(double lat, double lon) {
        double dLat = transLat(lon - 105.0, lat - 35.0);
        double dLon = transLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
        double mgLat = lat + dLat;
        double mgLon = lon + dLon;
        mgLat = lat * 2 - mgLat;
        mgLon = lon * 2 - mgLon;
        return new GPS(mgLat, mgLon);
    }
    private double transLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * pi) + 40.0 * Math.sin(y / 3.0 * pi)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * pi) + 320 * Math.sin(y * pi / 30.0)) * 2.0 / 3.0;
        return ret;
    }
    private double transLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * pi) + 40.0 * Math.sin(x / 3.0 * pi)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * pi) + 300.0 * Math.sin(x / 30.0 * pi)) * 2.0 / 3.0;
        return ret;
    }
}
