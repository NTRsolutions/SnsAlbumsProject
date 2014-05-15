package network;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.apache.http.conn.util.InetAddressUtils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings.Secure;


/**
 * @ClassName: NetInfomation 
 * @Description:    获取本机的网络信息
 * @Author: Mr.Simple (何红辉)
 * @E-mail: bboyfeiyu@gmail.com 
 * @Date 2012-11-17 下午7:40:03 
 *
 */
public class NetInfomation {
	
	
	/**
	 * @Method: getNetworkStatus
	 * @Description:  判断是否有网络连接
	 * @param context
	 * @return
	 */
	public static boolean getNetworkStatus(Context context)
	{
		ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		// WIFI检测
		NetworkInfo info = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		// boolean isWifiOn = info.isAvailable();
		boolean isWifiConnected = info.isConnected();
		if ( isWifiConnected )
		{
			return isWifiConnected;
		}
		
		// mobile网络检测
		info = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		boolean isMobileConnected = info.isConnected();
		return isMobileConnected;
		
	}

	
	/**
	 * @Method: getLocalIpAddress
	 * @Description:   获取本机IP地址
	 * @return
	 */
	public static String getLocalIpAddress() {  
	
	        try{ 
	             for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) { 
	                 NetworkInterface intf = en.nextElement();   
	                    for (Enumeration<InetAddress> enumIpAddr = intf   
	                            .getInetAddresses(); enumIpAddr.hasMoreElements();) {   
	                        InetAddress inetAddress = enumIpAddr.nextElement();   
	                        if (!inetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(inetAddress.getHostAddress())) {   
	                             
	                            return inetAddress.getHostAddress().toString();   
	                        }   
	                    }   
	             } 
	        }catch (SocketException e) { 
	        	e.printStackTrace();
	        } 
	         
	        return null;  
	    
	    }  	// end of getLocalIpAddress

	

	/**
	 * @Method: checkNetWorkStatus
	 * @Description:  检测是否已经开启了网络
	 * @param context
	 * @return
	 */
	public static boolean checkNetWorkStatus(Context context) {  
        boolean netSataus = false;  
        ConnectivityManager lxfManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);  
        lxfManager.getActiveNetworkInfo();  
        if (lxfManager.getActiveNetworkInfo() != null) {  
            netSataus = lxfManager.getActiveNetworkInfo().isAvailable();  
        }  
        return netSataus;  
    }
	

	/**
	 * @Method: getMobileID
	 * @Description:  获取手机的唯一编号
	 * @param context
	 * @return
	 */
	public static String getMobileID(Context context)
	{
		return Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
	}

}
