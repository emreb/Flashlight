package com.teneke.flashlight;

import java.lang.reflect.Field;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;


public class Flashlight extends Activity {
	public static final int PICK = Menu.FIRST +1;
	public static final int STROBE_GROUP = Menu.FIRST + 1;
	public static final int STROBE_ON = Menu.FIRST + 3;
	public static final int STROBE_OFF = Menu.FIRST + 4;
	private static float oldBrightness = 0f;
	private static PowerManager pm = null;
	private static PowerManager.WakeLock wl = null;
	private static boolean strobeSwitch = false;
	private static int currentColor = -1;
	GoogleAnalyticsTracker tracker;
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tracker = GoogleAnalyticsTracker.getInstance();
        String analyticsId = this.getResources().getString(R.string.analytics_id);
        tracker.start(analyticsId, 10, this);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        try{
        	oldBrightness = this.getCurrentBrightness();
        }
        catch (Exception e){
        	
        }

        pm = (PowerManager) getSystemService(POWER_SERVICE);
    	wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
    	
    	//setContentView(R.layout.white);
    	setBackgroundColor(Color.WHITE);
    	Toast.makeText(this,R.string.check_menu,3).show();
    	// Request for the progress bar to be shown in the title
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        //setContentView(R.layout.progressbar_2);

        // Make sure the progress bar is visible
        setProgressBarVisibility(true);
    	
    }
    public boolean onTouchEvent(MotionEvent event){
    	if (event.getAction() == MotionEvent.ACTION_UP)
    		Toast.makeText(this,R.string.check_menu,5).show();
    	return true;
    }
//    private void setBrightness(int b)
//    {
//    	System.out.println("Setting brightness to full");
//    	try {
//    		Class IHardwareService = Class.forName("android.os.IHardwareService");
//    		Class ServiceManager = Class.forName("android.os.ServiceManager");
//    		Class IBinder = Class.forName("android.os.IBinder");
//    		Object hardware = null; 
//    		Method getService = ServiceManager.getMethod("getService",String.class);
//    		Class[] Stub = IHardwareService.getClasses();
//    		Method asInterface = Stub[0].getMethod("asInterface", IBinder);
//    		asInterface.setAccessible(true);
//    		getService.setAccessible(true);
//    		hardware = asInterface.invoke(hardware,getService.invoke(null,"hardware"));
//            Method setScreenBacklight = IHardwareService.getMethod("setBacklights",int.class);
//            if (hardware != null) {
//                setScreenBacklight.setAccessible(true);
//                setScreenBacklight.invoke(hardware,b);
//            }
//
//        } catch (Exception e) {
//        	System.out.println(e);
//        }
//    }
    private static final String TAG = "ChangeBrightness";
    
    private void setBrightness(float f) {

        int v = (int) (255 * f);
        if (v < 10) {
            // never set backlight too dark
            v = 10;
            f = v / 255.f;
        }

        Settings.System.putInt(this.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS,
                v);

        int sdk = Integer.parseInt(Build.VERSION.SDK);
        if (sdk >= 3) {

            try {
                Window win = getWindow();
                LayoutParams attr = win.getAttributes();
                Field field = attr.getClass().getField("screenBrightness");
                field.setFloat(attr, f);

                win.setAttributes(attr);

                Log.i(TAG, String.format("Changed brightness to %.2f [SDK 3+]", f));

            } catch (Throwable t) {
                Log.e(TAG, String.format("Failed to set brightness to %.2f [SDK 3+]", f), t);
            }

        } 
    }

    /**
     * Returns screen brightness in range 0..1%.
     */
    public float getCurrentBrightness() {
        try {
            int v = Settings.System.getInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS);

            return v / 255.0f;
        } catch (SettingNotFoundException e) {
            // If not found, return default
            return 0.75f;
        }
    }

    public void onResume(){
    	super.onResume();
    	setBrightness(1);
    	setBackgroundColor(Color.WHITE);
    	strobeSwitch = false;
    	wl.acquire(); 
    	//ServerLoggerFactory.log(this, "Launch", "true");
    	tracker.trackPageView("/HomeScreen");
    }
    
    public void onPause(){
    	super.onPause();
    	setBrightness(oldBrightness);
    	wl.release(); 
    	strobeStop();
    	
    	
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO Auto-generated method stub
    	boolean result = super.onCreateOptionsMenu(menu);
        return result;
    }
    
    
    public boolean onPrepareOptionsMenu(Menu menu) 
    {
    	menu.removeGroup(0);
    	
    	menu.add(0, PICK, 1, R.string.menu_pick);
    	
    	menu.removeGroup(STROBE_GROUP);
    	if (!strobeSwitch){
    		menu.add(STROBE_GROUP, STROBE_ON, 1, R.string.menu_strobe_on);
    	}
    	else{
    		menu.add(STROBE_GROUP, STROBE_OFF, 1, R.string.menu_strobe_off);
    	}	
		
    	return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
       
    	switch (item.getItemId()) {
        case PICK:
        	pickColor();
        	//ServerLoggerFactory.log(this, "Color", "view");
        	tracker.trackPageView("/ColorPick");
        	
        	return true;
        case STROBE_ON:
        	strobeStart();
        	//ServerLoggerFactory.log(this, "Strobe", "on");
        	tracker.trackPageView("/StrobeOn");
        	
        	return true;
        case STROBE_OFF:
        	strobeStop();
        	//ServerLoggerFactory.log(this, "Strobe", "off");
        	tracker.trackPageView("/StrobeOff");
        	
        	return true;
        
        }
        return super.onOptionsItemSelected(item);
    }
   Runnable r = new Runnable(){
	   public void run(){
		   strobeBackgroundColor();
	   }
   };
   private Thread strobeThread = null;

    private void strobeStart()
    {
    	strobeSwitch = true;
    	
    	strobeThread = new Thread(){
    		public void run() {
    			while(true){
    				try{
    					sleep(100);
    				}
    				catch (Exception e){
    				
    				}
    				if (strobeSwitch) {
    				   runOnUiThread(r);
    				} else {
    				   return;
    				}
    			}
    		}
    	};
    	
		strobeThread.start();
    }

    private void strobeStop()
    {
    	strobeSwitch = false;
    	setBackgroundColor(currentColor,false);
    	try {
    		if (strobeThread != null)
    			strobeThread.join(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }
    
    static boolean b = false;
    
    private void strobeBackgroundColor()
    {
    	if (b){
    		if(currentColor == Color.WHITE)
    			setBackgroundColor(Color.BLACK,false);
    		else
    			setBackgroundColor(Color.WHITE,false);
    	}
    	else
    		setBackgroundColor(currentColor, false);
    	b=!b;
    }
    
    
    private void setBackgroundColor(int color){
    	setBackgroundColor(color,true);
    }
    
    private void setBackgroundColor(int color,boolean remember) {
        /*ImageView i = new ImageView(this);
        i.setBackgroundColor(color);
        i.setScaleType(ImageView.ScaleType.FIT_CENTER);
        i.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT));
        setContentView(i);*/
    	
    	getWindow().setBackgroundDrawable(new ColorDrawable(color));
    	
        if (remember)
        	currentColor = color;
    }
    
    /*
    public boolean onTouchEvent(MotionEvent e)
    {
    	pickColor();
    	return false;
    }*/
    
    
    public class MyOnColorChangedListener implements ColorPickerDialog.OnColorChangedListener{
        public void colorChanged(int color) {
        	setBackgroundColor(color);
        }
    }
    
    MyOnColorChangedListener listener = new MyOnColorChangedListener();
    
    
    private void pickColor()
    {
    	ColorPickerDialog d = new ColorPickerDialog(this,(ColorPickerDialog.OnColorChangedListener)listener,Color.WHITE);
    	d.show();
    }
    
    @Override
    protected void onDestroy() {
      super.onDestroy();
      // Stop the tracker when it is no longer needed.
      tracker.stop();
    }
    
}