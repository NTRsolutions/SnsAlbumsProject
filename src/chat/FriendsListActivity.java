package chat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import map.BaiduMapActivity;
import network.HttpThread;
import network.NetInfomation;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import album.entry.LoginActivity;
import album.entry.MainViewActivity;
import album.entry.MyProgressDialog;
import album.entry.R;
import albums.FriendAlbumListActivity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

/*
* Copyright (c) 2012,UIT-ESPACE
* All rights reserved.
*
* 文件名称：FriendsListActivity.java
* 摘 要：好友列表
*  1.好友列表的实现 : 使用的是ListView 和 SimpleAdapter.
*  2.进入该界面,当点击某个项时向服务器请求该用户的IP地址.本机获取该用户IP后使用Socket进行点对点通信.
*  3.后台监听好友发送来的消息
* 
* 当前版本：1.1
* 作 者：何红辉
* 完成日期：2012年11月3日
*
* 取代版本：1.0
* 原作者 ：何红辉
* 完成日期：2012年7月20日
*/

/**
 * 
 * @ClassName: FriendsListActivity 
 * @Description: 好友列表,当点击某个项时进入与该好友的聊天界面.
 * @Author: Mr.Simple (何红辉)
 * @E-mail: bboyfeiyu@gmail.com 
 * @Date 2012-11-17 下午5:07:00 
 *
 */
public class FriendsListActivity extends ListActivity implements OnTouchListener,
									android.view.GestureDetector.OnGestureListener,OnScrollListener{
	
	// 保存好友列表的用户头像、id、昵称等的链表
	private ArrayList<Map<String, Object>> mFriendsList = new ArrayList<Map<String, Object>>();
	
	private SimpleAdapter mAdapter;						// 适配器
	//private String array[];								// 好友IP列表
	private MyProgressDialog loadingDialog = null;		// 载入时的进度条
	private GestureDetector mGestureDetector = null;	// 手势触摸探测器
	private MyBroadcastReciver mDataReceiver = null;

	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onCreate
	 * @Description:   页面创建,控件和变量初始化
	 * @param savedInstanceState 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle("好友列表");
		
		//	好友列表的适配器  userId: 用户名 、  Name ：昵称   、  img : 用户头像	 
		mAdapter = new SimpleAdapter(this, mFriendsList , R.layout.friendslist,
				new String[]{"userId","Name","img"},				//	适配器格式 
				new int[]{R.id.userName,R.id.ipInfo,R.id.userImg});	// 将上一个参数的数据对应赋给这个参数的三个变量
		setListAdapter( mAdapter );
		// 设置动画显示ListView
		loadListAnimation();
		// 设定上下文菜单的ListView
		registerForContextMenu(getListView());	
		// 获取好友列表时的进度条窗口
		loadingDialog = new MyProgressDialog(this, "好友信息加载中···");
		// 手势识别
		mGestureDetector = new GestureDetector(this);    		
		
		// 清空在线的好友列表
		mFriendsList.clear();
		mAdapter.notifyDataSetChanged();
		try {
			// 从主界面的好友列表中添加
			addFriendFromList();
		} catch (Exception e1) {
			loadingDialog.show();
			getAllUsersInfo();
		}
		
		MainViewActivity.addActivityToHashSet( this );	// 将当前页面添加到activitymap中
		
		// 广播接收器,接收好友上线的的消息(通过service的广播来传递的);
		mDataReceiver = new MyBroadcastReciver();
		IntentFilter intentFilter = new IntentFilter("chat.SocketService.onlie");
		Log.d("TAG", "好友上线接收器注册结果 ： " + registerReceiver(mDataReceiver,  intentFilter) );
		
	}
	
	/**
	 * 
	 * @Method: loadListAnimation 
	 * @Description: 设置ListView显示动画  
	 * @return void  返回类型 
	 * @throws
	 */
	private void loadListAnimation(){
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
        // 在ListView上设置动画,设置的是动画控制器
        ListView listView = getListView();       
        // 设置背景色
        listView.setCacheColorHint(Color.TRANSPARENT);
        // 设置列表的背景,要设置图像背景则要把CacheColorHint设置为透明
        listView.setBackgroundResource(R.drawable.frilist);
        listView.setLayoutAnimation(controller);
        listView.setOnTouchListener(this);
        
	}
	

	/**
	 * @Method: addFriendFromList 
	 * @Description:  从MainViewActivity界面加载好友列表,存储了好友列表
	 * @throws Exception   
	 * @throws
	 */
	private void addFriendFromList() throws Exception {
		// Main中没有获取到好友列表的话直接自动获取
		if (MainViewActivity.sFriendList.size() == 0) {
			throw new ArrayIndexOutOfBoundsException();
		}

		// 清空List以及通知Adapter数据集改变
		mFriendsList.clear();
		mAdapter.notifyDataSetChanged();
		Log.d("划动更新", "更新列表");
		for (int i = 0; i < MainViewActivity.sFriendList.size(); i += 3) {

			// 向好友列表添加好友
			addFriend(MainViewActivity.sFriendList.get(i + 1),
					MainViewActivity.sFriendList.get(i),
					MainViewActivity.sFriendList.get(i + 2));
			Log.d("LIST", "从列表中载入好友列表");
		}

	}
	

	/**
	 * @Method: addFriend 
	 * @Description:  有新的用户上线时添加用户到列表 
	 * @param Name    用户昵称
	 * @param userId  用户ID
	 * @param ip	  用户的IP
	 * @throws Exception   
	 */
	private void addFriend(String Name,String userId, String ip) throws Exception
	{
		if (LoginActivity.mineName.equals(Name) || LoginActivity.mineID.equals(userId))
		{
			return ;
		}
		
		int[] imgArray = {	R.drawable.bad_smile_96,R.drawable.laugh_96,
							R.drawable.fire_96,R.drawable.money_96,
							R.drawable.grimace_96,R.drawable.girl_96,
							R.drawable.face_96, R.drawable.o_96
						 }; 
		
		// 产生随机数,获取随机图像
		Random imgIndex = new Random();
		int index = imgIndex.nextInt(7);
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("userId", userId);
		map.put("Name", Name);		// 昵称			
		map.put("img", imgArray[index]);
		map.put("ip", ip);
		
		mFriendsList.add( map );
		mAdapter.notifyDataSetChanged();
		
	}
	
	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onListItemClick
	 * @Description:  好友列表点击监听函数,点击某个好友进入聊天界面
	 * @param l
	 * @param v
	 * @param position
	 * @param id 
	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
			Log.d("my IP :", NetInfomation.getLocalIpAddress());
			
			String name = mFriendsList.get(position).get("Name").toString();			// 昵称
			String ip = mFriendsList.get(position).get("ip").toString();				// IP
			
			Intent intent = new Intent(this, ChatActivity.class);
			intent.putExtra("friendName", name);
		    intent.putExtra("friendIp", ip);
		    
		    Log.d("点击列表中的", " 好友昵称 : " + name + ",IP : " + ip);
		    startActivity(intent);
		    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
	}
	
	

	/**
	 * @Method: getAllUsersInfo 函数
	 * @Description:  向服务器发起请求,获取所有好友的列表
	 */
	private void getAllUsersInfo()
	{
		new Thread( new Runnable() {
			
			@Override
			public void run() {
				
				ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				nameValuePairs.add(new BasicNameValuePair("protocol","getIP"));// 封装键值对
				HttpThread h = new HttpThread(nameValuePairs,7);				// 获取7 好友列表
				String msg = (String)h.sendInfo(); 								// 接收服务器的返回值
				
				Looper mainLooper = Looper.getMainLooper ();					// 得到主线程loop
	            mHandler = new MyHandler(mainLooper);							// 创建主线程的handler
	            mHandler.removeMessages(0);										// 移除所有队列中的消息
	            Message m = mHandler.obtainMessage(1, 1, 1, msg);				// 把消息放入message
	            mHandler.sendMessage(m);
			}           
		}).start();
	}

	
	/**
	 *  接收服务器返回的在线好友数据,并且显示在好友列表上
	 */
	private Handler mHandler = new Handler();

	/**
	 * 
	 * @ClassName: MyHandler 
	 * @Description:  接收服务器返回的在线好友数据,并且显示在好友列表上
	 * @Author: XXJGOOD
	 * @E-mail: bboyfeiyu@gmail.com 
	 * @Date 2012-11-17 下午5:11:33 
	 *
	 */
	private class MyHandler extends Handler {

		public MyHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {

			String array[] = null;
			String s1 = msg.obj.toString(); // 接收到子线程的字符串
			s1 = s1.trim(); // 去空格

			if (s1.contains("error")) {
				Toast.makeText(FriendsListActivity.this,
								"好友列表加载失败,本机网络或者服务器异常...", 1).show();
				// 重新获取好友列表
				getAllUsersInfo() ;
				return;
			} 

			loadingDialog.dismiss();
			try {
				// 拆分字符串,获得有效数据的字符串数组
				array = s1.split(";;");
				int len = array.length;
				if ( len > 0 ){
					mFriendsList.clear();
					mAdapter.notifyDataSetChanged();
				}
				for (int i = 0; i < len; i += 3) {
					// 向好友列表添加好友
					addFriend(array[i + 1], array[i], array[i + 2]);
					// 将数据存到MainViewActivity中
					MainViewActivity.sFriendList.add(array[i + 1]);
					MainViewActivity.sFriendList.add(array[i]);
					MainViewActivity.sFriendList.add(array[i + 2]);

				}
			} catch (Exception e) {
				Toast.makeText(FriendsListActivity.this, "抱歉,好友列表获取失败~~~", 0).show();
			}
		}

	}
	

	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onCreateOptionsMenu
	 * @Description:  刷新菜单
	 * @param menu
	 * @return 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
 	@Override
     public boolean onCreateOptionsMenu(Menu menu) {

         // setIcon()方法为菜单设置图标，这里使用的是系统自带的图标
 		menu.add(Menu.NONE, Menu.FIRST + 1, 1, "刷新").setIcon(
 				android.R.drawable.ic_menu_send);
        
 		return true;
     }
 	
 	
 	/**
 	 *  功能 ： 刷新菜单选择事件
 	 *  (non-Javadoc)
 	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
 	 */
 	@Override
     public boolean onOptionsItemSelected(MenuItem item) {
         switch (item.getItemId()) {
	         case Menu.FIRST + 1:		// 我的位置
	        	 loadingDialog.show();
        	 	 getAllUsersInfo();
        	 	 break;	  
	        
	        default:
	         		break;

         }
         return false;

     }		// end of onOptionsItemSelected


 	/**
 	 * (非 Javadoc,覆写的方法) 
 	 * @Title: onCreateContextMenu
 	 * @Description:   创建上下文菜单
 	 * @param menu
 	 * @param v
 	 * @param menuInfo 
 	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
 	 */
		@Override
		public void onCreateContextMenu(ContextMenu menu, View v,
				ContextMenuInfo menuInfo) {
			
			    menu.setHeaderTitle("人物详情");
		        //添加菜单项
		        menu.add(0, Menu.FIRST, 0, "资料");
		        menu.add(0, Menu.FIRST + 1, 0, "相册");
		}

	   
		/**
		 * (非 Javadoc,覆写的方法) 
		 * @Title: onContextItemSelected
		 * @Description:   上下文菜单选择
		 * @param item
		 * @return 
		 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
		 */
	   @Override
		public boolean onContextItemSelected(MenuItem item) {
		   
		   // 获取当前被选择的菜单项的信息
	        AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();        
	        switch(item.getItemId()){
	        
		        case Menu.FIRST:		   
		        	ViewDataDialog(mFriendsList.get(info.position).get("userId").toString(),
		        					mFriendsList.get(info.position).get("Name").toString());
		                	break;  
		            
		        case Menu.FIRST + 1:
		        	// 向相册页面传递数据,通过ID号获取用户相册信息
					String userid = mFriendsList.get(info.position).get("userId").toString();
					Log.d("列表", "id: "+ userid );				
					// 传递数据
					Intent intent = new Intent(FriendsListActivity.this, FriendAlbumListActivity.class);
		    		intent.putExtra("id", userid);
		    		startActivity(intent);	    		
		            break;
		              
		        default:
		            break;
	        }
	        return true;
		}


	   /**
	    * @Method: ViewDataDialog 函数
	    * @Description:  查看好友信息的对话框
	    * @param viewfriendid
	    * @param viewfriendname
	    */
	   private void ViewDataDialog(String viewfriendid,String viewfriendname){
			
			LinearLayout ViewDataDialogLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.viewdata_dialog, null);
			final TextView viewid = (TextView) ViewDataDialogLayout.findViewById(R.id.ViewDataDialogID);
			final TextView viewname = (TextView) ViewDataDialogLayout.findViewById(R.id.ViewDataDialogName);
			viewid.setText("用户名:   " + viewfriendid);
			viewname.setText("昵称:   " + viewfriendname);
			
			Builder builder = new AlertDialog.Builder(this);
			builder.setView(ViewDataDialogLayout);
			builder.setPositiveButton("确定",
					new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton)
						{
							// 编写处理用户登录的代码
						}
					});
			builder.show();
		}
	   
	   
	   /**
	    * 
	    * @ClassName: MyBroadcastReciver 
	    * @Description:  广播接收器, 接收socket服务发来的消息,并且将消息添加到气泡列表
	    * @Author: Mr.Simple (何红辉)
	    * @E-mail: bboyfeiyu@gmail.com 
	    * @Date 2012-11-17 下午5:13:06 
	    *
	    */
	  private class MyBroadcastReciver extends BroadcastReceiver {  
	    	
	    	@Override
	    	public void onReceive(Context context, Intent intent) {
	    		
	    	   String action = intent.getAction();
	    	   if(action.equals("chat.SocketService.online")) {
	    		   
	    		   // 获得广播的消息,即接收到的消息
		    	    String msg = intent.getStringExtra("broadCast");
		    	    String fName = "";			// 好友昵称
		    	    String fid = "";			// 好友的ID
		    	    String fip = "";			// 好友IP
		    	    if ( msg.contains(";;") ){
		    	    	try {
			    	    	// 将好友加入到列表中
							addFriend(fName, fid, fip);
						} catch (Exception e) {
							e.printStackTrace();
						}
		    	    }
	    	   }
	    	 }
	    
	    }	// end of MyBroadcastReciver


	  /**
	   * (非 Javadoc,覆写的方法) 
	   * @Title: onRestart
	   * @Description:  页面重新获取焦点
	   * @see android.app.Activity#onRestart()
	   */
		@Override
		protected void onRestart() {
			
			Log.d("Friend list", "onRestart");
			super.onRestart();
		}

		
		/**
		 * (非 Javadoc,覆写的方法) 
		 * @Title: onStop
		 * @Description:  当前页面失去焦点,判断是否需要释放连接
		 * @see android.app.Activity#onStop()
		 */
		@Override
		protected void onStop() {
			super.onStop();
		}
		

		/**
		 * (非 Javadoc,覆写的方法) 
		 * @Title: onDestroy
		 * @Description:  
		 * @see android.app.ListActivity#onDestroy()
		 */
		@Override
		protected void onDestroy() {
			try{
				unregisterReceiver(mDataReceiver);
			}catch(Exception e){
				
			}
			super.onDestroy();
		}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}


	private int verticalMinDistance = 150;  
	private int minVelocity = 0;  
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onFling
	 * @Description: 手势切换activity .鼠标手势相当于一个向量（当然有可能手势是曲线），e1为向量的起点，e2为向量的终点，
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
		if (e1.getX() - e2.getX() > verticalMinDistance && Math.abs(velocityX) > minVelocity) {  
			  
			 // 从左向右划动的手势,切换Activity  
	    	Intent cIntent = new Intent(FriendsListActivity.this, BaiduMapActivity.class);
			startActivityForResult(cIntent, 6); 
	        overridePendingTransition(R.anim.slide_left, R.anim.slide_right);
	       
	    } else if (e2.getX() - e1.getX() > verticalMinDistance && Math.abs(velocityX) > minVelocity) {  
	    	 // 从右向左划动的手势
			Intent bIntent = new Intent(FriendsListActivity.this, MainViewActivity.class);
			startActivityForResult(bIntent, 7); 
	        overridePendingTransition(R.anim.slide_left, R.anim.slide_right);
	       
	    } else if ( e2.getY() - e1.getY() > verticalMinDistance && Math.abs(velocityY) > minVelocity){

	    }
	  
	    return false;  
	}
	
	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onLongPress
	 * @Description: 
	 * @param e 
	 * @see android.view.GestureDetector.OnGestureListener#onLongPress(android.view.MotionEvent)
	 */
	@Override
	public void onLongPress(MotionEvent e) {
		
	}

	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onScroll
	 * @Description: 
	 * @param e1
	 * @param e2
	 * @param distanceX
	 * @param distanceY
	 * @return 
	 * @see android.view.GestureDetector.OnGestureListener#onScroll(android.view.MotionEvent, android.view.MotionEvent, float, float)
	 */
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

	int lastItem = 0;
	// 好友总数
	int totalCount = mFriendsList.size();
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		// 最后一个列表的位置
		lastItem = firstVisibleItem + visibleItemCount;
		
	}


	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if ( lastItem == totalCount && scrollState == SCROLL_STATE_IDLE){
			//Toast.makeText(FriendsListActivity.this, "滑到顶部了啦", 0).show();
		}
		
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return mGestureDetector.onTouchEvent(event);
	}
	
	

}
