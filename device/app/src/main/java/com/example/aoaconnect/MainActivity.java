package com.example.aoaconnect;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.widget.TextView;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.usb4java.*;

import com.google.protobuf.*;


public class MainActivity extends AppCompatActivity{
    public final String TAG = "Device-side: ";
    public UsbManager manager;
    public UsbAccessory accessory;
    public ParcelFileDescriptor fileDescriptor;
    public FileInputStream inputStream;
    public FileOutputStream outputStream;
    IntentFilter filter;
    //broadcast receiver
    public final BroadcastReceiver usbReceiver = new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                try {
                    inputStream.close();
                    outputStream.close();
                    fileDescriptor.close();
                    unregisterReceiver(usbReceiver);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState){
        Log.d("Status: ","program started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        manager = (UsbManager)getSystemService(Context.USB_SERVICE);
        accessory = this.getIntent().getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        filter = new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(usbReceiver,filter);

        //keep trying to read until successful; once read() succeeds, write() a USBResponse back
        boolean accessoryOpened = openAccessory();
        if(accessoryOpened){
            while(true){
                boolean readSuccessful = read();
                if(readSuccessful) {
                    write();
                    break;
                }
            }
        }
    }
    public boolean openAccessory() {
        Log.d(TAG, "openAccessory: " + accessory);
        if(manager.hasPermission(accessory))
            fileDescriptor = manager.openAccessory(accessory);
        else
            return false;
        if (fileDescriptor != null) {
            FileDescriptor fd = fileDescriptor.getFileDescriptor();
            inputStream = new FileInputStream(fd);
            outputStream = new FileOutputStream(fd);
        } else
            return false;
        return true;
    }
    public void write(){
        USBResponse.Builder preRes = USBResponse.newBuilder();
        preRes.setId(0);
        preRes.setStatus(0);
        //payload?
        byte[] binOutput = {0,1,0,0,0,0,1,1,1,0,1,0,1,1};
        ByteString params = ByteString.copyFrom(binOutput);
        preRes.setParameters(params);

        UsbResponse res = preRes.build();
        byte writeData = res.toByteArray();
        try{
            outputStream.write(writeData)
        }
        catch(IOException i){
            i.printStackTrace();
        }
    }
    public boolean read() {
        byte[] in = new byte[64];
        try {
            int read = inputStream.read(in);
            if (read > 0) {
                Log.d(TAG, "Read successful");
                TextView t = findViewById(R.id.data);
                displayData(t,in);
                return true;
            }
            else {
                Log.d(TAG, "Stream dry");
                return false;
            }
          }
        catch (IOException i) {
            i.printStackTrace();
        }
    }
    public void displayData(TextView t, byte[] in){
        String s = "";
        for(byte b : in)
            s += b + " ";
        t.setText(s);
    }
}//end MainActivity
