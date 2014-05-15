package album.entry;

import help_dlg.HelpDialog;
import imageEdit.PictureEditActivity;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import map.BaiduMapActivity;
import network.HttpThread;
import network.NetInfomation;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import albums.PhotoAlbumActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import bluetooth.BluetoothChat;
import camera.CameraActivity;
import chat.ChatActivity;
import chat.FriendsListActivity;
import chat.SocketService;
import chat.SocketService.LocalBinder;

/**
 * 
 * @ClassName: MainViewActivity 
 * @Description:  程序主界面,各功能模块的入口
 * @Author: Mr.Simple (何红辉)
 * @E-mail: bboyfeiyu@gmail.com 
 * @Date 2012-11-17 下午5:15:12 
 *
 */
public class MainViewActivity extends Activity implements OnClickListener,
		OnTouchListener, android.view.GestureDetector.OnGestureListener {

	// 使用HashSet来存储用户的Activity,Set不允许含有重复元素
	public static Set<Activity> gAtcSet = new HashSet<Activity>();

	private ImageButton cameraBtn = null; 			// 进入拍照模块按钮
	private ImageButton ablumsBtn = null; 			// 进入相册模块按钮
	private ImageButton chatBtn = null; 			// 进入聊天模块按钮
	private ImageButton mapBtn = null; 				// 进入地图模块按钮
	private ImageButton psBtn = null; 				// 进入编辑模块按钮
	private ImageButton blueToothBtn = null; 		// 进入蓝牙模块按钮
	private GestureDetector mGestureDetector = null;// 手势触摸探测器

	private PopupWindow popupWindow; 		// 主菜单窗口
	private GridView menuGrid;				// 布局菜单的网格视图
	public static int mThemeMode = 0; 		// 主题模式
	private final String SETTING = "usr_info"; // 保存信息到本地的share
	private final String TAG = "MAINACTIVITY";

	private final String CHAT_ACTION = "chat.SocketService.chatMessage";// 接收消息广播器的action
	private final String ONLINE_ACTION = "chat.SocketService.onlie"; 	// 接收消息广播器的action
	public static SocketService mSocketService = null; 					// socket服务
	private static MyBroadcastReciver mDataReceiver = null; 			// 广播接收器

	private static Intent sIntent = null; 		// 启动服务的intent
	private MediaPlayer mMediaPlayer = null; 	// 多媒体对象
	private static int notifyTag = 0; 			// 推送标识
	private Vibrator mVibrator; 				// 震动
	private String fName = ""; 					// 好友的名字
	public static boolean isNotify = true; 		// 是否推送消息的标识
	private MyHandlerAlbum mHandlerAlb = null; 	// 向网络请求网络相册列表

	public static List<String> sFriendList = new ArrayList<String>();	// 好友列表
	public static List<String> sAlbumList = new ArrayList<String>();	// 相册列表

	// 菜单数组
	private String[] menu_name_array = { "设置", "注销", "换肤", "帮助", "退出" };
	// 图像数组
	int[] menu_image_array = { android.R.drawable.ic_menu_set_as,
			android.R.drawable.ic_menu_agenda, android.R.drawable.ic_menu_help,
			android.R.drawable.ic_menu_info_details,
			android.R.drawable.ic_lock_power_off };
	

	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onCreate
	 * @Description:  页面创建
	 * @param savedInstanceState 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		//设置无标题  
        requestWindowFeature(Window.FEATURE_NO_TITLE);  
		setContentView(R.layout.mainviewactivity);

		// 初始化按钮等组件
		initComponents();
		// 获取所有在线的好友列表,存在静态数组中
		getAllUsersInfo();
		// 获取自己的网络相册列表
		getNetAlbumsList();

		// 将当前页面添加到map中
		MainViewActivity.addActivityToHashSet(this);
		// 从本地读取皮肤模式
		readSkinModeFromLocal();
		// 根据时间主动切换主题
		autoChangeSkin();

		try{

			// 广播接收器,接收好友发来的消息(通过service的广播来传递的)
			mDataReceiver = new MyBroadcastReciver();
			IntentFilter intentFilter = new IntentFilter(CHAT_ACTION);
			Log.d(TAG, "注册结果 ： " + registerReceiver(mDataReceiver, intentFilter));
			
			// 启动socket监听服务
			sIntent = new Intent(this, SocketService.class);
			sIntent.putExtra("key", "Service Start");
			startService(sIntent);
		}catch(Exception e){
			e.printStackTrace() ;
		}

		// 向服务器发送一个socket消息,使得服务器含有客户端的路由信息
		sendRouteMsg() ;
	}


	/**
	 * @Method: initComponents 函数
	 * @Description: 初始化组件
	 */
	private void initComponents() {

		cameraBtn = (ImageButton) findViewById(R.id.cameraBtn);
		cameraBtn.setOnClickListener(this);

		ablumsBtn = (ImageButton) findViewById(R.id.ablumsBtn);
		ablumsBtn.setOnClickListener(this);

		chatBtn = (ImageButton) findViewById(R.id.chatBtn);
		chatBtn.setOnClickListener(this);

		mapBtn = (ImageButton) findViewById(R.id.mapBtn);
		mapBtn.setOnClickListener(this);

		psBtn = (ImageButton) findViewById(R.id.psBtn);
		psBtn.setOnClickListener(this);

		blueToothBtn = (ImageButton) findViewById(R.id.blueToothBtn);
		blueToothBtn.setOnClickListener(this);

		// 设置监听器mGestureDetector
		LinearLayout mainLayout = (LinearLayout) findViewById(R.id.parent);
		mGestureDetector = new GestureDetector(this); // 手势识别
		mainLayout.setOnTouchListener(this); // 设置触摸监听器
		mainLayout.setLongClickable(true); // 设置可长按

	}


	/**
	 * @Method: autoChangeSkin 函数
	 * @Description: 根据时间切换合适的主题模式
	 */
	private void autoChangeSkin() {

		Calendar cal = Calendar.getInstance();
		int hour = cal.get(Calendar.HOUR_OF_DAY); // 获取时间中的小时

		// 获取整个布局,mainActivity布局
		final LinearLayout layout = (LinearLayout) findViewById(R.id.parent);

		// 晚上七点到早晨七点之间，则切换到夜间类型主题
		if ((hour > 18 || hour < 7) && (mThemeMode != 0 || mThemeMode != 2)) {
			mThemeMode = 0;
			layout.setBackgroundResource(R.drawable.bg_black);
			Toast.makeText(MainViewActivity.this,
					"现在是夜间" + hour + "点,已切换到夜间模式", Toast.LENGTH_SHORT).show();
			setTheme(android.R.style.Theme_Black);
		}
		// 日间类型主题
		if ((hour > 6 && hour < 18) && (mThemeMode != 1 || mThemeMode != 3)) {
			mThemeMode = 1;
			layout.setBackgroundResource(R.drawable.bg_light_02);
			Toast.makeText(MainViewActivity.this,
					"现在是白天" + hour + "点,已切换到日间模式", Toast.LENGTH_SHORT).show();
		}

		Log.d("mThemeMode ", "" + mThemeMode);
		// 设置文本字体颜色
		changeTextColor(mThemeMode);
		// 将皮肤模式保存到本地
		saveSkinModeToLocal();
	}

	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onClick
	 * @Description:  点击事件,各个功能模块的按钮 
	 * @param v 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {

		// 载入动画,在点击按钮的时候有动画
		Animation animation = AnimationUtils.loadAnimation(this,
				R.anim.scale_anim);
		// 实例化对象
		Intent intent = new Intent();
		int code = 0;

		if (v == cameraBtn) // 前往拍照的页面
		{
			cameraBtn.startAnimation(animation);
			// 设置要跳转到的目标界面
			intent.setClass(MainViewActivity.this, CameraActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			code = 1;
			// 跳转页面
			startActivityForResult(intent, code);
			// 页面切换的动画效果
			overridePendingTransition(android.R.anim.fade_out, android.R.anim.fade_in);
			return ;
		}

		if (v == ablumsBtn) { // 前往相册页面
			// 设置按钮动画
			ablumsBtn.startAnimation(animation);
			// 设置要跳转到的目标界面
			intent.setClass(MainViewActivity.this, PhotoAlbumActivity.class);
			intent.putExtra("id", LoginActivity.mineID);
			code = 2;

		}
		if (v == psBtn) // 前往编辑界面
		{
			psBtn.startAnimation(animation);
			// 设置要跳转到的目标界面
			intent.setClass(MainViewActivity.this, PictureEditActivity.class);
			intent.putExtra("photopath", "");
			code = 3;

		}
		if (v == blueToothBtn) // 蓝牙聊天界面
		{
			blueToothBtn.startAnimation(animation);
			// 设置要跳转到的目标界面
			intent.setClass(MainViewActivity.this, BluetoothChat.class);
			code = 4;
		}

		if (v == chatBtn) // 聊天界面
		{
			chatBtn.startAnimation(animation);
			// 设置要跳转到的目标界面
			intent.setClass(MainViewActivity.this, FriendsListActivity.class);
			code = 5;

		}
		if (v == mapBtn) { // 进入地图界面

			mapBtn.startAnimation(animation);
			// 设置要跳转到的目标界面
			intent.setClass(MainViewActivity.this, BaiduMapActivity.class);
			code = 6;
		}

		// 跳转页面
		startActivityForResult(intent, code);
		// 页面切换的动画效果
		overridePendingTransition(R.anim.slide_left, R.anim.slide_right);

	}
	
	/**
	 * 
	 * @Method: sendMsgUDP
	 * @Description: 向服务器发送消息,使得服务器路由中包括该客户端的公网出口IP
	 * @return void 返回类型
	 * @throws
	 */
	private void sendRouteMsg() {
		new Thread() {
			@Override
			public void run() {
				Looper.prepare();
				try {
					String serverUrl = "199.36.75.40";
					InetAddress iaddr = InetAddress.getByName(serverUrl);
					// 封装消息pakcet,将我的IP地址发给服务器存储
					String mMsg = "$" + NetInfomation.getLocalIpAddress() + "$";
					byte data[] = mMsg.getBytes("GB2312");
					// 创建一个DatagramPacket对象，并指定要讲这个数据包发送到网络当中的哪个地址，以及端口号
					DatagramPacket packet = new DatagramPacket(data,
							data.length, iaddr, 9876);

					SocketService.mUdpRevSocket.send(packet);
					Log.d("UDP", " Packet已经发送 ： " + mMsg);

				} catch (Exception e) {
					Toast.makeText(MainViewActivity.this, "发送超时,请检查网络...", 0)
							.show();
					e.printStackTrace();
				}
				Looper.loop();
			}

		}.start();

	}
		

	/**
	 * @Method: getAllUsersInfo 函数
	 * @Description: 向服务器发起请求,获取所有好友的列表
	 */
	private void getAllUsersInfo() {
		new Thread(new Runnable() {

			@Override
			public void run() {

				ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				nameValuePairs.add(new BasicNameValuePair("protocol", "getIP"));// 封装键值对
				HttpThread h = new HttpThread(nameValuePairs, 7); // 获取7 好友列表
				String msg = (String) h.sendInfo(); // 接收服务器的返回值

				Looper mainLooper = Looper.getMainLooper(); // 得到主线程loop
				mFHandler = new MyHandler(mainLooper); // 创建主线程的handler
				mFHandler.removeMessages(0); // 移除所有队列中的消息
				Message m = mFHandler.obtainMessage(1, 1, 1, msg); // 把消息放入message
				mFHandler.sendMessage(m);
				Log.d("Main", "Main中请求好友列表");
			}
		}).start();
	}


	/**
	 *  接收服务器返回的在线好友数据,使用mFHandler发送消息给UI线程
	 */
	private Handler mFHandler = new Handler();

	/**
	 * 
	 * @ClassName: MyHandler 
	 * @Description: 处理服务器返回来的好友列表数据,将数据添加到好友列表中
	 * @Author: Mr.Simple (何红辉)
	 * @E-mail: bboyfeiyu@gmail.com 
	 * @Date 2012-11-17 下午5:19:06 
	 *
	 */
	private class MyHandler extends Handler {

		public MyHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {

			sFriendList.clear();
			String fList = msg.obj.toString().trim(); // 接收到子线程的字符串
			if (fList.contains("error")) {
				Toast.makeText(MainViewActivity.this, "好友列表加载失败,请查看网络...", 0)
						.show();
				// 获取在线好友列表失败,则继续提交请求
				getAllUsersInfo() ;
				return;
			}
			// 设置缓存数组
			String arr[] = null;
			try {
				// 分离出数据元素,以;;作为分隔符
				arr = fList.split(";;");
				for (String item : arr) {
					// 将数据导入到List中
					sFriendList.add(item);
					Log.d("获取好友列表数据 : ", item);
				}

			} catch (Exception e) {
				Toast.makeText(MainViewActivity.this, "抱歉,好友列表获取失败~~~", 0).show();
			}

		}
	}


	/**
	 * @Method: getNetAlbumsList 函数
	 * @Description:  向服务器请求网络相册列表
	 */
	private void getNetAlbumsList() {
		Thread albThread = new Thread( rNetAlbum );
		albThread.start();
	}


	/**
	 * 请求网络相册列表的runnable
	 */
	Runnable rNetAlbum = new Runnable() {
		String msg;

		@Override
		public void run() {
			ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("protocol", "getAlbums"));
			nameValuePairs.add(new BasicNameValuePair("id",
					LoginActivity.mineID));
			HttpThread h = new HttpThread(nameValuePairs, 11); // 11--请求相册列表
			msg = h.sendInfo().toString(); // 接收服务器的返回值
			Log.d("请求网络相册列表", msg);
			sendMessage();
		}

		public void sendMessage() { // 线程间数据传输
			// 得到主线程loop
			Looper mainLooper = Looper.getMainLooper();
			// 创建主线程的handler
			mHandlerAlb = new MyHandlerAlbum(mainLooper);
			// 移除所有队列中的消息
			mHandlerAlb.removeMessages(0);
			// 把消息放入message
			Message m = mHandlerAlb.obtainMessage(1, 1, 1, msg);
			// 发送message
			mHandlerAlb.sendMessage(m);
		}
	};

	/*
	 * 获取网络相册列表后的处理
	 */
	private class MyHandlerAlbum extends Handler {
		public MyHandlerAlbum(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			// 获取数据
			String AlbumList = msg.obj.toString().trim();
			// 定义缓存数组
			String[] buf = null;
			try {
				buf = AlbumList.split(";;");
				// 第一个为请求返回的成功或者失败的标志
				if ( buf[0].equals("error") ){
					Log.d("网络相册列表请求出错", "返回ERROR");
					// 重新请求
					getNetAlbumsList() ; 
				}
				
				// 将网络相册列表加入到静态变量sAlbumList中
				for (String item : buf) {
					if ( !item.equals("error") || !item.equals("fail")){
						sAlbumList.add(item);
					}
					Log.d("MAIN接到相册列表-->", item);
				}

			} catch (Exception e) {
				e.printStackTrace();
				// 再次请求网络相册列表
				getNetAlbumsList() ; 
			}

		}
	}

	
	long exitTime = 0;
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onKeyDown
	 * @Description: 按键事件,在2秒内连续按返回键实现退出程序的功能
	 * @param keyCode
	 * @param event
	 * @return 
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_MENU: 	// 菜单键按下的操作,弹出菜单窗口
				openPopupwin();
				break;
	
			case KeyEvent.KEYCODE_BACK: {	// 退出
				if ((System.currentTimeMillis() - exitTime) > 2000
						&& event.getAction() == KeyEvent.ACTION_DOWN) {
	
		            Toast.makeText(getApplicationContext(), "再按一次就退出程序咯...", 
							Toast.LENGTH_SHORT).show();  
					exitTime = System.currentTimeMillis();
	
				} else {
					// 退出程序
					killCurrentApp(this);
				}
				// 这句很重要啊!不然会进行默认的回退到上一界面
				return true;
			}
			default:
				break;
		}
		return super.onKeyDown(keyCode, event);
	}
	

	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onResume
	 * @Description:  
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		
		super.onResume();
	}


	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onRestart
	 * @Description:  页面重新获取焦点   
	 * @see android.app.Activity#onRestart()
	 */
	@Override
	protected void onRestart() {
		
		// 如果上一次没有获取好友列表和相册列表,则重新启动时要获取列表
		if (sFriendList.size() == 0) {
			Log.d("好友列表", "重新获取");
			getAllUsersInfo();
		}
		if (sAlbumList.size() == 0) {
			Log.d("相册列表", "重新获取");
			getNetAlbumsList();
		}
		super.onRestart();
	}

	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onStop
	 * @Description:  
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		
		mHandler.removeCallbacks(vibRunnable); // 将runnable移除
		if ( mFHandler != null ){
			mFHandler = null;
		} 
		
		try{
			bindService(sIntent, mConnection, Context.BIND_AUTO_CREATE); // 绑定服务,使服务随activity消亡
		}catch(Exception e){
			e.printStackTrace() ;
		}
		super.onStop();
	}

	/******************************************************************************************
	 * 功能 ： 销毁页面 (non-Javadoc)
	 * 
	 * @see android.app.Activity#onRestart()
	 ******************************************************************************************/
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onDestroy
	 * @Description:  销毁页面,停止服务 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {

		Log.d("", "OnDestory中销毁");
		stopService( sIntent ); // 停止服务
		
		unregisterReceiver( mDataReceiver );
		super.onDestroy();
	}


	/**
	 * @Method: getMenuAdapter 函数
	 * @Description:  popupwindow's adapter.菜单窗口的适配器
	 * @param menuNameArray
	 * @param menuImageArray
	 * @return
	 */
	private ListAdapter getMenuAdapter(String[] menuNameArray,
			int[] menuImageArray) {

		// 数据源列表
		ArrayList<HashMap<String, Object>> dataSrc = new ArrayList<HashMap<String, Object>>();

		for (int i = 0; i < menuNameArray.length; i++) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("itemImage", menuImageArray[i]);
			map.put("itemText", menuNameArray[i]);
			dataSrc.add(map);
		}

		// 构造适配器,指定数据源和显示模板item_menu
		SimpleAdapter simperAdapter = new SimpleAdapter(this, dataSrc,
				R.layout.item_menu, new String[] { "itemImage", "itemText" },
				new int[] { R.id.item_image, R.id.item_text });
		return simperAdapter;

	}

	
	/**
	 * @Method: openPopupwin 函数
	 * @Description: 菜单窗口---popupwindow
	 */
	private void openPopupwin() {

		LayoutInflater mLayoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		ViewGroup menuView = (ViewGroup) mLayoutInflater.inflate(
				R.layout.gridview_pop, null, true);

		menuGrid = (GridView) menuView.findViewById(R.id.gridview_popup);
		menuGrid.setAdapter(getMenuAdapter(menu_name_array, menu_image_array));
		menuGrid.requestFocus();

		// 点击事件
		menuGrid.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {

				Intent intent = new Intent(); // Intent 定义
				switch (arg2) {

				case 0: // 设置
					Toast.makeText(MainViewActivity.this, menu_name_array[0], 0)
							.show();
					intent.setClass(MainViewActivity.this,
							SettingActivity.class);
					startActivity(intent);
					overridePendingTransition(R.anim.slide_left,
							R.anim.slide_right);
					break;

				case 1: // 注销
					Toast.makeText(MainViewActivity.this, menu_name_array[1], 0)
							.show();
					userOffLine(); // 用户向服务器发送离线消息
					LoginActivity.gNoAuto = 1;
					intent.setClass(MainViewActivity.this, LoginActivity.class);
					startActivity(intent);
					overridePendingTransition(R.anim.slide_left,
							R.anim.slide_right);
					break;

				case 2: // 换肤
					popupWindow.dismiss();
					openSkinDialog();
					break;

				case 3: // 帮助
					Toast.makeText(MainViewActivity.this, menu_name_array[3], 0)
							.show();
					HelpDialog helpDlg = new HelpDialog(MainViewActivity.this,
							R.string.main_help_text);
					helpDlg.showHelp();
					break;

				case 4: // 退出
					popupWindow.dismiss();
					killCurrentApp(MainViewActivity.this);
					break;

				default:
					break;
				}

			}
		});
		popupWindow = new PopupWindow(menuView, LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT, true);

		popupWindow.setBackgroundDrawable(new BitmapDrawable());
		popupWindow.setAnimationStyle(R.style.PopupAnimation);
		// 设置父窗口和显示的位置
		popupWindow.showAtLocation(findViewById(R.id.parent), Gravity.CENTER
				| Gravity.CENTER, 0, 0);
		popupWindow.update();

	}

	
	/**
	 * @Method: openSkinDialog 函数
	 * @Description:  给界面换皮肤
	 */
	private void openSkinDialog() {

		// 获取整个布局,mainActivity布局
		final LinearLayout layout = (LinearLayout) findViewById(R.id.parent);

		View menuView = View.inflate(this, R.layout.gridview_dlg, null);
		String[] menuitems = { "夜间模式", "日间模式", "红色岁月", "浅色柔情" }; // 弹出的菜单
		int[] imgs = { R.drawable.bg_black_s, R.drawable.bg_light_5_s,
				R.drawable.bg_red_s, R.drawable.bg_light_02_s };

		// 创建AlertDialog
		final AlertDialog frameDialog = new AlertDialog.Builder(this).create();
		frameDialog.setView(menuView);

		menuGrid = (GridView) menuView.findViewById(R.id.gridview_dlg);
		menuGrid.setAdapter(getMenuAdapter(menuitems, imgs)); // 设置菜单和图像
		menuGrid.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {

				switch (arg2) {

				case 0:
					frameDialog.dismiss();
					mThemeMode = 0;
					layout.setBackgroundResource(R.drawable.bg_black);
					break;

				case 1:
					frameDialog.dismiss();
					mThemeMode = 1;
					layout.setBackgroundResource(R.drawable.bg_light_5);
					break;

				case 2:
					frameDialog.dismiss();
					mThemeMode = 2;
					layout.setBackgroundResource(R.drawable.bg_red_2);
					break;

				case 3:
					frameDialog.dismiss();
					mThemeMode = 3;
					layout.setBackgroundResource(R.drawable.bg_light_02);
					break;

				default:
					break;
				} // end of switch

				// 改变文本的颜色
				changeTextColor(mThemeMode);
				// 保存皮肤模式到本地
				saveSkinModeToLocal();
			} // end of click.
		});

		frameDialog.show();
	}


	/**
	 * @Method: changeTextColor 函数
	 * @Description: 改变主界面的文本控件字体颜色
	 * @param mode   日间或者夜间模式
	 */
	private void changeTextColor(int mode) {
		// 颜色
		int color = 0;
		// 主界面中文本控件的获取
		TextView cameraTv = (TextView) findViewById(R.id.cameraTextView);
		TextView albumTv = (TextView) findViewById(R.id.albumTextView);
		TextView chatTv = (TextView) findViewById(R.id.chatTextView);
		TextView mapTv = (TextView) findViewById(R.id.mapTextView);
		TextView picEditTv = (TextView) findViewById(R.id.picTextView);
		TextView bluetoothTv = (TextView) findViewById(R.id.btTextView);

		// 白天模式文字背景为亮色
		if (mode % 2 != 0) {
			color = Color.BLACK;
		} else {
			// 夜间模式则为暗色调
			color = Color.rgb(150, 150, 150);
		}

		// 设置文本字体颜色
		cameraTv.setTextColor(color);
		albumTv.setTextColor(color);
		chatTv.setTextColor(color);
		mapTv.setTextColor(color);
		picEditTv.setTextColor(color);
		bluetoothTv.setTextColor(color);
	}


	/**
	 * @Method: saveSkinModeToLocal 
	 * @Description: 将用户的皮肤号到本地
	 */
	private void saveSkinModeToLocal() {
		// 保存用户密码
		SharedPreferences ref = getSharedPreferences(SETTING, 0);
		ref.edit().putInt("skinMode", mThemeMode).commit();
		Log.d("", "保存皮肤模式： " + mThemeMode);

	}


	/**
	 * @Method: readSkinModeFromLocal
	 * @Description: 从本地文件中读取用户保存的皮肤号
	 */
	private void readSkinModeFromLocal() {
		SharedPreferences settings = getSharedPreferences(SETTING, 0); // 获取一个对象

		mThemeMode = settings.getInt("skinMode", 0); // 取出保存的NAME
		Log.d("", "读出皮肤模式： " + mThemeMode);

		final LinearLayout layout = (LinearLayout) findViewById(R.id.parent);
		checkSkin(layout);
	}

	
	/**
	 * 
	 * @ClassName: MyBroadcastReciver 
	 * @Description:  廣播接收器,接收好友發送來的聊天消息和上線消息
	 * @Author: Mr.Simple 
	 * @E-mail: bboyfeiyu@gmail.com 
	 * @Date 2012-11-7 下午6:13:55 
	 *
	 */
	private class MyBroadcastReciver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// 获取action
			String action = intent.getAction();
			// 获取消息
			String msg = intent.getStringExtra("broadCast");
			try {
				// 处理广播发来的消息
				postMessage(action, msg);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	} // 用于接收socket服务消息广播的接收器

	
	/**
	 * @Method: postMessage
	 * @Description: 处理服务器发来的消息,从Service广播出来的消息
	 * @param action
	 * @param msg 参数
	 * @return void 返回类型
	 * @throws
	 */
	private void postMessage(String action, String msg) throws Exception {
		if ("".equals(msg)) {
			Log.d("main界面", "空消息");
			return;
		}

		// 广播的是聊天消息类型
		if (action.equals(CHAT_ACTION)) {

			String buf[] = msg.split(";;") ;
			fName = buf[2];
			if ( SocketService.mMsgMap.size() > 0 
					&& SocketService.mMsgMap.get( fName ) == "NO"){
				
				Log.d("main的广播接收器", fName +"的消息不推送");
			}else { 	// 接收到消息,则推送消息
				showNotification( msg );
			}
		} else if (action.equals(ONLINE_ACTION)) { // 上线消息,用户上线更新好友列表
			// 好友上线的广播类型
			// 获取消息,将数据存到网络相册列表中
			String buf[] = null;
			// 拆分字符串,获得有效数据
			buf = msg.split(";;");
			// 将数据追加到列表中
			for (String item : buf) {
				MainViewActivity.sFriendList.add(item);
			}
		}
	}


	NotificationManager mNM;
	/**
	 * @Method: showNotification 
	 * @Description: 推送消息,好友发送来的短消息
	 * @param msg   
	 * @throws
	 */
	private void showNotification(String msg) {

		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE); // 震动

		/****************************************************************
		 * 四个参数含义为： 100 毫秒延迟后，震动 300 毫秒，暂停 200 毫秒后，再震动 800 毫秒
		 **************************************************************** */
		long[] pattern = { 100, 500, 100, 200 }; // 根据指定的模式进行震动
		mVibrator.vibrate(pattern, 2); 			// -1不重复，非-1为从pattern的指定下标开始重复
		mHandler.postDelayed(vibRunnable, 1500);

		String friendIp = ""; // 对方的IP
		String friendName = "";
		String msgCont = ""; // 获取真实的消息内容,即去掉消息头部
		try {
			if (msg.contains(";;")) {
				// 分割数据
				String buf[] = msg.split(";;") ;
				// 好友IP
				friendIp = buf[1] ;
				// 好友昵称
				friendName = buf[2];
				// 消息内容
				msgCont = buf[3] ;
				Log.d(TAG, "接到的新消息: " + msgCont);
			}
		} catch (Exception e) {
			Log.d("main界面推送", e.toString());
			mVibrator.cancel();
		}

		CharSequence tips = "好友 " + friendName + " 发来消息";
		// 设置图标、标题、时间线
		Notification notification = new Notification(R.drawable.ichat, tips,
				System.currentTimeMillis());
		notification.defaults |= Notification.DEFAULT_SOUND;
		notification.defaults |= Notification.DEFAULT_LIGHTS;
		notification.flags = Notification.FLAG_AUTO_CANCEL;

		// 创建intent,并且向ChatActivity传递数据, 保证这种情况下不自动连接好友
		Intent intent = new Intent(this, ChatActivity.class);
		intent.putExtra("friendName", friendName);
		intent.putExtra("pendingMsg", msgCont);
		intent.putExtra("friendIp", friendIp);

		// 如果用户点击推送的通知,则跳转到聊天界面ChatActivity
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				intent, 0);
		// 设置消息内容
		notification.setLatestEventInfo(this, "来自 " + friendName + "的新消息", msgCont,
				contentIntent);

		// 将消息推送出去
		mNM.notify("Notify" + notifyTag, notifyTag++, notification);
	}


	/**
	 *  取消消息震动.
	 */
	private Handler mHandler = new Handler();
	Runnable vibRunnable = new Runnable() {

		@Override
		public void run() {
			mVibrator.cancel();
			if (mMediaPlayer != null) {
				mMediaPlayer.pause();
				mMediaPlayer.release();
				mMediaPlayer = null;
			}
		}
	};


	/**
	 * @Method: checkSkin
	 * @Description: 修改皮肤,如果是夜间,则自动切换夜间皮肤模式
	 * @param layout
	 */
	public static void checkSkin(ViewGroup layout) {
		Log.d("换肤检测", "检测主题");
		if (MainViewActivity.mThemeMode == 1) {
			layout.setBackgroundResource(R.drawable.bg_light_02);
		}

		if (MainViewActivity.mThemeMode == 2) {
			layout.setBackgroundResource(R.drawable.bg_red_2);
		}

		if (MainViewActivity.mThemeMode == 3) {
			layout.setBackgroundResource(R.drawable.bg_light_5);
		}

	}


	/**
	 * @Method: userOffLine
	 * @Description: 用户下线的消息
	 */
	public static void userOffLine() {
		new Thread() {
			@Override
			public void run() {
				ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(); // 打包数据
				nameValuePairs
						.add(new BasicNameValuePair("protocol", "offLine"));
				nameValuePairs.add(new BasicNameValuePair("id",
						LoginActivity.mineID));
				HttpThread h = new HttpThread(nameValuePairs, 10); // 10--退出
				h.sendInfo();
				Log.d("1", "退出");
			};
			// 接收服务器的返回值
		}.start();
	}

	
	/**
	 * 定义service绑定的回调，传给bindService()的
	 */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {

			// 我们已经绑定到了LocalService，把IBinder进行强制类型转换并且获取LocalService实例．
			LocalBinder binder = (LocalBinder) service;
			mSocketService = binder.getService();
			Log.d("", "初始化-------> mConnection");
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {

		}
	};


	/**
	 * @Method: addActivityToHashSet
	 * @Description: 将页面添加到set中,用于完全退出程序
	 * @param atc    被添加进来的Activity
	 */
	public static void addActivityToHashSet(Activity atc) {

		if (gAtcSet.contains(atc)) {
			Log.d("SET", "窗口已经存在Set中");
		} else {
			Log.d("SET", "将窗口添加存在Set中");
			gAtcSet.add(atc);
		}

	}

	
	/**
	 * @Method: removeFromSet
	 * @Description:  将Activity移除
	 * @param atc
	 */
	public static void removeFromSet(Activity atc) {
		gAtcSet.remove(atc);
		Log.d("窗口移除", "MainActivity");
	}


	/**
	 * @Method: releaseService
	 * @Description: 释放服务中的资源
	 */
	private static void releaseService() {
		SocketService.mUdpStop = true; // 停止服务中接收消息的控制变量
		if (SocketService.mUdpRevThread != null) {
			SocketService.mUdpRevThread.interrupt();
			SocketService.mUdpRevThread = null;
		}

	}

	
	/**
	 * @Method: killCurrentApp
	 * @Description:  退出整个程序
	 * @param context
	 */
	public static void killCurrentApp(Context context) {
		// 用户下线的消息
		userOffLine();

		try {
			Thread.sleep(600); // 睡眠，等待退出
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// 释放服务中的变量
		releaseService();

		// 迭代结束掉窗口
		for (Activity atc : gAtcSet) {
			atc.finish();
		}
		android.os.Process.killProcess(android.os.Process.myPid());

	} // end of killCurrentApp(Context);

	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onDown
	 * @Description:   鼠标手势,触屏切换页面
	 * @param e
	 * @return 
	 * @see android.view.GestureDetector.OnGestureListener#onDown(android.view.MotionEvent)
	 */
	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}


	private int verticalMinDistance = 150;
	private int minVelocity = 0;
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onFling
	 * @Description:  手势划动,切换屏幕 描述： 鼠标手势相当于一个向量（当然有可能手势是曲线），e1为向量的起点，e2为向量的终点，
	 * 				  velocityX为向量水平方向的速度，velocityY为向量垂直方向的速度
	 * @param e1
	 * @param e2
	 * @param velocityX
	 * @param velocityY
	 * @return 
	 * @see android.view.GestureDetector.OnGestureListener#onFling(android.view.MotionEvent, android.view.MotionEvent, float, float)
	 */
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		if (e1.getX() - e2.getX() > verticalMinDistance
				&& Math.abs(velocityX) > minVelocity) {

			// 向右划动的手势,切换到相册的Activity
			Intent cIntent = new Intent();
			cIntent.setClass(MainViewActivity.this, PhotoAlbumActivity.class);
			cIntent.putExtra("id", LoginActivity.mineID);
			startActivityForResult(cIntent, 2);
			overridePendingTransition(R.anim.slide_left, R.anim.slide_right);

		} else if (e2.getX() - e1.getX() > verticalMinDistance
				&& Math.abs(velocityX) > minVelocity) {
			// 向左划动的手势
			Intent bIntent = new Intent();
			bIntent.setClass(MainViewActivity.this, CameraActivity.class);
			bIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivityForResult(bIntent, 1);
			overridePendingTransition(android.R.anim.fade_out, android.R.anim.fade_in);

		}

		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {

	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {

	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onTouch(View arg0, MotionEvent event) {
		return mGestureDetector.onTouchEvent(event);
	}

} // end of class

