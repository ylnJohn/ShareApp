package com.yln.shareapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.sina.weibo.sdk.api.share.BaseResponse;
import com.sina.weibo.sdk.api.share.IWeiboHandler;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.constant.WBConstants;
import com.tencent.connect.share.QQShare;
import com.tencent.connect.share.QzoneShare;
import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;
import com.tencent.mm.sdk.modelmsg.WXWebpageObject;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements IWeiboHandler.Response,ShareConstanse{

    private List<AppInfo> shareAppInfos;
    protected SinaWbShare wbShare;
    private Tencent mTencent = null;
    private Context mContext;
    private RadioGroup mShareGroup;
    private Button mSubmitBtn;
    private EditText mContentEdit;
    private List<AppInfo> customizeShareList;
    private static String[] appPkgNameArray = { WEIXIN_PKGNAME, WEIXIN_PKGNAME,
            WEIBO_PKGNAME, QQ_PKGNEMA, QQZONE_PKGNEMA };
    private static String[] appLauncherClassNameArray = { WEIXIN,
            WEIXIN_FRIEND, WEIBO, TENCENT_QQ, TENCENT_QQZONE };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext=MainActivity.this;
        mShareGroup= (RadioGroup) findViewById(R.id.share_group);
        mSubmitBtn= (Button) findViewById(R.id.submit_btn);
        mContentEdit= (EditText) findViewById(R.id.share_content);
        mTencent = Tencent.createInstance(ShareConstanse.QQ_APP_KEY,
                getApplicationContext());
        wbShare = new SinaWbShare(mContext);
        wbShare.setHandleWeiboResponse(getIntent(),this);
        getShareAppList(mContext);
        initCustomizeShareList(mContext);
        if(customizeShareList!=null&&customizeShareList.size()>0){
            int index=0;
            for(AppInfo app:customizeShareList){
                RadioButton button=new RadioButton(mContext);
                button.setText(app.getAppName());
                Drawable drawable=app.getAppIcon();
                drawable.setBounds(0,0,drawable.getIntrinsicWidth(),drawable.getIntrinsicHeight());
                button.setCompoundDrawables(null,null,drawable,null);
                button.setId(index);
                mShareGroup.addView(button);
                index++;
            }
        }
        mSubmitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position=mShareGroup.getCheckedRadioButtonId();
                if(position<0)
                    return;
                AppInfo appInfo = (AppInfo) customizeShareList.get(position);
                String notInstallApp = mContext.getResources().getString(
                        R.string.app_uninstall);
                String title=mContentEdit.getText().toString().trim();
                if(TextUtils.isEmpty(title))
                    return;
                String url="http://www.baidu.com";
                String image="https://ss0.bdstatic.com/94oJfD_bAAcT8t7mm9GUKT-xh_/timg?image&quality=100&size=b4000_4000&sec=1505983686&di=0df9cf78e08f1d0e3a4c274f00b0fb3e&src=http://www.th7.cn/d/file/p/2016/07/15/6659b589b0bd379a6acbdcbcf5a1a8a1.jpg";
                switch (position) {
                    case 0:
                    case 1://微信
                        if (checkApp(mContext, position)) {
                            Bitmap bitmap = BitmapFactory.decodeResource(
                                        mContext.getResources(),
                                        R.mipmap.ic_launcher);
                            Bitmap thumbBmp = Bitmap.createScaledBitmap(bitmap,
                                    THUMB_SIZE, THUMB_SIZE, true);

                            WXWebpageObject webpage = new WXWebpageObject();
                            webpage.webpageUrl = url;
                            WXMediaMessage msg = new WXMediaMessage(webpage);
                            msg.title = title;
                            msg.thumbData = WeiXinUtil.bmpToByteArrayNew(thumbBmp,
                                    true);

                            SendMessageToWX.Req req = new SendMessageToWX.Req();
                            req.transaction = buildTransaction("webpage");
                            req.message = msg;
                            if (position == 0) {
                                req.scene = SendMessageToWX.Req.WXSceneSession;
                            }
                            if (position == 1) {
                                req.scene = SendMessageToWX.Req.WXSceneTimeline;
                            }
                            IWXAPI api = WXAPIFactory.createWXAPI(mContext,
                                    ShareConstanse.WEIXIN_APP_KEY, true);
                            api.registerApp(ShareConstanse.WEIXIN_APP_KEY);
                            api.sendReq(req);
                        } else {
                            Toast.makeText(mContext, appInfo.getAppName()
                                    + notInstallApp,Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 2://sina微博
                        boolean conditionIsValid = wbShare.checkShareConditions();
                        if (conditionIsValid) {
                            wbShare.callWeiBoClient(title, image, url);
                        } else {
                            Toast.makeText(mContext, appInfo.getAppName()
                                    + notInstallApp,Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 3://QQ
                        if (checkApp(mContext, position)) {
                            Bundle aaparams = new Bundle();
                            aaparams.putString(QQShare.SHARE_TO_QQ_TITLE,
                                    getResources().getString(R.string.app_name));
                            aaparams.putString(QQShare.SHARE_TO_QQ_SUMMARY, title);
                            aaparams.putString(QQShare.SHARE_TO_QQ_TARGET_URL, url);
                            if (!TextUtils.isEmpty(image)) {
                                aaparams.putString(QQShare.SHARE_TO_QQ_IMAGE_URL,
                                        image);
                            }
                            aaparams.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE,
                                    QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
                            doShareToQQ(mContext, aaparams);
                        } else {
                            Toast.makeText(mContext, appInfo.getAppName()
                                    + notInstallApp,Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 4://QQ空间
                        if (checkApp(mContext, position)) {
                            Bundle params = new Bundle();
                            params.putString(QzoneShare.SHARE_TO_QQ_TITLE,
                                    getResources().getString(R.string.app_name));
                            params.putString(QzoneShare.SHARE_TO_QQ_SUMMARY, title);
                            params.putString(QzoneShare.SHARE_TO_QQ_TARGET_URL, url);
                             params.putString(QzoneShare.SHARE_TO_QQ_IMAGE_URL, image);
                             ArrayList<String> imgs = new ArrayList<String>();
                             imgs.add(image);
                             params.putStringArrayList(
                             QzoneShare.SHARE_TO_QQ_IMAGE_URL, imgs);
                            params.putInt(QzoneShare.SHARE_TO_QZONE_KEY_TYPE,
                                    QzoneShare.SHARE_TO_QZONE_TYPE_IMAGE_TEXT);
                            doShareToQQZone(mContext, params);
                        } else {
                            Toast.makeText(mContext, appInfo.getAppName()
                                    + notInstallApp,Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
            }
        });
    }

    protected boolean checkApp(Context context, int position) {
        if (position > customizeShareList.size())
            return false;
        String objPkgName = customizeShareList.get(position).getAppPkgName();
        for (AppInfo info : shareAppInfos) {
            if (info.getAppPkgName().equals(objPkgName))
                return true;
        }
        return false;
    }

    private void getShareAppList(Context context) {
        shareAppInfos = new ArrayList<AppInfo>();
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> mApps = new ArrayList<ResolveInfo>();
        Intent intent = new Intent(Intent.ACTION_SEND, null);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setType("text/plain");
        PackageManager pManager = context.getPackageManager();
        mApps = pManager.queryIntentActivities(intent,
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
        if (null != mApps) {
            for (ResolveInfo resolveInfo : mApps) {
                AppInfo appInfo = new AppInfo();
                appInfo.setAppPkgName(resolveInfo.activityInfo.packageName);
                appInfo.setAppLauncherClassName(resolveInfo.activityInfo.name);
                appInfo.setAppName(resolveInfo.loadLabel(packageManager)
                        .toString());
                appInfo.setAppIcon(resolveInfo.loadIcon(packageManager));
                shareAppInfos.add(appInfo);

            }
        }

    }
    private void initCustomizeShareList(Context context) {
        if (customizeShareList == null) {
            customizeShareList = new ArrayList<AppInfo>();
        }
        customizeShareList.clear();
        String[] appNameArray = { "微信", "朋友圈", "新浪微博", "QQ", "QQ空间" };
        Drawable[] appIconArray = {
                context.getResources().getDrawable(
                        R.drawable.icon_share_weixin_normal),
                context.getResources()
                        .getDrawable(R.drawable.icon_share_winxin),
                context.getResources().getDrawable(R.drawable.icon_sina_weibo),
                context.getResources().getDrawable(R.drawable.icon_qq_nomal),
                context.getResources().getDrawable(R.drawable.icon_share_qzone) };
        for (int i = 0; i < 5; i++) {
            AppInfo appInfo = new AppInfo();
            appInfo.setAppPkgName(appPkgNameArray[i]);
            appInfo.setAppLauncherClassName(appLauncherClassNameArray[i]);
            appInfo.setAppName(appNameArray[i]);
            appInfo.setAppIcon(appIconArray[i]);
            customizeShareList.add(appInfo);
        }

    }

    private void doShareToQQ(final Context context, final Bundle params) {
        final Activity activity = (Activity) context;
        new Thread(new Runnable() {
            @Override
            public void run() {
                // QQ分享结果回调
                mTencent.shareToQQ(activity, params, new IUiListener() {

                    @Override
                    public void onError(UiError arg0) {

                    }

                    @Override
                    public void onComplete(Object arg0) {

                    }

                    @Override
                    public void onCancel() {

                    }
                });
            }
        }).start();
    }

    private void doShareToQQZone(final Context context, final Bundle params) {
        final Activity activity = (Activity) context;
        // final Tencent mTencent = Tencent.createInstance(
        // ShareConstanse.QQ_APP_ID, activity.getApplicationContext());
        final QzoneShare share = new QzoneShare(activity, mTencent.getQQToken());
        new Thread(new Runnable() {

            @Override
            public void run() {
                // QQ空间分享结果回调
                share.shareToQzone(activity, params, new IUiListener() {

                    @Override
                    public void onError(UiError arg0) {

                    }

                    @Override
                    public void onComplete(Object arg0) {

                    }

                    @Override
                    public void onCancel() {

                    }
                });
            }
        }).start();
    }

    private String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis())
                : type + System.currentTimeMillis();
    }

    //sina微博分享结果回调
    @Override
    public void onResponse(BaseResponse baseResponse) {

        switch (baseResponse.errCode) {
            case WBConstants.ErrorCode.ERR_OK:
                Toast.makeText(this, R.string.weibosdk_toast_share_success,
                        Toast.LENGTH_LONG).show();
                break;
            case WBConstants.ErrorCode.ERR_CANCEL:
                Toast.makeText(this, R.string.weibosdk_toast_share_failed,
                        Toast.LENGTH_LONG).show();
                break;
            case WBConstants.ErrorCode.ERR_FAIL:
                Toast.makeText(
                        this,
                        getResources().getString(
                                R.string.weibosdk_toast_share_canceled)
                                + "Error Message: " + baseResponse.errMsg,
                        Toast.LENGTH_LONG).show();
                break;
            default:
                Toast.makeText(this, R.string.weibosdk_toast_share_success,
                        Toast.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            mTencent.onActivityResultData(requestCode, resultCode, data, new IUiListener() {
                //微信分享结果回调
                @Override
                public void onError(UiError arg0) {

                }
                @Override
                public void onComplete(Object arg0) {

                }
                @Override
                public void onCancel() {

                }
            });
            SsoHandler ssoHandler = wbShare.getSsoHandler();
            ssoHandler.authorizeCallBack(requestCode, resultCode, data);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (wbShare != null)
            wbShare.setHandleWeiboResponse(intent, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wbShare != null) {
            wbShare = null;
        }
    }
}
