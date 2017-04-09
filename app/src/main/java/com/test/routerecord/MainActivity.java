package com.test.routerecord;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.MyLocationStyle;
import com.smartdevicelink.proxy.rpc.enums.CompassDirection;
import com.test.routerecord.applink.AppLinkService;


/**
 * AMapV1地图中介绍如何显示世界图
 */
public class MainActivity extends Activity implements OnClickListener,LocationSource,LocationSource.OnLocationChangedListener {

    private MapView mapView;
    private AMap aMap;
    private Button basicmap;
    private Button rsmap;
    private TextView speedview;
    private RadioGroup mRadioGroup;
    private OnLocationChangedListener mListener;

    private ImageView imageView;
    private ImageView imageView1;

    private static final String TAG = "MainActivity";

    private static MainActivity INSTANCE = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startAppLinkService();
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);// 此方法必须重写

        init();
        INSTANCE = this;

    }
    public static MainActivity getInstance(){
        return INSTANCE;
    }
    /*
 * Called in onCreate() to start AppLink service so that the app is
 * listening for a SYNC connection in the case the app is installed or
 * restarted while the phone is already connected to SYNC.
 */
    public void startAppLinkService() {
//		BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
//		if (mBtAdapter != null && mBtAdapter.isEnabled()) {
        Log.d(TAG, "startAppLinkService: ");
        Intent intent = new Intent();
        intent.setClass(this, AppLinkService.class);
        startService(intent);
//		}
    }

    /*
     * Called in onDestroy() to reset AppLink service when user exits the app or
     * the app crashes
     */
    public void resetAppLinkService() {
        AppLinkService service = AppLinkService.getInstance();
        if(service != null){
            service.resetProxy();
        }
    }
    /**
     * 设置一些amap的属性
     */
    public void setUpMap() {
        if(aMap == null)return;
        // 自定义系统定位小蓝点
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory
                .fromResource(R.mipmap.location_marker));// 设置小蓝点的图标
        myLocationStyle.strokeColor(Color.BLACK);// 设置圆形的边框颜色
        myLocationStyle.radiusFillColor(Color.argb(100, 0, 0, 180));// 设置圆形的填充颜色
//         myLocationStyle.anchor(int,int)//设置小蓝点的锚点
//        myLocationStyle.getAnchorU()
        myLocationStyle.strokeWidth(1.0f);// 设置圆形的边框粗细
        aMap.setMyLocationStyle(myLocationStyle);
//        aMap.setMyLocationRotateAngle();
//        aMap.setOnMyLocationChangeListener(this);
        aMap.setLocationSource(this);// 设置定位监听
        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        // aMap.setMyLocationType()

//        Drawable drawable = Drawable.createFromResourceStream();
    }
    /**
     * 初始化AMap对象
     */
    private void init() {
        if (aMap == null) {
            aMap = mapView.getMap();
            if( AppLinkService.getInstance() != null) {
                setUpMap();
            }
        }
        speedview = (TextView)findViewById(R.id.speed);
        imageView = (ImageView)findViewById(R.id.imageview);
        imageView.setImageResource(R.mipmap.location_marker);
        imageView1 = (ImageView)findViewById(R.id.imageview1);
        imageView1.setImageResource(R.mipmap.location_marker);
//        basicmap = (Button)findViewById(R.id.basicmap);
//        basicmap.setOnClickListener(this);
//        rsmap = (Button)findViewById(R.id.rsmap);
//        rsmap.setOnClickListener(this);

        mRadioGroup = (RadioGroup) findViewById(R.id.check_language);

        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radio_en) {
                    aMap.setMapLanguage(AMap.ENGLISH);
                } else {
                    aMap.setMapLanguage(AMap.CHINESE);
                }
            }
        });
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        resetAppLinkService();
        INSTANCE = null;
    }

    @Override
    public void onClick(View v) {
//        switch (v.getId()) {
//            case R.id.basicmap:
//                aMap.setMapType(AMap.MAP_TYPE_NORMAL);// 矢量地图模式
//                break;
//            case R.id.rsmap:
//                aMap.setMapType(AMap.MAP_TYPE_SATELLITE);// 卫星地图模式
//                break;
//        }

    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        Log.d(TAG, "activate: ");
        mListener = onLocationChangedListener;
        if( mListener != null){
            AppLinkService.getInstance().addLocationListener(mListener);
        }

    }
    public static final String speed = "Speed: ";

    public void updateSpeed(final float speed){
        speedview.post(new Runnable() {
            @Override
            public void run() {
                speedview.setText("Speed: "+speed+" m/s; " + (speed*3.6)+" km/h");
            }
        });
    }

    public void updateHeading(final double heading,final CompassDirection direction){
        Log.i(TAG, "updateHeading: "+heading);

        imageView.post(new Runnable() {
            @Override
            public void run() {
                imageView.setRotation((float)heading);
            }
        });
        imageView1.post(new Runnable() {
            @Override
            public void run() {
                float degree = 0.0f;
                if( direction == CompassDirection.EAST) {
                    degree = 90f;
                } else if ( direction == CompassDirection.WEST){
                    degree = 270f;
                }else if ( direction == CompassDirection.SOUTH){
                    degree = 180f;
                }else if ( direction == CompassDirection.NORTH){
                    degree = 0f;
                }else if ( direction == CompassDirection.SOUTHEAST){
                    degree = 135f;
                }else if ( direction == CompassDirection.SOUTHWEST){
                    degree = 225f;
                }else if ( direction == CompassDirection.NORTHEAST){
                    degree = 45f;
                }else if ( direction == CompassDirection.NORTHWEST){
                    degree = 315;
                }
                imageView1.setRotation(degree);
            }
        });
    }
    @Override
    public void deactivate() {
        Log.d(TAG, "deactivate: ");
//        if( mListener != null){
//        }
    }

    @Override
    public void onLocationChanged(Location location) {

    }
}
