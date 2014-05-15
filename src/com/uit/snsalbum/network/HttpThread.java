package com.uit.snsalbum.network;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

/**
 * Http协议：发送post请求，并接收返回值
 * @author xxjgood
 * @param  ArrayList<NameValuePair>
 * @return String or Bitmap 服务器返回的字符串或图片
 * @date   2012.4.20
 * 
 * 备注：
 * deal = 1  	 注册   			返回成功或失败
 * deal = 2  	 登录   			返回失败或相册名
 * deal = 3  	 进入某相册			返回图片名
 * deal = 100	 请求小图			返回小图
 * deal = 101	 请求大图			返回大图
 * deal = 6		 上传图片			返回成功失败
 * deal = 7		 获取好友列表		返回所有好友IP
 * deal = 8		 请求好友坐标		返回好友坐标
 * deal = 10	 退出				返回成功失败   
 * deal = 11	 进入相册 			返回相册列表
 * deal = 12	 新建相册			返回成功或失败
 * deal = 13	 删除相册			返回成功或失败
 * deal = 14	 修改相册			返回成功或失败
 * deal = 15	 删除图片			返回成功或失败
 */

public class HttpThread { 
	
	private int deal = 0;						// 协议
	private String the_string_response = null;	// 存放返回的String
	private Bitmap bitmap = null;				// 存放返回图片的bitmap
	//private String url = "http://ass001.gotoip55.com/Android/receiveMessage.php";			// 要连接的url	
	private ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();	// 发送的ArrayList
	private final String TAG = "HttpThread";
	public static String serverURL = "";				// 服务器URL,在设置界面可以设定
	
	private String url = "http://199.36.75.40/Android/receiveMessage.php";
	

	/**
	 * 
	 * @Constructor: 
	 * @@param namevaluepairs
	 * @@param Deal
	 * @Description: 构造函数,初始化协议和要传送的内容
	 * @param namevaluepairs
	 * @param Deal
	 */
	public HttpThread(ArrayList<NameValuePair> namevaluepairs,int Deal) {	
		
		nameValuePairs = namevaluepairs;										
		deal = Deal;
		
		// 通过设置界面设置服务器IP
		if (!serverURL.equals(""))
		{
			url = serverURL;
			Log.d(TAG, "我的服务器IP"+url);
		}
	}
	
	
	/**
	 * @Method: sendInfo
	 * @Description: HTTP POST请求
	 * @return
	 */
	public Object sendInfo() {														
		try{ 						
			// httpClient协议
			HttpClient httpclient = new DefaultHttpClient(); 				
			// 设置超时参数
			HttpParams params = httpclient.getParams();
			HttpConnectionParams.setConnectionTimeout(params, 8000);
			HttpConnectionParams.setSoTimeout(params, 8000);
			
			HttpPost httppost = new	HttpPost(url); 							// HttpPost
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs,HTTP.UTF_8));	// 把数据放入entity
			HttpResponse response = httpclient.execute(httppost);			// 发送数据
			
			// 根据协议判断接收String or Bitmap
			if(deal < 100){			
				the_string_response = convertResponseToString(response); 	// 接收返回值
				return the_string_response;
			}
			else if(deal == 100 || deal == 101){
				bitmap = convertResponseToBitmap(response);
				return bitmap;
			}
			else{
				return 0;
			}
		}catch(Exception e){
			Log.d("1", "Error in http connection " + e.toString());
			return "error";
		}
	}
	
	
	/**
	 * @Method: convertResponseToString
	 * @Description:   接收 http post 返回的String
	 * @param response 服务器返回的字符串（成功、失败、数据）
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public String convertResponseToString(HttpResponse response)
			throws IllegalStateException, IOException {
		String res = null; // 返回的String
		StringBuffer buffer = new StringBuffer(); // new Buffer
		InputStream inputStream = response.getEntity().getContent(); // getting
																		// inputStream
		int contentLength = (int) response.getEntity().getContentLength(); // getting
																			// content
																			// length
		contentLength = inputStream.available();
		// int contentLength = (int)inputStream.read();
		Log.d(TAG, "len : " + contentLength);

		if (contentLength < 0) {
			Log.d(TAG, "contentLength < 0");
		} else {
			byte[] data = new byte[512];
			int len = 0;
			try {
				while (-1 != (len = inputStream.read(data))) {
					buffer.append(new String(data, 0, len)); // converting to
																// string and
																// appending to
																// buffer
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {

				inputStream.close(); // closing the stream
			} catch (IOException e) {
				e.printStackTrace();
			}

			res = buffer.toString(); // buffer to string
		}

		return res;
	}

	
	/**
	 * @Method: convertResponseToBitmap
	 * @Description:   接收 http post 返回的图片
	 * @param response
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public Bitmap convertResponseToBitmap(HttpResponse response) throws IllegalStateException, IOException{
		InputStream inputStream = response.getEntity().getContent();	// 获取流
		Bitmap bitmap = BitmapFactory.decodeStream(inputStream);		// 解码图片
		
		try {
			inputStream.close(); 										// closing the stream….. 
		}
		catch (IOException e) {
			e.printStackTrace(); 
		}
		
		return bitmap;
	}
}

