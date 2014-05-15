package imageEdit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;


/**
 * @ClassName: ToneView 
 * @Description:   图像的色相、饱和度、亮度的调整
 * @Author: Mr.Simple (何红辉)
 * @E-mail: bboyfeiyu@gmail.com 
 * @Date 2012-11-18 下午12:51:46 
 *
 */

public class ToneView
{
	private TextView mSaturation;
	private SeekBar mSaturationBar;			// 饱和度seekbar
	private TextView mHue;
	private SeekBar mHueBar;				// 色相seekbar
	private TextView mLum;
	private SeekBar mLumBar;				// 亮度seekbar
	
	private float mDensity;		
	private static final int TEXT_WIDTH = 50;
	
	private LinearLayout mParent;			// 线性布局
	
	private ColorMatrix mLightnessMatrix;	// 亮度的颜色矩阵
	private ColorMatrix mSaturationMatrix;	// 饱和度的颜色矩阵
	private ColorMatrix mHueMatrix;			// 色相的颜色矩阵
	private ColorMatrix mAllMatrix;			// 整体的颜色矩阵

	private float mLightnessValue = 1F;		// 亮度
	private float mSaturationValue = 0F;	// 饱和度
	private float mHueValue = 0F;			// 色相
	private final int MIDDLE_VALUE = 127;	// 中间值,最大值为255
	
	public ToneView(Context context)
	{
		init(context);
	}
	
	
	/**
	 * @Method: init
	 * @Description: 初始化组件和变量
	 * @param context
	 */
	private void init(Context context)
	{
		// 获取分辨率
		mDensity = context.getResources().getDisplayMetrics().density;
		
		// 文本显示控件的初始化
		mSaturation = new TextView(context);
		mHue = new TextView(context);
		mLum = new TextView(context);

		// 饱和度seekbar初始化
		mSaturationBar = new SeekBar(context);
		mSaturationBar.setMax(255);
		mSaturationBar.setProgress(127);
		mSaturationBar.setTag(1);
		
		// 色相seekbar初始化
		mHueBar = new SeekBar(context);
		mHueBar.setMax(255);
		mHueBar.setProgress(127);
		mHueBar.setTag(2);
		
		// 亮度seekbar初始化
		mLumBar = new SeekBar(context);
		mLumBar.setMax(255);
		mLumBar.setProgress(127);
		mLumBar.setTag(3);
		
		LinearLayout saturation = new LinearLayout(context);
		saturation.setOrientation(LinearLayout.HORIZONTAL);
		saturation.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
																	LayoutParams.WRAP_CONTENT));
		
		LinearLayout.LayoutParams txtLayoutparams = new LinearLayout.LayoutParams((int) (TEXT_WIDTH * mDensity), LayoutParams.MATCH_PARENT);
		mSaturation.setGravity(Gravity.CENTER);
		saturation.addView(mSaturation, txtLayoutparams);
		
		LinearLayout.LayoutParams seekLayoutparams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		saturation.addView(mSaturationBar, seekLayoutparams);
		
		LinearLayout hue = new LinearLayout(context);
		hue.setOrientation(LinearLayout.HORIZONTAL);
		hue.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		
		mHue.setGravity(Gravity.CENTER);
		hue.addView(mHue, txtLayoutparams);
		
		hue.addView(mHueBar, seekLayoutparams);
			
		LinearLayout lum = new LinearLayout(context);
		lum.setOrientation(LinearLayout.HORIZONTAL);
		lum.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		
		mLum.setGravity(Gravity.CENTER);
		lum.addView(mLum, txtLayoutparams);
		lum.addView(mLumBar, seekLayoutparams);
		
		mParent = new LinearLayout(context);
		mParent.setOrientation(LinearLayout.VERTICAL);
		mParent.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
																		LayoutParams.WRAP_CONTENT));
		mParent.addView(saturation);
		mParent.addView(hue);
		mParent.addView(lum);
	}
	
	
	/**
	 * @Method: setSaturationBarListener
	 * @Description: 饱和度监听器
	 * @param l
	 */
	public void setSaturationBarListener(OnSeekBarChangeListener l)
	{
		mSaturationBar.setOnSeekBarChangeListener(l);
	}
	
	

	/**
	 * @Method: setHueBarListener
	 * @Description: 色相监听器
	 * @param l
	 */
	public void setHueBarListener(OnSeekBarChangeListener l)
	{
		mHueBar.setOnSeekBarChangeListener(l);
	}
	
	
	/**
	 * @Method: setLumBarListener
	 * @Description: 亮度监听器
	 * @param l
	 */
	public void setLumBarListener(OnSeekBarChangeListener l)
	{
		mLumBar.setOnSeekBarChangeListener(l);
	}
	
	

	/**
	 * @Method: setSaturation
	 * @Description:  设置饱和度
	 * @param saturation
	 */
	public void setSaturation(int saturation)
	{
		mSaturationValue = (float) (saturation * 1.0D / MIDDLE_VALUE);
	}
	
	

	/**
	 * @Method: setHue
	 * @Description: 设置色相
	 * @param hue
	 */
	public void setHue(int hue)
	{
		mHueValue = (float) (hue * 1.0D / MIDDLE_VALUE);
	}
	
	

	/**
	 * @Method: setLum
	 * @Description: 设置亮度
	 * @param lum
	 */
	public void setLum(int lum)
	{
		mLightnessValue = (float) ((lum - MIDDLE_VALUE) * 1.0D / MIDDLE_VALUE * 180);
	}
	
	
	/**
	 * 功能 ： 调整图像的饱和度、色相、亮灯
	 * @param bitmap	要修改的原图
	 * @param flag       1 为饱和度 . 2为色相  ,3为亮度
	 */
	public Bitmap handleImage(Bitmap originalBmp, int flag)
	{
		Bitmap bmp = Bitmap.createBitmap(originalBmp.getWidth(),
				originalBmp.getHeight(), Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(bmp); 			// 用原图的中间变量进行编辑.
		Paint paint = new Paint(); 		
		paint.setAntiAlias(true); 					// 设置抗锯齿效果
		
		if (null == mAllMatrix)
		{
			mAllMatrix = new ColorMatrix();			// 整理的颜色矩阵
		}
		
		if (null == mLightnessMatrix)
		{
			mLightnessMatrix = new ColorMatrix(); 	// 创建颜亮度颜色矩阵
		}
		
		if (null == mSaturationMatrix)
		{
			mSaturationMatrix = new ColorMatrix();	// 饱和度颜色矩阵
		}
		
		if (null == mHueMatrix)
		{
			mHueMatrix = new ColorMatrix();			// 色相颜色矩阵
		}
		
		switch (flag)
		{		
			case 1: 	// 饱和度改变
				mSaturationMatrix.reset();
				mSaturationMatrix.setSaturation(mSaturationValue);
				break;
			
			case 2: 	// 色相值改变
				mHueMatrix.reset();		
				mHueMatrix.setScale(mHueValue, mHueValue, mHueValue, 1); //  红、绿、蓝三分量按相同的比例,最后一个参数1表示透明度不做变化，此函数详细说明参考  
				break;
				
			case 3: 	// 亮度
				mLightnessMatrix.reset(); // 
				mLightnessMatrix.setRotate(0, mLightnessValue); // 控制让红色区在色轮上旋转的角度 
				mLightnessMatrix.setRotate(1, mLightnessValue); // 控制让绿红色区在色轮上旋转的角度 
				mLightnessMatrix.setRotate(2, mLightnessValue); // 控制让蓝色区在色轮上旋转的角度  
				break;
		}
		
		// 最后进行效果叠加操作.
		mAllMatrix.reset();
		mAllMatrix.postConcat(mHueMatrix);
		mAllMatrix.postConcat(mSaturationMatrix); 			
		mAllMatrix.postConcat(mLightnessMatrix);			
		
		paint.setColorFilter(new ColorMatrixColorFilter(mAllMatrix));	// 设置颜色变换效果  
		canvas.drawBitmap(originalBmp, 0, 0, paint); 					// 用画布重绘整个图
		
		return bmp;
	}
	
}
