package chat;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;


/**
 * 
 * @ClassName: SocketService 
 * @Description: Socket服务类,在后台发送与接收、广播消息
 * @Author: Mr.Simple (何红辉)
 * @E-mail: bboyfeiyu@gmail.com 
 * @Date 2012-11-17 下午5:02:12 
 *
 */

public class SocketService extends Service {

	private int mStartId = 0;
	private final IBinder mBinder = new LocalBinder();  
	
	public static HashMap<String, String> mMsgMap = new HashMap<String, String>();
	
	public static UdpReceiveThread mUdpRevThread = null;	// UDP接收线程,后台监听ＵＤＰ的数据包

	public static DatagramSocket mUdpRevSocket = null;		// UDP SOCKET，用于后台监听数据
	public static boolean mUdpStop = false;					// socket监听是否停止的标识位
	private final String TAG = "SocketService";
	private final int SOCKET_PORT = 8765;


	/*
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onCreate
	 * @Description:  服务创建，只执行一次
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		
		Log.d("", "Service创建");

		initUdpSocket();						// 初始化socket,用户接收消息

	}


	/*
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onStart
	 * @Description: 服务启动
	 * @param intent
	 * @param startId 
	 * @see android.app.Service#onStart(android.content.Intent, int)
	 */
	@Override
	public void onStart(Intent intent, int startId) {
		
		Log.d(TAG, " 启动服务 -----> id : " + startId);
		
		super.onStart(intent, startId);
	}

	
	/*
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onDestroy
	 * @Description:  服务销毁
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		
		Log.d(TAG, "Service销毁");
		
		mUdpRevThread.interrupt();
		mUdpRevThread = null;
		
		this.stopSelf();							// 关闭服务

	}

	
	/*
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onBind
	 * @Description: 服务绑定
	 * @param arg0
	 * @return 
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		
		Log.d(TAG,"绑定服务");
		return mBinder;
	}

	
	/*
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onRebind
	 * @Description: 服务重新绑定
	 * @param intent 
	 * @see android.app.Service#onRebind(android.content.Intent)
	 */
	@Override
	public void onRebind(Intent intent) {
		
		Log.d(TAG, "重新绑定");
		super.onRebind(intent);
	}
	
	
	/*
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onStartCommand
	 * @Description:  执行命令
	 * @param intent
	 * @param flags
	 * @param startId
	 * @return 
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		mStartId = startId;
		return START_STICKY;
	}


	/*
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onUnbind
	 * @Description:  解除绑定
	 * @param intent
	 * @return 
	 * @see android.app.Service#onUnbind(android.content.Intent)
	 */
	@Override
	public boolean onUnbind(Intent intent) {
		
		Log.d(TAG, "解除绑定");
		return super.onUnbind(intent);
	}
	

	/**
	 * @Method: initUdpSocket 
	 * @Description: Socket的初始化  
	 * @throws
	 */
	private void initUdpSocket()
	{
		try {
			// 端口为SOCKET_PORT=8765
			mUdpRevSocket = new DatagramSocket( SOCKET_PORT );
			Log.d("", "用于UdpSocket初始化 " );
			
			if (mUdpRevSocket != null)
			{
				mUdpRevThread = new UdpReceiveThread();
				mUdpRevThread.start();
			}
			
		} catch (SocketException e) {	
			Log.d(TAG, "**socket初始化失败") ;
			e.printStackTrace();
		}
	}
	
	   
	/**
     * @ClassName: UdpReceiveThread 
     * @Description: UDP消息的接收线程 
     * @Author: Mr.Simple 
     * @E-mail: bboyfeiyu@gmail.com 
     * @Date 2012-11-9 下午1:35:31 
     *
     */
	public class UdpReceiveThread extends Thread {
		public UdpReceiveThread() {

		}

		@Override
		public void run() {
	
			while ( !mUdpStop ) {
				Log.d(TAG, "Udp接收线程已经启动");
				try {

					byte[] buf = new byte[1024]; // 指定最大接受信息的大小
					DatagramPacket datagramPacket = new DatagramPacket(buf, 1024);

					Log.d(TAG, "Service Udp消息等待中...(阻塞)");
					mUdpRevSocket.receive(datagramPacket);
					Log.d(TAG, "****Udp消息已接收数据****");

					// 从packet中读取消息,并且构造成一个字符串
					String msg = new String(datagramPacket.getData(), 0,
											datagramPacket.getLength(), "GB2312");
					Log.d(TAG, "Udp消息: " + msg);
					// 广播udp消息
					broadCastMsg( msg );

				} catch (Exception e) {

					e.printStackTrace();
				}

			}
		}
	} // end of UdpReceiveThread

	

	/**
	 * @Method: stopMyService 
	 * @Description:  自定义停止服务函数  
	 * @throws
	 */
	public void stopMyService() {

		Log.d(TAG, "停止服务 --->  id : " + mStartId);

		this.stopSelf(mStartId);
	}
	

	/**
	 * @Method: broadCastMsg 
	 * @Description:  广播消息,向UI线程中广播消息
	 * @param msg   
	 * @throws
	 */
	private void broadCastMsg(String msg)
	{
		// 默认的消息类型
		String action = "chat.SocketService.chatMessage";
		if( msg == null || "".equals( msg ) ){
			Log.d(TAG, "不能广播空消息");
			return ;
		}
				
		// 含有TEXI标志则表示这是更新地图上的关于出租车的消息
		if ( msg.contains("TEXI")){
			action = "chat.SocketService.texi";
			
		} else if ( msg.contains("CHAT_MSG") ){
			// 聊天消息的广播
			Log.d("聊天服务接到CHAT_MSG", msg);
		}

		// 根据不同的动作实例化Intent
		Intent intent = new Intent( action );
		intent.putExtra("broadCast", msg);
		// 发送广播
		sendBroadcast( intent );
	}
	
	
	/**
	 * @ClassName: LocalBinder 
	 * @Description: 用于客户端Binder的类．因为我们我们知道这个,service永远运行在与客户端相同的进程中，所以我们不需要处理IPC
	 * @Author: Mr.Simple 
	 * @E-mail: bboyfeiyu@gmail.com 
	 * @Date 2012-11-7 下午8:19:36 
	 *
	 */
    public class LocalBinder extends Binder {  
    	
    	public SocketService getService() {  
            // 返回本service的实例到客户端，于是客户端可以调用本service的公开方法  
            return SocketService.this;  
        }  
    } 
    
}	// END OF FILE
