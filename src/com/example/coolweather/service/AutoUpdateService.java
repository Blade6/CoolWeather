package com.example.coolweather.service;

import com.example.coolweather.receiver.AutoUpdateReceiver;
import com.example.coolweather.util.HttpCallbackListener;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

public class AutoUpdateService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		new Thread(new Runnable() {			
			@Override
			public void run() {
				updateWeather();				
			}
		}).start();
		AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
		int anHour = 8 * 60 * 60 * 1000; //这是8小时的毫秒数
		long triggerAtTime = SystemClock.elapsedRealtime() + anHour;
		Intent i = new Intent(this, AutoUpdateReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
		manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
		return super.onStartCommand(intent, flags, startId);
	}
	
	/**
	 * 更新天气信息 
	 */
	private void updateWeather() {
		SharedPreferences prefs = PreferenceManager.
				getDefaultSharedPreferences(this);
		String countyCode = prefs.getString("county_code", "");
		String address = "http://tj.nineton.cn/Heart/index/all?city=" +
				countyCode + "&language=zh-chs";
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			@Override
			public void onFinish(final String response) {
					// 处理服务器返回的天气信息
					Utility.handleWeatherResponse(AutoUpdateService.this,
							response);
			}

			@Override
			public void onError(Exception e) {
				e.printStackTrace();				
			}
		});
	}
}
