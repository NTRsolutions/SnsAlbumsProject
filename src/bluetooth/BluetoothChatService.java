
package bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * @ClassName: BluetoothChatService 
 * @Description:  	1.管理蓝牙服务的连接状态
 					2.连接远程蓝牙设备
 					3.通过线程向主界面投递本地蓝牙的状态消息
 * @Author: Mr.Simple (何红辉)
 * @E-mail: bboyfeiyu@gmail.com 
 * @Date 2012-11-18 下午6:50:49 
 *
 */

public class BluetoothChatService {
    // 调试信息
    private static final String TAG = "BluetoothChatService";
    private static final boolean D = true;

    // 服务器名称
    private static final String NAME = "BluetoothChat";

    // Unique UUID for this application.        UUID字符串唯一的.
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    // private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields。		数据成员字段
    private final BluetoothAdapter mAdapter;		// 适配器
    private final Handler mHandler;					// 消息分发线程,用于向UI线程发送数据
    private AcceptThread mAcceptThread;				// 用于接受的线程
    private ConnectThread mConnectThread;			// 用于连接的线程
    private ConnectedThread mConnectedThread;		// 已连接状态下的处理线程
    private int mState;								// 蓝牙状态
    
    private BluetoothDevice mConnectedDevice;		// 已经连接上的设备.用于获取设备地址,返回给ACTIVITY,以便发送图像.

    // 蓝牙状态常量
    public static final int STATE_NONE = 0;       	// 默认状态
    public static final int STATE_LISTEN = 1;     	// 蓝牙监听状态
    public static final int STATE_CONNECTING = 2; 	// 连接状态
    public static final int STATE_CONNECTED = 3;  	// now connected to a remote device
    

    /**
     * 功能 ： 构造函数,准备一个新的蓝牙聊天会话
     * @param context  The UI Activity Context.     
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothChatService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    
    /**
     * 功能： 设置蓝牙连接设备的当前状态
     * @param state  An integer defining the current connection state
     * 
     */
    private synchronized void setState(int state) {
        if (D) 
        	Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // 更新状态,向UI线程发送新的状态
        mHandler.obtainMessage(BluetoothChat.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }
       
    
    /**
     *  功能： 异步，获取蓝牙设备服务状态
     *  
     */
    public synchronized int getState() {
        return mState;
    }

    
    /**
     *    功能： 启动蓝牙聊天服务，并且指定一个监听客户端连接的线程,即AcceptThread,启动该线程,蓝牙进入监听状态
     *    
     */
    public synchronized void start() {
        if (D) 
        	Log.d(TAG, "start");

        // 取消所有正在尝试连接到本机的连接
        if (mConnectThread != null) {
        	mConnectThread.cancel();
        	mConnectThread = null;
        }

        // 取消所有已经连接到本机的连接
        if (mConnectedThread != null) {
        	mConnectedThread.cancel(); 
        	mConnectedThread = null;
        }

        // 启动线程监听网络连接。即进入监听状态
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        
        // 设置蓝牙设备当前状态为监听状态
        setState(STATE_LISTEN);
    }

    
    /**
     * 	功能： 启动连接线程,连接到指定的蓝牙设备
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) 
        	Log.d(TAG, "connect to: " + device);

        // 取消正在连接的连接
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
            	mConnectThread.cancel();
            	mConnectThread = null;
            }
        }

        // 取消正在运行的连接
        if (mConnectedThread != null) {
        	mConnectedThread.cancel();
        	mConnectedThread = null;
        }

        // 启动连接到远程设备的线程
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    
    /**
     * 	功能 ： 启动已连接线程来管理蓝牙连接.
     * @param socket  The BluetoothSocket on which the connection was made.	
     * @param device  The BluetoothDevice that has been connected.      已连接的蓝牙设备
     * 
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D)
        	Log.d(TAG, "connected");

        // 取消连接线程
        if (mConnectThread != null) {
        	mConnectThread.cancel(); 
        	mConnectThread = null;
        }

        // 取消正在运行的已连接线程
        if (mConnectedThread != null) {
        	mConnectedThread.cancel(); 
        	mConnectedThread = null;
        }

        // 取消接受连接线程
        if (mAcceptThread != null) {
        	mAcceptThread.cancel(); 
        	mAcceptThread = null;
        }

        // 启动线程来管理连接
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // 发送已连接设备的名字给UI线程,mHandler消息投递
        Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothChat.DEVICE_NAME, device.getName());
        msg.setData( bundle );				// 页面之间传递数据
        mHandler.sendMessage(msg);			// 向UI线程传递消息.UI线程则更新UI 

        setState(STATE_CONNECTED);			// 更新蓝牙状态
        
        mConnectedDevice = device;			// 连接上的蓝牙
    }

    
    /*
     *  功能 ： 获取已经连接上的蓝牙的设备地址
     *  
     */
    public String getConnectedDeviceAddress()
    {
    	if(mConnectedDevice != null){
    		return mConnectedDevice.getAddress();
    	}
    	else
    		return null;
    }
    
    
    /**
     * 功能： 停止所有的线程
     * 
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (mConnectThread != null) {
        	mConnectThread.cancel(); 
        	mConnectThread = null;
        }
        if (mConnectedThread != null) {
        	mConnectedThread.cancel();
        	mConnectedThread = null;
        }
        if (mAcceptThread != null) {
        	mAcceptThread.cancel(); 
        	mAcceptThread = null;
        }
        setState(STATE_NONE);
        
    }

    
    /**	
     * 功能： 用已连接线程写入数据,即传送数据. 基于异步的操作
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {	
    	
        ConnectedThread cThread;						// 已连接的线程.可以发送数据
        // 同步一个已连接线程的拷贝.    
        synchronized (this) {
            if (mState != STATE_CONNECTED) 				// 如果没有连接.则返回
            return;
            cThread = mConnectedThread;					// 已连接线程
        }
     
        cThread.write(out);						// 使用异步线程发送数据
    }

    
    /**
     * 功能： 指出尝试连接失败的连接,并且通知UI线程
     * 
     */
    private void connectionFailed() {			// 连接失败
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothChat.TOAST, "没有连接上设备.");
        msg.setData(bundle);
        mHandler.sendMessage(msg);			// 发送连接失败的消息给UI线程
    }

    
    /**
     * 功能 ： 指出丢失的连接,并且通知UI线程
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothChat.TOAST, "设备连接丢失.");
        msg.setData(bundle);
        mHandler.sendMessage(msg);			// 分发失去连接的消息给UI线程
    }

    /**	
     * 	功能： 蓝牙处于监听状态时的接受客户端连接的线程
     * 
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;		// 用的是服务器模式的套接字socket

            // 创建一个服务器socket监听
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);	// 监听客户端的连接请求
            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);
            }
            mmServerSocket = tmp;					// 将临时的服务器套接字赋给 类成员变量mmServerSocket
        }

        @Override
		public void run() {
            if (D) 
            	Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected.
            while (mState != STATE_CONNECTED) {
                try {
      
                    socket = mmServerSocket.accept();	// 接受客户端的主动连接.阻塞函数,即等待客户端连接
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }

                // If a connection was accepted.   连接被接受
                if (socket != null) {
                    synchronized (BluetoothChatService.this) {
                    	
                        switch (mState) {
                        
	                        case STATE_LISTEN:
	                        case STATE_CONNECTING:
	                            // 启动已连接线程
	                            connected(socket, socket.getRemoteDevice());
	                            break;
	                            
	                        case STATE_NONE:
	                        case STATE_CONNECTED:
	                            // Either not ready or already connected. Terminate new socket.
	                            try {
	                                socket.close();
	                            } catch (IOException e) {
	                                Log.e(TAG, "Could not close unwanted socket", e);
	                            }
	                            break;
                        }
                    }
                }
            }
            if (D) Log.i(TAG, "END mAcceptThread");
        }

        /*
         * 功能 ： 取消操作
         * 
         */
        public void cancel() {
            if (D) Log.d(TAG, "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }


    /**
     * 功能 ： 连接设备的线程
     * 
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        
        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        @Override
		public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // 使蓝牙设备不可发现
            mAdapter.cancelDiscovery();

            // 连接蓝牙设备
            try {

                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();
                // 关闭蓝牙连接
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // 启动BluetoothChatService来重启监听模式
                BluetoothChatService.this.start();
                return;
            }

            // 重置连接线程
            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }

            // 启动已连接线程
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    
    /**
     * 功能： 已连接线程,处理所有socket数据传输
     * 
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // 获得BluetoothSocket的输入、输出流
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
		public void run() {						// 默认的就是一直在检测是否用数据发来.
        	
            Log.i(TAG, "BEGIN mConnectedThread");
            
            byte[] buffer = new byte[1024];
            int bytes;

            // 当蓝牙已连接后,保持输入流的监听状态
            while (true) {

	                try {
	                    // 从输入流读入数据
	                    bytes = mmInStream.read(buffer);
	
	                    // 每接到1024字节就发送给UI线程
	                    mHandler.obtainMessage(BluetoothChat.MESSAGE_READ, bytes, -1, buffer)
	                            .sendToTarget();
	                } catch (IOException e) {
	                    Log.e(TAG, "disconnected", e);
	                    connectionLost();
	                    break;
	                }	
	                
            	}
            }		// end of run().

        
        /**
         * 功能 ： 向已连接的输出流写入数据
         * @param buffer  The bytes to write
         * 
         */
        public void write(byte[] buffer) {		
            try {
                mmOutStream.write(buffer);

                // 将已发送的信息投递给UI页面
                mHandler.obtainMessage(BluetoothChat.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

     
        /**
         * 功能 ： 取消并且关闭socket
         * 
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
