package com.example.splashanimationtest;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.ImageView;

public class MainActivity extends FragmentActivity {
    private static final String TAG = MainActivity.class.getCanonicalName();
    protected static final int CACHE_ITEM_SIZE = 20;
    private ImageView mImage;
    // long maxMemSize = 1024 * 1024 * 20;

    // todo define these in xml
    int lifetimeFrames = 39;
    int historyFrames = 61;
    int aeFrames = 26;

    String lifetimePrefix = "lifetime_splash";
    String historyPrefix = "splash00";
    String aePrefix = "splash_";

    Bitmap bitmapDecode;
    public boolean running;
    protected String mNamePrefix = lifetimePrefix;
    int mTotalFrames = lifetimeFrames;
    int frameLoadCount = 0;
    protected boolean canAnimate;

    int mFrameDelay = 2500 / mTotalFrames;
    private LruCache<Integer, BitmapDrawable> memCache;
    protected ArrayList<Integer> keyList = new ArrayList<Integer>();
    protected Runnable killRunnable = new Runnable() {

	@Override
	public void run() {
	    // after 5 seconds everyone dies!
	    running = false;
	    mImage.removeCallbacks(animRunnable);
	    memCache.evictAll();

	}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_main);
	mImage = (ImageView) findViewById(R.id.test_view);
	// DisplayMetrics metrics = getResources().getDisplayMetrics();
	// mImage.setScaleX(metrics.density);
	// mImage.setScaleY(metrics.density);

	memCache = new LruCache<Integer, BitmapDrawable>(CACHE_ITEM_SIZE) {
	    // @Override
	    // protected void entryRemoved(boolean evicted, Integer key,
	    // BitmapDrawable oldValue, BitmapDrawable newValue) {
	    // Log.d(TAG, "entry removed");
	    // super.entryRemoved(evicted, key, oldValue, newValue);
	    // // oldValue.getBitmap().recycle();
	    // }

	};

	prefetchBitmaps();

    }

    private Runnable decodeRunnable = new Runnable() {
	public void run() {
	    while (running) {
		if (memCache.size() < CACHE_ITEM_SIZE) {
		    Log.i(TAG,
			    "MainActivity.decodeRunnable.new Runnable() {...}.run(): cache size"
				    + memCache.size());
		    if (frameLoadCount < mTotalFrames) {
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inMutable = true;
			opts.inPreferQualityOverSpeed = false;
			opts.inPreferredConfig = Bitmap.Config.RGB_565;
			opts.inPurgeable = true;
			opts.inDensity = DisplayMetrics.DENSITY_MEDIUM;
			opts.inTargetDensity = DisplayMetrics.DENSITY_HIGH;
			opts.inScaled = true;
			long startTime = System.currentTimeMillis();

			/**
			 * 
			 */

			String sName = mNamePrefix + frameLoadCount;
			int resId = getResources().getIdentifier(sName,
				"drawable", getPackageName());

			try {
			    Log.i(TAG,
				    "MainActivity.decodeRunnable.new Runnable() {...}.run(): loading frame:"
					    + frameLoadCount);
			    bitmapDecode = BitmapFactory.decodeResource(
				    getResources(), resId, opts);
			    BitmapDrawable b = new BitmapDrawable(
				    getResources(), bitmapDecode);
			    memCache.put(resId, b);
			    keyList.add(resId);
			} catch (OutOfMemoryError e) {

			    Log.e(TAG, "ran out of memory on bitmap::"
				    + frameLoadCount);
			}
			Log.i(TAG,
				"MainActivity.decodeRunnable decoded bitmap in:"
					+ (System.currentTimeMillis() - startTime));

			frameLoadCount++;

		    } else {
			if (frameLoadCount == mTotalFrames) {
			    running = false;
			    mImage.postDelayed(killRunnable, 3000);
			    Log.i(TAG,
				    "MainActivity.decodeRunnable done looping!!!!");
			    return;
			}
		    }

		} else {
		    canAnimate = true;
		}
	    }
	    Log.d(TAG, "ENDED");

	}

    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
	if (event.getAction() == MotionEvent.ACTION_DOWN) {
	    if (!running) {
		prefetchBitmaps();
	    }
	}
	return super.onTouchEvent(event);
    }

    private void prefetchBitmaps() {

	frameLoadCount = 0;
	canAnimate = false;
	running = true;
	memCache.evictAll();
	keyList.clear();
	frameLoadCount = 0;
	Thread t = new Thread(decodeRunnable);
	t.start();
	mImage.removeCallbacks(killRunnable);
	mImage.removeCallbacks(animRunnable);
	mImage.postDelayed(animRunnable, mFrameDelay);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	getMenuInflater().inflate(R.menu.main, menu);
	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	int id = item.getItemId();
	Log.d(TAG, "MainActivity.onOptionsItemSelected():");
	if (mImage != null) {
	    switch (id) {
	    case R.id.action_history:
		mNamePrefix = historyPrefix;
		mTotalFrames = historyFrames;
		mFrameDelay = 2500 / mTotalFrames;
		running = false;
		prefetchBitmaps();
		break;
	    case R.id.action_ae:
		mNamePrefix = aePrefix;
		mTotalFrames = aeFrames;
		mFrameDelay = 2500 / mTotalFrames;
		running = false;
		prefetchBitmaps();
		break;
	    case R.id.action_lifetime:
		mNamePrefix = lifetimePrefix;
		mTotalFrames = lifetimeFrames;
		mFrameDelay = 2500 / mTotalFrames;
		running = false;
		prefetchBitmaps();

		break;
	    }

	}
	return true;
    }

    private Runnable animRunnable = new Runnable() {
	public void run() {
	    Log.w(TAG,
		    "MainActivity.animRunnable.run() key list:::"
			    + keyList.size() + "   canAnimate:" + canAnimate);
	    if (canAnimate) {

		if (keyList.size() > 0) {
		    Bitmap old = ((BitmapDrawable) mImage.getDrawable())
			    .getBitmap();
		    int key = keyList.remove(0);
		    mImage.setImageDrawable(memCache.remove(key));
		    if (old != null) {
			old.recycle();
		    }

		}
	    }

	    mImage.postDelayed(animRunnable, mFrameDelay);
	}
    };

}
