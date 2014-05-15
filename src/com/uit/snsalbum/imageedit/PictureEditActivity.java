package com.uit.snsalbum.imageedit;


import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.uit.snsalbum.bluetooth.BluetoothChat;
import com.uit.snsalbum.chat.FriendsListActivity;
import com.uit.snsalbum.entry.MainViewActivity;
import com.uit.snsalbum.entry.MyProgressDialog;
import com.uit.snsalbum.entry.R;
import com.uit.snsalbum.utils.HelpDialog;


/*
* Copyright (c) 2012,UIT-ESPACE
* All rights reserved.
*
* 文件名称：PictureEditActivity.java  
* 摘 要：图像编辑
* 
* 1.图像的编辑,包括色彩、饱和度等的调节
* 2.图像的缩放等
* 3.图像分享到人人网和保存到本地
* 4.将编辑后的图像设置为壁纸
* 5.添加边框进行美化
* 6.照片涂鸦操作
*  
* 当前版本：1.1
* 作 者：何红辉
* 完成日期：2012年11月3日
*
* 取代版本：1.0
* 原作者 ：何红辉
* 完成日期：2012年8月2日
* 
*/

public class PictureEditActivity extends Activity implements OnSeekBarChangeListener,
			 OnClickListener,OnTouchListener,android.view.GestureDetector.OnGestureListener{
	
	private ImageButton rotateBtn = null;		// 旋转按钮
	private ImageButton effectBtn = null;		// 特效按钮
	private ImageButton filterbtn = null;		// 滤镜按钮
	private ImageButton frameBtn = null;		// 添加边框按钮
	private ImageButton paletteBtn = null;		// 调色盘按钮
	private TextView palTextView = null ;		// 调色盘底面文字
	private ImageButton deleteBtn = null;		// 删除按钮
	private ImageButton loadBtn = null;			// 载入按钮

	private ImageView imgView = null;			// 图像显示控件
	private Bitmap mBitmap;						// 缓存图像
	private Bitmap alteredBitmap;				// 用于修改的图片
	private int degree = 0;						// 图片翻转的角度
	private int iOps = 0;						// 色彩模式选择索引
	private boolean isEdit = false;				// 判断是否可编辑图片
	private boolean isReloadImg = false;		// 分享图像后再Restart回调函数中是否重新载入图像

	private LinkedList<Bitmap> mBitmapList = null;
	private PopupWindow popupWindow;			// 窗口
	private GridView menuGrid;					// 菜单的网格视图
	private ToneView mToneView;  				// 色相饱和度等的类
	private ImageHandle m_ImgHandle;			// 图像效果处理
	
	private Canvas mCanvas;						// 画布,用于图像涂鸦
	private Paint mPaint;						// 画笔
	private Matrix mMatrix;						// 矩阵
	private boolean isPaint = false;			// 是否可涂鸦
	
	int column_index;							// 图像索引(Content provider)
	private static String m_ImagePath = null;	// 图像路径
	private  final int SELECT_PICTURE = 1;		// 选择图像
	private final String TAG = "图像编辑";			// TAG,用于输出调试信息
	private Uri imageFileUri;					// 图像的URI
	private String photoPath;
	 
	private MyProgressDialog mProgressDlg = null;// 图像处理时的进度条对话框
	private GestureDetector mGestureDetector;	// 手势识别对象
	private boolean isEdited = false ;			// 图片是否被编辑过的标志
	
	// 菜单数组
	private String[] menu_name_array = { "载入", "撤销", "设为壁纸", "帮助","分享", "涂鸦模式","保存","退出"};
	// 图像数组
	private int[] menu_image_array = { android.R.drawable.ic_menu_add,
					android.R.drawable.ic_menu_close_clear_cancel,
					android.R.drawable.ic_menu_compass,android.R.drawable.ic_menu_help,
					android.R.drawable.ic_menu_share,android.R.drawable.ic_menu_gallery,
					android.R.drawable.ic_menu_save,R.drawable.exit_48
		};
	
	private String[] frame_name_array = { "炫彩边框", "黑色圆圈", "细黑叶框", "绿草边框"};
	// 边框图像数组
	private int[] frame_image_array = { R.drawable.frame_small_1,
						R.drawable.frame_small_2,
						R.drawable.frame_flower_s,R.drawable.frame_small_4
					};
	
	private TextView sTextView;
	private TextView hueTextView;
	private TextView bTextView;
	private SeekBar sSeekBar;					// 饱和度seekbar
	private SeekBar hSeekBar;					// 色相seekbar
	private SeekBar bSeekBar;					// 亮度seekbar
	
	

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
		setContentView(R.layout.pictureedit);
		setTitle("图片编辑");
		
		Intent intent = getIntent();
		photoPath = intent.getStringExtra("photopath");
		
		// 初始化组件
		init_Components();		
		imgView = (ImageView)findViewById(R.id.imgSelected);
		
		 // 设置手势监听器
		 RelativeLayout mainLayout = (RelativeLayout)findViewById(R.id.edit_layout);
		 mGestureDetector = new GestureDetector(this);    		// 手势识别
	     mainLayout.setOnTouchListener(this);    				// 设置触摸监听器
	     mainLayout.setLongClickable(true);    					// 设置可长按
	     
	     mBitmapList = new LinkedList<Bitmap>();					// 最多可以撤销3步
	     
	     // 将页面添加到Set中
	     MainViewActivity.addActivityToHashSet( this );
	     	    
	     // 图像处理时的进度条对话框
	     mProgressDlg = new MyProgressDialog(this, "图像处理中,请稍后...");
	    
	     // 从相册页面传过来的图片路径photoPath
	     if( !"".equals( photoPath ) ){
	    	 loadBitmapFromPath(photoPath);
	    	 showImageView();
	    	 loadBtn.setVisibility(View.GONE);
		 }
	     
	     // 图片还未编辑过的标志
	     isEdited = false ;
	  
	}	// end of onCreate()
	

	/**
	 * @Method: init_Components
	 * @Description: 初始化组件
	 */
	private void init_Components()
	{
		// 载入按钮
		loadBtn = (ImageButton)findViewById(R.id.loadBtn);
		loadBtn.setOnClickListener( this );
		
		// 旋转按钮
		rotateBtn = (ImageButton)findViewById(R.id.btnRotate);
		rotateBtn.setOnClickListener( this );
		rotateBtn.setEnabled( false );
		
		// 特效按钮
		effectBtn = (ImageButton)findViewById(R.id.btnEffect);
		effectBtn.setOnClickListener(this);
		effectBtn.setEnabled(false);
		
		// 滤镜按钮
		filterbtn = (ImageButton)findViewById(R.id.btnFilter);
		filterbtn.setOnClickListener(this);
		filterbtn.setEnabled(false);
		
		// 边框按钮
		frameBtn = (ImageButton)findViewById(R.id.btnAddFrame);
		frameBtn.setOnClickListener(this);
		
		// 调色盘按钮
		paletteBtn = (ImageButton)findViewById(R.id.btnColor);
		paletteBtn.setOnClickListener( this );
		paletteBtn.setEnabled(false);
		// 调色盘底面文字
		palTextView = (TextView)findViewById(R.id.palTextView) ;

		// 删除按钮
		deleteBtn = (ImageButton)findViewById(R.id.btnCrop);
		deleteBtn.setOnClickListener( this );
		deleteBtn.setEnabled(false);
		
		// 色相、饱和度、亮度的标签
		sTextView = (TextView)findViewById(R.id.sTextView);
		hueTextView = (TextView)findViewById(R.id.hTextView);
		bTextView = (TextView)findViewById(R.id.bTextView);
		
		// 饱和度的SeekBar
		sSeekBar = (SeekBar)findViewById(R.id.SaturationBar);
		sSeekBar.setOnSeekBarChangeListener( this );
		
		// 色相的SeekBar
		hSeekBar = (SeekBar)findViewById(R.id.hueBar);
		hSeekBar.setOnSeekBarChangeListener(this);
		
		// 亮度的SeekBar
		bSeekBar = (SeekBar)findViewById(R.id.brightBar);
		bSeekBar.setOnSeekBarChangeListener(this);
		
		mToneView = new ToneView(PictureEditActivity.this);		// 色相  饱和度类的初始化
		
		// 涂鸦画笔的设置
    	mPaint = new Paint();
        mPaint.setDither(true);
        mPaint.setColor(0xFFFF0000);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(6);
        // 矩阵的初始化
      	mMatrix = new Matrix();
		
	}
	
		
	/**
	 * @Method: gotoGallery
	 * @Description: 进入系统图库
	 */
	private void gotoGallery()
	{
		isReloadImg = true ;
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
	
	}
	
	
	/*
	 * 功能 ： 回调,将选中的图像数据返回给当前页面，并且显示到UI上
	 * (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		
		try{			// 异常捕获,避免崩溃
			if (resultCode == Activity.RESULT_OK) {
				Log.d(TAG, "成功载入") ;
		        if (requestCode == SELECT_PICTURE && intent.getData() != null) {
		        	 		
		        	imageFileUri = intent.getData();									// 获取URI
		        	Log.d("载入图片",imageFileUri.toString());
		        	// 重新载入时将图像的列表清空
		        	if (mBitmapList.size() > 0)
		        	{
		        		mBitmapList.clear();
		        	}
		        	
		        	Log.d(TAG, "马上载入图片...") ;
		        	loadBitmap( imageFileUri );					// 获取图像
		        	
		        	m_ImagePath = getPath( imageFileUri );		// 通过uri获取图像路径
			        m_ImagePath.getBytes();
		        	Toast.makeText(this, m_ImagePath, Toast.LENGTH_SHORT).show();
		        	
			        showImageView();							// 显示图像到ImageView上
	
		        	isEdit = true;								// 图像可编辑
		        	isPaint = false;							// 不可涂鸦
	
		        	rotateBtn.setEnabled( true );
		        	paletteBtn.setEnabled(true);				// 色彩按钮启用
		        	effectBtn.setEnabled(true);
		        	deleteBtn.setEnabled(true);
		        	filterbtn.setEnabled(true);
		        	loadBtn.setVisibility(View.GONE);

		        	// 将图片添加到LinkedList中
		        	addBitmapToList() ;
		        	
		        }		
		         
			}		// end of OK
		}catch(Exception e){
			displayToast("很抱歉,图片载入出错...");
		}
 
	}		// end of onActivityResult().

	
	/**
	 * @Method: loadBitmap
	 * @Description:   载入图像,并且将图像绘制到画布上
	 * @param imageFileUri
	 */
	private void loadBitmap(Uri imageFileUri){
		
		Display currentDisplay = getWindowManager().getDefaultDisplay();
		float dw = currentDisplay.getWidth();
		float dh = currentDisplay.getHeight();
		
		// ARGB_4444 is desired
		Bitmap returnBmp = Bitmap.createBitmap((int)dw, (int)dh ,Bitmap.Config.ARGB_4444);
		
		try {
			
			// Load up the image's dimensions not the image itself
			BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
			bmpFactoryOptions.inJustDecodeBounds = true;
			
			returnBmp = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageFileUri),
																	null, bmpFactoryOptions);
		
			int heightRatio = (int)android.util.FloatMath.ceil(bmpFactoryOptions.outHeight/dh);
			int widthRatio = (int)android.util.FloatMath.ceil(bmpFactoryOptions.outWidth/dw);
			
			Log.v("HEIGHTRATIO",""+heightRatio);
			Log.v("WIDTHRATIO",""+widthRatio);
			
			/*
			 *  If both of the ratios are greater than 1, one of the sides of the image is 
			 *  greater than the screen
			 */
			if (heightRatio > 1 && widthRatio > 1){
				if (heightRatio > widthRatio){
					// Height ratio is larger, scale according to it
					bmpFactoryOptions.inSampleSize = heightRatio;
				}
				else{
				// Width ratio is larger, scale according to it
				bmpFactoryOptions.inSampleSize = widthRatio;
				}
			}
			
			// Decode it for real
			bmpFactoryOptions.inJustDecodeBounds = false;			
			mBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageFileUri), null,
    												bmpFactoryOptions);
    	
			alteredBitmap = Bitmap.createBitmap(mBitmap.getWidth(), 
    													mBitmap.getHeight(),mBitmap.getConfig());
			
        	// canvas是在这个图像上绘制的.
        	mCanvas = new Canvas( alteredBitmap );
        	mCanvas.drawBitmap(mBitmap, mMatrix, mPaint);

		}
		catch (FileNotFoundException e) {
			Log.v("ERROR",e.toString());
			displayToast("很抱歉,图像载入出错~~~") ;
		}
	
	}
	

	/**
	 * @Method: loadBitmapFromPath
	 * @Description: 用读文件方式载入图像,并且将图像绘制到画布上
	 * @param path
	 */
	private void loadBitmapFromPath(String path)
	{
	
		Display currentDisplay = getWindowManager().getDefaultDisplay();
		float dw = currentDisplay.getWidth();
		float dh = currentDisplay.getHeight();
		
		// ARGB_4444 is desired
		Bitmap returnBmp = Bitmap.createBitmap((int)dw, (int)dh ,Bitmap.Config.ARGB_4444);
		
		try {
			
			// Load up the image's dimensions not the image itself
			BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
			bmpFactoryOptions.inJustDecodeBounds = true;
			
			returnBmp = BitmapFactory.decodeFile(path, bmpFactoryOptions);
			int heightRatio = (int)android.util.FloatMath.ceil(bmpFactoryOptions.outHeight/dh);
			int widthRatio = (int)android.util.FloatMath.ceil(bmpFactoryOptions.outWidth/dw);
			
			Log.v("HEIGHTRATIO",""+heightRatio);
			Log.v("WIDTHRATIO",""+widthRatio);
			
			/*
			 *  If both of the ratios are greater than 1, one of the sides of the image is 
			 *  greater than the screen
			 */
			if (heightRatio > 1 && widthRatio > 1){
				if (heightRatio > widthRatio){
					// Height ratio is larger, scale according to it
					bmpFactoryOptions.inSampleSize = heightRatio;
				}
				else{
				// Width ratio is larger, scale according to it
				bmpFactoryOptions.inSampleSize = widthRatio;
				}
			}
			
			// Decode it for real
			bmpFactoryOptions.inJustDecodeBounds = false;
			mBitmap = BitmapFactory.decodeFile(path, bmpFactoryOptions);
    	
			alteredBitmap = Bitmap.createBitmap(mBitmap.getWidth(), 
    													mBitmap.getHeight(),mBitmap.getConfig());
			
        	// canvas是在这个图像上绘制的.
        	mCanvas = new Canvas( alteredBitmap );
        	mMatrix = new Matrix();
        	mCanvas.drawBitmap(mBitmap, mMatrix, mPaint);

		}
		catch (Exception e) {
			Log.v("ERROR",e.toString());
			displayToast("很抱歉,图像载入出错~~~") ;
		}
	
	}

	
	/**
	 * @Method: showImageView
	 * @Description:  载入图像以后显示imageView
	 */
	private void showImageView()
	{
    	Animation alphaAnimation = new AlphaAnimation(0.5f, 1.0f);  
        alphaAnimation.setDuration(100);  
        
        imgView.setImageBitmap(alteredBitmap);			// 设置图像显示控件的图片URI
        imgView.setVisibility( View.VISIBLE );			// 设置图像控件可见
    	imgView.setOnTouchListener( this );				// 设定触摸事件监听器
    	imgView.startAnimation(alphaAnimation); 
    	imgView.invalidate();
	}

	
	/*
	 *  按钮的点击事件处理
	 *  (non-Javadoc)
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		
		// 点击按钮的动画
		Animation animation = AnimationUtils.loadAnimation(this, R.anim.scale_anim);
		// 载入图片
		if (v == loadBtn)
		{
			loadBtn.startAnimation( animation );
			gotoGallery();		// 到图库选择要编辑的图像
		}
		
		// 旋转按钮
		if(v == rotateBtn)		
		{
			rotateBtn.startAnimation(animation);
			if(alteredBitmap != null)
			{
				degree += 90;
				alteredBitmap = PictureEffect.rotateBitmap(alteredBitmap, degree);		// 旋转九十度
				
				mCanvas.setBitmap(alteredBitmap);						// 修改画布源图像,否则涂鸦无效
				imgView.setImageBitmap(alteredBitmap);
				imgView.invalidate();
				try {
					// 将处理好的图片放到List里面
					addBitmapToList();
				} catch (Exception e) {
					e.printStackTrace();
					displayToast("出错啦~~~") ;
				}
				degree = 0;
				isEdited = true ;
			}
		}
		
		// 滤镜效果
		if ( v == filterbtn )
		{
			filterbtn.startAnimation(animation);
			selectFilter();		
		}
		
		// 色彩效果按钮
		if(v == effectBtn)		
		{
			effectBtn.startAnimation(animation);
			selectEffect();													// 效果选择.
		}
		
		// 给图片添加边框
		if(v == frameBtn && alteredBitmap != null)
		{
			frameBtn.startAnimation(animation);
			openAddFrameDialog();
			m_ImgHandle = new ImageHandle(PictureEditActivity.this);		// 图像处理对象
		}
		
		// 调整色相饱和度
		if( v == paletteBtn)
		{
			paletteBtn.startAnimation(animation);
			if( isEdit && alteredBitmap != null)
			{
				sTextView.setVisibility(0);
				hueTextView.setVisibility(0);
				bTextView.setVisibility(0);
				sSeekBar.setVisibility(0);
				hSeekBar.setVisibility(0);
				bSeekBar.setVisibility(0);	
				palTextView.setText("隐藏进度条") ;
			}
			
			if(!isEdit)
			{
				Log.v("TAG", "进度条隐");
				sTextView.setVisibility(View.GONE);
				hueTextView.setVisibility(View.GONE);
				bTextView.setVisibility(View.GONE);
				sSeekBar.setVisibility(View.GONE);
				hSeekBar.setVisibility(View.GONE);
				bSeekBar.setVisibility(View.GONE);
				palTextView.setText("调色盘") ;
				try {
					addBitmapToList();
				} catch (Exception e) {
					displayToast("出错啦~~~") ;
					e.printStackTrace();
				}
			}
			
			isEdit = !isEdit;
		}	// end of paletteBtn.
		
		// 删除图片
		if(deleteBtn == v && imageFileUri != null )			
		{
			deleteBtn.startAnimation(animation);
			new AlertDialog.Builder(PictureEditActivity.this)
            .setIcon(R.drawable.beaten)
            .setTitle("删除图片?")
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                @Override
				public void onClick(DialogInterface dialog, int whichButton) {
                	
                	 getContentResolver().delete(imageFileUri, null, null);		// 删除图片
                	 loadBtn.setVisibility(View.VISIBLE);
                	 imgView.setVisibility(View.GONE);
                	 displayToast("图片已删除!");
                }
            })
            .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
                @Override
				public void onClick(DialogInterface dialog, int whichButton) {

                    
                }
            }).show();
		
		}
	}

	
	/**
	 * @Method: addBitmapToList
	 * @Description: 将图像添加到列表中,便于撤销操作
	 * @throws Exception
	 */
	private void addBitmapToList() throws Exception
	{
		if (mBitmapList.size() < 5){
			mBitmapList.addLast(alteredBitmap);
			Log.d(TAG, "图片添加到LinkedList,size=" + mBitmapList.size()) ;
		}
		else{
			// 删除第一个元素
			mBitmapList.removeFirst();
			Log.d(TAG, "LinkedList元素大于5,删除表头后,size=" + mBitmapList.size()) ;
		}
	}
	
	
	/**
	 * @Method: goBack
	 * @Description:  图像的撤销操作
	 * @throws Exception
	 */
	private void goBack() throws Exception {
		int size = mBitmapList.size();
		if (size == 0) {
			displayToast("不能再撤销啦~~~");
			return;
		}

		try{
			// 先移除最新的那张图
			mBitmapList.removeLast() ;
			// 撤销操作
			alteredBitmap = Bitmap.createBitmap( mBitmapList.getLast() );
			mBitmapList.removeLast() ;
			mCanvas.setBitmap(alteredBitmap);
	
			imgView.setImageBitmap(alteredBitmap);
			imgView.invalidate();
		}catch(Exception e){
			e.printStackTrace();
		}

	}

	
	/**
	 * @Method: getPath
	 * @Description:  通过uri获取图像路径
	 * @param uri
	 * @return
	 */
		private String getPath(Uri uri) {
			
			String[] projection = { MediaColumns.DATA };
			Cursor cursor = managedQuery(uri, projection, null, null, null);
			column_index = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
			
			cursor.moveToFirst();
			m_ImagePath = cursor.getString(column_index);
		
			return cursor.getString(column_index);
		}
			
		
		/**
		 * 
		 * @Method: setPicToWallpaper 
		 * @Description: 将图像设置为壁纸  
		 * @return void  返回类型 
		 * @throws
		 */
		private void setPicToWallpaper()
		{
			if (mBitmapList.size() >= 1)
			{
				// 获取最新状态的图像
				Bitmap wallPaper = mBitmapList.getLast();
		        try{
		        	
		        	Display currentDisplay = getWindowManager().getDefaultDisplay();		// 获取屏幕大小
		        	int dw = currentDisplay.getWidth();										// 屏幕宽度
		        	int dh = currentDisplay.getHeight();									// 高度
		        	
		        	while(wallPaper.getHeight() > dh || wallPaper.getWidth() > dw)
		        	{
		        		wallPaper = m_ImgHandle.resizeBitmap(wallPaper, 0.9f);
		        	}
		        	setWallpaper( wallPaper );		// 直接设定壁纸 	
		        	
		        }catch (Exception e) {
					e.printStackTrace();
				}
		        finally{
		        	wallPaper.recycle();
		        }
			}
			else
			{
				displayToast("图像未选择...");
			}
		}
	
		
		/**
		 * @Method: displayToast
		 * @Description:  toast打印消息
		 * @param txt
		 */
	private void displayToast(String txt){
		Toast.makeText(this, txt, Toast.LENGTH_SHORT).show();
	}

	

	/**
	 * @Method: selectEffect
	 * @Description: 色彩 效果选择
	 */
	private void selectEffect()
	{
		final String[] items = getResources().getStringArray(R.array.effectItems);
			
		new AlertDialog.Builder(this)
		.setTitle("请选择效果..")  
		.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {  		// 点选了列表项的监听器
		    @Override
			public void onClick(DialogInterface dialog, int which) {  
		    	
		    	iOps = which;						// 获取选中的项
		    }  
		}) .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
            @Override
			public void onClick(DialogInterface dialog, int whichButton) {
            	
            	try {
            		// 选择色彩渲染效果,处理图像
					progressImage();
				} catch (Exception e) {
					e.printStackTrace();
				}		
            }
        })
        .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
            @Override
			public void onClick(DialogInterface dialog, int whichButton) {

        
            }
        }).show();
		
	}	// end of Effectselect().
	
	
	/**
	 * @Method: selectFilter
	 * @Description: 滤镜 效果选择
	 */
	private void selectFilter()
	{
		final String[] items = getResources().getStringArray(R.array.filterItems);
			
		new AlertDialog.Builder(this)
		.setTitle("请选择滤镜..")  
		.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() { // 点选了列表项的监听器 		
		    @Override
			public void onClick(DialogInterface dialog, int which) {  
		    	
		    	iOps = which;						// 获取选中的项
		    }  
		}) .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
            @Override
			public void onClick(DialogInterface dialog, int whichButton) {
            	
            	try {
            		// 选择色彩渲染效果,处理图像
					progressFilter();
				} catch (Exception e) {
					e.printStackTrace();
				}		
            }
        })
        .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
            @Override
			public void onClick(DialogInterface dialog, int whichButton) {

        
            }
        }).show();
		
	}	// end of selectFilter().
	
	
	/**
	 * @Method: progressImage
	 * @Description:  选择色彩效果后处理图像的线程 ,设置色彩渲染效果
	 */
	private void progressImage() throws Exception
	{
		// 显示进度条对话框
		mProgressDlg.show();
		// 处理图像的线程
		new Thread(new Runnable() {
			@Override
			public void run() {
				
				if (alteredBitmap != null)
				{
					switch( iOps )
					{
						case 0:		// 将图片进行旧风格渲染
							alteredBitmap = PictureEffect.oldRemeberEffect( alteredBitmap );	
							break;
						case 1:		// 锐化效果
							alteredBitmap = PictureEffect.sharpenEffect( alteredBitmap );
							break;
						case 2:		// 光照效果
							alteredBitmap = PictureEffect.sunshineEffect( alteredBitmap );
							break;
						case 3:		// 底片效果
							alteredBitmap = PictureEffect.filmEffect( alteredBitmap );
							break;
						case 4:		// 浮雕效果
							alteredBitmap = PictureEffect.embossEffect( alteredBitmap );
							break;
						case 5:		// 模糊效果
							alteredBitmap = PictureEffect.blurImage( alteredBitmap );
							break;
						case 6:		// 高斯模糊效果
							alteredBitmap = PictureEffect.blurImageAmeliorate(alteredBitmap );
							break;
						default:
								break;	
					}
				
				}
				
				iOps = 0;
				if ( alteredBitmap != null)
				{
					Message msg = new Message();
					msg.obj = alteredBitmap;
					imgHandler.sendMessage(msg);	// 向UI线程投递图像,更新UI线程
					isEdited = true ;
				}
				
			}
		}).start();

	}
	

	/**
	 * @Method: progressFilter
	 * @Description: 选择色彩效果后处理图像的线程 ,设置色彩渲染效果
	 */
	private void progressFilter() throws Exception
	{
		// 显示进度条对话框
		mProgressDlg.show();
		// 处理图像的线程
		new Thread(new Runnable() {
			@Override
			public void run() {
				
				if (alteredBitmap != null)
				{
					switch( iOps )
					{
						case 0:		// 冰冻滤镜
							alteredBitmap = ImageFilter.getInstance().IceFilter( alteredBitmap ).getDstBitmap();	
							break;
						case 1:		// 熔铸
							alteredBitmap = ImageFilter.getInstance().MoltenFilter( alteredBitmap ).getDstBitmap();
							break;
						case 2:		// 连环画
							alteredBitmap = ImageFilter.getInstance().ComicFilter( alteredBitmap ).getDstBitmap();
							break;
						case 3:		// 边缘高亮
							alteredBitmap = ImageFilter.getInstance().GlowingEdgeFilter( alteredBitmap ).getDstBitmap();
							break;
						
						default:
								break;	
					}
				
				}
				
				iOps = 0;
				if ( alteredBitmap != null)
				{
					Message msg = new Message();
					msg.obj = alteredBitmap;
					imgHandler.sendMessage(msg);	// 向UI线程投递图像,更新UI线程
					isEdited = true ;
				}
				
			}
		}).start();

	}
	
	
	/*
	 * 功能： 图像处理后更新UI
	 * 
	 */
	Handler imgHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			
			mProgressDlg.dismiss();						// 
			mCanvas.setBitmap(alteredBitmap);			// 修改画布源图像,否则涂鸦无效
			imgView.setImageBitmap((Bitmap)msg.obj);
			imgView.invalidate();

			try {
				// 处理完图片,保存图片的最新状态
				addBitmapToList();
			} catch (Exception e) {
				displayToast("出错啦~~~") ;
			}
		};
	};
	 
    
	/**
	 * @Method: shareToWeiBo
	 * @Description:  分享图像到人人网
	 * @throws Exception
	 */
	private void shareToWeiBo() throws Exception {
		// 如果图片没有载入,则不执行分享到微博
		if (m_ImagePath != null && alteredBitmap != null) {
			// 保存已经编辑的图像,再获取到最新的URI地址,再把URI解析成图片路径
			savePicture(); 		
			String path = getPath( imageFileUri ) ;
			Log.d("分享到微博 -图片地址 : ", path);
			
			Intent intent = new Intent(Intent.ACTION_SEND);
			// imagePath:完整路径，要带文件扩展名  
			String url = "file:///" + path;  				
			intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(url));
			intent.setType("image/jpeg");
			intent.putExtra(Intent.EXTRA_TITLE,"分享到微博");
			intent.putExtra(Intent.EXTRA_STREAM,Uri.parse(url));        
			startActivity(Intent.createChooser(intent, "分享方式"));
		} else {
			displayToast("图片未选择...");
		}

		isPaint = false;
	}

	
		float downx = 0;
		float downy = 0;
		float upx = 0;
		float upy = 0;

		/**
		 * (非 Javadoc,覆写的方法) 
		 * @Title: onTouch
		 * @Description:   触摸事件,用于绘图
		 * @param v
		 * @param event
		 * @return 
		 * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
		 */
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			
			if (alteredBitmap != null && isPaint){				// 判断是否可以绘制
				
				int action = event.getAction();
				switch (action) {
					case MotionEvent.ACTION_DOWN:
						downx = event.getX()-10;
						downy = event.getY()-10;
						break;
						
					case MotionEvent.ACTION_MOVE:
						upx = event.getX() - 5;
						upy = event.getY() - 5;
						mCanvas.drawLine(downx, downy, upx, upy, mPaint);
						imgView.invalidate();
						downx = upx;
						downy = upy;
						break;
						
					case MotionEvent.ACTION_UP:
						upx = event.getX() - 10;
						upy = event.getY() - 10;
						mCanvas.drawLine(downx, downy, upx, upy, mPaint);
						Log.d(TAG, "涂鸦");
						imgView.invalidate();		// 触发系统调用重绘UI.
						isEdited = true ;
						break;
						
					case MotionEvent.ACTION_CANCEL:
						break;
						
					default:
						break;
				}
				
				try {
					// 添加到图片列表中,用于撤销操作的
					addBitmapToList() ;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else
			{
				Log.d(TAG, "切换屏幕滑动");
				return mGestureDetector.onTouchEvent(event); 
			}
			
			return true;
		}
		
 
		/*
		 * 	功能 ： 用seekBar调整色相、饱和度、亮度
		 * (non-Javadoc)
		 * @see android.widget.SeekBar.OnSeekBarChangeListener#onProgressChanged(android.widget.SeekBar, int, boolean)
		 */
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {

			int toneFlag = 0;			
			switch (seekBar.getId())
			{
				case R.id.SaturationBar: 		// 饱和度改变
					toneFlag = 1;
					mToneView.setSaturation(progress);
					break;
				case R.id.hueBar: 				// 色相改变
					toneFlag = 2;
					mToneView.setHue(progress);
					break;
				case R.id.brightBar: 			// 亮度改变
					toneFlag = 3;
					mToneView.setLum(progress);
					break;
				default:
						break;
			}
					
			int size = mBitmapList.size();
			if (size > 0)
			{
				alteredBitmap = mToneView.handleImage(alteredBitmap, toneFlag);
			}
			
			try {
				addBitmapToList() ;
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			mCanvas.setBitmap(alteredBitmap);						// 修改画布源图像,否则涂鸦无效
			
			imgView.setImageBitmap( alteredBitmap );
			imgView.invalidate();
			isEdited = true ;
		}

		
		/**
		 * (非 Javadoc,覆写的方法) 
		 * @Title: onStartTrackingTouch
		 * @Description: 
		 * @param seekBar 
		 * @see android.widget.SeekBar.OnSeekBarChangeListener#onStartTrackingTouch(android.widget.SeekBar)
		 */
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			
		}

		/**
		 * (非 Javadoc,覆写的方法) 
		 * @Title: onStopTrackingTouch
		 * @Description: 
		 * @param seekBar 
		 * @see android.widget.SeekBar.OnSeekBarChangeListener#onStopTrackingTouch(android.widget.SeekBar)
		 */
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			
		}
		
		
		/*
		 *  功能 : 按键监听器
		 *  (non-Javadoc)
		 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
		 */
		@Override
		public boolean onKeyDown(int keyCode, KeyEvent event) {
			switch (keyCode) {
				case KeyEvent.KEYCODE_MENU:			// 按钮菜单
					openMenuPopupwin();
					break;
				case KeyEvent.KEYCODE_BACK:			// 返回效果
					// 图片未保存就退出的对话框
					confToQuit() ;
					break;
				default:
						break;
			}
			return super.onKeyDown(keyCode, event);
		}
		
		
		/**
		 * @Method: confToQuit
		 * @Description: 图片改动过后没有保存就退出的提示框
		 */
		private void confToQuit(){
			if ( isEdited ){
				new AlertDialog.Builder(this)
				.setIcon(R.drawable.beaten)
				.setTitle("图片未保存,是否保存 ?")  
				.setPositiveButton("保存", new DialogInterface.OnClickListener() {
	                @Override
					public void onClick(DialogInterface dialog, int whichButton) {
	                	// 保存图片
	                	savePicture() ;
	                }
	            })
	            .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
	                @Override
					public void onClick(DialogInterface dialog, int whichButton) {
	                	Intent intent = new Intent(PictureEditActivity.this, MainViewActivity.class);
	    				setResult(RESULT_OK, intent);
	    				finish();
	    				overridePendingTransition(R.anim.popup_enter,
	    						R.anim.popup_exit);
	                }
	            }).show();
			}else if (popupWindow != null){
				Intent intent = new Intent(PictureEditActivity.this, MainViewActivity.class);
				setResult(RESULT_OK, intent);
				finish();
				overridePendingTransition(R.anim.popup_enter,
						R.anim.popup_exit);
			}
			
		}
		
		/**
		 * @Method: getMenuAdapter
		 * @Description: 获得popupWindow菜单的适配器
		 * @param menuNameArray
		 * @param menuImageArray
		 * @return
		 */
			private ListAdapter getMenuAdapter(String[] menuNameArray,
					int[] menuImageArray) {
				
				ArrayList<HashMap<String, Object>> data = new ArrayList<HashMap<String, Object>>();
				for (int i = 0; i < menuNameArray.length; i++) {
					HashMap<String, Object> map = new HashMap<String, Object>();
					map.put("itemImage", menuImageArray[i]);
					map.put("itemText", menuNameArray[i]);
					data.add(map);
				}
				SimpleAdapter simperAdapter = new SimpleAdapter(this, data,
						R.layout.item_menu, new String[] { "itemImage", "itemText" },
						new int[] { R.id.item_image, R.id.item_text });
				return simperAdapter;

			}

			
			/**
			 * @Method: openAddFrameDialog
			 * @Description: 给图片加边框,用于选择边框
			 */
		private void openAddFrameDialog() {
				
			final int tWidth = alteredBitmap.getWidth();
			final int tHeight = alteredBitmap.getHeight();
			View menuView = View.inflate(this, R.layout.gridview_dlg, null);
			
			// 创建AlertDialog
			final AlertDialog frameDialog = new AlertDialog.Builder(this).create();
			frameDialog.setView(menuView);
				
			// 菜单的网格视图
			menuGrid = (GridView) menuView.findViewById(R.id.gridview_dlg);
			menuGrid.setAdapter(getMenuAdapter(frame_name_array, frame_image_array));	// 设置菜单和图像
			menuGrid.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
						
				switch (arg2) {
					case 0:
						frameDialog.dismiss();
						alteredBitmap = m_ImgHandle.addBigFrame(alteredBitmap, R.drawable.frame_big1);
						break;
					case 1:
						frameDialog.dismiss();
						alteredBitmap = m_ImgHandle.addBigFrame(alteredBitmap, R.drawable.frame_ciicle);
						break;
					case 2:
						frameDialog.dismiss();
						alteredBitmap = m_ImgHandle.addBigFrame(alteredBitmap, R.drawable.frame_flower);
						break;
					case 3:				
						frameDialog.dismiss();
						alteredBitmap = m_ImgHandle.addBigFrame(alteredBitmap, R.drawable.frame_4);
						break;
					case 4:
						frameDialog.dismiss();
						finish();
						break;
					default:
						break;
					}	// end of switch
						
					alteredBitmap = m_ImgHandle.resizeBitmap(alteredBitmap, tWidth, tHeight);
					mCanvas.setBitmap(alteredBitmap);						// 修改画布源图像,否则涂鸦无效
					
					imgView.setImageBitmap( alteredBitmap );
					imgView.invalidate();
					try {
						// 将修改好的图像保存到List
						addBitmapToList();
						isEdited = true ;
					} catch (Exception e) {
						displayToast("保存出错啦~~~") ;
					}
				}	// end of click.
			});	
				
			frameDialog.show();
		}
			
		
		/**
		 * @Method: openMenuPopupwin
		 * @Description: 初始化菜单popupwindow,整个页面的菜单选项
		 */
		private void openMenuPopupwin() {
			
			if(isPaint)
			{
				menu_name_array[5] = "关闭涂鸦";
			}
			else
			{
				menu_name_array[5] = "涂鸦模式";
			}
			
			LayoutInflater mLayoutInflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
			ViewGroup menuView = (ViewGroup) mLayoutInflater.inflate(R.layout.gridview_pop, null, true);
			
			menuGrid = (GridView) menuView.findViewById(R.id.gridview_popup);
			menuGrid.setAdapter(getMenuAdapter(menu_name_array, menu_image_array));
			menuGrid.requestFocus();
			
			// 菜单窗口的点击事件监听器
			menuGrid.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
						long arg3) {
					switch (arg2) {
					
					case 0:					// 去图库
						popupWindow.dismiss();
						Toast.makeText(PictureEditActivity.this, menu_name_array[0], 0).show();
						gotoGallery();		
						break;
						
					case 1:					// 撤销操作
						popupWindow.dismiss();
						try {
							goBack();
						} catch (Exception e1) {
							displayToast("不能再撤销啦~~~") ;
						}			// 撤销	
						break;
						
					case 2:					// 设置壁纸
						popupWindow.dismiss();		
						setPicToWallpaper();
						break;
						
					case 3:					// 帮助信息
						popupWindow.dismiss();
						 HelpDialog helpDlg = new HelpDialog(PictureEditActivity.this, R.string.picedit_help_text);
						 helpDlg.showHelp();
						break;
						
					case 4:					// 分享到人人网
						popupWindow.dismiss();
						try {
							// 分享到微博等各种平台
							shareToWeiBo();
						} catch (Exception e) {
							displayToast("分享失败,服务器受限...") ;
						}		
						isReloadImg = true;// 分享完图像以后重新回来该页面,进行重新载入图像
						break;
										
					case 5:					// 涂鸦模式
						popupWindow.dismiss();	
						
						if (menu_name_array[5].equals("涂鸦模式"))
						{
							pickPanColor();		// 选择画笔颜色
						}
						isPaint = !isPaint;
						break;
						
					case 6:					// 保存图像
						popupWindow.dismiss();
						savePicture();
						break;
						
					case 7:					// 退出
						popupWindow.dismiss();
						MainViewActivity.killCurrentApp( PictureEditActivity.this );
						break;
						
					default:
						break;
					}
					
				}
			});
			popupWindow = new PopupWindow(menuView, LayoutParams.FILL_PARENT,
							LayoutParams.FILL_PARENT, true);
			popupWindow.setBackgroundDrawable(new BitmapDrawable());
			popupWindow.setAnimationStyle(R.style.PopupAnimation);
			popupWindow.showAtLocation(findViewById(R.id.edit_layout), Gravity.CENTER
					| Gravity.CENTER, 0, 0);
			popupWindow.update();
		}
		
		
	
		/**
		 * @Method: pickPanColor
		 * @Description: 涂鸦模式下,画笔的颜色选择器
		 */
		private void pickPanColor()
		{
			if (alteredBitmap != null)
			{
				ColorPickerDialog dialog = new ColorPickerDialog(PictureEditActivity.this, Color.GREEN, 
						"Color Picker", 
						new ColorPickerDialog.OnColorChangedListener() {
					
					@Override
					public void colorChanged(int color) {
						
						Log.d(TAG, " Color : " + color);
						mPaint.setColor(color);				// 设置画笔颜色
						
						mBitmap = Bitmap.createBitmap(alteredBitmap);
						mCanvas.drawBitmap(mBitmap, mMatrix, mPaint);
				
					}
				});
				dialog.show();
			}
		}
				
		
		/**
		 * @Method: savePicture
		 * @Description: 保存图片
		 */
		private void savePicture()
		{
			
			if (alteredBitmap != null && isEdited ){
				String imgUrl = android.provider.MediaStore.Images.Media.insertImage(
										getContentResolver(), alteredBitmap, "SNSPic", "effectPic"); 

				Log.d("SAVE", imgUrl);
				displayToast("保存成功.");	
		
				m_ImagePath = imgUrl;
				isEdited = false ;
			}
			else
			{
				displayToast("图像未选择或者图像为编辑过...");	
			}
			
			isPaint = false;								// 不可涂鸦
			
		}	

		
	/*
	 * activity的重新启动 (non-Javadoc)
	 * 
	 * @see android.app.Activity#onRestart()
	 */
	@Override
	protected void onRestart() {

		isPaint = false; // 不可涂鸦
		isEdited = false;
		if (isReloadImg) {
			if (alteredBitmap != null) {
				mBitmapList.addLast(alteredBitmap); // 将图像添加到列表中
			}
			isReloadImg = false;
		} else {
			Log.d(TAG, "不需要重新载入图像.");
		}

		super.onRestart();

	} // end of restart().

		/**
		 * (非 Javadoc,覆写的方法) 
		 * @Title: onPause
		 * @Description:  
		 * @see android.app.Activity#onPause()
		 */
		@Override
		protected void onPause() {
	
			super.onPause() ;
		}


		/**
		 * (非 Javadoc,覆写的方法) 
		 * @Title: onStop
		 * @Description:  
		 * @see android.app.Activity#onStop()
		 */
		@Override
		protected void onStop() {
			if (mBitmapList.size() > 0)
			{
				mBitmapList.clear();
			}
			System.gc();
			super.onStop();
			
		}
		
		
		@Override
		protected void onDestroy() {
			
			super.onDestroy();
			
			if (mBitmapList.size() > 0)
			{
				mBitmapList.clear();
			}
			if(mBitmap != null)
			{
				mBitmap.recycle();
				mBitmap = null;
			}
			if (alteredBitmap != null)
			{
				alteredBitmap.recycle();
				alteredBitmap = null;
			}
		}


		/**
		 * 功能：   鼠标手势,触屏切换页面(non-Javadoc)
		 * @see android.view.GestureDetector.OnGestureListener#onDown(android.view.MotionEvent)
		 */
		@Override
		public boolean onDown(MotionEvent e) {
			return false;
		}

		
		/**
		 * 功能： 手势划动,切换屏幕
		 * 描述： 鼠标手势相当于一个向量（当然有可能手势是曲线），e1为向量的起点，e2为向量的终点，
		 * velocityX为向量水平方向的速度，velocityY为向量垂直方向的速度
		 * 
		 */
		private int verticalMinDistance = 200;  
		private int minVelocity = 0;  
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			if (e1.getX() - e2.getX() > verticalMinDistance && Math.abs(velocityX) > minVelocity) {  
				  
				 // 向右划动的手势,切换Activity  
		    	Intent cIntent = new Intent();
				cIntent.setClass(PictureEditActivity.this, BluetoothChat.class);
				startActivityForResult(cIntent, 4); 
		        overridePendingTransition(R.anim.slide_left, R.anim.slide_right);
		       
		    } else if (e2.getX() - e1.getX() > verticalMinDistance && Math.abs(velocityX) > minVelocity) {  
		    	 // 向左划动的手势
				Intent bIntent = new Intent();
				bIntent.setClass(PictureEditActivity.this, FriendsListActivity.class);
				startActivityForResult(bIntent, 5); 
		        overridePendingTransition(R.anim.slide_left, R.anim.slide_right);
		       
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
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
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
			return false;
		}

}		// end of class
