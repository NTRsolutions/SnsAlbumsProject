package album.entry;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * 
 * @ClassName: MyProgressDialog 
 * @Description:  进度条提示框
 * @Author: Mr.Simple (何红辉)
 * @E-mail: bboyfeiyu@gmail.com 
 * @Date 2012-11-17 下午5:36:54 
 *
 */

public class MyProgressDialog extends ProgressDialog {
	
	
	/**
	 * 
	 * @Constructor:  
	 * @@param context
	 * @@param title
	 * @Description: 构造函数
	 * @param context
	 * @param title
	 */
	public MyProgressDialog(Context context, String title) {
	
		super(context);
		setTitle(title);
		setIcon(R.drawable.l_cn_48);
	}


	/**
	 * 
	 * @Constructor: 
	 * @@param context
	 * @@param title
	 * @@param resId
	 * @Description: 构造函数
	 * @param context
	 * @param title
	 * @param resId
	 */
	public MyProgressDialog(Context context, String title,int resId) {
		
		super(context);
		setTitle(title);		// 设置标题
		setIcon(resId);			// 设置图标

	}
	
	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: setProgressStyle
	 * @Description:  设置进度条的现实风格为环形风格
	 * @param style 
	 * @see android.app.ProgressDialog#setProgressStyle(int)
	 */
	@Override
	public void setProgressStyle(int style) {
		super.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	}


	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: setIcon
	 * @Description:  设置图标
	 * @param resId 
	 * @see android.app.AlertDialog#setIcon(int)
	 */
	@Override
	public void setIcon(int resId) {

		super.setIcon(resId);
	}

	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: setTitle
	 * @Description:   设置标题
	 * @param title 
	 * @see android.app.AlertDialog#setTitle(java.lang.CharSequence)
	 */
	@Override
	public void setTitle(CharSequence title) {
		super.setTitle(title);
	}

}
