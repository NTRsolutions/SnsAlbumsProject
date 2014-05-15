package chat;

import java.util.ArrayList;

import album.entry.R;
import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;


/**
 * @ClassName: DetailAdapter 
 * @Description:  聊天消息列表适配器  （或者可以参考好友列表,使用ListActivity来实现气泡短信功能 ）
 * @Author: Mr.Simple (何红辉)
 * @E-mail: bboyfeiyu@gmail.com 
 * @Date 2012-11-17 下午7:22:56 
 *
 */

public class DetailAdapter implements ListAdapter{
	
	private ArrayList<DetailEntity> mMsgList;			// 消息列表
	private Context mContext;							// 当前的activity
	
	int[] imgArray = {	R.drawable.bad_smile_96,R.drawable.laugh_96,
						R.drawable.fire_96,R.drawable.money_96,		// 可选头像列表
						R.drawable.grimace_96,R.drawable.girl_96,
						R.drawable.face_96, R.drawable.o_96
						}; 
	
	
	/**
	 * @Constructor: 
	 * @@param context
	 * @@param msgList
	 * @Description: 构造函数
	 * @param context
	 * @param msgList
	 */
	public DetailAdapter(Context context ,ArrayList<DetailEntity> msgList) {
		
		mContext = context;
		mMsgList = msgList;
	}
	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: areAllItemsEnabled
	 * @Description: 
	 * @return 
	 * @see android.widget.ListAdapter#areAllItemsEnabled()
	 */
	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}
	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: isEnabled
	 * @Description: 
	 * @param arg0
	 * @return 
	 * @see android.widget.ListAdapter#isEnabled(int)
	 */
	@Override
	public boolean isEnabled(int arg0) {
		return false;
	}
	
	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: getCount
	 * @Description:  获取当前adapter中列表数据的数量
	 * @return 
	 * @see android.widget.Adapter#getCount()
	 */
	@Override
	public int getCount() {
		return mMsgList.size();
	}
	
	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: getItem
	 * @Description:  获取当前adapter的一项
	 * @param position
	 * @return 
	 * @see android.widget.Adapter#getItem(int)
	 */
	@Override
	public Object getItem(int position) {
		return mMsgList.get(position);
	}
	

	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: getItemId
	 * @Description:  获取当前adapter中一项的ID号
	 * @param position
	 * @return 
	 * @see android.widget.Adapter#getItemId(int)
	 */
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: getItemViewType
	 * @Description: 
	 * @param position
	 * @return 
	 * @see android.widget.Adapter#getItemViewType(int)
	 */
	@Override
	public int getItemViewType(int position) {
		return position;
	}
	
	
	/**
	 * @Method: clear
	 * @Description:
	 */
	public void clear(){
		mMsgList.clear();
	}
	

	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: getView
	 * @Description:   获取消息列表中的view  (每次获取一条数据)
	 * @param position
	 * @param convertView
	 * @param parent
	 * @return 
	 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		// 得到当前entity
		DetailEntity entity = mMsgList.get(position);
		// 获取当前entity的布局ID号
		int itemLayout = entity.getLayoutID();				
		
		// 用ctx构造一个线性布局
		LinearLayout layout = new LinearLayout(mContext);
		
		// 用LayoutInflater来找layout文件夹下的xml布局文件，并且实例化
		LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		vi.inflate(itemLayout, layout, true);
		
		// 以下是在各个消息显示控件中赋值
		TextView tvName = (TextView) layout.findViewById(R.id.messagedetail_row_name);
		tvName.setText(entity.getName());
		
		TextView tvDate = (TextView) layout.findViewById(R.id.messagedetail_row_date);
		tvDate.setText(entity.getDate());
		
		TextView tvText = (TextView) layout.findViewById(R.id.messagedetail_row_text);
		tvText.setText(entity.getText());
		
		return layout;
	}
	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: getViewTypeCount
	 * @Description: 
	 * @return 
	 * @see android.widget.Adapter#getViewTypeCount()
	 */
	@Override
	public int getViewTypeCount() {
		
		return mMsgList.size();
	}
	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: hasStableIds
	 * @Description: 
	 * @return 
	 * @see android.widget.Adapter#hasStableIds()
	 */
	@Override
	public boolean hasStableIds() {
		return false;
	}
	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: isEmpty
	 * @Description: 
	 * @return 
	 * @see android.widget.Adapter#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return false;
	}
	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: registerDataSetObserver
	 * @Description: 
	 * @param observer 
	 * @see android.widget.Adapter#registerDataSetObserver(android.database.DataSetObserver)
	 */
	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
	}
	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: unregisterDataSetObserver
	 * @Description: 
	 * @param observer 
	 * @see android.widget.Adapter#unregisterDataSetObserver(android.database.DataSetObserver)
	 */
	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
	}
    
	
}
