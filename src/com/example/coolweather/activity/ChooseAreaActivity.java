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
	 * ���б�
	 */
	private List<City> cityList;
	/**
	 * ���б�
	 */
	private List<County> countyList;
	/**
	 * ѡ�еĳ���
	 */
	private City selectedCity;
	/**
	 * ��ǰѡ�еļ���
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
		//�����м�����
		queryCities();
	}
	
	/**
	 * ��ѯѡ��ʡ�����е��У������ݿ��ѯ��
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
			titleText.setText("�� ��");
			currentLevel = LEVEL_CITY;
		} else {
			readCity();
		}
	}
	
	/**
	 * ��ѯѡ���������е��أ����ȴ����ݿ��ѯ�����û�в�ѯ���ٶ����ı��ļ���
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
	 * ���ı��ļ���ȡ���С�
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
			 * buffer[0-1]�ǽ���utf��
			 * buffer[2-7]�ǳ��������������֣�ÿ�����������ֽ�
			 * buffer[8-9]�ǽ���utf��
			 * buffer[10-12]�ǳ��д��ţ�һ������һ���ֽ�
			 */
			for(int i=0;i<buffer.length;){
				//�����˽���utf���ֽ�
				i += 2;
				byte[] cityBuffer = new byte[]{buffer[i],buffer[i+1],buffer[i+2],
						buffer[i+3],buffer[i+4],buffer[i+5]};
				String cityString = new String(cityBuffer, "utf8");
				//�����Ѿ����˵��������ֹ�6���ֽ��Լ�����utf�������ֽ�
				i += 8;
				byte[] cityCodeBuffer = new byte[]{buffer[i],buffer[i+1],buffer[i+2]};
				String cityCodeString = new String(cityCodeBuffer, "utf8");
				//�����Ѿ����˵���������
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
	 * ��ʾ���ȶԻ���
	 */
	private void showProgressDialog() {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("���ڼ���...");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}
	
	/**
	 * �رս��ȶԻ���
	 */
	private void closeProgressDialog() {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
	}
	
	/**
	 * ����Back���������ݵ�ǰ�ļ������жϣ���ʱӦ�÷������б�ʡ�б�����ֱ���˳���
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
