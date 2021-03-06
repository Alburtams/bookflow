package com.hust.bookflow.activity;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.activity.CaptureActivity;
import com.hust.bookflow.MyApplication;
import com.hust.bookflow.R;
import com.hust.bookflow.fragment.BookFragment;
import com.hust.bookflow.fragment.HomeFragment;
import com.hust.bookflow.fragment.LikeListFragment;
import com.hust.bookflow.fragment.SettingFragment;
import com.hust.bookflow.fragment.factory.FragmentFactory;
import com.hust.bookflow.utils.Constant;
import com.hust.bookflow.utils.Constants;
import com.hust.bookflow.utils.PreferncesUtils;
import com.hust.bookflow.utils.SpUtils;
import com.hust.bookflow.utils.ToastUtils;
import com.hust.bookflow.utils.UIUtils;
import com.hust.bookflow.utils.UserUtils;
import com.hust.bookflow.utils.jsoupUtils.GetNavImage;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener, View.OnClickListener {

    private NavigationView mainnav;
    private android.support.v4.widget.DrawerLayout maindrawerlayout;
    private RelativeLayout main_container;
    private Toolbar main_toolbar;
    private FragmentManager mFragmentManager;
    private String title;
    private Fragment DefaultFragment;
    private long mExitTime = 0;
    private View headerView;
    private ImageView nav_header_img;
    private TextView nav_header_title;
    private ImageButton nav_header_button;
    public static final String ACTION_LOCAL_SEND = "action.local.send";
    private static final String SAVE_STATE_TITLE = "title";
    private final LocalBroadcastReceiver localReceiver = new LocalBroadcastReceiver();
    private List<String> navList = new ArrayList<>();
    private mAsyncTask mAsy;
    private TextView nav_header_logintxt;
    private MenuItem nav_header_logout;
    private SharedPreferences userData;
    private String stuId;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /**
         *启动Activity,设置主题
         */
        String nowtheme = PreferncesUtils.getString(this, Constants.PREF_KEY_THEME, "1");
        if (nowtheme.equals("1")) {
            setTheme(R.style.AppTheme);
        } else {
            setTheme(R.style.AppTheme_Light);

        }
        setContentView(R.layout.activity_main);
        initView();
        initToolbar();
        setupDrawerContent();
        initFragment(savedInstanceState);
        initreceiver();
        if (MyApplication.isNetworkAvailable(this)) {
            mAsy = new mAsyncTask();
            mAsy.execute();
            // TODO 添加获取首页图书信息的函数
        } else {
            setDefaultNav();
        }
        //SharedPreferences preferences = getSharedPreferences("userInfo",  Activity.MODE_PRIVATE);
        //String stuid= UserUtils.getStuID(preferences);
        /*if (stuid.equals("")) {
            // 未登录
        }*/
    }


    /**
     * 初始化广播接收器
     */
    private void initreceiver() {
        LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver, new IntentFilter(ACTION_LOCAL_SEND));
    }

    /**
     * 初始化View
     */
    private void initView() {
        this.maindrawerlayout = (DrawerLayout) findViewById(R.id.main_drawer_layout);
        this.mainnav = (NavigationView) findViewById(R.id.main_nav);
        this.main_toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        this.main_container = (RelativeLayout) findViewById(R.id.main_container);
        main_toolbar.inflateMenu(R.menu.menu_toolbar);
        main_toolbar.setOnMenuItemClickListener(this);
        main_toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                maindrawerlayout.openDrawer(GravityCompat.START);
            }
        });
        headerView = mainnav.getHeaderView(0);
        nav_header_img = (ImageView) headerView.findViewById(R.id.nav_header_img);
        nav_header_title = (TextView) headerView.findViewById(R.id.nav_header_title);
        nav_header_button = (ImageButton) headerView.findViewById(R.id.nav_header_button);
        nav_header_logintxt = (TextView) headerView.findViewById(R.id.nav_header_logintxt);

        nav_header_logout = mainnav.getMenu().findItem(R.id.nav_menu_exit);

        nav_header_button.setOnClickListener(this);

        userData = getSharedPreferences("userInfo", Activity.MODE_PRIVATE);
        stuId = UserUtils.getStuID(userData);
        if (!stuId.equals("")) {
            nav_header_button.setEnabled(false);
            nav_header_logintxt.setText(stuId);
            nav_header_logout.setVisible(true);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.action_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * 初始化Fragment
     */
    private void initFragment(Bundle savedInstanceState) {
        SpUtils.putBoolean(this, "istheme", true);
        //根据title创建Fragment
        if (savedInstanceState != null) {
            title = savedInstanceState.getString(SAVE_STATE_TITLE);
        }
        if (title == null) {
            title = UIUtils.getString(this, R.string.nav_menu_home);
        }


        mFragmentManager = getSupportFragmentManager();
        DefaultFragment = mFragmentManager.findFragmentByTag(title);
        if (DefaultFragment == null) {
            Fragment homeFragment = new BookFragment();
            mFragmentManager.beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .add(R.id.main_container, homeFragment, title)
                    .commit();
            DefaultFragment = homeFragment;
        }
    }

    /**
     * 清空其他Fragment
     */
    public void removeFragment(String title) {
        mFragmentManager = getSupportFragmentManager();
        @SuppressLint("RestrictedApi") List<Fragment> fragments = mFragmentManager.getFragments();
        if (fragments == null) {
            return;
        }

        //保留title的Fragment
        for (Fragment fragment : fragments) {
            if (fragment == null || fragment.getTag().equals(title))
                continue;
            mFragmentManager.beginTransaction().remove(fragment).commit();
        }
    }

    /**
     * 初始化Toolbar
     */
    private void initToolbar() {
        if (main_toolbar.getTitle() == null) {
            main_toolbar.setTitle(UIUtils.getString(this, R.string.nav_menu_home));
            mainnav.getMenu().getItem(0).setChecked(true);
        }
    }

    /**
     * nav menu点击事件
     */
    private void setupDrawerContent() {
        mainnav.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                menuItem.setChecked(true);
                maindrawerlayout.closeDrawers();
                String title = (String) menuItem.getTitle();
                if (title.equals(getString(R.string.string_exit))) {
                    logout();
                } else {
                    if ((title.equals(getString(R.string.nav_menu_bookToReturn)) || title.equals(getString(R.string.nav_menu_like))) && stuId.equals("")) {
                        ToastUtils.show(MainActivity.this, "请先登录");
                        LoginActivity.toActivity(MainActivity.this);
                    } else {
                        main_toolbar.setTitle(title);
                        //根据menu的Title开启Fragment
                        if(title.equals(getString(R.string.nav_menu_like))){
                            SharedPreferences.Editor editor = userData.edit();
                            editor.putString("tag", title);
                            editor.apply();
                        }
                        switchFragment(title);
                    }
                }

                return true;
            }
        });
    }

    /**
     * 根据nav的menu开启Fragment
     *
     * @param title
     */
    private void switchFragment(String title) {
        FragmentTransaction transaction = mFragmentManager.beginTransaction();

        //根据Tag判断是否已经开启了Fragment，如果开启了就直接复用，没开启就创建
        Fragment fragment = mFragmentManager.findFragmentByTag(title);
        if (fragment == null) {
            transaction.hide(DefaultFragment);
            fragment = createFragmentByTitle(title);
            transaction.add(R.id.main_container, fragment, title);
            DefaultFragment = fragment;
        } else if (fragment != null) {
            transaction.hide(DefaultFragment).show(fragment);
            DefaultFragment = fragment;
        }
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).
                commit();

    }

    public void tagFragmentToList(String title){
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        transaction.hide(DefaultFragment);
        SharedPreferences.Editor editor = userData.edit();
        editor.putString("tag", title);
        editor.apply();
        main_toolbar.setTitle(title);
        Fragment fragment = new LikeListFragment();
        transaction.add(R.id.main_container, fragment, title);
        DefaultFragment = fragment;
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constant.REQ_PERM_CAMERA:
                // 摄像头权限申请
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 获得授权
                    startQrCode();
                } else {
                    // 被禁止授权
                    Toast.makeText(MainActivity.this, "请至权限中心打开本应用的相机访问权限", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    // 开始扫码
    private void startQrCode() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // 申请权限
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, Constant.REQ_PERM_CAMERA);
            return;
        }
        // 二维码扫码
        Bundle bundle = new Bundle();
        bundle.putString("flag", "1");
        Intent intent = new Intent(MainActivity.this, CaptureActivity.class).putExtras(bundle);
        startActivity(intent);
    }

    /**
     * Toolbar menu点击事件
     *
     * @param item
     * @return
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_menu_search:
                Intent mIntent = new Intent(MainActivity.this, SearchActivity.class);
                startActivity(mIntent);
                break;
            //添加扫码按钮
            case R.id.toolbar_menu_scan:
                startQrCode();
                break;
        }
        return false;
    }

    private void logout() {
        userData.edit().clear().apply();
        nav_header_button.setEnabled(true);
        nav_header_logintxt.setText(R.string.string_nav_login);
        nav_header_logout.setVisible(false);
        ToastUtils.show(MainActivity.this, "您已成功退出登录");
    }

    /**
     * 根据title创建Fragment
     *
     * @param title
     * @return
     *
     */
    private Fragment createFragmentByTitle(String title) {
        if (title.equals(Constants.SETTING)) {
            SettingFragment mSettingFragment = new SettingFragment();
            return mSettingFragment;
        } else {
            Fragment fragment = FragmentFactory.getFragment(title);
            return fragment;
        }

    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!maindrawerlayout.isDrawerOpen(GravityCompat.START)) {
                if ((System.currentTimeMillis() - mExitTime) > 2000) {
                    ToastUtils.show(MainActivity.this, "再按一次退出程序");
                    mExitTime = System.currentTimeMillis();
                } else {
                    ToastUtils.cancel();
                    finish();
                }
            } else {
                maindrawerlayout.closeDrawers();
            }
        }
        return true;
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outPersistentState.putString(SAVE_STATE_TITLE, title);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.nav_header_button:
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
        }
    }

    public class LocalBroadcastReceiver extends BroadcastReceiver {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceive(Context context, Intent intent) {
            setDefaultNav();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setDefaultNav() {
        nav_header_img.setImageDrawable(getDrawable(R.drawable.nav_bg));
        nav_header_title.setText("书上链，恋上书");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localReceiver);
    }

    @Override
    protected void onPause() {
        if (mAsy != null && mAsy.getStatus() == AsyncTask.Status.RUNNING) {
            mAsy.cancel(true);
        }
        super.onPause();
    }

    class mAsyncTask extends AsyncTask<Void, Void, List<String>> {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        protected void onPostExecute(List<String> list) {

            setDefaultNav();

        }

        @Override
        protected List<String> doInBackground(Void... voids) {
            if (isCancelled()) {
                return null;
            } else {
                GetNavImage getimage = new GetNavImage();
                navList = getimage.getImage();
                return navList;
            }
        }
    }
}
