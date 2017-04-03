package com.example.coolweather.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.coolweather.db.CoolWeatherDB;

public class Utility {
	
	/**
	 * �������������ص�JSON���� �����������������ݴ洢������
	 */
	public static void handleWeatherResponse(Context context, String response) {
		try {
			JSONObject jsonObject = new JSONObject(response);
			String status = jsonObject.getString("status");
			if (status.equals("OK")) {
				JSONArray weather_init = jsonObject.getJSONArray("weather");
				//ȥ��������
				JSONObject weather = weather_init.getJSONObject(0);
				String cityName = weather.getString("city_name");
				String countyCode = weather.getString("city_id");
				String lastUpdate_init = weather.getString("last_update");
				//��ȡʱ��
				String lastUpdate = lastUpdate_init.substring(11, 16);
				JSONObject now = weather.getJSONObject("now");
				String weather_desp = now.getString("text");
				String temperature = now.getString("temperature")+"��C";
				saveWeatherInfo(context, cityName, countyCode, lastUpdate, weather_desp, temperature);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * �����������ص�����������Ϣ�洢�� SharedPreferences�ļ���
	 */
	public static void saveWeatherInfo(Context context, String cityName, String countyCode,
			String lastUpdate, String weather_desp, String temperature) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy��M��d��",
				Locale.CHINA);
		SharedPreferences.Editor editor = PreferenceManager.
				getDefaultSharedPreferences(context).edit();
		editor.putBoolean("city_selected", true);
		editor.putString("city_name", cityName);
		editor.putString("county_code", countyCode);
		editor.putString("last_update", lastUpdate);
		editor.putString("weather_desp", weather_desp);
		editor.putString("temperature", temperature);
		editor.putString("current_date", sdf.format(new Date()));
		editor.commit();
	}
}
