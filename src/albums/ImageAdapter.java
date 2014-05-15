package albums;

import java.util.ArrayList;

import album.entry.R;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;


/**
 * @ClassName: ImageAdapter 
 * @Description:  重写ImageList，使之能够加载内存中的Bitmap
 * @Author: xxjgood
 * @E-mail: bboyfeiyu@gmail.com 
 * @Date 2012-11-17 下午6:02:00 
 *
 */
public class ImageAdapter extends BaseAdapter{  
      
    public Bitmap[] image;  
    public ArrayList<Bitmap> mImageList = null;
    Activity activity; 
    LayoutInflater mInflater;
      
    /**
     * 
     * @Constructor: 
     * @@param atv
     * @Description: 构造函数
     * @param atv
     */
    public ImageAdapter(Activity atv) {  
        activity = atv;  
        mInflater = (LayoutInflater)atv.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mImageList = new ArrayList<Bitmap>();

    }
   
    /**
     * (非 Javadoc,覆写的方法) 
     * @Title: getCount
     * @Description: 
     * @return 
     * @see android.widget.Adapter#getCount()
     */
    @Override
	public int getCount() {  
        
    	return mImageList.size();
    }  
    
    /**
     * (非 Javadoc,覆写的方法) 
     * @Title: getItem
     * @Description: 
     * @param position
     * @return 
     * @see android.widget.Adapter#getItem(int)
     */
    @Override
	public Object getItem(int position) {  
    
    	return mImageList.get(position);
    }   
    
    
    /**
     * (非 Javadoc,覆写的方法) 
     * @Title: getItemId
     * @Description: 
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
     * @Title: getView
     * @Description:  获得view,在此用ViewHolder提升性能
     * @param position
     * @param convertView
     * @param parent
     * @return 
     * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    @Override
	public View getView(int position, View convertView, ViewGroup parent) {  
 
    	ViewHolder holder = null;
        if (convertView == null) {
        	
            convertView = mInflater.inflate(R.layout.image, null);
            holder = new ViewHolder();
            holder.localImageView = (ImageView)convertView.findViewById(R.id.bigimage);
            convertView.setLayoutParams(new GridView.LayoutParams(100, 100));
            holder.localImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            convertView.setTag(holder);
            
        } else {
            holder = (ViewHolder)convertView.getTag();
        }
        holder.localImageView.setImageBitmap(mImageList.get(position));
        
        return convertView;
    } 
    
    private class ViewHolder   
    {  
        public ImageView localImageView = null;  

    }  
    

    /**
     * @Method: drawableToBitmap
     * @Description:  将drawable转换成Bitmap
     * @param drawable
     * @return
     */
    public static Bitmap drawableToBitmap(Drawable drawable) {    
            
        Bitmap bitmap = Bitmap.createBitmap(    
                                        drawable.getIntrinsicWidth(),    
                                        drawable.getIntrinsicHeight(),    
                                        drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888    
                                                        : Bitmap.Config.RGB_565);    
        Canvas canvas = new Canvas(bitmap);    
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());    
        drawable.draw(canvas);    
        return bitmap;    
    }  
}  
