/*
 * This file is part of MAME4droid.
 *
 * Copyright (C) 2015 David Valdeita (Seleuco)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Linking MAME4droid statically or dynamically with other modules is
 * making a combined work based on MAME4droid. Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * In addition, as a special exception, the copyright holders of MAME4droid
 * give you permission to combine MAME4droid with free software programs
 * or libraries that are released under the GNU LGPL and with code included
 * in the standard release of MAME under the MAME License (or modified
 * versions of such code, with unchanged license). You may copy and
 * distribute such a system following the terms of the GNU GPL for MAME4droid
 * and the licenses of the other code concerned, provided that you include
 * the source code of that other code when and as the GNU GPL requires
 * distribution of source code.
 *
 * Note that people who make modified versions of MAME4idroid are not
 * obligated to grant this special exception for their modified versions; it
 * is their choice whether to do so. The GNU General Public License
 * gives permission to release a modified version without this exception;
 * this exception also makes it possible to release a modified version
 * which carries forward this exception.
 *
 * MAME4droid is dual-licensed: Alternatively, you can license MAME4droid
 * under a MAME license, as set out in http://mamedev.org/
 */

package com.projectgg.cninja;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.gms.ads.MobileAds;
import com.projectgg.cninja.Emulator;
import com.projectgg.cninja.helpers.DialogHelper;
import com.projectgg.cninja.helpers.MainHelper;
import com.projectgg.cninja.helpers.MenuHelper;
import com.projectgg.cninja.helpers.PrefsHelper;
import com.projectgg.cninja.input.ControlCustomizer;
import com.projectgg.cninja.input.InputHandler;
import com.projectgg.cninja.input.InputHandlerExt;
import com.projectgg.cninja.input.InputHandlerFactory;
import com.projectgg.cninja.prefs.UserPreferences;
import com.projectgg.cninja.views.IEmuView;
import com.projectgg.cninja.views.InputView;
import com.projectgg.cninja.R;


import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.widget.Toast;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Vector;

import static com.projectgg.cninja.input.IController.COIN_VALUE;


final class NotificationHelper
{
        private static NotificationManager notificationManager = null;
 
		public static void addNotification(Context ctx, String onShow, String title, String message)
        {
                if(notificationManager == null)
                        notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                int icon = R.drawable.icon_sb; // TODO: don't hard-code
                long when = System.currentTimeMillis();

				Intent notificationIntent = new Intent(ctx, MAME4droid.class);
				PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, new Intent(ctx, MAME4droid.class), PendingIntent.FLAG_UPDATE_CURRENT);
				//Notification notification = new Notification(icon, /*onShow*/null, when);
				Notification notification = new Notification.Builder(ctx).setContentTitle(title)
																		.setContentText(message)
																		.setSmallIcon(icon)
																		.setWhen(when)
																		.setContentIntent(pendingIntent)
																		.setDefaults(Notification.FLAG_ONGOING_EVENT | Notification.FLAG_AUTO_CANCEL)
																		.build();

                notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_AUTO_CANCEL;
                PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, notificationIntent, 0);

        //        notification.setLatestEventInfo(ctx, contentTitle, contentText, contentIntent);
                notificationManager.notify(1, notification);
        }
       
        public static void removeNotification()
        {
                if(notificationManager != null)
                        notificationManager.cancel(1);
        }
}

public class MAME4droid extends Activity {

	protected View emuView = null;

	protected InputView inputView = null;
		
	protected MainHelper mainHelper = null;
	protected MenuHelper menuHelper = null;
	protected PrefsHelper prefsHelper = null;
	protected DialogHelper dialogHelper = null;
	
	protected InputHandler inputHandler = null;
	
	protected FileExplorer fileExplore = null;
	
	protected NetPlay netPlay = null;

	public static final boolean BANNER_TEST_DEVICE = true;
	private String full_unit_id_start = "ca-app-pub-3903577701358811/4003270138";
	private final static String TAG = "cninja-Activity";
	private AdRequest mAdrequest_startgame;
	private InterstitialAd mFullbannerAd;
	private final static int HANDLER_SHOW_FULLAD_STARTGAME  = 100;
	private static Handler handler;

    private Queue<Integer> coin_queue;


	public NetPlay getNetPlay() {
		return netPlay;
	}

	public FileExplorer getFileExplore() {
		return fileExplore;
	}

	public MenuHelper getMenuHelper() {
		return menuHelper;
	}
    	
    public PrefsHelper getPrefsHelper() {
		return prefsHelper;
	}
    
    public MainHelper getMainHelper() {
		return mainHelper;
	}
    
    public DialogHelper getDialogHelper() {
		return dialogHelper;
	}
    
	public View getEmuView() {
		return emuView;
	}
	
	public InputView getInputView() {
		return inputView;
	}
	
    public InputHandler getInputHandler() {
		return inputHandler;
	}
	public void fixOrientation(){
		//KKY force orientation to landscape . (true = landscape only. false = horizontal only)
		boolean landscape_orientation = true;

		if( landscape_orientation ) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
			/* [CNINJA-007] unexpected Orientation change (to Vertical mode) when resume application
			//LG폰 세로버그 픽스를 위한 주석처리 	 */
			//setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
		}
	}

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		Log.d("EMULATOR", "onCreate "+this);
		System.out.println("onCreate intent:"+getIntent().getAction());

		//sunghook freeze orientation.
		fixOrientation();

		coin_queue = new LinkedList<>();


		overridePendingTransition(0, 0);
		getWindow().setWindowAnimations(0);
		
		prefsHelper = new PrefsHelper(this);

        dialogHelper  = new DialogHelper(this);
        
        mainHelper = new MainHelper(this);
                             
        fileExplore = new FileExplorer(this);
        
        netPlay = new NetPlay(this);

        menuHelper = new MenuHelper(this);
                
        inputHandler = InputHandlerFactory.createInputHandler(this);
        
        mainHelper.detectDevice();
        
        inflateViews();
        
        Emulator.setMAME4droid(this);  
        
        mainHelper.updateMAME4droid();
        
        //mainHelper.checkNewViewIntent(this.getIntent());
               
        if(!Emulator.isEmulating())
        {
			if(prefsHelper.getROMsDIR()==null)
			{	            
				if(DialogHelper.savedDialog==DialogHelper.DIALOG_NONE) {
					//sunghook
					/*showDialog(DialogHelper.DIALOG_ROMs_DIR);*/
					if(getMainHelper().ensureInstallationDIR(getMainHelper().getInstallationDIR()))
					{
						getPrefsHelper().setROMsDIR("");
						runMAME4droid();
					}
				}
			}
			else
			{
				boolean res = getMainHelper().ensureInstallationDIR(mainHelper.getInstallationDIR());
				if(res==false)
				{
					this.getPrefsHelper().setInstallationDIR(this.getPrefsHelper().getOldInstallationDIR());
				}
				else
				{
				    runMAME4droid();
				}
			}
        }

		MobileAds.initialize(this,  full_unit_id_start);

		try {
			mFullbannerAd = new InterstitialAd(this);
			mFullbannerAd.setAdUnitId(full_unit_id_start);
			mFullbannerAd.setAdListener(new AdListener(){
				@Override
				public void onAdLoaded() {
					Log.d(TAG, "HANDLER_SHOW_FULLAD_STARTGAME onAdLoaded");
					super.onAdLoaded();
				}

				@Override
				public void onAdClosed() {
					Log.d(TAG, "HANDLER_SHOW_FULLAD_STARTGAME onAdClosed");
					super.onAdClosed();
					load_fullAD();

					Emulator.resume();

					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					int newtouches = COIN_VALUE;
					int devId = dequeue_coin();
					Emulator.setPadData(devId, newtouches);

				}

			});

			load_fullAD();
		} catch (Throwable e){
        	e.printStackTrace();
		}
		handler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
					case HANDLER_SHOW_FULLAD_STARTGAME:

						Log.d(TAG, "case HANDLER_SHOW_FULLAD_STARTGAME start");
						if(mFullbannerAd.isLoaded()==true){
							Emulator.pause();
							mFullbannerAd.show();
						} else {
							Log.d(TAG, "Ad skip as AD download is not ready ");
							int ret = dequeue_coin(); //remove coin inserted when Network is not ready.
							Context context = getApplicationContext();
							CharSequence text = "Ad download failed !";
							int duration = Toast.LENGTH_SHORT;
							Toast toast = Toast.makeText(context, text, duration);
							toast.setDuration(1000);
							toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
							toast.show();
						}
						break;


				}
				fixOrientation();
			}

		};

	}


    public void inflateViews(){
    	inputHandler.unsetInputListeners();
    	
        Emulator.setPortraitFull(getPrefsHelper().isPortraitFullscreen());
        
        boolean full = false;
		if(prefsHelper.isPortraitFullscreen() && mainHelper.getscrOrientation() == Configuration.ORIENTATION_PORTRAIT)
		{
			setContentView(R.layout.main_fullscreen);
			full = true;
		}
		else 
		{
            setContentView(R.layout.main);
		}        
                
        FrameLayout fl = (FrameLayout)this.findViewById(R.id.EmulatorFrame);
                
        Emulator.setVideoRenderMode(getPrefsHelper().getVideoRenderMode());
        
        if(prefsHelper.getVideoRenderMode()==PrefsHelper.PREF_RENDER_SW)
        {
        	/*
        	if(emuView != null && (emuView instanceof EmulatorViewSW))
        	{
        		EmulatorViewSW s = (EmulatorViewSW)emuView;
        		s.getHolder().removeCallback(s);
        	}*/
        		
        	this.getLayoutInflater().inflate(R.layout.emuview_sw, fl);
        	emuView = this.findViewById(R.id.EmulatorViewSW);        
        }
        else 
        { 
        	if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN  && prefsHelper.getNavBarMode()!=PrefsHelper.PREF_NAVBAR_VISIBLE)
        	    this.getLayoutInflater().inflate(R.layout.emuview_gl_ext, fl);
        	else
        		this.getLayoutInflater().inflate(R.layout.emuview_gl, fl);
    		
        	emuView = this.findViewById(R.id.EmulatorViewGL);        	
        }
        
        if(full && prefsHelper.isPortraitTouchController())
        {
        	FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams )emuView.getLayoutParams();
        	lp.gravity =  Gravity.TOP | Gravity.CENTER;
        }
                       
        inputView = (InputView) this.findViewById(R.id.InputView);
                
        ((IEmuView)emuView).setMAME4droid(this);

        inputView.setMAME4droid(this);
                          
        View frame = this.findViewById(R.id.EmulatorFrame);
	    frame.setOnTouchListener(inputHandler);    
	   
                
        inputHandler.setInputListeners();   	
    }
        
    public void runMAME4droid(){  	
    	    	
	    getMainHelper().copyFiles();
	    getMainHelper().removeFiles();
	    	    
    	Emulator.emulate(mainHelper.getLibDir(),mainHelper.getInstallationDIR());    	
    }
     
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
				
		overridePendingTransition(0, 0);
		
		inflateViews();

		getMainHelper().updateMAME4droid();
		
		overridePendingTransition(0, 0);
	}

	//MENU STUFF
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {		
		
		if(menuHelper!=null)
		{
		   if(menuHelper.createOptionsMenu(menu))return true;
		}
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if(menuHelper!=null)
		{	
		   if(menuHelper.prepareOptionsMenu(menu)) return true;
		}   
		return super.onPrepareOptionsMenu(menu); 
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(menuHelper!=null)
		{
		   if(menuHelper.optionsItemSelected(item))
			   return true;
		}
		return super.onOptionsItemSelected(item);
	}

	//ACTIVITY
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(mainHelper!=null)
		   mainHelper.activityResult(requestCode, resultCode, data);
	}
	
	//LIVE CYCLE
	@Override
	protected void onResume() {
		Log.d("EMULATOR", "onResume "+this);
		//sunghook
		fixOrientation();
		super.onResume();

				
		if(prefsHelper!=null)
		   prefsHelper.resume();
				
		if(DialogHelper.savedDialog!=-1)
			showDialog(DialogHelper.savedDialog);
		else if(!ControlCustomizer.isEnabled())
		  Emulator.resume();
		
		if(inputHandler!= null)
		{
			if(inputHandler.getTiltSensor()!=null)
			   inputHandler.getTiltSensor().enable();
			inputHandler.resume();
		}
		
		NotificationHelper.removeNotification();
		//System.out.println("OnResume");		 
	}
	
	@Override
	protected void onPause() {
		Log.d("EMULATOR", "onPause "+this);
		super.onPause();
		if(prefsHelper!=null)
		   prefsHelper.pause();
		if(!ControlCustomizer.isEnabled())		
		   Emulator.pause();
		if(inputHandler!= null)
		{
			if(inputHandler.getTiltSensor()!=null)
			   inputHandler.getTiltSensor().disable();
		}	
		
		if(dialogHelper!=null)
		{
			dialogHelper.removeDialogs();
		}
		
		if(prefsHelper.isNotifyWhenSuspend()) 
		  NotificationHelper.addNotification(getApplicationContext(), "MAME4droid was suspended!", "MAME4droid was suspended", "Press to return to MAME4droid");
		
		//System.out.println("OnPause");
	}
	
	@Override
	protected void onStart() {
		Log.d("EMULATOR", "onStart "+this);
		super.onStart();
		try{InputHandlerExt.resetAutodetected();}catch(Error e){};		
		//System.out.println("OnStart");
	}

	@Override
	protected void onStop() {
		Log.d("EMULATOR", "onStop "+this);
		super.onStop();
		//System.out.println("OnStop");
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.d("EMULATOR", "onNewIntent "+this);
		System.out.println("onNewIntent action:"+intent.getAction() );
		mainHelper.checkNewViewIntent(intent);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d("EMULATOR", "onDestroy "+this);
				
        View frame = this.findViewById(R.id.EmulatorFrame);
	    if(frame!=null)
           frame.setOnTouchListener(null); 
	    
		if(inputHandler!= null)
		{
		   inputHandler.unsetInputListeners();
		   
			if(inputHandler.getTiltSensor()!=null)
				   inputHandler.getTiltSensor().disable();
		}
			
        if(emuView!=null)
		   ((IEmuView)emuView).setMAME4droid(null);

        /*
        if(inputView!=null)
           inputView.setMAME4droid(null);
        
        if(filterView!=null)
           filterView.setMAME4droid(null);
                       
        prefsHelper = null;
        
        dialogHelper = null;
        
        mainHelper = null;
        
        fileExplore = null;
        
        menuHelper = null;
        
        inputHandler = null;
        
        inputView = null;
        
        emuView = null;
        
        filterView = null; */     	    
	}	
		

	//Dialog Stuff
	@Override
	protected Dialog onCreateDialog(int id) {

		if(dialogHelper!=null)
		{	
			Dialog d = dialogHelper.createDialog(id);
			if(d!=null)return d;
		}
		return super.onCreateDialog(id);		
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		if(dialogHelper!=null)
		   dialogHelper.prepareDialog(id, dialog);
	} 
	
	@Override
	public boolean dispatchGenericMotionEvent(MotionEvent event)
	{
		if(inputHandler!=null)
		   return inputHandler.genericMotion(event);
		return false;
	}


	public void load_fullAD(){

		AdRequest.Builder adbuilder_full = new AdRequest.Builder();


		if (BANNER_TEST_DEVICE) {

			String android_id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
			String deviceId = md5(android_id).toUpperCase();

			//deliLog.d("EMULATOR", "my deviceId " + deviceId);

			adbuilder_full.addTestDevice(deviceId);
		}

		mAdrequest_startgame = adbuilder_full.build();
		mFullbannerAd.loadAd(mAdrequest_startgame);

	}
	public void show_fullAD_StartGame(){



		load_fullAD();

		Message msg = new Message();
		msg.what = HANDLER_SHOW_FULLAD_STARTGAME;
		handler.sendMessage(msg);


		//deli
		Log.d("admob", "HANDLER_SHOW_FULLAD_STARTGAME show_fullAD_StartGame sendMessage completed");
	}

	public static final String md5(final String s) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			digest.update(s.getBytes());
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++) {
				String h = Integer.toHexString(0xFF & messageDigest[i]);
				while (h.length() < 2)
					h = "0" + h;
				hexString.append(h);
			}
			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {
			Log.e("ADD TEST DEVICE", "ADD TEST DEVICE ERROR!!");
		}
		return "";
	}

	public void enqueue_coin(int deviceId){
		coin_queue.add(deviceId);
	}
	public int dequeue_coin(){
	    return coin_queue.remove();
	}


}
