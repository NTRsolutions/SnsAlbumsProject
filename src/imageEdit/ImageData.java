package imageEdit;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;

/**
 * 
 * @author 何红辉		类功能 : 图像的原始数据操作类			date: 8.8
 * 
 * 1.图像的克隆				clone()
 * 2.获取图像的像素颜色		getPixelColor(int x, int y)
 * 3.获取图像的红色成分		getRComponent的多个重载函数
 * 4.获取图像的绿色成分		getGComponent的多个重载函数
 * 5.获取图像的黑色成分		getBComponent的多个重载函数
 * 6.设置图像的像素颜色		setPixelColor(int x, int y, int rgbcolor)
 * 7.获取颜色数组			getColorArray()
 * 8.获取目标图像			getDstBitmap()
 * 
 */

public class ImageData {
	private Bitmap srcBitmap;
	private Bitmap dstBitmap;

	private int width;
	private int height;

	protected int[] colorArray;

	/**
	 * 
	 * @Constructor: 
	 * @@param bmp
	 * @Description: 构造函数
	 * @param bmp
	 */
	public ImageData(Bitmap bmp) {
		srcBitmap = bmp;
		width = bmp.getWidth();
		height = bmp.getHeight();
		dstBitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		initColorArray();
	}

	
	/**
	 * (非 Javadoc,覆写的方法) 
	 * @Title: clone
	 * @Description: 
	 * @return 
	 * @see java.lang.Object#clone()
	 */
	@Override
	public ImageData clone() {
		return new ImageData(this.srcBitmap);
	}

	
	/**
	 * @Method: initColorArray
	 * @Description: 初始化颜色数组
	 */
	private void initColorArray() {
		colorArray = new int[width * height];
		srcBitmap.getPixels(colorArray, 0, width, 0, 0, width, height);
	}

	
	/**
	 * 	功能 ：获取像素点上的颜色
	 * @param x
	 * @param y
	 * @return int
	 */
	public int getPixelColor(int x, int y) {
		return colorArray[y * srcBitmap.getWidth() + x];
	}

	public int getPixelColor(int x, int y, int offset) {
		return colorArray[y * srcBitmap.getWidth() + x + offset];
	}

	
	/**
	 * 	功能 ：获取红色成分的色调
	 * @param x
	 * @param y
	 * @return int
	 * 
	 */
	public int getRComponent(int x, int y) {
		return Color.red(colorArray[y * srcBitmap.getWidth() + x]);
	}

	
	/**
	 * @Method: getRComponent
	 * @Description:
	 * @param x
	 * @param y
	 * @param offset
	 * @return
	 */
	public int getRComponent(int x, int y, int offset) {
		return Color.red(colorArray[y * srcBitmap.getWidth() + x + offset]);
	}

	
	/**
	 * 	功能 ：获取绿色成分的色调
	 * @param x
	 * @param y
	 * @return int
	 * 
	 */
	public int getGComponent(int x, int y) {
		return Color.green(colorArray[y * srcBitmap.getWidth() + x]);
	}

	/**
	 * @Method: getGComponent
	 * @Description:
	 * @param x
	 * @param y
	 * @param offset
	 * @return
	 */
	public int getGComponent(int x, int y, int offset) {
		return Color.green(colorArray[y * srcBitmap.getWidth() + x + offset]);
	}

	
	/**
	 * 	功能 ：获取黑色成分的色调
	 * @param x
	 * @param y
	 * @return int
	 * 
	 */
	public int getBComponent(int x, int y) {
		return Color.blue(colorArray[y * srcBitmap.getWidth() + x]);
	}

	
	/**
	 * @Method: getBComponent
	 * @Description:
	 * @param x
	 * @param y
	 * @param offset
	 * @return
	 */
	public int getBComponent(int x, int y, int offset) {
		return Color.blue(colorArray[y * srcBitmap.getWidth() + x + offset]);
	}

	
	/**
	 * 	功能 ：设置像素点颜色
	 * @param x
	 * @param y
	 * @param r			红色
	 * @param g			绿色
	 * @param b			黑色
	 * @return int
	 * 
	 */
	public void setPixelColor(int x, int y, int r, int g, int b) {
		int rgbcolor = (255 << 24) + (r << 16) + (g << 8) + b;
		colorArray[((y * srcBitmap.getWidth() + x))] = rgbcolor;
	}

	
	/**
	 * @Method: setPixelColor
	 * @Description:
	 * @param x
	 * @param y
	 * @param rgbcolor
	 */
	public void setPixelColor(int x, int y, int rgbcolor) {
		colorArray[((y * srcBitmap.getWidth() + x))] = rgbcolor;
	}

	
	/**
	 * 	功能 ：获取颜色数组
	 * @return int[]
	 * 
	 */
	public int[] getColorArray() {
		return colorArray;
	}

	/**
	 * 	功能 ：获取目标图像
	 * @return Bitmap
	 * 
	 */
	public Bitmap getDstBitmap() {
		dstBitmap.setPixels(colorArray, 0, width, 0, 0, width, height);
		return dstBitmap;
	}

	/**
	 * @Method: safeColor
	 * @Description:
	 * @param a
	 * @return
	 */
	public int safeColor(int a) {
		if (a < 0)
			return 0;
		else if (a > 255)
			return 255;
		else
			return a;
	}

	/**
	 * @Method: getWidth
	 * @Description:
	 * @return
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * @Method: getHeight
	 * @Description:
	 * @return
	 */
	public int getHeight() {
		return height;
	}

}
