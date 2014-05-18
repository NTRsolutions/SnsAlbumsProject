package com.uit.snsalbum.map;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
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

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;
import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.map.ItemizedOverlay;
import com.baidu.mapapi.map.LocationData;
import com.baidu.mapapi.map.MapController;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationOverlay;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayItem;
import com.baidu.mapapi.map.PoiOverlay;
import com.baidu.mapapi.map.RouteOverlay;
import com.baidu.mapapi.map.TransitOverlay;
import com.baidu.mapapi.search.MKAddrInfo;
import com.baidu.mapapi.search.MKBusLineResult;
import com.baidu.mapapi.search.MKDrivingRouteResult;
import com.baidu.mapapi.search.MKGeocoderAddressComponent;
import com.baidu.mapapi.search.MKPlanNode;
import com.baidu.mapapi.search.MKPoiResult;
import com.baidu.mapapi.search.MKSearch;
import com.baidu.mapapi.search.MKSearchListener;
import com.baidu.mapapi.search.MKShareUrlResult;
import com.baidu.mapapi.search.MKSuggestionResult;
import com.baidu.mapapi.search.MKTransitRouteResult;
import com.baidu.mapapi.search.MKWalkingRouteResult;
import com.baidu.platform.comapi.basestruct.GeoPoint;
import com.uit.snsalbum.R;
import com.uit.snsalbum.entry.LoginActivity;
import com.uit.snsalbum.entry.MainViewActivity;
import com.uit.snsalbum.network.HttpThread;

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

public class BaiduMapActivity extends Activity {

	BMapManager mBMapManager = null; // 地图管理对象
	MapView mMapView = null;

	private MapController mMapController = null;
	private GeoPoint mMyGeoPoint;
	private FriendOverlay mOverLays = null; // 地图上的覆盖物

	public LocationClient mLocationClient = null;
	public BDLocationListener myListener = new MyLocationListener();

	private final String TAG = "BAIDU MAP";

	private MKSearch mMKSearch; // 查询服务
	private GeoPoint geoPointTo; // 好友的位置
	private int mPoiFlag = 1; // 路线行驶方式
	private static View mPopView = null; // 点击mark时弹出的气泡View
	private Button findBtn = null; // 找好友路线
	private String mFCity = null; // 好友所在城市
	private int mAddrFlag = 0; // 获取城市的类型
	private int mIndex = 0; // 菜单选择的索引
	private int mFindBtnFlag = 0; // 搜索按钮还是呼叫
	private String mTelephone = null;
	boolean isFirstLocated = true;
	float mDirection;

	MyLocationOverlay myLocationOverlay;

	private List<Overlay> mMapOverlays;

	/**
	 * 功能 ： 页面创建 (non-Javadoc)
	 * 
	 * @see android.app.ActivityGroup#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		mBMapManager = new BMapManager(getApplication());
		mBMapManager.init(null);

		// 设置为无标题模式
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.baidumap);

		// 注意：请在试用setContentView前初始化BMapManager对象，否则会报错
		mMapView = (MapView) findViewById(R.id.bmapsView);
		mMapView.setBuiltInZoomControls(true);
		// 设置启用内置的缩放控件
		mMapController = mMapView.getController();
		// 得到mMapView的控制权,可以用它控制和驱动平移和缩放
		mMyGeoPoint = new GeoPoint((int) (39.915 * 1E6), (int) (116.404 * 1E6));
		// 用给定的经纬度构造一个GeoPoint，单位是微度 (度 * 1E6)
		mMapController.setCenter(mMyGeoPoint);// 设置地图中心点
		mMapController.setZoom(12);// 设置地图zoom级别

		mLocationClient = new LocationClient(getApplicationContext()); // 声明LocationClient类
		mLocationClient.registerLocationListener(myListener); // 注册监听函数
		LocationClientOption option = new LocationClientOption();
		option.setLocationMode(LocationMode.Hight_Accuracy);// 设置定位模式
		option.setCoorType("bd09ll");// 返回的定位结果是百度经纬度，默认值gcj02
		option.setScanSpan(20000);
		option.setIsNeedAddress(true);// 返回的定位结果包含地址信息
		option.setNeedDeviceDirect(true);// 返回的定位结果包含手机机头的方向
		mLocationClient.setLocOption(option);
		mLocationClient.start();
		if (mLocationClient != null && mLocationClient.isStarted()) {
			mLocationClient.requestLocation();
		} else {
			Log.d("LocSDK4", "locClient is null or not started");
		}

		mMapOverlays = mMapView.getOverlays();
		mMapView.setClickable(true);
		mMapView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mPopView != null && mPopView.isShown()) {
					mPopView.setVisibility(View.GONE);
				}
			}
		});

		mMKSearch = new MKSearch(); // 搜索类的对象
		mMKSearch.init(mBMapManager, new MySearchListener()); // 搜索类

		initComponents(); // 初始化控件
		turnGPSOn(); // 开启GPS

		MainViewActivity.addActivityToHashSet(this); // 将页面添加到map中

	}

	/**
	 * 
	 */
	private void mockFriendsInMap() {

		/**
		 * 在想要添加Overlay的地方使用以下代码， 比如Activity的onCreate()中
		 */
		// 准备要添加的Overlay
		double mLat1 = 38.90923;
		double mLon1 = 121.397428;
		double mLat2 = 39.9022;
		double mLon2 = 116.3922;
		double mLat3 = 39.917723;
		double mLon3 = 116.3722;

		// 用给定的经纬度构造GeoPoint，单位是微度 (度 * 1E6)
		GeoPoint p1 = new GeoPoint((int) (mLat1 * 1E6), (int) (mLon1 * 1E6));
		GeoPoint p2 = new GeoPoint((int) (mLat2 * 1E6), (int) (mLon2 * 1E6));
		GeoPoint p3 = new GeoPoint((int) (mLat3 * 1E6), (int) (mLon3 * 1E6));
		// 准备overlay图像数据，根据实情情况修复
		Drawable mark = getResources().getDrawable(R.drawable.location_48);
		// 用OverlayItem准备Overlay数据
		OverlayItem item1 = new OverlayItem(p1, "shawn", "xiongshuangquan");
		// 使用setMarker()方法设置overlay图片,如果不设置则使用构建ItemizedOverlay时的默认设置
		OverlayItem item2 = new OverlayItem(p2, "item2", "item2");
		item2.setMarker(mark);
		OverlayItem item3 = new OverlayItem(p3, "item3", "item3");

		// 创建IteminizedOverlay
		FriendOverlay itemOverlay = new FriendOverlay(mark, mMapView);
		// 将IteminizedOverlay添加到MapView中

		mMapView.getOverlays().clear();
		mMapView.getOverlays().add(itemOverlay);

		// 现在所有准备工作已准备好，使用以下方法管理overlay.
		// 添加overlay, 当批量添加Overlay时使用addItem(List<OverlayItem>)效率更高
		itemOverlay.addItem(item1);
		itemOverlay.addItem(item2);
		itemOverlay.addItem(item3);
		mMapView.refresh();
	}

	/*
	 * (非 Javadoc,覆写的方法) <p>Title: onRestoreInstanceState</p> <p>Description:
	 * </p>
	 * 
	 * @param savedInstanceState
	 * 
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		Log.d(TAG,
				"onRestoreInstanceState取出我的位置"
						+ savedInstanceState.getString("myLocation"));
	}

	/*
	 * (非 Javadoc,覆写的方法) <p>Title: onSaveInstanceState</p> <p>Description: </p>
	 * 
	 * @param outState
	 * 
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {

		outState.putString("myLocation", mMyGeoPoint.toString());
		Log.d(TAG, "onSaveInstanceState保存我的位置");
		super.onSaveInstanceState(outState);
	}

	/**
	 * 功能 ： 页面销毁 (non-Javadoc)
	 * 
	 * @see com.baidu.mapapi.MapActivity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		mMapView.destroy();
		if (mBMapManager != null) {
			mBMapManager.destroy();
			mBMapManager = null;
		}
		// 将本页面从Set中移除
		MainViewActivity.removeFromSet(this);
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		mMapView.onPause();
		if (mBMapManager != null) {
			mBMapManager.stop();
		}
		// 关闭GPS
		turnGPSOff();
		super.onPause();
	}

	@Override
	protected void onResume() {
		mMapView.onResume();
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
		mOverLays = new FriendOverlay(marker, mMapView);
		mMapOverlays.add(mOverLays);

		// 创建点击mark时的弹出泡泡
		mPopView = super.getLayoutInflater().inflate(R.layout.popview, null);
		mMapView.addView(mPopView, new MapView.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, null,
				MapView.LayoutParams.TOP_LEFT));
		mPopView.setVisibility(View.GONE);

		// 弹出的气泡窗口中的按钮,点击时获取到好友位置的路线
		findBtn = (Button) mPopView.findViewById(R.id.findBtn);
		findBtn.getBackground().setAlpha(240);
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
					startActivity(myIntentDial);
				}
			}
		});

	}

	/**
	 * 功能 ： 在地图上显示我的位置
	 * 
	 */
	private void updateMyLocation() {
		if (myLocationOverlay == null) {
			// 添加定位图层
			myLocationOverlay = new MyLocationOverlay(mMapView);
			mMapView.getOverlays().add(myLocationOverlay);
		}
		if (mMyGeoPoint == null) {
			return;
		}
		LocationData locData = new LocationData();
		// 手动将位置源置为天安门，在实际应用中，请使用百度定位SDK获取位置信息，要在SDK中显示一个位置，需要使用百度经纬度坐标（bd09ll）
		locData.latitude = mMyGeoPoint.getLatitudeE6() / 1E6;
		locData.longitude = mMyGeoPoint.getLongitudeE6() / 1E6;
		locData.direction = mDirection;
		myLocationOverlay.setData(locData);
		mMapView.refresh();
		if (isFirstLocated) {
			mMapController.animateTo(mMyGeoPoint);
			isFirstLocated = false;
		}
	}

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
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// setIcon()方法为菜单设置图标，这里使用的是系统自带的图标
		menu.add(Menu.NONE, Menu.FIRST + 1, 1, "我的位置").setIcon(
				android.R.drawable.ic_menu_mylocation);

		menu.add(Menu.NONE, Menu.FIRST + 2, 2, "搜索").setIcon(
				android.R.drawable.ic_menu_search);

		menu.add(Menu.NONE, Menu.FIRST + 3, 3, "视图").setIcon(
				android.R.drawable.ic_menu_slideshow);
		menu.add(Menu.NONE, Menu.FIRST + 4, 4, "分享位置").setIcon(
				android.R.drawable.ic_menu_share);
		menu.add(Menu.NONE, Menu.FIRST + 5, 5, "清空视图").setIcon(
				android.R.drawable.ic_menu_info_details);
		menu.add(Menu.NONE, Menu.FIRST + 6, 6, "退出").setIcon(
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
			mLocationClient.requestLocation();
			mMapController.animateTo(mMyGeoPoint);
			mMapController.setCenter(mMyGeoPoint);
			break;
		case Menu.FIRST + 2: // 搜索菜单,包括路线搜索、周边搜索、同城好友
			searchMenu();
			break;
		case Menu.FIRST + 3: // 视图修改
			changeMapViewMenu();
			break;
		case Menu.FIRST + 4: // 分享我的位置
			shareMyLocation();
			break;
		case Menu.FIRST + 5: // 清空视图
			clearMapView();
			updateMyLocation();
			break;
		case Menu.FIRST + 6: // 退出程序
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
			mOverLays.removeAll();
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

	/**
	 * 
	 * @param location
	 * @return
	 */
	private GeoPoint makeGeoPoint(BDLocation location) {
		return new GeoPoint((int) (location.getLatitude() * 1E6),
				(int) (location.getLongitude() * 1E6));
	}

	/****
	 * 功能 ： 向服务器发送我的坐标位置,分享给其他好友
	 * 
	 ****/
	private void shareMyLocation() {
		// WIFI模式下无法获取坐标
		if (mMyGeoPoint == null) {
			mMyGeoPoint = new GeoPoint((int) (121.388 * 1E6),
					(int) (38.588 * 1E6));
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
				String result = (String) h.executeRequest();
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
	private void addFriendToMap(GeoPoint geoPoint, int flag, String id,
			String name) {
		// Log.d(TAG, "#### addFriendToMap  111");
		// mAddrFlag = flag;
		// if (geoPoint != mMyGeoPoint) {
		// geoPointTo = geoPoint;
		// }
		//
		// geoPointTo = geoPoint;
		// mMKSearch.reverseGeocode(geoPointTo);
		// // 将好友添加到地图上
		// mOverLays.addFriendOverLayItem(geoPoint, name, id);
		// mMapOverlays.clear();
		// // mMapOverlays.add(myLocationOverlay);
		// // mMapOverlays.add(mOverLays);
		// mMapView.refresh();

		mOverLays.addFriendOverLayItem(geoPoint, id, name);
		mMapOverlays.add(mOverLays);
		mMapView.refresh();
		Log.d(TAG, "#### addFriendToMap  222");
	}

	/**
	 * 
	 * @Method: getFriendsLocation
	 * @Description: 向服务器请求所有好友的坐标信息 ,并且添加到地图上
	 * @return void 返回类型
	 * @throws
	 */
	private void getFriendsLocation() {
		// 获取在线的同城好友,测试的时候不能用,局域网问题
		Thread friendsPointThread = new Thread(friendGeoRunnable);
		friendsPointThread.start();
	}

	/**
	 * 向服务器发出请求的线程,并且向UI线程投递消息
	 */
	private Handler mHandler1 = new MyHandler(Looper.getMainLooper());

	/**
	 * 
	 */
	Runnable friendGeoRunnable = new Runnable() {
		String msg = ""; // 要发送给主线程的String

		@Override
		public void run() {
			ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs
					.add(new BasicNameValuePair("protocol", "getAddress"));// 封装键值对
			HttpThread h = new HttpThread(nameValuePairs, 8); // 8--获取好友坐标
			msg = h.executeRequest().toString(); // 接收服务器的返回值
			Log.d(TAG, "### 获取周边好友数据 : " + msg);
			sendMessage(); // 发送消息给主线程
		}

		private void sendMessage() {
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
			try {
				String s1 = msg.obj.toString().toString(); // 接收到子线程的字符串
				String array[] = s1.split(";;"); // 拆分字符串

				int len = array.length / 4;
				for (int i = 0; i < len; i++) {
					// 构造坐标点
					// GeoPoint geoPoint = new GeoPoint(
					// (int) (Integer.parseInt(array[i + 2]) * 1E6),
					// (int) (Integer.parseInt(array[i + 3]) * 1E6));
					GeoPoint geoPoint = new GeoPoint(
							(int) (Float.parseFloat(array[i + 3]) * 1E6),
							(int) (Float.parseFloat(array[i + 2]) * 1E6));

					Log.d(TAG, array[i] + " ,昵称 : " + array[i + 1]
							+ ",好友的经度 : " + array[i + 2] + " 纬度 : "
							+ array[i + 3]);
					Log.d(TAG, "### 好友最终GeoPoint : " + geoPoint.toString());
					// 接收到坐标数据,将坐标点添加到地图上
					addFriendToMap(geoPoint, 2, array[i] + " --> ADD",
							array[i + 1]);
				}
			} catch (Exception e) {
				Log.d(TAG, e.toString());
				e.printStackTrace();
			}

			mMapView.refresh();
		}
	}

	/**
	 * 
	 * @Method: displayToast
	 * @Description: 显示toast信息
	 * @param msg
	 *            要显示的内容
	 * @return void 返回类型
	 * @throws
	 */
	private void displayToast(String msg) {

		Toast.makeText(BaiduMapActivity.this, msg, 0).show();
	}

	/**
	 * 
	 * @Method: searchMenu
	 * @Description: 搜索菜单的选择操作
	 * @return void 返回类型
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
	 * @Description: POI选择响应函数,获取被选择的POI类型
	 * @param i
	 *            代表选择的POI类型,比如ATM、邮局、公园等
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

		mLocationClient.stop();
		// 关闭GPS
		turnGPSOff();
		super.onStop();

	}

	/*
	 * 要处理overlay点击事件时需要继承ItemizedOverlay 不处理点击事件时可直接生成ItemizedOverlay.
	 */
	class FriendOverlay extends ItemizedOverlay<OverlayItem> {
		/**
		 * 用MapView构造ItemizedOverlay
		 * 
		 * @param mark
		 * @param mapView
		 */
		public FriendOverlay(Drawable mark, MapView mapView) {
			super(mark, mapView);
		}

		protected boolean onTap(int index) {

			OverlayItem curItem = getItem(index);
			String title = curItem.getTitle();

			// 在此处理item点击事件
			Log.d(TAG,
					"item onTap: " + index + ",  item info : "
							+ curItem.toString());

			mFindBtnFlag = 1; // 设置为路线搜索模式
			geoPointTo = curItem.getPoint(); // 获取好友的位置,也是气泡显示的坐标位置

			// 搜索到好友位置的路线
			Button dailBtn = (Button) mPopView.findViewById(R.id.findBtn);
			dailBtn.setText("搜索路线");

			// 设置气泡中的内容
			TextView titleView = (TextView) mPopView
					.findViewById(R.id.pop_title);
			if (LoginActivity.mineName.equals(title)) {
				titleView.setText(title);
				// 隐藏获取路线的按钮
				dailBtn.setVisibility(View.GONE);
				dailBtn.setEnabled(false);
				displayToast("hey,这是本机标识...");
			} else {
				titleView.setText("好友 : " + title);
				// 隐藏获取路线的按钮
				dailBtn.setVisibility(View.VISIBLE);
				dailBtn.setEnabled(true);
			}

			mMapView.updateViewLayout(mPopView, new MapView.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					geoPointTo, MapView.LayoutParams.BOTTOM_CENTER));

			mPopView.setVisibility(View.VISIBLE); // 显示气泡

			return true;
		}

		public boolean onTap(GeoPoint pt, MapView mapView) {
			// 在此处理MapView的点击事件，当返回 true时
			super.onTap(pt, mapView);
			return false;
		}

		public void addFriendOverLayItem(GeoPoint geoPoint, String username,
				String snippet) {
			OverlayItem item = new OverlayItem(geoPoint, username, snippet);
			addItem(item);
		}
	}

	// /**
	// *
	// * 地图覆盖物内部类,好友的覆盖物内部类
	// *
	// * @Author: Mr.Simple
	// * @E-mail: bboyfeiyu@gmail.com
	// * @Date 2012-11-16 下午6:03:22
	// *
	// */
	//
	// class MyOverItem extends ItemizedOverlay<OverlayItem> {
	//
	// protected List<OverlayItem> mGeoList = new ArrayList<OverlayItem>();
	// // 保存已经存在的用户的坐标位置
	// protected List<String> mNameList = new ArrayList<String>();
	// // 画笔对象，绘制地图覆盖物
	// Paint paint = new Paint();
	//
	// /**
	// *
	// * @@param marker
	// * @@param context
	// * @Description: 构造函数
	// * @param marker
	// * @param context
	// */
	// public MyOverItem(Drawable marker, Context context) {
	// super(marker, mMapView);
	//
	// if (mMyGeoPoint != null) {
	// mGeoList.add(new OverlayItem(mMyGeoPoint, "MR.SIMPLE", "我在大连."));
	// }
	// }
	//
	// @Override
	// protected OverlayItem createItem(int i) {
	// return mGeoList.get(i);
	// }
	//
	// /**
	// * (非 Javadoc,覆写的方法)
	// *
	// * @Title: size
	// * @Description:
	// * @return
	// * @see com.baidu.mapapi.ItemizedOverlay#size()
	// */
	// @Override
	// public int size() {
	// return mGeoList.size();
	// }
	//
	// /**
	// * 功能 ： 处理当点击事件 (non-Javadoc)
	// *
	// * @see com.baidu.mapapi.ItemizedOverlay#onTap(int)
	// */
	// @Override
	// protected boolean onTap(int i) {
	//
	// mFindBtnFlag = 1; // 设置为路线搜索模式
	// geoPointTo = mGeoList.get(i).getPoint(); // 获取好友的位置,也是气泡显示的坐标位置
	//
	// // 搜索到好友位置的路线
	// Button dailBtn = (Button) mPopView.findViewById(R.id.findBtn);
	// dailBtn.setText("搜索路线");
	//
	// // 设置气泡中的内容
	// TextView titleView = (TextView) mPopView.findViewById(R.id.pop_title);
	// if (LoginActivity.mineName.equals(mGeoList.get(i).getTitle())) {
	// titleView.setText(mGeoList.get(i).getTitle());
	// // 隐藏获取路线的按钮
	// dailBtn.setVisibility(View.GONE);
	// dailBtn.setEnabled(false);
	// displayToast("hey,这是本机标识...");
	// } else {
	// titleView.setText("好友 : " + mGeoList.get(i).getTitle());
	// // 隐藏获取路线的按钮
	// dailBtn.setVisibility(View.VISIBLE);
	// dailBtn.setEnabled(true);
	// }
	//
	// BaiduMapActivity.this.mMapView.updateViewLayout(
	// BaiduMapActivity.mPopView, new MapView.LayoutParams(
	// LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
	// geoPointTo, MapView.LayoutParams.BOTTOM_CENTER));
	//
	// BaiduMapActivity.mPopView.setVisibility(View.VISIBLE); // 显示气泡
	//
	// return true;
	//
	// }
	//
	// /**
	// * 功能 ： 失去焦点,消去弹出的气泡 (non-Javadoc)
	// *
	// * @see com.baidu.mapapi.ItemizedOverlay#onTap(com.baidu.mapapi.GeoPoint,
	// * com.baidu.mapapi.MapView)
	// */
	// @Override
	// public boolean onTap(GeoPoint geoPoint, MapView mapView) {
	//
	// BaiduMapActivity.mPopView.setVisibility(View.GONE);
	// return super.onTap(geoPoint, mapView);
	// }
	//
	// /**
	// *
	// * 添加好友标识覆盖物到某坐标
	// *
	// * @param gPoint
	// * 好友的坐标
	// * @param name
	// * 好友昵称
	// * @param content
	// * @throws
	// */
	// public void addFriendOverLayItem(GeoPoint gPoint, String name,
	// String content) {
	// // Log.d(TAG,
	// // "addFriendOverLayItem --> 添加好友" + name + "到地图 "
	// // + gPoint.toString() + ",    lati e6 = "
	// // + gPoint.getLatitudeE6());
	// // GeoPoint geoPoint = new GeoPoint(
	// // (int) (gPoint.getLatitudeE6() * 1e6),
	// // (int) (gPoint.getLongitudeE6() * 1e6));
	// // new OverlayItem(geoPoint, name, content);
	// // if (!mNameList.contains(name)) {
	// // OverlayItem item = new OverlayItem(geoPoint, name, content);
	// // mGeoList.add(item);
	// // addItem(item);
	// // mNameList.add(name);
	// // }
	// }
	//
	// /**
	// *
	// * @Method: clearOverLayItems
	// * @Description: 将数据清除
	// * @throws
	// */
	// public void clearOverLayItems() {
	// mGeoList.clear();
	// mNameList.clear();
	// }
	//
	// } // end of MyItemOverlay class.

	/**
	 * 定位监听器
	 * 
	 * @author mrsimple
	 *
	 */
	public class MyLocationListener implements BDLocationListener {
		@Override
		public void onReceiveLocation(BDLocation location) {
			if (location == null) {
				return;
			}
			StringBuffer sb = new StringBuffer(256);
			sb.append("time : ");
			sb.append(location.getTime());
			sb.append("\nerror code : ");
			sb.append(location.getLocType());
			sb.append("\nlatitude : ");
			sb.append(location.getLatitude());
			sb.append("\nlontitude : ");
			sb.append(location.getLongitude());
			sb.append("\nradius : ");
			sb.append(location.getRadius());
			if (location.getLocType() == BDLocation.TypeGpsLocation) {
				sb.append("\nspeed : ");
				sb.append(location.getSpeed());
				sb.append("\nsatellite : ");
				sb.append(location.getSatelliteNumber());
			} else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {
				sb.append("\naddr : ");
				sb.append(location.getAddrStr());
			}
			mDirection = location.getDirection();
			Log.d(TAG, "### 位置信息 : " + sb.toString());
			mMyGeoPoint = makeGeoPoint(location);
			updateMyLocation();
		}

		public void onReceivePoi(BDLocation poiLocation) {
			// 将在下个版本中去除poi功能
			if (poiLocation == null) {
				return;
			}
			StringBuffer sb = new StringBuffer(256);
			sb.append("Poi time : ");
			sb.append(poiLocation.getTime());
			sb.append("\nerror code : ");
			sb.append(poiLocation.getLocType());
			sb.append("\nlatitude : ");
			sb.append(poiLocation.getLatitude());
			sb.append("\nlontitude : ");
			sb.append(poiLocation.getLongitude());
			sb.append("\nradius : ");
			sb.append(poiLocation.getRadius());
			if (poiLocation.getLocType() == BDLocation.TypeNetWorkLocation) {
				sb.append("\naddr : ");
				sb.append(poiLocation.getAddrStr());
			}
			if (poiLocation.hasPoi()) {
				sb.append("\nPoi:");
				sb.append(poiLocation.getPoi());
			} else {
				sb.append("noPoi information");
			}
			Log.d(TAG, sb.toString());
		}
	}

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

			if (addrInfo.city.contains(mFCity) && geoPointTo != null) // 同城则添加到地图上
			{
				displayToast("好友" + "在 ： " + addrInfo.city + " 坐标： " + Location);
				mOverLays.addFriendOverLayItem(geoPointTo, "Name", "title");
			}
		}

		/**
		 * 功能 ： 返回驾乘路线搜索结果 参数1： 搜索结果 参数2： 错误号，0表示正确返回
		 */
		@Override
		public void onGetDrivingRouteResult(MKDrivingRouteResult result,
				int iError) {

			if (result == null || iError != 0) {
				displayToast("路线搜索失败...");
				return;
			}

			// 路线覆盖物
			RouteOverlay routeOverlay = new RouteOverlay(BaiduMapActivity.this,
					mMapView);
			routeOverlay.setData(result.getPlan(0).getRoute(0));
			// mMapView.getOverlays().clear();
			mMapView.getOverlays().add(routeOverlay);
			mMapView.refresh();
			mMapController.setCenter(result.getStart().pt);

		}

		/**
		 * 返回poi搜索结果。 参数1 ：搜索结果 , 参数2 ：返回结果类型: MKSearch.TYPE_POI_LIST
		 * 、MKSearch.TYPE_AREA_POI_LIST、 MKSearch.TYPE_CITY_LIST 参数3 ： -
		 * 错误号，0表示正确返回
		 */
		@Override
		public void onGetPoiResult(MKPoiResult result, int type, int iError) {

			if (result == null || iError != 0) {
				displayToast("抱歉,未找到结果.");
				return;
			}

			PoiOverlay poioverlay = new PoiOverlay(BaiduMapActivity.this,
					mMapView);
			poioverlay.setData(result.getAllPoi());
			mMapView.getOverlays().add(poioverlay);
			mMapView.invalidate();
		}

		/**
		 * 返回公交搜索结果 参数1： 搜索结果 参数2： - 错误号，0表示正确返回， 当返回MKEvent.ERROR_ROUTE_ADDR时，
		 * 表示起点或终点有歧义， 调用MKTransitRouteResult的getAddrResult方法获取推荐的起点或终点信息
		 */
		@Override
		public void onGetTransitRouteResult(MKTransitRouteResult result,
				int iError) {

			Log.d("RoutePlan", "the res is " + result + "__" + iError);

			if (iError != 0 || result == null) {
				displayToast("抱歉,未找到结果");
				return;
			}

			TransitOverlay routeOverlay = new TransitOverlay(
					BaiduMapActivity.this, mMapView);
			// 此处仅展示一个方案作为示例
			routeOverlay.setData(result.getPlan(0));
			// mMapView.getOverlays().clear();
			mMapView.getOverlays().add(routeOverlay);
			mMapView.invalidate();

			mMapView.getController().animateTo(result.getStart().pt);

		}

		/**
		 * 返回步行路线搜索结果。 参数1： 搜索结果 参数2：- 错误号，0表示正确返回
		 */
		@Override
		public void onGetWalkingRouteResult(MKWalkingRouteResult result,
				int iError) {

			if (iError != 0 || result == null) {
				displayToast("抱歉,未找到结果");
				return;
			}
			RouteOverlay routeOverlay = new RouteOverlay(BaiduMapActivity.this,
					mMapView);
			// 此处仅展示一个方案作为示例
			routeOverlay.setData(result.getPlan(0).getRoute(0));
			mMapView.getOverlays().clear();
			mMapView.getOverlays().add(routeOverlay);
			mMapView.invalidate();

			mMapView.getController().animateTo(result.getStart().pt);
		}

		@Override
		public void onGetBusDetailResult(MKBusLineResult arg0, int arg1) {

		}

		@Override
		public void onGetPoiDetailSearchResult(int arg0, int arg1) {

		}

		@Override
		public void onGetShareUrlResult(MKShareUrlResult arg0, int arg1,
				int arg2) {

		}

		@Override
		public void onGetSuggestionResult(MKSuggestionResult arg0, int arg1) {

		}
	}

} // end of activity
