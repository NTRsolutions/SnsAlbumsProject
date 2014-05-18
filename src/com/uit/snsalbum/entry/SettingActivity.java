package com.uit.snsalbum.entry;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

import com.uit.snsalbum.R;
import com.uit.snsalbum.utils.ImageCacheToSDCard;


/**
 * 
 * @ClassName: SettingActivity
 * @Description: 设置界面
 * @Author: Mr.Simple
 * @E-mail: bboyfeiyu@gmail.com
 * @Date 2012-11-16 下午6:15:58
 * 
 */
public class SettingActivity extends PreferenceActivity implements
		OnPreferenceChangeListener, OnPreferenceClickListener {

	static final String TAG = "PreferenceActivity";

	SharedPreferences preference = null;
	CheckBoxPreference updateCheckBoxPreference = null;
	ListPreference lististPreference = null;
	CheckBoxPreference isneilflag_CheckBoxPreference = null;
	CheckBoxPreference clearCache_CheckBoxPreference = null;
	EditTextPreference usernameEditTextPreference = null;
	EditTextPreference passwordEditTextPreference = null;

	
	/*
	 * (非 Javadoc,覆写的方法) 
	 * <p>Title: onCreate</p> 
	 * <p>Description: </p> 
	 * @param savedInstanceState 
	 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 设置显示Preferences
		addPreferencesFromResource(R.layout.setting);
		// 获得SharedPreferences
		preference = PreferenceManager.getDefaultSharedPreferences(this);

		// 找到preference对应的Key标签并转化
		updateCheckBoxPreference = (CheckBoxPreference) findPreference(
															getString(R.string.update_key));
		lististPreference = (ListPreference) findPreference(
													getString(R.string.auto_update_frequency_key));
		isneilflag_CheckBoxPreference = (CheckBoxPreference) findPreference(
													getString(R.string.isneilflag_key));
		usernameEditTextPreference = (EditTextPreference) findPreference(
													getString(R.string.username_key));
		passwordEditTextPreference = (EditTextPreference) findPreference(
													getString(R.string.password_key));
		clearCache_CheckBoxPreference = (CheckBoxPreference) findPreference(
													getString(R.string.clearcache));
		// 为Preference注册监听
		updateCheckBoxPreference.setOnPreferenceChangeListener(this);
		updateCheckBoxPreference.setOnPreferenceClickListener(this);
		clearCache_CheckBoxPreference.setOnPreferenceClickListener( this ) ;

		lististPreference.setOnPreferenceClickListener(this);
		lististPreference.setOnPreferenceChangeListener(this);

		isneilflag_CheckBoxPreference.setOnPreferenceChangeListener(this);
		isneilflag_CheckBoxPreference.setOnPreferenceClickListener(this);

		usernameEditTextPreference.setOnPreferenceChangeListener(this);
		passwordEditTextPreference.setOnPreferenceChangeListener(this);
	}

	/*
	 * (非 Javadoc,覆写的方法) 
	 * <p>Title: onPreferenceClick</p> 
	 * <p>Description: </p> 
	 * @param preference
	 * @return 
	 * @see android.preference.Preference.OnPreferenceClickListener#onPreferenceClick(android.preference.Preference)
	 */
	@Override
	public boolean onPreferenceClick(Preference preference) {
		// 判断是哪个Preference改变了
		if (preference.getKey().equals(getString(R.string.update_key))) {
			Log.e(TAG, getString(R.string.update_key));
		} else if (preference.getKey().equals(
				getString(R.string.isneilflag_key))) {
			Log.e(TAG, getString(R.string.isneilflag_key));
		} else if (preference.getKey().equals(getString(R.string.clearcache))) {
			
			// 弹出确认对话框
			AlertDialog.Builder  builder = new AlertDialog.Builder(SettingActivity.this); 
			AlertDialog dlg = builder.create();
			dlg.setIcon(R.drawable.beaten) ;
			dlg.setTitle("确定清除图片缓存?") ;
			dlg.setButton("确定", new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					ImageCacheToSDCard cache = ImageCacheToSDCard.getInstance();
					cache.clearImageCache() ;
				}
			});
			dlg.setButton2("取消", new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
				}
			}) ;
			
			dlg.show() ;	
		}
		// 返回true表示允许改变
		return true;
	}

	/*
	 * (非 Javadoc,覆写的方法) 
	 * <p>Title: onPreferenceChange</p> 
	 * <p>Description: </p> 
	 * @param preference
	 * @param newValue
	 * @return 
	 * @see android.preference.Preference.OnPreferenceChangeListener#onPreferenceChange(android.preference.Preference, java.lang.Object)
	 */
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// 判断是哪个Preference改变了
		if (preference.getKey().equals(getString(R.string.username_key))) {
			// 账号
			Log.e(TAG, getString(R.string.username_key));
		} else if (preference.getKey().equals(getString(R.string.password_key))) {
			// 密码
			Log.e(TAG, getString(R.string.password_key));

		} else if (preference.getKey().equals(
				getString(R.string.auto_update_frequency_key))) {
			// 列表
			Log.e(TAG, getString(R.string.auto_update_frequency_key));
		}
		// 返回true表示允许改变
		return true;
	}
}
