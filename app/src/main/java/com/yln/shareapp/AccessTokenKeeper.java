package com.yln.shareapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.sina.weibo.sdk.auth.Oauth2AccessToken;

public class AccessTokenKeeper {
	private static final String PREFERENCES_NAME = "com_weibo_sdk_android";

	private static final String KEY_UID = "uid";
	private static final String KEY_ACCESS_TOKEN = "access_token";
	private static final String KEY_EXPIRES_IN = "expires_in";

	public static void writeAccessToken(Context context, Oauth2AccessToken token) {
		if (null == context || null == token) {
			return;
		}

		SharedPreferences pref = context.getSharedPreferences(PREFERENCES_NAME,
				Context.MODE_APPEND);
		Editor editor = pref.edit();
		editor.putString(KEY_UID, token.getUid());
		editor.putString(KEY_ACCESS_TOKEN, token.getToken());
		editor.putLong(KEY_EXPIRES_IN, token.getExpiresTime());
		editor.commit();
	}

	public static Oauth2AccessToken readAccessToken(Context context) {
		if (null == context) {
			return null;
		}
		Oauth2AccessToken token = new Oauth2AccessToken();
		try {
			SharedPreferences pref = context.getSharedPreferences(
					PREFERENCES_NAME, Context.MODE_APPEND);
			token.setUid(pref.getString(KEY_UID, ""));
			token.setToken(pref.getString(KEY_ACCESS_TOKEN, ""));
			token.setExpiresTime(pref.getLong(KEY_EXPIRES_IN, 0));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return token;
	}

	public static void clear(Context context) {
		if (null == context) {
			return;
		}

		SharedPreferences pref = context.getSharedPreferences(PREFERENCES_NAME,
				Context.MODE_APPEND);
		Editor editor = pref.edit();
		editor.clear();
		editor.commit();
	}
}
