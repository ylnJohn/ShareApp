package com.yln.shareapp;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.sina.weibo.sdk.api.ImageObject;
import com.sina.weibo.sdk.api.TextObject;
import com.sina.weibo.sdk.api.WebpageObject;
import com.sina.weibo.sdk.api.WeiboMessage;
import com.sina.weibo.sdk.api.WeiboMultiMessage;
import com.sina.weibo.sdk.api.share.IWeiboHandler;
import com.sina.weibo.sdk.api.share.IWeiboShareAPI;
import com.sina.weibo.sdk.api.share.SendMessageToWeiboRequest;
import com.sina.weibo.sdk.api.share.SendMultiMessageToWeiboRequest;
import com.sina.weibo.sdk.api.share.WeiboShareSDK;
import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.exception.WeiboException;
import com.sina.weibo.sdk.utils.Utility;

public class SinaWbShare {

	private static final String TAG = "SinaWbShare";

	public static final int SHARE_SINGLE = 1;

	public static final int SHARE_MULTI = 2;

	private IWeiboShareAPI mWeiboShareAPI;

	private Oauth2AccessToken mAccessToken;

	protected SsoHandler mSsoHandler;

	protected AuthInfo mAuthInfo;

	private Context mContext;

	private int mShareType = SHARE_MULTI;

	private List<String> imageUrls = null;

	// private String postUrl = "";

	public SinaWbShare(Context context) {
		mContext = context;
		mWeiboShareAPI = WeiboShareSDK.createWeiboAPI(context,
				ShareConstanse.SINA_APP_KEY);
		mWeiboShareAPI.registerApp();

	}

	public boolean checkShareConditions() {

		boolean isInstalledWeibo = mWeiboShareAPI.isWeiboAppInstalled();
		if (!isInstalledWeibo)
			return isInstalledWeibo;
		int supportApiLevel = mWeiboShareAPI.getWeiboAppSupportAPI();
		if (supportApiLevel == -1)
			return false;
		if (supportApiLevel >= 10351) {

			mShareType = SHARE_MULTI;
		} else {

			mShareType = SHARE_SINGLE;
		}
		return true;
	}

	public synchronized void callWeiBoClient(String text, String imgUrl,
			String postUrl) {
		mAuthInfo = new AuthInfo(mContext, ShareConstanse.SINA_APP_KEY,
				ShareConstanse.SERVER, ShareConstanse.SCOPE);
		mAccessToken = AccessTokenKeeper.readAccessToken(mContext.getApplicationContext());
		mSsoHandler = new SsoHandler((Activity) mContext, mAuthInfo);
		if (mAccessToken.isSessionValid()) {

			reqMsg(text, imgUrl, postUrl);
		} else {

			mSsoHandler.authorizeClientSso(new AuthDialogListener(mContext,
					text, imgUrl, postUrl));
		}
	}

	private void reqMsg(String text, String imgUrl, String postUrl) {
		if (text == null)
			return;
		String content = formatText(text, postUrl);
//		String content=text;
		switch (mShareType) {
		case SHARE_MULTI:

			WeiboMultiMessage weiboMessage = new WeiboMultiMessage();
			weiboMessage.textObject = getTextObject(content);
			// weiboMessage.mediaObject = getWebPageObject(text, content,
			// imgUrl, postUrl);
			ImageObject imgObj = getImageObject(imgUrl);
			if (imgObj != null) {
				weiboMessage.imageObject = imgObj;
			}

			SendMultiMessageToWeiboRequest request = new SendMultiMessageToWeiboRequest();
			request.transaction = String.valueOf(System.currentTimeMillis());
			request.multiMessage = weiboMessage;
			mWeiboShareAPI.sendRequest((Activity) mContext, request);
			break;
		case SHARE_SINGLE:

			WeiboMessage weiboSingleMessage = new WeiboMessage();
			WebpageObject mediaObject = getWebPageObject(text, content, imgUrl,
					postUrl);
			weiboSingleMessage.mediaObject = mediaObject;

			SendMessageToWeiboRequest requestSingle = new SendMessageToWeiboRequest();

			requestSingle.transaction = String.valueOf(System
					.currentTimeMillis());
			requestSingle.message = weiboSingleMessage;

			mWeiboShareAPI.sendRequest((Activity) mContext, requestSingle);
			break;
		default:
			break;

		}

	}

	private String formatText(String text, String url) {
		String content = "";
		if (!TextUtils.isEmpty(text)) {
			String format = mContext.getResources().getString(
					R.string.share_title_format);
			content = String.format(format, text, url);
		}
		return content;
	}

	private TextObject getTextObject(String text) {
		TextObject textObject = null;
		if (!TextUtils.isEmpty(text)) {

			textObject = new TextObject();
			textObject.text = text;
		}
		return textObject;
	}

	private ImageObject getImageObject(String imgUrl) {
		ImageObject imageObject = null;
		Bitmap bitmap = getBitmap(imgUrl);
		if (bitmap != null) {
			imageObject = new ImageObject();
			imageObject.setImageObject(bitmap);
		}
		return imageObject;
	}

	private ImageObject getImageObject() {
		ImageObject imageObject = null;
		Bitmap bitmap = getBitmap();
		if (bitmap != null) {
			imageObject = new ImageObject();
			imageObject.setImageObject(bitmap);
		}
		return imageObject;

	}

	private Bitmap getBitmap() {
		if (imageUrls == null)
			return null;
		int size = imageUrls.size();
		if (size == 0)
			return null;
		Bitmap temp = getBitmap(imageUrls.get(0));
		for (int i = 0; i < size - 1; i++) {
			temp = bitmapsPieceTogether(temp, getBitmap(imageUrls.get(i + 1)));
		}
		return temp;
	}

	private Bitmap getBitmap(String imgUrl) {
        Bitmap bitmap =null;
        try {
            URL url=new URL(imgUrl);
            InputStream in=url.openStream();
            bitmap=BitmapFactory.decodeStream(in);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // if (bitmap == null) {
		// bitmap = BitmapFactory.decodeResource(mContext.getResources(),
		// R.drawable.coolyou_ic_launcher);
		// }
		return bitmap;
	}

	private WebpageObject getWebPageObject(String text, String content,
			String imgUrl, String postUrl) {
		WeiboMultiMessage weiboMessage = new WeiboMultiMessage();
		Bitmap bitmap = getBitmap(imgUrl);

		WebpageObject mediaObject = new WebpageObject();
		mediaObject.identify = Utility.generateGUID();
		mediaObject.title = content;
		mediaObject.description = text;

		Bitmap thumbBmp = Bitmap.createScaledBitmap(bitmap,
				ShareConstanse.THUMB_SIZE, ShareConstanse.THUMB_SIZE, true);
		mediaObject.setThumbImage(thumbBmp);
		mediaObject.actionUrl = postUrl;
		weiboMessage.mediaObject = mediaObject;
		return mediaObject;
	}

	public void setHandleWeiboResponse(Intent intent,
			IWeiboHandler.Response callBack) {
		if (mWeiboShareAPI == null) {
			Log.d(TAG, "mWeiboShareAPI is null!");
			return;
		}
		mWeiboShareAPI.handleWeiboResponse(intent, callBack);
	}

	class AuthDialogListener implements WeiboAuthListener {

		Context mContext;

		String text;
		String imgUrl;
		String postUrl;

		public AuthDialogListener(Context context, String text, String imgUrl,
				String postUrl) {
			mContext = context;
			this.text = text;
			this.imgUrl = imgUrl;
			this.postUrl = postUrl;
		}

		@Override
		public void onComplete(Bundle values) {

			mAccessToken = Oauth2AccessToken.parseAccessToken(values);
			if (mAccessToken.isSessionValid()) {

				try {
					AccessTokenKeeper.writeAccessToken(
							mContext.getApplicationContext(),
							mAccessToken);
					// Toast.makeText(mContext,
					// R.string.weibosdk_demo_toast_auth_success,
					// Toast.LENGTH_SHORT).show();
					reqMsg(text, imgUrl, postUrl);
				} catch (Exception e) {
					e.printStackTrace();
				}

			} else {

				String code = values.getString("code");
				String message ="取消授权";
				if (!TextUtils.isEmpty(code)) {
					message = message + "\nObtained the code: " + code;
				}
				Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
			}
		}

		@Override
		public void onCancel() {
			Toast.makeText(mContext,
					"取消授权",
					Toast.LENGTH_LONG).show();
		}

		@Override
		public void onWeiboException(WeiboException e) {
			Toast.makeText(mContext, "Auth exception : " + e.getMessage(),
					Toast.LENGTH_LONG).show();
		}

	}

	public void setImgUrls(List<String> list) {
		imageUrls = list;
	}

	public SsoHandler getSsoHandler() {
		return mSsoHandler;
	}

	public static Bitmap bitmapsPieceTogether(Bitmap first, Bitmap second) {
		if (first == null && second == null)
			return null;
		if (first == null)
			return second;
		if (second == null)
			return first;

		int width = Math.max(first.getWidth(), second.getWidth());
		int height = first.getHeight() + second.getHeight();
		Bitmap result = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas canvas = new Canvas(result);
		canvas.drawBitmap(first, 0, 0, null);
		canvas.drawBitmap(second, 0, first.getHeight(), null);
		return result;

	}

}
