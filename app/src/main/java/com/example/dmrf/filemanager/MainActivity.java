package com.example.dmrf.filemanager;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.IdRes;
import android.support.constraint.solver.LinearSystem;
import android.support.constraint.solver.widgets.ConstraintAnchor;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MainActivity extends ListActivity implements AdapterView.OnItemLongClickListener {

    //声明成员变量

    //存放显示的文件列表的名称
    private List<String> mFileName = null;

    //存放显示的文件列表的相对路径
    private List<String> mFilePath = null;

    //起始目录"/"
    private String mRootPath = File.separator;

    //sd卡根目录
    private String mSDCard = Environment.getExternalStorageDirectory().toString();

    private String OldPath = "";
    private String NewPath = "";
    private String keyWords;

    //用于显示当前路径
    private TextView mPath;

    //用于放置工具栏
    private GridView mGridViewToolBar;
    private int[] grifview_menu_image = {R.mipmap.menu_phone, R.mipmap.menu_sdcard, R.mipmap.menu_search
            , R.mipmap.menu_create, R.mipmap.menu_palse, R.mipmap.menu_exit};
    private String[] gridview_menu_title = {"手机", "SD卡", "搜索", "创建", "粘贴", "退出"};

    //代表手机或SD卡，1代表手机，2代表SD卡
    private static int menuPosition = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //初始化菜单视图
        initGridViewMenu();

        //初始化菜单监听器
        initMenuListener();

        //为列表绑定长按监听器
        getListView().setOnItemLongClickListener(this);


        mPath = (TextView) findViewById(R.id.mPath);
        //程序一开始的时候加载手机目录下的文件列表
        initFileListInfo(mRootPath);
    }

    /**
     * 注册广播
     **/


    private IntentFilter mFilter;
    private FileBroadCast mFileBroadCast;
    private IntentFilter mIntentFilter;
    private SearchBroadCast mServiceBroadCast;

    protected void onStart() {
        super.onStart();
        mFilter = new IntentFilter();
        mFilter.addAction(FileService.FILE_NOTIFICATION);
        mFilter.addAction(FileService.FILE_SEARCH_COMPLETED);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(KEYWOED_BROADCAST);
        if (mFileBroadCast == null) {
            mFileBroadCast = new FileBroadCast();
        }

        if (mServiceBroadCast == null) {
            mServiceBroadCast = new SearchBroadCast();
        }

        //动态注册广播
        this.registerReceiver(mFileBroadCast, mFilter);
        this.registerReceiver(mServiceBroadCast, mIntentFilter);
    }

    /**
     * 注销广播
     **/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFileName.clear();
        mFilePath.clear();
        this.unregisterReceiver(mFileBroadCast);
        this.unregisterReceiver(mServiceBroadCast);
    }

    public static boolean isComeBackFromNotification = false;
    String mAction;

    /**
     * 内部广播类
     **/

    class FileBroadCast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            mAction = intent.getAction();
            //搜索完毕的广播
            if (FileService.FILE_SEARCH_COMPLETED.equals(mAction)) {
                mFileName = intent.getStringArrayListExtra("mFileNameList");
                mFilePath = intent.getStringArrayListExtra("mFilePathsList");
                Toast.makeText(MainActivity.this, "搜索完毕！", Toast.LENGTH_SHORT).show();
                //这里搜索完毕后应弹出一个对话框提示用户要不要显示搜索结果
                searchCompletedDialog("搜索完毕，是否马上显示结果？");
                //当搜索完成后停止服务，然后在服务中取消通知
                getApplicationContext().stopService(serviceIntent);
            } else if (FileService.FILE_NOTIFICATION.equals(mAction)) {
                //单击通知栏发送过来的广播
                //单击通知回到当前Activity，读取其中信息
                String mNotification = intent.getStringExtra("notification");
                Toast.makeText(MainActivity.this, mNotification, Toast.LENGTH_LONG).show();
                searchCompletedDialog("你确定要取消搜索吗？");
            }
        }
    }

    //搜索完毕后单击通知栏时的提示框
    private void searchCompletedDialog(String s) {
        AlertDialog.Builder searchDialog = new AlertDialog.Builder(MainActivity.this).setTitle("提示").setMessage(s)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        /**
                         * 当弹出框时，要对这个“确定”按钮进行一个判断，因为要对不同的情况作不同的处理（2种情况）：
                         * 1：搜索完毕
                         * 2：取消搜索
                         */
                        if (FileService.FILE_SEARCH_COMPLETED.equals(mAction)) {
                            findViewById(R.layout.search_layout).setVisibility(View.GONE);
                            if (mFileName.size() == 0) {
                                Toast.makeText(MainActivity.this, "无相关文件/文件夹！", Toast.LENGTH_SHORT).show();
                                setListAdapter(new FileAdapter(MainActivity.this, mFileName, mFilePath));//清空列表
                            } else {
                                //显示文件列表
                                setListAdapter(new FileAdapter(MainActivity.this, mFileName, mFilePath));
                            }
                        } else {
                            //设置搜索标识为true
                            isComeBackFromNotification = true;
                            //关闭服务，取消搜索
                            getApplicationContext().stopService(serviceIntent);
                        }
                    }
                }).setNegativeButton("取消", null);
        searchDialog.create();
        searchDialog.show();
    }

    //菜单项的监听
    private void initMenuListener() {
        mGridViewToolBar.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                switch (arg2) {
                    //回到手机根目录
                    case 0:
                        menuPosition = 1;
                        initFileListInfo(mRootPath);
                        break;

                    //回到SD卡根目录
                    case 1:
                        menuPosition = 2;
                        initFileListInfo(mSDCard);
                        break;

                    //显示搜索对话框
                    case 2:
                        searchDialog();
                        break;
                    //创建文件夹
                    case 3:
                        createFolder();
                        break;
                    //粘贴文件
                    case 4:
                        try {
                            palseFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    //退出
                    case 5:
                        MainActivity.this.finish();
                        break;

                }
            }
        });
    }

    //用静态变量存储当前目录路径信息
    public static String mCurrentFilePath = "";

    /**
     * 根据给定的一个文件夹路径字符串遍历出这个文件夹中包含的文件名称并配置到ListView列表中
     *
     * @param filePath
     */
    private void initFileListInfo(String filePath) {
        isAddBackUp = false;
        mCurrentFilePath = filePath;
        //显示当前路径
        mPath.setText(mCurrentFilePath);
        mFileName = new ArrayList<String>();
        mFilePath = new ArrayList<String>();
        File mFile = new File(filePath);
        //遍历出该文件夹目录下的所有文件/文件夹
        File[] mFiles = mFile.listFiles();
        //只要当前目录不是手机根目录或者SD卡根目录，则显示“返回上一级”和“返回根目录”
        if (menuPosition == 1 && !mCurrentFilePath.equals(mRootPath)) {
            initAddBackUp(filePath, mRootPath);
        } else if (menuPosition == 2 && mCurrentFilePath.equals(mSDCard)) {
            initAddBackUp(filePath, mSDCard);
        }

        //将所有文件信息添加到集合中
        for (File mCurrentFile : mFiles) {
            mFileName.add(mCurrentFile.getName());
            mFilePath.add(mCurrentFile.getPath());
        }

        //适配数据
        setListAdapter(new FileAdapter(MainActivity.this, mFileName, mFilePath));
    }


    //标识是否添加了“返回上一层/根目录”
    private boolean isAddBackUp = false;

    private void initAddBackUp(String filePath, String phone_sdcard) {
        if (!filePath.equals(phone_sdcard)) {
            /* 列表项的第一项为返回根目录*/
            mFileName.add("BackToRoot");
            mFilePath.add(phone_sdcard);
            /*列表项的第二项为返回上一层*/
            mFileName.add("BackToUp");
            mFilePath.add(new File(filePath).getParent());
            //将添加返回按键标识为置为true
            isAddBackUp = true;
        }
    }


    private void palseFile() throws IOException {
        NewPath = mCurrentFilePath + File.separator + mCopyFaleName;//获得新文件路径
        Log.d("copy", "mOldFilePath is " + OldPath + "| mNewFilePath is " + NewPath + "| isCopy is " + isCopy);
        if (!OldPath.equals(NewPath) && isCopy == true) {
            if (!(new File(NewPath)).exists()) {
                copyFile(OldPath, NewPath);
                initFileListInfo(mCurrentFilePath);
            } else {
                new AlertDialog.Builder(MainActivity.this).setTitle("提示！").setMessage("该文件名已存在，是否覆盖？").setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            copyFile(OldPath, NewPath);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        initFileListInfo(mCurrentFilePath);
                    }
                }).setNegativeButton("否", null);
            }
        } else {
            Toast.makeText(MainActivity.this, "未复制文件！", Toast.LENGTH_SHORT).show();
        }

    }

    private int i;
    FileInputStream fis;
    FileOutputStream fos;

    //复制文件
    private void copyFile(String oldFile, String newFile) throws IOException {
        fis = new FileInputStream(oldFile);
        fos = new FileOutputStream(newFile);

        //逐个byte复制文件
        while ((i = fis.read()) != -1) {
            fos.write(i);
        }

        if (fis != null) {
            fis.close();
        }
        if (fos != null) {
            fos.close();
        }

    }

    //新文件/文件夹的名字
    private String mNewFolderName = "";
    //新建的文件/文件夹
    private File mCreateFile;
    private RadioGroup mCreateRadioGroup;
    private static int mChecked;

    private void createFolder() {
        //用于标识当前选中的时新建文件还是新建文件夹
        mChecked = 2;

        LayoutInflater mLI = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //初始化对话框布局
        final LinearLayout mLL = (LinearLayout) mLI.inflate(R.layout.creat_idalog, null);

        mCreateRadioGroup = mLL.findViewById(R.id.radiogroup_creat);
        final RadioButton mCreateFolderButton = mLL.findViewById(R.id.create_folder);
        final RadioButton mCreateFileButton = mLL.findViewById(R.id.create_file);

        //设置默认为创建文件夹
        mCreateFolderButton.setChecked(true);

        //为按钮设置监听器
        mCreateRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            //当选择改变时触发
            @Override
            public void onCheckedChanged(RadioGroup arg0, @IdRes int arg1) {
                if (arg1 == mCreateFileButton.getId()) {
                    mChecked = 1;
                } else if (arg1 == mCreateFolderButton.getId()) {
                    mChecked = 2;
                }
            }
        });

        //显示对话框
        AlertDialog.Builder mBilder = new AlertDialog.Builder(MainActivity.this).setTitle("新建").setView(mLL).setPositiveButton("创建", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //获得用户输入的名称
                mNewFolderName = ((EditText) mLL.findViewById(R.id.new_filename)).getText().toString();
                if (mChecked == 1) {
                    mCreateFile = new File(mCurrentFilePath + File.separator + mNewFolderName + ".txt");
                    try {
                        mCreateFile.createNewFile();
                    } catch (IOException e) {
                        Toast.makeText(MainActivity.this, "文件名拼写出错", Toast.LENGTH_SHORT).show();
                    }
                    initFileListInfo(mCurrentFilePath);
                } else if (mChecked == 2) {
                    mCreateFile = new File(mCurrentFilePath + File.separator + mNewFolderName);
                    if (!mCreateFile.exists() && !mCreateFile.isDirectory() && mNewFolderName.length() != 0) {
                        if (mCreateFile.mkdirs()) {
                            //刷新当前文件列表
                            initFileListInfo(mCurrentFilePath);
                        } else {
                            Toast.makeText(MainActivity.this, "权限不够，root一下？", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "文件名为空或该文件夹已存在", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }).setNegativeButton("取消", null);
        mBilder.show();
    }

    RadioGroup mRadioGroup;
    Intent serviceIntent;
    public static String KEYWOED_BROADCAST = "com.example.dmrf.filemanager.BRODCAST";


    //显示搜索对话框
    private void searchDialog() {
        //用于确定是在当前目录搜索还是全部目录搜索
        mChecked = 1;
        LayoutInflater mLI = LayoutInflater.from(MainActivity.this);
        final View mLL = mLI.inflate(R.layout.search_dialog, null);
        mRadioGroup = mLL.findViewById(R.id.radiogroup_search);
        final RadioButton mCurrentPathButton = mLL.findViewById(R.id.search_currentpath);
        final RadioButton mWholePathButton = mLL.findViewById(R.id.search_wholepath);
        //设置默认在当前目录下检索
        mCurrentPathButton.setChecked(true);
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            //当选择改变时触发
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int checkId) {
                if (checkId == mCurrentPathButton.getId()) {
                    //当前目录标志为1
                    mChecked = 1;
                }

                if (checkId == mWholePathButton.getId()) {
                    //全部目录标志为2
                    mChecked = 2;
                }
            }
        });

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this).setView(mLL).setTitle("搜索").setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                keyWords = ((EditText) mLL.findViewById(R.id.edit_search)).getText().toString();
                if (keyWords.length() == 0) {
                    Toast.makeText(MainActivity.this, "关键字不能为空！", Toast.LENGTH_SHORT).show();
                    searchDialog();//递归了一下
                } else {
                    if (menuPosition == 1) {
                        mPath.setText(mRootPath);
                    } else {
                        mPath.setText(mSDCard);
                    }
                    //获取用户输入的关键字并发送广播--开始
                    Intent keyWordsIntent = new Intent();
                    keyWordsIntent.setAction(KEYWOED_BROADCAST);
                    //传递搜索的范围区间，1在当前路径下搜索，2在SD卡根目录下搜索
                    if (mChecked == 1) {
                        keyWordsIntent.putExtra("searchpath", mCurrentFilePath);
                    } else {
                        keyWordsIntent.putExtra("searchpath", mSDCard);
                    }
                    //传递关键字
                    keyWordsIntent.putExtra("keywords", keyWords);
                    //到这里为止是携带关键字信息并发送广播，会在Service服务中接收该广播并根据关键字进行搜索
                    //获取用户输入的关键字并发送广播-结束
                    getApplicationContext().sendBroadcast(keyWordsIntent);
                    serviceIntent = new Intent("com.android.service.FILE_SEARCH_START");
                    serviceIntent.setPackage("com.example.dmrf.filemanager");
                    MainActivity.this.startService(serviceIntent);//开启搜索服务
                    new AlertDialog.Builder(MainActivity.this).setView(R.layout.search_layout).setTitle("正在搜索中...").setPositiveButton("取消搜索", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Toast.makeText(MainActivity.this, "已取消搜索", Toast.LENGTH_SHORT).show();
                            MainActivity.this.stopService(serviceIntent);
                        }
                    }).setNegativeButton("隐藏进度条", null).create().show();
                }
            }
        }).setNegativeButton("取消", null);
        mBuilder.create().show();
    }

    //为GridView配置菜单资源
    private void initGridViewMenu() {
        mGridViewToolBar = (GridView) findViewById(R.id.file_gridview_toolbar);
        //设置选中时候的背景图片
        mGridViewToolBar.setSelector(R.mipmap.menu_item_selected);

        //设置背景图片
        mGridViewToolBar.setBackgroundResource(R.mipmap.menu_background);

        //设置列数
        mGridViewToolBar.setNumColumns(6);

        //设置居中对齐
        mGridViewToolBar.setGravity(Gravity.CENTER);

        //设置水平、垂直间距为10
        mGridViewToolBar.setVerticalSpacing(10);
        mGridViewToolBar.setHorizontalSpacing(10);

        //设置适配器
        mGridViewToolBar.setAdapter(getMenuAdapter(gridview_menu_title, grifview_menu_image));
    }

    //菜单适配器
    private SimpleAdapter getMenuAdapter(String[] menuNameArray, int[] imageResourceArray) {
        //数组列表用于存放映射表
        ArrayList<HashMap<String, Object>> mData = new ArrayList<HashMap<String, Object>>();

        for (int i = 0; i < menuNameArray.length; i++) {
            HashMap<String, Object> mMap = new HashMap<String, Object>();
            //将image映射成图片资源
            mMap.put("image", imageResourceArray[i]);

            //将name映射成标题
            mMap.put("name", menuNameArray[i]);

            mData.add(mMap);
        }
        //新建简单适配器，设置适配器的布局文件和映射关系
        SimpleAdapter mAdapter = new SimpleAdapter(this, mData, R.layout.item_menu, new String[]{"image", "name"}, new int[]{R.id.item_image, R.id.item_text});
        return mAdapter;

    }

    //长按列表项的时间的监听：对长按需要有一个控制，当列表中包括“返回根目录”和“返回上一层”时，需要对着两列进行屏蔽
    @Override
    public boolean onItemLongClick(AdapterView<?> arg0, View arg1, final int position, long arg3) {

        if (isAddBackUp == true) {//说明存在“返回根目录”和“返回上一层”这两列，需要对其进行屏蔽
            if (position != 0 && position != 1) {
                initItemLongClickListener(new File(mFilePath.get(position)));
            }
        }

        if (mCurrentFilePath.equals(mRootPath) || mCurrentFilePath.equals(mSDCard)) {
            initItemLongClickListener(new File(mFilePath.get(position)));
        }
        return false;
    }

    private String mCopyFaleName;
    private boolean isCopy = false;

    /*长按文件或文件夹时弹出带listview效果的功能菜单*/
    private void initItemLongClickListener(final File file) {
        //新建DialogInterface.OnClickListener监听器
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            //item的值就是从0开始的索引值（从列表项的第一项开始)
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (file.canRead()) {
                    //注意，对所有文件的操作必须建立在文件可读的基础上，否则会报错
                    if (item == 0) {
                        //复制
                        if (file.isFile() && "txt".equals((file.getName().substring(file.getName().lastIndexOf(".") + 1, file.getName().length())).toLowerCase())) {
                            Toast.makeText(MainActivity.this, "已复制", Toast.LENGTH_SHORT).show();
                            //复制标志位，                                                                                                 表明已复制
                            isCopy = true;
                            //获得复制文件的名字
                            mCopyFaleName = file.getName();
                            //记录复制文件的路径
                            OldPath = mCurrentFilePath + File.separator + mCopyFaleName;
                        } else {
                            Toast.makeText(MainActivity.this, "对不起，目前只支持复制文本文件！", Toast.LENGTH_SHORT).show();
                        }
                    } else if (item == 1) {//重命名
                        initRenameDialog(file);
                    } else if (item == 2) {//删除
                        initDeleteDialog(file);
                    }

                } else {
                    Toast.makeText(MainActivity.this, "对不起，您的访问权限不足！", Toast.LENGTH_SHORT).show();
                }
            }
        };
        //列表项名称
        String[] mMenu = {"复制", "重命名", "删除"};
        //显示操作选择对话框
        new AlertDialog.Builder(MainActivity.this).setTitle("请选择操作").setItems(mMenu, listener).setPositiveButton("取消", null).show();
    }

    //弹出删除文件/文件夹的对话框
    private void initDeleteDialog(final File file) {
        new AlertDialog.Builder(MainActivity.this).setTitle("提示！").setMessage("您确定要删除改" + (file.isDirectory() ? "文件夹" : "文件") + "吗？")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int witch) {
                        if (file.isFile()) {
                            //是文件则直接删除
                            file.delete();
                        } else {
                            //是文件夹则用这个方法删除
                            deleteFolder(file);
                        }
                        //重新遍历该文件的父目录
                        initFileListInfo(file.getParent());
                    }
                }).setNegativeButton("取消", null).show();
    }

    //删除文件夹的方法
    private void deleteFolder(File folder) {
        if (folder.exists()) {
            if (folder.isFile()) {
                folder.delete();
            } else if (folder.isDirectory()) {
                File[] files = folder.listFiles();
                if (files != null) {
                    for (File f : files) {
                        deleteFolder(f);
                    }
                }
                folder.delete();
            } else {
                Toast.makeText(MainActivity.this, "权限不足", Toast.LENGTH_SHORT).show();
            }
        }
      /*  File[] fileArray = folder.listFiles();
        if (fileArray.length == 0) {
            //如果是空文件夹则直接删除
            folder.delete();
        } else {
            //遍历该目录
            for (File currentFile : fileArray) {
                if (currentFile.exists() && currentFile.isFile()) {
                    //文件则直接删除
                    currentFile.delete();
                } else {
                    //递归删除
                    deleteFolder(currentFile);
                }
            }
            folder.delete();
        }
*/
    }


    EditText mET;

    //显示重命名对话框
    private void initRenameDialog(final File file) {
        LayoutInflater mLI = LayoutInflater.from(MainActivity.this);
        //初始化重命名对话框
        LinearLayout mLL = (LinearLayout) mLI.inflate(R.layout.rename_dialog, null);
        mET = mLL.findViewById(R.id.new_filename);
        //显示当前文件名
        mET.setText(file.getName());
        //设置监听器
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int witch) {
                String modifyName = mET.getText().toString();
                final String modifyFilePath = file.getParentFile().getPath() + File.separator;
                final String newFilePath = modifyFilePath + modifyName;
                //判断新的文件名是否已经在当前目录下存在
                if (new File(newFilePath).exists()) {
                    if (!modifyName.equals(file.getName())) {//把重命名操作时没做任何修改的情况过滤掉
                        //弹出该新命名后的文件已经存在的提示，并提示接下来的操作
                        new AlertDialog.Builder(MainActivity.this).setTitle("提示！").setMessage("该文件名已存在，是否覆盖？").setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                file.renameTo(new File(newFilePath));
                                Toast.makeText(MainActivity.this, "the file path is" + new File(newFilePath), Toast.LENGTH_SHORT).show();
                                //更新当前目录信息
                                initFileListInfo(file.getParentFile().getPath());
                            }
                        }).setNegativeButton("取消", null).show();
                    }
                } else {
                    //文件名不重复时直接重命名文件然后刷新列表
                    file.renameTo(new File(newFilePath));
                    initFileListInfo(file.getParentFile().getPath());
                }
            }
        };
        //显示重命名对话框
        new AlertDialog.Builder(MainActivity.this).setView(mLL).setPositiveButton("确定", listener).setNegativeButton("取消", null).create().show();
    }

    /**
     * 列表项单击时的事件监听
     **/
    @Override
    protected void onListItemClick(ListView listview, View view, int position, long id) {
        final File mFile = new File(mFilePath.get(position));
        //如果文件是可读的，我们就进去查看文件
        if (mFile.canRead()) {
            if (mFile.isDirectory()) {
                //如果是文件夹，则直接进入该文件夹，查看文件目录
                initFileListInfo(mFilePath.get(position));
            } else {
                //如果是文件，则用相应的方式打开文件
                String FileName = mFile.getName();
                String FileEnds = FileName.substring(FileName.lastIndexOf(".") + 1, FileName.length()).toLowerCase();
                if (FileEnds.equals("txt")) {
                    //显示进度条，表示正在读取
                    initProgressDialog(ProgressDialog.STYLE_HORIZONTAL);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //打开文本文件
                            try {
                                openTxtFile(mFile.getPath());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (true) {
                                if (isTxtDataOk == true) {
                                    //关闭进度条
                                    mProgressDialog.dismiss();
                                    executeIntent(txtData.toString(), mFile.getPath());
                                    break;
                                }

                                if (isCancleProgressDialog == true) {
                                    //关闭进度条
                                    mProgressDialog.dismiss();
                                    break;
                                }
                            }
                        }
                    }).start();
                } else if (FileEnds.equals("html") || FileEnds.equals("htm") || FileEnds.equals("mht")) {
                    //如果是html文件，则用自己写的工具打开
                    Intent intent = new Intent(MainActivity.this, WebActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("filepath", mFile.getPath());
                    startActivity(intent);
                } else {
                    openFile(mFile);
                }

            }
        } else {
            //如果文件不可读，我们给出提示不能访问，防止用户操作系统文件造成系统崩溃等
            Toast.makeText(MainActivity.this, "该文件为空或您的访问权限不足！", Toast.LENGTH_SHORT).show();
        }

    }

    //调用系统的方法打开文件的方法
    private void openFile(File mFile) {
        if (mFile.isDirectory()) {
            initFileListInfo(mFile.getPath());
        } else {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(Intent.ACTION_VIEW);
            //设置当前的文件类型
            intent.setDataAndType(Uri.fromFile(mFile), getMIMEType(mFile));
            startActivity(intent);
        }
    }

    //获得MIME类型的方法
    private String getMIMEType(File file) {
        String type = "";
        String fileName = file.getName();
        //取出文件后缀名并转化为小写
        String fileEnds = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length()).toLowerCase();
        if (fileEnds.equals("m4a") || fileEnds.equals("mp3") || fileEnds.equals("mid") || fileEnds.equals("xmf") || fileEnds.equals("ogg") || fileEnds.equals("wav")) {
            type = "audio/*";// 系统将列出所有可能打开音频文件的程序选择器
        } else if (fileEnds.equals("3gp") || fileEnds.equals("mp4")) {
            type = "video/*";// 系统将列出所有可能打开视频文件的程序选择器
        } else if (fileEnds.equals("jpg") || fileEnds.equals("gif") || fileEnds.equals("png") || fileEnds.equals("jpeg") || fileEnds.equals("bmp")) {
            type = "image/*";// 系统将列出所有可能打开图片文件的程序选择器
        } else {
            type = "*/*"; // 系统将列出所有可能打开该文件的程序选择器
        }
        return type;
    }

    //执行intent跳转的方法
    private void executeIntent(String s, String path) {
        Intent intent = new Intent(MainActivity.this, EditTxtActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //传递文件的标题，路径和内容
        intent.putExtra("path", path);
        intent.putExtra("title", new File(path).getName());
        intent.putExtra("data", s);
        //跳转到EditTxtActivity
        startActivity(intent);
    }


    //进度条
    private boolean isCancleProgressDialog = false;
    ProgressDialog mProgressDialog;

    //弹出正在解析文本数据的ProgressDialog
    private void initProgressDialog(int styleHorizontal) {
        isCancleProgressDialog = false;
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("提示");
        mProgressDialog.setMessage("正在为你解析文本数据，请稍后...");
        mProgressDialog.setCancelable(true);
        mProgressDialog.setButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isCancleProgressDialog = true;
                mProgressDialog.dismiss();
            }
        });
        mProgressDialog.show();
    }

    private boolean isTxtDataOk = false;
    String txtData;

    //打开文本文件的方法
    private void openTxtFile(String path) throws IOException {
        isTxtDataOk = false;
        FileInputStream fis = new FileInputStream(new File(path));
        StringBuilder mSB = new StringBuilder();
        int m;
        //读取文本文件的内容
        while ((m = fis.read()) != -1) {
            mSB.append((char) m);
        }
        fis.close();
        //读取完毕
        isTxtDataOk = true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!mCurrentFilePath.equals(mRootPath) && !mCurrentFilePath.equals(mSDCard)) {
                File file = new File(mCurrentFilePath);
                String path = file.getParent();
                initFileListInfo(path);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
