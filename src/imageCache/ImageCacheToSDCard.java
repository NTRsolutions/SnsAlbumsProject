package imageCache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

/**
 * Copyright (c) 2012,UIT-ESPACE( TEAM: UIT-GEEK) All rights reserved.
 * 
 * @Title: ImageCacheToSDCard.java
 * @Package: imageCache
 * @Author: 何红辉(Mr.Simple)
 * @E-mail: bboyfeiyu@gmail.com
 * @Version: V1.0
 * @Date： 2012-11-8 下午7:40:40
 * @Description: 图片缓存类 1.
 * 
 */

public class ImageCacheToSDCard {

	private final String TAG = "图片缓存";
	private String mBigImgCachePath = null;
	private String mSmallImgCachePath = null;
	private final int MB = 1024 * 1024;
	private final int FREE_SD_SPACE = 40;
	private final int CACHE_SIZE = 40;
	private int mCacheType = 1; // 小图缓存
	private final long mTimeDiff = 604800000; // 定义三天动的图片则删除
	private Bitmap mBitmap = null;
	private String mFileName = null;
	private static ImageCacheToSDCard mImageCache = new ImageCacheToSDCard();

	/**
	 * 
	 * @Constructor: 构造函数
	 * @
	 * @Description:
	 */
	private ImageCacheToSDCard() {
		mBigImgCachePath = Environment.getExternalStorageDirectory()
				+ File.separator + "a_sns_cache";
		mSmallImgCachePath = Environment.getExternalStorageDirectory()
				+ File.separator + "a_sns_small_cache";
		// 创建目录
		File file = new File(mSmallImgCachePath);
		if (!file.exists()) {
			file.mkdir();
		}

		file = null ;
		System.gc() ;
		
		file = new File(mBigImgCachePath);
		if (!file.exists()) {
			file.mkdir();
		}

		try{
			// 自动清除过期的图片缓存
			removeExpiredCache( mSmallImgCachePath );
			removeExpiredCache( mBigImgCachePath ) ;
			// 如果内存卡的剩余容量太小,则清除40%的缓存
			remove40PercentCache( mSmallImgCachePath ) ;
			remove40PercentCache( mBigImgCachePath ) ;
		}catch(Exception e){
			e.printStackTrace() ;
		}
		Log.d(TAG, "缓存构造函数");
		
	}

	/**
	 * @Method: getInstance
	 * @Description: 单例模式, 获取ImageCacheToSDCard实例
	 * @return ImageCacheToSDCard 返回类型
	 * @throws
	 */
	public static ImageCacheToSDCard getInstance() {
		if (mImageCache == null) {
			mImageCache = new ImageCacheToSDCard();
		}

		return mImageCache;
	}

	/**
	 * 
	 * @Method: saveBmpToSd
	 * @Description:   将图片保存到SD卡中
	 * @param bm 	        要缓存的图片
	 * @param fileName 图片名 #param type 要缓存的图片类型,大图还是小图
	 * @return void 返回类型
	 * @throws
	 */
	public void saveBmpToSd(Bitmap bmp, String fileName, int type)
			throws Exception {

		mBitmap = bmp;
		mFileName = fileName;
		mCacheType = type;
		// 同步块
		synchronized (this) {
			if (mBitmap == null) {
				Log.w(TAG, "错误-->要缓存的图片为空");
				return;
			}

			// 判断sdcard上的空间,内存卡上的空间小于50MB,则不缓存
			if (FREE_SD_SPACE > freeSpaceOnSd()) {
				Log.w(TAG, "内存卡的剩余容量太小,不进行缓存");
				return;
			}

			// 默认是写小图缓存
			String dirPath = mSmallImgCachePath;
			if (mCacheType == 2) {
				dirPath = mBigImgCachePath;
			}
			File file = new File(dirPath + File.separator + mFileName);
			try {
				if (!file.exists()) {

					file.createNewFile();
					OutputStream outStream = new FileOutputStream(file);
					// 将图片压缩到输出流中
					mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
					// 强制刷新,使数据写入到文件里面
					outStream.flush();
					outStream.close();
					Log.i(TAG, mFileName + "图片已经保存到文件");
					// 更新文件的最后修改时间
					updateFileTime( dirPath, mFileName);
				} else {
					Log.i(TAG, mFileName + "图片已经存在");
				}

			} catch (Exception e) {
				Log.w(TAG, "保存缓存图片错误");
				e.printStackTrace();
			}
		}


	}

	
	/**
	 * @Method: getImageFromSD
	 * @Description:
	 * @param path     从不同的路径获取不同的缓存
	 * @param fileName 要获取的文件名
	 * @return Bitmap 返回类型
	 * @throws
	 */
	public synchronized Bitmap getImageFromSD(int type, String fileName) {
		
		mCacheType = type;
		mFileName = fileName;
		mBitmap = Bitmap.createBitmap(5, 5, Bitmap.Config.ARGB_4444);
		
		String path = mSmallImgCachePath;
		if (2 == mCacheType) {
			path = mBigImgCachePath;
		}
		try {
			File file = new File(path, mFileName);
			InputStream input = new FileInputStream(file);
			// 通过文件流构造图片
			mBitmap = BitmapFactory.decodeStream(input);
			input.close();
			input = null;
			System.gc() ;
			if (mBitmap.getHeight() > 10) {
				Log.d(TAG, mFileName + "从缓存中读取成功");
			}
		} catch (Exception e) {
			Log.d(TAG, "从缓存中读取失败") ;
			e.printStackTrace();
		}

		return mBitmap;
	}

	
	/**
	 * @Method: freeSpaceOnSd
	 * @Description: 计算sdcard上的剩余空间
	 * @return int 返回类型
	 * @throws
	 */
	private int freeSpaceOnSd() {
		StatFs stat = new StatFs(Environment.getExternalStorageDirectory()
				.getPath());
		double sdFreeMB = ((double) stat.getAvailableBlocks() * (double) stat
									.getBlockSize()) / MB;
		Log.d(TAG, "内存卡剩余内存为　：　" + sdFreeMB);
		return (int) sdFreeMB;
	}

	
	/**
	 * @Method: updateFileTime
	 * @Description: 修改文件的最后修改时间
	 * @param fileName
	 * @return void 返回类型
	 * @throws
	 */
	private void updateFileTime(String path, String fileName) {
		File file = new File( path, fileName );
		long newModifiedTime = System.currentTimeMillis();
		file.setLastModified( newModifiedTime );
	}

	
	/**
	 * @Method: removeCache
	 * @Description:  计算存储目录下的文件大小，当文件总大小大于规定的40MB,或者sdcard剩余空间小于40MB的规定
	 *                那么删除40%最近没有被使用的文件
	 * @return void 返回类型
	 * @throws
	 */
	public void remove40PercentCache(String path) throws Exception{

		File dir = new File( path );
		// 获取文件列表
		File[] files = dir.listFiles();
		if ( files.length == 0) {
			return;
		}

		int dirSize = 0;
		// 遍历文件,获取所有文件的总大小
		for (int i = 0; i < files.length; i++) {
			dirSize += files[i].length();
		}

		if (dirSize > CACHE_SIZE * MB || FREE_SD_SPACE > freeSpaceOnSd()) {
			int removeFactor = (int) ((0.4 * files.length) + 1);
			// 按最后修改时间排序
			Arrays.sort(files, new FileLastModifSort());

			Log.i(TAG, "清除40%过期的缓存图片");
			for (int i = 0; i < removeFactor; i++) {
				files[i].delete();
			}

		}

	}

	
	/**
	 * @Method: removeExpiredCache
	 * @Description: 删除过期文件,mTimeDiff为设定的长时间限制,到了这个时间则自动删除
	 * @return void 返回类型
	 * @throws
	 */
	private void removeExpiredCache( String path ) throws Exception{

		File file = new File( path );
		File[] files = file.listFiles();
		for (int i = 0; i < file.length(); i++) {
			if (System.currentTimeMillis() - file.lastModified() > mTimeDiff) {

				Log.i(TAG, "清除过期缓存!");
				files[i].delete();

			}
		}
	}

	
	/**
	 * @Method: clearImageCache
	 * @Description: 手动删除所有缓存
	 * @return void 返回类型
	 * @throws
	 */
	public void clearImageCache() {
		try{
			File file = new File( mBigImgCachePath );
			File[] files = file.listFiles();
			for (File fs : files) {
				fs.delete();
			}
			
			file = new File( mSmallImgCachePath );
			files = file.listFiles();
			for(File fss : files){
				fss.delete() ;
			}
			
		}catch(Exception e){
			e.printStackTrace() ;
		}
		
	}

	
	/**
	 * @ClassName: FileLastModifSort
	 * @Description: 根据文件的最后修改时间进行排序
	 * @Author: Mr.Simple
	 * @E-mail: bboyfeiyu@gmail.com
	 * @Date 2012-11-8 下午8:01:13
	 * 
	 */
	class FileLastModifSort implements Comparator<File> {
		@Override
		public int compare(File arg0, File arg1) {
			if (arg0.lastModified() > arg1.lastModified()) {
				return 1;
			} else if (arg0.lastModified() == arg1.lastModified()) {
				return 0;
			} else {
				return -1;
			}
		}
	}

}
