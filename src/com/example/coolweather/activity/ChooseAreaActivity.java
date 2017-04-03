package com.example.coolweather.activity;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.example.coolweather.db.CoolWeatherDB;
import com.example.coolweather.model.City;
import com.example.coolweather.model.County;
import com.example.coolweather.util.HttpCallbackListener;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;
import com.example.coolweather.R;

import android.R.integer;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaMuxer.OutputFormat;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ChooseAreaActivity extends Activity {

	public static final int LEVEL_CITY = 1;	
	public static final int LEVEL_COUNTY = 2;
	
	private ProgressDialog progressDialog;	
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private CoolWeatherDB coolWeatherDB;
	private List<String> dataList = new ArrayList<String>();

	/**
	 * 市列表
	 */
	private List<City> cityList;
	/**
	 * 县列表
	 */
	private List<County> countyList;
	/**
	 * 选中的城市
	 */
	private City selectedCity;
	/**
	 * 当前选中的级别
	 */
	private int currentLevel;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SharedPreferences prefs = PreferenceManager.
				getDefaultSharedPreferences(this);
		if (prefs.getBoolean("city_selected", false)) {
			Intent intent = new Intent(this, WeatherActivity.class);
			startActivity(intent);
			finish();
			return;
		}
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.choose_area);
		listView = (ListView) findViewById(R.id.list_view);
		titleText = (TextView) findViewById(R.id.title_text);
		adapter = new ArrayAdapter<String>(this, android.R.layout.
				simple_expandable_list_item_1, dataList);
		listView.setAdapter(adapter);
		coolWeatherDB = CoolWeatherDB.getInstance(this);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View view,
					int index, long arg3) {
				if (currentLevel == LEVEL_CITY) {
					selectedCity = cityList.get(index);
					queryCounties();
				} else if (currentLevel == LEVEL_COUNTY) {
					String countyCode = countyList.get(index).getCountyCode();
					Intent intent = new Intent(ChooseAreaActivity.this,
					WeatherActivity.class);
					intent.putExtra("county_code", countyCode);
					startActivity(intent);
					finish();
				}
			}
		});
		//加载市级数据
		queryCities();
	}
	
	/**
	 * 查询选中省内所有的市，从数据库查询。
	 * @throws IOException 
	 */
	private void queryCities(){
		cityList = coolWeatherDB.loadCities();
		if (cityList.size() > 0){
			dataList.clear();
			for (City city : cityList) {
				dataList.add(city.getCityName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText("城 市");
			currentLevel = LEVEL_CITY;
		} else {
			readCity();
		}
	}
	
	/**
	 * 查询选中市内所有的县，优先从数据库查询，如果没有查询到再读入文本文件。
	 * @throws IOException 
	 */
	private void queryCounties(){
		countyList = coolWeatherDB.loadCounties(selectedCity.getCityCode());
		if (countyList.size() > 0) {
			dataList.clear();		
			for (County county : countyList) {
				dataList.add(county.getCountyName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			currentLevel = LEVEL_COUNTY;
		} else {
			readCounty(selectedCity.getCityCode());
		}
	}
	
	/**
	 * 从文本文件读取城市。
	 */
	private void readCity(){
		try {
			InputStream input = getResources().getAssets().open("city.dat");
			int length = input.available();
			byte[] buffer = new byte[length];
			input.read(buffer);
			input.close();
			int j = 0;
			/*
			 * buffer[0-1]是解释utf的
			 * buffer[2-7]是城市名，两个汉字，每个汉字三个字节
			 * buffer[8-9]是解释utf的
			 * buffer[10-12]是城市代号，一个数字一个字节
			 */
			for(int i=0;i<buffer.length;){
				//光标过滤解释utf的字节
				i += 2;
				byte[] cityBuffer = new byte[]{buffer[i],buffer[i+1],buffer[i+2],
						buffer[i+3],buffer[i+4],buffer[i+5]};
				String cityString = new String(cityBuffer, "utf8");
				//跳过已经读了的两个汉字共6个字节以及解释utf的两个字节
				i += 8;
				byte[] cityCodeBuffer = new byte[]{buffer[i],buffer[i+1],buffer[i+2]};
				String cityCodeString = new String(cityCodeBuffer, "utf8");
				//跳过已经读了的三个数字
				i += 3;
				
				City city = new City();
				city.setId(++j);
				city.setCityName(cityString);
				city.setCityCode(Integer.parseInt(cityCodeString));
				coolWeatherDB.saveCity(city);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		queryCities();
	}
	
	public void readCounty(int cityCode){
		try{
			InputStream input = getResources().getAssets().open("county.dat");
			int length = input.available();
			byte[] buffer = new byte[length];
			input.read(buffer);
			input.close();
			int j = 0;
			for(int i=0;i<buffer.length;){
				i += 2;
				byte[] countyBuffer = new byte[]{buffer[i],buffer[i+1],buffer[i+2],
						buffer[i+3],buffer[i+4],buffer[i+5]};
				String countyString = new String(countyBuffer, "utf8");

				i += 8;
				byte[] countyCodeBuffer = new byte[]{buffer[i],buffer[i+1],buffer[i+2],
						buffer[i+3],buffer[i+4],buffer[i+5],buffer[i+6],buffer[i+7],
						buffer[i+8],buffer[i+9]};
				String countyCodeString = new String(countyCodeBuffer, "utf8");
				
				i += 12;
				byte[] cityCodeBuffer = new byte[]{buffer[i],buffer[i+1],buffer[i+2]};
				String cityCodeString = new String(cityCodeBuffer, "utf8");
				int cityCodeInt = Integer.parseInt(cityCodeString);
				i += 3;
				
				if (cityCodeInt == cityCode) {
					County county = new County();
					county.setId(++j);
					county.setCountyName(countyString);
					county.setCountyCode(countyCodeString);
					county.setCityCode(cityCode);
					coolWeatherDB.saveCounty(county);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		queryCounties();
	}
	
	/**
	 * 显示进度对话框
	 */
	private void showProgressDialog() {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("正在加载...");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}
	
	/**
	 * 关闭进度对话框
	 */
	private void closeProgressDialog() {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
	}
	
	/**
	 * 捕获Back按键，根据当前的级别来判断，此时应该返回市列表、省列表、还是直接退出。
	 */
	@Override
	public void onBackPressed() {
		if (currentLevel == LEVEL_COUNTY) {
			queryCities();
		} else {
			finish();
		}
	}
	
}
