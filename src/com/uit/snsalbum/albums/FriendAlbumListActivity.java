package com.uit.snsalbum.albums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.uit.snsalbum.entry.MyProgressDialog;
import com.uit.snsalbum.entry.R;
import com.uit.snsalbum.network.HttpThread;

/**
 * Copyright (c) 2012,UIT-ESPACE( TEAM: UIT-GEEK)
 * All rights reserved.
 *
 * @Title: FriendAlbumListActivity.java 
 * @Package albums 
 * @Author 徐晓佳(Mr.Abert) 
 * @E-mail:uit_xuxiaojia@163.com
 * @Version V1.0
 * @Date：2012-11-13 下午8:34:45
 * @Description:
 *
 */

public class FriendAlbumListActivity extends Activity{

	private String userID = null;
	private ListView albumListView = null;
	private SimpleAdapter adapter = null;
	private List<Map<String, Object>> albumsList= new ArrayList<Map<String, Object>>();
	private Thread tAlbum = null;
	private MyHandler mHandler = null;
	private MyProgressDialog loadingDialog ;	// 更新网络相册时的进度条显示框
	private String[] albumArray;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		userID = getIntent().getStringExtra("id");
		setContentView(R.layout.friendalbumlist);
		
		loadingDialog = new MyProgressDialog(this,
    			"相册加载中，请稍候···") ;					// 初始化更新相册列表时的进度条显示框	
		
		
		// userName: 用户名  ,   Name ：昵称   , img : 用户头像
		adapter = new SimpleAdapter(this, albumsList, R.layout.insidelistview,
				new String[]{"albumName","picnum","date","img1"},				// 适配器格式 
				new int[]{R.id.userName2,R.id.ipInfo2,R.id.ipInfo3,R.id.userImg2});		// 将上一个参数的数据对应赋给这个参数的三个变量
		albumListView = (ListView) findViewById(R.id.friendalbumlistview);
		albumListView.setAdapter( adapter );
		
		
		tAlbum = new Thread(rAlbum);
		tAlbum.start();
	}
	
	
	
	/**
	 * 功能：HTTP请求网络相册
	 */
	Runnable rAlbum = new Runnable()								
	{
		String msg;
		@Override
		public void run()
		{
            ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("protocol","getShareAlbum"));
            nameValuePairs.add(new BasicNameValuePair("id", userID));
			HttpThread h = new HttpThread(nameValuePairs,11);	// 11--请求相册列表
			msg = h.sendInfo().toString(); 						// 接收服务器的返回值
			Log.d("请求网络相册列表", msg);
			sendMessage();
		}
		
		public void sendMessage(){								// 线程间数据传输
			Looper mainLooper = Looper.getMainLooper ();		// 得到主线程loop
            mHandler = new MyHandler(mainLooper);				// 创建主线程的handler
            mHandler.removeMessages(0);						// 移除所有队列中的消息
            Message m = mHandler.obtainMessage(1, 1, 1, msg);	// 把消息放入message
            mHandler.sendMessage(m);							// 发送message
		}
	};
	private class MyHandler extends Handler{       
        public MyHandler(Looper looper){
               super (looper);
        }
        @Override
        public void handleMessage(Message msg) {
        	String s = msg.obj.toString();
        	s = s.trim();	
        	try{
        		albumArray = s.split(";;");
        	}catch(Exception e){
        		Toast.makeText(FriendAlbumListActivity.this, "主人,网络列表请求错误~~~", 0).show();
        	}
        	
        	loadingDialog.dismiss();
        	// 载入新的数据
        	OnlineAlbum();
        }            
	}
	

	/**
	 * @Method: OnlineAlbum
	 * @Description:  网络相册载入以及列表事件等
	 */
	void OnlineAlbum(){

		int n = albumArray.length;
		
		if(albumArray[0].equals("fail"))
		{
			setTitle("没有连接服务器");
		}
		else
		{
			setTitle(albumArray[0]);
			if(n == 1)
				Toast.makeText(FriendAlbumListActivity.this, "您的好友的相册为空~~~", 1).show();
				
		}
		
		// 首先清空数据列表,再重新载入列表
		albumsList.clear();
		// 将获取到的相册列表加入到ListView中
		for(int i = 1; i < n; i=i+2){
			addFolder(albumArray[i], albumArray[i+1],true);
		}
		adapter.notifyDataSetChanged();
		
		loadListAnimation(true);
		
		albumListView.setOnItemClickListener(new OnItemClickListener() {  
	        @Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,  long arg3) {
	        	
	        	Log.d("3","arg2=" + arg2);
	        	Intent intent = new Intent(FriendAlbumListActivity.this, InPhotoAlbumActivity.class);
	        	intent.putExtra("id", userID);
	    		intent.putExtra("username",  albumArray[0]);
	    		intent.putExtra("albumname", albumArray[arg2*2 + 1]);
	    		intent.putExtra("num", albumArray[arg2*2 + 2]);  		
	    		startActivity(intent);
	        }  
	    });
		
	}
	

	/**
	 * @Method: addFolder
	 * @Description:  添加一个相册到列表
	 * @param albumName  相册名
	 * @param picnum     照片张数
	 * @param netOrLacal 网络或者本地相册
	 */
	void addFolder(String albumName, String picnum,Boolean netOrLacal)
	{
		int[] imgArray = {	R.drawable.ablum_01, R.drawable.ablum_03, 
				R.drawable.ablum_05, R.drawable.pictures, R.drawable.folder}; 

		// 产生随机数,获取随机图像
		Random imgIndex = new Random();
		int index = imgIndex.nextInt(4);
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("albumName", albumName);
		map.put("picnum", picnum + "张");
		map.put("img1", imgArray[index]);
		//map.put("share", value);
		albumsList.add( map );
		adapter.notifyDataSetChanged();
	} 
	
	
	/**
	 * @Method: loadListAnimation
	 * @Description:   设置ListView显示动画
	 * @param NetOrLocal
	 */
	private void loadListAnimation(boolean NetOrLocal){
		// 动画集
        AnimationSet set = new AnimationSet(true);

        // Alpha动画,Duration设置为150毫秒
        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(150);
        // 添加到动画集中
        set.addAnimation(animation);

        // 设置转移动画
        animation = new TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, -1.0f,Animation.RELATIVE_TO_SELF, 0.0f
        );
        animation.setDuration(300);
        // 添加到动画集中
        set.addAnimation(animation);

        // 设置布局动画控制器,0.5f为延时时间
        LayoutAnimationController controller = new LayoutAnimationController(set, 0.5f);    
        // 在ListView上设置动画
        albumListView.setLayoutAnimation(controller);
	}
	
}

