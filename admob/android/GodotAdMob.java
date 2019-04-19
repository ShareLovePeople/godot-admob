package org.godotengine.godot;

import com.google.android.gms.ads.*;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.app.Activity;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.view.ViewGroup.LayoutParams;
import android.provider.Settings;
import android.graphics.Color;
import android.util.Log;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;

import android.view.Gravity;
import android.view.View;
import android.os.Bundle;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;

public class GodotAdMob extends Godot.SingletonBase {
    private static String TAG = "godot_adinfo";
    private Activity activity = null; // The main activity of the game
    private int instance_id = 0;

    private AdView adView = null; // Banner view

    private boolean isReal = false; // Store if is real or not
    private boolean isForChildDirectedTreatment = false; // Store if is children directed treatment desired
    private Bundle extras = null;


    private FrameLayout layout = null; // Store the layout
    private FrameLayout.LayoutParams adParams = null; // Store the layout params


    private Map<String, AdView> bannerMap = new HashMap<>();
    private Map<String, InterstitialAd> interAdMap = new HashMap<>();
    private Map<String, RewardedVideoAd> rewardedVideoAdMap = new HashMap<>();

    /* Init
     * ********************************************************************** */

    /**
     * Prepare for work with AdMob
     *
     * @param admobAppId  admob应用ID
     * @param isReal      是否真实环境 true 真实 false测试
     * @param instance_id 由Godot传入
     */
    public void init(String admobAppId, boolean isReal, int instance_id) {
        this.initWithContentRating(admobAppId, isReal, instance_id, false, "");
    }

    /**
     * Init with content rating additional options
     *
     * @param admobAppId
     * @param isReal
     * @param instance_id
     * @param isForChildDirectedTreatment
     * @param maxAdContentRating
     */
    public void initWithContentRating(String admobAppId, boolean isReal, int instance_id, boolean isForChildDirectedTreatment, String maxAdContentRating) {
        this.isReal = isReal;
        this.instance_id = instance_id;
        this.isForChildDirectedTreatment = isForChildDirectedTreatment;
        if (maxAdContentRating != null && maxAdContentRating != "") {
            extras = new Bundle();
            extras.putString("max_ad_content_rating", maxAdContentRating);
        }
        MobileAds.initialize(activity, admobAppId);
        Logi(TAG, " init to app_id:" + admobAppId + "");
    }


    /**
     * Returns AdRequest object constructed considering the parameters set in constructor of this class.
     *
     * @return AdRequest object
     */
    private AdRequest getAdRequest() {
        AdRequest.Builder adBuilder = new AdRequest.Builder();
        AdRequest adRequest;
        if (!this.isForChildDirectedTreatment && extras != null) {
            adBuilder.addNetworkExtrasBundle(AdMobAdapter.class, extras);
        }
        if (this.isForChildDirectedTreatment) {
            adBuilder.tagForChildDirectedTreatment(true);
        }
        if (!isReal) {
            adBuilder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
            adBuilder.addTestDevice(getAdmobDeviceId());
        }
        adRequest = adBuilder.build();
        return adRequest;
    }

    /* Rewarded Video
     * ********************************************************************** */


    private void idInitRewardedVideo(final String id) {
        RewardedVideoAd rewardedVideoAd = MobileAds.getRewardedVideoAdInstance(activity);
        rewardedVideoAd.setRewardedVideoAdListener(new RewardedVideoAdListener() {
            @Override
            public void onRewardedVideoAdLeftApplication() {

                Toast("onRewardedVideoAdLeftApplication");

                Log.w("godot", " onRewardedVideoAdLeftApplication");

            }

            @Override
            public void onRewardedVideoAdClosed() {

                Toast("onRewardedVideoAdClosed");

                Log.w("godot", " onRewardedVideoAdClosed");


                rewardedVideoAdMap.get(id).loadAd(id, getAdRequest());
            }

            @Override
            public void onRewardedVideoAdFailedToLoad(int errorCode) {
                Logw(TAG, " onRewardedVideoAdFailedToLoad. errorCode: " + errorCode + "  id:" + id);

            }

            @Override
            public void onRewardedVideoAdLoaded() {
                Logi("godot", " onRewardedVideoAdLoaded id:" + id);
                Toast(" onRewardedVideoAdLoaded id:" + id);
                idUpAdloadStatus(id, true);
            }

            @Override
            public void onRewardedVideoAdOpened() {

                Toast("onRewardedVideoAdOpened");
                Log.w("godot", " onRewardedVideoAdOpened");

            }

            @Override
            public void onRewarded(RewardItem reward) {

                Logd(TAG, " " + String.format(" onRewarded! currency: %s amount: %d", reward.getType(),
                        reward.getAmount()));
                Toast(String.format("onRewarded! currency: %s amount: %d", reward.getType(),
                        reward.getAmount()));
                idReutrnShowAdResult(0, id, reward.getType(), reward.getAmount());

                if (rewardedVideoAdMap.get(id) != null) {
                    rewardedVideoAdMap.get(id).loadAd(id, getAdRequest());
                }

            }

            @Override
            public void onRewardedVideoStarted() {

                Toast("onRewardedVideoStarted");

                Log.w("godot", " onRewardedVideoStarted");

            }

            //@Override
            public void onRewardedVideoCompleted() {

                Log.w("godot", " onRewardedVideoCompleted");

            }
        });
        rewardedVideoAdMap.put(id, rewardedVideoAd);
        Logi(TAG, " init RewardedVideo to id:" + id);
    }

    public void idLoadRewardedVideo(final String id) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (rewardedVideoAdMap.get(id) == null) {
                    idInitRewardedVideo(id);
                }

                if (!rewardedVideoAdMap.get(id).isLoaded()) {
                    rewardedVideoAdMap.get(id).loadAd(id, getAdRequest());
                    Logi(TAG, " load RewardedVideo to id:" + id);
                }
            }
        });

    }

    public void idShowRewardedVideo(final String id) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (rewardedVideoAdMap.get(id) != null) {
                    if (rewardedVideoAdMap.get(id).isLoaded()) {
                        rewardedVideoAdMap.get(id).show();
                        idUpAdloadStatus(id, false);
                        Logi(TAG, " show RewardedVideo to id:" + id);
                    } else {
                        Toast("showRewardedVideo - rewardedVideo not loaded");
                        Log.w("w", " idShowRewardedVideo - rewardedVideo not loaded");
                    }
                } else {
                    Toast("showRewardedVideo - don't init Object");
                    Log.w("w", " idShowRewardedVideo - don't init Object");
                }
            }
        });
    }


    /* Banner
     * ********************************************************************** */
    private void idInitBanner(final String id, boolean isOnTop, int sizeType) {
        if (bannerMap.get(id) != null) {
            return;
        }
        layout = ((Godot) activity).layout;
        FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );

        if (isOnTop) {
            adParams.gravity = Gravity.TOP;
        } else {
            adParams.gravity = Gravity.BOTTOM;
        }

        AdView adView = new AdView(activity);
        adView.setAdUnitId(id);

        adView.setBackgroundColor(Color.TRANSPARENT);

        //                调整横幅大小dp为单位
        switch (sizeType) {
            case 0:
                adView.setAdSize(AdSize.SMART_BANNER);//智能横幅屏幕宽度 *32|50|90
                break;
            case 1:
                adView.setAdSize(AdSize.BANNER);
                break;
            case 2:
                adView.setAdSize(AdSize.MEDIUM_RECTANGLE);
                break;
            case 3:
                adView.setAdSize(AdSize.FULL_BANNER);
                break;
            case 4:
                adView.setAdSize(AdSize.LEADERBOARD);
                break;
            case 5:
                adView.setAdSize(AdSize.WIDE_SKYSCRAPER);
                break;
            case 6:
                adView.setAdSize(AdSize.FLUID);
                break;
            case 7:
                adView.setAdSize(AdSize.SMART_BANNER);
                break;
            case 8:
                adView.setAdSize(AdSize.SEARCH);
                break;
            case 9:
                adView.setAdSize(AdSize.LARGE_BANNER);
                break;
            default:
                adView.setAdSize(AdSize.SMART_BANNER);
        }
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Logi(TAG, " onAdLoaded Banner id:" + id);

                idUpAdloadStatus(id, true);
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                String str;
                switch (errorCode) {
                    case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                        str = "ERROR_CODE_INTERNAL_ERROR";
                        break;
                    case AdRequest.ERROR_CODE_INVALID_REQUEST:
                        str = "ERROR_CODE_INVALID_REQUEST";
                        break;
                    case AdRequest.ERROR_CODE_NETWORK_ERROR:
                        str = "ERROR_CODE_NETWORK_ERROR";
                        break;
                    case AdRequest.ERROR_CODE_NO_FILL:
                        str = "ERROR_CODE_NO_FILL";
                        break;
                    default:
                        str = "Code: " + errorCode;
                        break;
                }
                Logw(TAG, " fail to load  ERROR_CODE" + errorCode + "  id:" + id);
            }

            //此方法会在用户点按广告时调用。
            @Override
            public void onAdOpened() {
                Log.w("godot", " onAdLoaded");

            }

            //此方法会在用户点击打开其他应用（例如，Google Play）时于 onAdOpened() 之后调用，从而在后台运行当前应用。
            @Override
            public void onAdLeftApplication() {
                Log.w("godot", " onAdLoaded");

            }

            //在用户查看广告的目标网址后返回应用时，会调用此方法。应用可以使用此方法恢复暂停的活动，或执行任何其他必要的操作，以做好互动准备。 有关 Android API Demo 应用中广告监听器方法的实现方式，请参阅 AdMob AdListener 示例。
            @Override
            public void onAdClosed() {
                Log.w("godot", " onAdLoaded");

            }
        });
        layout.addView(adView, adParams);

        adView.loadAd(getAdRequest());

        adView.setVisibility(View.GONE);
        adView.pause();
        bannerMap.put(id, adView);
        Logi(TAG, " init Banner to id:" + id);
    }

    public void idLoadBanner(final String id, final boolean isOnTop, final int sizeType) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (bannerMap.get(id) == null) {
                    idInitBanner(id, isOnTop, sizeType);
                    Logi(TAG, " load Banner to id:" + id + " isTop:" + isOnTop + "  sizeType:" + sizeType);
                }
            }
        });

    }


    public void idShowBanner(final String id) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (bannerMap.get(id) == null) {
                    Logw(TAG, " showBanner - don't init Object  id:" + id);
                    return;
                }
                if (bannerMap.get(id).getVisibility() == View.VISIBLE)
                    return;

                bannerMap.get(id).setVisibility(View.VISIBLE);
                bannerMap.get(id).resume();
                Logi(TAG, " show Banner to id:" + id);
            }
        });
    }


    public void idHideBanner(final String id) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (bannerMap.get(id) != null) {
                    if (bannerMap.get(id).getVisibility() == View.GONE)
                        return;

                    bannerMap.get(id).setVisibility(View.GONE);
                    bannerMap.get(id).pause();
                    Logi(TAG, " show Banner to id:" + id);
                } else {
                    Logw(TAG, " hideBanner - don't init Object" + id);
                }
            }
        });
    }


    /**
     * Get the banner width
     *
     * @return int Banner width
     */
    public int getBannerWidth() {
        return AdSize.SMART_BANNER.getWidthInPixels(activity);
    }

    /**
     * Get the banner height
     *
     * @return int Banner height
     */
    public int getBannerHeight() {
        return AdSize.SMART_BANNER.getHeightInPixels(activity);
    }

    /**
     * Resize the banner 根据类别修改Banner尺寸
     */
    public void resize(final int type) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (layout == null || adView == null || adParams == null) {
                    return;
                }

                layout.removeView(adView); // Remove the old view

                // Extract params

                int gravity = adParams.gravity;
                FrameLayout layout = ((Godot) activity).layout;
                adParams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                );
                adParams.gravity = gravity;
                AdListener adListener = adView.getAdListener();
                String id = adView.getAdUnitId();

                // Create new view & set old params
                adView = new AdView(activity);
                adView.setAdUnitId(id);
                adView.setBackgroundColor(Color.TRANSPARENT);
//                调整横幅大小dp为单位
                switch (type) {
                    case 0:
                        adView.setAdSize(AdSize.SMART_BANNER);//智能横幅屏幕宽度 *32|50|90
                        break;
                    case 1:
                        adView.setAdSize(AdSize.BANNER);
                        break;
                    case 2:
                        adView.setAdSize(AdSize.MEDIUM_RECTANGLE);
                        break;
                    case 3:
                        adView.setAdSize(AdSize.FULL_BANNER);
                        break;
                    case 4:
                        adView.setAdSize(AdSize.LEADERBOARD);
                        break;
                    case 5:
                        adView.setAdSize(AdSize.WIDE_SKYSCRAPER);
                        break;
                    case 6:
                        adView.setAdSize(AdSize.FLUID);
                        break;
                    case 7:
                        adView.setAdSize(AdSize.SMART_BANNER);
                        break;
                    case 8:
                        adView.setAdSize(AdSize.SEARCH);
                        break;
                    case 9:
                        adView.setAdSize(AdSize.LARGE_BANNER);
                        break;
                    default:
                        adView.setAdSize(AdSize.SMART_BANNER);
                }

                adView.setAdListener(adListener);

                // Add to layout and load ad
                layout.addView(adView, adParams);

                // Request
                adView.loadAd(getAdRequest());

                Log.d("godot", " Banner Resized");
            }
        });
    }

    /*自定义Banner尺寸*/
    public void myResize(final int width, final int height) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (layout == null || adView == null || adParams == null) {
                    return;
                }

                layout.removeView(adView); // Remove the old view

                // Extract params

                int gravity = adParams.gravity;
                FrameLayout layout = ((Godot) activity).layout;
                adParams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                );
                adParams.gravity = gravity;
                AdListener adListener = adView.getAdListener();
                String id = adView.getAdUnitId();

                // Create new view & set old params
                adView = new AdView(activity);
                adView.setAdUnitId(id);
                adView.setBackgroundColor(Color.TRANSPARENT);
                AdSize adSize = new AdSize(width, height);
                adView.setAdSize(adSize);
                adView.setAdListener(adListener);

                // Add to layout and load ad
                layout.addView(adView, adParams);

                // Request
                adView.loadAd(getAdRequest());

                Log.d("godot", " Banner Resized");
            }
        });
    }

    /* Interstitial
     * ********************************************************************** */


    private void idInitInterstitial(final String id) {
        InterstitialAd interstitialAd = new InterstitialAd(activity);
        interstitialAd.setAdUnitId(id);
        interstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Logi(TAG, " onAdLoaded Interstitial id:" + id);
                idUpAdloadStatus(id, true);

            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                Logw(TAG, " FailedToLoad Interstitial error code: " + Integer.toString(errorCode) + "  id:" + id);

            }

            @Override
            public void onAdOpened() {
                Log.w("godot", " onAdOpened()");

                idReutrnShowAdResult(0, id, "Interstitial", 0);
            }

            @Override
            public void onAdLeftApplication() {
                Log.w("godot", " onAdLeftApplication()");

            }

            @Override
            public void onAdClosed() {

                Log.w("godot", " onAdClosed");


                interAdMap.get(id).loadAd(getAdRequest());
            }
        });

        interAdMap.put(id, interstitialAd);
        Logi(TAG, " init Interstitial to id:" + id);
    }

    public void idLoadInterstitial(final String id) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (interAdMap.get(id) == null) {
                    idInitInterstitial(id);
                }

                if (!interAdMap.get(id).isLoaded()) {
                    interAdMap.get(id).loadAd(getAdRequest());
                    Logi(TAG, " load Interstitial to id:" + id);
                    Toast(" load Interstitial to id:" + id);
                }
            }
        });
    }

    public void idShowInterstitial(final String id) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (interAdMap.get(id) != null) {
                    if (interAdMap.get(id).isLoaded()) {
                        interAdMap.get(id).show();
                        idUpAdloadStatus(id, false);
                    } else {
                        Toast("idShowInterstitial - interstitial not loaded id:" + id);
                        Log.w("w", " idShowInterstitial - interstitial not loaded");
                    }
                } else {
                    Log.w("w", " idShowInterstitial - don't init Object");
                    Toast("idShowInterstitial - don't init Object id:" + id);
                }
            }
        });
    }

    /*********************************************************************************************/
    /*更新各个广告位置的加载情况 ture广告已经预加载随时可以调用 false(为加载)*/

    /**
     * @param id     广告位ID
     * @param status 加载状态
     */
    private void idUpAdloadStatus(final String id, boolean status) {
        GodotLib.calldeferred(instance_id, "_id_up_adload_status", new Object[]{id, status});
        Logd(TAG, "更新广告加载情况列表  id:" + id + " bool:" + status);
    }

    /**
     * 返回广告展示结果
     *
     * @param code   广告展示结果代码 0成功 1失败
     * @param id     广告位ID
     * @param reward 奖励
     */
    private void idReutrnShowAdResult(final int code, final String id, final String reward, final int number) {
        GodotLib.calldeferred(instance_id, "_id_showad_result", new Object[]{code, id, reward, number});
    }
    /* Utils
     * ********************************************************************** */

    /**
     * Generate MD5 for the deviceID
     *
     * @param s
     * @return String The MD5 generated
     */
    private String md5(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2) h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            //Logger.logStackTrace(TAG,e);
        }
        return "";
    }

    /**
     * Get the Device ID for AdMob
     *
     * @return String Device ID
     */
    private String getAdmobDeviceId() {
        String android_id = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);
        String deviceId = md5(android_id).toUpperCase(Locale.US);
        return deviceId;
    }

    /*Debug
     * *************************************************************/


    private void Logi(final String TAG, final String log) {
        if (!isReal) {
            Log.i(TAG, log);
        }
    }

    private void Logw(final String TAG, final String log) {
        if (!isReal) {
            Log.w(TAG, log);
        }
    }

    private void Logd(final String TAG, final String log) {
        if (!isReal) {
            Log.d(TAG, log);
        }
    }

    private void Toast(String str) {
        if (!isReal) {
            Toast.makeText(activity, str, Toast.LENGTH_SHORT).show();
        }
    }

    /* Definitions
     * ********************************************************************** */

    /**
     * Initilization Singleton
     *
     * @param activity
     * @return
     */
    static public Godot.SingletonBase initialize(Activity activity) {
        return new GodotAdMob(activity);
    }

    /**
     * Constructor
     *
     * @param p_activity
     */
    public GodotAdMob(Activity p_activity) {
        registerClass("AdMob", new String[]{
                "init",
                "initWithContentRating",
                // banner
                "idLoadBanner", "idShowBanner", "idHideBanner", "getBannerWidth", "getBannerHeight", "resize", "myResize",
                // Interstitial
                "idLoadInterstitial", "idShowInterstitial",
                // Rewarded video
                "idLoadRewardedVideo", "idShowRewardedVideo"
        });
        activity = p_activity;
    }
}
