package com.uit.snsalbum.entry;

import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.uit.snsalbum.R;
import com.uit.snsalbum.network.HttpThread;

/**
 * 
 * @ClassName: RegisterActivity
 * @Description:
 * @Author: Mr.Simple (何红辉) & xxjgood
 * @E-mail: bboyfeiyu@gmail.com
 * @Date 2012-11-17 下午5:40:21
 *
 */

public class RegisterActivity extends Activity {

	private MyHandler mHandler = null; // UI线程中的 Handler
	private Thread regThread = null; // 定义一个新线程
	private Button regBtn = null; // 注册按钮
	private MyProgressDialog mProgressDlg = null; // 登陆时的进度条

	/**
	 * (非 Javadoc,覆写的方法)
	 * 
	 * @Title: onCreate
	 * @Description:
	 * @param savedInstanceState
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.register);

		regBtn = (Button) findViewById(R.id.eregisterbutton);
		regBtn.setOnClickListener(new RegisterButtonClick()); // 设置按钮监听

		// 进度条对话框初始化
		mProgressDlg = new MyProgressDialog(this, "注册中,请稍后...");
		setTitle("注册");
	}

	/**
	 * 
	 * @ClassName: MyHandler
	 * @Description: 内部类 MyHandler，监听注册成功与否
	 * @Author: Mr.Simple (何红辉)
	 * @E-mail: bboyfeiyu@gmail.com
	 * @Date 2012-11-17 下午5:40:45
	 *
	 */
	private class MyHandler extends Handler {
		public MyHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) { // 处理消息
			String s1 = "";
			try {
				s1 = msg.obj.toString();
				s1 = s1.trim();
			} catch (Exception e) {

			}
			// 去空格
			if (s1.equals("success")) { // 如果成功，结束这个Activity
				Toast.makeText(RegisterActivity.this, "恭喜您，注册成功！", 0).show();
				mProgressDlg.dismiss();
				finish();
			} else {
				Log.d("1", s1);
				Toast.makeText(RegisterActivity.this, "对不起，注册失败！", 0).show();
				mProgressDlg.dismiss();
			}
		}
	}

	/**
	 * @ClassName: RegisterButtonClick
	 * @Description: 内部类，注册按钮响应
	 * @Author: Mr.Simple (何红辉)
	 * @E-mail: bboyfeiyu@gmail.com
	 * @Date 2012-11-17 下午5:41:33
	 *
	 */
	class RegisterButtonClick implements OnClickListener {

		private ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		private boolean isPacked = false;

		@Override
		public void onClick(View v) { // 按键响应

			isPacked = packData(); // 打包数据
			if (isPacked) {
				mProgressDlg.show(); // 进度条对话框显示
				regThread = new Thread(runnable); // 启动一个新线程
				regThread.start();
			} else {
				displayToast("抱歉,您的提交失败,请核查注册信息...");
			}
		}

		Runnable runnable = new Runnable() // Runable
		{
			String msgToSend = null; // 发送给主线程的String

			@Override
			public void run() {
				Looper.prepare();

				HttpThread h = new HttpThread(nameValuePairs, 1); // 协议 1--注册
				msgToSend = (String) h.executeRequest(); // 向服务器发送请求，并返回数据
				sendMessage(); // 将子线程的数据发送到主线程

				Looper.loop();
			}

			public void sendMessage() { // 线程间数据传输

				Looper mainLooper = Looper.getMainLooper(); // 得到主线程loop
				String msg;

				mHandler = new MyHandler(mainLooper); // 创建主线程的handler
				msg = msgToSend;
				mHandler.removeMessages(0); // 移除所有队列中的消息
				Message m = mHandler.obtainMessage(1, 1, 1, msg); // 把消息放入message
				mHandler.sendMessage(m); // 发送message
			}
		};

		/**
		 * @Method: packData
		 * @Description: 打包要提交的注册信息
		 * @return
		 */
		private boolean packData() {
			boolean flag = true;
			Log.d("", "数据打包");

			String id = ((EditText) findViewById(R.id.eid)).getText()
					.toString();
			String name = ((EditText) findViewById(R.id.ename)).getText()
					.toString(); // 用户昵称
			String password = ((EditText) findViewById(R.id.epassword))
					.getText().toString(); // 用户密码
			String pwd2 = ((EditText) findViewById(R.id.epwd2)).getText()
					.toString(); // 确认密码
			String email = ((EditText) findViewById(R.id.email)).getText()
					.toString(); // 邮件

			Log.d("打包数据 : ", id + " pwd : " + password + " Name : " + name
					+ pwd2 + email);

			if (id.length() == 0) {
				displayToast("用户名不能为空,请输入名...");
				flag = false;
			}
			if (pwd2.trim().length() == 0 || password.length() == 0) {
				displayToast("密码不能为空,请输入密码...");
				flag = false;
			}
			if (!password.equals(pwd2)) {
				displayToast("两次密码输入不匹配,请重新输入...");
				flag = false;
			}
			if (name.length() == 0) {
				displayToast("昵称不能为空，请输入昵称...");
				flag = false;
			}

			nameValuePairs.add(new BasicNameValuePair("protocol", "regist"));// 封装键值对
			nameValuePairs.add(new BasicNameValuePair("id", id));
			nameValuePairs.add(new BasicNameValuePair("password", password));
			nameValuePairs.add(new BasicNameValuePair("name", name));
			nameValuePairs.add(new BasicNameValuePair("email", email));
			return flag;
		}
	}

	/**
	 * @Method: displayToast
	 * @Description: 显示Toast消息
	 * @param tips
	 */
	private void displayToast(String tips) {
		Toast.makeText(RegisterActivity.this, tips, 0).show();
	}

}
