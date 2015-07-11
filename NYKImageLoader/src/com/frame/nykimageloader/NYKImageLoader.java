package com.frame.nykimageloader;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

import com.frame.nykimageloader.ImageTrans.ImageSize;

public class NYKImageLoader {
	// 单例实例
	private static NYKImageLoader SingleInstance;

	// LRU
	private LruCache<String, Bitmap> mLruCache;

	// 线程池
	private ExecutorService mThreadPool;

	private static final int DEAFULT_THREAD_COUNT = 1;

	// 选择调度方式
	private Type mType = Type.LIFO;

	// 任务队列
	private LinkedList<Runnable> mTaskQueue;

	// 调度线程
	private Thread mPoolThread;
	private Handler mPoolThreadHandler;

	// main线程的handler
	private Handler UIHandler;
	private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0);
	private Semaphore mSemaphoreThreadPool;

	private boolean isDiskCacheEnable = true;

	private static final String TAG = "NYKImageLoader";

	public enum Type {
		LIFO, FIFO;
	}

	private NYKImageLoader(int threadCount, Type type) {

		init(threadCount, type);
	}

	public static NYKImageLoader getInstance() {
		if (SingleInstance == null) {

			synchronized (NYKImageLoader.class) {
				if (SingleInstance == null) {
					SingleInstance = new NYKImageLoader(DEAFULT_THREAD_COUNT,
							Type.LIFO);
				}
			}

		}
		return SingleInstance;

	}

	public static NYKImageLoader getInstance(int threadCount, Type type) {
		if (SingleInstance == null) {

			synchronized (NYKImageLoader.class) {
				if (SingleInstance == null) {
					SingleInstance = new NYKImageLoader(threadCount, type);
				}
			}

		}
		return SingleInstance;

	}

	public void loadImage(String path, ImageView imageView, boolean isNetWork) {
		imageView.setTag(path);

		if (UIHandler == null) {

			UIHandler = new Handler() {
				public void handleMessage(Message msg) {

					ImgBeanHolder holder = (ImgBeanHolder) msg.obj;

					Bitmap bm = holder.bitmap;
					ImageView imageview = holder.imageView;
					String path = holder.path;

					if (imageview.getTag().toString().equals(path)) {
						imageview.setImageBitmap(bm);
					}
				};
			};

		}

		// 根据path在缓存中获取bitmap
		Bitmap bm = getBitmapFromLruCache(path);

		if (bm != null) {
			refreashBitmap(path, imageView, bm);
		} else {
			addTask(buildTask(path, imageView, isNetWork));
		}

	}

	private synchronized void addTask(Runnable runnable) {
		mTaskQueue.add(runnable);
		// if(mPoolThreadHandler==null)wait();
		try {
			if (mPoolThreadHandler == null)
				mSemaphorePoolThreadHandler.acquire();
		} catch (InterruptedException e) {
		}
		mPoolThreadHandler.sendEmptyMessage(0x110);
	}

	private Runnable buildTask(final String path, final ImageView imageView,
			final boolean isNetWork) {

		return new Runnable() {
			@Override
			public void run() {

				Bitmap bm = null;
				if (isNetWork) {
					File file = getDiskCacheDir(imageView.getContext(),
							md5(path));

					if (file.exists()) {
						// Log.e(TAG, "find image :" + path +
						// " in disk cache .");
						bm = loadImageFromLocal(file.getAbsolutePath(),
								imageView);

					} else {
						if (isDiskCacheEnable)// 可以缓存
						{
							boolean downloadState = DownLoadImage
									.downloadImageUrl(path, file);
							if (downloadState) {
//								Log.e(TAG,
//										"download image :" + path
//												+ " to disk cache . path is "
//												+ file.getAbsolutePath());
								bm = loadImageFromLocal(file.getAbsolutePath(), imageView);
								
							}
						}else {
							//network下载 
							bm = DownLoadImage.downloadImageUrl(path, imageView);
							
						}
					}

				}else {
					bm = loadImageFromLocal(path, imageView);
				}
				// 3、把图片加入到缓存
				addBitmapToLruCache(path, bm);
				refreashBitmap(path, imageView, bm);
				mSemaphoreThreadPool.release();

			}

		};
	}
	/**
	 * 将图片加入LruCache
	 * 
	 * @param path
	 * @param bm
	 */
	protected void addBitmapToLruCache(String path, Bitmap bm) {
		if (getBitmapFromLruCache(path) == null)
		{
			if (bm != null)
				mLruCache.put(path, bm);
		}
	}

	private Bitmap loadImageFromLocal(final String path,
			final ImageView imageView) {

		Bitmap bm = null;
		// 加载图片
		// 压缩
		// 获得需要显示的大小
		ImageSize imageSize = ImageTrans.getImageViewSize(imageView);

		// 压缩
		bm = decodeSampledBitmapFromPath(path, imageSize.width,
				imageSize.height);
		return bm;
	}

	private Bitmap decodeSampledBitmapFromPath(String path, int width,
			int height) {
		// 二次取样

		// 不读进内存第一次
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);

		options.inSampleSize = ImageTrans.getInSampleRate(options, width,
				height);
		// 第二次，insamplesize再次解析
		options.inJustDecodeBounds = false;
		Bitmap bitmap = BitmapFactory.decodeFile(path, options);

		return bitmap;
	}

	private File getDiskCacheDir(Context context, String uniqueName) {
		String cachePath;
		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState()))
		{
			cachePath = context.getExternalCacheDir().getPath();
		} else
		{
			cachePath = context.getCacheDir().getPath();
		}
		return new File(cachePath + File.separator + uniqueName);
	}

	/**
	 * 利用签名辅助类，将字符串字节数组
	 * 
	 * @param str
	 * @return
	 */
	public String md5(String str) {
		byte[] digest = null;
		try {
			MessageDigest md = MessageDigest.getInstance("md5");
			digest = md.digest(str.getBytes());
			return bytes2hex02(digest);

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 方式二
	 * 
	 * @param bytes
	 * @return
	 */
	public String bytes2hex02(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		String tmp = null;
		for (byte b : bytes) {
			// 将每个字节与0xFF进行与运算，然后转化为10进制，然后借助于Integer再转化为16进制
			tmp = Integer.toHexString(0xFF & b);
			if (tmp.length() == 1)// 每个字节8为，转为16进制标志，2个16进制位
			{
				tmp = "0" + tmp;
			}
			sb.append(tmp);
		}

		return sb.toString();

	}

	private void refreashBitmap(String path, ImageView imageView, Bitmap bm) {
		Message message = Message.obtain();
		ImgBeanHolder holder = new ImgBeanHolder();
		holder.bitmap = bm;
		holder.path = path;
		holder.imageView = imageView;
		message.obj = holder;
		UIHandler.sendMessage(message);
	}

	private Bitmap getBitmapFromLruCache(String path) {
		return mLruCache.get(path);
	}

	private void init(int threadCount, Type type) {
		initBackThread();

		// 获取最大内存
		int maxMemory = (int) Runtime.getRuntime().maxMemory();

		int cacheMemory = maxMemory / 8;
		mLruCache = new LruCache<String, Bitmap>(cacheMemory) {
			@Override
			protected int sizeOf(String key, Bitmap value) {
				return value.getRowBytes() * value.getHeight();
			}

		};

		mThreadPool = Executors.newFixedThreadPool(threadCount);
		mTaskQueue = new LinkedList<Runnable>();
		mType = type;
		mSemaphoreThreadPool = new Semaphore(threadCount);
	}

	/**
	 * init后台loop
	 */
	private void initBackThread() {

		// 后台轮询线程
		mPoolThread = new Thread() {
			@Override
			public void run() {
				Looper.prepare();
				mPoolThreadHandler = new Handler() {
					@Override
					public void handleMessage(Message msg) {
						// 线程池去取出一个任务进行执行
						mThreadPool.execute(getTask());
						try {
							mSemaphoreThreadPool.acquire();
						} catch (InterruptedException e) {
						}
					}
				};
				// 释放一个信号量
				mSemaphorePoolThreadHandler.release();
				Looper.loop();
			};
		};

		mPoolThread.start();
	}

	// 从任务队列中取一个
	private Runnable getTask() {
		if (mType == Type.FIFO) {
			return mTaskQueue.removeFirst();
		} else if (mType == Type.LIFO) {
			return mTaskQueue.removeLast();
		}
		return null;
	}

	private class ImgBeanHolder {
		Bitmap bitmap;
		ImageView imageView;
		String path;
	}
}
