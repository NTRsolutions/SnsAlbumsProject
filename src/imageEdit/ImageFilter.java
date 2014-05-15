package imageEdit;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;


/**
 * 
 * @author 何红辉		类功能 : 图像滤镜类(单例模式)			date: 8.8
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

public class ImageFilter {

	private static ImageFilter filterInstance = new ImageFilter();			// 滤镜实例
	public float BrightnessFactor = 0.25f;
	public float ContrastFactor = 0f;										// 常量因子,范围为-1到1
	
	/**
	 * 	功能 ：返回滤镜实例
	 * @return ImageFilter
	 * 
	 */
	public static ImageFilter getInstance()
	{
		if ( filterInstance == null)
		{
			filterInstance = new ImageFilter();
		}

		return filterInstance;
	}
	
	
	/**
	 * 	功能 ：  高亮对比度特效
	 * @return ImageData
	 * 
	 */
	public ImageData BrightFilter(Bitmap olgImg) {
		
		ImageData image = new ImageData(olgImg);
		int width = image.getWidth();
		int height = image.getHeight();
		int r, g, b;
		// Convert to integer factors
		int bfi = (int) (BrightnessFactor * 255);
		float cf = 1f + ContrastFactor;
		cf *= cf;
		int cfi = (int) (cf * 32768) + 1;
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				r = image.getRComponent(x, y);
				g = image.getGComponent(x, y);
				b = image.getBComponent(x, y);
				// Modify brightness (addition)
				if (bfi != 0) {
					// Add brightness
					int ri = r + bfi;
					int gi = g + bfi;
					int bi = b + bfi;
					// Clamp to byte boundaries
					r = ri > 255 ? 255 : (ri < 0 ? 0 : ri);
					g = gi > 255 ? 255 : (gi < 0 ? 0 : gi);
					b = bi > 255 ? 255 : (bi < 0 ? 0 : bi);
				}
				// Modifiy contrast (multiplication)
				if (cfi != 32769) {
					// Transform to range [-128, 127]
					int ri = r - 128;
					int gi = g - 128;
					int bi = b - 128;

					// Multiply contrast factor
					ri = (ri * cfi) >> 15;
					gi = (gi * cfi) >> 15;
					bi = (bi * cfi) >> 15;

					// Transform back to range [0, 255]
					ri = ri + 128;
					gi = gi + 128;
					bi = bi + 128;

					// Clamp to byte boundaries
					r = ri > 255 ? 255 : (ri < 0 ? 0 : ri);
					g = gi > 255 ? 255 : (gi < 0 ? 0 : gi);
					b = bi > 255 ? 255 : (bi < 0 ? 0 : bi);
				}
				image.setPixelColor(x, y, r, g, b);
			}
		}
		return image;
	}
	
	
	/**
	 * 	功能 ： 冰冻滤镜
	 * @return ImageData
	 * 
	 */
	public ImageData IceFilter(Bitmap olgImg) {
		
		ImageData image = new ImageData(olgImg); 
		int width = image.getWidth();
		int height = image.getHeight();
		int R, G, B, pixel;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				R = image.getRComponent(x, y); // 获取RGB三原色
				G = image.getGComponent(x, y);
				B = image.getBComponent(x, y);

				pixel = R - G - B;
				pixel = pixel * 3 / 2;
				if (pixel < 0)
					pixel = -pixel;
				if (pixel > 255)
					pixel = 255;
				R = pixel; 

				pixel = G - B - R;
				pixel = pixel * 3 / 2;
				if (pixel < 0)
					pixel = -pixel;
				if (pixel > 255)
					pixel = 255;
				G = pixel;

				pixel = B - R - G;
				pixel = pixel * 3 / 2;
				if (pixel < 0)
					pixel = -pixel;
				if (pixel > 255)
					pixel = 255;
				B = pixel;
				image.setPixelColor(x, y, R, G, B);
			} // x
		} // y
		
		return image;
	}
	
	
	/**
	 * 	功能 ： 熔铸
	 * @return ImageData
	 * 
	 */
	public ImageData MoltenFilter(Bitmap olgImg) {
			
		ImageData image = new ImageData( olgImg ); 
		int width = image.getWidth();
		int height = image.getHeight();
		int R, G, B, pixel;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				R = image.getRComponent(x, y); // 获取RGB三原色
				G = image.getGComponent(x, y);
				B = image.getBComponent(x, y);

				pixel = R * 128 / (G + B + 1);
				if (pixel < 0)
					pixel = 0;
				if (pixel > 255)
					pixel = 255;
				R = pixel;

				pixel = G * 128 / (B + R + 1);
				if (pixel < 0)
					pixel = 0;
				if (pixel > 255)
					pixel = 255;
				G = pixel;

				pixel = B * 128 / (R + G + 1);
				if (pixel < 0)
					pixel = 0;
				if (pixel > 255)
					pixel = 255;
				B = pixel;
				image.setPixelColor(x, y, R, G, B);
			} // x
		} // y
		
		return image;
	}
	
	/**
	 * 	功能 ： 照亮边缘
	 * @return ImageData
	 * 
	 */
	public ImageData GlowingEdgeFilter( Bitmap olgImg) {
		
		ImageData image = new ImageData( olgImg ); 
		int width = image.getWidth();
		int height = image.getHeight();
		// 图像实际处理区域
		// 不考虑最右 1 列，和最下 1 行
		int rectTop = 0;
		int rectBottom = height - 1;
		int rectLeft = 0;
		int rectRight = width - 1;
		int pixel;

		int R, G, B;
		for (int y = rectTop; y < rectBottom; y++) {
			for (int x = rectLeft; x < rectRight; x++) {
				{
					pixel = (int) (Math.pow((image.getBComponent(x, y) - image.getBComponent(x, y, width)), 2) + Math
							.pow((image.getBComponent(x, y) - image.getBComponent(x, y, 1)), 2));
					pixel = (int) (Math.sqrt(pixel) * 2);

					if (pixel < 0)
						pixel = 0;
					if (pixel > 255)
						pixel = 255;

					B = pixel;
				}
				{
					pixel = (int) (Math.pow((image.getGComponent(x, y) - image.getGComponent(x, y, width)), 2) + Math
							.pow((image.getGComponent(x, y) - image.getGComponent(x, y, 1)), 2));
					pixel = (int) (Math.sqrt(pixel) * 2);

					if (pixel < 0)
						pixel = 0;
					if (pixel > 255)
						pixel = 255;

					G = pixel;
				}
				{
					pixel = (int) (Math.pow((image.getRComponent(x, y) - image.getRComponent(x, y, width)), 2) + Math
							.pow((image.getRComponent(x, y) - image.getRComponent(x, y, 1)), 2));
					pixel = (int) (Math.sqrt(pixel) * 2);

					if (pixel < 0)
						pixel = 0;
					if (pixel > 255)
						pixel = 255;

					R = pixel;
				}

				image.setPixelColor(x, y, R, G, B);
			} // x
		} // y

		return image;
	}
	
	
	/**
	 * 	功能 ： 连环画滤镜
	 * @return ImageData
	 * 
	 */
	public ImageData ComicFilter( Bitmap olgImg) {
		
		ImageData image = new ImageData( olgImg ); 
		int width = image.getWidth();
		int height = image.getHeight();
		int R, G, B, pixel;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				R = image.getRComponent(x, y); // 获取RGB三原色
				G = image.getGComponent(x, y);
				B = image.getBComponent(x, y);

				// R = |g – b + g + r| * r / 256;
				pixel = G - B + G + R;
				if (pixel < 0)
					pixel = -pixel;
				pixel = pixel * R / 256;
				if (pixel > 255)
					pixel = 255;
				R = pixel;

				// G = |b – g + b + r| * r / 256;
				pixel = B - G + B + R;
				if (pixel < 0)
					pixel = -pixel;
				pixel = pixel * R / 256;
				if (pixel > 255)
					pixel = 255;
				G = pixel;

				// B = |b – g + b + r| * g / 256;
				pixel = B - G + B + R;
				if (pixel < 0)
					pixel = -pixel;
				pixel = pixel * G / 256;
				if (pixel > 255)
					pixel = 255;
				B = pixel;
				image.setPixelColor(x, y, R, G, B);
			}
		}
		Bitmap bitmap = image.getDstBitmap();
		bitmap = toGrayscale( bitmap ); 	// 图片灰度化处理
		image = new ImageData(bitmap);
		return image;
	}
	
	/**
	 * 	功能 ：  图片灰度化
	 * @param bmpOriginal
	 * @return ImageData
	 * 
	 */
	private Bitmap toGrayscale(Bitmap bmpOriginal) {
		
		int width, height;
		height = bmpOriginal.getHeight();
		width = bmpOriginal.getWidth();

		Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		Canvas c = new Canvas(bmpGrayscale);
		Paint paint = new Paint();
		ColorMatrix cm = new ColorMatrix();
		cm.setSaturation(0); // 灰色
		ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
		paint.setColorFilter(f);
		c.drawBitmap(bmpOriginal, 0, 0, paint);
		return bmpGrayscale;
	}
}
