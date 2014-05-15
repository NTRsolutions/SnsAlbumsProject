package chat;

import android.util.Log;


/**
 * @ClassName: DetailEntity 
 * @Description:   聊天消息类封装
 * @Author: Mr.Simple (何红辉)
 * @E-mail: bboyfeiyu@gmail.com 
 * @Date 2012-11-17 下午7:24:57 
 *
 */

public class DetailEntity {
	
    private String name;			// 用户名
    private String date;			// 系统时间
    private String text;			// 消息内容
    private int layoutID;			// 布局ID号
    
    
    /**
     * 
     * @Constructor: 
     * @
     * @Description: 构造函数
     */
    public DetailEntity() {
		
	}
	
    
    /**
     * 
     * @Constructor: 
     * @@param name
     * @@param date
     * @@param text
     * @@param layoutID
     * @Description: 构造函数
     * @param name
     * @param date
     * @param text
     * @param layoutID
     */
	public DetailEntity(String name, String date, String text, int layoutID) {
		super();
		this.name = name;
		this.date = date;
		this.text = text;
		this.layoutID = layoutID;
	}
	
	
	/**
	 * @Method: getName
	 * @Description: 获取当前entity的名称
	 * @return
	 */
	public String getName() {
		return name;
	}
	
	
	/**
	 * @Method: setName
	 * @Description:   设置当前entity的名称
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	
	/**
	 * @Method: getDate
	 * @Description:  获取当前entity的时间
	 * @return
	 */
	public String getDate() {
		return date;
	}
	
	
	/**
	 * @Method: setDate
	 * @Description: 设置当前entity的时间
	 * @param date
	 */
	public void setDate(String date) {
		
		this.date = date;
		Log.d("Time : ", this.date);
	}
	
	
	/**
	 * @Method: getText
	 * @Description:  获取当前entity的文本
	 * @return
	 */
	public String getText() {
		return text;
	}
	
	
	/**
	 * @Method: setText
	 * @Description: 设置当前entity的文本
	 * @param text
	 */
	public void setText(String text) {
		this.text = text;
	}
	
	

	/**
	 * @Method: getLayoutID
	 * @Description:  获取当前entity布局ID号
	 * @return
	 */
	public int getLayoutID() {
		return layoutID;
	}
	
	
	/**
	 * @Method: setLayoutID
	 * @Description:  设置布局ID号
	 * @param layoutID
	 */
	public void setLayoutID(int layoutID) {
		this.layoutID = layoutID;
	}
}
