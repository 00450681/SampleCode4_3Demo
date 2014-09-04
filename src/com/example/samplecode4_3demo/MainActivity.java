package com.example.samplecode4_3demo;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.EncodingUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bde.parentcyTransport.ACSUtility;
import com.bde.parentcyTransport.ACSUtility.blePort;

public class MainActivity extends Activity implements View.OnClickListener, OnCheckedChangeListener{

	private Button mEnumPortsBtn, mOpenOrCloseBtn, mSendBtn, mClearBtn, mReceiveClearBtn, mSendClearBtn;
	private TextView mSendCountTV, mReceiveCountTV, mReceiveRateTV;
	private TextView mReceiveDataTV, mSendDataTV;
	private EditText mInternalTV;
	private TextView mSelectedPortTV;
	private Switch mSendFormat, mReceiveFormat, mReceiveClear;
	private ScrollView mScrollView;
	private CheckBox mPeriod;
	private ACSUtility util;
	private boolean isPortOpen = false, isReceiveHex = false, isSendHex = false, isClear = true;
	private boolean isPeriod = false;
	private ACSUtility.blePort mCurrentPort, mSelectedPort;
	private String mReceivedData, mSendData;
	private int mTotalReceiveSize = 0, mLastSecondTotalReceiveSize = 0;
	private int mTotalSendSize = 0;
	private int mInternal = 0;
	private countRecevieRateThread mCountRecevieRateThread;
	private ProgressDialog mProgressDialog;
	public final static int REQEEST_ENUM_PORTS = 10;
	
	private Handler mCountReceiveRateHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			//super.handleMessage(msg);
			
		}
		
	};
	private final static String TAG = "ACSMainActivity";
	private ACSUtility.IACSUtilityCallback userCallback = new ACSUtility.IACSUtilityCallback() {

		@Override
		public void didFoundPort(blePort newPort) {
			// TODO Auto-generated method stub

		}

		@Override
		public void didFinishedEnumPorts() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void didOpenPort(blePort port, Boolean bSuccess) {
			// TODO Auto-generated method stub
			Log.d(TAG, "The port is open ? " + bSuccess);
			if (bSuccess)  {
				isPortOpen = true;
				mCurrentPort = port;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						updateUiObject();
						showSuccessDialog();
						getProgressDialog().cancel();
					}
				});
			} else {
				getProgressDialog().cancel();
				showFailDialog();
			}
			
		}

		

		@Override
		public void didClosePort(blePort port) {
			// TODO Auto-generated method stub
			isPortOpen = false;
			mCurrentPort = null;
			if (getProgressDialog().isShowing()) {
				getProgressDialog().dismiss();
			}
			Toast.makeText(MainActivity.this, "Disconnected from Peripheral", Toast.LENGTH_SHORT).show();
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					updateUiObject();
				}
			});
		}

		@Override
		public void didPackageReceived(blePort port, byte[] packageToSend) {
			// TODO Auto-generated method stub
			/*try {
				mReceivedData = new String(packageToSend, "GBK");
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						updateUiObject();
					}
				});
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			StringBuilder sb = new StringBuilder();
			if (!isClear)
				sb.append(mReceivedData);
			if (isReceiveHex) {
				for (byte b : packageToSend) {
					sb.append("0x");
					if ((b & 0xff) <= 0x0f) {
						sb.append("0");
					}
					sb.append(Integer.toHexString(b & 0xff) + " ");
				}
			} else {
				String temp = "";
				try {
					temp = new String(packageToSend, "utf-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				sb.append(temp);
			}
			mReceivedData = sb.toString();
			mTotalReceiveSize += packageToSend.length;
			
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					updateUiObject();
					//mLastSecondTotalReceiveSize = mTotalReceiveSize;
				}
			});
		}

		@Override
		public void heartbeatDebug() {
			// TODO Auto-generated method stub

		}

		@Override
		public void utilReadyForUse() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void didPackageSended(boolean succeed) {
			// TODO Auto-generated method stub
			if (succeed) {
				Toast.makeText(MainActivity.this, "数据发送成功", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(MainActivity.this, "数据发送失败", Toast.LENGTH_SHORT).show();
			}
		}

	};
	
	private int REQUEST_ENABLE_BT = 11;
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		//super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQEEST_ENUM_PORTS
				&& resultCode == Activity.RESULT_OK) {
			Bundle bundle = data.getExtras();
			
			BluetoothDevice device = bundle.getParcelable(BluetoothDevice.EXTRA_DEVICE);
			mSelectedPort = util.new blePort(device);
			//util = new ACSUtility(this, userCallback);
			//util.setUserCallback(userCallback);
			updateUiObject();
		} else if (requestCode == REQUEST_ENABLE_BT) {
			final BluetoothManager bluetoothManager =
			        (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			mBluetoothAdapter = bluetoothManager.getAdapter();
			if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
			    Toast.makeText(MainActivity.this, "Bluetooth Disable...Quit...", Toast.LENGTH_SHORT).show();
			    finish();
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		util = new ACSUtility(this, userCallback);
		mCountRecevieRateThread = new countRecevieRateThread();
		
		mCountReceiveRateHandler.postDelayed(mCountRecevieRateThread, 1000);
		initViewObject();
	}

	BluetoothAdapter mBluetoothAdapter;
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		final BluetoothManager bluetoothManager =
		        (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		//mSendDataTV.setFocusable(true);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		util.closeACSUtility();
	}

	private void initViewObject() {
		mEnumPortsBtn = (Button) findViewById(R.id.enumAllPortsBtn);
		mOpenOrCloseBtn = (Button) findViewById(R.id.openOrClosePortsBtn);
		mSendBtn = (Button) findViewById(R.id.sendBtn);
		mClearBtn = (Button) findViewById(R.id.clearBtn);
		mReceiveClearBtn = (Button) findViewById(R.id.receiveTextClearBtn);
		mSendClearBtn = (Button) findViewById(R.id.sendTextClearBtn);
		
		
		mSendCountTV = (TextView) findViewById(R.id.sendCount);
		mReceiveCountTV = (TextView) findViewById(R.id.receiveCount);
		mReceiveRateTV = (TextView) findViewById(R.id.receviceRate);
		
		mReceiveDataTV = (TextView) findViewById(R.id.receiveData);
		mSendDataTV = (TextView) findViewById(R.id.sendData);
		mInternalTV = (EditText) findViewById(R.id.internal);
		
		mSelectedPortTV = (TextView) findViewById(R.id.selectedPort);
		
		mSendFormat = (Switch) findViewById(R.id.sendFormat);
		mReceiveFormat = (Switch) findViewById(R.id.receiveFormat);
		mReceiveClear = (Switch) findViewById(R.id.receiveClear);
		
		mScrollView = (ScrollView) findViewById(R.id.scroll);
		
		mPeriod = (CheckBox) findViewById(R.id.period);
		
		mEnumPortsBtn.setOnClickListener(this);
		mOpenOrCloseBtn.setOnClickListener(this);
		mSendBtn.setOnClickListener(this);
		mClearBtn.setOnClickListener(this);
		mReceiveClearBtn.setOnClickListener(this);
		mSendClearBtn.setOnClickListener(this);
		
		mSendFormat.setOnCheckedChangeListener(this);
		mReceiveFormat.setOnCheckedChangeListener(this);
		mReceiveClear.setOnCheckedChangeListener(this);
		
		//mSendDataTV.setFocusable(false);
		
		//mInternalTV.seton
		isPeriod = mPeriod.isChecked();
		mPeriod.setOnCheckedChangeListener(this);
		
		//mReceiveDataTV.setMovementMethod(ScrollingMovementMethod.getInstance());
		
		/*mEnumPortsBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent();
				intent.setClass(MainActivity.this, EnumPortActivity.class);
				MainActivity.this.startActivityForResult(intent, REQEEST_ENUM_PORTS);
			}
			
		});
		mOpenOrCloseBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (isPortOpened) {
					util.closePort();
				}
				else if (mSelectedPort != null) {
						util.openPort(mSelectedPort);
				}
				else {
						Log.i(TAG, "User didn't select a port...So the port won't be opened...");
				}
			}
			
		});
		mSendBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String data = (String) mSendDataTV.getText();
				util.writePort(data.getBytes());
			}
			
		});*/
		
		mOpenOrCloseBtn.setText("Open");
	}
	
	private void updateUiObject() {
		// TODO Auto-generated method stub
		if (isPortOpen) {
			mOpenOrCloseBtn.setText("Close");
		}
		else {
			mOpenOrCloseBtn.setText("Open");
		}
		mSendCountTV.setText(mTotalSendSize + "");
		mReceiveCountTV.setText(mTotalReceiveSize + "");
		//mReceiveRateTV.setText((mTotalReceiveSize - mLastSecondTotalReceiveSize) + " B/s");
		
		mReceiveDataTV.setText(mReceivedData);
		
		
		if (mSelectedPort != null) {
			mSelectedPortTV.setText(mSelectedPort._device.getName());
		}
		scrollToBottom();
		/*if (isPortOpen) {
			
		}*/
	}
	private void scrollToBottom() {
		int off = mReceiveDataTV.getHeight() - mScrollView.getHeight();
		if (off > 0) {
			mScrollView.scrollTo(0, off);
		}
	}
	
	private void showSuccessDialog() {
		new AlertDialog.Builder(MainActivity.this)
    	.setTitle("Open Port")
    	.setMessage("open port success")
    	.setPositiveButton("comfirm", new android.content.DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				dialog.dismiss();
			}
		}).show();
	}
	private void showFailDialog() {
		new AlertDialog.Builder(MainActivity.this)
    	.setTitle("Open Port")
    	.setMessage("open port failed")
    	.setPositiveButton("comfirm", new android.content.DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				dialog.dismiss();
			}
		}).show();
	}
	private ProgressDialog getProgressDialog() {
		if (mProgressDialog != null) {
			return mProgressDialog;
		}
		mProgressDialog = new ProgressDialog(MainActivity.this);
		mProgressDialog.setCanceledOnTouchOutside(false);
		mProgressDialog.setMessage("Connecting...");
		//progressDialog.setTitle(title)
		return mProgressDialog;
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	class countRecevieRateThread implements Runnable {

		public void run() {
			// TODO Auto-generated method stub
			//super.run();
			//mReceiveRate.setText("接收速率：" + (totalReceiveSize - lastSecondTotalReceiveSize) + " B/s");
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					//updateUiObject();
					mReceiveRateTV.setText((mTotalReceiveSize - mLastSecondTotalReceiveSize) + " B/s");
				}
			});
			mLastSecondTotalReceiveSize = mTotalReceiveSize;
			mCountReceiveRateHandler.postDelayed(mCountRecevieRateThread, 1000);
		}
		
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.enumAllPortsBtn:
				// TODO Auto-generated method stub
				Intent intent = new Intent();
				intent.setClass(MainActivity.this, EnumPortActivity.class);
				this.startActivityForResult(intent, REQEEST_ENUM_PORTS);
				break;
		case R.id.openOrClosePortsBtn:
			if (isPortOpen) {
					util.closePort();
				}
				else if (mSelectedPort != null) {
					// 等待窗口
					getProgressDialog().show();
					util.openPort(mSelectedPort);
				}
				else {
						Log.i(TAG, "User didn't select a port...So the port won't be opened...");
				}
			
			break;
		case R.id.sendBtn:
			//StringBuilder sb = new StringBuilder();
			sendData();
			/*String data = mSendDataTV.getText().toString();
			ByteArrayBuffer bab = new ByteArrayBuffer(data.length() / 2);
			
			if (isSendHex) {
				for (int i = 0; i < data.length();) {
					if (data.charAt(i) != ' ') {
						String byteStr =  data.substring(i, i + 2);
						bab.append(hexStrToByteArray(byteStr));
						i += 2;
					} else {
						i++;
					}
				}
			} else {
				byte []dataBytes = data.getBytes(); 
				bab.append(dataBytes, 0, dataBytes.length);
			}
			mTotalSendSize += bab.toByteArray().length;
			util.writePort(bab.toByteArray());
			updateUiObject();*/
			
			/*String data = "";
			try {
				data = readFileSdcardFile("/sdcard/test.txt");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mTotalSendSize += data.length();
			util.writePort(data.getBytes());
			updateUiObject();*/
			break;
		case R.id.clearBtn:
			mTotalSendSize = 0;
			mTotalReceiveSize = 0;
			updateUiObject();
			break;
		case R.id.receiveTextClearBtn:
			mReceivedData = "";
			updateUiObject();
			break;
		case R.id.sendTextClearBtn:
			mSendData = "";
			mSendDataTV.setText(mSendData);
			updateUiObject();
		}
	}
	private void sendData() {
		String data = mSendDataTV.getText().toString();
		ByteArrayBuffer bab = new ByteArrayBuffer(data.length() / 2);
		
		if (isSendHex) {
			for (int i = 0; i < data.length();) {
				if (data.charAt(i) != ' ') {
					String byteStr =  data.substring(i, i + 2);
					bab.append(hexStrToByteArray(byteStr));
					i += 2;
				} else {
					i++;
				}
			}
		} else {
			byte []dataBytes = data.getBytes(); 
			bab.append(dataBytes, 0, dataBytes.length);
		}
		mTotalSendSize += bab.toByteArray().length;
		util.writePort(bab.toByteArray());
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				updateUiObject();
			}
			
		});
		
	}
	private byte hexStrToByteArray(String hexString) {
		byte []hexStrBytes = hexString.getBytes();
		byte bit0 = Byte.decode("0x" + new String(new byte[]{hexStrBytes[0]}));
		bit0 = (byte) (bit0 << 4);
		byte bit1 = Byte.decode("0x" + new String(new byte[]{hexStrBytes[1]}));
		return (byte) (bit0 | bit1);
	}
	private SendPeriodThread mSendThread;
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// TODO Auto-generated method stub
		switch (buttonView.getId()) {
		case R.id.sendFormat:
			mSendData = "";
			mSendDataTV.setText(mSendData);
			isSendHex = isChecked;
			break;
		case R.id.receiveFormat:
			mReceivedData = "";
			mReceiveDataTV.setText(mReceivedData);
			isReceiveHex = isChecked;
			break;
		case R.id.receiveClear:
			mReceivedData = "";
			mReceiveDataTV.setText(mReceivedData);
			isClear = !isChecked;
			break;
		case R.id.period:
			isPeriod = isChecked;
			if (isPeriod) {
				mInternal = Integer.parseInt(mInternalTV.getText().toString());
				mSendThread = new SendPeriodThread(mInternal);
				mSendThread.start();
			} else {
				if (mSendThread != null) {
					mSendThread.stopThread();
				}
			}
			break;
		}
	}
	private String readFileSdcardFile(String fileName) throws IOException {
		String res="";
		byte[] buffer = null;
		try {
			FileInputStream fin = new FileInputStream(fileName);

			int length = fin.available();

			buffer = new byte[length];
			fin.read(buffer);

			res = EncodingUtils.getString(buffer, "UTF-8");

			fin.close();
		}

		catch (Exception e) {
			e.printStackTrace();
		}
		//return buffer;
		return res;
	}

	public class SendPeriodThread extends Thread {
		int mInternal;
		//byte []mData;
		boolean start = true;
		public SendPeriodThread(int internal) {
			mInternal = internal;
			//mData = data;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			while (start) {
				sendData();
				try {
					Thread.sleep(mInternal);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		public void stopThread() {
			start = false;
		}
	}
}
