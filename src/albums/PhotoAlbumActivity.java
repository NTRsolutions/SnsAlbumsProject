package albums;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import network.HttpThread;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import album.entry.LoginActivity;
import album.entry.MainViewActivity;
import album.entry.MyProgressDialog;
import album.entry.R;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.BaseColumns;
import android.provider.MediaStore.Images.Media;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;
import camera.CameraActivity;


/**
 * @ClassName: PhotoAlbumActivity 
 * @Description:  进入相册后的主界面，两个Tab，默认是网络相册，另一个是本地相册
 * @Author: xxjgood
 * @E-mail: bboyfeiyu@gmail.com 
 * @Date 2012-11-17 下午6:42:44 
 *
 */

public class PhotoAlbumActivity extends TabActivity{
	
	private MyHandler1 mHandler1 = null;			// 加载网络相册
	private MyHandler2 mHandler2 = null;			// 新建网络相册
	private MyHandler3 mHandler3 = null;			// 删除相册
	private MyHandler4 mHandler4 = null;			// 修改相册
	private Thread tNetAlbum = null;				// 加载网络相册的线程
	private Thread tNewAlbum = null;				// 新建相册的执行线程
	private Thread tDeleteAlbum = null;				// 删除相册的线程
	private Thread tModifyAlbum = null;				// 修改相册的线程
	
	private TabHost myTabhost;     					// TabHost
	private TabWidget tabWidget;   					// 设置tab的样式
	private TextView TabTV1,TabTV2;					// TabText
	private View TabView1,TabView2;					// TabView
	private ListView netAlbumListView,localAlbumListView = null;
	private SimpleAdapter tabNetAdapter,tabLocalAdapter;
	
	private ImageButton titleNetAddButton;
	private ImageButton titleLocalTakePhotoButton;
	private ImageButton titleNetRefreshButton;
	private ImageButton titleLocalRefreshButton;
	private TextView refreshTips = null ;
	private TextView localRefTips = null;
	
	private String[] albumArray = null;				// 相册名 数组
	private List<Map<String, Object>> netAlbumsList= new ArrayList<Map<String, Object>>();
	private List<Map<String, Object>> localAlbumsList= new ArrayList<Map<String, Object>>();
	
	public static List<String> AlbumsNameList = new ArrayList<String>();
	public static Map<String, List<String>> AlbumsFloderPath = new HashMap<String, List<String>>();	// 存分组的文件
	public static Map<String, List<String>> AlbumsFloderName = new HashMap<String, List<String>>();	// 存分组的文件（带扩展名）
	public static Map<String, List<String>> AlbumsFloderTitle = new HashMap<String, List<String>>();	// 存分组的文件
	public static Map<String, List<String>> AlbumsFloderTime = new HashMap<String, List<String>>();
	
	private Cursor cursor; 			// 游标，在媒体库里遍历
	private int photoIndex; 		// 这三个变量主要用来保存Media.DATA,Media.TITLE,Media.DISPLAY_NAME的索引号，来获取每列的数据 
	private int titleIndex; 
	private int nameIndex;	
	private int timeIndex;
	private int colorGray;
	private int colorBlue;
	private String ischecked;
	private String newAlbumName;
	private String ChooseAlbumName;
	private String ChooseAlbumNum;
	private int ChooseAlbumPosition;
	private String userId = "";					// 要查看的好友相册的ID或者自己的ID
	private static boolean isReload = false;
	
	private MyProgressDialog loadingDialog ;	// 更新网络相册时的进度条显示框
	public static boolean bRefresh = false;		// 是否重新请求网络相册列表
	public static Map<String, List<Long>> AlbumsFloderID = new HashMap<String, List<Long>>();
	private int idIndex;
	
	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onCreate
	 * @Description:  页面载入
	 * @param savedInstanceState 
	 * @see android.app.ActivityGroup#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	
		super.onCreate(savedInstanceState);
		//设置无标题  
        requestWindowFeature(Window.FEATURE_NO_TITLE); 
        
		userId = getIntent().getStringExtra("id");			// 获取好友的id,查看好友相册的情况下
		if (userId == null)									// 若是异常进入该页面,则ID会为空指针
		{
			finish();
		}
		
		myTabhost=this.getTabHost();						// TabHost
		tabWidget = myTabhost.getTabWidget();				// TabWidget
		
    	loadingDialog = new MyProgressDialog(this,
    					"相册加载中,请稍候···") ;					// 初始化更新相册列表时的进度条显示框	
    	
       LayoutInflater.from(this).inflate(
        		R.layout.tablistview, myTabhost.getTabContentView(), true);										// 获取view
        
        try {
        	// 设置tabHost
			SetTabHost();
		} catch (Exception e1) {
			e1.printStackTrace();
		}										// 设置TabHost
    	initAdapter();										// 初始化ListView的适配器
    	// 初始化各种组件和变量
    	initComponents() ;
    	
    	// 自己查看自己的相册时
    	if(userId == LoginActivity.mineID){
    		try {
    			// 加载网络自己的相册列表
    			getMyNetAlbums();								
    		} catch (Exception e) {
    			Log.d("载入出错", e.toString());
    			Toast.makeText(PhotoAlbumActivity.this, "亲,相册列表载入失败,正在重新获取~~~", 0).show();
    			loadingDialog.show();
    			SetOnlineAlbum();
    		}
    	}
    	else{
    		loadingDialog.show() ;
    		SetOnlineAlbum();
    	}
        
        setTitle("相册");
        
        if(userId.equals(LoginActivity.mineID)){
			try {
				SearchLocalAlbum();
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
        
	}
	
	
	/**
	 * @Method: initAdapter
	 * @Description: 适配器初始化
	 */
	private void initAdapter(){

		// userName: 用户名  ,   Name ：昵称   , img : 用户头像
		tabNetAdapter = new SimpleAdapter(this, netAlbumsList, R.layout.insidelistview,
				new String[]{"albumName","picnum","date","img1"},				// 适配器格式 
				new int[]{R.id.userName2,R.id.ipInfo2,R.id.ipInfo3,R.id.userImg2});		// 将上一个参数的数据对应赋给这个参数的三个变量
		
		netAlbumListView = (ListView) findViewById(R.id.tab1listview);
		/*实现ViewBinder()这个接口*/  
	    tabNetAdapter.setViewBinder(new ViewBinder() {  
	    	@Override  
	    	public boolean setViewValue(View view, Object data, String textRepresentation) {  
	    		if(view instanceof ImageView && data instanceof Bitmap){  
	    			ImageView i = (ImageView)view;  
	    			i.setImageBitmap((Bitmap) data);  
	    			return true;  
	    		}  
	    		return false;
	    	}
	    });
	    
		netAlbumListView.setAdapter( tabNetAdapter );
		tabNetAdapter.notifyDataSetChanged();
		
		tabLocalAdapter = new SimpleAdapter(this, localAlbumsList, R.layout.insidelistview,
				new String[]{"albumName","picnum","date","img1"},				// 适配器格式 
				new int[]{R.id.userName2,R.id.ipInfo2,R.id.ipInfo3,R.id.userImg2});		// 将上一个参数的数据对应赋给这个参数的三个变量
		
		localAlbumListView = (ListView) findViewById(R.id.tab2listview);
		/*实现ViewBinder()这个接口 */ 
	    tabLocalAdapter.setViewBinder(new ViewBinder() {  
	    	@Override  
	    	public boolean setViewValue(View view, Object data, String textRepresentation) {  
	     
	    		if(view instanceof ImageView && data instanceof Bitmap){  
	    			ImageView i = (ImageView)view;  
	    			i.setImageBitmap((Bitmap) data);  
	    			return true;  
	    		}  
	    		return false;
	    	}
	    });
		localAlbumListView.setAdapter( tabLocalAdapter );
		tabLocalAdapter.notifyDataSetChanged();
		// 设置ListView的动画
	}
	
	
	/**
	 * @Method: initComponents
	 * @Description: 初始化各种组件和变量
	 */
	private void initComponents(){
		// 网络相册更新按钮
    	titleNetRefreshButton = (ImageButton)findViewById(R.id.downnetlistbutton1);
    	titleNetRefreshButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				refreshTips.setVisibility(View.VISIBLE) ;
				refreshTips.setText("列表更新中...") ;
				// 启动线程更新列表
				new Thread(rNetAlbum).start();
			}
		});
    	
    	// 网络相册新建按钮
    	titleNetAddButton = (ImageButton)findViewById(R.id.downnetlistbutton2);
    	titleNetAddButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				refreshTips.setVisibility(View.VISIBLE) ;
				refreshTips.setText("创建相册中...") ;
				CreatAlbumDialog();
			}
		});
    	
    	// 本地相册更新按钮
    	titleLocalRefreshButton = (ImageButton)findViewById(R.id.downlocallistbutton1);
    	titleLocalRefreshButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					localRefTips.setText("本地相册更新中...") ;
					localRefTips.setVisibility(View.VISIBLE) ;
					// 更新相册列表
					SearchLocalAlbum();
					loadListAnimation(false);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
    	
    	// 拍照按钮
    	titleLocalTakePhotoButton = (ImageButton)findViewById(R.id.downlocallistbutton2);
    	titleLocalTakePhotoButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(PhotoAlbumActivity.this,CameraActivity.class);
				startActivity(intent);
			}
		});
    	
    	
    	refreshTips = (TextView)findViewById(R.id.refreshTips) ;
    	refreshTips.setVisibility(View.GONE) ;
    	
    	localRefTips = (TextView)findViewById(R.id.localRefreshTips) ;
    	localRefTips.setVisibility(View.GONE) ;

    	delayHandler = new Handler();
	}
	
	
	/**
	 * @Method: SetTabHost
	 * @Description: 设置TabHost 修改它的样式
	 */
	void SetTabHost() throws Exception{
		
		colorGray = this.getResources().getColor(R.drawable.gray);//改变字体颜色
        colorBlue = this.getResources().getColor(R.drawable.blue);
		
		myTabhost.addTab(myTabhost.newTabSpec("tab1")			// 加载一个Tab1
                .setIndicator("网络相册",getResources().getDrawable(R.drawable.world1))
                .setContent(R.id.tab1listviewl));
		tabWidget.getChildAt(0).getLayoutParams().height = 90;	// 设置Tab高度
		TabTV1 = (TextView) tabWidget.getChildAt(0).findViewById(android.R.id.title);
        TabTV1.setTextColor(colorBlue);
        TabView1 = tabWidget.getChildAt(0);   					// 设置背景
        TabView1.setBackgroundResource(R.drawable.backw);
        
        if(userId.equals(LoginActivity.mineID))
        {
	        myTabhost.addTab(myTabhost.newTabSpec("tab2")			// 加载一个Tab2
	                .setIndicator("本地相册",getResources().getDrawable(R.drawable.bank))
	                .setContent(R.id.tab2localalbum));
	        tabWidget.getChildAt(1).getLayoutParams().height = 90;	// 设置Tab高度
	        TabTV2 = (TextView) tabWidget.getChildAt(1).findViewById(android.R.id.title);
	        TabTV2.setTextColor(colorGray);
	        TabView2 = tabWidget.getChildAt(1);						// 设置背景
	        TabView2.setBackgroundResource(R.drawable.backb);
        }
        
        tabWidget.setStripEnabled(false);	 					// 去掉下边白色
        setContentView(myTabhost);								// 显示Tab

        if(userId.equals(LoginActivity.mineID))
        {
			myTabhost.setOnTabChangedListener(new OnTabChangeListener(){		// 设置Tab切换响应
				
	            @Override
				public void onTabChanged(String tabId) {

	                if(tabId.equals("tab1")){									// 如果是tab1，背景色变白
	                	Log.d("3","tab1");
	                	TabView1.setBackgroundResource(R.drawable.backw);
	                	TabView2.setBackgroundResource(R.drawable.backb);
	                	
	                	ImageView imageView = (ImageView)tabWidget.getChildAt(0).findViewById(android.R.id.icon); 
	                    imageView.setImageDrawable(getResources().getDrawable(R.drawable.world1)); 
	                	imageView = (ImageView)tabWidget.getChildAt(1).findViewById(android.R.id.icon); 
	                    imageView.setImageDrawable(getResources().getDrawable(R.drawable.bank)); 
	                    
	                	TabTV1.setTextColor(colorBlue);
	                	TabTV2.setTextColor(colorGray);
	                	// 是否需要刷新相册列表
	                	if ( bRefresh )	{        
	                		// 获取相册列表的线程
	                		new Thread(rNetAlbum).start();
	                		bRefresh = false;
	                	}
	                	loadListAnimation(true); 	// ListView动画
	                }
	                else if(tabId.equals("tab2")){							// 如果是tab2，背景色变白，并启动子线程，加载本地图片
	                	Log.d("3","tab2");
	                	TabView2.setBackgroundResource(R.drawable.backw);
	                	TabView1.setBackgroundResource(R.drawable.backb);
	
	                	ImageView imageView = (ImageView)tabWidget.getChildAt(0).findViewById(android.R.id.icon); 
	                    imageView.setImageDrawable(getResources().getDrawable(R.drawable.world)); 
	                	imageView = (ImageView)tabWidget.getChildAt(1).findViewById(android.R.id.icon); 
	                    imageView.setImageDrawable(getResources().getDrawable(R.drawable.bank1)); 
	                	
	                	TabTV2.setTextColor(colorBlue);
	                	TabTV1.setTextColor(colorGray);
	                	
	                	if( isReload )										// 是否重新加载图像
	                	{
	                		//LocalAlbum();									// 设置本地相册
	                		isReload = false;
	                		try {
								SearchLocalAlbum();
							} catch (Exception e) {
								e.printStackTrace();
							}
	                	}
	                	loadListAnimation(false); 	// ListView动画
	                }
	            }           
	        });
        }// END IF
	}
	

	/**
	 * @Method: SearchLocalAlbum
	 * @Description: 查找本地图库的路径
	 * @throws Exception
	 */
	void SearchLocalAlbum() throws Exception
	{
		String columns[] = new String[]{ 					// 指定获取的列 
				MediaColumns.DATA,
				MediaColumns.DATE_MODIFIED,
 				BaseColumns._ID,
 				MediaColumns.TITLE,
 				MediaColumns.DISPLAY_NAME };
 
		cursor = getContentResolver().query(
				 Media.EXTERNAL_CONTENT_URI, 
				 columns, null, null, null); 
		idIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID);
		photoIndex = cursor.getColumnIndexOrThrow(MediaColumns.DATA); 			// 获取列
		titleIndex = cursor.getColumnIndexOrThrow(MediaColumns.TITLE); 
		nameIndex = cursor.getColumnIndexOrThrow(MediaColumns.DISPLAY_NAME); 
		timeIndex = cursor.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED);
		cursor.moveToFirst();							// 把游标移动至最开始
		
		AlbumsFloderPath.clear();
		AlbumsFloderName.clear();
		AlbumsFloderTitle.clear();
		AlbumsFloderTime.clear();
		AlbumsNameList.clear();
		AlbumsFloderID.clear();
		do{
			String tempPath = cursor.getString(photoIndex);
			String tempName = cursor.getString(nameIndex);
			String tempTitle = cursor.getString(titleIndex);
			String tempTime = cursor.getString(timeIndex);
			long tempID = cursor.getLong(idIndex);
			
			tempPath.trim();
			String[] ArrayTemp = tempPath.split("/");
			String FolderName = ArrayTemp[ArrayTemp.length - 2];
			
			if(AlbumsFloderPath.get(FolderName) == null)
			{
				AlbumsFloderPath.put(FolderName, new ArrayList<String>());
				AlbumsFloderName.put(FolderName, new ArrayList<String>());
				AlbumsFloderTitle.put(FolderName, new ArrayList<String>());
				AlbumsFloderTime.put(FolderName, new ArrayList<String>());
				AlbumsNameList.add(FolderName);
				AlbumsFloderID.put(FolderName, new ArrayList<Long>());
			}
			AlbumsFloderPath.get(FolderName).add(tempPath);
			AlbumsFloderName.get(FolderName).add(tempName);
			AlbumsFloderTitle.get(FolderName).add(tempTitle);
			AlbumsFloderTime.get(FolderName).add(tempTime);
			AlbumsFloderID.get(FolderName).add(tempID);
			
		}while(cursor.moveToNext());
		
		localAlbumsList.clear();
		for(int i=0; i<AlbumsNameList.size(); i++){
			addFolder(AlbumsNameList.get(i), 
					AlbumsFloderPath.get(AlbumsNameList.get(i)).size(),false);
		}
		
		setOnClickListener(false);
		Log.d("debug",AlbumsFloderPath.size()+"");
		Log.d("debug",AlbumsFloderPath.get(AlbumsNameList.get(0)).size()+"");
		
		// 隐藏"相册列表更新..."的提示文本
		localRefTips.setVisibility(View.GONE) ;
	}
	

	/**
	 * @Method: SetOnlineAlbum
	 * @Description: 加载网络相册
	 */
	void SetOnlineAlbum()
	{		
		tNetAlbum = new Thread(rNetAlbum);
		tNetAlbum.start();
		
	}

	
	/**
	 * @Method: getMyNetAlbums
	 * @Description: 网络相册载入以及列表事件等,从MainViewActivity中的数组读取
	 * @throws Exception
	 */
	private void getMyNetAlbums() throws Exception{
	
		// 获取列表中第一个元素
		String title = MainViewActivity.sAlbumList.get(0);
		int size = MainViewActivity.sAlbumList.size();
		
		if( title.equals("fail") )
		{
			setTitle("没有连接服务器");
		}
		else
		{
			setTitle( title );
			if(size == 1)
			{
				if(userId.equals(LoginActivity.mineID))
					Toast.makeText(PhotoAlbumActivity.this, "亲,你还没有相册，动手整一个吧~~~", 1).show();
				else
					Toast.makeText(PhotoAlbumActivity.this, "这家伙还没有相册~~~", 1).show();
			}
				
		}

		albumArray = new String[size];
		albumArray[0] = title;
		for(int i = 1; i < size; i=i+2){
			// 将MainViewActivity中保存的网络相册列表加入到ListView中
			int itemp = 0;
			try {
				itemp = Integer.parseInt(MainViewActivity.sAlbumList.get(i+1));
			} catch (Exception e) {
				continue;
			}
			addFolder(MainViewActivity.sAlbumList.get(i), itemp, true);
			albumArray[i] = MainViewActivity.sAlbumList.get(i);
			albumArray[i+1] = MainViewActivity.sAlbumList.get(i+1);
		}
		
		
		// 点击监听器
		setOnClickListener(true);
		Log.d("1","111");
		loadListAnimation(true);
		
		if(userId.equals(LoginActivity.mineID)){
			registerForContextMenu(netAlbumListView);			// 设定上下文菜单的ListView
		}
		
	}
	

	/**
	 * @Method: loadListAnimation
	 * @Description:  设置ListView显示动画
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
        if(NetOrLocal){
        	netAlbumListView.setLayoutAnimation(controller);
        }
        else{
        	localAlbumListView.setLayoutAnimation(controller);
        }
	}
	

	/**
	 * @Method: OnlineAlbum
	 * @Description: 网络相册载入以及列表事件等
	 */
	private void OnlineAlbum() throws Exception{

		int n = albumArray.length;
		
		if(albumArray[0].equals("fail"))
		{
			setTitle("没有连接服务器");
		}
		else
		{
			setTitle(albumArray[0]);
			if(n == 1)
			{
				
				if(userId.equals(LoginActivity.mineID))
					Toast.makeText(PhotoAlbumActivity.this, "亲,你还没有相册，动手整一个吧~~~", 1).show();
				else
					Toast.makeText(PhotoAlbumActivity.this, "这家伙还没有相册为~~~", 1).show();
			}
				
		}
		
		// 首先清空数据列表,再重新载入列表
		netAlbumsList.clear();
		// 将获取到的相册列表加入到ListView中
		for(int i = 1; i < n; i=i+2){
			int itemp;
			try {
				itemp = Integer.parseInt(albumArray[i+1]);
			} catch (Exception e) {
				continue;
			}
			try {
				addFolder(albumArray[i], itemp,true);
			} catch (Exception e) {
				Log.d("添加相册列表", "添加失败") ;
				e.printStackTrace();
			}
		}
		tabNetAdapter.notifyDataSetChanged();
		
		loadListAnimation(true);
		
		netAlbumListView.setOnItemClickListener(new OnItemClickListener() {  
	        @Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,  long arg3) {
	        	
	        	Log.d("3","arg2=" + arg2);
	        	Intent intent = new Intent(PhotoAlbumActivity.this, InPhotoAlbumActivity.class);
	        	intent.putExtra("id", userId);
	    		intent.putExtra("username",  albumArray[0]);
	    		intent.putExtra("albumname", albumArray[arg2*2 + 1]);
	    		intent.putExtra("num", albumArray[arg2*2 + 2]);  		
	    		startActivity(intent);
	        }  
	    });
		
		if(userId.equals(LoginActivity.mineID))
			registerForContextMenu(netAlbumListView);			// 设定上下文菜单的ListView
		
	}
	
	/**
	 * @Method: setOnClickListener
	 * @Description: 设置监听器
	 * @param NetOrLocal
	 */
	void setOnClickListener(Boolean NetOrLocal)
	{
		if(NetOrLocal)
		{
			// 点击ListView上的监听器,点击则进入到对应的相册里面
			netAlbumListView.setOnItemClickListener(new OnItemClickListener() {  
		        @Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,  long arg3) {
		        	
		        	Log.d("3","arg2=" + arg2);
		        	Intent intent = new Intent(PhotoAlbumActivity.this, InPhotoAlbumActivity.class);
		        	intent.putExtra("id", userId);
		    		intent.putExtra("username",  albumArray[0]);
		    		intent.putExtra("albumname", albumArray[arg2*2 + 1]);
		    		intent.putExtra("num", albumArray[arg2*2 + 2]);  		
		    		startActivity(intent);
		        }  
		    });
		}
		else {
			// 点击ListView上的监听器,点击则进入到对应的相册里面
			localAlbumListView.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {

					Intent intent = new Intent(PhotoAlbumActivity.this,
							InLocalSmallPhotoActivity.class);
					intent.putExtra("albumname", AlbumsNameList.get(arg2));
					intent.putExtra("albumArray", albumArray);
					//Toast.makeText(PhotoAlbumActivity.this, albumArray.length+"长度", 1).show();
					startActivity(intent);
				}
			});
		}
	}
	
	
	/**
	 * @Method: addFolder
	 * @Description:     添加一个相册到列表
	 * @param albumName  相册名
	 * @param picnum     照片张数
	 * @param netOrLacal
	 */
	void addFolder(String albumName, int picnum, Boolean netOrLacal) throws Exception {

		Map<String, Object> map = new HashMap<String, Object>();
		if (netOrLacal) {
			map.put("albumName", albumName);
			map.put("picnum", picnum + "张");
			map.put("img1", R.drawable.ablum_03);
		} else {
			// 得到相册的路径存在stemp1
			String[] stemp = AlbumsFloderPath.get(albumName).get(0).split("/");
			String stemp1 = "/";
			for (int i = 1; i < stemp.length - 2; i++)
				stemp1 += stemp[i] + "/";

			// 得到时间
			long itemp = 0;
			if (AlbumsFloderTime.get(albumName).get(0) == null) {
				Date d = new Date();
				d.getTime();
				String sd = d.toString();
				AlbumsFloderTime.get(albumName).remove(0);
				AlbumsFloderTime.get(albumName).add(0, sd);
			} else {
				try {
					itemp = Integer.parseInt(AlbumsFloderTime.get(albumName)
							.get(0));
				} catch (Exception e) {
				}
			}

			long itemp1 = 0;
			for (int i = 1; i < picnum; i++, itemp1 = 0) {
				try {
					itemp1 = Integer.parseInt(AlbumsFloderTime.get(albumName)
							.get(i));
				} catch (Exception e) {
				}
				if (itemp < itemp1)
					itemp = itemp1;
			}

			String stemp2;
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
				// 前面的lSysTime是秒数，先乘1000得到毫秒数，再转为java.util.Date类型
				java.util.Date dt = new Date(itemp * 1000);
				// 得到精确到秒的表示：08/31/2006 21:08:00
				stemp2 = sdf.format(dt);
			} catch (Exception e) {
				stemp2 = "2012-11-15 17:40";
			}

			map.put("albumName", albumName + " (" + picnum + ")");
			map.put("picnum", stemp1);
			map.put("date", stemp2);
			map.put("img1",
					decodeBitmap(AlbumsFloderPath.get(albumName).get(0)));

		}

		if (netOrLacal) {
			netAlbumsList.add(map);
			tabNetAdapter.notifyDataSetChanged();
		} else {
			localAlbumsList.add(map);
			tabLocalAdapter.notifyDataSetChanged();
		}
	}
	
	
	/**
	 * 
	 * @Method: decodeBitmap 
	 * @Description:  读取SD卡中的图片
	 * @param path
	 * @return    参数
	 * @return Bitmap  返回类型 
	 * @throws
	 */
	Bitmap decodeBitmap(String path){ 
    	BitmapFactory.Options op = new BitmapFactory.Options(); 
    	op.inJustDecodeBounds = true; 
    	Bitmap bmp = BitmapFactory.decodeFile(path, op); 
    	int wRatio = (int)Math.ceil(op.outWidth/100); 	//获取比例大小 
    	int hRatio = (int)Math.ceil(op.outHeight/100); 
    	if(wRatio > 1 && hRatio > 1){ 					//如果超出指定大小，则缩小相应的比例
    		if(wRatio > hRatio){ 
    			op.inSampleSize = wRatio; 
    		}else{ 
    			op.inSampleSize = hRatio; 
    		} 
    	} 
    	op.inJustDecodeBounds = false; 
    	bmp = BitmapFactory.decodeFile(path, op);
    	return bmp; 
    }
	
	
	/**
	 * @Method: CreatAlbumDialog
	 * @Description: 新建相册的对话框
	 */
	public void CreatAlbumDialog(){
		
		LinearLayout CreateAlbumDialogLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.createalbumdialog, null);
		final EditText editname = (EditText) CreateAlbumDialogLayout.findViewById(R.id.newalbumname);
		final CheckBox checkshare = (CheckBox) CreateAlbumDialogLayout.findViewById(R.id.newalbumshare);
		
		Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(R.drawable.newalbums);
		builder.setTitle("创建新相册");
		builder.setView(CreateAlbumDialogLayout);
		builder.setPositiveButton("确认",
				new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog,
							int whichButton)
					{
						// 编写处理用户登录的代码
						if(editname.length() == 0)
							Toast.makeText(PhotoAlbumActivity.this, "相册名为空，您的相册创建失败,请重新创建..", 1).show();
						else{
							newAlbumName = editname.getText().toString();
							if(checkshare.isChecked())
								ischecked = "yes";
							else
								ischecked = "no";
							tNewAlbum = new Thread(rNewAlbum);
							tNewAlbum.start();
						}
					}
				});
		builder.setNegativeButton("取消",
				new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog,
							int whichButton){
						
						refreshTips.setText("取消创建！");
						delayHandler.postDelayed(delayRunnable, 2000);
					}
				});
		
		
		builder.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				Log.d("debug","return11");
				if(keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0)
				{
					refreshTips.setText("取消创建！");
					delayHandler.postDelayed(delayRunnable, 2000);
				}
				return false;
			}
		});

		builder.show();
	}
	
	
	/**
	 * @Method: AlterAlbumDialog
	 * @Description:  修改相册
	 */
	public void AlterAlbumDialog(){
		
		LinearLayout AlterAlbumDialogLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.createalbumdialog, null);
		final EditText editname = (EditText) AlterAlbumDialogLayout.findViewById(R.id.newalbumname);
		final CheckBox checkshare = (CheckBox) AlterAlbumDialogLayout.findViewById(R.id.newalbumshare);
		
		editname.setText(ChooseAlbumName);
		
		Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(R.drawable.newalbums);
		builder.setTitle("修改相册");
		builder.setView(AlterAlbumDialogLayout);
		builder.setPositiveButton("确认",
				new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog,
							int whichButton)
					{
						// 编写处理用户登录的代码
						if(editname.length() == 0)
							Toast.makeText(PhotoAlbumActivity.this, "亲,相册名为空，您的相册创修改失败...", 1).show();
						else{
							newAlbumName = editname.getText().toString();
							if(checkshare.isChecked())
								ischecked = "yes";
							else
								ischecked = "no";
							tModifyAlbum = new Thread(rModifyAlbum);
							tModifyAlbum.start();
						}
					}
				});
		builder.setNegativeButton("取消",
				new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog,
							int whichButton)
					{
						// 取消用户登录，退出程序

					}
				});
		builder.show();
	}
	
	
	
	/**
	 * 功能：HTTP请求网络相册
	 */
	Runnable rNetAlbum = new Runnable()								
	{
		String msg;
		@Override
		public void run()
		{
            ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            if(userId.equals(LoginActivity.mineID)){
            	nameValuePairs.add(new BasicNameValuePair("protocol","getAlbums"));
            }
            else{
            	nameValuePairs.add(new BasicNameValuePair("protocol","getShareAlbum"));
            }
            nameValuePairs.add(new BasicNameValuePair("id", userId));
			HttpThread h = new HttpThread(nameValuePairs,11);	// 11--请求相册列表
			msg = h.sendInfo().toString(); 						// 接收服务器的返回值
			Log.d("请求网络相册列表", msg);
			sendMessage();
		}
		
		public void sendMessage(){								// 线程间数据传输
			Looper mainLooper = Looper.getMainLooper ();		// 得到主线程loop
            mHandler1 = new MyHandler1(mainLooper);				// 创建主线程的handler
            mHandler1.removeMessages(0);						// 移除所有队列中的消息
            Message m = mHandler1.obtainMessage(1, 1, 1, msg);	// 把消息放入message
            mHandler1.sendMessage(m);							// 发送message
		}
	};
	private class MyHandler1 extends Handler{       
        public MyHandler1(Looper looper){
               super (looper);
        }
        @Override
        public void handleMessage(Message msg) {
        	String s = msg.obj.toString();
        	s = s.trim();	
        	try{
        		albumArray = s.split(";;");
        	}catch(Exception e){
        		Toast.makeText(PhotoAlbumActivity.this, "主人,网络列表请求错误~~~", 0).show();
        	}
        	
        	loadingDialog.dismiss();
        	// 将提示隐藏
        	refreshTips.setVisibility(View.GONE) ;

        	try {
            	// 载入新的数据
				OnlineAlbum();
			} catch (Exception e) {
				e.printStackTrace();
			}
        }            
	}

	/**
	 * 功能 ： 新建相册
	 */
	Runnable rNewAlbum = new Runnable()								
	{
		String msg;
		@Override
		public void run()
		{
            ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("protocol","addAlbum"));
            nameValuePairs.add(new BasicNameValuePair("id", LoginActivity.mineID));
            nameValuePairs.add(new BasicNameValuePair("albumName", newAlbumName));
            nameValuePairs.add(new BasicNameValuePair("share", ischecked));
			HttpThread h = new HttpThread(nameValuePairs, 12);	// 12--新建相册
			msg = h.sendInfo().toString(); 						// 接收服务器的返回值
			sendMessage();
		}
		public void sendMessage(){								// 线程间数据传输
			Looper mainLooper = Looper.getMainLooper ();		// 得到主线程loop
            mHandler2 = new MyHandler2(mainLooper);				// 创建主线程的handler
            mHandler2.removeMessages(0);						// 移除所有队列中的消息
            Message m = mHandler2.obtainMessage(1, 1, 1, msg);	// 把消息放入message
            mHandler2.sendMessage(m);							// 发送message
		}
	};
	private class MyHandler2 extends Handler{       
        public MyHandler2(Looper looper){
               super (looper);
        }
        @Override
        public void handleMessage(Message msg) {
        	String s = msg.obj.toString();
        	s = s.trim();	
        	if(s.equals("success")){
        		
        		try {
        			// 添加相册列表
					addFolder(newAlbumName, 0,true);
				} catch (Exception e) {
					e.printStackTrace();
				}
        		
        		String ArrayTemp[] = new String[albumArray.length+2];
        		for(int i=0;i<albumArray.length;i++)
        			ArrayTemp[i] = albumArray[i];
        		ArrayTemp[albumArray.length] = newAlbumName;
        		ArrayTemp[albumArray.length+1] = "0";
        		albumArray = new String[ArrayTemp.length];
        		albumArray = ArrayTemp;
        		
        		refreshTips.setText("成功创建相册！");
        		delayHandler.postDelayed(delayRunnable, 2000);
  
        	}
        	else if(s.equals("fail")){
        		Toast.makeText(PhotoAlbumActivity.this, "服务器拒绝了您的创建，请检查你的相册名...", 1).show();
        	}
        	else{
        		Toast.makeText(PhotoAlbumActivity.this, "您的网络貌似有点问题~~~" + s, 1).show();
        	}
        }            
	}
	
	/**
	 *  定时器
	 */
	private Handler delayHandler;
	Runnable delayRunnable=new Runnable(){
		@Override
		public void run() {
			refreshTips.setVisibility(View.GONE);
			delayHandler.removeCallbacks(delayRunnable);
		}
	};
	
	
	/**
	 * 功能 ： 删除相册
	 */
	Runnable rDeleteAlbum = new Runnable()								
	{
		String msg;
		@Override
		public void run()
		{
            ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("protocol","deleteAlbum"));
            nameValuePairs.add(new BasicNameValuePair("id", LoginActivity.mineID));
            nameValuePairs.add(new BasicNameValuePair("albumName", ChooseAlbumName));
			HttpThread h = new HttpThread(nameValuePairs,13);	// 13--删除相册
			msg = h.sendInfo().toString(); 						// 接收服务器的返回值
			sendMessage();
		}
		public void sendMessage(){							// 线程间数据传输
			Looper mainLooper = Looper.getMainLooper ();	// 得到主线程loop
            mHandler3 = new MyHandler3(mainLooper);			// 创建主线程的handler
            mHandler3.removeMessages(0);								// 移除所有队列中的消息
            Message m = mHandler3.obtainMessage(1, 1, 1, msg);	// 把消息放入message
            mHandler3.sendMessage(m);								// 发送message
		}
	};
	private class MyHandler3 extends Handler{       
        public MyHandler3(Looper looper){
               super (looper);
        }
        @Override
        public void handleMessage(Message msg) {
        	String s = msg.obj.toString();
        	s = s.trim();	
        	if(s.equals("success")){
        		
        		netAlbumsList.remove(ChooseAlbumPosition);
        		
        		for(int i=ChooseAlbumPosition*2; i<albumArray.length-4; i+=2)
        		{
        			albumArray[i+1] = albumArray[i+3];
        			albumArray[i+2] = albumArray[i+4];
        		}
        		albumArray[albumArray.length-2] = null;
        		albumArray[albumArray.length-1] = null;
        		String[] arrayTemp = new String[albumArray.length - 2];
        		for(int i=0;i<albumArray.length-2;i++) 
					arrayTemp[i] = albumArray[i];
				albumArray = new String[arrayTemp.length];
        		albumArray = arrayTemp;
        		arrayTemp = null;
        		System.gc();
        		
        		tabNetAdapter.notifyDataSetChanged();
        		
        		refreshTips.setVisibility(View.GONE) ;
        	}
        	else if(s.equals("fail")){
        		Toast.makeText(PhotoAlbumActivity.this, "服务器拒绝了您的操作...", 1).show();
        	}
        	else{
        		Toast.makeText(PhotoAlbumActivity.this, "请检查您的网络连接是否正确...", 1).show();
        	}
        }            
	}
	
	/**
	 * 功能 ： 修改相册
	 */
	Runnable rModifyAlbum = new Runnable()								
	{
		String msg;
		@Override
		public void run()
		{
            ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("protocol","changeAlbum"));
            nameValuePairs.add(new BasicNameValuePair("id", LoginActivity.mineID));
            nameValuePairs.add(new BasicNameValuePair("albumName", ChooseAlbumName));
            nameValuePairs.add(new BasicNameValuePair("newName", newAlbumName));
            nameValuePairs.add(new BasicNameValuePair("share", ischecked));
			HttpThread h = new HttpThread(nameValuePairs,14);	// 14--修改相册
			msg = h.sendInfo().toString(); 						// 接收服务器的返回值
			sendMessage();
		}
		public void sendMessage(){								// 线程间数据传输
			Looper mainLooper = Looper.getMainLooper ();		// 得到主线程loop
            mHandler4 = new MyHandler4(mainLooper);				// 创建主线程的handler
            mHandler4.removeMessages(0);						// 移除所有队列中的消息
            Message m = mHandler4.obtainMessage(1, 1, 1, msg);	// 把消息放入message
            mHandler4.sendMessage(m);							// 发送message
		}
	};
	private class MyHandler4 extends Handler{       
        public MyHandler4(Looper looper){
               super (looper);
        }
        @Override
        public void handleMessage(Message msg) {
        	String s = msg.obj.toString();
        	s = s.trim();	
        	if(s.equals("success")){
        		Map<String, Object> map = new HashMap<String, Object>();
        		map.put("albumName", newAlbumName);
        		map.put("picnum", ChooseAlbumNum);
        		map.put("img", R.drawable.ablum_03);
        		netAlbumsList.remove(ChooseAlbumPosition);
        		netAlbumsList.add(ChooseAlbumPosition, map);
        		albumArray[ChooseAlbumPosition*2+1] = newAlbumName;
        		tabNetAdapter.notifyDataSetChanged();
        	}
        	else if(s.equals("fail")){
        		Toast.makeText(PhotoAlbumActivity.this, "服务器拒绝了您的修改，请检查你的相册名...", 1).show();
        	}
        	else{
        		Toast.makeText(PhotoAlbumActivity.this, "您的网络貌似有点问题~~~", 1).show();
        	}
        }            
	}
	
	
	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onCreateOptionsMenu
	 * @Description:  创建菜单
	 * @param menu
	 * @return 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if(userId.equals(LoginActivity.mineID))
		{
			menu.add(Menu.NONE, Menu.FIRST + 1, 1, "新建相册").setIcon( android.R.drawable.ic_menu_add);
		}
        return super.onCreateOptionsMenu(menu);
	}
	
	@Override
    public boolean onMenuOpened(int featureId, Menu menu) {
		
		if(myTabhost.getCurrentTab() == 0)
			return true;												// 返回为true 则显示系统menu 
		else
			return false;												// 返回为true 则显示系统menu   
    }
		
		
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onOptionsItemSelected
	 * @Description:   菜单选择响应
	 * @param item
	 * @return 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override  
    public boolean onOptionsItemSelected(MenuItem item) {  
        switch (item.getItemId()) {  
  
	        case Menu.FIRST + 1:  					// 创建相册
	            CreatAlbumDialog();
		        break;  
	            
	        }    
        return false;  
    }   
	

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
		
		    menu.setHeaderTitle("相册操作");
	        //添加菜单项
	        menu.add(0, Menu.FIRST, 0, "修改相册");
	        menu.add(0, Menu.FIRST + 1, 0, "删除相册");
	        menu.add(0, Menu.FIRST + 2, 0, "取消");
	}
	
	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onContextItemSelected
	 * @Description:  上下文菜单选择
	 * @param item
	 * @return 
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
   @Override
	public boolean onContextItemSelected(MenuItem item) {
	   
	    // 获取当前被选择的菜单项的信息
        AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();     
        ChooseAlbumName = netAlbumsList.get(info.position).get("albumName").toString();
        ChooseAlbumNum = netAlbumsList.get(info.position).get("picnum").toString();
        ChooseAlbumPosition = info.position;
        
        switch(item.getItemId()){
        
	        case Menu.FIRST:				// 修改相册
	        	AlterAlbumDialog();
	        	
	        	break;
	        	
	        case Menu.FIRST + 1:			// 删除相册
	        	// 确认删除
				new AlertDialog.Builder(PhotoAlbumActivity.this)
	        	.setIcon(R.drawable.beaten)
				.setTitle("确认删除?")  
				.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
		            @Override
					public void onClick(DialogInterface dialog, int whichButton) {
		            	
						refreshTips.setVisibility(View.VISIBLE) ;
						refreshTips.setText("相册删除中...") ;
						
			        	tDeleteAlbum = new Thread(rDeleteAlbum);
			        	tDeleteAlbum.start();
		            }
		        })
		        .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
		            @Override
					public void onClick(DialogInterface dialog, int whichButton) {

		            }
		        }).show();

	            break;
	            
	        default:
	            break;
        }
        return true;
	}
   

   /**
    * (非 Javadoc,覆写的方法) 
    * @Title: onStop
    * @Description:   释放对象
    * @see android.app.ActivityGroup#onStop()
    */
	@Override
	protected void onStop() {

		try{
			if(tNetAlbum != null)
			{
				if (rNetAlbum != null && mHandler1 != null)
				{
					mHandler1.removeCallbacks(rNetAlbum);
				}
				tNetAlbum.interrupt();
				tNetAlbum = null;
			}
			if(tNewAlbum!=null)
			{
				if (rNetAlbum != null)
				{
					mHandler2.removeCallbacks(rNewAlbum);
				}
			
				tNewAlbum.interrupt();
				tNewAlbum = null;
			}
			if(tDeleteAlbum!=null)
			{
				if (rNetAlbum != null)
				{
					mHandler3.removeCallbacks(rDeleteAlbum);
				}
	
				tDeleteAlbum.interrupt();
				tDeleteAlbum = null;
			}
			if(tModifyAlbum!=null)
			{
				if (rNetAlbum != null)
				{
					mHandler4.removeCallbacks(rModifyAlbum);
				}
	
				tModifyAlbum.interrupt();
				tModifyAlbum = null;
			}
			
			isReload = true;
			// 将本页面从Set中移除
			MainViewActivity.removeFromSet( this );
		}catch(Exception e){
			e.printStackTrace() ;
		}
		
		super.onStop();
	}


	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onRestart
	 * @Description:  
	 * @see android.app.Activity#onRestart()
	 */
	@Override
	protected void onRestart() {
		
		Log.d("重新载入", "本地相册重新载入");
		super.onRestart();
	}  
	
}