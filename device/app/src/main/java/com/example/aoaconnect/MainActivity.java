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

        while(true) {
            if (openAccessory()) {
                rw();
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
    public void rw() {
        //read
        byte[] in = new byte[8];
        try {
            int read = inputStream.read(in);
            if (read > 0) {
                Log.d(TAG, "Read successful");
                TextView t = findViewById(R.id.data);
                displayData(t,in);
            }
            else {
                Log.d(TAG, "Stream dry");
            }
          }
        catch (IOException e) {
            e.printStackTrace();
        }
        //write
        //byte[] out = new byte[8];
        //for (int i = 0; i < in.length; i++) {
            //Log.d(TAG, "Data: " + in[i]);
            //out[i] = (byte) (in[i] + 1);
        //}
       // try {
            //outputStream.write(out);
       // }
        //catch (IOException e) {
            //e.printStackTrace();
        //}
    }
    public void displayData(TextView t, byte[] in){
        String s = "";
        for(byte b : in)
            s += b + " ";
        t.setText(s);
    }
}//end MainActivity

