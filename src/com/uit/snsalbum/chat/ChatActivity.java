package com.uit.snsalbum.chat;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.uit.snsalbum.entry.LoginActivity;
import com.uit.snsalbum.entry.MainViewActivity;
import com.uit.snsalbum.entry.R;
import com.uit.snsalbum.network.NetInfomation;

/**
 * @ClassName: ChatActivity 
 * @Description: 聊天界面 
 * @Author: Mr.Simple (何红辉)
 * @E-mail: bboyfeiyu@gmail.com 
 * @Date 2012-11-17 下午7:20:37 
 *
 */

public class ChatActivity extends Activity implements OnClickListener{
	
	   private Button sendBtn;											// 发送按钮
	   private String mFriendIp = null;									// 好友的IP,这里测试默认设置
	   private ListView mMsgListView;									// 对话列表ListView
	   private DetailEntity mMsgEntity = null;							// 自定义的消息对象
	   private ArrayList<DetailEntity> conversationList = null;			// 对话实体列表,即消息的封装类
	   private  String mFriName;										// 好友昵称
	   
	   final String TAG = "CHATACTIVITY";
	   private final String CHAT_ACTION = "chat.SocketService.chatMessage";	// 聊天广播消息的action
	   private MyBroadcastReciver mMsgReceiver = null;					// 广播接收器
	   private Vibrator mVibrator; 										// 震动
	   private String mMsg = "";										// 要发送的消息
	   public static int gMyIcon = 0;									// 自己的头像索引

	   
		/**
		 * 功能 ： 页面创建,进行初始化
		 * (non-Javadoc)
		 * @see android.app.ActivityGroup#onCreate(android.os.Bundle)
		 */
	   @Override
	   public void onCreate(Bundle savedInstanceState) {
		   
		    super.onCreate(savedInstanceState);
		    setContentView(R.layout.bluetooth);				// 跟蓝牙聊天一样的布局.
		      
		    Intent intent = getIntent();
		    mFriName = intent.getStringExtra("friendName");		// 好友昵称
		    mFriendIp = intent.getStringExtra("friendIp");		// 好友IP
		    // 获取从推送通知栏里面传递来的消息
		    String msg = intent.getStringExtra("pendingMsg");
		    Log.d(TAG, "接到推送新消息:" + msg);    
		    
		    // 设置标题为与好友聊天
		    setTitle("与" + mFriName+ "聊天中");
		    Log.d(TAG, "好友 : "+ mFriName + " 好友 IP : " + mFriendIp);
		    
			// 如果进入到聊天界面,则设置该好友用户的消息不用mainActvity广播
	      	SocketService.mMsgMap.put(mFriName, "NO");	      	
	      	// 初始化数据成员等,包括聊天列表、适配器等
		    initActivity();								
		    
		    // 如果是从推送栏进入到该界面,则将该消息添加到好友会话列表中
		    if ( msg != null && mFriName != null ){
		    	addConversationMsg(mFriName, msg, 2);
		    }
			
			// 震动
			mVibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);     

	     
	   }	// end of onCreate().
	   
	   
	/**
	 * @Method: initActivity
	 * @Description: 初始化类成员变量、控件、适配器等
	 */
	private void initActivity() {

		sendBtn = (Button) findViewById(R.id.button_send);// 发送按钮
		sendBtn.setOnClickListener(this);

		// 广播接收器,接收好友发来的消息(通过service的广播来传递),使用动态注册
		mMsgReceiver = new MyBroadcastReciver();

		IntentFilter chatFilter = new IntentFilter(CHAT_ACTION);
		Log.d(TAG, "聊天接收器是否注册成功: " + registerReceiver(mMsgReceiver, chatFilter));

		// 消息列表初始化
		conversationList = new ArrayList<DetailEntity>();
		// 初始化好友聊天的信息记录列表
		mMsgListView = (ListView) findViewById(R.id.conversationList);

		// 获取当前布局
		final LinearLayout layout = (LinearLayout) findViewById(R.id.bluetoothLayout);
		MainViewActivity.checkSkin(layout); // 设置背景,即程序皮肤
		MainViewActivity.addActivityToHashSet(this); // 将当前页面添加到activity map中

	}

	/**
	 * 功能 ： 按钮点击事件 (non-Javadoc)
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 * 
	 */
	@Override
	public void onClick(View v) {

		// 获取输入框中的消息文本
		EditText msgEdit = (EditText) findViewById(R.id.edit_text_out);
		mMsg = msgEdit.getText().toString();

		if (mMsg.trim().length() == 0) {
			Toast.makeText(ChatActivity.this, "不能发送空消息,请重新输入", 0).show();
			return;
		}

		msgEdit.setText(""); // 清空输入框
		Log.d(TAG, "要发送的消息 ： " + mMsg);
		addConversationMsg("我说 : ", mMsg, 1); // 将该条消息添加到列表中
		try {
			// 将消息通过socket发送出去
			//sendPost( mMsg );
			sendMsgUDP() ;
		} catch (Exception e) {
			Toast.makeText(this, "发送失败,服务器异常~~~", 0).show();
			
		}

	}

	 
	/**
	 * @Method: sendMsgUDP
	 * @Description: 发送消息
	 */
	private void sendMsgUDP() {
		Log.d(TAG, "好友IP : " + mFriendIp);
		new Thread() {
			@Override
			public void run() {
				try {
					String myIP = NetInfomation.getLocalIpAddress();
					// 设置远程服务器的IP与地址
					InetAddress iaddr = InetAddress.getByName("199.36.75.40");
					// 封装消息pakcet
					mMsg = "CHAT_MSG" + ";;" + mFriendIp + ";;" + myIP + ";;"
							+ LoginActivity.mineName + ";;" + mMsg;
					byte data[] = mMsg.getBytes("GB2312");
					// 创建一个DatagramPacket对象，并指定要讲这个数据包发送到网络当中的哪个地址，以及端口号
					DatagramPacket packet = new DatagramPacket(data,
							data.length, iaddr, 9876);
					
					// 调用socket对象的send方法，发送数据
					SocketService.mUdpRevSocket.send( packet ) ;
					Log.d("UDP", " Packet已经发送 ： " + mMsg);

				} catch (Exception e) {
					Toast.makeText(ChatActivity.this, "发送超时,请检查网络...", 0)
							.show();
					e.printStackTrace();
				}
			}
		}.start();

	}
	
	
	/**
	 * 功能： socket消息处理,接收好友消息并且更新UI线程
	 */
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0: // 我的消息
				addConversationMsg("我说 ", (msg.obj).toString(), 1);
				break;
			case 1: // 好友的消息
				addConversationMsg(mFriName, (msg.obj).toString(), 2);
				break;
			default:
				break;

			} // end of switch()

		}// end of handleMessage(Message msg)
	};
		   
	   
	/**
	 * @Method: addConversationMsg
	 * @Description:
	 * @param fname
	 *            好友昵称
	 * @param content
	 *            好友昵称
	 * @param layoutId
	 *            使用哪个布局 (1表示自己发送消息的显示布局,2表示为好友发来的消息布局)
	 * @return void 返回类型
	 * @throws
	 */
	private void addConversationMsg(String fname, String content, int layoutId) {
		// 在这里获取系统时间
		SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm:ss");
		Date curDate = new Date(System.currentTimeMillis()); // 获取当前时间
		String date = formatter.format(curDate);

		// 自己发出去的消息
		if (layoutId == 1) {
			// 添加消息对话实体对象
			mMsgEntity = new DetailEntity(fname, date, content,
					R.layout.list_say_me_item);
		} else {
			// 添加消息对话实体对象, 好友发来的消息,使用不同的显示方式
			mMsgEntity = new DetailEntity(fname, date, content,
					R.layout.list_say_he_item);
		}

		// 将内容实体添加到List<DetailEntity>中
		conversationList.add( mMsgEntity );
		// 设置适配器
		mMsgListView.setAdapter(new DetailAdapter(ChatActivity.this,
				conversationList));

	}

	
	/**
	 * @ClassName: MyBroadcastReciver 
	 * @Description: 广播接收器, 接收socket服务发来的消息,并且将消息添加到气泡列表 
	 * @Author: Mr.Simple 
	 * @E-mail: bboyfeiyu@gmail.com 
	 * @Date 2012-11-9 下午1:37:03 
	 *
	 */
	private class MyBroadcastReciver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();
			// 聊天消息
			if (action.equals(CHAT_ACTION)) {
				Log.d(TAG, "聊天软件接收到广播") ;
				// 获得广播的消息,即接收到的消息
				String msg = intent.getStringExtra("broadCast");
				String fName = ""; // 好友昵称
				String newMsg = ""; // 新消息
				
				if ( msg.contains(";;") ){
					Log.d(TAG, "好友列表接到的数据为" + msg) ;
					try{
						// 获取数据
						String[] cont = msg.split(";;");
						mFriendIp = cont[1];
						fName = cont[2];
						newMsg = cont[3];
						
						long[] pattern = { 200, 500, 100, 200 }; // 根据指定的模式进行震动
						mVibrator.vibrate(pattern, 2); 			// -1不重复，非-1为从pattern的指定下标开始重复
						mHandler.postDelayed(vibRunnable, 1100);
						addConversationMsg(fName, newMsg, 2); // 将消息添加到气泡列表
					}catch(Exception e){
						Log.d(TAG, "**消息处理出错**") ;
						e.printStackTrace() ;
					}
				}
			} // end of if
		} // enf of onReceive

	} // end of MyBroadcastReciver
	  
	
	/**
	 * 功能 ： 取消消息震动
	 */
	Runnable vibRunnable = new Runnable() {
			
		@Override
		public void run() {
			mVibrator.cancel();
		}
	};
		
	
	/**
	 * 功能 ： 页面停止回调函数
	 */
	@Override
	protected void onStop() {
		
		SocketService.mMsgMap.clear();	
		// 将本页面从Set中移除
		MainViewActivity.removeFromSet( this );
		super.onStop();
	}


	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onDestroy
	 * @Description:  
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		if (mMsgReceiver != null){
			unregisterReceiver(mMsgReceiver);
		}
		super.onDestroy();
	}

}	// end of class
