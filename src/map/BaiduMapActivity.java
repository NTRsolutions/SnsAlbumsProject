package map;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import network.HttpThread;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import album.entry.LoginActivity;
import album.entry.MainViewActivity;
import album.entry.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.GeoPoint;
import com.baidu.mapapi.ItemizedOverlay;
import com.baidu.mapapi.LocationListener;
import com.baidu.mapapi.MKAddrInfo;
import com.baidu.mapapi.MKDrivingRouteResult;
import com.baidu.mapapi.MKGeocoderAddressComponent;
import com.baidu.mapapi.MKLocationManager;
import com.baidu.mapapi.MKPlanNode;
import com.baidu.mapapi.MKPoiResult;
import com.baidu.mapapi.MKSearch;
import com.baidu.mapapi.MKSearchListener;
import com.baidu.mapapi.MKTransitRouteResult;
import com.baidu.mapapi.MKWalkingRouteResult;
import com.baidu.mapapi.MapActivity;
import com.baidu.mapapi.MapController;
import com.baidu.mapapi.MapView;
import com.baidu.mapapi.MyLocationOverlay;
import com.baidu.mapapi.OverlayItem;
import com.baidu.mapapi.PoiOverlay;
import com.baidu.mapapi.RouteOverlay;
import com.baidu.mapapi.TransitOverlay;


/*
* Copyright (c) 2012,UIT-ESPACE
* All rights reserved.
*
* 文件名称：BaiduMapActivity.java  (特此注明,由于Google跟大陆的微妙关系,导致谷歌地图的使用不太稳定,因此转用百度地图)
* 摘 要：地图模块
* 
* 功能：
* 1.获取我的位置
* 2.分享我的位置
* 3.路线搜索 （公交、驾车、步行）
* 4.获取同城好友的地图标示
* 5.获取到好友位置的路线
* 6.地图视图修改
* 7.出租车功能      (新增)
*  
* 当前版本：1.1
* 作 者：何红辉
* 完成日期：2012年11月3日
*
* 取代版本：1.0
* 原作者 ：何红辉
* 完成日期：2012年9月12日
* 
*/


public class BaiduMapActivity extends MapActivity{

	private BMapManager mBMapManager = null;									// 地图管理对象
	private MKLocationManager mLocationManager = null;							// 位置管理器
	private MapView mMapView = null;											// MapView对象
	private final String MAP_KEY = "382D9C48CADF05B90D8CB985772514087A8DB779";	// 百度地图的key
	private MapController mMapContoller = null;
	private GeoPoint mMyGeoPoint;
	private MyOverItem mOverLays = null;										// 地图上的覆盖物
	private final String TAG = "BAIDU MAP";
	
    private MKSearch mMKSearch;  												// 查询服务
    private GeoPoint geoPointTo;												// 好友的位置
    private int mPoiFlag = 1; 													// 路线行驶方式
    private static View mPopView = null;										// 点击mark时弹出的气泡View
	private Button findBtn = null;												// 找好友路线
	private String mFCity = null;												// 好友所在城市
	private int mAddrFlag = 0;													// 获取城市的类型
	private int mIndex = 0;														// 菜单选择的索引
	private int mFindBtnFlag = 0;												// 搜索按钮还是呼叫
	private String mTelephone = null;
	
	
	/**
	 * 功能 ： 页面创建
	 * (non-Javadoc)
	 * @see android.app.ActivityGroup#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		// 设置为无标题模式
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.baidumap);
				
		// 地图管理控件初始化
		mBMapManager = new BMapManager(this);
		mBMapManager.init(MAP_KEY, null);
		super.initMapActivity(mBMapManager);
		// 初始化位置管理器
	 	mLocationManager = mBMapManager.getLocationManager();
	 	// 注册监听,位置改变则实时获得本机位置
	 	mLocationManager.requestLocationUpdates( mLocationListener );
	 	
		// 地图显示控件
		mMapView = (MapView)findViewById(R.id.bmapsView);
		mMapView.setBuiltInZoomControls( true );				// 启动内置的缩放控件
		mMapView.setTraffic( true );							// 开启实时交通信息
		
		mMapContoller = mMapView.getController();				// 获取地图控制权
		GeoPoint point = new GeoPoint((int) (39.02 * 1E6),
									   (int) (121.44 * 1E6));   // 用给定的经纬度构造一个GeoPoint，单位是微度 (度 * 1E6)
		mMapContoller.setCenter( point );  						// 设置地图中心点
		mMapContoller.setZoom(12);    							// 设置地图zoom级别
		
 		mMKSearch = new MKSearch();								// 搜索类的对象
    	mMKSearch.init(mBMapManager, new MySearchListener()); 	// 搜索类
    	
    	initComponents();										// 初始化控件
		turnGPSOn();											// 开启GPS
		
		mHandler.postDelayed(myLocationRunnable, 1500);			// 两秒后显示我的位置,wifi模式下暂时屏蔽
		
		MainViewActivity.addActivityToHashSet( this );	// 将页面添加到map中
		
	}

	
	/*
	 * (非 Javadoc,覆写的方法) 
	 * <p>Title: onRestoreInstanceState</p> 
	 * <p>Description: </p> 
	 * @param savedInstanceState 
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
		Log.d(TAG, "onRestoreInstanceState取出我的位置" + savedInstanceState.getString("myLocation")) ;
	}

	
	/*
	 * (非 Javadoc,覆写的方法) 
	 * <p>Title: onSaveInstanceState</p> 
	 * <p>Description: </p> 
	 * @param outState 
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {

		outState.putString("myLocation", mMyGeoPoint.toString() ) ;
		Log.d(TAG, "onSaveInstanceState保存我的位置") ;
		super.onSaveInstanceState(outState);
	}


	/**
	 * 功能 ： 是否显示路线
	 * (non-Javadoc)
	 * @see com.baidu.mapapi.MapActivity#isRouteDisplayed()
	 */
	@Override
	protected boolean isRouteDisplayed() {
	
		return false;
	}
	
	/**
	 * 功能 ： 页面销毁 (non-Javadoc)
	 * 
	 * @see com.baidu.mapapi.MapActivity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		if (mBMapManager != null) {
			mBMapManager.destroy();
			mBMapManager = null;
		}
		// 将本页面从Set中移除
		MainViewActivity.removeFromSet(this);
		super.onDestroy();
	}

	/**
	 * 功能 ： 页面转入暂停状态 (non-Javadoc)
	 * 
	 * @see com.baidu.mapapi.MapActivity#onPause()
	 */
	@Override
	protected void onPause() {
		if (mBMapManager != null) {
			mBMapManager.stop();
		}
		// 关闭GPS
		turnGPSOff();
		super.onPause();

	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see com.baidu.mapapi.MapActivity#onPause()
	 */
	@Override
	protected void onResume() {
		if (mBMapManager != null) {
			mBMapManager.start();
		}
		super.onResume();
	}

	/**
	 * 
	 * @Method: init_components
	 * @Description: 初始化组件
	 * @return void 返回类型
	 * @throws
	 */
	private void initComponents() {

		// 覆盖物不图标
		Drawable marker = getResources().getDrawable(R.drawable.location_48);
		// 地图上的覆盖物
		mOverLays = new MyOverItem(marker, BaiduMapActivity.this);

		// 创建点击mark时的弹出泡泡
		mPopView = super.getLayoutInflater().inflate(R.layout.popview, null);
		mMapView.addView(mPopView, new MapView.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, null,
				MapView.LayoutParams.TOP_LEFT));
		mPopView.setVisibility(View.GONE);

		// 弹出的气泡窗口中的按钮,点击时获取到好友位置的路线
		findBtn = (Button) mPopView.findViewById(R.id.findBtn);
		findBtn.getBackground().setAlpha(200);
		findBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				if (geoPointTo != null && mFindBtnFlag == 1) {
					mPopView.setVisibility(View.GONE);
					displayToast("路线搜索中,请稍后...");
					getRoute(geoPointTo); // 查找到好友位置的路线
				} else if (mFindBtnFlag == 2) { // 出租车模式

					displayToast("拨打电话给用户");
					/* 建构一个新的Intent 运行action.CALL的常数与通过Uri将字符串带入 */
					Intent myIntentDial = new Intent(
							"android.intent.action.CALL", Uri.parse("tel:"
									+ mTelephone));
					startActivity( myIntentDial );
				}
			}
		});

	}

	
	/**
	 * 功能 ： 初始化后向UI线程投递消息,显示我的位置,将我的位置贴到地图上
	 * 
	 */
	Handler mHandler = new Handler();
	Runnable myLocationRunnable = new Runnable() {

		@Override
		public void run() {

			showMyLocation();
			if (mMyGeoPoint != null) {
				// 将我的标识添加到地图上
				addFriendToMap(mMyGeoPoint, 1, LoginActivity.mineID,
						LoginActivity.mineName);
			}

		}
	};
	    
		
	/**
	 * 功能 ： 在地图上显示我的位置
	 * 
	 */
	private boolean showMyLocation() {
		// 添加定位图层
		MyLocationOverlay myLocation = new MyLocationOverlay(this, mMapView);
		myLocation.enableMyLocation(); 				// 启用定位
		mMapView.getOverlays().add( myLocation );   // 在地图上绘制出当前位置的Overlay
		mMapView.invalidate() ;
		
		// 获取我的坐标
		mMyGeoPoint = myLocation.getMyLocation();
		if (mMapContoller != null && mMyGeoPoint != null) {
			mMapContoller.animateTo(mMyGeoPoint);
			mMapContoller.setCenter(mMyGeoPoint);
			return true;
		} else {
			Log.d(TAG, "mMapContoller 空指针.");
		}
		return false;

	}

	/**
	 * 位置变化监听器,实时更新自己的位置
	 */
	LocationListener mLocationListener = new LocationListener() {
		
		@Override
		public void onLocationChanged(Location location) {
			// 位置改变,则显示我的位置
			showMyLocation();
		}
	};

	
	/**
	 * 功能 ：开启GPS
	 * 
	 */
	private void turnGPSOn() {

		// 获取允许操作GPS的provider
		String provider = Settings.Secure.getString(getContentResolver(),
				Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

		if (!provider.contains("gps") && canToggleGPS()) { // 如果GPS是关闭的和GPS能启动

			final Intent poke = new Intent();
			poke.setClassName("com.android.settings",
					"com.android.settings.widget.SettingsAppWidgetProvider");
			poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
			poke.setData(Uri.parse("3"));
			sendBroadcast(poke);
		}
	}

	/**
	 * 功能 ：关闭GPS
	 */
	private void turnGPSOff() {
		String provider = Settings.Secure.getString(getContentResolver(),
				Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

		if (provider.contains("gps")) { // if gps is enabled
			final Intent poke = new Intent();
			poke.setClassName("com.android.settings",
					"com.android.settings.widget.SettingsAppWidgetProvider");
			poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
			poke.setData(Uri.parse("3"));
			sendBroadcast(poke);
			Log.d("GPS", "**关闭GPS**");
		}
	}

	/**
	 * 功能 ：判断 GPS能否打开
	 * 
	 */
	private boolean canToggleGPS() {
		PackageManager pacman = getPackageManager();
		PackageInfo pacInfo = null;

		try {
			pacInfo = pacman.getPackageInfo("com.android.settings",
					PackageManager.GET_RECEIVERS);
		} catch (NameNotFoundException e) {
			return false;
		}

		if (pacInfo != null) {
			for (ActivityInfo actInfo : pacInfo.receivers) {
				// 是否能开启GPS
				if (actInfo.name
						.equals("com.android.settings.widget.SettingsAppWidgetProvider")
						&& actInfo.exported) {
					Log.d(TAG, "GPS能开启");
					return true;
				}
			}
		}

		Log.d(TAG, "GPS不能能开启");
		return false;
	}

	
	/**
	 * 功能 ： 菜单 (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// setIcon()方法为菜单设置图标，这里使用的是系统自带的图标
		menu.add(Menu.NONE, Menu.FIRST + 1, 1, "我的位置").setIcon(
				android.R.drawable.ic_menu_mylocation);

		menu.add(Menu.NONE, Menu.FIRST + 2, 2, "搜索").setIcon(
				android.R.drawable.ic_menu_search);

		menu.add(Menu.NONE, Menu.FIRST + 3, 3, "出租车").setIcon(
				android.R.drawable.ic_menu_compass);

		menu.add(Menu.NONE, Menu.FIRST + 4, 4, "视图").setIcon(
				android.R.drawable.ic_menu_slideshow);
		menu.add(Menu.NONE, Menu.FIRST + 5, 5, "分享位置").setIcon(
				android.R.drawable.ic_menu_share);
		menu.add(Menu.NONE, Menu.FIRST + 6, 6, "清空视图").setIcon(
				android.R.drawable.ic_menu_info_details);
		menu.add(Menu.NONE, Menu.FIRST + 7, 7, "退出").setIcon(
				android.R.drawable.ic_lock_power_off);

		return true;
	}

	/**
	 * 功能 ： 菜单选择事件 (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case Menu.FIRST + 1: // 我的位置
			showMyLocation();
			break;
		case Menu.FIRST + 2: // 搜索菜单,包括路线搜索、周边搜索、同城好友
			searchMenu();
			break;
		case Menu.FIRST + 3: // 出租车功能
			texiSelectMenu();
			break;
		case Menu.FIRST + 4: // 视图修改
			changeMapViewMenu();
			break;
		case Menu.FIRST + 5: // 分享我的位置
			shareMyLocation();
			break;
		case Menu.FIRST + 6: // 清空视图
			clearMapView();
			showMyLocation();
			break;
		case Menu.FIRST + 7: // 退出程序
			MainViewActivity.killCurrentApp(BaiduMapActivity.this);
			break;
		default:
			break;

		}
		return false;

	} // end of onOptionsItemSelected
	 	
	
	/**
	 * 
	 * @Method: clearMapView
	 * @Description: 清空整个地图view
	 * @throws
	 */
	private void clearMapView() {
		try {
			mMapView.getOverlays().clear();
			mMapView.getOverlays().clear();
			// 好友的视图清空
			mOverLays.clearOverLayItems();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @Method: getRoute
	 * @Description: 查找路线,从我的位置到好友的位置
	 * @param fGeoPoint
	 * @return void 返回类型
	 * @throws
	 */
	private void getRoute(GeoPoint fGeoPoint) {
		// 起点
		MKPlanNode start = new MKPlanNode();
		start.pt = mMyGeoPoint;

		// 终点
		MKPlanNode end = new MKPlanNode();
		end.pt = fGeoPoint;

		// 设置驾车路线搜索策略，时间优先、费用最少或距离最短
		mMKSearch.setDrivingPolicy(MKSearch.ECAR_TIME_FIRST);
		mMKSearch.drivingSearch(null, start, null, end);
	}
	 	
	/**
	 * 
	 * @Method: getRoute
	 * @Description: 查找路线,以地址查询
	 * @param from
	 *            源地址
	 * @param to
	 *            目标地址
	 * @param city
	 *            所在城市
	 * @return void 返回类型
	 * @throws
	 */
	private void getRoute(String from, String to, String city) {

		Log.d(TAG, city + from + to);
		if (mMapView.getOverlays().size() > 3) {
			mMapView.getOverlays().remove(0);
		}

		// 起点
		MKPlanNode stNode = new MKPlanNode();
		stNode.name = from;
		// 如果没有指定起点地址则默认为我的位置
		if (from.equals("") && mMyGeoPoint != null) {
			stNode.pt = mMyGeoPoint;
		}

		// 终点
		MKPlanNode enNode = new MKPlanNode();
		enNode.name = to;

		// 设置驾车路线搜索策略，时间优先、费用最少或距离最短
		mMKSearch.setDrivingPolicy(MKSearch.ECAR_TIME_FIRST);
		if (mPoiFlag == 1) {

			mMKSearch.drivingSearch(city, stNode, city, enNode);
			displayToast("驾车路线搜索中...");

		} else if (mPoiFlag == 2) {

			displayToast("公交路线搜索中...");
			mMKSearch.setTransitPolicy(MKSearch.EBUS_TRANSFER_FIRST);
			mMKSearch.transitSearch(city, stNode, enNode);

		} else if (mPoiFlag == 3) {

			displayToast("步行路线搜索中...");
			mMKSearch.walkingSearch(city, stNode, city, enNode);
		}

		mPoiFlag = 1;

	}

	/**
	 * 
	 * @Method: searchRoude
	 * @Description: 路线搜索的搜索输入窗口
	 * @return void 返回类型
	 * @throws
	 */
	private void searchRoude() {
		// 获取该窗口的view才能进行获取输入框中的文本
		LayoutInflater factory = LayoutInflater.from(this);
		final View dlgView = factory.inflate(R.layout.alert_dialog_text_entry,
				null);

		new AlertDialog.Builder(BaiduMapActivity.this)
				.setIcon(R.drawable.cute_48)
				.setTitle(R.string.alert_dialog_text_entry)
				.setView(dlgView)

				.setPositiveButton(R.string.alert_dialog_ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {

								EditText fromEdit = (EditText) dlgView
										.findViewById(R.id.from_edit);
								EditText toEdit = (EditText) dlgView
										.findViewById(R.id.to_edit);
								EditText cityEdit = (EditText) dlgView
										.findViewById(R.id.city_edit);

								String fromAddr = fromEdit.getText().toString();
								String toAddr = toEdit.getText().toString();
								String city = cityEdit.getText().toString();

								getRoute(fromAddr, toAddr, city); // 获取两地的路线

							}
						})
				.setNegativeButton(R.string.alert_dialog_cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {

							}
						}).show();

		// 单选按钮,设置路线搜索的方式,驾车、公交、步行等
		RadioGroup radioGroup = (RadioGroup) dlgView
				.findViewById(R.id.radioGroup);
		radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {

				if (checkedId == R.id.carRadioBtn) {
					mPoiFlag = 1;
				}
				if (checkedId == R.id.busRadioBtn) {
					mPoiFlag = 2;
				}
				if (checkedId == R.id.walkRadioBtn) {
					mPoiFlag = 3;
				}
			}
		});

	}

	/****
	 * 功能 ： 向服务器发送我的坐标位置,分享给其他好友
	 * 
	 ****/
	private void shareMyLocation() {
		// WIFI模式下无法获取坐标
		if (mMyGeoPoint == null) {
			// 添加定位图层
			MyLocationOverlay myLocation = new MyLocationOverlay(this, mMapView);
			myLocation.enableMyLocation(); // 启用定位
			// 获取我的坐标
			mMyGeoPoint = myLocation.getMyLocation();
		}

		new Thread(new Runnable() {

			@Override
			public void run() {

				String mapX = String.valueOf(mMyGeoPoint.getLatitudeE6()); // 纬度
				String mapY = String.valueOf(mMyGeoPoint.getLongitudeE6()); // 精度

				ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				nameValuePairs.add(new BasicNameValuePair("protocol",
						"sharePoint"));// 封装键值对
				nameValuePairs.add(new BasicNameValuePair("id",
						LoginActivity.mineID));
				nameValuePairs.add(new BasicNameValuePair("xpoint", mapX));
				nameValuePairs.add(new BasicNameValuePair("ypoint", mapY));

				Log.d("分享我的位置", mapX + "," + mapY);

				HttpThread h = new HttpThread(nameValuePairs, 9);
				String result = (String) h.sendInfo();
				Log.d("分享结果", result);
			}
		}).start();

		displayToast("分享成功");

	}
	 	
	/**
	 * 
	 * @Method: addFriendToMap
	 * @Description: 将好友图标贴到地图上
	 * @param geoPoint
	 * @param flag
	 * @return void 返回类型
	 * @throws
	 */
	private void addFriendToMap(GeoPoint geoPoint, int flag,String id,String name) {
		mAddrFlag = flag;
		if (geoPoint != mMyGeoPoint) {
			geoPointTo = geoPoint;
		}
		
		geoPointTo = geoPoint;
		mMKSearch.reverseGeocode(geoPointTo);
		// 将好友添加到地图上
		mOverLays.addFriendOverLayItem(geoPoint, name, id) ;
		mMapView.getOverlays().add(mOverLays) ;
		mMapView.invalidate() ;

	}

	/**
	 * 
	 * @Method: getFriendsLocation
	 * @Description: 向服务器请求所有好友的坐标信息 ,并且添加到地图上
	 * @return void 返回类型
	 * @throws
	 */
	private void getFriendsLocation() {
		// 模拟几个好友的位置
		if (mOverLays != null) {
			mMapView.getOverlays().add(mOverLays); // 添加ItemizedOverlay实例到mMapView
		}
		mMapView.invalidate();

		// 获取在线的同城好友,测试的时候不能用,局域网问题
		Thread friendsPointThread = new Thread( runnable1 );
		friendsPointThread.start();
	}
	 	
	 /**
	  *  向服务器发出请求的线程,并且向UI线程投递消息
	  */
	private Handler mHandler1 = new Handler();
	Runnable runnable1 = new Runnable() {
		String msg = null; // 要发送给主线程的String

		@Override
		public void run() {
			ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs
					.add(new BasicNameValuePair("protocol", "getAddress"));// 封装键值对
			HttpThread h = new HttpThread(nameValuePairs, 8); // 8--获取好友坐标
			msg = h.sendInfo().toString(); // 接收服务器的返回值
			sendMessage(); // 发送消息给主线程
		}

		public void sendMessage() { // 线程间数据传输

			Looper mainLooper = Looper.getMainLooper(); // 得到主线程loop
			mHandler1 = new MyHandler(mainLooper); // 创建主线程的handler
			mHandler1.removeMessages(0); // 移除所有队列中的消息
			Message m = mHandler1.obtainMessage(1, 1, 1, msg);// 把消息放入message
			mHandler1.sendMessage(m); // 发送message
		}
	};

	
	/**
	 * 
	 * @ClassName: MyHandler
	 * @Description: 内部类,接收服务器返回来的在线用户消息,更新UI线程
	 * @Author: Mr.Simple
	 * @E-mail: bboyfeiyu@gmail.com
	 * @Date 2012-11-9 下午2:31:32
	 * 
	 */
	private class MyHandler extends Handler {
		public MyHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			
			try{
				String s1 = msg.obj.toString();  // 接收到子线程的字符串
				s1 = s1.trim(); 				 // 去空格
				String array[] = s1.split(";;"); // 拆分字符串

				int len = array.length / 4;
				Log.d(TAG, "接到的长度为 " + len) ;
				for (int i = 0; i < len; i++) {
					// 构造坐标点
					GeoPoint geoPoint = new GeoPoint(
							( Integer.parseInt( array[i + 2]) ),
							(  Integer.parseInt( array[i + 3]) ) );
	
					Log.d(TAG, array[i] +" ,昵称 : " +  array[i+1] + ",好友的经度 : " + array[i + 2]
											+ " 纬度 : " + array[i + 3]);
					// 接收到坐标数据,将坐标点添加到地图上
					addFriendToMap(geoPoint, 2, array[i], array[i+1]); 
				}
			}catch (Exception e) {
				e.printStackTrace() ;
			}
		}
	}

	/**
	 * 
	 * @Method: displayToast
	 * @Description: 显示toast信息
	 * @param msg    要显示的内容
	 * @return void  返回类型
	 * @throws
	 */
	private void displayToast(String msg ) {
		
		Toast.makeText(BaiduMapActivity.this, msg, 0).show() ;
	}
	
	/**
	 * 
	 * @Method: texiSelectMenu 
	 * @Description: 出租车操作的选择项窗口  
	 * @return void  返回类型 
	 * @throws
	 */
	private void texiSelectMenu() {
		String[] menuItems = { "发布空车信息", "发布乘车信息", "周边空车","周边乘客", "删除状态"}; // 弹出的菜单

		new AlertDialog.Builder(this)
				.setTitle("请选择...")
				.setSingleChoiceItems(menuItems, -1,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// 获取选项
								mIndex = which;
							}
						})
				.setPositiveButton(R.string.alert_dialog_ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// 出租车选项选择以后
								texiSelected( mIndex );
							}
						})
				.setNegativeButton(R.string.alert_dialog_cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {

							}
						}).show();
	}

	
	/**
	 * 
	 * @Method: texiSelected
	 * @Description: 出租车选择某项的操作
	 * @param index
	 *            选择的项索引
	 * @return void 返回类型
	 * @throws
	 */
	private void texiSelected(int index) {
		// 获取电话管理服务
		TelephonyManager tManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		// 获取电话号码
		String number = tManager.getLine1Number();
		Log.d("电话", number);
	
		// 如果获取失败则手动输入电话号码
		if (  mIndex != 2 && mIndex != 3 ) {
			if ( number.equals("") ) {
			// 跳出输入框输入手机号码
			LayoutInflater factory = LayoutInflater.from(this);
			final View dlgView = factory.inflate(R.layout.inputtel, null);

			EditText telEdit = (EditText) dlgView.findViewById(R.id.telEdit);
			mTelephone = readInfoFromLocal() ;
			telEdit.setText( mTelephone ) ;
			
			new AlertDialog.Builder(BaiduMapActivity.this)
					.setIcon(R.drawable.cute_48)
					.setTitle("请输入您的电话号码...")
					.setView(dlgView)
					.setPositiveButton(R.string.alert_dialog_ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int whichButton) {

									EditText telEdit = (EditText) dlgView
											.findViewById(R.id.telEdit);
									mTelephone = telEdit.getText().toString();
									if (!isPhoneNumberValid(mTelephone)) {
										displayToast("输入的号码无效, 请重新输入");
									} else {
										saveInfoToLocal( mTelephone ) ;
										// 提交请求
										new TexiAsyncTask(mIndex, 0).execute();
									}

								}
							})
					.setNegativeButton(R.string.alert_dialog_cancel,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int whichButton) {

								}
							}).show();
			} // 判断电话号码的if 
		}else{
			// 提交请求,即查看状态的请求
			new TexiAsyncTask(mIndex, 0).execute();
		}


	}

	
	/**
	 * 
	 * @Method: saveInfoToLocal 
	 * @Description: 保存用户登录信息到本地 （包括记住密码、自动登录等）
	 * @param num   
	 * @throws
	 */
	private void saveInfoToLocal(String num)
	{
		// 保存用户电话号码
	   	SharedPreferences ref = getSharedPreferences("phone", 0);
	    ref.edit().putString("phoneNum", num).commit();
	    Log.d(TAG, "将电话号码写入到xml") ;
	}
	
	
	/**
	 * 
	 * @Method: readInfoFromLocal 
	 * @Description: 从本地文件中读取用户数据等
	 * @return   
	 * @throws
	 */
	private String readInfoFromLocal()
	{
		 SharedPreferences settings = getSharedPreferences("phone", 0);  // 获取一个对象
	        
		 String phoneNum = settings.getString("phoneNum", "");
		 return phoneNum ;
	}
	
	/**
	 * 
	 * @Method: addTexiToMap 
	 * @Description: 将从服务器得到的出租车信息贴到地图上,包括乘客和司机  
	 * @return void  返回类型 
	 * @throws
	 */
	private void addTexiToMap(GeoPoint point, String telephone, String type) {

		Drawable marker = null;
		// 实例化出租车覆盖物
		TexiOverItem textOverLays = null;
		int titleIndex = 0;
		if (type.contains("Texi") || type.contains("Driver")) {
			// 覆盖物不图标
			marker = getResources().getDrawable(R.drawable.location_red);
			titleIndex = 1;
		} else {
			marker = getResources().getDrawable(R.drawable.location_green);
			titleIndex = 2;
		}
		// 实例化出租车覆盖物
		textOverLays = new TexiOverItem(marker, BaiduMapActivity.this, titleIndex);
		// 发布空车信息,坐标、电话传给服务器
		textOverLays.addTexiOverLayItem(point, telephone);
		// 添加到视图中
		mMapView.getOverlays().add( textOverLays ); // 添加ItemizedOverlay实例到mMapView
		mMapView.invalidate();
		Log.d(TAG, "将出租车信息贴到地图上" + point.toString());

	}
	

	/**
	 * @Method: isPhoneNumberValid
	 * @Description: 检查字符串是否为电话号码的方法,并返回true or false的判断值
	 * @param phoneNumber
	 * @return
	 */
	private boolean isPhoneNumberValid(String phoneNumber) {
		boolean isValid = false;
		/*
		 * 可接受的电话格式有: ^\\(? : 可以使用 "(" 作为开头 (\\d{3}): 紧接着三个数字 \\)? : 可以使用")"继续
		 * [- ]? : 在上述格式后可以使用具选择性的 "-". (\\d{4}) : 再紧接着三个数字 [- ]? : 可以使用具选择性的
		 * "-" 继续. (\\d{4})$: 以四个数字结束. 可以比较下列数字格式: (123)456-78900,
		 * 123-4560-7890, 12345678900, (123)-4560-7890
		 */
		String expression = "^\\(?(\\d{3})\\)?[- ]?(\\d{3})[- ]?(\\d{5})$";
		String expression2 = "^\\(?(\\d{3})\\)?[- ]?(\\d{4})[- ]?(\\d{4})$";
		CharSequence inputStr = phoneNumber;
		/* 创建Pattern */
		Pattern pattern = Pattern.compile(expression);
		/* 将Pattern 以参数传入Matcher作Regular expression */
		Matcher matcher = pattern.matcher(inputStr);
		/* 创建Pattern2 */
		Pattern pattern2 = Pattern.compile(expression2);
		/* 将Pattern2 以参数传入Matcher2作Regular expression */
		Matcher matcher2 = pattern2.matcher(inputStr);

		if (matcher.matches() || matcher2.matches()) {
			isValid = true;
		}
		return isValid;
	}
		 	

	/**
	 * 
	 * @Method: searchMenu 
	 * @Description: 搜索菜单的选择操作
	 * @return void  返回类型 
	 * @throws
	 */
	private void searchMenu() {
		String[] menuItems = { "路线搜索", "周边搜索", "同城好友" }; // 弹出的菜单

		new AlertDialog.Builder(this)
				.setTitle("请选择...")
				.setSingleChoiceItems(menuItems, -1,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								mIndex = which;
							}
						})
				.setPositiveButton(R.string.alert_dialog_ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// 搜索菜单选择处理
								searchMenuSelected(mIndex);
							}
						})
				.setNegativeButton(R.string.alert_dialog_cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {

							}
						}).show();

	}
	 	 
	 	 

	/**
	 * @Method: showPOIMenu
	 * @Description: 弹出POI搜索选择菜单
	 */
	private void showPOIMenu() {
		String[] menuitems = getResources().getStringArray(R.array.poiMenu); // 弹出的菜单

		new AlertDialog.Builder(this)
				.setTitle("请选择...")
				.setSingleChoiceItems(menuitems, -1,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {

								POISearchIndex(which); // POI选择的搜索项目

							}
						})
				.setPositiveButton(R.string.alert_dialog_ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								displayToast("搜索中,请稍后...");

							}
						})
				.setNegativeButton(R.string.alert_dialog_cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {

								/* User clicked No so do some stuff */
							}
						}).show();
	}


	private int mViewIndex = -1;
	/**
	 * 
	 * @Method: changeMapViewMenu
	 * @Description: 地图视图修改
	 * @throws
	 */
	private void changeMapViewMenu() {
		String[] menuitems = { "基本视图", "卫星视图" }; // 弹出的菜单
		new AlertDialog.Builder(this)
				.setTitle("请选择...")
				.setSingleChoiceItems(menuitems, 0,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								mViewIndex = which;

							}
						})
				.setPositiveButton(R.string.alert_dialog_ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								
								if (mViewIndex == 0) {
									mMapView.setSatellite(false);
								} else if (mViewIndex == 1) // POI选择的搜索项目
								{
									mMapView.setSatellite(true);
								}

							}
						})
				.setNegativeButton(R.string.alert_dialog_cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {

								/* User clicked No so do some stuff */
							}
						}).show();
	}


	/**
	 * 
	 * @Method: searchMenuSelected 
	 * @Description: 搜索菜单的选择,有三个选项:路线、周边、同城好友
	 * @param index   
	 * @throws
	 */
	private void searchMenuSelected(int index) {
		switch (index) {
		case 0:
			// 路线搜索
			searchRoude();
			break;
		case 1:
			// 周边搜索
			showPOIMenu();
			break;
		case 2:
			// 同城好友
			getFriendsLocation();
			break;
		default:
			break;
		}

	}
	 	 

	/**
	 * 
	 * @Method: POISearchIndex 
	 * @Description:  POI选择响应函数,获取被选择的POI类型
	 * @param i       代表选择的POI类型,比如ATM、邮局、公园等
	 * @throws
	 */
	private void POISearchIndex(int i) {
		String[] menu = getResources().getStringArray(R.array.poiMenu);
		String poiType = menu[i];

		if (mMKSearch == null) {
			mMKSearch = new MKSearch();
			mMKSearch.init(mBMapManager, new MySearchListener());
		}

		mMKSearch.poiSearchNearBy(poiType, mMyGeoPoint, 5000); // 搜索周边5KM的
		Log.d(TAG, "Poi 搜索 ： " + poiType);

	}

	/*
	 * (非 Javadoc,覆写的方法) 按键按下的事件 <p>Title: onKeyDown</p> <p>Description: </p>
	 * 
	 * @param keyCode
	 * 
	 * @param event
	 * 
	 * @return
	 * 
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && mPopView.isSelected()) {
			mPopView.setVisibility(View.GONE);
			return true;
		}
		return super.onKeyDown(keyCode, event);

	}

	@Override
	protected void onStop() {

		// 添加定位图层
		MyLocationOverlay myLocation = new MyLocationOverlay(this, mMapView);
		if (myLocation.isMyLocationEnabled()) {
			myLocation.disableMyLocation();
		}
		// 关闭GPS
		turnGPSOff();

		super.onStop();

	}

	/**
	 * 
	 * @ClassName: MyOverItem
	 * @Description: 地图覆盖物内部类,好友的覆盖物内部类
	 * @Author: Mr.Simple
	 * @E-mail: bboyfeiyu@gmail.com
	 * @Date 2012-11-16 下午6:03:22
	 * 
	 */

	class MyOverItem extends ItemizedOverlay<OverlayItem> {

		private List<OverlayItem> mGeoList = new ArrayList<OverlayItem>();
		// 保存已经存在的用户的坐标位置
		private List<String> mNameList = new ArrayList<String>();
		// 画笔对象，绘制地图覆盖物
		Paint paint = new Paint();

		/**
		 * 
		 * @Constructor: 
		 * @@param marker
		 * @@param context
		 * @Description: 构造函数
		 * @param marker
		 * @param context
		 */
		public MyOverItem(Drawable marker, Context context) {
			super(boundCenterBottom(marker));

			if (mMyGeoPoint != null) {
				mGeoList.add(new OverlayItem(mMyGeoPoint, "MR.SIMPLE", "我在大连."));
			}

			populate(); // createItem(int)方法构造item。一旦有了数据，在调用其它方法前，首先调用这个方法
		}

		/**
		 * (非 Javadoc,覆写的方法) 
		 * @Title: createItem
		 * @Description: 
		 * @param i
		 * @return 
		 * @see com.baidu.mapapi.ItemizedOverlay#createItem(int)
		 */
		@Override
		protected OverlayItem createItem(int i) {
			return mGeoList.get(i);
		}

		/**
		 * (非 Javadoc,覆写的方法) 
		 * @Title: size
		 * @Description: 
		 * @return 
		 * @see com.baidu.mapapi.ItemizedOverlay#size()
		 */
		@Override
		public int size() {
			return mGeoList.size();
		}

		/**
		 * 功能 ： 处理当点击事件 (non-Javadoc)
		 * @see com.baidu.mapapi.ItemizedOverlay#onTap(int)
		 */
		@Override
		protected boolean onTap(int i) {

			mFindBtnFlag = 1; // 设置为路线搜索模式
			geoPointTo = mGeoList.get(i).getPoint(); // 获取好友的位置,也是气泡显示的坐标位置
			setFocus(mGeoList.get(i)); // 获取焦点

			// 搜索到好友位置的路线
			Button dailBtn = (Button) mPopView.findViewById(R.id.findBtn);
			dailBtn.setText("搜索路线");

			// 设置气泡中的内容
			TextView titleView = (TextView) mPopView
					.findViewById(R.id.pop_title);
			if (LoginActivity.mineName.equals(mGeoList.get(i).getTitle())) {
				titleView.setText(mGeoList.get(i).getTitle());
				// 隐藏获取路线的按钮
				dailBtn.setVisibility(View.GONE);
				dailBtn.setEnabled(false);
				displayToast("hey,这是本机标识...");
			} else {
				titleView.setText("好友 : " + mGeoList.get(i).getTitle());
				// 隐藏获取路线的按钮
				dailBtn.setVisibility(View.VISIBLE);
				dailBtn.setEnabled(true);
			}

			BaiduMapActivity.this.mMapView.updateViewLayout(
					BaiduMapActivity.mPopView, new MapView.LayoutParams(
							LayoutParams.WRAP_CONTENT,
							LayoutParams.WRAP_CONTENT, geoPointTo,
							MapView.LayoutParams.BOTTOM_CENTER));

			BaiduMapActivity.mPopView.setVisibility(View.VISIBLE); // 显示气泡

			return true;

		}

		/**
		 * 功能 ： 失去焦点,消去弹出的气泡 (non-Javadoc)
		 * 
		 * @see com.baidu.mapapi.ItemizedOverlay#onTap(com.baidu.mapapi.GeoPoint,
		 *      com.baidu.mapapi.MapView)
		 */
		@Override
		public boolean onTap(GeoPoint geoPoint, MapView mapView) {

			BaiduMapActivity.mPopView.setVisibility(View.GONE);
			return super.onTap(geoPoint, mapView);
		}

		/**
		 * 功能 ： 重载draw函数,实现自定义绘制功能 (non-Javadoc)
		 * 
		 * @see com.baidu.mapapi.ItemizedOverlay#draw(android.graphics.Canvas,
		 *      com.baidu.mapapi.MapView, boolean)
		 */
		@Override
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {

			super.draw(canvas, mMapView, shadow);

		}

		/**
		 * 
		 * @Method: setMarker
		 * @Description: 设置新的覆盖物图标
		 * @param newMarker
		 * @throws
		 */
		public void setMarker(Drawable newMarker) {
			super.boundCenterBottom(newMarker); // 设置新的覆盖物图标
		}

		
		/**
		 * 
		 * @Method: addFriendOverLayItem
		 * @Description: 添加好友标识覆盖物到某坐标
		 * @param gPoint  好友的坐标
		 * @param name    好友昵称
		 * @param content
		 * @throws
		 */
		public void addFriendOverLayItem(GeoPoint gPoint, String name,
				String content) {
			Log.d(TAG, "添加好友" + name + "到地图 " + gPoint.toString());
			if (!mNameList.contains(name)) {
				mGeoList.add(new OverlayItem(gPoint, name, content));
				mNameList.add(name);
			}
			populate();
		}

		/**
		 * 
		 * @Method: clearOverLayItems
		 * @Description: 将数据清除
		 * @throws
		 */
		public void clearOverLayItems() {
			mGeoList.clear();
			mNameList.clear();
		}

	} // end of class.
	 	
	 	 
	 	/**
	 	 * 
	 	 * @ClassName: TexiOverItem 
	 	 * @Description: 出租车相关的地图覆盖物内部类
	 	 * @Author: Mr.Simple 
	 	 * @E-mail: bboyfeiyu@gmail.com 
	 	 * @Date 2012-11-16 下午6:04:00 
	 	 *
	 	 */
	 	class TexiOverItem extends ItemizedOverlay<OverlayItem> {
	 		
	 		// 用链表存储
	 	    private List<OverlayItem> mItemsList = new ArrayList<OverlayItem>();	
	 	    private String mTitle = null;				// 覆盖物被点击时的标题
	 	    private List<String> mPhoneList = new ArrayList<String>() ;
	 	    
	 	    Paint paint = new Paint();					// 画笔对象，绘制地图覆盖物
	 	   
	 	    // 构造函数,增加一个参数,为类型,1为出租车,2为乘客,在构造中设置不同的标识符
	 	    public TexiOverItem(Drawable marker, Context context) {
	 	
	 	        super(boundCenterBottom(marker));
	 	    	initOverItem( 1 ) ;
	 
	 	    }
	 	    // 构造函数,增加一个参数,为类型,1为出租车,2为乘客,在构造中设置不同的标识符
	 	    public TexiOverItem(Drawable marker, Context context, int type) {
	 	       super(boundCenterBottom(marker));
	 	       initOverItem( type ) ;
	 	       Log.d(TAG, "出租车标志类构造  " + mTitle) ;
	 	    }
	 	 
	 	    /**
	 	     * 
	 	     * @Method: initOverItem 
	 	     * @Description: 初始化覆盖物,设置不同的图标以及标题
	 	     * @param type   
	 	     * @return void  返回类型 
	 	     * @throws
	 	     */
	 	    private void initOverItem(int type){
	 	    	if ( 1 == type ){
	 	    		mTitle = "出租车司机";
	 	    		// 在此也设置marker
	 	    	}else{
	 	    		mTitle = "出租车乘客";
	 	    	}
	 	    }
	 	    
	 	    /**
	 	     * (非 Javadoc,覆写的方法) 
	 	     * @Title: createItem
	 	     * @Description: 
	 	     * @param i
	 	     * @return 
	 	     * @see com.baidu.mapapi.ItemizedOverlay#createItem(int)
	 	     */
	 	    @Override
	 	    protected OverlayItem createItem(int i) {
	 	        return mItemsList.get(i);
	 	    }
	 	 
	 	    /**
	 	     * (非 Javadoc,覆写的方法) 
	 	     * @Title: size
	 	     * @Description: 
	 	     * @return 
	 	     * @see com.baidu.mapapi.ItemizedOverlay#size()
	 	     */
	 	    @Override
	 	    public int size() {
	 	        return mItemsList.size();
	 	    }
	 	 
	 	    
	 	    /**
	 	     *  功能 ： 处理点击覆盖物图标的动作
	 	     *  (non-Javadoc)
	 	     * @see com.baidu.mapapi.ItemizedOverlay#onTap(int)
	 	     */
	 	    @Override
	 	    protected boolean onTap(int i) {
	 	    	
	 	    	mFindBtnFlag = 2;										// 设置为路线搜索模式
	 	    	geoPointTo = mItemsList.get(i).getPoint();				// 获取好友的位置,也是气泡显示的坐标位置

	 	    	setFocus(mItemsList.get(i));							// 获取焦点
	 	    	// 获取该用户的电话
	 	    	mTelephone = mItemsList.get(i).getSnippet();
	 	    	displayToast( "该用户电话为 : " + mTelephone ) ;
	 	    	
	 			// 设置气泡中的内容
	 			TextView titleView = (TextView)mPopView.findViewById(R.id.pop_title);
	 			titleView.setText( mTitle );
	 			// 打电话的按钮
	 			Button dailBtn = (Button)mPopView.findViewById(R.id.findBtn);
	 			dailBtn.setVisibility( View.VISIBLE ) ;
	 			dailBtn.setEnabled( true ) ;
	 			dailBtn.setText("呼叫");
	 			
	 			BaiduMapActivity.this.mMapView.updateViewLayout( BaiduMapActivity.mPopView,
	 	                new MapView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
	 	                		geoPointTo, MapView.LayoutParams.BOTTOM_CENTER));
	 			
	 			BaiduMapActivity.mPopView.setVisibility(View.VISIBLE);	// 显示气泡
	
	 			return true;
	 			
	 	    }
	 	    
	 	    
			/**
			 *  功能 ： 失去焦点,消去弹出的气泡
			 *  (non-Javadoc)
			 * @see com.baidu.mapapi.ItemizedOverlay#onTap(com.baidu.mapapi.GeoPoint, com.baidu.mapapi.MapView)
			 */
	 	   @Override
	 		public boolean onTap(GeoPoint geoPoint, MapView mapView) {

	 			BaiduMapActivity.mPopView.setVisibility(View.GONE);
	 			return super.onTap(geoPoint, mapView);
	 		}
	 	    
	 	   
	 	    /**
	 	     * 功能 ： 重载draw函数,实现在地图上的自定义绘制覆盖物功能
	 	     * (non-Javadoc)
	 	     * @see com.baidu.mapapi.ItemizedOverlay#draw(android.graphics.Canvas, com.baidu.mapapi.MapView, boolean)
	 	     */
	 	    @Override
			public void draw(Canvas canvas, MapView mapView, boolean shadow) {
	 	    	
				super.draw(canvas, mMapView, shadow);
	
			}

	 	  
	 	    /**
	 	     * 
	 	     * @Method: addFriendOverLayItem 
	 	     * @Description: 添加覆盖物到某坐标
	 	     * @param gPoint 坐标点
	 	     * @param title  点击覆盖物时的标题
	 	     * @param telephone  电话
	 	     * @return void  返回类型 
	 	     * @throws
	 	     */
	 	    public void addTexiOverLayItem(GeoPoint gPoint, String telephone)
	 	    {
	 	    	if ( !mPhoneList.contains( telephone ) ){
		 	    	Log.d(TAG, "添加" + mTitle  + "到地图 " + gPoint.toString());
		 	    	mItemsList.add(new OverlayItem(gPoint, mTitle, telephone));
		 	    	mPhoneList.add( telephone ) ;
		 	    	populate();
	 	    	}
	 	    }
	 	    
	 	    /**
	 	     * 
	 	     * @Method: clearOverLayItem 
	 	     * @Description: 清空数据 
	 	     * @throws
	 	     */
	 	    public void clearOverLayItem(){
	 	    	mItemsList.clear() ;
	 	    	mPhoneList.clear() ;
	 	    	mTitle = "NULL";
	 	    }
	 	    
	 	}// end of class.

	 	
	/**
	 * 
	 * @ClassName: MySearchListener
	 * @Description: 百度地图移动版API集成搜索服务包括：
	 *               位置检索、周边检索、范围检索、公交检索、驾乘检索、步行检索，通过初始化MKSearch类，
	 *               注册搜索结果的监听对象MKSearchListener，实现异步搜索服务。
	 *               首先自定义MySearchListener实现MKSearchListener接口，通过不同的回调方法，获得搜索结果
	 * @Author: Mr.Simple
	 * @E-mail: bboyfeiyu@gmail.com
	 * @Date 2012-11-16 下午6:04:37
	 * 
	 */
	public class MySearchListener implements MKSearchListener {

		/****
		 * 功能 ： 返回驾乘路线搜索结果。 参数1： 搜索结果 参数2 ： 错误号，0表示正确返回
		 ****/
		@Override
		public void onGetAddrResult(MKAddrInfo result, int iError) {

			String Location = null;
			if (result == null) {
				Location = "没有搜索到该地址";
				return;
			} else {
				// 获得搜索地址的经纬度
				Location = "纬度：" + result.geoPt.getLatitudeE6() / 1E6 + "\n"
						+ "经度：" + result.geoPt.getLongitudeE6() / 1E6 + "\n";
				geoPointTo = result.geoPt; // 测试使用
			}

			// 获取城市名,reverseGeocode的回调函数,包含了用户的位置信息等
			MKGeocoderAddressComponent addrInfo = result.addressComponents;
			if (mAddrFlag == 1) {
				mFCity = addrInfo.city;
				displayToast("我在 ： " + mFCity + " 坐标： " + Location);
				return;
			}

			if (addrInfo.city.contains(mFCity)) // 同城则添加到地图上
			{
				displayToast("好友" + "在 ： " + addrInfo.city + " 坐标： " + Location);
				mOverLays.addFriendOverLayItem(geoPointTo, "Name", "title");
			}

			if (geoPointTo != null) {
				mOverLays.addFriendOverLayItem(geoPointTo, "Name", "title");
			}

		}
	 	   
	 	    /**
             * 功能 ： 返回驾乘路线搜索结果
             *  参数1： 搜索结果 
             *  参数2： 错误号，0表示正确返回 
             */ 
	 	    @Override
	 	    public void onGetDrivingRouteResult(MKDrivingRouteResult result, int iError) {
	 	    	
	 	    	 if (result == null || iError != 0) {
	 	    		 displayToast("路线搜索失败...");
	 	            return;
	 	        }
	 	    	 
	 	    	 // 路线覆盖物
	 	        RouteOverlay routeOverlay = new RouteOverlay(BaiduMapActivity.this, mMapView);
	 	        routeOverlay.setData(result.getPlan(0).getRoute(0));
	 	       // mMapView.getOverlays().clear();
	 	        mMapView.getOverlays().add( routeOverlay );
	 	        mMapView.invalidate();
	 	        mMapContoller.setCenter(result.getStart().pt);
	 	        
	 	    }
	 	 
	 	    
	 	    /**
             * 返回poi搜索结果。
             * 参数1 ：搜索结果 ,
             * 参数2 ：返回结果类型: MKSearch.TYPE_POI_LIST 、MKSearch.TYPE_AREA_POI_LIST、
             * 					 MKSearch.TYPE_CITY_LIST 
             * 参数3 ：  - 错误号，0表示正确返回 
             */ 
	 	    @Override
	 	    public void onGetPoiResult(MKPoiResult result, int type, int iError) {
	 	    	
	 	       if (result == null || iError != 0) {
	 	    	   displayToast("抱歉,未找到结果.");
	 	          return;
	 	      }
	 	       
	 	      PoiOverlay poioverlay = new PoiOverlay(BaiduMapActivity.this, mMapView);
	 	      poioverlay.setData( result.getAllPoi() );
	 	      mMapView.getOverlays().add( poioverlay );
	 	      mMapView.invalidate();
	 	    }
	 
	 	   
	 	   /**
             * 返回公交搜索结果
             * 参数1：  搜索结果 
             * 参数2： - 错误号，0表示正确返回， 当返回MKEvent.ERROR_ROUTE_ADDR时，
             * 表示起点或终点有歧义， 调用MKTransitRouteResult的getAddrResult方法获取推荐的起点或终点信息 
             */ 
	 	    @Override
	 	    public void onGetTransitRouteResult(MKTransitRouteResult result, int iError) {
	 	    	
	 	    	Log.d("RoutePlan", "the res is " + result + "__" + iError);
	 	      
				if (iError != 0 || result == null) {
					displayToast("抱歉,未找到结果");
					return;
				}
				
				TransitOverlay  routeOverlay = new TransitOverlay (BaiduMapActivity.this, mMapView);
			    // 此处仅展示一个方案作为示例
			    routeOverlay.setData(result.getPlan(0));
			    //mMapView.getOverlays().clear();
			    mMapView.getOverlays().add(routeOverlay);
			    mMapView.invalidate();
			    
			    mMapView.getController().animateTo(result.getStart().pt);
	 	       
	 	    }
	 	 
	 	    /** 
             * 返回步行路线搜索结果。
             * 参数1： 搜索结果 
 			 * 参数2：- 错误号，0表示正确返回 
             */ 
	 	    @Override
	 	    public void onGetWalkingRouteResult(MKWalkingRouteResult result, int iError) {
	 	    
		 	   	if (iError != 0 || result == null) {
		 	   		displayToast("抱歉,未找到结果");
					return;
				}
				RouteOverlay routeOverlay = new RouteOverlay(BaiduMapActivity.this, mMapView);
			    // 此处仅展示一个方案作为示例
			    routeOverlay.setData(result.getPlan(0).getRoute(0));
			    mMapView.getOverlays().clear();
			    mMapView.getOverlays().add(routeOverlay);
			    mMapView.invalidate();
			    
			    mMapView.getController().animateTo(result.getStart().pt);
	 	    } 	    
	 	}
	 	
	 	
	 /**
	  * 
	  * @ClassName: TexiAsyncTask 
	  * @Description: 
	  * @Author: Mr.Simple 
	  * @E-mail: bboyfeiyu@gmail.com 
	  * @Date 2012-11-12 下午3:45:33 
	  *
	  */
	public class TexiAsyncTask extends AsyncTask<Integer, Void, String> {

		private int mType = -1 ;
		private final String EMPTY_TEXI = "emptyTexi" ;
		private final String TAKE_TEXI = "takeTexi" ;
		private final String CHECK = "check" ;
		private final String CANCEL = "cancel" ;
		
		/**
		 * 
		 * @Constructor: 
		 * @@param type   请求类型,0为提交空车状态,1为提交乘车状态,2为查看,司机与乘客的返回数据是不一样的
		 * @@param role   当type为2时才有用,该参数为"查看"选项的参数,即查看看地图上的空车,或者乘客
		 * @Description:  出租车的异步请求
		 * @param type
		 * @param role
		 */
		public TexiAsyncTask(int type, int role){
			mType = type ;
		}
		
		
		/**
		 * (非 Javadoc,覆写的方法) 
		 * @Title: doInBackground
		 * @Description: 
		 * @param params  是查看状态时的角色,1代表司机或者2代表乘客
		 * @return 
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected String doInBackground(Integer... params) {

			// HTTP请求的键值对
			ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			String url = "http://199.36.75.40/Android/receiveMessage.php";
			// 结果
			String result = "NULL";
			try {
				// 请求的类型
				String msgType = "";
				switch ( mType ) {
					case 0:
						// 司机发布空车状态
						msgType = EMPTY_TEXI;
						break;
					case 1:
						// 乘客发布乘车状态
						msgType = TAKE_TEXI;
						break;
					case 2:
						// 设置请求该服务的角色,司机与乘客请求的返回数据不一样
						msgType = CHECK + "TexiDriver" ; 
						break;
					case 3:
						// 设置请求该服务的角色,司机与乘客请求的返回数据不一样
						msgType = CHECK + "Passenger" ; 
						break;
					case 4:
						msgType = CANCEL ;
						// 通过电话来删除状态
						nameValuePairs.add(new BasicNameValuePair("phone", mTelephone));
						Log.d(TAG,  "取消状态,msgType = "+ msgType + " telephone = " + mTelephone) ;
						break;
						
					default:
						break;
				}
				
				Log.d(TAG,  "msgType = "+ msgType) ;
				// 添加协议类型
				nameValuePairs.add(new BasicNameValuePair("protocol", msgType));
				
				// 1和2的类型都是提交给服务器数据的
				if ( mType == 0 || mType == 1 && mMyGeoPoint != null){
					// 获取本机位置的经纬度
					String mapX = String.valueOf( mMyGeoPoint.getLatitudeE6() ); 
					String mapY = String.valueOf( mMyGeoPoint.getLongitudeE6() ); 
					
					nameValuePairs.add(new BasicNameValuePair("phone", mTelephone));
					nameValuePairs.add(new BasicNameValuePair("xpoint", mapX));
					nameValuePairs.add(new BasicNameValuePair("ypoint", mapY));

					Log.d("分享空车状态", "我的地理位置为: " + mapX + "," + mapY);
				}

				// 创建httpPost对象
				HttpPost post = new HttpPost(url);

				post.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
				// 发送http请求
				HttpResponse response = new DefaultHttpClient().execute(post);
				// 请求成功
				if (200 == response.getStatusLine().getStatusCode()) {
					// 获取返回的结果
					result = EntityUtils.toString(response.getEntity());
					Log.d("出租车服务器返回", result);
				} else {
					Log.d("出租车服务器", "请求失败");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return result;
		}

		
		private String resType = "";
		@Override
		protected void onPostExecute(String result) {
			
			if ( !result.equals("NULL")){
				try {
					// 返回的结果头是success还是失败fail
					String flag = result.substring(0, result.indexOf("_")) ;
					if ( flag.equals("success") ){
						// 这个是提交的请求的类型,比如emptyTexi
						resType = result.substring(result.indexOf("_") + 1 ,
														 result.lastIndexOf("_") ) ;
						if ( resType.equals(EMPTY_TEXI) || resType.equals(TAKE_TEXI) 
								|| resType.equals( CANCEL ) ){
							// 获取有用的数据
							result = result.substring(result.lastIndexOf("_") + 1, result.length()) ;
							Log.d(TAG, "有效数据为 : " + result ) ;
						}
						// 根据返回的不同数据类型进行不同的处理
						processResult(resType, result) ;
						displayToast( "请求成功..." ) ;
						
					}else{
						displayToast( "请求失败..." ) ;
					}
				}catch(Exception e){
					displayToast("请求失败...") ;
					e.printStackTrace() ;
				}

			}
		}
		
		/**
		 * 
		 * @Method: processResult 
		 * @Description:
		 * @param type
		 * @param result   
		 * @throws
		 */
		private void processResult(String type ,String result) throws Exception{
			Log.d(TAG, "返回类型为 : " + type + ",结果为: " + result ) ;
			
			if ( EMPTY_TEXI.equals( type ) ){
				Log.d(TAG, "发布空车状态成功") ;
				displayToast( "发布空车状态成功" ) ;
			}else if ( type.contains( CHECK )){
				// 处理查看出租车的返回结果
				parseCheckTexi( result ) ;
				
			}else if ( type.contains( CANCEL )){
				displayToast("出租车状态取消成功") ;
			}
		} // end of processResult
		
		
		/**
		 * @Method: parseCheckTexi
		 * @Description: 处理检查的类型
		 * @param result
		 */
		private void parseCheckTexi(String result){
			try{
				if ( result.contains("::") ){
					// 每个用户的数据是用"::"分割,而每个用户的每个数据使用";;"分割
					String temp[] = result.split("::") ;
					for(int i=0; i<temp.length; i++){
						String res[] = temp[i].split(";;") ;
						String tel = res[0];
						float x = Float.parseFloat( res[1]);
						float y = Float.parseFloat( res[2] ) ;
						GeoPoint point = new GeoPoint((int)(x*1E6), (int)(y*1E6) );
						// 添加到地图上
						addTexiToMap( point , tel , resType) ;
					}
				}
			}catch(Exception e){
				e.printStackTrace() ;
			}
		}

	} // end of TexiAsyncTask
	
}	// end of activity
