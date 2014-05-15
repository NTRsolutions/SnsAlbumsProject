package albums;

import java.util.ArrayList;

import album.entry.R;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Copyright (c) 2012,UIT-ESPACE( TEAM: UIT-GEEK)
 * All rights reserved.
 *
 * @Title: InLocalSmallPhotoActivity.java 
 * @Package albums 
 * @Author 徐晓佳(Mr.Abert) 
 * @E-mail:uit_xuxiaojia@163.com
 * @Version V1.0
 * @Date：2012-11-9 下午5:11:43
 * @Description:
 *
 */

public class InLocalSmallPhotoActivity extends Activity{

	private String AlbumName = null;
	private String[] albumArray = null;
	public static ImageAdapter localGridAdapter = null;
	private GridView gv = null;
	private MyHandler mHandler = null;
	private Thread tLocalAlbum = null;
	private ImageButton titleReturnButton = null;
	private TextView titleTextView = null;
	
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
		AlbumName = intent.getStringExtra("albumname");
		albumArray = intent.getStringArrayExtra("albumArray");
		setTitle("本地" + "/" + AlbumName);				// 设置标题
		
		// 初始化各种组件
		initComponents() ;
		
		tLocalAlbum = new Thread(rLocalAlbum);			// 启动一个新线程
		tLocalAlbum.start();
	}
	
	
	/**
	 * @Method: init
	 * @Description: 初始化函数
	 */
	private void initComponents(){
		
		titleReturnButton = (ImageButton)findViewById(R.id.upgridbutton1);
		titleReturnButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		titleTextView = (TextView)findViewById(R.id.upgridtextview);
		titleTextView.setText("本地/" + AlbumName + "(" + 
				PhotoAlbumActivity.AlbumsFloderName.get(AlbumName).size() + ")");

		
		localGridAdapter = new ImageAdapter(this);
		localGridAdapter.mImageList = new ArrayList<Bitmap>();
		
		gv = (GridView) findViewById(R.id.gridview); 	// GridView
        gv.setAdapter(localGridAdapter);
        // 网格图像中的点击响应，跳转到本地照片的大图浏览Activity
		gv.setOnItemClickListener(new OnItemClickListener() {  			
	     
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {  
	        
				try{
			        Intent i = new Intent(InLocalSmallPhotoActivity.this, LocalImageView.class);  
			        i.putExtra("currentpic", arg2);
			        i.putExtra("albumArray", albumArray);
			        i.putExtra("photopath", PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(arg2));
			        i.putExtra("AlbumName", AlbumName);
			        startActivity(i);  
		        }catch(Exception e){
		        	Toast.makeText(InLocalSmallPhotoActivity.this, "图像载入出错了~~~~", 0).show();
		        }
			}  
		});//ClickListener
	}
	
	
	/**
	 * 本地相册小图
	 */
	Runnable rLocalAlbum = new Runnable()								
	{
		Bitmap bm;
		@Override
		public void run()
		{
			Looper mainLooper = Looper.getMainLooper ();		// 得到主线程loop
            mHandler = new MyHandler(mainLooper);				// 创建主线程的handler
	        
	        int albumSize = PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).size();
			for(int i=0; i<albumSize && !Thread.interrupted(); i++)
			{
				bm = decodeBitmap(PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).get(i));			// 取得图片
	        	sendMessage(bm,i);
			}
		}
		public void sendMessage(Bitmap bitmap,int i){				// 线程间数据传输
            mHandler.removeMessages(0);								// 移除所有队列中的消息
            Message m = mHandler.obtainMessage(1, 1, i, bitmap);	// 把消息放入message
            mHandler.sendMessage(m);								// 发送message
		}
	};
	private class MyHandler extends Handler{       
        public MyHandler(Looper looper){
               super (looper);
        }
        @Override
        public void handleMessage(Message msg) {
        	localGridAdapter.mImageList.add((Bitmap)msg.obj);	// 把照片放入网格
        	localGridAdapter.notifyDataSetChanged();				// 更新网格
        }            
	}
	
	
	/**
	 * @Method: decodeBitmap
	 * @Description: 从文件中读取图片
	 * @param path   图片的路径
	 * @return
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
	 * (非 Javadoc,覆写的方法) 
	 * @Title: onRestart
	 * @Description:  
	 * @see android.app.Activity#onRestart()
	 */
	@Override
	protected void onRestart() {
		
		if(PhotoAlbumActivity.AlbumsFloderPath.get(AlbumName).size() == 0){
			finish();
		}
		super.onRestart();
	}
	
	
}

