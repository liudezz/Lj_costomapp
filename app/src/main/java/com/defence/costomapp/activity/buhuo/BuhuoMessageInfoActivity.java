package com.defence.costomapp.activity.buhuo;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.UiSettings;
import com.amap.api.maps2d.model.CameraPosition;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MarkerOptions;
import com.defence.actionsheetmenumlib.JFActionSheetMenu;

import com.defence.costomapp.R;
import com.defence.costomapp.adapter.BuhuoMessageInfoAdapter;
import com.defence.costomapp.base.BaseActivity;
import com.defence.costomapp.bean.AllMachineBean;
import com.defence.costomapp.bean.BuhuoInfoEntity;
import com.defence.costomapp.bean.BuhuoMessageEntity;
import com.defence.costomapp.bean.MachineEntity;
import com.defence.costomapp.utils.DividerItemDecoration;
import com.defence.costomapp.utils.SgqUtils;
import com.defence.costomapp.utils.SharePerenceUtil;
import com.defence.costomapp.utils.httputils.HttpInterface;
import com.defence.costomapp.base.Urls;
import com.defence.costomapp.myinterface.RVItemClickListener;
import com.defence.costomapp.utils.APPUtils;
import com.defence.costomapp.utils.ActionSheelUtil;
import com.defence.costomapp.utils.RecyclerViewUtils;
import com.google.gson.Gson;
import com.loopj.android.http.RequestParams;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 补货信息列表
 */
public class BuhuoMessageInfoActivity extends BaseActivity {


    List<String> machinename = new ArrayList<>();
    String selectNum = "";
    private BuhuoMessageEntity buhuoMessageEntity;
    private int alarmStock;
    private MachineEntity machineEntity;
    private ArrayList<BuhuoInfoEntity> buhuoInfoEntities;
    private BuhuoMessageInfoAdapter buhuoMessageInfoAdapter;
    private RecyclerView rv;
    private TextView machineAddr;
    private MapView mMapView = null;
    //侧滑菜单
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Spinner tv_selectMachine;
    private ArrayAdapter<String> arr_adapter;

    // 构建Runnable对象，在runnable中更新界面
    Runnable runnableUi = new Runnable() {
        //更新界面
        @Override
        public void run() {
            //适配器
            arr_adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, machinename);
            //设置样式
            arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            //加载适配器
            tv_selectMachine.setAdapter(arr_adapter);

        }
    };


    private int mIndex = 0;//位置
    private boolean move = false;


    private TextView tv_transferMachine, tv_arrangingGoods, tv_transferAmount;
    private Handler handler = null;
    private TextView tv_confirm;
    private String gui_ge_id;
    private String tv_transferAmountbefore;

    private LinearLayoutManager mLinearLayoutManager;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buhuo_message_info);
        //获取地图控件引用
        mMapView = findViewById(R.id.map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mMapView.onCreate(savedInstanceState);

        rv = findViewById(R.id.buhuoinforv);
        machineAddr = findViewById(R.id.machineAddr);

        //创建属于主线程的handler
        handler = new Handler();

//        所有机器
        getAllMachineData();

//        侧滑菜单
        ShowNavigation();

//        RecyclerViewUtils.setReRecyclerView(this, rv);
        mLinearLayoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setItemAnimator(new DefaultItemAnimator());
        rv.addItemDecoration(new DividerItemDecoration(
                this, DividerItemDecoration.VERTICAL_LIST));

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        getdata();
        rv.addOnScrollListener(new RecyclerViewListener());

        findViewById(R.id.buhuodone).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RequestParams params = new RequestParams();
                params.put("machineNo", machineEntity.getMachinenumber());
                params.put("machineID", machineEntity.getId());
                params.put("isrepair", "1");
                params.put("itemNo", "");
                httpUtils.doPost(Urls.setBuhuoMessageRead(), SgqUtils.BUHUO_TYPE, params, new HttpInterface() {
                    @Override
                    public void onSuccess(Gson gson, Object result) {
                        BuhuoMessageActivity.handler.sendEmptyMessage(1);
                    }
                });
            }
        });
    }

    private void getdata() {

        buhuoMessageEntity = (BuhuoMessageEntity) getIntent().getSerializableExtra("bundle");
        buhuoInfoEntities = new ArrayList<>();
        if (buhuoMessageEntity != null) {
            RequestParams params = new RequestParams();
            params.put("machineNo", buhuoMessageEntity.getMachinenumber());
            params.put("machineID", buhuoMessageEntity.getId());
            httpUtils.doPost(Urls.getBuhuoMessageInfo(), SgqUtils.BUHUO_TYPE, params, new HttpInterface() {
                @Override
                public void onSuccess(Gson gson, Object result) {
                    JSONObject jb = (JSONObject) result;
                    try {
                        alarmStock = jb.getInt("alarmStock");
                        machineEntity = gson.fromJson(jb.getJSONObject("machine").toString(), MachineEntity.class);
                        machineAddr.setText(machineEntity.getAddress() + machineEntity.getDetailedinstalladdress());

                        AMap aMap = mMapView.getMap();
                        LatLng latLng = new LatLng(machineEntity.getLatitude(), machineEntity.getLongitude());
                        UiSettings uiSettings = aMap.getUiSettings();
                        // 通过UISettings.setZoomControlsEnabled(boolean)来设置缩放按钮是否能显示
                        uiSettings.setZoomControlsEnabled(false);
                        uiSettings.setScrollGesturesEnabled(false);
                        uiSettings.setZoomGesturesEnabled(false);
                        //可视化区域，将指定位置指定到屏幕中心位置
                        CameraUpdate cameraUpdate = CameraUpdateFactory
                                .newCameraPosition(new CameraPosition(latLng, 19, 0, 30));
                        aMap.moveCamera(cameraUpdate);
                        aMap.addMarker(new MarkerOptions().position(latLng));
                        aMap.setOnMapClickListener(new AMap.OnMapClickListener() {
                            @Override
                            public void onMapClick(final LatLng latLng) {
                                ActionSheelUtil.showMenu(BuhuoMessageInfoActivity.this, "选择导航",
                                        new String[]{"百度地图"}, false, new JFActionSheetMenu.OnActionSheetItemClickListener() {
                                            @Override
                                            public void onItemClick(View view, int itemPosition) {
                                                switch (itemPosition) {
                                                    case 0:
                                                        APPUtils.jumpBaiduNav(BuhuoMessageInfoActivity.this, latLng);
                                                        break;
                                                }
                                            }

                                            @Override
                                            public void onCanceClick(View view) {

                                            }
                                        });
                            }
                        });
                        JSONArray ja = jb.getJSONArray("list");
                        for (int i = 0; i < ja.length(); i++) {
                            BuhuoInfoEntity buhuoInfoEntity = gson.fromJson(ja.get(i).toString(), BuhuoInfoEntity.class);
                            buhuoInfoEntities.add(buhuoInfoEntity);
                        }
//                        if (buhuoMessageInfoAdapter == null) {
                        sortInfo();
                        buhuoMessageInfoAdapter = new BuhuoMessageInfoAdapter(alarmStock, buhuoInfoEntities, BuhuoMessageInfoActivity.this, new RVItemClickListener() {
                            @Override
                            public void onItemClick(int position) {
                                if (Integer.parseInt(buhuoInfoEntities.get(position).getKu_cun()) <= 0) {
                                    Toast.makeText(BuhuoMessageInfoActivity.this, "剩余量为0，不能调货", Toast.LENGTH_SHORT).show();
                                } else {
                                    tv_transferMachine.setText(machineEntity.getAddress() + machineEntity.getDetailedinstalladdress());
                                    tv_arrangingGoods.setText(buhuoInfoEntities.get(position).getLatticenumbers() + "-" + buhuoInfoEntities.get(position).getDescVal() + "-" + buhuoInfoEntities.get(position).getShowName());
                                    tv_transferAmountbefore = buhuoInfoEntities.get(position).getKu_cun().toString();
                                    tv_transferAmount.setText(buhuoInfoEntities.get(position).getKu_cun().toString());
                                    gui_ge_id = buhuoInfoEntities.get(position).getGui_ge_id();

                                    if (drawerLayout.isDrawerOpen(navigationView)) {
                                        drawerLayout.closeDrawer(navigationView);
                                    } else {
                                        drawerLayout.openDrawer(navigationView);
                                        //监听RecyclerView滚动状态
                                        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
                                            @Override
                                            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                                                super.onScrollStateChanged(recyclerView, newState);
                                                if (recyclerView.getLayoutManager() != null) {
                                                    LinearLayoutManager layoutManager = (LinearLayoutManager) rv.getLayoutManager();
                                                    //获取可视的第一个view
                                                    View topView = layoutManager.getChildAt(0);
                                                    if (topView != null) {
                                                        //得到该View的数组位置
                                                        mIndex = layoutManager.getPosition(topView);
                                                    }
                                                }
                                            }
                                        });
                                    }
                                }
                            }
                        });
                        rv.setAdapter(buhuoMessageInfoAdapter);


//                        } else {qina
//                            buhuoMessageInfoAdapter.notifyDataSetChanged();
//                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /*侧滑菜单*/
    private void ShowNavigation() {

        tv_transferMachine = findViewById(R.id.tv_transferMachine);
        tv_arrangingGoods = findViewById(R.id.tv_arrangingGoods);
        tv_transferAmount = findViewById(R.id.tv_transferAmount);
        drawerLayout = findViewById(R.id.activity_navigation);
        navigationView = findViewById(R.id.nav);
        tv_selectMachine = findViewById(R.id.tv_selectMachine);
        tv_confirm = findViewById(R.id.tv_confirm);

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
//关闭手势滑动
//        mDrawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
//打开手势滑动
        //初始化窗口属性，让状态栏和导航栏透明
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                drawerLayout.closeDrawer(navigationView);
                return true;
            }
        });

        tv_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                int after = Integer.parseInt(tv_transferAmount.getText().toString());
                int before = Integer.parseInt(tv_transferAmountbefore.toString());
                if (after > before) {
                    Toast.makeText(getApplicationContext(), "调离量不能大于库存量", Toast.LENGTH_SHORT).show();
                } else {
                    //   生成调货任务
                    machineTask();
                }
            }
        });
    }

    private void machineTask() {
        String groupid = SharePerenceUtil.getStringValueFromSp("groupid");
        String[] splitgoods = tv_arrangingGoods.getText().toString().split("-");

        RequestParams params = new RequestParams();
//       当前详情面机器编号 LJ-010-04-001-001

        params.put("machine_ff_mtl", buhuoMessageEntity.getMachinenumber());
//       当前长按所选的商品规格ID
        params.put("iiint2_mkl", gui_ge_id);
//       调货至某台机器上, 机器列表中手动选中的机器
        params.put("machine_tt_mtl", selectNum);
//       商品规格全称, 商品名-规格名
        params.put("vvval1_mkl", splitgoods[2] + "-" + splitgoods[1]);
//       调货的数量, 最小值是壹(1),最大值是当前所选商品规格柜子库存剩余量
        params.put("iiint1_mkl", tv_transferAmount.getText().toString());
//        {登录返回的adminGroupID}
        params.put("adminGroupID", groupid);
        httpUtils.doPost(Urls.machineTask(), SgqUtils.BUHUO_TYPE, params, new HttpInterface() {

            @Override
            public void onSuccess(Gson gson, Object result) {
                Toast.makeText(getApplicationContext(), "调货成功", Toast.LENGTH_SHORT).show();
                drawerLayout.closeDrawer(navigationView);
                getdata();
//                buhuoMessageInfoAdapter.notifyDataSetChanged();
//                move(mIndex);


            }
        });

    }

    private void getAllMachineData() {

        OkHttpClient client = new OkHttpClient();
        //创建一个Request
        Request request = new Request.Builder()
                .get()
                .url(Urls.getAllMachine())
                .build();
        //通过client发起请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                try {
                    Toast.makeText(BuhuoMessageInfoActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String str = response.body().string();

                    List<Object> list_sum = new Gson().fromJson(str, List.class);
                    List<AllMachineBean> list_sum2 = new ArrayList<>();

                    for (Object obj : list_sum) {
                        AllMachineBean allMachineBean = new Gson().fromJson(obj.toString(), AllMachineBean.class);
                        list_sum2.add(allMachineBean);
                    }

                    for (int i = 0; i < list_sum2.size(); i++) {
                        machinename.add(list_sum2.get(i).getMachinename());
                    }
                    new Thread() {
                        public void run() {
                            //更新ui
                            handler.post(runnableUi);
                        }
                    }.start();

                    tv_selectMachine.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view,
                                                   int position, long id) {
                            //拿到被选择项的值
                            String selectedItem = (String) tv_selectMachine.getSelectedItem();
                            for (int i = 0; i < list_sum2.size(); i++) {
                                if (selectedItem.equals(list_sum2.get(i).getMachinename())) {
                                    selectNum = list_sum2.get(i).getMachinenumber();
                                }
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            // TODO Auto-generated method stub
                        }
                    });
                }
            }
        });
    }

    private void sortInfo() {
        Comparator<BuhuoInfoEntity> itemComparator = new Comparator<BuhuoInfoEntity>() {
            public int compare(BuhuoInfoEntity info1, BuhuoInfoEntity info2) {
                return info1.getLatticenumbers() - info2.getLatticenumbers();
            }
        };

        Collections.sort(buhuoInfoEntities, itemComparator);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mMapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mMapView.onSaveInstanceState(outState);
    }

    private void move(int n) {
        if (n < 0 || n >= buhuoMessageInfoAdapter.getItemCount()) {
            Toast.makeText(this, "超出范围了", Toast.LENGTH_SHORT).show();
            return;
        }
//        mIndex = n;
        rv.stopScroll();
        smoothMoveToPosition(n);
    }

    private void smoothMoveToPosition(int n) {

        int firstItem = mLinearLayoutManager.findFirstVisibleItemPosition();
        int lastItem = mLinearLayoutManager.findLastVisibleItemPosition();
        if (n <= firstItem) {
            rv.smoothScrollToPosition(n);
        } else if (n <= lastItem) {
            int top = rv.getChildAt(n - firstItem).getTop();
            rv.smoothScrollBy(0, top);
        } else {
            rv.smoothScrollToPosition(n);
            move = true;
        }

    }

    class RecyclerViewListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (move && newState == RecyclerView.SCROLL_STATE_IDLE) {
                move = false;
                int n = mIndex - mLinearLayoutManager.findFirstVisibleItemPosition();
                if (0 <= n && n < rv.getChildCount()) {
                    int top = rv.getChildAt(n).getTop();
                    rv.smoothScrollBy(0, top);
                }

            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
        }
    }

}
