
package bluetooth;

import help_dlg.HelpDialog;
import imageEdit.PictureEditActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import album.entry.R;
import album.entry.MainViewActivity;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import camera.CameraActivity;
import chat.DetailAdapter;
import chat.DetailEntity;


/**
 * 
 * @ClassName: BluetoothChat 
 * @Description: 蓝牙聊天和蓝牙图像分享
 * @Author: Mr.Simple 
 * @E-mail: bboyfeiyu@gmail.com 
 * @Date 2012-11-9 下午2:39:59 
 *
 */

public class BluetoothChat extends Activity implements OnTouchListener,
											android.view.GestureDetector.OnGestureListener{
    // 调试信息
    private static final String TAG = "BluetoothChat";
    private static final boolean bDebug = true;

    // 从对方发来的消息类型.
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // 从 BluetoothChatService接收到的设备名称
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "TOAST";

    // Intent请求码
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int PICK_PICTURE = 3;						// 选择图片的CODE
    
    private int column_index;										// 图像索引
    private String imagePath = "NULL";								// 图像路径

    // Layout 视图
    private TextView mTitle;
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    
    private ArrayList<DetailEntity> conversationList = null;		// 对话列表
	private DetailEntity mMsgEntity = null;							// 消息实体类对象
	private DetailAdapter mConversationAdapter = null;				// 适配器对象

    // 已连接的设备名称
    private String mConnectedDeviceName = null;
    // 聊天线程中的适配器名称数组
    //private ArrayAdapter<String> mConversationArrayAdapter;
    // 输出信息的StringBuffer
    private StringBuffer mOutStringBuffer;
    // 本地蓝牙设备适配器
    private BluetoothAdapter mBluetoothAdapter = null;
    // BluetoothChatService的对象
    private BluetoothChatService mChatService = null;

    
	/**
	 * 功能 ： 页面创建
	 * (non-Javadoc)
	 * @see android.app.ActivityGroup#onCreate(android.os.Bundle)
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(bDebug) Log.e(TAG, "+++ ON CREATE +++");

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.bluetooth);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        // 设置自定义的标题栏
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText("蓝牙");
        mTitle = (TextView) findViewById(R.id.title_right_text);

        // 获取默认的蓝牙适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // 如果适配器为Null，则提示不可用
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "蓝牙不可用.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
     		
        MainViewActivity.addActivityToHashSet( this );	// 将当前页面添加到activitymap中
        final LinearLayout layout = (LinearLayout) findViewById(R.id.bluetoothLayout);
	    MainViewActivity.checkSkin(layout);
      
    }

    
    /**
     * 功能： activity Onstart
     * 
     */
    @Override
    public void onStart() {
        super.onStart();
        if(bDebug) Log.e(TAG, "++ ON START ++");

        // 如果蓝牙没有启动,则设置蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

        } else {
            if (mChatService == null) 
            	setupChat();
        }
    }

    
    /**
     * 功能： onResume
     * 
     */
    @Override
    public synchronized void onResume() {
        super.onResume();
        if(bDebug) Log.e(TAG, "+ ON RESUME +");

       // 如果BluetoothChatService对象不为空,则获取BluetoothChatService对象的状态
        if (mChatService != null) {
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
            	
            // 启动BluetoothChatService对象服务 
              mChatService.start();
            }
        }
    }

    
    /**
     * 功能： 初始化蓝牙设备
     * 
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");
     	        
        // 初始化消息编辑器,并且设置监听器
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // 初始化发送按钮
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            @Override
			public void onClick(View v) {
               // 发送消息给对方
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                sendMessage(message);
            }
        });

        // 初始化BluetoothChatService对象来执行蓝牙连接
        mChatService = new BluetoothChatService(this, mHandler);

        // 初始化输出信息的StringBuffer为空
        mOutStringBuffer = new StringBuffer("");
    }

    
    /**
     * 功能： 页面onPause
     * 
     */
    @Override
    public synchronized void onPause() {
        super.onPause();
        if(bDebug)
        	Log.e(TAG, "- ON PAUSE -");
    }
    

    /**
     * 功能： 页面ONSTOP
     * 
     */
    @Override
    public void onStop() {
        super.onStop();
        if(bDebug) 
        	Log.e(TAG, "-- ON STOP --");
        // 关闭蓝牙
        mBluetoothAdapter.disable() ;
		// 将本页面从Set中移除
		MainViewActivity.removeFromSet( this );
    }

    
    /**
     * 功能：页面销毁,停止蓝牙服务线程
     * 
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null)
        	mChatService.stop();
        if(bDebug) 
        	Log.e(TAG, "--- ON DESTROY ---");
    }

    
    /**
     * 功能： 使蓝牙可以发现
     * 
     */
    private void ensureDiscoverable() {			
        if(bDebug)
        	Log.d(TAG, "使蓝牙可发现");
        // 如果蓝牙设备不是可连接和可发现的,则调用intent跳转到蓝牙设置界面
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
        	
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    
    /**
     * 
     * @Method: sendMessage 
     * @Description: 通过蓝牙发送 文字消息
     * @param message   
     * @return void  返回类型 
     * @throws
     */
    private void sendMessage(String message) {
    	
        // 判断蓝牙状态,没有连接则退出
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // 判断消息的长度
        if (message.length() > 0) {
         
            byte[] byteData = message.getBytes();	// 转换成字节流
            mChatService.write( byteData );			// 发送消息
            
            // 重置StringBuffer对象
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
            
            addConversationMsg("我说: ", message, 1);// 添加到聊天列表
        }
    }

    /**
     * 功能 ：初始化聊天的气泡消息列表和适配器等
     * 
     */
    private void initConversationList()
    {
        // 在这里获取系统时间
   		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss");     
   		Date curDate = new Date(System.currentTimeMillis());		//获取当前时间     
   		String date = formatter.format(curDate); 
   		Log.d(TAG, "时间 : " + date);
   		   
        conversationList = new ArrayList<DetailEntity>();
           
        DetailEntity tips = new DetailEntity("提示信息","2010-04-26","已连接,可进行聊天.",
   											R.layout.list_say_me_item);
        conversationList.add(tips);
           
        // 适配器
        mConversationAdapter = new DetailAdapter(this, conversationList);
        mConversationView = (ListView)findViewById(R.id.conversationList); 	// 蓝牙聊天里面的布局
        mConversationView.setAdapter(mConversationAdapter);
        // 设置列表的触摸事件监听器,用于手势发送图片.
        mConversationView.setOnTouchListener(this);
        
    }
    
    
    /**
	 *  功能： 将消息添加到气泡列表
	 *  
	 */
   private void addConversationMsg(String name, String content , int layoutId)
   {
	   // 在这里获取系统时间
	   SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");     
	   Date curDate = new Date(System.currentTimeMillis());		//获取当前时间     
	   String date = formatter.format(curDate); 
	   Log.d(TAG, "时间 : " + date);
	   
	   // 我自己发出去的消息
	   if ( layoutId == 1)
	   {
		// 添加对话
			mMsgEntity = new DetailEntity(name, date,content, R.layout.list_say_me_item);
	       
	   }
	   else
	   {
		   mMsgEntity = new DetailEntity(name, date, content, R.layout.list_say_he_item);
	   }
	   
	   conversationList.add( mMsgEntity );
       mConversationView.setAdapter(new DetailAdapter(BluetoothChat.this, conversationList));
       
   }
   
    /**
     *  功能: 消息编辑器的监听,可按回车发送.
     *  
     */
    private TextView.OnEditorActionListener mWriteListener =
        new TextView.OnEditorActionListener() {
        @Override
		public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // 按回车时发送消息
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);			// 发送消息
            }
            if(bDebug) Log.i(TAG, "END onEditorAction");
            return true;
        }
    };
    
    
    /**
     *  功能： 利用多线程进行蓝牙操作，从BluetoothChatService中返回蓝牙信息
     *  
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            
            case MESSAGE_STATE_CHANGE:								// 蓝牙状态改变
                if(bDebug)
                	Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                
                switch (msg.arg1) {
                
	                case BluetoothChatService.STATE_CONNECTED:		// 蓝牙已经连接
	                    mTitle.setText(R.string.title_connected_to);
	                    mTitle.append(mConnectedDeviceName);
	                    
	                    Log.d(TAG, "蓝牙已经连接");
	                    //mConversationAdapter.clear();				// 先清空原来的消息列表
	                    initConversationList();						// 蓝牙连接后初始化信息列表等
	                    
	                    break;
	                    
	                case BluetoothChatService.STATE_CONNECTING:		// 蓝牙正在连接
	                    mTitle.setText(R.string.title_connecting);
	                    break;
	                    
	                case BluetoothChatService.STATE_LISTEN:			// 蓝牙正在监听
	                case BluetoothChatService.STATE_NONE:
	                    mTitle.setText(R.string.title_not_connected);
	                    break;
	                }
                break;
                
            case MESSAGE_WRITE:										// 蓝牙的写操作,即传送字节流,发送消息
                byte[] writeBuf = (byte[]) msg.obj;
                String writeMessage = new String (writeBuf );
         
                break;
                
            case MESSAGE_READ:										// 蓝牙的读操作,即接收信息
                byte[] readBuf = (byte[]) msg.obj;					// 从Handler提交来的消息中读取消息.转换成字节流
                // 从buffer里构造一个字符串
                String readMessage = new String(readBuf, 0, msg.arg1);
                addConversationMsg(mConnectedDeviceName + " 说 : ", readMessage, 2);
                //mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                break;
                
            case MESSAGE_DEVICE_NAME:
                // 保存已经连接的设备名
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "已连接到 "
                               + mConnectedDeviceName + "!", Toast.LENGTH_SHORT).show();
                break;
                
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               								Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    
    /**
     * 功能： 回调函数,处理各类Activity事件
     * (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     * 
     */
    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent intentData) {
        if(bDebug) 
        	Log.d(TAG, "onActivityResult " + resultCode);
        
        switch (requestCode) {
        
        case REQUEST_CONNECT_DEVICE:		// 连接蓝牙
         
            if (resultCode == Activity.RESULT_OK) {
                // 获取蓝牙设备的MAC地址
                String address = intentData.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // 获取远程蓝牙设备的地址
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // 连接远程蓝牙设备
                mChatService.connect(device);
            }
            break;
            
        case REQUEST_ENABLE_BT:			// 使蓝牙启动

            if (resultCode == Activity.RESULT_OK) {
                // 蓝牙已经启动,则初始化
                setupChat();
            } else {
                // 蓝牙没有开启
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
            break;
            
        case PICK_PICTURE:		// 从图库获取的图片数据
        	
        	if (intentData == null)
        	{
        		break;
        	}
        	Uri imageFileUri = intentData.getData();
        	
        	try {
	        	
	        	imagePath = getPath( imageFileUri );			// 获得所选图像的路径
	        	Log.v(TAG, "从图库中选择了: " + imagePath);
            
        	}catch (Exception e) {
				e.printStackTrace();
			}
        	break;
        }
    }

    
    	/**
    	 * 功能 ：获取图片的路径
    	 * @pamara Uri 图像的uri
    	 * return  图像的路径
    	 */
 		private String getPath(Uri uri) {
 			
 			String[] projection = { MediaColumns.DATA };
 			Cursor cursor = managedQuery(uri, projection, null, null, null);
 			column_index = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
 			
 			cursor.moveToFirst();
 			imagePath = cursor.getString(column_index);
 		
 			return cursor.getString(column_index);
 		}
 		
 		
    /**
     *  功能： 创建菜单
     *  (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     * 
     */
 	@Override
     public boolean onCreateOptionsMenu(Menu menu) {

         // setIcon()方法为菜单设置图标，这里使用的是系统自带的图标
 		menu.add(Menu.NONE, Menu.FIRST + 1, 1, "扫描").setIcon(
 	    android.R.drawable.ic_menu_search);

 		menu.add(Menu.NONE, Menu.FIRST + 2, 2, "可发现").setIcon(
 		android.R.drawable.ic_menu_mylocation);
 		
 		menu.add(Menu.NONE, Menu.FIRST + 3, 3, "分享靓照").setIcon(
 		 		android.R.drawable.ic_menu_share);
 		
 		menu.add(Menu.NONE, Menu.FIRST + 4, 4, "关闭蓝牙").setIcon(
 		 		android.R.drawable.ic_menu_close_clear_cancel);
 		menu.add(Menu.NONE, Menu.FIRST + 5 , 5, "帮助").setIcon(android.R.drawable.ic_menu_help);
 		menu.add(Menu.NONE, Menu.FIRST + 6, 6, "退出").setIcon(android.R.drawable.ic_lock_power_off);
 		
         return true;
     }
 	
 	
 	/**
 	 *  功能： 菜单选择事件处理
 	 *  (non-Javadoc)
 	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
 	 * 
 	 */
 	@Override
     public boolean onOptionsItemSelected(MenuItem item) {
         switch (item.getItemId()) {

	         case Menu.FIRST + 1:			// 扫描蓝牙
	        	 Intent serverIntent = new Intent(this, DeviceListActivity.class);
		         startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
		         return true;
	  
	         case Menu.FIRST + 2:		// 蓝牙可发现
	        	 ensureDiscoverable();
	         	return true;
	         	
	         case Menu.FIRST + 3:		// 发送图片文件
	        	 gotoGallery();
	         	return true;
	         	
	         case Menu.FIRST + 4:		// 关闭蓝牙
	        	 mBluetoothAdapter.disable();
	         	return true;
	         	
	         case Menu.FIRST + 5:		// 帮助
	        	 HelpDialog helpDlg = new HelpDialog(BluetoothChat.this, R.string.camera_help_text);
			     helpDlg.showHelp();
			   return true;
			   
	         case Menu.FIRST + 6:		// 退出程序
	        	 MainViewActivity.killCurrentApp(BluetoothChat.this);
	        	 break;
	        default:
	         		break;

         }
         return false;

     }		// end of onOptionsItemSelected
 	
 	
 	/**
 	 * 功能 ：  进入系统图库选择要分享给对方的图片
 	 * 
 	 */
 	private void gotoGallery()
 	{
 		Toast.makeText(this,"选择图片之后在屏幕向上划即可发送！",1).show();
 		Intent intent = new Intent();
 		intent.setType("image/*");
 		intent.setAction(Intent.ACTION_GET_CONTENT);
 		startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_PICTURE);
 	
 	}
    
 	
 	Handler mSendHandler = new Handler();	
    /***
	 * 功能 ： 通过蓝牙发送图像等文件,调用系统蓝牙发送程序      （可选择使用蓝牙的socket发送）		
	 * 
	 **/
	private void sendPictureByBluetooth() {
	
		mSendHandler.post( mSendThread );
	}
	
	
	/**
	 * 功能 ：  发送图片的线程
	 *
	 */
	Runnable mSendThread = new Runnable() {
		
		@Override
		public void run() {

			final String deviceAddress = mChatService.getConnectedDeviceAddress();
			Log.v(TAG, "设备地址为 : " + deviceAddress);
			
			if( deviceAddress != null){		// 如果有选择图像
				//调用系统程序发送文件
				ContentValues cv = new ContentValues();
				String uri = "file://" + imagePath;			// 文件的全路径
				cv.put("uri", uri);
				// 要发送给的蓝牙设备地址
				cv.put("destination", deviceAddress);		// 数据要传送到的目标设备地址
				cv.put("direction", 0);
				Long ts = System.currentTimeMillis();
				cv.put("timestamp", ts);
				getContentResolver().insert(Uri.parse("content://com.android.bluetooth.opp/btopp"), cv);			
						
				Toast.makeText(BluetoothChat.this, "图像发送中...", 1).show();
			}
			else {
				Toast.makeText(BluetoothChat.this, "发送失败!(蓝牙未连接或图片未选择).", Toast.LENGTH_SHORT).show();
			}
			
		}	// end of run().
	};		// end of Runnable()
 	
	
 	/**
 	 * 功能： 按键按下的事件处理
 	 * (non-Javadoc)
 	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
 	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		switch (keyCode) {
			
		case KeyEvent.KEYCODE_BACK:				// 返回主界面
				Intent intent = new Intent(BluetoothChat.this, MainViewActivity.class);
				setResult(RESULT_OK, intent);
				finish();
				overridePendingTransition(R.anim.slide_left, R.anim.slide_right);
				return false;
		default:
				break;
	}
		return super.onKeyDown(keyCode, event);

	}		// end of keydown.

	
	float downx = 0;		// 按下时的X坐标
	float downy = 0;		// 按下时的Y坐标
	float upx = 0;			// 抬起时的X坐标
	float upy = 0;			// 抬起时的Y坐标
	/**
	 * 功能： 通过屏幕上方划动手指,将选中的相片分享给已经连接的蓝牙设备
	 *  触摸事件(non-Javadoc)
	 * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
	 * 
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int action = event.getAction();
		switch (action) {
			case MotionEvent.ACTION_DOWN:
				downx = event.getX();
				downy = event.getY();
				Log.v(TAG, "downy : " + downy);
				break;
				
			case MotionEvent.ACTION_UP:
				upx = event.getX();
				upy = event.getY();
				Log.v(TAG, "upy : " + upy);
				if(downy - upy > 250 && imagePath != "NULL")
				{
					Log.v(TAG, "Touch and Send image." + (downy - upy));
		        	sendPictureByBluetooth();							// 向对方分享照片
				}
				break;
				
			case MotionEvent.ACTION_CANCEL:
				break;
			default:
				break;
		}
		return true;
	}
	

	/**
	 *  功能： 鼠标手势,触屏切换页面
	 *  (non-Javadoc)
	 * @see android.view.GestureDetector.OnGestureListener#onDown(android.view.MotionEvent)
	 * 
	 */
		@Override
		public boolean onDown(MotionEvent e) {
			return false;
		}

		
		/**
		 * 功能 ：在屏幕上划动,切换activity
		 * 描述: 鼠标手势相当于一个向量（当然有可能手势是曲线），e1为向量的起点，e2为向量的终点，
		 * 		velocityX为向量水平方向的速度，velocityY为向量垂直方向的速度
		 * 
		 */
		private int verticalMinDistance = 200;  		// 竖直的最小距离
		private int minVelocity = 0;  					// 最小速率
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			if (e1.getX() - e2.getX() > verticalMinDistance && Math.abs(velocityX) > minVelocity) {  
				  
				 // 向右划动的手势,切换Activity  
		    	Intent cIntent = new Intent(BluetoothChat.this, PictureEditActivity.class);
				startActivityForResult(cIntent, 6); 
		        overridePendingTransition(R.anim.slide_left, R.anim.slide_right);
		       
		    } else if (e2.getX() - e1.getX() > verticalMinDistance && Math.abs(velocityX) > minVelocity) {  
		    	 // 向左划动的手势
				Intent bIntent = new Intent(BluetoothChat.this, CameraActivity.class);
				startActivityForResult(bIntent, 7); 
		        overridePendingTransition(R.anim.slide_left, R.anim.slide_right);
		       
		    }  
		  
		    return false;  
		}

		@Override
		public void onLongPress(MotionEvent e) {
			
		}


		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			return false;
		}


		@Override
		public void onShowPress(MotionEvent e) {
			
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			return false;
		}

}