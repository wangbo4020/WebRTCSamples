package org.appspot.apprtc;

import android.app.Application;

import com.github.moduth.blockcanary.BlockCanary;
import com.github.moduth.blockcanary.BlockCanaryContext;
import com.squareup.leakcanary.LeakCanary;

/**
 * Created by Dylan on 2017/10/31.
 */

public class App extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		if (BuildConfig.DEBUG) {
			BlockCanary.install(this, new BlockCanaryContext() {
				@Override
				public int provideBlockThreshold() {
					return 500;
				}

				@Override
				public boolean displayNotification() {
					return true;
				}
			}).start();

			LeakCanary.install(this);
		}

	}
}
