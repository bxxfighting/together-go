package com.example.move;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
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

import com.kongqw.rockerlibrary.view.RockerView;
import com.rabtman.wsmanager.WsManager;
import com.rabtman.wsmanager.listener.WsStatusListener;
import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;
import com.tencent.mapsdk.raster.model.BitmapDescriptorFactory;
import com.tencent.mapsdk.raster.model.LatLng;
import com.tencent.mapsdk.raster.model.Marker;
import com.tencent.mapsdk.raster.model.MarkerOptions;
import com.tencent.tencentmap.mapsdk.map.MapView;
import com.tencent.tencentmap.mapsdk.map.TencentMap;
import com.tencent.tencentmap.mapsdk.map.UiSettings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Response;
import okio.ByteString;


public class MainActivity extends AppCompatActivity implements TencentLocationListener {
    private double pi = 3.1415926535897932384626;
    public double a = 6378245.0;
    public double ee = 0.00669342162296594323;
    private float clickButtonAlpha = 0.8f;
    private float unClickButtonAlpha = 0.3f;
    private float clickHeadAlpha = 1.0f;
    private float unclickHeadAlpha = 0.3f;
    // 一次搜索，腾讯返回结果经纬度的跨度
    private double latSpan =  0.01369;
    private double lonSpan = 0.01786;
    private DisplayMetrics metrics;
    // 腾讯地图
    MapView mapView = null;
    TencentMap tencentMap = null;
    private Marker marker;
    private boolean isMarker = false;
    private boolean firstLocation = true;
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
    private int isPatrol = 0;
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
    private Button backButton;
    private Button filterButton;
    private Button mapButton;
    private Button nextButton;
    private Button patrolButton;
    // 定位范围
    private Button autoButton;
    private int autoCount = 0;
    // 判断控制器是收起还是展开
    private int isBack = 0;
    private RockerView rockerView;
    private double angle = 0;
    private int count = 1;
    private int loop = 1000;
    private int step = 200;
    // 悬浮窗地图
    private View floatMapView;
    private WindowManager.LayoutParams floatMapViewParams;
    private Button floatMapCloseButton;
    private MapView floatTencentMapView;
    private TencentMap floatTencentMap;

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
    private Map<Integer, Bitmap> headBitmaps = new HashMap<>();
    private Set<Integer> allPetSet = new LinkedHashSet<>();
    private Set<Integer> selectedPetSet = new HashSet<>();
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
    private JSONArray petJsonArray = new JSONArray();
    // 记录当前查找到jsonArray中的哪个妖灵
    private int currentIndex = 0;
    // 每次请求都有requestId，而成功返回时，同样有此Id，通过记录和比较requestId来判断，上次请求是否成功
    private long requestId;
    private boolean requestSuccess = false;
    private long checkRequestId;
    private Toast toast;
    // 触摸板

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init(savedInstanceState);
        continueLocation();
        showController();
    }

    private void init(Bundle savedInstanceState) {
        initPermission();
        // 初始化妖灵数据
        initPets();
        // 初始化各个控制窗口
        initWindowManager(savedInstanceState);
        initWebsocket();
        initMoveManager();
        initLocation();
    }

    private void initWindowManager(Bundle savedInstanceState) {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);

        // 以下为取其它activity内容
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        // 控制按钮悬浮窗
        controllerView = layoutInflater.inflate(R.layout.activity_controller, null);
        initController();
        // 筛选悬浮窗
        filterView = layoutInflater.inflate(R.layout.activity_filter, null);
        initFilter();
        showPets();
        // 地图悬浮窗
        floatMapView = layoutInflater.inflate(R.layout.activity_map, null);
        initMap(savedInstanceState);
    }

    private void initPermission() {
        // 定位权限
        // 模拟定位权限
        // 悬浮窗权限
        // 三种权限都是必须权限
    }

    // 初始化腾讯地图的一些信息
    private void initMap(Bundle savedInstanceState) {
        mapView = (MapView) floatMapView.findViewById(R.id.floatmapview);
        mapView.onCreate(savedInstanceState);
        tencentMap = mapView.getMap();
        tencentMap.setZoom(15);
        UiSettings uiSettings = mapView.getUiSettings();
        // 地图缩放
        // uiSettings.setZoomGesturesEnabled(false);
        // 地图滚动
        // uiSettings.setScrollGesturesEnabled(false);
        // 当前位置marker

        // 设置点击地图事件监听
        tencentMap.setOnMapClickListener(new TencentMap.OnMapClickListener() {
            @Override
            public void onMapClick(final LatLng latLng) {
                //handleMapClick(latLng.getLatitude(), latLng.getLongitude());
                getPets(latLng.getLatitude(), latLng.getLongitude());
            }
        });
        // 设置点击marker的事件监听
        tencentMap.setOnMarkerClickListener(new TencentMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                LatLng pos =  marker.getPosition();
                handleMapClick(pos.getLatitude(), pos.getLongitude());
                return false;
            }
        });
    }

    // 处理点击地图事件
    private void handleMapClick(final double lat, final double lon) {
        moveTo(lat, lon);
    }

    // 初始化腾讯定位的一些信息
    private void initLocation() {
        tencentLocationManager = TencentLocationManager.getInstance(this);
        tencentLocationManager.setCoordinateType(TencentLocationManager.COORDINATE_TYPE_WGS84);
        tencentLocationRequest = TencentLocationRequest.create();
        tencentLocationRequest.setInterval(100);
        tencentLocationManager.requestLocationUpdates(tencentLocationRequest, this);
    }

    // 初始化模拟定位的一些信息
    private void initMoveManager() {
        if (Build.VERSION.SDK_INT < 23) {
            if (Settings.Secure.getInt(this.getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION, 0) == 0) {
                simulateLocationPermission();
            }
        }
        random = new Random();
        try {
            locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
            locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false,
                    true, false, false, true,
                    true, true, 0, 5);
            locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
        } catch (SecurityException e) {
            simulateLocationPermission();
        }
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
            }

            @Override
            public void onMessage(ByteString bytes) {
                requestSuccess = true;
                super.onMessage(bytes);
                byte[] bs = bytes.toByteArray();
                byte[] buffer = new byte[bs.length-4];
                System.arraycopy(bytes.toByteArray(), 4, buffer, 0, bs.length-4);
                String j = new String(buffer);
                try {
                    JSONObject json = new JSONObject(j);
                    jsonArray = json.getJSONArray("sprite_list");
                    formatPets(jsonArray);
                    //onClickNext();
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

    private void formatPets(JSONArray ja) {
        JSONArray tmpJsonArray = new JSONArray();
        for (int i = 0; i < ja.length(); i ++) {
            try {
                JSONObject pet = ja.getJSONObject(i);
                int petId = pet.getInt("sprite_id");
                if (selectedPetSet.contains(petId)) {
                    double tmpNextLatitude = (double)pet.getInt("latitude") / (1000 * 1000);
                    double tmpNextLongtitude = (double)pet.getInt("longtitude") / (1000 * 1000);
                    GPS gps = gcj2gps84(tmpNextLatitude, tmpNextLongtitude);
                    pet.put("latitude", gps.getLat());
                    pet.put("longtitude", gps.getLon());
                    int gentime = pet.getInt("gentime");
                    int lifetime = pet.getInt("lifetime");
                    pet.put("endtime", gentime + lifetime);
                    tmpJsonArray.put(pet);
                    showPetToMap(petId, gps.getLat(), gps.getLon());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        // 给petJsonArray重新赋值
        petJsonArray = tmpJsonArray;
        currentIndex = petJsonArray.length() - 1;
        if (petJsonArray.length() == 0) {
            toast = Toast.makeText(getApplicationContext(), "无结果，请更改位置或增加筛选妖灵", Toast.LENGTH_LONG);
        } else {
            toast = Toast.makeText(getApplicationContext(), "搜索完毕", Toast.LENGTH_SHORT);
        }
        toast.show();
    }

    // 显示妖灵到地图上
    private void showPetToMap(int petId, double lat, double lon) {
        tencentMap.addMarker(new MarkerOptions()
                .anchor(0.5f, 0.5f)
                .position(new LatLng(lat, lon))
                .icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(headBitmaps.get(petId), dpi2pix(30), dpi2pix(30), true)))
                .draggable(true));
    }

    private void clearPetsFromMap() {
        tencentMap.clearAllOverlays();
        isMarker = false;
    }

    private void getPets(double lat, double lon) {
        clearPetsFromMap();
        toast = Toast.makeText(getApplicationContext(), "搜索中...", Toast.LENGTH_SHORT);
        toast.show();
        try {
            jsonObject = new JSONObject();
            jsonObject.put("request_type", "1001");
            jsonObject.put("latitude", (int)(lat*1000*1000));
            jsonObject.put("longtitude", (int)(lon*1000*1000));
            jsonObject.put("platform", 0);
            // 保证requestId为七位数字
            requestId = System.currentTimeMillis() % (10 * 1000 * 1000);
            jsonObject.put("requestid", requestId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String json = jsonObject.toString();
        int length = json.length();
        byte[] jsonByte = json.getBytes();
        byte[] buffer = new byte[4+length];
        length += 4;
        buffer[0] = (byte)(length & 0xFF000000);
        buffer[1] = (byte)(length & 0xFF0000);
        buffer[2] = (byte)(length & 0xFF00);
        buffer[3] = (byte)(length & 0xFF);
        System.arraycopy(jsonByte, 0, buffer, 4, jsonByte.length);
        final ByteString bytes = ByteString.of(buffer);
        // if (!wsManager.isWsConnected()) {
        //     wsManager.startConnect();
        // }
        // wsManager.sendMessage(bytes);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                requestSuccess = false;
                int try_count = 5;
                while (try_count > 0) {
                    if (requestSuccess) {
                        break;
                    }
                    wsManager.sendMessage(bytes);
                    try_count --;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    // 初始化控制悬浮窗
    private void initController() {
        // 悬浮窗参数
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
        controllerLayoutParams.width = dpi2pix(100);
        controllerLayoutParams.height = dpi2pix(290);
        // 这是悬浮窗处于屏幕的位置
        controllerLayoutParams.x = metrics.widthPixels;
        controllerLayoutParams.y = metrics.heightPixels / 2 - dpi2pix(240) / 2;

        // 悬浮窗各个组件
        stopButton = controllerView.findViewById(R.id.stopButton);
        autoButton = controllerView.findViewById(R.id.autoButton);
        backButton = controllerView.findViewById(R.id.backButton);
        filterButton = controllerView.findViewById(R.id.filterButton);
        mapButton = controllerView.findViewById(R.id.mapButton);
        nextButton = controllerView.findViewById(R.id.nextButton);
        patrolButton = controllerView.findViewById(R.id.patrolButton);
        // 这里是控制移动速度的，有一个基础速度，然后根据速度条的位置增加相应的速度值
        // 因为这里调用了其它layout内的元素，所以需要用LayoutInflater获取到对应的layout再操作
        speedSeekBar = (SeekBar) controllerView.findViewById(R.id.speedSeekBar);
        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                speed = baseSpeed + baseSpeed * (i / 30);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        // 摇杆功能，监听旋转角度，根据角度来进行方向移动
        rockerView = controllerView.findViewById(R.id.rockerView);
        rockerView.setCallBackMode(RockerView.CallBackMode.CALL_BACK_MODE_STATE_CHANGE);
        rockerView.setOnAngleChangeListener(new RockerView.OnAngleChangeListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void angle(double v) {
                angle = v;
            }

            @Override
            public void onFinish() {

            }
        });
    }

    // 显示控制悬浮窗
    private void showController() {
        if (Settings.canDrawOverlays(this)) {
            // 这就是去获取activity_controller，这样才能用它里面的元素
            windowManager.addView(controllerView, controllerLayoutParams);
        }
    }

    // 初始化筛选
    private void initFilter() {
        headLinearLayout = (LinearLayout) filterView.findViewById(R.id.headLinearLayout);
        // 筛选悬浮窗参数
        filterLayoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            filterLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            filterLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        filterLayoutParams.format = PixelFormat.RGBA_8888;
        filterLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        filterLayoutParams.gravity = Gravity.START | Gravity.TOP;
        // 这是悬浮窗的宽高
        filterLayoutParams.width = metrics.widthPixels;
        filterLayoutParams.height = metrics.heightPixels;
        // 这是悬浮窗处于屏幕的位置
        int width = (metrics.widthPixels - 110) / 10;
        int height = width;
        headLayoutParams = new LayoutParams(width, height);
        headLayoutParams.setMargins(5, 5, 5, 5);

        // 获取assets下的小妖头像
        assetManager = this.getResources().getAssets();
        try {
            // 将所有头像都放到assets/heads下，避免其它图片干扰
            headImages = assetManager.list("heads");
        } catch(IOException e) {
            e.printStackTrace();
        }
        int col = (int)Math.ceil(headImages.length / 10);
        filterLayoutParams.x = 5;
        filterLayoutParams.y = (metrics.heightPixels - col * 10 - width * col) / 2 - 150;
        filterLayoutParams.height = col * 10 + width * col + 300;
        // 记录小妖当前是否被选，以小妖id为key，false为未选、true为已选
        petSharedPreferences = getSharedPreferences("pet", this.MODE_PRIVATE);
        editor = petSharedPreferences.edit();
        petSet = new HashSet<String>(petSharedPreferences.getStringSet("selected", new HashSet<String>()));
        for (String str : petSet) {
            selectedPetSet.add(Integer.valueOf(str));
        }
    }

    // 显示筛选界面
    private void showFilter() {
        if (Build.VERSION.SDK_INT > 22) {
            if (Settings.canDrawOverlays(this)) {
                windowManager.addView(filterView, filterLayoutParams);
            }
        }
        //showPets();
    }
    private void initFloatMap() {
        floatMapViewParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            floatMapViewParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            floatMapViewParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        floatMapViewParams.format = PixelFormat.RGBA_8888;
        floatMapViewParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        floatMapViewParams.gravity = Gravity.START | Gravity.TOP;
        floatMapViewParams.width = metrics.widthPixels - 40;
        floatMapViewParams.x = 40 / 2;
        floatMapViewParams.height = metrics.heightPixels - 40 * (metrics.heightPixels / metrics.widthPixels);
        floatMapViewParams.y = (metrics.heightPixels - floatMapViewParams.height) / 2;
        LinearLayout floatMapLinearLayout = (LinearLayout)floatMapView.findViewById(R.id.floatmaplinearlayout);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)floatMapLinearLayout.getLayoutParams();
        params.width = floatMapViewParams.width;
        params.height = floatMapViewParams.height - 200;
        floatMapLinearLayout.setLayoutParams(params);
        floatMapCloseButton = floatMapView.findViewById(R.id.closefloatmapbutton);
        showFloatMap();
    }

    //显示地图悬浮窗
    private void showFloatMap() {
        if (Build.VERSION.SDK_INT > 22) {
            if (Settings.canDrawOverlays(this)) {
                windowManager.addView(floatMapView, floatMapViewParams);
            }
        }
    }
    //隐藏地图悬浮窗
    private void removeFloatMap() {
        windowManager.removeView(floatMapView);
    }

    private void initPets() {
        // 获取assets下的小妖头像
        assetManager = this.getResources().getAssets();
        try {
            // 将所有头像都放到assets/heads下，避免其它图片干扰
            headImages = assetManager.list("heads");
        } catch(IOException e) {
            e.printStackTrace();
        }
        InputStream input = null;
        for (int i = 0; i < headImages.length; i ++) {
            int petId = Integer.valueOf(headImages[i].substring(0, headImages[i].indexOf(".")));
            allPetSet.add(petId);
            try {
                input = assetManager.open(headImages[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            headBitmaps.put(petId, BitmapFactory.decodeStream(input));
        }
    }

    // 显示小妖头像
    // 这里就是读取assets中的图片，一排显示10个
    private void showPets() {
        InputStream input = null;
        LinearLayout imageLinearLayout = null;
        int count = 0;
        for (int petId : allPetSet) {
            // 每十个一换行
            if (count % 10 == 0) {
                imageLinearLayout = new LinearLayout(this);
                imageLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
                headLinearLayout.addView(imageLinearLayout);
            }
            count ++;
            final ImageView imgView = new ImageView(this);
            imgView.setLayoutParams(headLayoutParams);
            imgView.setImageBitmap(headBitmaps.get(petId));
            imgView.setId(petId);
            if (selectedPetSet.contains(petId)) {
                imgView.setAlpha(clickHeadAlpha);
            } else {
                imgView.setAlpha(unclickHeadAlpha);
            }
            // 监听图标点击事件来切换选中与未选中状态
            imgView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int imageId = view.getId();
                    if (selectedPetSet.contains(imageId)) {
                        petSet.remove(String.valueOf(imageId));
                        selectedPetSet.remove(imageId);
                        view.setAlpha(unclickHeadAlpha);
                    } else {
                        petSet.add(String.valueOf(imageId));
                        selectedPetSet.add(imageId);
                        view.setAlpha(clickHeadAlpha);
                    }
                    // 每一次改变，都将数据写入到SharedPreferences，以便保存用户数据
                    editor.putStringSet("selected", petSet);
                    editor.commit();
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
        setButtonColor((Button)view);
        switch (view.getId()) {
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
            case R.id.closeFilterButton:
                removeFilter();
                break;
            case R.id.mapButton:
                onClickFloatMap();
                break;
            case R.id.closefloatmapbutton:
                removeFloatMap();
                break;
            case R.id.nextButton:
                onClickNext();
                break;
            case R.id.patrolButton:
                onPatrolClick();
                break;
        }
    }
    // dpi转pix
    private int dpi2pix(float dpi) {
        return (int)(dpi * metrics.density + 0.5f);
    }

    // 控制面板是展开还是收回的方法，由backButton调用
    // 主要就是重新设置悬浮窗的宽度，然后更新悬浮窗
    public void onClickBack() {
        isBack += 1;
        if (isBack % 2 == 1) {
            // controllerLayoutParams.width = 260 / 2 - 10;
            controllerLayoutParams.width = dpi2pix(50);
            // controllerLayoutParams.height = (260 / 2 - 10) * 2;
            controllerLayoutParams.height = dpi2pix(150);
            windowManager.updateViewLayout(controllerView, controllerLayoutParams);
            backButton.setText("开");
        } else {
            controllerLayoutParams.width = dpi2pix(100);
            controllerLayoutParams.height = dpi2pix(290);
            windowManager.updateViewLayout(controllerView, controllerLayoutParams);
            backButton.setText("收");
        }
    }
    // 设置要筛选小妖
    public void onClickFilter() {
        showFilter();
    }
    private void onClickFloatMap() {
        initFloatMap();
    }
    private void onClickAuto() {
        getPets(latitude, longtitude);
    }
    // 自动到小妖身边
    public void onClickNext() {
        // 我这里逆序查找，因为，一般后台的妖灵都比较好
        double nextLatitude = 0;
        double nextLongtitude = 0;
        if (petJsonArray != null && petJsonArray.length() > 0 && currentIndex >= 0) {
            for (; currentIndex >= 0; currentIndex --) {
                try {
                    JSONObject currentPet = petJsonArray.getJSONObject(currentIndex);
                    nextLatitude = currentPet.getDouble("latitude");
                    nextLongtitude = currentPet.getDouble("longtitude");
                    final int sprite_id = currentPet.getInt("sprite_id");
                    int endtime = currentPet.getInt("endtime");
                    long currentTime = System.currentTimeMillis() / 1000;
                    if (selectedPetSet.contains(sprite_id) && endtime > (currentTime + 2)) {
                        break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            // 如果循环退出时，currentIndex大于等于0就说明是break出来的
            if (currentIndex >= 0) {
                moveTo(nextLatitude, nextLongtitude);
                currentIndex --;
            } else {
                getPets(latitude, longtitude);
            }
        } else {
            getPets(latitude, longtitude);
        }
    }
    private void moveTo(final double lat, final double lon) {
        Thread moveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                final double latitudeStep = (lat - latitude) / 3000;
                final double longtitudeStep = (lon - longtitude) / 3000;
                // 这里我想的是，用3秒走到对应的坐标
                for (int i = 1; i < 3000; i ++) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    latitude += latitudeStep;
                    longtitude += longtitudeStep;
                }
                // 最后直接将要去的坐标进行赋值，保证是正确的位置
                latitude = lat;
                longtitude = lon;
            }
        });
        moveThread.start();
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
        // 自动点击屏幕操作
        // int x = metrics.widthPixels / 2;
        // int y = metrics.heightPixels / 2 - 50 * 3 / 2;
        // String[] order = {"input", "tap", "" + x, "" + y};
        // try {
        //     new ProcessBuilder(order).start();
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }


        if (firstLocation && latitude != 0 && longtitude != 0) {
            firstLocation = false;
            tencentMap.setCenter(new LatLng(latitude, longtitude));
        }
        if (!isMarker) {
            marker = tencentMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.defaultMarker())
                    .draggable(true));
            isMarker = true;
        }
        marker.setPosition(new LatLng(latitude, longtitude));
    }

    @Override
    public void onStatusUpdate(String name, int status, String desc) {

    }

    private void setButtonColor(Button btn) {
        int alpha = genInt(0x8D, 0xEF);
        btn.setBackgroundColor((alpha<<24) | random.nextInt(0x00FFFFFF));
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
        try {
            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, location);
        } catch (SecurityException e) {
            simulateLocationPermission();
        }
    }

    // 持续定位
    public void continueLocation() {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
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
                        if (isPatrol % 2 == 0) {
                            if ( angle >= 0 && angle < 90) {
                                double radinas = Math.toRadians(angle);
                                latitude -= Math.sin(radinas) * speed;
                                longtitude += Math.cos(radinas) * speed;
                            } else if (angle >= 90 && angle < 180) {
                                double radinas = Math.toRadians(180 - angle);
                                latitude -= Math.sin(radinas) * speed;
                                longtitude -= Math.cos(radinas) * speed;
                            } else if (angle >= 180 && angle < 270) {
                                double radinas = Math.toRadians(angle - 180);
                                latitude += Math.sin(radinas) * speed;
                                longtitude -= Math.cos(radinas) * speed;
                            } else {
                                double radinas = Math.toRadians(360 - angle);
                                latitude += Math.sin(radinas) * speed;
                                longtitude += Math.cos(radinas) * speed;
                            }
                        } else {
                            // 每走多少步后换方向
                            if (count % loop == 0) {
                                angle += 90;
                                angle %= 360;
                                if (angle == 270) {
                                    loop += step;
                                }
                            }
                            count += 1;
                            if (angle == 0) {
                                longtitude += speed;
                            } else if (angle == 90) {
                                latitude -= speed;
                            } else if (angle == 180) {
                                longtitude -= speed;
                            } else if (angle == 270) {
                                latitude += speed;
                            }
                        }
                    }
                    setLocation(longtitude, latitude);
                }
            }
        });
        thread.start();
    }

    // 巡逻
    private void onPatrolClick() {
        isRun += 1;
        isPatrol += 1;
        angle = 0;
        count = 1;
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

    // 悬浮窗提示
    private void floatWindowPermission() {
        new AlertDialog.Builder(this)
                .setTitle("启用悬浮窗")
                .setMessage(("必须开启此权限"))
                .setPositiveButton("设置",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                try {
                                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                                    startActivity(intent);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .show();
    }

    // 模拟定位的权限
    private void simulateLocationPermission() {
        new AlertDialog.Builder(this)
                .setTitle("启用模拟位置")
                .setMessage("在开发者选项中模拟位置应用选择此程序")
                .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                            startActivity(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .show();
    }

    // 获取下一次定位点
    private void getNextLocation() {

    }
}
