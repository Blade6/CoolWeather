package com.example.coolweather.db;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.example.coolweather.model.City;
import com.example.coolweather.model.County;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

public class CoolWeatherDB {

	/**
	 * ���ݿ���
	 */
	public static final String DB_NAME = "cool_weather";
	
	/**
	 * ���ݿ�汾
	 */
	public static final int VERSION = 1;
	
	private static CoolWeatherDB coolWeatherDB;
	
	private SQLiteDatabase db;
	
	/**
	 * �����췽��˽�л�
	 */
	private CoolWeatherDB(Context context) {
		CoolWeatherOpenHelper dbHelper = new CoolWeatherOpenHelper(context,
				DB_NAME, null, VERSION);
		db = dbHelper.getWritableDatabase();
	}
	
	/**
	 * ��ȡCoolWeatherDB��ʵ��
	 */
	public synchronized static CoolWeatherDB getInstance(Context context) {
		if (coolWeatherDB == null) {
			coolWeatherDB = new CoolWeatherDB(context);
		}
		return coolWeatherDB;
	}
	
	/**
	* ��Cityʵ���洢�����ݿ⡣
	*/
	public void saveCity(City city) {
		if (city != null) {
		ContentValues values = new ContentValues();
		values.put("city_name", city.getCityName());
		values.put("city_code", city.getCityCode());
		db.insert("City", null, values);
		}
	}
	
	/**
	 * ���ı��ļ���ȡ���еĳ�����Ϣ
	 */
	public List<City> loadCities() {
		List<City> list = new ArrayList<City>();
		Cursor cursor = db.query("City", null, null,
				null, null, null, null);
		if (cursor.moveToFirst()) {
			do {
				City city = new City();
				city.setId(cursor.getInt(cursor.getColumnIndex("id")));
				city.setCityName(cursor.getString(cursor
						.getColumnIndex("city_name")));
				city.setCityCode(cursor.getInt(
						cursor.getColumnIndex("city_code")));
				list.add(city);
			} while (cursor.moveToNext());
		}
		return list;
	}
	
	/**
	 * ��Countyʵ���洢�����ݿ�
	 */
	public void saveCounty(County county) {
		if (county != null) {
			ContentValues values = new ContentValues();
			values.put("county_name", county.getCountyName());
			values.put("county_code", county.getcountyCode());
			values.put("city_code", county.getCityCode());
			db.insert("County", null, values);
		}
	}
	
	/**
	 * �����ݿ��ȡĳ���������е�����Ϣ
	 */
	public List<County> loadCounties(int city_code) {
		List<County> list = new ArrayList<County>();
		Cursor cursor = db.query("county", null, "city_code = ?",
				new String[]{String.valueOf(city_code)}, null, null, null);
		if (cursor.moveToFirst()) {
			do {
				County county = new County();
				county.setId(cursor.getInt(cursor.getColumnIndex("id")));
				county.setCountyName(cursor.getString(cursor.getColumnIndex("county_name")));
				county.setCountyCode(cursor.getString(cursor.getColumnIndex("county_code")));
				county.setCityCode(city_code);
				list.add(county);
			} while (cursor.moveToNext());
		}
		if (cursor != null) {
			cursor.close();
		}
		return list;
	}
	
}
