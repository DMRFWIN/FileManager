package com.example.dmrf.filemanager;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;


/**
 * Created by DMRF on 2017/7/13.
 */

public class FileService extends Service {
    private Looper mLooper;
    private FileHandler mFileHandler;
    private ArrayList<String> mFileName = null;
    private ArrayList<String> mFilePaths = null;
    public static final String FILE_SEARCH_COMPLETED = "com.example.dmrf.filemanager.FILE_SEARCH_COMPLETED";
    public static final String FILE_NOTIFICATION = "com.example.dmrf.filemanager.FILE_NOTIFICATION";

    //创建服务
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("FileService", "FileService is onCreate");
        //新建处理线程
        HandlerThread mHT = new HandlerThread("FileService", HandlerThread.NORM_PRIORITY);
        mHT.start();
        mLooper = mHT.getLooper();
        mFileHandler = new FileHandler(mLooper);
    }

    //服务开始
    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.d("FileService", "FileService is onStart");
        mFileName = new ArrayList<String>();
        mFilePaths = new ArrayList<String>();
        mFileHandler.sendEmptyMessage(0);
        //发出通知表明正在搜索
        fileSearchNotification();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        //取消通知
        mNF.cancel(R.string.app_name);
    }


    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    private class FileHandler extends android.os.Handler {
        public FileHandler(Looper mLooper) {
            super(mLooper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d("FileService", "FileService is handleMessage");
            //在指定范围搜索
            initFileArray(new File(SearchBroadCast.mServiceSerchPath));
            //当用户单击了取消搜索则不发送广播，如果搜索完毕且用户没有取消，则发送包含搜索的结果的广播
            if (!MainActivity.isComeBackFromNotification) {
                Intent intent = new Intent(FILE_SEARCH_COMPLETED);
                intent.putStringArrayListExtra("mFileNameList", mFileName);
                intent.putStringArrayListExtra("mFilePathsList", mFilePaths);
                //搜索完毕后携带数据并发送广播
                sendBroadcast(intent);
            }
        }


    }

    private int m = -1;

    /**
     * 具体做搜索事件的可回调函数
     **/
    private void initFileArray(File file) {
        Log.d("FileService", "currentArray is" + file.getPath());
        //只能遍历可读的文件夹，否则会报错
        if (file.canRead()) {
            File[] mFileArray = file.listFiles();
            for (File currentFile : mFileArray) {
                if (currentFile.getName().contains(SearchBroadCast.mServiceKeyWord)) {
                    if (m == -1) {
                        m++;
                        //返回搜索之前的目录
                        mFileName.add("BacktoSearchBefore");
                        mFilePaths.add(MainActivity.mCurrentFilePath);
                    }
                    mFileName.add(currentFile.getName());
                    mFilePaths.add(currentFile.getPath());
                }
                //如果是文件夹则回调该方法
                if (currentFile.exists() && currentFile.isDirectory()) {
                    //先检查用户有没有取消搜索
                    if (MainActivity.isComeBackFromNotification) {
                        return;
                    }
                    initFileArray(currentFile);
                }
            }
        }
    }


    NotificationManager mNF;

    /**
     * 通知
     **/
    private void fileSearchNotification() {

        int notification_id = 1;//通知的ID号
        Notification notification = null;

        mNF = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);//通过getSystemService（）来获取Notification实例

        Intent intent = new Intent(this, MainActivity.class);
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.mipmap.logo);//设置图标
        builder.setContentText("在" + SearchBroadCast.mServiceSerchPath + "下搜索," + "搜索关键字为" + SearchBroadCast.mServiceKeyWord + "点击可取消搜索");
        builder.setWhen(System.currentTimeMillis()); // 设置时间
        builder.setContentTitle("通知栏通知");//设置标题

        //点击通知之后想要跳转到某个页面，需要PendingIntent和Intent(意图)来实现
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);//请求码和flag都设置为0
        builder.setContentIntent(pendingIntent);//点击后的意图
        builder.setDefaults(Notification.DEFAULT_ALL);//设置所有的提示效果，包括声音、震动、指示灯
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            notification = builder.build();//4.1及以上版本
            //  Notification notification1=builder.getNotification();//4.1以下版本
        }
        Intent intent2 = new Intent(FILE_NOTIFICATION);
        PendingIntent mPI = PendingIntent.getBroadcast(this, 0, intent2, 0);
        if (mNF == null) {
            mNF = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
        mNF.notify(R.string.app_name, notification);
    }

}
