package imageEdit;

import java.io.ByteArrayOutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;


/**
 * 
 * @author 何红辉		类功能 : 图像的编辑效果类
 * 1.图像的缩放				resizeBitmap(Bitmap, float)等
 * 2.图像的反转				reverseBitmap(bitmap , int )
 * 3.给图像加边框			addBigFrame(Bitmap bmp, int res)
 * 4.给图像加上美化的小图像		doodle(Bitmap, Bitmap)
 * 5.bitmap、drawable、byte之间的互相转换
 * 6.图像裁剪等
 * 
 */

public class ImageHandle
{
	public static final int FRAME_BIG = 0x1;
	public static final int FRAME_SMALL = FRAME_BIG + 1;

	private Context mContext;

	
	/**
	 * 功能 : 构造函数
	 * @param  Context  
	 * 
	 **/
	public ImageHandle(Context context) {
		mContext = context;
	}
	
	
	/*
	 * 功能： 给图像添加边框, 覆盖图层方式
	 * 描述： 配合  drawableToBitmap 和  decodeBitmap 使用即可
	 * @param bm 原图片
	 * @param res 边框资源
	 * @return
	 **/
	public Bitmap addBigFrame(Bitmap bmp, int res)
	{
		// 花边相框图
		Bitmap frameBitmap = decodeBitmap( res );
		Drawable[] array = new Drawable[2];
		array[0] = new BitmapDrawable( bmp );				// array[0]未加边框的图层
		
		// 修改边框大小以适应背景图
		Bitmap tempFrameBmp = resize(frameBitmap, bmp.getWidth(), bmp.getHeight());	
		array[1] = new BitmapDrawable( tempFrameBmp );
		
		LayerDrawable layer = new LayerDrawable(array);		// 两图层叠加在一起
		return drawableToBitmap( layer );					// 将drawable转换成bitmap
	}
	

	/*
	 * 	功能 ： 将Drawable转换成Bitmap
	 * @param drawable
	 * @return
	 * 
	 **/
	public Bitmap drawableToBitmap(Drawable drawable)
	{
		Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
						drawable.getIntrinsicHeight(), drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
		
		// 重新绘制整个图像,将合成相框的图像绘制到画布上
		Canvas canvas = new Canvas( bitmap );
		drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
		drawable.draw(canvas);
		return bitmap;
	}

	
	/*
	 * 功能 ： 将drawable转换成Bitmap
	 * @param res
	 * @return
	 * 
	 */
	public Bitmap decodeBitmap(int res)
	{
		return BitmapFactory.decodeResource(mContext.getResources(), res);
	}
	
	
	/**
	 * 功能： 图片缩放
	 * @param bm
	 * @param w
	 * @param h
	 * @return
	 * 
	 */
	public Bitmap resize(Bitmap bm, int w, int h)
	{
		Bitmap BitmapOrg = bm;

		int width = BitmapOrg.getWidth();
		int height = BitmapOrg.getHeight();
		int newWidth = w;
		int newHeight = h;

		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;

		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);
	
		// recreate the new Bitmap
		Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width,
				height, matrix, true);
		return resizedBitmap;
	}

	
	/**
	 * 功能  ： 图片旋转
	 * @param bmp  		 要旋转的图片
	 * @param degree    图片旋转的角度，负值为逆时针旋转，正值为顺时针旋转
	 * @return
	 */
	public static Bitmap rotateBitmap(Bitmap bmp, float degree) {
		Matrix matrix = new Matrix();
		matrix.postRotate(degree);
		return Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
	}

	
	/**
	 * 功能： 图片缩放
	 * @param bm
	 * @param scale      值小于1则为缩小，否则为放大
	 * @return
	 */
	public Bitmap resizeBitmap(Bitmap bm, float scale) {
		Matrix matrix = new Matrix();
		matrix.postScale(scale, scale);
		return Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
	}

	
	/**
	 * 功能： 图片缩放
	 * @param bm	要缩放的图像
	 * @param w     缩小或放大成的宽
	 * @param h     缩小或放大成的高
	 * @return
	 * 
	 */
	public Bitmap resizeBitmap(Bitmap bm, int w, int h) {
		Bitmap BitmapOrg = bm;

		int width = BitmapOrg.getWidth();
		int height = BitmapOrg.getHeight();

		float scaleWidth = ((float) w) / width;
		float scaleHeight = ((float) h) / height;

		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);
		return Bitmap.createBitmap(BitmapOrg, 0, 0, width, height, matrix, true);
	}

	
	/**
	 * 功能 ： 图片反转
	 * @param bm		原图像
	 * @param flag      0为水平反转，1为垂直反转
	 * @return
	 */
	public Bitmap reverseBitmap(Bitmap bmp, int flag) {
		float[] floats = null;
		switch (flag) {
		case 0: // 水平反转
			floats = new float[] { -1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f };
			break;
		case 1: // 垂直反转
			floats = new float[] { 1f, 0f, 0f, 0f, -1f, 0f, 0f, 0f, 1f };
			break;
		}

		if (floats != null) {
			Matrix matrix = new Matrix();
			matrix.setValues(floats);
			return Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
		}

		return null;
	}
	
	
	/*
	 * 功能 ： 获得带倒影的图像
	 * 
	 */
	public static Bitmap createReflectionImageWithOrigin(Bitmap bitmap){ 
		
		 // 图片与倒影之间的距离间隔
		final int reflectionGap = 4; 
		int width = bitmap.getWidth(); 
		int height = bitmap.getHeight(); 

		 // 图片旋转，缩放等控制对象
		Matrix matrix = new Matrix(); 
		matrix.preScale(1, -1); 

		
		Bitmap reflectionImage = Bitmap.createBitmap(bitmap, 0, height/2,
									width, height/2, matrix, false);

		Bitmap bitmapWithReflection = Bitmap.createBitmap(width, (height + height/2), Config.ARGB_8888); 

		Canvas canvas = new Canvas(bitmapWithReflection); 
		canvas.drawBitmap(bitmap, 0, 0, null); 
		Paint deafalutPaint = new Paint(); 
		canvas.drawRect(0, height,width,height + reflectionGap, 
		deafalutPaint); 

		canvas.drawBitmap(reflectionImage, 0, height + reflectionGap, null); 

		Paint paint = new Paint(); 
		LinearGradient shader = new LinearGradient(0, 
		bitmap.getHeight(), 0, bitmapWithReflection.getHeight() 
		+ reflectionGap, 0x70ffffff, 0x00ffffff, TileMode.CLAMP); 
		paint.setShader(shader); //important
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN)); 
		canvas.drawRect(0, height, width, bitmapWithReflection.getHeight() 
		+ reflectionGap, paint); 
		return bitmapWithReflection; 
	}

	
	/**
	 * 图片与边框组合,通过组合图片的方法
	 * @param bm 原图片
	 * @param res 边框资源（八个图块）
	 * @return
	 * 
	 */
	public Bitmap combinateFrame(Bitmap bm, int[] res)
	{
		Bitmap bmp = decodeBitmap(res[0]);
		// 边框的宽高
		final int smallW = bmp.getWidth();
		final int smallH = bmp.getHeight();
		
		// 原图片的宽高
		final int bigW = bm.getWidth();
		final int bigH = bm.getHeight();
		
		int wCount = (int) Math.ceil(bigW * 1.0 / smallW);
		int hCount = (int) Math.ceil(bigH  * 1.0 / smallH);
		
		// 组合后图片的宽高
		int newW = (wCount + 2) * smallW;
		int newH = (hCount + 2) * smallH;
		
		// 重新定义大小
		Bitmap newBitmap = Bitmap.createBitmap(newW, newH, Config.ARGB_8888);
		Canvas canvas = new Canvas(newBitmap);
		Paint p = new Paint();
		p.setColor(Color.TRANSPARENT);
		canvas.drawRect(new Rect(0, 0, newW, newH), p);
		
		Rect rect = new Rect(smallW, smallH, newW - smallW, newH - smallH);
		Paint paint = new Paint();
		paint.setColor(Color.WHITE);
		canvas.drawRect(rect, paint);
		
		// 绘原图
		canvas.drawBitmap(bm, (newW - bigW - 2 * smallW) / 2 + smallW, (newH - bigH - 2 * smallH) / 2 + smallH, null);
		// 绘边框
		// 绘四个角
		int startW = newW - smallW;
		int startH = newH - smallH;
		Bitmap leftTopBm = decodeBitmap(res[0]); // 左上角
		Bitmap leftBottomBm = decodeBitmap(res[2]); // 左下角
		Bitmap rightBottomBm = decodeBitmap(res[4]); // 右下角
		Bitmap rightTopBm = decodeBitmap(res[6]); // 右上角
		
		canvas.drawBitmap(leftTopBm, 0, 0, null);
		canvas.drawBitmap(leftBottomBm, 0, startH, null);
		canvas.drawBitmap(rightBottomBm, startW, startH, null);
		canvas.drawBitmap(rightTopBm, startW, 0, null);
		
		leftTopBm.recycle();
		leftTopBm = null;
		leftBottomBm.recycle();
		leftBottomBm = null;
		rightBottomBm.recycle();
		rightBottomBm = null;
		rightTopBm.recycle();
		rightTopBm = null;
		
		// 绘左右边框
		Bitmap leftBm = decodeBitmap(res[1]);
		Bitmap rightBm = decodeBitmap(res[5]);
		for (int i = 0, length = hCount; i < length; i++)
		{
			int h = smallH * (i + 1);
			canvas.drawBitmap(leftBm, 0, h, null);
			canvas.drawBitmap(rightBm, startW, h, null);
		}
		
		leftBm.recycle();
		leftBm = null;
		rightBm.recycle();
		rightBm = null;
		
		// 绘上下边框
		Bitmap bottomBm = decodeBitmap(res[3]);
		Bitmap topBm = decodeBitmap(res[7]);
		for (int i = 0, length = wCount; i < length; i++)
		{
			int w = smallW * (i + 1);
			canvas.drawBitmap(bottomBm, w, startH, null);
			canvas.drawBitmap(topBm, w, 0, null);
		}
		
		bottomBm.recycle();
		bottomBm = null;
		topBm.recycle();
		topBm = null;
		
		canvas.save(Canvas.ALL_SAVE_FLAG);
		canvas.restore();
		
		return newBitmap;
	}
	
	
	/**
	 * 功能 ： 截取图片的中间的200X200的区域
	 * @param bm
	 * @return
	 * 
	 */
	public Bitmap cropCenter(Bitmap bm)
	{
		int dstWidth = 200;
        int dstHeight = 200;
        int startWidth = (bm.getWidth() - dstWidth)/2;
        int startHeight = ((bm.getHeight() - dstHeight) / 2);
        Rect src = new Rect(startWidth, startHeight, startWidth + dstWidth, startHeight + dstHeight);
        return dividePart(bm, src);
	}
	
	
	/**
	 * 功能 ： 剪切图片
	 * @param bmp 被剪切的图片
	 * @param src 剪切的位置
	 * @return 剪切后的图片
	 * 
	 */
	public Bitmap dividePart(Bitmap bmp, Rect src)
	{
		int width = src.width();
		int height = src.height();
		Rect des = new Rect(0, 0, width, height);
		Bitmap croppedImage = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(croppedImage);
		canvas.drawBitmap(bmp, src, des, null);
		return croppedImage;
	}

	
	/**
	 * 功能 ： 组合涂鸦图片和源图片
	 * @param src 源图片
	 * @param watermark 涂鸦图片
	 * @return
	 * 
	 */
	public Bitmap doodle(Bitmap src, Bitmap watermark)
	{
		// 另外创建一张图片
		Bitmap newb = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Config.ARGB_8888);// 创建一个新的和SRC长度宽度一样的位图
		Canvas canvas = new Canvas(newb);
		canvas.drawBitmap(src, 0, 0, null);// 在 0，0坐标开始画入原图片src
		canvas.drawBitmap(watermark, (src.getWidth() - watermark.getWidth()) / 2, (src.getHeight() - watermark.getHeight()) / 2, null); // 涂鸦图片画到原图片中间位置
		canvas.save(Canvas.ALL_SAVE_FLAG);
		canvas.restore();
		
		watermark.recycle();
		watermark = null;
		
		return newb;
	}

	
	/**
	 *   功能 ： bitmap转成字节数组
	 * Bitmap to byte array
	 * @param bm
	 * @return
	 * 
	 */
	public static byte[] Bitmap2Bytes(Bitmap bm)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
		return baos.toByteArray();
	}
	
	
	/**
	 * 功能 ： 字节数组转换成Bitmap
	 * @param buffer
	 * @return
	 * 
	 */
	public static Bitmap byte2Bitmap(byte[] buffer)
	{
		return BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
	}
		
	
	/**
	 * 功能  : Bitmap转换成Drawable
	 * @param bmp
	 * @return
	 * 
	 */
	public static Drawable bitmap2Drawable(Bitmap bmp)
	{
		return new BitmapDrawable(bmp);
	}
		
	
	/**
	* 功能 ： 将BitmapDrawable转换成Bitmap
	* @param drawable
	* @return
	* 
	*/
	public static Bitmap drawable2Bitmap(BitmapDrawable drawable)
	{
		return drawable.getBitmap();
	}

}
