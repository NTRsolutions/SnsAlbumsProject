package chat;

import album.entry.MainViewActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Copyright (c) 2012,UIT-ESPACE( TEAM: UIT-GEEK)
 * All rights reserved.
 *
 * @Title: PhoneReceiver.java 
 * @Package: chat 
 * @Author: 何红辉(Mr.Simple) 
 * @E-mail:bboyfeiyu@gmail.com
 * @Version: V1.0
 * @Date：2012-11-18 下午5:08:37
 * @Description:  电话监听器,有电话打进来则直接关掉程序.避免出问题
 *
 */

public class PhoneReceiver extends BroadcastReceiver {
	 
	private Context mContext = null;
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onReceive
	 * @Description: 
	 * @param context
	 * @param intent 
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	 @Override
	 public void onReceive(Context context, Intent intent) {
		 
		System.out.println("action" + intent.getAction());
		mContext = context ;
		
		if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
			Toast.makeText(context, "电话拨出", 0).show();
			Log.d("电话打出去", "关闭程序") ;
			// 退出程序
			MainViewActivity.killCurrentApp(context);
		} else {
			// 查了下android文档，貌似没有专门用于接收来电的action,所以，非去电即来电
			TelephonyManager tm = (TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE);
			// 设置监听器
			tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
			Log.d("电话打进来了", "关闭程序") ;
			// 退出程序
			MainViewActivity.killCurrentApp(context);

		}
	}
	 
	 /**
	  * 电话状态监听器
	  */
	 PhoneStateListener listener = new PhoneStateListener(){
	 
	  @Override
	  public void onCallStateChanged(int state, String incomingNumber) {
	   //state 当前状态 incomingNumber,貌似没有去电的API
	   super.onCallStateChanged(state, incomingNumber);
	   switch( state ){
    	   case TelephonyManager.CALL_STATE_IDLE:
    		   System.out.println("挂断");
    		   break;
    	   case TelephonyManager.CALL_STATE_OFFHOOK:
    		   System.out.println("接听");
    		   break;
    	   case TelephonyManager.CALL_STATE_RINGING:
    		   System.out.println("响铃:来电号码"+incomingNumber);
    		   MainViewActivity.killCurrentApp( mContext ) ;
    		   Log.d("来点", "程序关闭") ;
    		   break;
    	    default:
    	    	break;
	   }
	   
	  } // enf of onCallStateChanged
	 
	 };
	}
