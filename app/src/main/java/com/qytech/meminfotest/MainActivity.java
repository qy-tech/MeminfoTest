package com.qytech.meminfotest;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {

    public static String TAG = MainActivity.class.getName();
    private LineChart chart;
    private Spinner spinner;
    private String[] spinnerItems = new String[]{"Slab", "SUnreclaim"};
    private ArrayAdapter<String> adapter;
    private TextView tv_startTime;
    private TextView tv_elapsed;
    private Button bt_start;
    private GetMemThread getMemThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        chart = findViewById(R.id.meminfoChart);
        initView();
        initThread();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"onStop");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
        if(chart!=null && chart.getLineData()!=null){
            Log.d(TAG,"count: "+chart.getLineData().getEntryCount());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy");
    }

    private void initThread() {
        getMemThread = new GetMemThread();
    }

    private void initView() {
        initSpinner();
        initChart(chart);

        tv_startTime = (TextView) findViewById(R.id.tv_startTime);
        tv_elapsed = (TextView) findViewById(R.id.tv_elapsed);
        bt_start = (Button) findViewById(R.id.bt_start);
        bt_start.setOnClickListener(this);

        showLineDataSet(chart, "Slab", Color.BLUE);
        addLine(chart, "SUnreclaim", Color.RED);

    }

    private void initSpinner() {
        //Slab: cat /proc/meminfo 如果一直增大，内容存在泄漏
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, spinnerItems);  //创建一个数组适配器
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);     //设置下拉列表框的下拉选项样式

        spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(new SpinnerSelectedListener());
        spinner.setAdapter(adapter);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.bt_start) {
            if (bt_start.getText().equals("Stop")) {
                Log.d(TAG, "Stop get mem..");
                getMemThread.exit = false;
                getMemThread.commandItem = 0;
                bt_start.setText("Start");
            } else {
                if(spinner.getSelectedItem().toString().equals(spinnerItems[0])){
                    Log.d(TAG, "Start Slab");
                    if(chart!=null &&chart.getLineData()!=null){
                        chart.getLineData().getDataSets().get(0).clear();
                    }
                    setChartDescription(chart, "Slab [kB]");
                    getMemInfo(0);
                }else if(spinner.getSelectedItem().toString().equals(spinnerItems[1])){
                    Log.d(TAG, "Start SUnreclaim");
                    if(chart!=null &&chart.getLineData()!=null){
                        chart.getLineData().getDataSets().get(0).clear();
                    }
                    setChartDescription(chart, "SUnreclaim [kB]");
                    getMemInfo(1);
                }
                Date date = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                String sim = dateFormat.format(date);
                //Log.i("md", "时间sim为： " + sim);
                tv_startTime.setText(sim);
                bt_start.setText("Stop");
            }
        }
    }

    //使用数组形式操作
    class SpinnerSelectedListener implements OnItemSelectedListener {
        private String choose;

        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            choose = adapter.getItem(position);
            Log.d(TAG, "arg2= " + position + ",  choose=" + choose);
        }

        public void onNothingSelected(AdapterView<?> arg0) {

        }
    }

    /**-----------------------图表----------------------*/
    /**
     * 图表设置
     *
     * @param lineChart
     */
    private void initChart(LineChart lineChart) {
        lineChart.setDrawGridBackground(false); //是否展示网格线
        lineChart.setDrawBorders(false);        //是否显示边界
        lineChart.setDragEnabled(true);        //是否可以拖动
        lineChart.setTouchEnabled(true);        //是否有触摸事件
        lineChart.animateY(2500);        //设置XY轴动画效果
        lineChart.animateX(1500);
        lineChart.setScaleEnabled(true);        // 可缩放
        lineChart.setBackgroundColor(Color.WHITE); //Color.LTGRAY
//        Description description = new Description();
//        description.setText(des);
//        description.setEnabled(true);
//        lineChart.setDescription(description);
        /***XY轴的设置***/
        XAxis xAxis = lineChart.getXAxis();
        YAxis leftYAxis = lineChart.getAxisLeft();
        YAxis rightYaxis = lineChart.getAxisRight();

        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); //X轴设置显示位置在底部
        xAxis.setAxisMinimum(0f);
        xAxis.setGranularity(1f);
        //保证Y轴从0开始，不然会上移一点
        //leftYAxis.setAxisMinimum(0f);
        //rightYaxis.setAxisMinimum(0f);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int)value + "s";
            }
        });
        leftYAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int)value + "";
            }
        });
        rightYaxis.setDrawGridLines(false);
        leftYAxis.setDrawGridLines(false);   //去掉网格线
        //leftYAxis.enableGridDashedLine(5f, 3f, 0f);
        //leftYAxis.setGridLineWidth(1);
//        leftYAxis.setAxisMaximum(35);
//        leftYAxis.setAxisMinimum(-35);
        rightYaxis.setEnabled(false);
        //xAxis.setLabelCount(2,false);  //设置X轴分割数量

        /***折线图例 标签 设置***/
        Legend legend = lineChart.getLegend(); //(只有当数据集存在时候才生效)
        //设置显示类型，LINE CIRCLE SQUARE EMPTY 等等 多种方式，查看LegendForm 即可
        legend.setForm(Legend.LegendForm.LINE);
        legend.setTextSize(12f);
        //legend.setTextColor(Color.BLACK);  // 颜色
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);//显示位置 左下方
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);        //是否绘制在图表里面
    }

    private void setChartDescription(LineChart chart, String text) {
        Description description = new Description();
        description.setText(text);
        description.setTextSize(18);
        description.setYOffset(10);
        chart.setDescription(description);
    }

    /**
     * 创建第一根线
     *
     * @param lineChart
     * @param name
     * @param color
     */
    private void showLineDataSet(LineChart lineChart, String name, int color) {
        List<Entry> entries = new ArrayList<>();
        LineDataSet lineDataSet = new LineDataSet(entries, name);
        initLineDataSet(lineDataSet, color, null); //使用了折线
        LineData lineData = new LineData(lineDataSet);
        lineChart.setData(lineData);
    }

    /**
     * 在chart中再添加曲线
     *
     * @param lineChart
     * @param name
     * @param color
     */
    private void addLine(LineChart lineChart, String name, int color) {
        List<Entry> entries = new ArrayList<>();
        LineDataSet lineDataSet = new LineDataSet(entries, name);
        initLineDataSet(lineDataSet, color, null); //使用了折线
        lineChart.getLineData().addDataSet(lineDataSet);
        lineChart.invalidate();
    }

    /**
     * 曲线初始化设置 一个LineDataSet 代表一条曲线
     *
     * @param lineDataSet 线条
     * @param color       线条颜色
     * @param mode
     */
    private void initLineDataSet(LineDataSet lineDataSet, int color, LineDataSet.Mode mode) {
        lineDataSet.setColor(color);
        lineDataSet.setCircleColor(color);
        lineDataSet.setLineWidth(1f);
        lineDataSet.setCircleRadius(2f);
        lineDataSet.setDrawCircleHole(false); //设置曲线值的圆点是实心还是空心
        lineDataSet.setValueTextSize(10f);

        lineDataSet.setDrawFilled(false); //设置折线图填充 //不填充折线以下的颜色
        lineDataSet.setFormLineWidth(1f);
        lineDataSet.setFormSize(15.f);
        if (mode == null) {  //LineDataSet.Mode.LINEAR
            lineDataSet.setMode(LineDataSet.Mode.LINEAR);//设置曲线展示为圆滑曲线（如果不设置则默认折线）
        } else {
            lineDataSet.setMode(mode);
        }
        lineDataSet.setDrawCircles(false);
        lineDataSet.setDrawValues(false);  //显示折线上的值
        lineDataSet.setHighLightColor(Color.BLUE);            // 设置点击时高亮的点的颜色

        //lineDataSet.setDrawHorizontalHighlightIndicator(false); //点击value之后的横线和绿线
        //lineDataSet.setDrawVerticalHighlightIndicator(false);
    }

    /**
     * 获取内存信息
     */
    private void getMemInfo(int positon) {
        initThread();
        getMemThread.exit = true;
        getMemThread.commandItem = positon;
        Log.d(TAG,"exit ="+getMemThread.exit);
        getMemThread.start();
    }

    class GetMemThread extends Thread {
        public volatile boolean exit = false;
        public int commandItem=-1;
        String COMMAND_MEMINFO = "cat /proc/meminfo";

        @Override
        public void run() {
            super.run();
            LineData lineData = chart.getData();
            LineDataSet dataSet =null;
            if(commandItem==0){
                dataSet = (LineDataSet) lineData.getDataSetByLabel("Slab", false);
            }else if(commandItem==1){
                dataSet = (LineDataSet) lineData.getDataSetByLabel("SUnreclaim", false);
            }
            long startMilliSeconds = System.currentTimeMillis();
            //long startSeconds = startMilliSeconds / 1000;
            Log.d(TAG, "GetMemThread run..  totalSeconds=" + (startMilliSeconds / 1000));
            while (exit) {
                String res;
                if (commandItem == 0 || commandItem==1) {
                    res = exec(COMMAND_MEMINFO);
                    //Log.d(TAG,"GetMemThread res="+res);
                    if (res != null && !res.isEmpty()) {
                        String temp = null;
                        String slab = null;
                        int value = 0;
                        if(commandItem==0){
                            temp = res.substring(res.indexOf("Slab:"), res.indexOf("SReclaimable"));
                            slab = temp.substring(temp.indexOf(":") + 1, temp.indexOf("k") - 1).trim();
                        }else if(commandItem==1){
                            temp = res.substring(res.indexOf("SUnreclaim:"), res.indexOf("KernelStack"));
                            slab = temp.substring(temp.indexOf(":") + 1, temp.indexOf("k") - 1).trim();
                        }
                        if(slab !=null && !slab.isEmpty()){
                            value= Integer.valueOf(slab);
                            Log.d(TAG, "slab =" + slab);
                        }
                        long interval = (System.currentTimeMillis() - startMilliSeconds) / 1000;
                        Log.d(TAG, "interval=" + interval);


                        if (dataSet != null && interval>0) {
                            dataSet.addEntry(new Entry((float) interval, (float) value));
                            lineData.notifyDataChanged();
                        }

                        Message message = handler.obtainMessage();
                        message.what = 1;
                        message.obj = interval;
                        handler.sendMessage(message);
                    }
                }

                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG,"Thread end");
        }
    }

    private String exec(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();
            process.waitFor();
            return output.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                long interval = (long) msg.obj;
                Log.d(TAG, "Hander interval=" + interval);//求出现在的秒
                long currentSecond = interval % 60;//求出现在的分
                long totalMinutes = interval / 60;
                long currentMinute = totalMinutes % 60;//求出现在的小时
                long totalHour = totalMinutes / 60;
                Log.i("md", "小时：" + totalHour + " 分钟： " + currentMinute + " 秒 ：" + currentSecond);
                String elapsed = totalHour + " hour " + currentMinute + " min " + currentSecond + " sec ";
                tv_elapsed.setText(elapsed);
                chart.notifyDataSetChanged();
                chart.invalidate();
            }
        }
    };
}
