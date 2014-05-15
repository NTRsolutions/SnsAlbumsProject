package album.entry;

import java.util.ArrayList;
import java.util.Random;

import network.HttpThread;
import network.NetInfomation;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


/*
* Copyright (c) 2012,UIT-ESPACE
* All rights reserved.
*
* 文件名称：LoginActivity.java  
* 摘 要： 用户登录页面
* 
* 功能：
* 1.用户登录
*  
* 当前版本：1.1
* 作 者：何红辉
* 完成日期：2012年11月3日
*
* 取代版本：1.0
* 原作者 ：徐晓佳
* 完成日期：2012年9月12日
* 
*/

/**
 * 
 * @ClassName: LoginActivity 
 * @Description: 登录页面
 * @Author: Mr.Simple 
 * @E-mail: bboyfeiyu@gmail.com 
 * @Date 2012-11-7 下午8:17:46 
 *
 */

public class LoginActivity extends Activity {
  
	private Button loginBtn = null;			// 登录按钮
	private EditText userEdit = null;		// 用户ID输入框
	private EditText pwdEdit = null;		// 用户密码输入框
	private TextView regTextView = null;	// 注册的文本点击
	private CheckBox remPwdBox;				// 是否记住密码CheckBox
	private CheckBox autoLoginBox = null;	// 自动登录
	
	private MyHandler mHandler = null ;		// UI线程中的 Handler
	private Thread loginThread = null;		// 定义一个新线程
	public static String mineID = null;		// 用户id
	public static String mineName = null;	// 用户name
	private String password = null;			// 用户密码
	private String localIP = null;
	private MyProgressDialog mProgressDlg = null;	// 登陆时的进度条
	private final String SETTING = "usr_info";
	public static int gNoAuto = 0;			// 注销时不自动登陆
	private boolean mCancleLogin = false;	// 按返回键的时候取消登录
	private ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * <p>Title: onCreate</p> 
	 * <p>Description: </p> 
	 * @param savedInstanceState 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		// 设置为无标题模式
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.login);
        
        // 检测网络是否可用
        checkNetWorkStatus();				

		 // 获取用户名
		mineID = ((EditText)findViewById(R.id.idEdit)).getText().toString();
		Log.d("登录的用户ID ： ", mineID);
		
		// 进度条对话框
		mProgressDlg = new MyProgressDialog(this, "登录中,请稍候...");
       
        setTitle("Login");
        loginBtn = (Button)findViewById(R.id.loginBtn);
        loginBtn.setOnClickListener(new LoginButtonClick());
        
        userEdit = (EditText)findViewById(R.id.idEdit);
        pwdEdit = (EditText)findViewById(R.id.pwdEdit);
        userEdit.setText("android");
        pwdEdit.setText("android");
        
        // 是否保存密码的checkBox
        remPwdBox = (CheckBox)findViewById(R.id.remPwd);			
        autoLoginBox = (CheckBox)findViewById(R.id.autoLogin);
        
        // 从本地读取密码
        readInfoFromLocal();										
        
        // 点击注册
        regTextView = (TextView)findViewById(R.id.register);
        regTextView.setClickable( true );
        regTextView.setOnClickListener(new OnRegisterClick());
        	
        // 将当前页面添加到map中 ,用于退出整个应用程序
        MainViewActivity.addActivityToHashSet( this );
        // 注销的情况下需要再次添加到列表
        if (gNoAuto != 0){
        	MainViewActivity.addActivityToHashSet( this );
        }
        
    }
    
    
    /**
     * 
     * @ClassName: LoginButtonClick 
     * @Description:  内部类，登录按钮的响应，启动新线程，完成登录
     * @Author: xxjgood
     * @E-mail: bboyfeiyu@gmail.com 
     * @Date 2012-11-17 下午4:47:59 
     *
     */
	class LoginButtonClick implements OnClickListener {

		@Override
		public void onClick(View v) {

			// 是否取消登录的控制变量
			mCancleLogin = false;

			saveInfoToLocal(); // 保存用户密码到本地
			checkNetWorkStatus();

			if ("".equals(userEdit.getText().toString().trim())) {
				Toast.makeText(LoginActivity.this, "用户名不能为空！", 1).show();
				return;
			} else {
				mineID = userEdit.getText().toString();
			}

			if ("".equals(pwdEdit.getText().toString().trim())) {
				Toast.makeText(LoginActivity.this, "密码不能为空！", 1).show();
				return;
			} else
				password = pwdEdit.getText().toString();

			// 启动登录线程
			loginThread = new Thread( runnable ); 
			loginThread.start();
			
			// 进度条显示
			mProgressDlg.show(); 
			gNoAuto = 0;
		}

	} // end of Click
	
	
	/**
	 * Run方法,将登录信息提交到服务器
	 */
	Runnable runnable = new Runnable()						
	{
		String msg = null; 									// 要发送给主线程的String
		@Override
		public void run()
		{
			packData(); 									// 打包数据
			HttpThread h = new HttpThread(nameValuePairs,2);// 2--登录
			msg = (String)h.sendInfo(); 					// 接收服务器的返回值
			sendMessage();									// 发送消息给主线程
		}
					
		public void sendMessage(){							// 线程间数据传输
		      Looper mainLooper = Looper.getMainLooper ();	// 得到主线程loop
		      mHandler = new MyHandler(mainLooper);			// 创建主线程的handler
		      mHandler.removeMessages(0);						// 移除所有队列中的消息
		      Message m = mHandler.obtainMessage(1, 1, 1, msg);// 把消息放入message
		      mHandler .sendMessage(m);						// 发送message
		}
	};		// end of Runnable
				
			
	/**
	 * 
	 * @Method: packData 
	 * @Description: 打包要发送到服务器的数据  
	 * @throws
	 */
	void packData(){
				
		mineID = userEdit.getText().toString();
		password = pwdEdit.getText().toString();
					
		nameValuePairs.add(new BasicNameValuePair("protocol","landon"));// 封装键值对
		nameValuePairs.add(new BasicNameValuePair("id", mineID));
		nameValuePairs.add(new BasicNameValuePair("password", password));
		nameValuePairs.add(new BasicNameValuePair("ip", localIP));
	}
	

	/**
	 * 
	 * @ClassName: OnRegisterClick 
	 * @Description:   点击进入到注册界面
	 * @Author: xxjgood
	 * @E-mail: bboyfeiyu@gmail.com 
	 * @Date 2012-11-17 下午4:53:21 
	 *
	 */
	class OnRegisterClick implements OnClickListener{

		@Override
		public void onClick(View v) {
			
			Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
			startActivity(intent);
			overridePendingTransition(R.anim.slide_left, R.anim.slide_right);
		}

	}
	
	
	/**
	 * 
	 * @Method: checkNetWorkStatus 
	 * @Description: 检测网络是否可用
	 * @return   
	 * @throws
	 */
	private boolean checkNetWorkStatus() {
		boolean netSataus = false;
		ConnectivityManager cwjManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		cwjManager.getActiveNetworkInfo();

		if (cwjManager.getActiveNetworkInfo() != null) {
			netSataus = cwjManager.getActiveNetworkInfo().isAvailable();
			netSataus = true;
			
			localIP = NetInfomation.getLocalIpAddress();
	        Log.d("IP : ", localIP);
		}

		// 网络不可用,则提示设置网络
		if (netSataus == false) {
			Builder b = new AlertDialog.Builder(this).setTitle("没有可用的网络")
					.setMessage("请开启GPRS或WIFI网络连接");
			b.setPositiveButton("确认", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					// 网络不可用,则设置网络的intent
					Intent mIntent = new Intent("/");
					ComponentName comp = new ComponentName("com.android.settings",
																"com.android.settings.WirelessSettings");
					mIntent.setComponent(comp);
					mIntent.setAction("android.intent.action.VIEW");
					startActivityForResult(mIntent, 0); // 如果在设置完成后需要再次进行操作，可以重写操作代码，在这里不再重写
				}
			}).setNeutralButton("取消", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.cancel();
				}
			}).show();
		}
		return netSataus;
	}
	

	/**
	 * 
	 * @ClassName: MyHandler 
	 * @Description:  MyHandler,监听子线程发送的消息，并进行处理
	 * @Author: xxjgood
	 * @E-mail: bboyfeiyu@gmail.com 
	 * @Date 2012-11-17 下午4:54:35 
	 *
	 */
	private class MyHandler extends Handler {

		/**
		 * 
		 * @Constructor: 
		 * @param looper
		 * @Description: 构造函数
		 * @param looper
		 */
		public MyHandler(Looper looper) {
			super(looper);
		}

		/*
		 * (非 Javadoc,覆写的方法) 
		 * <p>Title: handleMessage</p> 
		 * <p>Description: </p> 
		 * @param msg 
		 * @see android.os.Handler#handleMessage(android.os.Message)
		 */
		@Override
		public void handleMessage(Message msg) {
			String result = "";
			try {
				result = msg.obj.toString(); // 接收到子线程的字符串
				result = result.trim(); // 去空格
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			// 截取服务器返回的用户登录信息
			String s2[] = result.split(";;");
			// 如果登录成功，则跳转至主界面
			if (!mCancleLogin && s2[0].equals("success")
					&& !result.contains("error")) {

				mineName = s2[1];
				Log.d("我的昵称", mineName);
				Intent intent = new Intent(LoginActivity.this, MainViewActivity.class);
				// 注意该地方是传递数据给其他的activity,获取ID号等
				startActivity(intent);
			} else if (s2[0].equals("fail")) { // 提示错误

				Toast.makeText(LoginActivity.this, "嘛事啊? 账户或者密码错误,请重新输入...", 0)
						.show();
				Animation shake = AnimationUtils.loadAnimation(
						LoginActivity.this, R.layout.shake);
				findViewById(R.id.pwdEdit).startAnimation(shake);
			} else {
				Toast.makeText(LoginActivity.this, "亲,您的网络不给力噢,元芳,你怎么看? ", 1)
						.show();
				Log.d("1", s2[0] + "");
			}

			// 登录进度框隐藏
			mProgressDlg.dismiss();
			mCancleLogin = false;
		}
	}
		
	
	/**
	 * @Method: saveInfoToLocal 
	 * @Description: 保存用户登录信息到本地 （包括记住密码、自动登录等） 
	 * @throws
	 */
	private void saveInfoToLocal() {
		boolean bAutoLogin = false;

		// 保存用户密码
		SharedPreferences ref = getSharedPreferences(SETTING, 0);
		if (remPwdBox.isChecked()) {
			String pwd = pwdEdit.getText().toString();
			Random rd = new Random();
			int num = rd.nextInt(9);
			pwd = "s" + num + pwd + "ns" + rd.nextInt(9);
			ref.edit().putString("pwd", pwd).putBoolean("rem", true).commit();

		} else {
			ref.edit().putString("pwd", "").putBoolean("rem", false).commit();
		}

		// 自动登录
		if (autoLoginBox.isChecked()) {
			bAutoLogin = true;
		}

		ref.edit().putBoolean("autoLogin", bAutoLogin) // 自动登录
				.putString("id", userEdit.getText().toString()).commit();
	}

	/**
	 * 
	 * @Method: readInfoFromLocal
	 * @Description: 功能 ： 从本地文件中读取用户数据等
	 * @throws
	 */
	private void readInfoFromLocal() {
		SharedPreferences settings = getSharedPreferences(SETTING, 0); // 获取一个对象

		String id = settings.getString("id", "");
		String pwd = settings.getString("pwd", ""); // 取出保存的密码
		if (pwd.length() >= 6) {
			pwd = pwd.substring(2, pwd.length() - 3); // 解密
			Log.d("密码读取", pwd);
		}
		boolean rem = settings.getBoolean("rem", false);
		boolean auto = settings.getBoolean("autoLogin", false);

		if (!id.equals("") && !pwd.equals("")) {
			userEdit.setText(id);
			pwdEdit.setText(pwd);
		}

		if (rem) {
			remPwdBox.setChecked(true);
		} else {
			remPwdBox.setChecked(false);
		}

		if (auto && gNoAuto == 0) // gNoAuto 为在用户点击注销时不自动登陆
		{
			autoLoginBox.setChecked(true);
			loginThread = new Thread(runnable); // 启动一个新线程
			loginThread.start();
			mProgressDlg.show(); // 进度条显示
		} else {
			autoLoginBox.setChecked(false);
		}

	}

	long exitTime = 0;

	/*
	 * (非 Javadoc,覆写的方法) 按键事件响应,在两秒内连续点击"back"键退出
	 * @Title: onKeyDown
	 * @Description:
	 * @param keyCode
	 * @param event
	 * @return
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		switch (keyCode) {

		case KeyEvent.KEYCODE_BACK: // 退出
		{
			// 取消登录
			mCancleLogin = true;
			if ((System.currentTimeMillis() - exitTime) > 2000
					&& event.getAction() == KeyEvent.ACTION_DOWN) {

				Toast.makeText(getApplicationContext(), "再按一次就退出程序了噢...",
						Toast.LENGTH_SHORT).show();
				exitTime = System.currentTimeMillis();

			} else {
				// 退出程序
				MainViewActivity.killCurrentApp(this);
			}
			// 这句很重要啊!不然会进行默认的回退到上一界面
			return true;
		}

		default:
			break;
		}

		return super.onKeyDown(keyCode, event);
	}


	/*
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onStop
	 * @Description: 页面停止的回调函数
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();

		if (mHandler != null) {
			mHandler.removeCallbacks(runnable);
		}

		if (loginThread != null) {
			loginThread.interrupt();
			loginThread = null;
		}
	}
		

	/*
	 * (非 Javadoc,覆写的方法)   建立菜单
	 * <p>Title: onCreateOptionsMenu</p> 
	 * <p>Description: </p> 
	 * @param menu
	 * @return 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		menu.add(Menu.NONE, Menu.FIRST + 1, 1, "设置").setIcon(
				android.R.drawable.ic_menu_set_as);
		menu.add(Menu.NONE, Menu.FIRST + 2, 2, "退出").setIcon(
				android.R.drawable.ic_lock_power_off);

		return true;
	}

	/*
	 * (非 Javadoc,覆写的方法) 菜单选择事件 
	 * Title: onOptionsItemSelected
	 * @Description: 菜单选择事件
	 * @param item
	 * @return
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		case Menu.FIRST + 1: // 我的位置
			Intent intent = new Intent(this, SettingActivity.class);
			startActivity(intent);
			return true;

		case Menu.FIRST + 2:
			MainViewActivity.killCurrentApp(this);
			return true;

		default:
			break;
		}
		return false;

	} // end of onOptionsItemSelected

	}