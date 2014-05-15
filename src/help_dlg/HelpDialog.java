
package help_dlg;

import album.entry.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

/**
 * @ClassName: HelpDialog 
 * @Description:  帮助信息窗口
 * @Author: Mr.Simple (何红辉)
 * @E-mail: bboyfeiyu@gmail.com 
 * @Date 2012-11-17 下午7:29:19 
 *
 */


public class HelpDialog  {

	private Context mContext;
	private int mMessageId;
	
	/**
	 * 
	 * @Constructor: 
	 * @@param context
	 * @@param messageId
	 * @Description: 构造函数
	 * @param context    调用该函数的activity
	 * @param messageId 调用该函数的activity
	 */
	public HelpDialog(Context context, int messageId){
		mContext = context;
		mMessageId = messageId;
	}


	/**
	 * @Method: showHelp
	 * @Description:   显示帮助信息
	 */
    public void showHelp(){
    	new AlertDialog.Builder( mContext )
    	.setIcon(R.drawable.help_64)
        .setTitle("	帮   助")
        .setMessage( mMessageId )
        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
            @Override
			public void onClick(DialogInterface dialog, int whichButton) {
            	
            }
        }).show();
    }
}
