package albums;

import imageCache.ImageCacheToSDCard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import network.HttpThread;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import album.entry.LoginActivity;
import album.entry.MyProgressDialog;
import album.entry.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;


/**
 * @ClassName: BigImageView 
 * @Description:  显示网络相册的大图，可以左右滑动查看图片 Activity
 * @Author: xxjgood
 * @E-mail: bboyfeiyu@gmail.com 
 * @Date 2012-11-17 下午5:44:48 
 *
 */
public class BigImageView extends Activity{
	
	private MyHandlerGetPhoto mHandlerGetPhoto = null ;	// 得到一张Bitmap，并设置为当前可见 setImageBitmap
	private MyHandlerDelPhoto mHandlerDelPhoto = null;	// 删除图片
	private Thread tGetPhoto = null;					// 获取图片线程
	private Thread tDelPhoto = null;					// 删除图片线程
	private ImageView imageView = null;					// ImageView
	private String userId = null;
	private String username = null;						// User name
	private String albumname = null;					// User albumname
	private String[] photoArray = null;					// User photoArray
	private Bitmap currentPhoto = null; 				// Current Photo
	private int currentpic = 0;							// 目前显示的photo序号
	private GridView menuGrid;							// GridView
	private PopupWindow popupWindow;					// popupwindow
	private ImageButton titleButtonReturn;
	private ImageButton titleButtonDelete;
	private ImageButton titleButtonSave;
	private TextView titleTextView = null;
	private FrameLayout titleParent;
	private Boolean titleVisible = false;
	
	private int[] menu_image_array = { android.R.drawable.ic_menu_save ,
										android.R.drawable.ic_menu_delete, 
										android.R.drawable.ic_menu_close_clear_cancel};	// popupwindow中按钮图片
	private String[] menu_name_array = { "保存","删除","取消"};							// popupwindow中按钮名称
	private ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(); // 发给服务器打包数据
	private MyProgressDialog loadingDialog;											// 等待的dialog
	private ImageCacheToSDCard mImageCache = ImageCacheToSDCard.getInstance() ;		// 缓存的使用

	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onCreate
	 * @Description:   页面创建、初始化、下载第一张照片
	 * @param savedInstanceState 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		//设置无标题  
        requestWindowFeature(Window.FEATURE_NO_TITLE); 
        //设置全屏  
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
                WindowManager.LayoutParams.FLAG_FULLSCREEN); 
		setContentView(R.layout.bigimage);

		imageView = (ImageView)findViewById(R.id.bigimage1); 	
		imageView.setOnTouchListener(new OnTouchClick() ); 		// 开启触摸响应
		imageView.setLongClickable( true );
		
		// 从上一界面获取数据
		Intent intent = getIntent();						
		userId = intent.getStringExtra("userid");
		username = intent.getStringExtra("username");
		albumname = intent.getStringExtra("albumname");
		photoArray = intent.getStringArrayExtra("photoArray");
		currentpic = intent.getIntExtra("currentpic", 0);
		
		// 初始化各种组件和变量
		initComponents() ;
		
		loadingDialog = new MyProgressDialog(BigImageView.this, "图片加载中，请稍后···");
		loadingDialog.show();

		// 从内存卡中读取缓存
		Bitmap bmp = mImageCache.getImageFromSD(2, photoArray[currentpic]);
		try {
			Thread.sleep( 100 );
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (bmp.getWidth() != 5 && bmp.getHeight() != 5) {
			loadingDialog.dismiss();
			imageView.setImageBitmap(bmp);

		} else {
			tGetPhoto = new Thread(rGetPhoto); // 启动一个新线程 获取一张大图
			tGetPhoto.start();
		}
		
	}
	

	/**
	 * @Method: initComponents
	 * @Description: 初始化各种组件和变量
	 */
	private void initComponents(){
				
		titleButtonReturn = (ImageButton)findViewById(R.id.upbigbutton1);
		titleButtonDelete = (ImageButton)findViewById(R.id.upbigbutton2);
		titleButtonSave = (ImageButton)findViewById(R.id.upbigbutton3);
		// 相册名
		titleTextView = (TextView)findViewById(R.id.upbigtextview);
		titleTextView.setText(photoArray[currentpic]);
		titleParent = (FrameLayout)findViewById(R.id.upbigimageparent1);
		titleParent.setVisibility(View.INVISIBLE);

		// 返回 
		titleButtonReturn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		// 删除
		titleButtonDelete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				deletePhoto();
			}
		});
		
		// 保存 
		titleButtonSave.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				savePhoto();
			}
		});
	}
	
	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onCreateOptionsMenu
	 * @Description:   建立菜单
	 * @param menu
	 * @return 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
    	menu.add("menu");									// 必须创建一项
		return super.onCreateOptionsMenu(menu);
	}
	

	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onMenuOpened
	 * @Description: 菜单弹出 
	 * @param featureId
	 * @param menu
	 * @return 
	 * @see android.app.Activity#onMenuOpened(int, android.view.Menu)
	 */
	@Override
    public boolean onMenuOpened(int featureId, Menu menu) {
    	
    	openPopupwin();										// 打开popupwindow
    	return false;										// 返回为true 则显示系统menu   
    }
	
	
	/**
	 * @Method: getMenuAdapter
	 * @Description: 设置 popupwindow 适配器
	 * @param menuNameArray
	 * @param menuImageArray
	 * @return
	 */
	private ListAdapter getMenuAdapter(String[] menuNameArray,int[] menuImageArray) {
		ArrayList<HashMap<String, Object>> data = new ArrayList<HashMap<String, Object>>(); 
		
		for (int i = 0; i < menuNameArray.length; i++) {					// 循环 把文字与对应图片放入HashMap
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("itemImage", menuImageArray[i]);
			map.put("itemText", menuNameArray[i]);
			data.add(map);
		}
		SimpleAdapter simperAdapter = new SimpleAdapter(this, data,			// 设置适配器
				R.layout.item_menu, new String[] { "itemImage", "itemText" },
				new int[] { R.id.item_image, R.id.item_text });
		return simperAdapter;												// 返回一个适配器
	}
	
	
	/**
	 * @Method: openPopupwin
	 * @Description: popupwindow的设置及响应
	 */
	private void openPopupwin() {
		
		LayoutInflater mLayoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		ViewGroup menuView = (ViewGroup) mLayoutInflater.inflate(R.layout.gridview_popx, null, true);
		menuGrid = (GridView) menuView.findViewById(R.id.popgridview);
		menuGrid.setAdapter(getMenuAdapter(menu_name_array, menu_image_array));
		// menuGrid的设置部署
		menuGrid.requestFocus();													
		
		// 显示popupwindow
		popupWindow = new PopupWindow(menuView, LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT, true);
		popupWindow.setBackgroundDrawable(new BitmapDrawable());
		popupWindow.showAtLocation(findViewById(R.id.imageparent1), Gravity.BOTTOM | Gravity.BOTTOM, 0, 0);
		popupWindow.update();
		
		
		// 监听popupwindow被点击的消息
		menuGrid.setOnItemClickListener(new OnItemClickListener() {  			
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,long arg3) {
				// 下载
				if(arg2 == 0){					
					try{
						android.provider.MediaStore.Images.Media.insertImage(getContentResolver(),currentPhoto,
							photoArray[currentpic],"onlinePhoto");		// 将照片保存到本地数据库
						Toast.makeText(BigImageView.this, "保存成功~~", 1).show();
					}catch(Exception e){
						Toast.makeText(BigImageView.this, e.toString(), 1).show();
					}
				}
				// 删除
				else if(arg2 == 1){				
					if(userId.equals(LoginActivity.mineID)){
						deletePhoto() ;
					}
					else{
						Toast.makeText(BigImageView.this, "这是您好友的相册，您没有权限删除！", 1).show();
					}
				}
				// 取消
				else if (arg2 == 2) {			
					
				}
				popupWindow.dismiss();
			}
		});
		
		// 焦点到了gridview上，所以需要监听此处的键盘事件。否则会出现不响应键盘事件的情况
		menuGrid.setOnKeyListener(new OnKeyListener() {
					@Override
					public boolean onKey(View v, int keyCode, KeyEvent event) {
						switch (keyCode) {
						case KeyEvent.KEYCODE_MENU:
							if (popupWindow != null && popupWindow.isShowing()) {
								popupWindow.dismiss();
							}
							break;
						}
						System.out.println("menuGridfdsfdsfdfd");
						return true;
					}
				});
		

	}

	
	
	/**
	 * @Method: deletePhoto
	 * @Description:
	 */
	private void deletePhoto() {
		if (userId.equals(LoginActivity.mineID)) {
			// 确认删除
			new AlertDialog.Builder(BigImageView.this)
					.setIcon(R.drawable.beaten)
					.setTitle("确认删除?")
					.setPositiveButton(R.string.alert_dialog_ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int whichButton) {
									loadingDialog.setTitle("删除中,请稍候...") ;
									loadingDialog.show() ;
									// 删除图像
									tDelPhoto = new Thread(deleteRunnable);
									tDelPhoto.start();
								}
							})
					.setNegativeButton(R.string.alert_dialog_cancel,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int whichButton) {

								}
							}).show();

		} else {
			Toast.makeText(BigImageView.this, "这是您好友的相册，您没有权限删除！", 1).show();
		}
	}

	
	/**
	 * @Method: savePhoto
	 * @Description: 保存图片到本地
	 */
	private void savePhoto() {
		try {
			android.provider.MediaStore.Images.Media.insertImage(
					getContentResolver(), currentPhoto, photoArray[currentpic],
					"onlinePhoto"); // 将照片保存到本地数据库
			Toast.makeText(BigImageView.this, "保存成功~~", 1).show();
		} catch (Exception e) {
			Toast.makeText(BigImageView.this, e.toString(), 1).show();
		}
	}


	
	
	/**
	 * @ClassName: OnTouchClick 
	 * @Description:  内部类  触摸监听
	 * @Author: xxjgood
	 * @E-mail: bboyfeiyu@gmail.com 
	 * @Date 2012-11-18 下午10:27:43 
	 *
	 */
class OnTouchClick implements OnTouchListener,OnGestureListener{
	// 手势监听
	GestureDetector mGestureDetector = new GestureDetector(this); 	
	
	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onFling
	 * @Description:  手势切换图
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 * @return 
	 * @see android.view.GestureDetector.OnGestureListener#onFling(android.view.MotionEvent, android.view.MotionEvent, float, float)
	 */
	@Override
	public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
		
		Log.d("2", "Onfling");
		// 滑动距离和速度
		final int FLING_MIN_DISTANCE = 100, FLING_MIN_VELOCITY = 200;  
	    if (arg0.getX() - arg1.getX() > FLING_MIN_DISTANCE && Math.abs(arg2) > FLING_MIN_VELOCITY) {  
	        // Fling left  
	        Log.d("3", "Fling left"); 
	        if(currentpic == photoArray.length-1)	// 如果滑到最后一张，则跳转到第一张
	        	currentpic = 0;
	        else									// 否则跳到下一张
	        	currentpic++;
	        	        
	    } else if (arg1.getX() - arg0.getX() > FLING_MIN_DISTANCE && Math.abs(arg2) > FLING_MIN_VELOCITY) {  
	        // Fling right  
	        Log.d("3", "Fling right"); 
	        if(currentpic == 0)
	        	currentpic = photoArray.length - 1;
	        else
	        	currentpic--;
	    }else{
	    	return false ;
	    }
	    imageView.setLongClickable(false);		// 关闭关闭触摸响应

	    titleTextView.setText(photoArray[currentpic]);
	    // 从缓存或者网络中获取大图图片
	    getImageFromCacheOrNet( currentpic ) ;
	    
	    return false;
	}

	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onTouch
	 * @Description: 
	 * @param v
	 * @param event
	 * @return 
	 * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		
		Log.d("2", "OnTouch");
		
	 	// 设置长按
		return mGestureDetector.onTouchEvent(event);
	}

	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onDown
	 * @Description: 
	 * @param arg0
	 * @return 
	 * @see android.view.GestureDetector.OnGestureListener#onDown(android.view.MotionEvent)
	 */
	@Override
	public boolean onDown(MotionEvent arg0) {
		
		Log.d("2", "OnDown");
		return false;
	}
	
	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onLongPress
	 * @Description: 
	 * @param arg0 
	 * @see android.view.GestureDetector.OnGestureListener#onLongPress(android.view.MotionEvent)
	 */
	@Override
	public void onLongPress(MotionEvent arg0) {
		
		Log.d("2", "Onlongpress");
	}

	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onScroll
	 * @Description: 
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 * @return 
	 * @see android.view.GestureDetector.OnGestureListener#onScroll(android.view.MotionEvent, android.view.MotionEvent, float, float)
	 */
	@Override
	public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2,
			float arg3) {
		
		Log.d("2", "Onscroll");
		return false;
	}

	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onShowPress
	 * @Description: 
	 * @param arg0 
	 * @see android.view.GestureDetector.OnGestureListener#onShowPress(android.view.MotionEvent)
	 */
	@Override
	public void onShowPress(MotionEvent arg0) {
		
		Log.d("2", "Onshowpress");
	}

		/**
		 * (非 Javadoc,覆写的方法)
		 * 
		 * @Title: onSingleTapUp
		 * @Description: 单击事件
		 * @param arg0
		 * @return
		 * @see android.view.GestureDetector.OnGestureListener#onSingleTapUp(android.view.MotionEvent)
		 */
		@Override
		public boolean onSingleTapUp(MotionEvent arg0) {

			Log.d("2", "Onsingletapup");
			if (titleVisible) {
				titleVisible = false;
				titleParent.setVisibility(View.VISIBLE);
			} else {
				titleVisible = true;
				titleParent.setVisibility(View.INVISIBLE);
			}
			return false;
		}

	} // end of inner class OnTouchClick



/**
 * @Method: getImageFromCacheOrNet
 * @Description:  从缓存中读取图片,如果缓存中没有则从网络服务器请求该图片
 */
private void getImageFromCacheOrNet( int curIndex ){
	 // 从缓存中读取
    Bitmap bmp = null ;
    try {
    	// 从缓存中读取图片
    	String filename = photoArray[curIndex] ;
    	bmp = mImageCache.getImageFromSD(2, filename) ;
    }catch(Exception e){
    	Log.d("滑动读取缓存", "滑动读取缓存出出错") ;
    	e.printStackTrace();
    }
    
    // 从缓存中读取到的图片,宽和高都不为5则为读取到缓存图片,否则向网络服务器请求图片
   if (bmp.getWidth() != 5 && bmp.getHeight() != 5){
	    
	   Animation anim = AnimationUtils.loadAnimation(BigImageView.this, 
			   				android.R.anim.fade_in) ;
	    anim.setInterpolator(new AccelerateDecelerateInterpolator()); 
	    anim.setDuration( 800 ); 
	    
	    imageView.startAnimation( anim ) ;
	    imageView.setImageBitmap(bmp);
	    imageView.setLongClickable(true);		// 开启触摸响应

   }else {  // 缓存没有该图片则启动线程下载
	    loadingDialog.show();
        tGetPhoto = new Thread(rGetPhoto);		// 开线程下载图片
		tGetPhoto.start();
   }
	
}


	/**
	 * @Method: packData
	 * @Description: 打包数据
	 */
	private void packData()														
	{
		nameValuePairs.add(new BasicNameValuePair("protocol", "getImage"));	// 封装键值对
		nameValuePairs.add(new BasicNameValuePair("id", userId));
		nameValuePairs.add(new BasicNameValuePair("albumName", albumname));
		nameValuePairs.add(new BasicNameValuePair("imageName", photoArray[currentpic]));	
	}
	
	
	/**
	 * @ClassName: MyHandlerGetPhoto 
	 * @Description:  内部类  MyHandlerGetPhoto 接收子线程消息
	 * @Author: xxjgood
	 * @E-mail: bboyfeiyu@gmail.com 
	 * @Date 2012-11-17 下午5:47:10 
	 *
	 */
	private class MyHandlerGetPhoto extends Handler{       
        public MyHandlerGetPhoto(Looper looper){
               super (looper);
        }
        @Override
        public void handleMessage(Message msg) { 									// 处理消息
        	String filename = photoArray[currentpic] ;
        	setTitle(username + "/" + albumname + "/" + filename); 	// 设置title
        	currentPhoto = (Bitmap)msg.obj;				// 获取Bitmap
        	imageView.setImageBitmap(currentPhoto);		// 显示Bitmap
        	imageView.setLongClickable(true);			// 开启触摸响应
        	try {
        		// 将下载下来的图片保存到SD卡中缓存起来
				mImageCache.saveBmpToSd(currentPhoto, filename, 2) ;
			} catch (Exception e) {
				Log.d("大图浏览", "大图写入缓存失败") ;
				e.printStackTrace();
			}
        	loadingDialog.dismiss();
        }            
	}

	
	/**
	 *  Run 方法 ，发送一张大图到主线程
	 */
	Runnable rGetPhoto = new Runnable()								
	{
		Bitmap bitmap = null;
		@Override
		public void run()
		{
			Log.d("1", "new thread" + Thread.currentThread().getId());
			packData();
			HttpThread h = new HttpThread(nameValuePairs,101);
			bitmap = (Bitmap)h.sendInfo();
			sendMessage();
		}
		public void sendMessage(){										// 线程间数据传输
            Looper mainLooper = Looper.getMainLooper ();				// 得到主线程loop
            Bitmap msg ;
            mHandlerGetPhoto = new MyHandlerGetPhoto(mainLooper);		// 创建主线程的handler
            msg = bitmap;
            mHandlerGetPhoto.removeMessages(0);							// 移除所有队列中的消息
            Message m = mHandlerGetPhoto.obtainMessage(1, 1, 1, msg);	// 把消息放入message
            mHandlerGetPhoto.sendMessage(m);							// 发送message
		}
	};

	
	/**
	 * @ClassName: MyHandlerDelPhoto 
	 * @Description:   删除图像的handler
	 * @Author: Mr.Simple (何红辉)
	 * @E-mail: bboyfeiyu@gmail.com 
	 * @Date 2012-11-17 下午5:56:15 
	 *
	 */
	private class MyHandlerDelPhoto extends Handler{       
        public MyHandlerDelPhoto(Looper looper){
               super (looper);
        }
        
        /**
         * (非 Javadoc,覆写的方法) 
         * @Title: handleMessage
         * @Description: 
         * @param msg 
         * @see android.os.Handler#handleMessage(android.os.Message)
         */
        @Override
        public void handleMessage(Message msg) {
        	String s = msg.obj.toString();
        	s = s.trim();	
        	if(s.equals("success")){
        		
        		Toast.makeText(BigImageView.this, "删除成功！", 1).show();
         	   // 从mImageList移除该图片
         	   InPhotoAlbumActivity.netGridAdapter.mImageList.remove(currentpic);
         	   InPhotoAlbumActivity.netGridAdapter.mImageList.trimToSize();
         	   
         	   removeItemFromArray( currentpic ) ;
         	   
        		if(currentpic == photoArray.length - 1)	// 如果滑到最后一张，则跳转到第一张
		        	currentpic = 0;
		        else									// 否则跳到下一张
		        	currentpic++;
        		
        		imageView.setLongClickable(false);		// 关闭关闭触摸响应
        		
        		loadingDialog.setTitle("图片加载中,请稍候...") ;
				loadingDialog.dismiss() ;
				
        		// 删除图片后,从缓存中或者网络服务器获取下一张大图图片
        		getImageFromCacheOrNet( currentpic );
        		
        	   // 数量减1
        	   InPhotoAlbumActivity.photoCount--;	
        	  
        	}
        	else if(s.equals("fail")){
        		Toast.makeText(BigImageView.this, "删除失败！", 1).show();
        	}
        	else{
        		Toast.makeText(BigImageView.this, "出错啦，请检查您的网络！", 1).show();
        	}
        }            
	}
	
	/**
	 * @Method: removeItemFromArray
	 * @Description:
	 * @param index
	 */
	private void removeItemFromArray(int index){
		try{
			List<String> temp = new ArrayList<String>() ;
			for(String item : photoArray){
				temp.add( item );
				Log.d("", "图片 :" + item) ;
			}
			
			Log.d("", "要删除的图片 :" + photoArray[index]) ;
			temp.remove( photoArray[index] );
			
			photoArray = new String[temp.size()];
			for(int i=0; i<temp.size(); i++){
				photoArray[i] = temp.get(i) ;
				Log.d("删除后", temp.get(i)) ;
			}
		}catch(Exception e){
			e.printStackTrace() ;
		}
		
	}
	
	
	/**
	 * 删除图像的线程
	 */
	Runnable deleteRunnable = new Runnable() {
		String msg;
		@Override
		public void run() {
			// 删除图像
			ArrayList<NameValuePair> nameValuePair1 = new ArrayList<NameValuePair>();
			nameValuePair1.add(new BasicNameValuePair("protocol", "deleteImage"));
			nameValuePair1.add(new BasicNameValuePair("id", userId));
			nameValuePair1.add(new BasicNameValuePair("albumName", albumname));
			nameValuePair1.add(new BasicNameValuePair("imageName", photoArray[currentpic]));
			
			HttpThread h = new HttpThread(nameValuePair1, 15);
			msg = (String)h.sendInfo();
			sendMessage();
		}
		public void sendMessage(){							// 线程间数据传输
			Looper mainLooper = Looper.getMainLooper ();	// 得到主线程loop
            mHandlerDelPhoto = new MyHandlerDelPhoto(mainLooper);			// 创建主线程的handler
            mHandlerDelPhoto.removeMessages(0);								// 移除所有队列中的消息
            Message m = mHandlerDelPhoto.obtainMessage(1, 1, 1, msg);	// 把消息放入message
            mHandlerDelPhoto.sendMessage(m);								// 发送message
		}
	};
}
