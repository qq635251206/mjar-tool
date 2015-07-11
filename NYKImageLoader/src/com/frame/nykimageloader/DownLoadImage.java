package com.frame.nykimageloader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.widget.ImageView;

import com.frame.nykimageloader.ImageTrans.ImageSize;

public class DownLoadImage {

	public static boolean downloadImageUrl(String urlStr, File file) {
		FileOutputStream fos = null;
		InputStream is = null;
		try {
			URL url = new URL(urlStr);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			is = conn.getInputStream();
			fos = new FileOutputStream(file);
			byte[] buf = new byte[512];
			int len = 0;
			while ((len = is.read(buf)) != -1) {
				fos.write(buf, 0, len);
			}
			fos.flush();
		}

		catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (IOException e) {
			}

			try {
				if (fos != null)
					fos.close();
			} catch (IOException e) {
			}
		}

		return false;
	}

	public static Bitmap downloadImageUrl(String urlStr, ImageView imageView) {
		FileOutputStream fos = null;
		InputStream is = null;

		try {
			URL url = new URL(urlStr);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			is = new BufferedInputStream(conn.getInputStream());
			is.mark(is.available());

			Options opts = new Options();
			opts.inJustDecodeBounds = true;
			Bitmap bitmap = BitmapFactory.decodeStream(is, null, opts);

			// 得到iv想显示的宽高
			ImageSize ivSize = ImageTrans.getImageViewSize(imageView);
			opts.inSampleSize = ImageTrans.getInSampleRate(opts, ivSize.width,
					ivSize.height);

			opts.inJustDecodeBounds = false;
			is.reset();
			bitmap = BitmapFactory.decodeStream(is, null, opts);

			conn.disconnect();
			return bitmap;

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (IOException e) {
			}

			try {
				if (fos != null)
					fos.close();
			} catch (IOException e) {
			}
		}

		return null;

	}
}
