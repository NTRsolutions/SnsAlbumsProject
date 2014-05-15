package com.uit.snsalbum.albums;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.uit.snsalbum.entry.MyProgressDialog;
import com.uit.snsalbum.entry.R;
import com.uit.snsalbum.network.HttpThread;
import com.uit.snsalbum.utils.ImageCacheToSDCard;


/**
 * 
 * @ClassName: InPhotoAlbumActivity 
 * @Description:  网格显示 网络相册里的小照片 Activity
 * @Author: xxjgood 
 * @E-mail: bboyfeiyu@gmail.com 
 * @Date 2012-11-16 下午6:39:04 
 *
 */

public class InPhotoAlbumActivity extends Activity{
	
	private MyHandler mHandler = null ;		// UI线程中的 Handler
	private Thread smallImgThread = null;				// 定义一个新线程
	private int deal = 3;					// 协议 3--请求相册中的图片名
	private String userId = null;
	private String albumname = null;		// User albumname
	private String username = null;			// User name					
	public static int photoCount = 0;		// 相册中照片的张数
	private String[] photoArray = null;		// 照片名数组, 这个地方可以删除掉
	public static ImageAdapter netGridAdapter = null;	// 适配器，显示照片	
	int gridNum = 0;						// 在网格中插入照片，记录当前是第几格
	private MyProgressDialog loadingDialog;
	private ImageCacheToSDCard mImgCache = ImageCacheToSDCard.getInstance();	// 用来清理过期缓存等
	private String TAG = "网络相册小图界面";
	private List<String> mNoCacheList = new ArrayList<String>() ; // 缓存中没有的图片
	private List<String> mNewPhotoList = new ArrayList<String>() ; // 获取缓存后新的图片数组
	private List<String> mPhotoArray = new ArrayList<String>();// 保存所有图片名称
	private GridView gv = null ;
	
	private TextView titleTextView;
	private ImageButton titleReturnButton;
	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onCreate
	 * @Description: 
	 * @param savedInstanceState 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		//设置无标题  
        requestWindowFeature(Window.FEATURE_NO_TITLE);  
		setContentView(R.layout.grid);
		
		Intent intent = getIntent();					// 获取intent 消息
		userId = intent.getStringExtra("id");
		photoCount = Integer.parseInt(intent.getStringExtra("num"));
		albumname = intent.getStringExtra("albumname");
		username = intent.getStringExtra("username");
		
		// new一个适配器，并使用自己的ImageList
		netGridAdapter = new ImageAdapter(InPhotoAlbumActivity.this);   		   
        netGridAdapter.mImageList = new ArrayList<Bitmap>();  	// 设置网格数为photoCount
        
        // 设置自定义的title

        titleTextView = (TextView)findViewById(R.id.upgridtextview);
		titleTextView.setText(albumname + "(" + photoCount + ")");
		
		titleReturnButton = (ImageButton)findViewById(R.id.upgridbutton1);
		titleReturnButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		
		LayoutGridView();								// 布局
		
		if ( photoCount != 0){
			loadingDialog = new MyProgressDialog(this, "图片加载中,请稍候···");
			loadingDialog.show();
			// 启动线程,接收小图片
			NewThreadForAsk newthreadforask = new NewThreadForAsk();
			newthreadforask.StartNewThread();
		}else{
			Toast.makeText(this, "相册为空...", 0).show() ;
			finish() ;
		}
		

	}
	

	/**
	 * @ClassName: MyHandler 
	 * @Description:  内部类  MyHandler，监听子线程消息
	 * @Author: xxjgood 
	 * @E-mail: bboyfeiyu@gmail.com 
	 * @Date 2012-11-16 下午6:39:41 
	 *
	 */
	private class MyHandler extends Handler{       
        public MyHandler(Looper looper){
               super (looper);
        }
        @Override
        public void handleMessage(Message msg) { 	// 处理消息
        	Log.d("1","handle");
        	if(deal == 3){							// 3为请求相册中的图片名
        		String s1 = msg.obj.toString();		// 获得相册中的所有照片名
        		s1 = s1.trim();						// 去空格
        		photoArray = s1.split(";;");		// 拆分字符串到数组
        		// 将数组中的数据全部存进List中
        		addToList();
        		// 从缓存中读取小图
        		try {
					getCacheImageFromSD( 1 ) ;
				} catch (Exception e) {
					Log.d(TAG, "从缓存中读取图片失败") ;
					e.printStackTrace();
				}
        		deal = 100;							// 设置deal=100，下次执行else（贴图 ）
        	}
        	else{												// 4为贴图
        		try {
        			netGridAdapter.mImageList.add( (Bitmap)msg.obj );		// 把小照片放入网格中
            		netGridAdapter.notifyDataSetChanged();					// 更新适配器，使刚贴的照片立刻显示出来
            		gridNum++;
            		if(gridNum == 1){
            			loadingDialog.dismiss();
            		}
            		
	        		Log.d("", "图片已经下载");
	        		// 把mPhotoArray锁住
	        		synchronized ( mPhotoArray ) {
	        			// 将下载下来的图片写到SD卡中
		        		if ( ! (mPhotoArray.contains("error") || photoArray[0].equals("fail") ) ){
		        			// 文件名
		        			String filename = photoArray[gridNum-1];
		            		try {
		                		// 将图片缓存到SD卡中
								mImgCache.saveBmpToSd((Bitmap)msg.obj, filename , 1);
							} catch (Exception e) {
								Log.d(TAG, filename + "写入SD卡失败!" ) ;
								e.printStackTrace();
							}
		        		} // end if
					}
	        		
	    		}catch(Exception e){
	    			Log.d(TAG, "图片转换失败") ;
	    			e.printStackTrace();
	    		}
        	}
        }            
	}
	
	
	/**
	 * 
	 * @Method: addToList 
	 * @Description: 将文件名全部存进List中
	 * @return void  返回类型 
	 * @throws
	 */
	private void addToList(){
		
		for (String item : photoArray){
			// 
			mPhotoArray.add( item );
			Log.d("图片名", item);
		}
	}
	
	
	/** 使用这个函数要将packData(int i,int tag)中的photoArray改成mPhotoArray
	 * 
	 * @Method: getCacheImageFromSD 
	 * @Description: 从缓存中读取图片
	 * @param type   图片类型,1为小图缓存,2为大图缓存
	 * @return void  返回类型 
	 * @throws
	 */
	private void getCacheImageFromSD(int type) throws Exception{
		Log.d("获取小图缓存", "从内存卡中获取");

		if ("error".equals(mPhotoArray.get(0))) {
			// 再次请求小图片的线程
			NewThreadForAsk newthreadforask = new NewThreadForAsk();	
			newthreadforask.StartNewThread();
			Toast.makeText(this, "网络不太稳定,列表获取失败,元芳,你说呢...", 1).show() ;
			return;
		}

		// 迭代,依次从缓存中读取图片
		Iterator<String> iter = mPhotoArray.iterator();
		while ( iter.hasNext() ) {
			// 获取当前文件的文件名
			String filename = iter.next().trim();
			// 启动异步任务加载图片缓存
			new CacheAsycTask().execute(filename, "small");
			Thread.sleep(100) ;

		} // end of while()

	}
	

	/**
	 * 
	 * @Method: LayoutGridView 
	 * @Description:  网格布局 ,并且设置监听器
	 * @throws
	 */
	private void LayoutGridView(){											
		gv = (GridView) findViewById(R.id.gridview); 		// GridView
        gv.setAdapter(netGridAdapter);										// 设置适配器
  
        gv.setOnItemClickListener(new OnItemClickListener() {  		// 设置监听
            @Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {  
                
                Intent i=new Intent(InPhotoAlbumActivity.this, BigImageView.class);  
                i.putExtra("userid", userId);
                i.putExtra("albumname", albumname);
                i.putExtra("username", username);
                i.putExtra("photoArray", photoArray);
                i.putExtra("currentpic", arg2);
                startActivity(i);  							// 点击网格中的某张照片后，跳转到它对应的大图Activity
            }  
        });
	}
	

	/**
	 * 
	 * @ClassName: NewThreadForAsk 
	 * @Description: 向服务器请求照片名和对象的小照片
	 * @Author: xxjgood 
	 * @E-mail: bboyfeiyu@gmail.com 
	 * @Date 2012-11-16 下午4:18:40 
	 *
	 */
	class NewThreadForAsk{
		private ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		
		/**
		 * 
		 * @Constructor: 
		 * @Description:
		 */
		public NewThreadForAsk() {
			
		}
		
		/**
		 * @Method: StartNewThread 
		 * @Description: 启动线程   
		 * @throws
		 */
		public void StartNewThread(){
			smallImgThread = new Thread( smallImgRbl );
			smallImgThread.start();
		}
		
		
		/**
		 *  Runnable的覆写
		 */
		Runnable smallImgRbl = new Runnable()							
		{
			@Override
			public void run()
			{
				Log.d("1", "new thread" + Thread.currentThread().getId());
				packData(0,0);													// 打包数据
				HttpThread h = new HttpThread(nameValuePairs,3);		// 创建网络发送类的对象，传入数据
				String s = (String)h.sendInfo();								// 向服务器发送请求，并返回数据
				Log.d("1","return" + s);
				sendMessage(s);												// 将子线程的数据发送到主线程
				
				try {
					Thread.sleep(200);// 4.24晚上修改
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
		
			/**
			 * 
			 * @Method: sendMessage 
			 * @Description: 发送String 至主线程MyHandler
			 * @param s   
			 * @throws
			 */
			public void sendMessage(String s){
                Looper mainLooper = Looper.getMainLooper ();		// 得到主线程loop
                String msg ;
                mHandler = new MyHandler(mainLooper);				// 创建主线程的handler
                msg = s;
                mHandler.removeMessages(0);							// 移除所有队列中的消息
                Message m = mHandler.obtainMessage(1, 1, 1, msg);	// 把消息放入message
                mHandler.sendMessage(m);							// 发送message
                Log.d("1","sendstring");
			}
			
	
		};
		
		
		/**
		 * @Method: packData 
		 * @Description: 打包数据
		 * @param i
		 * @param tag   
		 * @throws
		 */
		void packData(int i,int tag)
		{
			Log.d("1", "the data is packed");
			if(tag == 0){
				nameValuePairs.add(new BasicNameValuePair("protocol", "getImageName"));// 封装键值对
				nameValuePairs.add(new BasicNameValuePair("id", userId));
				nameValuePairs.add(new BasicNameValuePair("albumName", albumname));
			}
			if(tag == 1){
				Log.d("1","the data is packed tag1");
				nameValuePairs.add(new BasicNameValuePair("protocol", "getSmallImage"));// 封装键值对
				Log.d("1","fengzhuang");
				nameValuePairs.add(new BasicNameValuePair("id", userId));
				nameValuePairs.add(new BasicNameValuePair("albumName", albumname));
				Log.d("1","albumname");
				nameValuePairs.add(new BasicNameValuePair("imageName", photoArray[i]));
				Log.d("1","packend");
			}
			
		}
	}

	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onRestart
	 * @Description:  
	 * @see android.app.Activity#onRestart()
	 */
	@Override
	protected void onRestart() {
		super.onRestart();
		
		netGridAdapter.notifyDataSetChanged();			// 刷新页面,更新视图
	}
	
	
	/**
	 * 
	 * @ClassName: CacheAsycTack 
	 * @Description: 在执行execute时需要传入两个参数,第一个为要读取缓存的文件名,第二个为图片大小,"big"为大图,否则为小图
	 * @Author: Mr.Simple 
	 * @E-mail: bboyfeiyu@gmail.com 
	 * @Date 2012-11-10 下午8:20:23 
	 *
	 */
	private int temp = 0;

	private class CacheAsycTask extends AsyncTask<String, Void, Map<String, Bitmap> > {
		// 缓存路径
		private String cachePath = Environment.getExternalStorageDirectory()
											+ File.separator + "a_sns_small_cache";
		private int size = 0;

		
		/*
		 * (非 Javadoc,覆写的方法) <p>
		 * Title: onPreExecute</p> 
		 * <p>Description: </p>
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			size = mPhotoArray.size();
			Log.d(TAG, "mPhotoArray.size() =" + size);
		}

		
		/*
		 * (非 Javadoc,覆写的方法) <p>
		 * Title: doInBackground</p> 
		 * <p>Description: </p>
		 * @param params
		 * @return
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@SuppressWarnings("finally")
		@Override
		protected Map<String, Bitmap> doInBackground(String... params) {

			Map<String, Bitmap> map = new HashMap<String, Bitmap>();
			synchronized (params[0]) {
				temp++;
				Log.d(TAG, "读缓存执行了" + temp);
				// 文件名
				String fileName = params[0];
				// 读取的图片类型
				String type = params[1];
				Bitmap mBitmap = Bitmap.createBitmap(5, 5,
						Bitmap.Config.ARGB_4444);

				if ("big" == type) {
					cachePath = Environment.getExternalStorageDirectory()
							+ File.separator + "a_sns_cache";
				}

				try {

					File file = new File(cachePath, fileName);
					InputStream input = new FileInputStream(file);
					// 通过文件流构造图片
					mBitmap = BitmapFactory.decodeStream(input);
					input.close();
					// 将图片加入到map中
					map.put(fileName, mBitmap);

				} catch (Exception e) {
					// 将没有缓存的图片名称添加到mNoCacheList
					synchronized (mNoCacheList) {
						mNoCacheList.add(fileName);
						Log.d(TAG, fileName + " ****没有缓存");
					}
					e.printStackTrace();
				} finally {
					return map;
				}
			}

		}

		/*
		 * (非 Javadoc,覆写的方法)
		 * Title: onPostExecute
		 * Description: 
		 * @param result
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(Map<String, Bitmap> result) {

			synchronized (result) {
				Set<Map.Entry<String, Bitmap>> bitmap = result.entrySet();
				Iterator<Map.Entry<String, Bitmap>> iter = bitmap.iterator();
				while (iter.hasNext()) {
					Map.Entry<String, Bitmap> map = iter.next();

					if (result != null && map.getValue().getWidth() != 5
							&& map.getValue().getHeight() != 5) {
						// 获取读取缓存的结果,将图片添加到list中
						netGridAdapter.mImageList.add(map.getValue());
						netGridAdapter.notifyDataSetChanged();
						loadingDialog.dismiss();

						// 将该图片从图片列表中移除 , 即将图片名字移除
						mPhotoArray.remove(map.getKey());
						mNewPhotoList.add(map.getKey());
						Log.d(TAG, "异步任务--图片读取成功");
					} else {
						Log.d(TAG, "--异步任务读取失败");
					}
				}
			} // end of synchronized

			// 如果又缓存获取失败的,则马上用异步任务从服务器获取
			if (mNoCacheList.size() > 0 && temp == size) {
				Log.d(TAG, "启动获取小图片的**异步网络线程");
				temp = 0;
				// 向服务器请求没有缓存的图片
				//new ImageAsycTask().execute();
				mSize = mNoCacheList.size() ;
				Log.d(TAG, "mNoCacheList size = " + mSize ) ;
				for(int i=0; i<mSize; i++){
					new GetImageAsycTask().execute( i ) ;
				}
				
			} else {
				// 将新的图片名字数组导入到photoArray中
				try {
					mNewPhotoList.toArray(photoArray);
					loadingDialog.dismiss();
				} catch (Exception e) {
					Log.d(TAG, "导入photoArray失败");
					e.printStackTrace();
				}
			}

		}

	} // end of asycTack
	
	

	/**
	 * 
	 * @ClassName: GetImageAsycTask 
	 * @Description: 向服务器请求没有缓存的小图片的异步任务
	 * @Author: Mr.Simple 
	 * @E-mail: bboyfeiyu@gmail.com 
	 * @Date 2012-11-10 下午8:59:51 
	 *
	 */
	private int mSize = 0 ;
	private int mCount = 0;
	private class GetImageAsycTask extends AsyncTask<Integer, Void, Map<String,Bitmap> >{
		
		ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		private ImageCacheToSDCard mImgCache2SD = ImageCacheToSDCard.getInstance() ;
		
		@Override
		protected void onPreExecute() {
			nameValuePairs.clear();
			loadingDialog.show() ;
		}

		/*
		 * (非 Javadoc,覆写的方法) 
		 * <p>Title: doInBackground</p> 
		 * <p>Description: 从没列表集合中获取没有缓存的图片名字，并且向服务器请求该图片
		 * @param params
		 * @return 
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected Map<String,Bitmap> doInBackground(Integer... params) {
			
			Bitmap bmp = null;
			Map<String, Bitmap> map = new HashMap<String, Bitmap>();
			try {
				// 从没有缓存的列表中获取文件名
				String imgName = mNoCacheList.get(params[0]);
				nameValuePairs.add(new BasicNameValuePair("protocol",
						"getSmallImage"));// 封装键值对
				nameValuePairs.add(new BasicNameValuePair("id", userId));
				nameValuePairs.add(new BasicNameValuePair("albumName",
						albumname));
				// 向服务器请求要获取的图片,保存在List对象mPhotoArray中
				nameValuePairs
						.add(new BasicNameValuePair("imageName", imgName));

				// 想服务器请求小图片的线程,一张一张获取
				HttpThread h = new HttpThread(nameValuePairs, 100);
				bmp = (Bitmap) h.sendInfo();
				Log.d("ASYC", "向服务器请求小图" + imgName);
				// 将处理结果放到map中
				map.put(imgName, bmp);

				loadingDialog.dismiss();

			} catch (Exception e) {
				Log.d(TAG, "请求小图失败");
				e.printStackTrace();
			}

			return map;
		}

		/**
		 * (非 Javadoc,覆写的方法) 
		 * @Title: onPostExecute
		 * @Description: 
		 * @param result 
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(Map<String,Bitmap> result) {
			
			synchronized (result) {
				Set<Map.Entry<String,Bitmap> > bitmap = result.entrySet();
				Iterator<Map.Entry<String,Bitmap> > iter = bitmap.iterator() ;
				while ( iter.hasNext() ){
					Map.Entry<String,Bitmap> map = iter.next() ;
					// 获取读取缓存的结果,将图片添加到list中
					netGridAdapter.mImageList.add( map.getValue() );
					netGridAdapter.notifyDataSetChanged();
					loadingDialog.dismiss() ;
					
					Log.d(TAG, "*******请求成功********") ; 
					// 将新的文件名放到newPhotoList
					mNewPhotoList.add( map.getKey() ) ;
					synchronized (map) {
						try {
							// 将图片写入到SD卡中, 小图模式
							mImgCache2SD.saveBmpToSd(map.getValue(), map.getKey(), 1) ;
							Thread.sleep( 100 ) ;
						}catch(Exception e){
							Log.d(TAG, "写入缓存失败--ImageAsycTack") ; 
						}
						
						iter.remove() ;
					}

					// 将新的数组名依次导入到photo数组中
					if ( mSize == ++mCount){
						try {
							mNewPhotoList.toArray( photoArray ) ;
							mNoCacheList.clear() ;
							loadingDialog.dismiss() ;
							Log.d(TAG, "导入photoArray成功") ;
						}catch(Exception e){
							Log.d(TAG, "导入photoArray失败") ;
							e.printStackTrace() ;
						}
					}
					Log.d(TAG, "mCount=" + mCount) ;

				} // end of while
			} // end of synchronized
		} // end of onPostExecute
		
	} // end of GetImageAsycTask
	
}
