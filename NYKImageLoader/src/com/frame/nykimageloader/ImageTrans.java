package com.frame.nykimageloader;

import java.lang.reflect.Field;

import android.graphics.BitmapFactory.Options;
import android.util.DisplayMetrics;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

public class ImageTrans {

	public static class ImageSize {
		int width;
		int height;
	}

	/**
	 * ImageView适当的压缩的宽和高
	 * 
	 * @param imageView
	 * @return
	 */
	public static ImageSize getImageViewSize(ImageView imageView) {
		// 得到iv的实际宽高，如果没在布局中的宽高，如果没在iv最大值设宽高，如果没屏幕宽高
		ImageSize imageSize = new ImageSize();
		DisplayMetrics displayMetrics = imageView.getContext().getResources()
				.getDisplayMetrics();

		LayoutParams layoutParams = (LayoutParams) imageView.getLayoutParams();

		// iv实际的宽
		int width = imageView.getWidth();// 实际
		if (width <= 0) {
			width = layoutParams.width;// 布局
		}
		if (width <= 0) {
			// width =imageView.getMaxWidth();
			width = getImageViewFieldValue(imageView, "mMaxWidth");// 最大值
		}
		if (width <= 0) {
			width = displayMetrics.widthPixels;// 屏幕
		}

		int height = imageView.getHeight();
		if (height <= 0) {
			height = layoutParams.height;
		}
		if (height <= 0) {
			height = getImageViewFieldValue(imageView, "mMaxHeight");
		}
		if (height <= 0) {
			height = displayMetrics.heightPixels;
		}
		imageSize.width = width;
		imageSize.height = height;

		return imageSize;

	}

	/**
	 * 通过反射得到实际iv的最大值 兼容2.3
	 * 
	 * @param imageView
	 * @param string
	 * @return
	 */
	private static int getImageViewFieldValue(ImageView imageView,
			String fieldName) {

		int value = 0;

		Field field;
		try {
			field = ImageView.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			int fieldValue = field.getInt(imageView);
			if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
				value = fieldValue;
				;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return value;
	}

	/**
	 * 根据需求的宽和高以及图片实际的宽和高计算SampleSize
	 * 
	 * @param options
	 * @param width
	 * @param height
	 * @return
	 */
	public static int getInSampleRate(Options options, int reqWidth,
			int reqHeight) {
		int width = options.outWidth;
		int height = options.outHeight;

		int inSampleSize = 1;

		if (width > reqWidth || height > reqHeight) {
			int widthRadio = Math.round(width * 1.0f / reqWidth);
			int heightRadio = Math.round(height * 1.0f / reqHeight);

			inSampleSize = Math.max(widthRadio, heightRadio);
		}
		return inSampleSize;

	}

}
