package com.uit.snsalbum.entry;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;

/**
 * @ClassName: SplashScreen
 * @Description: 启动画面
 * @Author: Mr.Simple (何红辉)
 * @E-mail: bboyfeiyu@gmail.com
 * @Date 2012-11-17 下午5:44:10
 *
 */

public class SplashScreen extends Activity {

	private long ms = 0;
	private long splashTime = 2500;
	private boolean splashActive = true;
	private boolean paused = false;
	private ImageView splashImgView = null;

	/**
	 * (非 Javadoc,覆写的方法)
	 * 
	 * @Title: onCreate
	 * @Description:
	 * @param savedInstanceState
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// 全屏设置，隐藏窗口所有装饰
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		// 标题是属于View的，所以窗口所有的修饰部分被隐藏后标题依然有效,需要去掉标题
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.splash);

		splashImgView = (ImageView) findViewById(R.id.image);
		// AlphaAnimation动画效果
		AlphaAnimation animation = new AlphaAnimation(0.7f, 1.0f);
		animation.setDuration(1500);
		splashImgView.startAnimation(animation);

		// 将当前页面添加到activity的哈希表中
		MainViewActivity.addActivityToHashSet(this);

		Thread mythread = new Thread() {
			@Override
			public void run() {
				try {
					while (splashActive && ms < splashTime) {
						if (!paused) {
							ms = ms + 100;
						}
						sleep(100); // 线程睡眠十毫秒
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					// Intent intent = new Intent(SplashScreen.this,
					// LoginActivity.class);
					Intent intent = new Intent(SplashScreen.this,
							MainViewActivity.class);
					startActivity(intent);
					overridePendingTransition(R.anim.slide_left,
							R.anim.slide_right);
				}
			}
		};
		mythread.start();
	}

}
