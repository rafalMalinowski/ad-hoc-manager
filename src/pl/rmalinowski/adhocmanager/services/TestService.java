package pl.rmalinowski.adhocmanager.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class TestService extends Service {
	private final IBinder mBinder = new MyBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		int i = 0;
		i++;
		return Service.START_NOT_STICKY;
	}

	public class MyBinder extends Binder {
		public TestService getService() {
			return TestService.this;
		}
	}

	public String getTest() {
		return "OK";
	}

}
