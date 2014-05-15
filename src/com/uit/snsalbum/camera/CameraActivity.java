package com.uit.snsalbum.camera;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore.Images.Media;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.uit.snsalbum.albums.PhotoAlbumActivity;
import com.uit.snsalbum.entry.LoginActivity;
import com.uit.snsalbum.entry.MainViewActivity;
import com.uit.snsalbum.entry.R;
import com.uit.snsalbum.utils.HelpDialog;


/*
* Copyright (c) 2012,UIT-ESPACE
* All rights reserved.
*
* 文件名称：CameraActivity.java
* 摘 要：拍照类
* 1.自定义拍照的实现(触摸屏幕实现自动对焦)
* 2.色彩模式的设置
* 3.闪关灯模式设置
* 4.白平衡模式设置
* 5.场景设置
* 6.自拍模式(语音读秒提示)
* 
* 当前版本：1.1
* 作 者：何红辉
* 完成日期：2012年11月3日
*
* 取代版本：1.0
* 原作者 ：何红辉
* 完成日期：2012年8月26日
*/

public class CameraActivity extends Activity implements OnTouchListener,TextToSpeech.OnInitListener,
		SurfaceHolder.Callback, Camera.PictureCallback {

	private SurfaceView cameraView;					// 高速画布
	private SurfaceHolder surfaceHolder;			// SurfaceHolder连接camera和SurfaceView的对象
	private Camera mCamera;							// Camera对象
	private Camera.Parameters mParameters;			// 设置摄像头参数的对象
	
	private TextView countdownTextView;				// 自拍模式下的倒计时显示控件
	private TextToSpeech mTts;						// TTS 自拍时的读秒
	private Handler timerUpdateHandler;				// 自拍模式下的延时Handler
	private boolean timerRunning = false;			
	private int currentTime = 5;					// 5秒定时
	
	private static int m_Effectflag = 0;			// 设置哪种摄像头参数的变量
	private boolean af = false;						// 自动对焦的布尔型变量
	
	private PopupWindow popupWindow;				// 菜单弹出的PopupWindow
	private GridView menuGrid;						// 菜单的网格显示模型
	
	// 菜单选项,拍照的主菜单选项
	private String[] menu_name_array = null;
	// 菜单图像数组
	private int[] menu_image_array = { android.R.drawable.ic_menu_set_as,
			android.R.drawable.ic_menu_agenda,
			android.R.drawable.ic_menu_help,
			android.R.drawable.ic_menu_info_details,
			android.R.drawable.ic_media_play,
			android.R.drawable.ic_menu_gallery,
			android.R.drawable.ic_menu_help,
			android.R.drawable.ic_menu_close_clear_cancel
			};
	
	
	/**********************************************************************************************
	 * 功能 ： 页面创建
	 * (non-Javadoc)
	 * @see android.app.ActivityGroup#onCreate(android.os.Bundle)
	 **********************************************************************************************/
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		// 设置为无标题模式
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.cameraview);
		// 获得拍照界面的菜单项
		menu_name_array = this.getResources().getStringArray(R.array.menuItems) ;
		
		cameraView = (SurfaceView) this.findViewById(R.id.CameraView);
		cameraView.setOnTouchListener( this );		// 设置触摸事件监听器
		
		surfaceHolder = cameraView.getHolder();		// 设置holder
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		surfaceHolder.addCallback( this );			// 设置 SurfaceHolder.Callback
		
		countdownTextView = (TextView) findViewById(R.id.CountTextView);
		
		// 初始化语音
		mTts = new TextToSpeech(CameraActivity.this, CameraActivity.this);  
		
		// 将当前页面添加到activitymap中
		MainViewActivity.addActivityToHashSet( this );	
	}

	
	/***************************************************************************************
	 *	功能：surfaceCreated的创建,在这里初始化摄像头
	 * (non-Javadoc)
	 * @see android.view.SurfaceHolder.Callback#surfaceCreated(android.view.SurfaceHolder)
	 ***************************************************************************************/
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mCamera = Camera.open();
		try {
			mCamera.setPreviewDisplay(holder);
			mParameters = mCamera.getParameters();
			if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
				mParameters.set("orientation", "portrait");

				// 摄像头预览的画面旋转90度，否则图像倒转90显示
				mCamera.setDisplayOrientation(90);
				mParameters.setRotation(90);
			}
			// 设置摄像头参数
			mCamera.setParameters(mParameters);
		} catch (IOException exception) {
			mCamera.release();
		}
	}
	
	
	/***************************************************************************************
	 *功能： 在这里设置摄像头参数
	 * (non-Javadoc)
	 * @see android.view.SurfaceHolder.Callback#surfaceChanged(android.view.SurfaceHolder, int, int, int)
	 ***************************************************************************************/
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		
		  mParameters = mCamera.getParameters();								// 获取所有参数

	       List<Size> sizes = mParameters.getSupportedPreviewSizes();
	       Size optimalSize = getOptimalPreviewSize(sizes, w, h);
	       mParameters.setPreviewSize( optimalSize.width, optimalSize.height );	// 设置显示的宽度和高度
	          
	       mCamera.setParameters( mParameters );								// 设置参数
	       mCamera.setDisplayOrientation(90);									// 设置旋转九十度
	       mCamera.startPreview();
	}

	
	/***************************************************************************************
	 *功能： 释放摄像头对象
	 * (non-Javadoc)
	 * @see android.view.SurfaceHolder.Callback#surfaceDestroyed(android.view.SurfaceHolder)
	 ***************************************************************************************/
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mCamera.stopPreview();
		mCamera.release();
	}

	/***************************************************************************************
	 *功能： 拍照后的回调函数,在这里将图像存到content provider中
	 * (non-Javadoc)
	 * @see android.hardware.Camera.PictureCallback#onPictureTaken(byte[], android.hardware.Camera)
	 ***************************************************************************************/
	@Override
	public void onPictureTaken(byte[] data, Camera mCamera) {
		
		// 将图像插入到内容提供者当中
		Uri imageFileUri = getContentResolver().insert(Media.EXTERNAL_CONTENT_URI,
							new ContentValues());
		try {
			// 将图像的真实数据写入到content provider
			OutputStream imageFileOS = getContentResolver().openOutputStream( imageFileUri );
			imageFileOS.write(data);
			imageFileOS.flush();
			imageFileOS.close();
			if ( imageFileOS != null){
				imageFileOS = null ;
			}

			Toast.makeText(this, "保存成功！", Toast.LENGTH_SHORT).show();

		} catch (FileNotFoundException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
		}
		
		mCamera.startPreview();
	}

	
	/***************************************************************************************
	 *  功能： 获取优化的预览大小
	 *  @param List<Size>		包含各种尺寸的list
	 *  @param int 				宽度
	 *  @param int 				高度
	 *  return Size				返回优化后的大小
	 ***************************************************************************************/
	 private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
	        final double ASPECT_TOLERANCE = 0.05;
	        double targetRatio = (double) w / h;
	        if (sizes == null) return null;

	        Size optimalSize = null;
	        double minDiff = Double.MAX_VALUE;

	        int targetHeight = h;

	        // Try to find an size match aspect ratio and size
	        for (Size size : sizes) {
	            double ratio = (double) size.width / size.height;
	            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
	            if (Math.abs(size.height - targetHeight) < minDiff) {
	                optimalSize = size;
	                minDiff = Math.abs(size.height - targetHeight);
	            }
	        }

	        // Cannot find the one match the aspect ratio, ignore the requirement
	        if (optimalSize == null) {
	            minDiff = Double.MAX_VALUE;
	            for (Size size : sizes) {
	                if (Math.abs(size.height - targetHeight) < minDiff) {
	                    optimalSize = size;
	                    minDiff = Math.abs(size.height - targetHeight);
	                }
	            }
	        }
	        return optimalSize;
	    }
	 
	
	 /**
	  * (非 Javadoc,覆写的方法) 
	  * @Title: onTouch
	  * @Description:  触屏拍照,当抬起手指时拍照,并且自动对焦
	  * @param v
	  * @param event
	  * @return 
	  * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
	  */
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// 按下时自动对焦
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				mCamera.autoFocus( null );
				af = true;
			}
			//放开后拍照
			if (event.getAction() == MotionEvent.ACTION_UP && af ==true) {
				// 拍照的函数
				takePictureThread();		
			}   
			
			return true;
		}	
	

		/**
		 * @Method: setColorEffect
		 * @Description: 设置摄像头的色彩效果
		 * @param index  特效的索引
		 */
	   private void setColorEffect(int index){
	    	switch(index)
	    	{
	    		case 0:
	    			Log.d("特效","无特效");
		    		mParameters.setColorEffect(Camera.Parameters.EFFECT_NONE);
		    		break;
		    	case 1:
		    		Log.d("特效","单色特效");
		    		mParameters.setColorEffect(Camera.Parameters.EFFECT_MONO);
		    		break;
		    	case 2:
		    		Log.d("特效","负片特效");
		    		mParameters.setColorEffect(Camera.Parameters.EFFECT_NEGATIVE);
		    		break;
		    	case 3:
		    		Log.d("特效","曝光特效");
		    		mParameters.setColorEffect(Camera.Parameters.EFFECT_SOLARIZE);
		    		break;
		    	case 4:
		    		Log.d("特效","老照片特效");
		    		mParameters.setColorEffect(Camera.Parameters.EFFECT_SEPIA);
		    		break;
		    	case 5:
		    		Log.d("特效","白板特效");
		    		mParameters.setColorEffect(Camera.Parameters.EFFECT_WHITEBOARD);
		    		break;
		    	case 6:
		    		Log.d("特效","黑板特效");
		    		mParameters.setColorEffect(Camera.Parameters.EFFECT_BLACKBOARD);
		    		break;
		    	case 7:
		    		Log.d("特效","浅绿色特效");
		    		mParameters.setColorEffect(Camera.Parameters.EFFECT_AQUA);
		    		break;
		    		default:
		    			break;
		    		
	    	}
	    	
	    	mCamera.setParameters(mParameters);
	    }
	    
		
	   /**
	    * @Method: setSceneMode
	    * @Description: 设置场景模式
	    * @param index  场景的索引
	    */
	    private void setSceneMode(int index){
	    	switch(index)
	    	{
	    		case 0:
	    			Log.d("场景","自动");
		    		mParameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
		    		break;
		    	case 1:
		    		Log.d("特效","动作");
		    		mParameters.setSceneMode(Camera.Parameters.SCENE_MODE_ACTION);
		    		break;
		    	case 2:
		    		Log.d("特效","肖像");
		    		mParameters.setSceneMode(Camera.Parameters.SCENE_MODE_PORTRAIT);
		    		break;
		    	case 3:
		    		Log.d("特效","风景");
		    		mParameters.setSceneMode(Camera.Parameters.SCENE_MODE_LANDSCAPE);
		    		break;

		    	case 4:
		    		Log.d("特效","夜间");
		    		mParameters.setSceneMode(Camera.Parameters.SCENE_MODE_NIGHT);
		    		break;
		    	case 5:
		    		Log.d("特效","夜间肖像");
		    		mParameters.setSceneMode(Camera.Parameters.SCENE_MODE_NIGHT_PORTRAIT);
		    		break;
		    	case 6:
		    		Log.d("特效","剧院");
		    		mParameters.setSceneMode(Camera.Parameters.SCENE_MODE_THEATRE);
		    		break;
		    	case 7:
		    		Log.d("特效","海滩");
		    		mParameters.setSceneMode(Camera.Parameters.SCENE_MODE_BEACH);
		    		break;
		
		    	case 8:
		    		Log.d("特效","日落");
		    		mParameters.setSceneMode(Camera.Parameters.SCENE_MODE_SUNSET);
		    		break;
		    	case 9:
		    		Log.d("特效","平稳图像");
		    		mParameters.setSceneMode(Camera.Parameters.SCENE_MODE_STEADYPHOTO);
		    		break;
		    	case 10:
		    		Log.d("特效","烟火");
		    		mParameters.setSceneMode(Camera.Parameters.SCENE_MODE_FIREWORKS);
		    		break;
		    	case 11:
		    		Log.d("特效","运动");
		    		mParameters.setSceneMode(Camera.Parameters.SCENE_MODE_SPORTS);
		    		break;
		    	case 12:
		    		Log.d("特效","聚会");
		    		mParameters.setSceneMode(Camera.Parameters.SCENE_MODE_PARTY);
		    		break;
		    	case 13:
		    		Log.d("特效","雪景");
		    		mParameters.setSceneMode(Camera.Parameters.SCENE_MODE_SNOW);
		    		break;
		    	default:
		    			break;
		    		
	    	}
	    	mCamera.setParameters( mParameters );
	    }
	    
		
	    /**
	     * @Method: setflashMode
	     * @Description:  设置闪关灯模式
	     * @param index   闪光灯的索引
	     */
	    private void setflashMode(int index){
	    	switch(index)
	    	{
		    	case 0:
		    		Log.v("Flash Mode", "On");
		    		mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
		    		break;
		    	case 1:
		    		Log.v("Flash Mode", "Auto");
		    		mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
		    		break;
		    	case 2:
		    		Log.v("Flash Mode", "red eye");
		    		mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_RED_EYE);
		        	break;
		        case 3:
		    		Log.v("Flash Mode", "补光");
		    		mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
		            break;
		        case 4:
		    		Log.v("Flash Mode", "关闭");
		    		mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
		            break;
		        default:
		            	break;
	    	}
	    	
	    	mCamera.setParameters(mParameters);
	    }	// end of setFlashMode().
	    
		
	    /**
	     * @Method: setWhiteBalance
	     * @Description:  设置白平衡
	     * @param index   白平衡的索引
	     */
	    private void setWhiteBalance(int index){
	    	switch(index)
	    	{
		    	case 0:
		    		mParameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
		    		break;
		    	case 1:
		    		mParameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_FLUORESCENT);
		    		break;
		    	case 2:
		    		mParameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_DAYLIGHT);
		        	break;
		        case 3:
		        	mParameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_WARM_FLUORESCENT);
		            break;
		        case 4:
		        	mParameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_DAYLIGHT);
		            break;
		        case 5:
		        	   mParameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_TWILIGHT);
		        	break;
		        case 6:
		        	   mParameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT);
		            break;
		        case 7:
		        	   mParameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_SHADE);
		            break;
		         
		        default:
		            	break;
	    	}
	    	mCamera.setParameters(mParameters);
	    }
	    
		
	    /**
	     *  定时器拍照线程,使用语音输出读秒提示信息,五秒结束后自动拍照
	     */
		private Runnable timerUpdateTask = new Runnable() {
			@Override
			public void run() {
			
				if (currentTime >= 1) {
					countdownTextView.setText("" + currentTime);
					sayTTS("" + currentTime);			//  *******************
					currentTime--;
					timerUpdateHandler.postDelayed(timerUpdateTask, 1000);
				} else {
					countdownTextView.setText("");
					takePictureThread();							// 拍照线程
					timerRunning = false;
					currentTime = 5;
					countdownTextView.setVisibility( View.GONE );	// 隐藏定时器文本
				}	
			}
		};
	 	
		
		/**
		 * @Method: setTakePictureOpition
		 * @Description: 设置拍摄效果的菜单选项,index的不同将显示不同的选择菜单
		 * @param index  功能菜单的索引
		 */
		private void setTakePictureOpition(int index){
			
			String[] menuitems = null;		// 弹出的菜单 
			switch(index)
			{
			case 1:			// 色彩效果
				m_Effectflag = 1;
				menuitems = getResources().getStringArray(R.array.colorEffectItems);
				break;
				
			case 2:			// 设置闪光灯模式
				 m_Effectflag = 2;
				 menuitems = getResources().getStringArray(R.array.flashModesItems);
	             break;

	         case 3:	 	 // 设置场景选择
		         m_Effectflag = 3;
		         menuitems = getResources().getStringArray(R.array.sceneModesItems);
		         break;

	         case 4:	     // 设置白平衡
	        	m_Effectflag = 4;
	        	menuitems = getResources().getStringArray(R.array.wBalanceModesItems);
	             break;
	 
	         case 5:	         // 自拍模式
	             m_Effectflag = 5;
	             takePictureSelf();		// 自拍
	             break;

	         case 6:		// 退出
	        	 m_Effectflag = 0; 
	         	 finish();
	             break;
				
			}
			
			Log.v("m_Effectflag",""+ m_Effectflag);
			
			new AlertDialog.Builder(this)
			.setTitle("请选择效果...")  
			.setSingleChoiceItems(menuitems, 0, new DialogInterface.OnClickListener() {  
			    @Override
				public void onClick(DialogInterface dialog, int which) {  
			    	
			         setEffectOpition( which );							// 设置拍照的效果参数
			         
			    }  
			}) .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                @Override
				public void onClick(DialogInterface dialog, int whichButton) {

                   
                }
            })
            .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
                @Override
				public void onClick(DialogInterface dialog, int whichButton) {

                    /* User clicked No so do some stuff */
                }
            }).show();
			
			//m_iOps = 0;
		}		// end of takePictureOption().
		
		
		/**
		 * @Method: setEffectOpition
		 * @Description:  设置拍照效果,which的不同将设置不同的参数
		 * @param which   相机设置的参数
		 */
		private void setEffectOpition(int which){
			// 设置相机选项	
			switch (m_Effectflag) {
			
			case 1:
					setColorEffect( which );		// 设置色彩特效
					break;
			case 2:
					setflashMode(which);			// 闪光灯模式设置
					break;
			case 3:
					setSceneMode(which);			// 场景设置
					break;
			case 4:
					setWhiteBalance( which);		// 场景设置
					break;
			default:
					break;
			}	// end of switch.
		}
		

		/**
		 * @Method: takePictureSelf
		 * @Description:  自拍函数
		 */
		private void takePictureSelf()
		{
			timerUpdateHandler = new Handler();		// Handler对象
        	countdownTextView.setVisibility(0);
        	if (!timerRunning) {
    			timerRunning = true;
    			timerUpdateHandler.post(timerUpdateTask);
    		 }
		}
		
		
		/**
		 * @Method: takePictureThread
		 * @Description: 拍照线程 
		 */
		private void takePictureThread()
		{
			new Thread(){
				@Override
				public void run() {
					super.run();
					 mCamera.takePicture(null, null, CameraActivity.this);
			         af = false;
				}
			}.start();
		}
		
		
		/**
		 * (非 Javadoc,覆写的方法) 
		 * @Title: onKeyDown
		 * @Description:  按钮按下的事件处理
		 * @param keyCode
		 * @param event
		 * @return 
		 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
		 */
		@Override
		public boolean onKeyDown(int keyCode, KeyEvent event) {
			switch (keyCode) {
			
				case KeyEvent.KEYCODE_MENU:			// 调出菜单
					openPopupwin();
					break;
					
				case KeyEvent.KEYCODE_BACK:			// 返回效果
					if (popupWindow != null)
					{
						Intent intent = new Intent(CameraActivity.this, MainViewActivity.class);
						setResult(RESULT_OK, intent);
						finish();
						overridePendingTransition(R.anim.slide_left, R.anim.slide_right);
						return false;
					}
					break;
				default:
						break;
			}
			return super.onKeyDown(keyCode, event);
		}
		
		

		/**
		 * @Method: getMenuAdapter
		 * @Description:  构造一个popupwindow 的适配器
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
			 * @Method: openPopupwin
			 * @Description: 打开功能选项菜单的popupwindow
			 */
		private void openPopupwin() {
			
			LayoutInflater mLayoutInflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
			ViewGroup menuView = (ViewGroup) mLayoutInflater.inflate(R.layout.gridview_pop, null, true);
			
			menuGrid = (GridView) menuView.findViewById(R.id.gridview_popup);
			menuGrid.setAdapter(getMenuAdapter(menu_name_array, menu_image_array));
			menuGrid.requestFocus();
			
			// 点击事件
			menuGrid.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					
					switch (arg2) {
					case 0:		// 色彩效果设置
						popupWindow.dismiss();
						setTakePictureOpition(1);
						break;
					case 1:		// 闪光灯模式设置
						popupWindow.dismiss(); 
						setTakePictureOpition(2);
						break;
					case 2:	 	 // 场景选择
						popupWindow.dismiss();						
						setTakePictureOpition(3);
					     break;

				   case 3:	     // 白平衡
					   popupWindow.dismiss();
					   setTakePictureOpition(4);
				       break;
				         
				   case 4:	 	// 自拍模式
					   	popupWindow.dismiss();
					   	takePictureSelf();
				        break;

				   case 5:		// 进入相册
					   Intent intent = new Intent(CameraActivity.this,PhotoAlbumActivity.class);
					   intent.putExtra("id", LoginActivity.mineID);
					   startActivity(intent);
					   break;
					   
				   case 6:		// 帮助
					   popupWindow.dismiss();
					   HelpDialog helpDlg = new HelpDialog(CameraActivity.this, R.string.camera_help_text);
					   helpDlg.showHelp();
					   break;
					   
				    case 7:		// 退出
				        m_Effectflag = 0; 
				        popupWindow.dismiss();
				        MainViewActivity.killCurrentApp( CameraActivity.this );	
				        break;
					default:
						break;
					}
					
				}
			});
			popupWindow = new PopupWindow(menuView, LayoutParams.FILL_PARENT,
							LayoutParams.WRAP_CONTENT, true);
			popupWindow.setBackgroundDrawable(new BitmapDrawable());
			popupWindow.setAnimationStyle(R.style.PopupAnimation);
			popupWindow.showAtLocation(findViewById(R.id.camera_layout), Gravity.CENTER
					| Gravity.BOTTOM, 0, 0);
			popupWindow.update();
		}

		
		/**
		 * (非 Javadoc,覆写的方法) 
		 * @Title: onInit
		 * @Description:   语音初始化
		 * @param status 
		 * @see android.speech.tts.TextToSpeech.OnInitListener#onInit(int)
		 */
		@Override
		public void onInit(int status) {
			
	        if (status == TextToSpeech.SUCCESS) {
	         
	            int result = mTts.setLanguage(Locale.US);
	
	            if (result == TextToSpeech.LANG_MISSING_DATA ||
	                result == TextToSpeech.LANG_NOT_SUPPORTED) {
	            
	                Log.e("TTS", "Language is not available.");
	            } else {
	            	Log.e("TTS", "Language is available.");
	            }
	        } else {
	
	            Log.e("TTS", "Could not initialize TextToSpeech.");
	        }
			
		}		// END OF INIT
		
		
		/**
		 * @Method: sayTTS
		 * @Description: 语音TTS
		 * @param words
		 */
		  private void sayTTS(String words) {
		   
		        mTts.speak(words, TextToSpeech.QUEUE_FLUSH, null);
		    }
		
		
		  /**
		   * (非 Javadoc,覆写的方法) 
		   * @Title: onStop
		   * @Description:  程序页面停止,释放资源
		   * @see android.app.Activity#onStop()
		   */
		@Override
		protected void onStop() {
			
			if (timerUpdateHandler != null)
			{
				timerUpdateHandler.removeCallbacks(timerUpdateTask);
			}
	
			// 将本页面从Set中移除
			MainViewActivity.removeFromSet( this );
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
			super.onRestart();
		}


		/**
		 * (非 Javadoc,覆写的方法) 
		 * @Title: onDestroy
		 * @Description: 程序页面销毁,释放资源
		 * @see android.app.Activity#onDestroy()
		 */
		 @Override
		    public void onDestroy() {

		        if (mTts != null) {
		            mTts.stop();
		            mTts.shutdown();
		        }

		        super.onDestroy();
		    }

}
