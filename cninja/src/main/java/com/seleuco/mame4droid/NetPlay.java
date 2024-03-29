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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/* [CNINJA-011] Netplay doesn't work */
//import org.apache.http.conn.util.InetAddressUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.projectgg.cninja.helpers.PrefsHelper;
import com.projectgg.cninja.R;


public class NetPlay {

    public final String ROOM_MULTICAST_ADDR = "239.1.5.9";
    public final int ROOM_MULTICAST_PORT = 9004;
    public static final String TAG = "NetPlay";
	public EditText input = null;
	public Spinner input2 = null;
	public NetPlay myself = null;
	public List<String> roomList = null;
	public static Handler handler = null;
	ArrayAdapter<String> spinnerArrayAdapter = null ;
	public Thread receiverThread=null;
	public boolean State_fetchingIP = false;
	final int MSG_ROOM_LIST_ADD = 10000;

	protected Dialog netplayDlg = null;

	protected ProgressDialog progressDialog = null; 
	
	private boolean canceled = false;	
	
	protected MAME4droid mm = null;
		
	public NetPlay(MAME4droid mm) {
		this.mm = mm;
		this.myself = this;
		State_fetchingIP = false;
		handler = new Handler() {
			public void handleMessage(Message msg){
				if(msg.what == MSG_ROOM_LIST_ADD){
					Log.d(TAG, "sunghook enter handleMessage msg.what=MSG_ROOM_LIST_ADD ");
			        String scanedRoomIP = msg.obj.toString();
			        if ( myself.roomList.contains(scanedRoomIP) == false ) {
						myself.spinnerArrayAdapter.add(scanedRoomIP);
						Log.d(TAG, "sunghook debug new IP will be added to spinner !!!");
					} else {
						myself.spinnerArrayAdapter.add(scanedRoomIP);
						Log.d(TAG, "sunghook debug duplicated IP !!! ");
					}
				}
			}
		};
	}
	
	DialogInterface.OnCancelListener dialogCancelListener = new DialogInterface.OnCancelListener() { 
		public void onCancel(DialogInterface dialog) { 									
			Emulator.resume();					
		} 
	};	
	
	protected void prepareButtons(){
		
		final Button startButton = (Button) netplayDlg.findViewById(R.id.StartGameBtn);
		final Button joinButton = (Button) netplayDlg.findViewById(R.id.JoinPeerGameBtn);
		final Button disconnectButton = (Button) netplayDlg.findViewById(R.id.DisconnectBtn);
		
		if(Emulator.getValue(Emulator.NETPLAY_HAS_CONNECTION)==1)
		{
			startButton.setEnabled(false);
			joinButton.setEnabled(false);
			disconnectButton.setEnabled(true);
		}
		else
		{
			startButton.setEnabled(true);
			joinButton.setEnabled(true);
			disconnectButton.setEnabled(false);		
		}		
		
		String name = Emulator.getValueStr(Emulator.GAME_SELECTED);
		if(name!=null && name.length()!=0)
		{
		   startButton.setText("Create Game");
		}
		else
		{
			startButton.setText("Start game");
			startButton.setEnabled(false);
		}
	}

	public void addRoomSuggestions(String roomIp){
	    Log.d(TAG, "sunghook enter addRoomSuggestions() ");
		Message msg = handler.obtainMessage(MSG_ROOM_LIST_ADD, roomIp);
		handler.sendMessage(msg);
	}
	public void createDialog() {
		
		netplayDlg = new Dialog(mm);

		netplayDlg.setContentView(R.layout.netplayview);
		netplayDlg.setTitle("Peer-To-Peer Netplay");
		netplayDlg.setCancelable(true);
		netplayDlg.setOnCancelListener(dialogCancelListener);

		final Button startButton = (Button) netplayDlg.findViewById(R.id.StartGameBtn);
		startButton.setOnClickListener(createGameClick);
		
		final Button joinButton = (Button) netplayDlg.findViewById(R.id.JoinPeerGameBtn);
		joinButton.setOnClickListener(joinGameClick);
		
		final Button disconnectButton = (Button) netplayDlg.findViewById(R.id.DisconnectBtn);
		disconnectButton.setOnClickListener(disconnectGameClick);
		
		prepareButtons();
		
		netplayDlg.show();
		
		/*
		final SharedPreferences sp = mm.getPrefsHelper().getSharedPreferences();
		 
		AlertDialog.Builder builder = new AlertDialog.Builder(mm);
		View checkBoxView = View.inflate(mm, R.layout.wifiwarnview, null);
		builder.setView(checkBoxView); 
		
		if (sp.getBoolean("warnWIFI", true) && mm.getMainHelper().getDeviceDetected() != MainHelper.DEVICE_OUYA) {

			CheckBox checkBox = (CheckBox) checkBoxView
					.findViewById(R.id.dontbotherCBox);
			checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					SharedPreferences.Editor edit = sp.edit();
					edit.putBoolean("warnWIFI", !isChecked);
					edit.commit();
				}
			});

			builder.setTitle("Open Wi-Fi Settings?");
			builder.setPositiveButton("Yes",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mm.startActivity(new Intent(
									WifiManager.ACTION_PICK_WIFI_NETWORK));
						}
					});
			builder.setNegativeButton("No",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							netplayDlg.show();
						}
					});
			builder.show();
		} else {
			netplayDlg.show();
		}
		*/
				
	}

    public  String getIPAddress() {
        try {
            Enumeration<NetworkInterface> ifaceList;
            NetworkInterface selectedIface = null;

            // First look for a WLAN interface
            ifaceList = NetworkInterface.getNetworkInterfaces();
            while (selectedIface == null && ifaceList.hasMoreElements()) {
                    NetworkInterface intf = ifaceList.nextElement();
                    if (intf.getName().startsWith("wlan")) {
                        List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                        for (InetAddress addr : addrs) {
                            if (!addr.isLoopbackAddress()) {
                                String sAddr = addr.getHostAddress().toUpperCase();
								/* [CNINJA-011] Netplay doesn't work */
                                /* boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                                if (isIPv4) 
                                    return sAddr;   */
								if( addr instanceof Inet4Address)
									return sAddr;
                            }
                        }                                               	
                    }
            }

            // If we didn't find that, look for an Ethernet interface
            ifaceList = NetworkInterface.getNetworkInterfaces();
            while (selectedIface == null && ifaceList.hasMoreElements()) {
                    NetworkInterface intf = ifaceList.nextElement();
                    if (intf.getName().startsWith("eth")) {
                        List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                        for (InetAddress addr : addrs) {
                            if (!addr.isLoopbackAddress()) {
                                String sAddr = addr.getHostAddress().toUpperCase();
								/* [CNINJA-011] Netplay doesn't work */
                                /* boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                                if (isIPv4) 
                                    return sAddr; */
								if ( addr instanceof Inet4Address)
									return sAddr;
                            }
                        }                                               	
                    }
            }       	
        	
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
						/* [CNINJA-011] Netplay doesn't work */
                        /* boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        if (isIPv4) 
                            return sAddr;*/
						if (addr instanceof Inet4Address)
						    return sAddr;
                    }
                }
            }
        } catch (Exception ex) { }
        return null;
    }
	
	Button.OnClickListener createGameClick = new Button.OnClickListener() {
		public void onClick(View v) {			
			createGame();			
		}
	};

	public String checkIfRoomExistOnSameNetwork(){
		String ip="";

		MulticastSocket ms = null;
		DatagramPacket packet;
		byte[] data = new byte[1024];

		Log.d(TAG, "tony debug 111111");
		try {
			InetAddress groupAddress = InetAddress.getByName(ROOM_MULTICAST_ADDR);
			ms = new MulticastSocket(ROOM_MULTICAST_PORT);
			ms.joinGroup(groupAddress);
		} catch (Exception e) {
			e.printStackTrace();
			Log.d(TAG, "error message  : " + e.toString());
		}

		Log.d(TAG, "tony debug 2222222");
		try {
			packet = new DatagramPacket(data, data.length);
			ms.setSoTimeout(300);
			if (ms != null)
				ms.receive(packet);

			String codeString = new String(data, 0, packet.getLength());
			Log.d(TAG, "received ROOM IP : " + packet.getAddress().toString() + " data[" + codeString + "]");

			if(codeString.length() > 0)
			{
				String[] tokens = codeString.split("/");
				String ipaddress = tokens[0];
				String gamename = tokens[1];
				String gamehostdevicename = tokens[2];
				if(gamename.compareTo(mm.getPackageName()) == 0){
					ip = ipaddress;
				}
			}
		} catch (Exception e) {
			Log.d(TAG, "tony trace error : " + e.toString());
			e.printStackTrace();
			ip = "";
		}
		ms.close();
		return ip;
	}
	Button.OnClickListener joinGameClick = new Button.OnClickListener() {
		public void onClick(View v) {	
			AlertDialog.Builder alert = new AlertDialog.Builder(mm);

			alert.setTitle("Enter peer IP Address:");
			//alert.setMessage("Enter peer IP address:");

			//EditText
			View alertView = mm.getLayoutInflater().inflate(R.layout.netplay_join, null);

			if(input == null) {
				input = (EditText) alertView.findViewById(R.id.ipinput);
				input2 = (Spinner) alertView.findViewById(R.id.ipsuggestions);
				input2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
						input.setText(input2.getItemAtPosition(position).toString());
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {

					}
				});
				String[] initialServerIP = new String[] {};
			    roomList = new ArrayList<>(Arrays.asList(initialServerIP));
			    if(spinnerArrayAdapter == null) {
					spinnerArrayAdapter = new ArrayAdapter<String>(mm.getApplicationContext(), R.layout.spinner_item, roomList);
					spinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_item);
				}
				input2.setAdapter(spinnerArrayAdapter);
						//final ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
				//                this,R.layout.spinner_item,plantsList);
			}

//				input = new EditText(mm);
//			EditText input2 = new EditText(mm);
//			input2.setText("hello second");
			alert.setView(alertView);
//			alert.setView(input2);
			
//			String ip = mm.getPrefsHelper().getSharedPreferences().getString(PrefsHelper.PREF_NETPLAY_PEERADDR,"");
			String ip = "" ;
//TODO : get list of ip addresses -
			Log.d(TAG, "tony debug before check room existance");
			receiverThread = new Thread((new Runnable() {
				//int x = 0;
				NetPlay parent = null;
				EditText et = null;
				ArrayAdapter<String> spinAdp = null ;
				Object obj;
				String ip = "";
				public void run() {
					while (myself.State_fetchingIP == true){
						ip = checkIfRoomExistOnSameNetwork();
						Log.d(TAG, "tony debug ip address received : " + ip );
						try {
							Thread.sleep(300);
						} catch(InterruptedException e){
							e.printStackTrace();
						}
						if(ip.length() > 0 ) {
							//et.setText(ip);
//							spinAdp.add(ip);
							parent.myself.addRoomSuggestions(ip);
						}
					}
				}
				public Runnable pass(NetPlay parent) {
					this.et = parent.input;
					this.parent = parent;
					this.spinAdp = parent.spinnerArrayAdapter;
					//this.x = x;
					//this.obj = obj;
//					((String) obj) =
					return this;
				}
			}).pass(myself));

			spinnerArrayAdapter.clear();
			roomList.clear();
			State_fetchingIP = true;
			receiverThread.start();

			input.setText(ip);
			input.setSelection(input.getText().length());

			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {

			     String ip = input.getText().toString();

				if(ip==null || ip.length()==0)
			     {
					Toast.makeText(mm, "Invalid peer IP!",Toast.LENGTH_SHORT).show();
					return;
			     }
	
			     InputMethodManager imm = (InputMethodManager)mm.getSystemService(Service.INPUT_METHOD_SERVICE);
			     imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
			     
			     SharedPreferences sp = mm.getPrefsHelper().getSharedPreferences();
			     Editor edit = sp.edit();
				 edit.putString(PrefsHelper.PREF_NETPLAY_PEERADDR,ip);
				 edit.commit();

				 //sunghook : clear server room fetching thread -
				{
					State_fetchingIP = false;
					spinnerArrayAdapter.clear();
					roomList.clear();
					input = null;
					try {
						receiverThread.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
						Log.d(TAG, "thread joining error with " + e.toString());
					}
				}
			     joinGame(ip);
			  }
			});

			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			  public void onClick(DialogInterface dialog, int whichButton) {
			    // Canceled.
				  myself.State_fetchingIP = false;
				  spinnerArrayAdapter.clear();
				  roomList.clear();
				  input = null;

			  }
			});
			alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					myself.State_fetchingIP = false;
					spinnerArrayAdapter.clear();
					roomList.clear();
					input = null;

				}
			});

			AlertDialog dlg = alert.create();					
			dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
			dlg.show();					
		}
	};
	
	Button.OnClickListener disconnectGameClick = new Button.OnClickListener() {
		public void onClick(View v) {			
			Emulator.setValue(Emulator.NETPLAY_HAS_CONNECTION, 0);
			Toast.makeText(mm, "Disconnected from Netplay",Toast.LENGTH_SHORT).show();	
			prepareButtons();

		}
	};	

	public String getDeviceName() {
		String manufacturer = Build.MANUFACTURER;
		String model = Build.MODEL;
		if (model.startsWith(manufacturer)) {
			return capitalize(model);
		} else {
			return capitalize(manufacturer) + " " + model;
		}
	}


	private String capitalize(String s) {
		if (s == null || s.length() == 0) {
			return "";
		}
		char first = s.charAt(0);
		if (Character.isUpperCase(first)) {
			return s;
		} else {
			return Character.toUpperCase(first) + s.substring(1);
		}
	}
	public void createGame() {
		
		String strPort = mm.getPrefsHelper().getNetplayPort();
		final String RoomName = getDeviceName();
		final String GameName = mm.getPackageName();


		int port = 0;
		try{port = Integer.parseInt(strPort);}catch(Exception e){}
		if(!(port>=1024 && port <= 32768*2)){
			Toast.makeText(mm, "Invalid Port",Toast.LENGTH_SHORT).show();
			return;
		}

		if (Emulator.netplayInit(null, port, 0) == -1) {
			Toast.makeText(mm, "Error initializing Netplay!",Toast.LENGTH_SHORT).show();
			return;
		}
		
		//netplayDlg.hide();

		canceled = false;
		progressDialog = ProgressDialog.show(mm, "Press back to cancel",
				"Creating game at ...", true, true,
				new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						canceled = true;
					}
				});

		Thread t = new Thread(new Runnable() {
			byte[] roomBroadcastingData = new byte[1024];
			public void run() {
				final String ip = getIPAddress(); 
				if(ip==null)
				{
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					canceled = true;
			    	mm.runOnUiThread(new Runnable() {
			            public void run() {
			            	Toast.makeText(mm, "No IP address available!. Is Wi-Fi enabled?",Toast.LENGTH_LONG).show();
			            }
			    	});					
				}
		    	mm.runOnUiThread(new Runnable() {
		            public void run() {
		            	 progressDialog.setMessage("Waiting for peer...\nCreating game at :" + ip );
		            }
		    	});					  
				while (Emulator.getValue(Emulator.NETPLAY_HAS_JOINED) == 0 && !canceled) {
					MulticastSocket sender = null;
					DatagramPacket packet = null;
					InetAddress group = null;
					try {
						//sunghook debug --> send UDP broadcast from here ->>>>>
						// eg., ) "192.168.10.108/com.projectgg.cninja/Samsung Galaxy S10"
						roomBroadcastingData = (ip+"/"+ GameName + "/"+ RoomName).getBytes();
						sender = new MulticastSocket();
						group = InetAddress.getByName(ROOM_MULTICAST_ADDR);
						packet = new DatagramPacket(roomBroadcastingData, roomBroadcastingData.length, group, ROOM_MULTICAST_PORT);
						sender.send(packet);
						Log.d(TAG, "sending broadcast over" + ROOM_MULTICAST_ADDR + ":" + ROOM_MULTICAST_PORT);
						sender.close();
						Thread.sleep(300);
						//System.out.println("Esperando...");
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (IOException e){
						e.printStackTrace();
					}
				}

				if (progressDialog != null && progressDialog.isShowing()) {
					progressDialog.dismiss();
				}

				if (canceled) {
					Emulator.setValue(Emulator.NETPLAY_HAS_CONNECTION, 0);
				} else {
					Emulator.setValue(Emulator.EXIT_GAME_KEY, 1);
				}
		    	mm.runOnUiThread(new Runnable() {
		            public void run() {
		            	if(!canceled)
		            	{
		            		if(netplayDlg.isShowing())
		            		  netplayDlg.hide();
		            		Toast.makeText(mm, "Connected. Starting Netplay!",Toast.LENGTH_SHORT).show();			            		
		            		Emulator.resume();
		            	}
		            }
		    	});					
			}
		});
		t.start();
	}
    
    public void joinGame(String addr){

		String strPort = mm.getPrefsHelper().getNetplayPort();		
		int port = 0;
		try{port = Integer.parseInt(strPort);}catch(Exception e){}
		if(!(port>=1024 && port <= 32768*2)){
			Toast.makeText(mm, "Invalid Port",Toast.LENGTH_SHORT).show();
			return;
		}
		
		if (Emulator.netplayInit(addr, port, 0) == -1) {
			Toast.makeText(mm, "Error initializing Netplay!",
					Toast.LENGTH_SHORT).show();
			return;
		}
		
		//netplayDlg.hide();
		
		canceled = false;
		progressDialog = ProgressDialog.show(mm, "Press back to cancel",
				"Connecting to :" + addr, true, true,
				new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						canceled = true;
						State_fetchingIP = false;
						spinnerArrayAdapter.clear();
						roomList.clear();
						input = null;
						Log.d(TAG, "sunghook clear IP fetching thread -----");
						try {
							receiverThread.join();
						} catch (InterruptedException e){
							e.printStackTrace();
							Log.d(TAG, "thread joining error with " + e.toString());
						}
					}
				});

		Thread t = new Thread(new Runnable() {
			public void run() {
				while (Emulator.getValue(Emulator.NETPLAY_HAS_JOINED) == 0
						&& !canceled) {
					try {
						if (Emulator.netplayInit(null, 0, 1) == -1)
							canceled = true;
							State_fetchingIP = false;
						spinnerArrayAdapter.clear();
						roomList.clear();
						input = null;
						Thread.sleep(1000);
						//System.out.println("Esperando...");
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				if (progressDialog != null && progressDialog.isShowing()) {
					progressDialog.dismiss();
				}

				if (canceled) {
					Emulator.setValue(Emulator.NETPLAY_HAS_CONNECTION, 0);
				} else {
					Emulator.setValue(Emulator.EXIT_GAME_KEY, 1);
				}
				
		    	mm.runOnUiThread(new Runnable() {
		            public void run() {
		            	if(!canceled)
		            	{
		            		if(netplayDlg.isShowing())
			            		  netplayDlg.hide();
		            		Toast.makeText(mm, "Connected. Starting Netplay!",Toast.LENGTH_SHORT).show();
		            		Emulator.resume();
		            	}
		            }
		    	});	
			}
		});
		t.start();    	   		
    }
    
}
