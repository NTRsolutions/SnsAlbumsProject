package albums;

import imageEdit.PictureEditActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import network.Base64;
import network.HttpThread;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import album.entry.LoginActivity;
import album.entry.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore.Audio.Media;
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
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;


/**
 * @ClassName: LocalImageView 
 * @Description:  浏览本地照片大图的Activity
 * @Author: Mr.Simple (何红辉)
 * @E-mail: bboyfeiyu@gmail.com 
 * @Date 2012-11-17 下午6:34:11 
 *
 */

public class LocalImageView extends Activity implements OnGestureListener{
	
	private String[] albumArray;			// User albumArray
	private String choosedAlbum;			// 上传时选中的相册
	private int currentNum;					// 当前图片的序号
	private String photoPath;				// 照片的路径
	private String AlbumName;
	private Bitmap currentBitmap;			// 当前Bitmap
	private MyHandler mHandler = null ;		// UI线程中的 MyHandler
	private Thread uploadThread = null;		// 上传照片线程
	private GridView menuGrid;				// GridView
	private PopupWindow menuPopupWindow;	// popupwindow
	private PopupWindow albumPopupWindow;	// popupwindow
	private int[] menu_image_array = { android.R.drawable.ic_menu_upload,
										android.R.drawable.ic_menu_edit, 
										android.R.drawable.ic_menu_delete,
										android.R.drawable.ic_menu_close_clear_cancel};	// popupwindow中按钮图片
	private String[] menu_name_array = { "上传","编辑", "删除","取消" };						// popupwindow中按钮名称
	private ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();	// 打包数据发送到服务器
	private ProgressDialog mProgressDialog;												// 登陆时的进度条
	
	public ViewFlipper flipper;					// 实现动画的控件
	private ArrayList<ImageView> ImageViewList;	// 3个ImageView的List
	private int middleView;						// 中间那个View的真实序号
	private int lastView;						// 最后那个View的真实序号
	private int firstView;
	private boolean LeftIsTrue = false;			// 如果是向左滑，则为true，否则为false
	private boolean once = true;				// 判断第一次滑动
	private int photoCount = 0;					// 此相册的照片总数
	private GestureDetector detector;			// 手势滑动的检测器
	SoftReference<Bitmap> bitmapcache;			// 软引用
	private boolean touchEnable = true;			// 判断是否对触摸进行响应
	private Handler handler;					// 定时器
	private Bitmap[] BitmapList;
	
	private ImageButton titleButtonReturn;		// 返回按钮,最上层的toolButton
	private ImageButton titleButtonShare = null;// 分享按钮
	private ImageButton titleButtonDelete;		// 删除按钮
	private TextView titleTextView;				// 类似自定义的title
	private FrameLayout titleParent;
	private Boolean titleVisible = false;		// 标题是否可见

	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onCreate
	 * @Description:  创建Activity，并且各种初始化
	 * @param savedInstanceState 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		//设置无标题  
        requestWindowFeature(Window.FEATURE_NO_TITLE);  
        //设置全屏  
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
                WindowManager.LayoutParams.FLAG_FULLSCREEN); 
        
		setContentView(R.layout.photo_viewfilpper);
		
		// 获取intent信息,通过Intent传递的数据
		Intent intent = getIntent();					
		currentNum = intent.getIntExtra("currentpic", 0);
		albumArray = intent.getStringArrayExtra("albumArray");
		photoPath = intent.getStringExtra("photopath");
		AlbumName = intent.getStringExtra("AlbumName");
		
		detector = new GestureDetector(this);					
		flipper = (ViewFlipper)findViewById(R.id.ViewFlipper);	
		
		try {
			// 初始化各种组件和变量
			initCompenents() ;
		} catch (Exception e) {
			e.printStackTrace();
		}
				

	}	
	

	/**
	 * @Method: initCompenents
	 * @Description: 初始化各种组件和变量
	 */
	private void initCompenents() throws Exception {

		titleButtonReturn = (ImageButton) findViewById(R.id.upfilpperbutton1);
		titleButtonShare = (ImageButton) findViewById(R.id.upfilpperbutton3);
		titleButtonDelete = (ImageButton) findViewById(R.id.upfilpperbutton2);
		titleButtonDelete.setBackgroundColor(Color.TRANSPARENT);
		titleTextView = (TextView) findViewById(R.id.upfilppertextview);
		titleTextView.setText(PhotoAlbumActivity.AlbumsFloderTitle.get(
				AlbumName).get(currentNum));
		titleParent = (FrameLayout) findViewById(R.id.upfilpperparent);
		titleParent.setVisibility(View.INVISIBLE);

		// 返回上一级Activity
		titleButtonReturn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		// 删除当前照片
		titleButtonDelete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sureToDelete();
			}
		});
		
		// 分享当前照片
		titleButtonShare.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				shareToWeiBo(PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName)
						.get(currentNum));
			}
		});

		photoCount = PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).size();
		middleView = 1;
		lastView = 2;
		firstView = 0;

		BitmapList = new Bitmap[3];

		ImageViewList = new ArrayList<ImageView>();
		for (int i = 0; i < 3; i++)
			ImageViewList.add(new ImageView(this));

		BitmapList[0] = decodeBitmap(
				PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(
						currentNum), 400);
		ImageViewList.get(0).setImageBitmap(BitmapList[0]);

		if (currentNum == photoCount - 1)
			BitmapList[1] = decodeBitmap(PhotoAlbumActivity.AlbumsFloderPath
					.get(AlbumName).get(0), 400);
		else
			BitmapList[1] = decodeBitmap(PhotoAlbumActivity.AlbumsFloderPath
					.get(AlbumName).get(currentNum + 1), 400);
		ImageViewList.get(1).setImageBitmap(BitmapList[1]);

		if (currentNum == 0)
			BitmapList[2] = decodeBitmap(PhotoAlbumActivity.AlbumsFloderPath
					.get(AlbumName).get(photoCount - 1), 400);
		else
			BitmapList[2] = decodeBitmap(PhotoAlbumActivity.AlbumsFloderPath
					.get(AlbumName).get(currentNum - 1), 400);
		ImageViewList.get(2).setImageBitmap(BitmapList[2]);

		flipper.addView(ImageViewList.get(0));
		flipper.addView(ImageViewList.get(1));
		flipper.addView(ImageViewList.get(2));

		System.gc();

		handler = new Handler();
	}
	
	
	/**
	 *  定时器
	 */
	Runnable runnable=new Runnable(){
		@Override
		public void run() {
			touchEnable = true;
			handler.removeCallbacks(runnable);
		}
	};
	

	/**
	 * @ClassName: MyHandler 
	 * @Description:  内部类  MyHandler 接收上传图片的成功与否
	 * @Author: xxjgood
	 * @E-mail: bboyfeiyu@gmail.com 
	 * @Date 2012-11-17 下午6:35:13 
	 *
	 */
	private class MyHandler extends Handler{      
        public MyHandler(Looper looper){
               super (looper);
        }
        @Override
        public void handleMessage(Message msg) { 
        	String s = msg.obj.toString();
        	if(s.equals("success")){
        		Toast.makeText(LocalImageView.this, " 恭喜您，成功上传图片", 0).show();
        		// 图像上传成功,在相册列表需要刷新,设置标识符
        		PhotoAlbumActivity.bRefresh = true;
        	}
        	else if(s.equals("fail")){
        		Toast.makeText(LocalImageView.this, "上传图片失败！！！", 0).show();
        	}
        	else
        		Toast.makeText(LocalImageView.this, "出错了", 0).show();	
        	
        	mProgressDialog.dismiss();
        } 
        
	}
	

	/**
	 *  Run 方法， 上传照片
	 */
	Runnable uploadRunnable = new Runnable()							
	{
		String s = null;	// 存放需要发送的msg
		@Override
		public void run()
		{
			packData();										// 打包数据
			HttpThread h = new HttpThread(nameValuePairs,6);// 6--上传
			Log.d("上传","shangchuan");
			s = (String)h.sendInfo();						// 获取s
			sendMessage();									// 发送msg
		}
		
		public void sendMessage(){	
			nameValuePairs.clear();
            Looper mainLooper = Looper.getMainLooper ();	// 得到主线程loop
            mHandler = new MyHandler(mainLooper);			// 创建主线程的handler
            mHandler.removeMessages(0);						// 移除所有队列中的消息
            Message m = mHandler.obtainMessage(1, 1, 1, s);	// 把消息放入message
            mHandler.sendMessage(m);						// 发送message
		}
	};
	

	/**
	 * @Method: packData
	 * @Description:  打包要发送的数据
	 */
	private void packData(){
		
		currentBitmap = BitmapList[firstView];
		ByteArrayOutputStream stream1 = new ByteArrayOutputStream();	// stream1
		currentBitmap.compress(Bitmap.CompressFormat.PNG, 1, stream1); 	// compress to which format you want
		byte[] byte_arr = stream1.toByteArray();						// stream1 to byte
		try {
			stream1.close();
			stream1 = null;
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		String image_str = Base64.encodeBytes(byte_arr);				// byte to string
		
		Bitmap smallcurrentBitmap;
		float wRatio = (float) (currentBitmap.getWidth()/100.0);
		float hRatio = (float) (currentBitmap.getHeight()/100.0);
		if(wRatio > 1 && hRatio > 1) 									//如果超出指定大小，则缩小相应的比例 
		{
			float scaleTemp;
			if(wRatio>hRatio)
				scaleTemp = hRatio;
			else
				scaleTemp = wRatio;
			wRatio = currentBitmap.getWidth()/scaleTemp;
			hRatio = currentBitmap.getHeight()/scaleTemp;
			int h = (int)wRatio;
			int w = (int)hRatio;
    		smallcurrentBitmap = Bitmap.createScaledBitmap(currentBitmap, h, w, false); 
		}
		else
			smallcurrentBitmap = currentBitmap;
		
		ByteArrayOutputStream stream2 = new ByteArrayOutputStream();						// stream2
		smallcurrentBitmap.compress(Bitmap.CompressFormat.PNG, 50, stream2);				// compress to which format you want
		smallcurrentBitmap.recycle();
		byte_arr = stream2.toByteArray();													// stream2 to byte
		try {
			stream2.close();
			stream2 = null;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		String smallimage_str = Base64.encodeBytes(byte_arr);								// byte to string
		
		nameValuePairs.add(new BasicNameValuePair("protocol","upload"));					// 封装键值对
		nameValuePairs.add(new BasicNameValuePair("id", LoginActivity.mineID));
		nameValuePairs.add(new BasicNameValuePair("albumName", choosedAlbum));			
		nameValuePairs.add(new BasicNameValuePair("imageName", 
				PhotoAlbumActivity.AlbumsFloderName.get(AlbumName).get(currentNum)));
		Log.d("debug",PhotoAlbumActivity.AlbumsFloderName.get(AlbumName).get(currentNum));
		nameValuePairs.add(new BasicNameValuePair("image", image_str));
		nameValuePairs.add(new BasicNameValuePair("smallImage", smallimage_str));
		
		System.gc();
	}
	
	/**
	 * @Method: shareToWeiBo
	 * @Description: 分享图片到微博
	 * @param path   图片的路径
	 */
	private void shareToWeiBo(String path){
		Log.d("分享到微博 -图片地址 : ", path);
		Intent intent = new Intent(Intent.ACTION_SEND);
		// imagePath:完整路径，要带文件扩展名  
		String url = "file:///" + path;  				
		intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(url));
		intent.setType("image/jpeg");
		intent.putExtra(Intent.EXTRA_TITLE,"分享到微博");
		intent.putExtra(Intent.EXTRA_STREAM,Uri.parse(url));        
		startActivity(Intent.createChooser(intent, "分享方式"));
	}

	
	/**
	 * @Method: showProgress
	 * @Description:  登陆时进度条显示.
	 */
	private void showProgress(){
		mProgressDialog = new ProgressDialog( this );
		mProgressDialog.setIcon(R.drawable.l_cn_48);
		mProgressDialog.setTitle("上传中,请稍后...");
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		
		mProgressDialog.show();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

    	menu.add("menu");											// 必须创建一项
		return super.onCreateOptionsMenu(menu);
	}
	@Override
    public boolean onMenuOpened(int featureId, Menu menu) {

    	openPopupwin();
    	return false;												// 返回为true 则显示系统menu   
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
		
		// 循环 把文字与对应图片放入HashMap
		for (int i = 0; i < menuNameArray.length; i++) {					
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("itemImage", menuImageArray[i]);
			map.put("itemText", menuNameArray[i]);
			data.add(map);
		}
		
		// 设置适配器
		SimpleAdapter simperAdapter = new SimpleAdapter(this, data,			
				R.layout.item_menu, new String[] { "itemImage", "itemText" },
				new int[] { R.id.item_image, R.id.item_text });
		
		// 返回一个适配器
		return simperAdapter;												
	}
	
	
	/**
	 * @Method: openPopupwin
	 * @Description:  popupwindow的设置及响应
	 */
	private void openPopupwin() {
		
		LayoutInflater mLayoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		ViewGroup menuView = (ViewGroup) mLayoutInflater.inflate(R.layout.gridview_popx, null, true);
		menuGrid = (GridView) menuView.findViewById(R.id.popgridview);
		menuGrid.setAdapter(getMenuAdapter(menu_name_array, menu_image_array));
		// menuGrid的设置部署
		menuGrid.requestFocus();													
		
		// 监听popupwindow被点击的消息
		menuGrid.setOnItemClickListener(new OnItemClickListener() {  			
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,long arg3) {
				menuPopupWindow.dismiss();
				if(arg2 == 0){		// 上传
					albumPopupwin();
				}
				else if(arg2 == 1)	// 编辑
				{
					Intent intent = new Intent(LocalImageView.this,PictureEditActivity.class);
					intent.putExtra("photopath", photoPath);
					startActivity(intent);
				}
				else if(arg2 == 2)	// 删除
				{
					sureToDelete();
				}

			}
		});
		
		// 焦点到了gridview上，所以需要监听此处的键盘事件。否则会出现不响应键盘事件的情况
		menuGrid.setOnKeyListener(new OnKeyListener() {
					@Override
					public boolean onKey(View v, int keyCode, KeyEvent event) {
						switch (keyCode) {
						case KeyEvent.KEYCODE_MENU:
							if (menuPopupWindow != null && menuPopupWindow.isShowing()) {
								menuPopupWindow.dismiss();
							}
							break;
						}
						System.out.println("menuGridfdsfdsfdfd");
						return true;
					}
				});
		
		// 显示popupwindow
		menuPopupWindow = new PopupWindow(menuView, LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT, true);
		menuPopupWindow.setBackgroundDrawable(new BitmapDrawable());
		menuPopupWindow.setAnimationStyle(R.style.PopupAnimation);
		menuPopupWindow.showAtLocation(findViewById(R.id.filpperparent), Gravity.BOTTOM | Gravity.BOTTOM, 0, 0);
		menuPopupWindow.update();
	}
	

	/**
	 * @Method: sureToDelete
	 * @Description:  删除照片前的确认对话框
	 */
	private void sureToDelete(){
		new AlertDialog.Builder(this)
		.setIcon(R.drawable.beaten)
		.setTitle("确定删除")
		.setPositiveButton("确定", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {

				try {
					// 删除照片
					deleteCurrentPhoto() ;
				} catch (Exception e) {
					Toast.makeText(LocalImageView.this, "删除出错...", 0).show() ;
					e.printStackTrace();
				}
			}
		})
		.setNegativeButton("取消", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		}).create().show();
		
	}
	
	
	/**
	 * @Method: deleteCurrentPhoto
	 * @Description: 删除当前的照片
	 */
	private void deleteCurrentPhoto() throws Exception
	{
		// 根据数据库中的ID号获取URI
		Uri uri = ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, 
				PhotoAlbumActivity.AlbumsFloderID.get(AlbumName).get(currentNum));
		// 用URI删除图片
		getContentResolver().delete(uri, null,null); 
		
		// 更新adaper
		InLocalSmallPhotoActivity.localGridAdapter.mImageList.remove(currentNum);
		InLocalSmallPhotoActivity.localGridAdapter.notifyDataSetChanged();
		
		if(photoCount == 1)
		{
			;
		}
		else if(photoCount == 2)
		{
			if(currentNum == 0)
			{
				BitmapList[firstView] = decodeBitmap(
						PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(1), 400);
				BitmapList[middleView] = decodeBitmap(
						PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(1), 400);
				BitmapList[lastView] = decodeBitmap(
						PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(1), 400);
			}
			else
			{
				BitmapList[firstView] = decodeBitmap(
						PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(0), 400);
				BitmapList[middleView] = decodeBitmap(
						PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(0), 400);
				BitmapList[lastView] = decodeBitmap(
						PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(0), 400);
			}
		}
		else
		{
			// 更新flipper里面的数据
			if(currentNum == photoCount-1)
			{
				BitmapList[firstView] = decodeBitmap(
						PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(0), 400);
				BitmapList[middleView] = decodeBitmap(
						PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(1), 400);
			}
			else if(currentNum == photoCount-2)
			{
				BitmapList[firstView] = decodeBitmap(
						PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(currentNum+1), 400);
				BitmapList[middleView] = decodeBitmap(
						PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(0), 400);
			}
			else
			{
				BitmapList[firstView] = decodeBitmap(
						PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(currentNum+1), 400);
				BitmapList[middleView] = decodeBitmap(
						PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(currentNum+2), 400);
			}
			if(currentNum == 0)
				BitmapList[lastView] = decodeBitmap(
						PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(photoCount-1), 400);
			else
				BitmapList[lastView] = decodeBitmap(
						PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(currentNum-1), 400);
		}
		
		
		// 更新内存中的数据
		PhotoAlbumActivity.AlbumsFloderID.get(AlbumName).remove(currentNum);
		PhotoAlbumActivity.AlbumsFloderName.get(AlbumName).remove(currentNum);
		PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).remove(currentNum);
		PhotoAlbumActivity.AlbumsFloderTime.get(AlbumName).remove(currentNum);
		PhotoAlbumActivity.AlbumsFloderTitle.get(AlbumName).remove(currentNum);
		
		// 切换到下一张图片
		ImageViewList.get(firstView).destroyDrawingCache();
		ImageViewList.get(middleView).destroyDrawingCache();
		ImageViewList.get(firstView).setImageBitmap(BitmapList[firstView]);
		ImageViewList.get(middleView).setImageBitmap(BitmapList[lastView]);
		
		photoCount--;
		once = true;
		if(photoCount == 0)
			finish();
		else
			titleTextView.setText(PhotoAlbumActivity.AlbumsFloderTitle.get(AlbumName).get(currentNum));
	}

	
	/**
	 * @Method: albumPopupwin
	 * @Description: menuPopupwindow的设置及响应
	 */
	private void albumPopupwin(){
		
    	ListView popList;
    	ImageView popImage;
    	SimpleAdapter adapter;			
    	List<Map<String, Object>> foldersList = new ArrayList<Map<String, Object>>();
    	
		LayoutInflater mLayoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		ViewGroup menuView = (ViewGroup) mLayoutInflater.inflate(R.layout.listview_pop, null, true);
		popList = (ListView)menuView.findViewById(R.id.poplistview);
		popImage = (ImageView)menuView.findViewById(R.id.poplistviewimage);
		// popwindow 的标题
		popImage.setImageDrawable(getResources().getDrawable(R.drawable.pop_titleimg));
		
		// 适配器格式 
		adapter = new SimpleAdapter(this, foldersList, R.layout.smallinsidelistview,
				new String[]{"albumName","picnum","img"},				
				new int[]{R.id.smalluserName,R.id.smallipInfo,R.id.smalluserImg});	
		popList.setAdapter(adapter);
		
		foldersList.clear();
		
    	for (int i = 1; i < albumArray.length; i+=2) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("albumName", albumArray[i]);
			map.put("picnum", albumArray[i+1] + "张");
			map.put("img", R.drawable.folder);
			foldersList.add(map);
			adapter.notifyDataSetChanged();
		}
		
		popList.requestFocus();
		popList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,long arg3) {
				Log.d("debug",albumArray.length + "");
				choosedAlbum = albumArray[arg2*2+1];
				uploadThread = new Thread( uploadRunnable );// 启动一个新线程
        		uploadThread.start();
				albumPopupWindow.dismiss();	// 相册选择框
				showProgress();				// 进度条
			}
		});
		popList.setOnKeyListener(new OnKeyListener() {// 焦点到了gridview上，所以需要监听此处的键盘事件。否则会出现不响应键盘事件的情况
					@Override
					public boolean onKey(View v, int keyCode, KeyEvent event) {
						switch (keyCode) {
						case KeyEvent.KEYCODE_MENU:
							if (albumPopupWindow != null && albumPopupWindow.isShowing()) {
								albumPopupWindow.dismiss();
							}
							break;
						}
						System.out.println("menuGridfdsfdsfdfd");
						return true;
					}
				});
		
		int poplength = (albumArray.length - 1)*53 + 65;
		Log.d("10",albumArray.length + "");
		if(poplength > 595)
			poplength = 595;
		albumPopupWindow = new PopupWindow(menuView, 300,poplength, true);
		albumPopupWindow.setBackgroundDrawable(new BitmapDrawable());
		albumPopupWindow.setAnimationStyle(R.style.PopupAnimation);
		albumPopupWindow.showAtLocation(findViewById(R.id.filpperparent), Gravity.CENTER, 0, 0);
		albumPopupWindow.update();
		
	}
	
	
	/**
	 * @Method: decodeBitmap
	 * @Description: 读取path路径下的图片，并返回
	 * @param path
	 * @param rect
	 * @return
	 */
	Bitmap decodeBitmap(String path,int rect){ 
    	BitmapFactory.Options op = new BitmapFactory.Options(); 
    	op.inJustDecodeBounds = true; 
    	op.inPreferredConfig = Bitmap.Config.ALPHA_8;
    	Bitmap bmp = BitmapFactory.decodeFile(path, op);
    	//获取比例大小									 
    	int wRatio = (int)Math.ceil(op.outWidth/rect); 		
    	int hRatio = (int)Math.ceil(op.outHeight/rect); 
    	
    	//如果超出指定大小，则缩小相应的比例 
    	if(wRatio > 1 && hRatio > 1){ 			
    		if(wRatio > hRatio){ 
    			op.inSampleSize = wRatio; 
    		}else{ 
    			op.inSampleSize = hRatio; 
    		} 
    	} 
    	op.inPreferredConfig = Bitmap.Config.ALPHA_8;
    	op.inJustDecodeBounds = false; 
    	bmp = BitmapFactory.decodeFile(path, op); 
    	return bmp; 
    }
	
	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onFling
	 * @Description:  手势切换图
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
		if(!touchEnable)
			return false;
		touchEnable = false;
		handler.postDelayed(runnable, 500);
		
		try{
			if (e1.getX() - e2.getX() > 120) {
				// 第一次不执行，以后每次都执行
				if(!once)
				{
					if(BitmapList[middleView].isRecycled()==false) //如果没有回收  
						BitmapList[middleView].recycle(); 
					System.gc();
					
					// 上次是向左滑动
					if(LeftIsTrue)
					{
						if(currentNum == photoCount-1)
							BitmapList[middleView] = decodeBitmap(PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(0), 400);
						else
							BitmapList[middleView] = decodeBitmap(PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(currentNum+1), 400);
						
					}
					// 上次向右滑动
					else
					{
						if(currentNum == 0)
							BitmapList[middleView] = decodeBitmap(PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(photoCount-1), 400);
						else
							BitmapList[middleView] = decodeBitmap(PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(currentNum-1), 400);
					}
					ImageViewList.get(middleView).destroyDrawingCache();
					ImageViewList.get(middleView).setImageBitmap(BitmapList[middleView]);
					
				}
				
				// 三个变量记录当前状态
				if(firstView == 2)
					firstView = 0;
				else
					firstView++;
				
				if(middleView == 2)
					middleView = 0;
				else
					middleView++;
				
				if(lastView == 2)
					lastView = 0;
				else
					lastView++;
				
				if(currentNum == photoCount-1)
					currentNum = 0;
				else
					currentNum ++;
				
				once = false;
				LeftIsTrue = true;
	
				flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_in));
				flipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_out));
				flipper.showNext();
				
			} else if (e1.getX() - e2.getX() < -120) {
				if(!once)
				{	
					if(BitmapList[lastView].isRecycled()==false) //如果没有回收  
						BitmapList[lastView].recycle();  
					System.gc();
					
					if(LeftIsTrue)
					{
						if(currentNum == photoCount-1)
							BitmapList[lastView] = decodeBitmap(PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(0), 400);
						else
							BitmapList[lastView] = decodeBitmap(PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(currentNum+1), 400);
						
					}
					else
					{
						if(currentNum == 0)
							BitmapList[lastView] = decodeBitmap(PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(photoCount-1), 400);
						else
							BitmapList[lastView] = decodeBitmap(PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(currentNum-1), 400);
					}
	
					ImageViewList.get(lastView).destroyDrawingCache();
					ImageViewList.get(lastView).setImageBitmap(BitmapList[lastView]);
				}
				
				if(firstView == 0)
					firstView = 2;
				else
					firstView--;
				
				if(middleView == 0)
					middleView = 2;
				else
					middleView--;
				
				if(lastView == 0)
					lastView = 2;
				else
					lastView--;
				
				if(currentNum == 0)
					currentNum = photoCount-1;
				else
					currentNum --;
				
				LeftIsTrue = false;
				once = false;
				
				flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_right_in));
				flipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.push_right_out));
				flipper.showPrevious();
				
			
			}
			
			titleTextView.setText(PhotoAlbumActivity.AlbumsFloderTitle.get(AlbumName).get(currentNum));
		}catch(Exception e){
			e.printStackTrace() ;
		}
		
		return false;
	}

	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onTouchEvent
	 * @Description: 
	 * @param event
	 * @return 
	 * @see android.app.Activity#onTouchEvent(android.view.MotionEvent)
	 */
	@Override
    public boolean onTouchEvent(MotionEvent event) {
		
    	return this.detector.onTouchEvent(event);
    }
    
	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onDown
	 * @Description: 
	 * @param e
	 * @return 
	 * @see android.view.GestureDetector.OnGestureListener#onDown(android.view.MotionEvent)
	 */
    @Override
	public boolean onDown(MotionEvent e) {

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
    
    /**
     * (非 Javadoc,覆写的方法) 
     * @Title: onShowPress
     * @Description: 
     * @param e 
     * @see android.view.GestureDetector.OnGestureListener#onShowPress(android.view.MotionEvent)
     */
    @Override
    public void onShowPress(MotionEvent e) {
    
    	
    }
    
    /**
     * (非 Javadoc,覆写的方法) 
     * @Title: onSingleTapUp
     * @Description: 
     * @param e
     * @return 
     * @see android.view.GestureDetector.OnGestureListener#onSingleTapUp(android.view.MotionEvent)
     */
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
    	
    	if(titleVisible)
    	{
    		titleVisible = false;
    		titleParent.setVisibility(View.VISIBLE);
    	}
    	else
    	{
    		titleVisible = true;
    		titleParent.setVisibility(View.INVISIBLE);
    	}
    	return false;
    }
}
