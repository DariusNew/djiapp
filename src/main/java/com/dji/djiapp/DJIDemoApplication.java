package com.dji.djiapp;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.multidex.MultiDex;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.useraccount.UserAccountManager;

public class DJIDemoApplication extends Application {

    private static final String TAG = DJIDemoApplication.class.getName();

    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";

    private DJISDKManager.SDKManagerCallback mDJISDKManagerCallback;
    private BaseProduct.BaseProductListener mDJIBaseProductListener;
    private BaseComponent.ComponentListener mDJIComponentListener;
    private static BaseProduct mProduct;
    public Handler mHandler;

    private Application instance;

    public void setContext(Application application) {
        instance = application;
    }

    @Override
    public Context getApplicationContext() {
        return instance;
    }

    public DJIDemoApplication() {

    }

    public static synchronized BaseProduct getProductInstance() {
        if (null == mProduct) {
            mProduct = DJISDKManager.getInstance().getProduct();
        }
        return mProduct;
    }

    public static synchronized Camera getCameraInstance(){
        if (getProductInstance() == null) return null;

        Camera camera = null;

        if (getProductInstance() instanceof Aircraft) camera = ((Aircraft) getProductInstance()).getCamera();
        else if (getProductInstance() instanceof HandHeld) camera = ((HandHeld) getProductInstance()).getCamera();

        return camera;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(Looper.getMainLooper());

        mDJIComponentListener = new BaseComponent.ComponentListener() {
            @Override
            public void onConnectivityChange(boolean isConnected) {
                notifyStatusChange();
            }
        };

        mDJIBaseProductListener = new BaseProduct.BaseProductListener() {
            @Override
            public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent, BaseComponent newComponent) {
                if (newComponent != null){
                    newComponent.setComponentListener(mDJIComponentListener);
                }
                notifyStatusChange();
            }

            @Override
            public void onConnectivityChange(boolean b) {
                notifyStatusChange();
            }
        };

        mDJISDKManagerCallback = new DJISDKManager.SDKManagerCallback() {
            @Override
            public void onRegister(DJIError djiError) {
                if (djiError == DJISDKError.REGISTRATION_SUCCESS){

                    Handler handler = new Handler (Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Register Success", Toast.LENGTH_LONG).show();
                        }
                    });
                    DJISDKManager.getInstance().startConnectionToProduct();
                }
                else {
                    Handler handler = new Handler (Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Registration Failed", Toast.LENGTH_LONG).show();
                        }
                    });
                } Log.e("TAG", djiError.toString());
            }

            @Override
            public void onProductChange(BaseProduct oldProduct, BaseProduct newProduct) {
                mProduct = newProduct;
                if(mProduct != null) mProduct.setBaseProductListener(mDJIBaseProductListener);
                notifyStatusChange();
            }
            };
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionCheck2 = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (permissionCheck == 0 && permissionCheck2 ==0)) DJISDKManager.getInstance().registerApp(getApplicationContext(), mDJISDKManagerCallback);


//        mDJIComponentListener = new BaseComponent.ComponentListener() {
//            @Override
//            public void onConnectivityChange(boolean b) {
//                notifyStatusChange();
//            }
//        };
        }

        private void loginAccount(){
            UserAccountManager.getInstance().logIntoDJIUserAccount(this, new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                @Override
                public void onSuccess(UserAccountState userAccountState) {
                    Log.e("TAG", "Login Success");
                }

                @Override
                public void onFailure(DJIError djiError) {
                    Log.e("TAG", "Login Error: " + djiError.getDescription());
                }
            });
        }

        protected void attachBaseContext(Context base){
            super.attachBaseContext(base);
            MultiDex.install(this);
        }

        private void notifyStatusChange(){
            mHandler.removeCallbacks(updateRunnable);
            mHandler.postDelayed(updateRunnable, 500);
        }
        private Runnable updateRunnable = new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent (FLAG_CONNECTION_CHANGE);
                getApplicationContext().sendBroadcast(intent);
            }
        };
}

