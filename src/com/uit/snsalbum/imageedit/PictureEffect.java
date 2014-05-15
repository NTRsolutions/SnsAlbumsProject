package com.uit.snsalbum.imageedit;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;

/**
 * author : 何红辉 		功能:	图片处理类,图像的各种色彩调整.
 * 
 * 1.旋转图像			Bitmap rotateBitmap(Bitmap bmp, float degree);
 * 2.图像的怀旧效果		oldRemeberEffect(Bitmap bmp);
 * 3.图像的锐化效果		sharpenEffect(Bitmap bmp);
 * 4.图像的底片效果		filmEffect(Bitmap bmp);
 * 5.图像的光照效果		sunshineEffect(Bitmap bmp);
 * 6.图像的浮雕效果		embossEffect(Bitmap bmp);
 * 7.图像的模糊效果		blurImage(Bitmap bmp)
 * 8.图像的高斯模糊效果	blurImageAmeliorate(Bitmap bmp);
 * 
 */

public class PictureEffect{

	
	/**
	 * 功能： 图片旋转 
	 * @param bmp
	 *      要旋转的图片
	 * @param degree
	 *      图片旋转的角度，负值为逆时针旋转，正值为顺时针旋转
	 * @return
	 */
	public static Bitmap rotateBitmap(Bitmap bmp, float degree) {
		if ( bmp != null )
		{
			Matrix matrix = new Matrix();
			matrix.postRotate(degree);
			return Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
		}
		else
		{
			return null;
		}
	}

	
	/**
	 * 功能 ： 怀旧效果
	 * @param bmp
	 * @return
	 * 算法：（修改RGB值即可）
	 * 公式：   r、g、b为原RGB值.
	 * R = 0.393r + 0.769g + 0.189b;
	 * G = 0.349r + 0.686g + 0.168b;
	 * B = 0.272r + 0.534g + 0.131b;
	 * 
	 */
	public static Bitmap oldRemeberEffect(Bitmap bmp)
	{
		
		int width = bmp.getWidth();
		int height = bmp.getHeight();
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		
		int oldColor = 0;
		int oldR = 0;
		int oldG = 0;
		int oldB = 0;
		int newR = 0;
		int newG = 0;
		int newB = 0;
		
		int[] pixels = new int[width * height];
		bmp.getPixels(pixels, 0, width, 0, 0, width, height);
		for (int i = 0; i < height; i++)
		{
			for (int k = 0; k < width; k++)
			{
				oldColor = pixels[width * i + k];
				oldR = Color.red(oldColor);
				oldG = Color.green(oldColor);
				oldB = Color.blue(oldColor);
				newR = (int) (0.393 * oldR + 0.769 * oldG + 0.189 * oldB);
				newG = (int) (0.349 * oldR + 0.686 * oldG + 0.168 * oldB);
				newB = (int) (0.272 * oldR + 0.534 * oldG + 0.131 * oldB);
				int newColor = Color.argb(255, newR > 255 ? 255 : newR,
											newG > 255 ? 255 : newG, newB > 255 ? 255 : newB);
				pixels[width * i + k] = newColor;
			}
		}
		
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
	
		return bitmap;
	}		// end of oldRemeber()
	
	
	/**
	 *  功能： 图片锐化（拉普拉斯变换）:将拉普拉斯矩阵中的项与相应点的RGB值之积再乘以相应的系数的和作为当前点的RGB值。
	 * @param bmp
	 * @return
	 * 
	 */
	public static Bitmap sharpenEffect(Bitmap bmp)
	{
		// 拉普拉斯矩阵
		int[] laplacian = new int[] { -1, -1, -1, -1, 9, -1, -1, -1, -1 };
		
		int width = bmp.getWidth();
		int height = bmp.getHeight();
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		
		int oldColor = 0;
		int oldR = 0;
		int oldG = 0;
		int oldB = 0;
		int newR = 0;
		int newG = 0;
		int newB = 0;
		
		int idx = 0;
		float alpha = 0.3F;
		int[] pixels = new int[width * height];
		bmp.getPixels(pixels, 0, width, 0, 0, width, height);
		for (int i = 1, length = height - 1; i < length; i++)
		{
			for (int k = 1, len = width - 1; k < len; k++)
			{
				idx = 0;
				for (int m = -1; m <= 1; m++)
				{
					for (int n = -1; n <= 1; n++)
					{
						oldColor = pixels[(i + n) * width + k + m];
						oldR = Color.red(oldColor);
						oldG = Color.green(oldColor);
						oldB = Color.blue(oldColor);
						
						newR = newR + (int) (oldR * laplacian[idx] * alpha);
						newG = newG + (int) (oldG * laplacian[idx] * alpha);
						newB = newB + (int) (oldB * laplacian[idx] * alpha);
						idx++;
					}
				}
				
				newR = Math.min(255, Math.max(0, newR));
				newG = Math.min(255, Math.max(0, newG));
				newB = Math.min(255, Math.max(0, newB));
				
				pixels[i * width + k] = Color.argb(255, newR, newG, newB);
				newR = 0;
				newG = 0;
				newB = 0;
			}
		}
		
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		return bitmap;
	}	// end of sharpenImageAmeliorate()
	
	
	/**
	 * 功能： 浮雕效果:用前一个像素点的RGB值分别减去当前像素点的RGB值并加上127作为当前像素点的RGB值
	 * 求B点的浮雕效果如下：
	 * B.r = C.r - B.r + 127; 	B.g = C.g - B.g + 127;	B.b = C.b - B.b + 127;
	 * @param bmp
	 * @return
	 * 
	 */
	public static Bitmap embossEffect(Bitmap bmp)
	{
		int width = bmp.getWidth();
		int height = bmp.getHeight();
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		
		int pixR = 0;
		int pixG = 0;
		int pixB = 0;
		int pixColor = 0;
		int newR = 0;
		int newG = 0;
		int newB = 0;
		
		int[] pixels = new int[width * height];
		bmp.getPixels(pixels, 0, width, 0, 0, width, height);
		int pos = 0;
		for (int i = 1, length = height - 1; i < length; i++)
		{
			for (int k = 1, len = width - 1; k < len; k++)
			{
				pos = i * width + k;
				pixColor = pixels[pos];
				
				pixR = Color.red(pixColor);
				pixG = Color.green(pixColor);
				pixB = Color.blue(pixColor);
				
				pixColor = pixels[pos + 1];
				newR = Color.red(pixColor) - pixR + 127;
				newG = Color.green(pixColor) - pixG + 127;
				newB = Color.blue(pixColor) - pixB + 127;
				
				newR = Math.min(255, Math.max(0, newR));
				newG = Math.min(255, Math.max(0, newG));
				newB = Math.min(255, Math.max(0, newB));
				
				pixels[pos] = Color.argb(255, newR, newG, newB);
			}
		}
		
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		return bitmap;
	}	// end of embossEffect()
	
	
	/**
	 * 功能： 底片效果算法原理：将当前像素点的RGB值分别与255之差后的值作为当前点的RGB值。
	 * 例：ABC 求B点的底片效果：
	 * B.r = 255 - B.r; 	B.g = 255 - B.g; 	B.b = 255 - B.b;
	 * @param bmp
	 * @return
	 * 
	 */
	public static Bitmap filmEffect(Bitmap bmp)
	{
		// RGBA的最大值
		final int MAX_VALUE = 255;
		int width = bmp.getWidth();
		int height = bmp.getHeight();
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		
		int pixR = 0;
		int pixG = 0;
		int pixB = 0;
		
		int pixColor = 0;
		
		int newR = 0;
		int newG = 0;
		int newB = 0;
		
		int[] pixels = new int[width * height];
		bmp.getPixels(pixels, 0, width, 0, 0, width, height);
		int pos = 0;
		for (int i = 1, length = height - 1; i < length; i++)
		{
			for (int k = 1, len = width - 1; k < len; k++)
			{
				pos = i * width + k;
				pixColor = pixels[pos];
				
				pixR = Color.red(pixColor);
				pixG = Color.green(pixColor);
				pixB = Color.blue(pixColor);
				
				newR = MAX_VALUE - pixR;
				newG = MAX_VALUE - pixG;
				newB = MAX_VALUE - pixB;
				
				newR = Math.min(MAX_VALUE, Math.max(0, newR));
				newG = Math.min(MAX_VALUE, Math.max(0, newG));
				newB = Math.min(MAX_VALUE, Math.max(0, newB));
				
				pixels[pos] = Color.argb(MAX_VALUE, newR, newG, newB);
			}
		}
		
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		return bitmap;
	}	// end of filmEffect()
	
	
	/**
	 * 功能： 光照效果:算法原理：图片上面的像素点按照给定圆心，按照圆半径的变化，像素点的RGB值分别加上相应的值作为当前点的RGB值。
	 * 例：
		ABCDE
		FGHIJ
		KLMNO
		如果指定H点为光照效果的中心，半径为两个像素点，那么G点RGB值分别加上的值会比F点的要大，
		因为RGB值越大，就越接近白色，所以G点看起来比F点要白，也就是距光照中心越近，看效果图
	 * @param bmp
	 * @return
	 * 
	 */
	public static Bitmap sunshineEffect(Bitmap bmp)
	{
		final int width = bmp.getWidth();
		final int height = bmp.getHeight();
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		
		int pixR = 0;
		int pixG = 0;
		int pixB = 0;
		
		int pixColor = 0;
		
		int newR = 0;
		int newG = 0;
		int newB = 0;
		
		int centerX = width / 2;
		int centerY = height / 2;
		int radius = Math.min(centerX, centerY);
		
		final float strength = 150F; // 光照强度 100~150
		int[] pixels = new int[width * height];
		bmp.getPixels(pixels, 0, width, 0, 0, width, height);
		int pos = 0;
		for (int i = 1, length = height - 1; i < length; i++)
		{
			for (int k = 1, len = width - 1; k < len; k++)
			{
				pos = i * width + k;
				pixColor = pixels[pos];
				
				pixR = Color.red(pixColor);
				pixG = Color.green(pixColor);
				pixB = Color.blue(pixColor);
				
				newR = pixR;
				newG = pixG;
				newB = pixB;
				
				// 计算当前点到光照中心的距离，平面座标系中求两点之间的距离
				int distance = (int) (Math.pow((centerY - i), 2) + Math.pow(centerX - k, 2));
				if (distance < radius * radius)
				{
					// 按照距离大小计算增加的光照值
					int result = (int) (strength * (1.0 - Math.sqrt(distance) / radius));
					newR = pixR + result;
					newG = pixG + result;
					newB = pixB + result;
				}
				
				newR = Math.min(255, Math.max(0, newR));
				newG = Math.min(255, Math.max(0, newG));
				newB = Math.min(255, Math.max(0, newB));
				
				pixels[pos] = Color.argb(255, newR, newG, newB);
			}
		}
		
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		return bitmap;
	}
	
	
	/**
	 * 功能： 模糊效果
	 * @param bmp
	 * @return
	 * 
	 */
	public static Bitmap blurImage(Bitmap bmp)
	{
		int width = bmp.getWidth();
		int height = bmp.getHeight();
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		
		int pixColor = 0;
		
		int newR = 0;
		int newG = 0;
		int newB = 0;
		
		int newColor = 0;
		
		int[][] colors = new int[9][3];
		for (int i = 1, length = width - 1; i < length; i++)
		{
			for (int k = 1, len = height - 1; k < len; k++)
			{
				for (int m = 0; m < 9; m++)
				{
					int s = 0;
					int p = 0;
					switch(m)
					{
					case 0:
						s = i - 1;
						p = k - 1;
						break;
					case 1:
						s = i;
						p = k - 1;
						break;
					case 2:
						s = i + 1;
						p = k - 1;
						break;
					case 3:
						s = i + 1;
						p = k;
						break;
					case 4:
						s = i + 1;
						p = k + 1;
						break;
					case 5:
						s = i;
						p = k + 1;
						break;
					case 6:
						s = i - 1;
						p = k + 1;
						break;
					case 7:
						s = i - 1;
						p = k;
						break;
					case 8:
						s = i;
						p = k;
					}
					pixColor = bmp.getPixel(s, p);
					colors[m][0] = Color.red(pixColor);
					colors[m][1] = Color.green(pixColor);
					colors[m][2] = Color.blue(pixColor);
				}
				
				for (int m = 0; m < 9; m++)
				{
					newR += colors[m][0];
					newG += colors[m][1];
					newB += colors[m][2];
				}
				
				newR = (int) (newR / 9F);
				newG = (int) (newG / 9F);
				newB = (int) (newB / 9F);
				
				newR = Math.min(255, Math.max(0, newR));
				newG = Math.min(255, Math.max(0, newG));
				newB = Math.min(255, Math.max(0, newB));
				
				newColor = Color.argb(255, newR, newG, newB);
				bitmap.setPixel(i, k, newColor);
				
				newR = 0;
				newG = 0;
				newB = 0;
			}
		}
		
		return bitmap;
	}
	
	/**
	 * 功能： 柔化效果(高斯模糊)
	 * @param bmp
	 * @return
	 * 
	 */
	public static Bitmap blurImageAmeliorate(Bitmap bmp)
	{
		// 高斯矩阵
		int[] gauss = new int[] { 1, 2, 1, 2, 4, 2, 1, 2, 1 };
		
		int width = bmp.getWidth();
		int height = bmp.getHeight();
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		
		int pixR = 0;
		int pixG = 0;
		int pixB = 0;
		
		int pixColor = 0;
		
		int newR = 0;
		int newG = 0;
		int newB = 0;
		
		int delta = 16; // 值越小图片会越亮，越大则越暗
		
		int idx = 0;
		int[] pixels = new int[width * height];
		bmp.getPixels(pixels, 0, width, 0, 0, width, height);
		for (int i = 1, length = height - 1; i < length; i++)
		{
			for (int k = 1, len = width - 1; k < len; k++)
			{
				idx = 0;
				for (int m = -1; m <= 1; m++)
				{
					for (int n = -1; n <= 1; n++)
					{
						pixColor = pixels[(i + m) * width + k + n];
						pixR = Color.red(pixColor);
						pixG = Color.green(pixColor);
						pixB = Color.blue(pixColor);
						
						newR = newR + (pixR * gauss[idx]);
						newG = newG + (pixG * gauss[idx]);
						newB = newB + (pixB * gauss[idx]);
						idx++;
					}
				}
				
				newR /= delta;
				newG /= delta;
				newB /= delta;
				
				newR = Math.min(255, Math.max(0, newR));
				newG = Math.min(255, Math.max(0, newG));
				newB = Math.min(255, Math.max(0, newB));
				
				pixels[i * width + k] = Color.argb(255, newR, newG, newB);
				
				newR = 0;
				newG = 0;
				newB = 0;
			}
		}
		
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

		return bitmap;
	}

}
