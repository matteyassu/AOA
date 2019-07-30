//PC-SIDE VERSION
//Next Dev: Multithreading
//Problems: 
//changes: altsetting.bInterfaceNumber = 0
import org.usb4java.*;
import java.nio.*;
//import android.hardware.usb.manager;
/*
TODO: automate device-side libusb-based driver runtime installation with Zadig libwdi library (will unnecessarily slow execution)
Redownload libusbK driver after board enters AOA mode
*/
public class PCConnection{
   /*
   Messages:
   libusb initialized
   device found
   Device attempting to restart in accessory mode
   Device in accessory mode
   searching for endpoints
   (display endpoints)
   endpoints found
   Attempting bulk transfer
   */
   public static void main(String [] args){
      Device device = null;
      int result = LibUsb.init(null);
      if(result != LibUsb.SUCCESS)
         throw new LibUsbException("Unable to initialize libusb(create new session)", result);
      else
         System.out.println("libusb initialized");  
         
     //find HiKey board
      DeviceList list = new DeviceList();
      result = LibUsb.getDeviceList(null, list);
      if (result < 0)
         throw new LibUsbException("Unable to get device list", result);         
      try {
         for (Device d : list) {
            DeviceDescriptor descriptor = new DeviceDescriptor();
            result = LibUsb.getDeviceDescriptor(d, descriptor);
            if (result != LibUsb.SUCCESS)
               throw new LibUsbException("Unable to read device descriptor", result);
            if (descriptor.idVendor() == 0x18D1){
               device = d;
               break;
            }
         }
         if(device == null)
            throw new LibUsbException("No appropriate devices connected", -1);
         else
            System.out.println("Device found: " + device.toString());
      }
      catch(Exception e){
         e.printStackTrace();
      }        
      //get device descriptor and handle
      DeviceDescriptor descriptor = new DeviceDescriptor();
      result = LibUsb.getDeviceDescriptor(device,descriptor);
      DeviceHandle handle = new DeviceHandle();
      result = LibUsb.open(device,handle);
      if(result != LibUsb.SUCCESS)
         throw new LibUsbException("Unable to open USB device handle", result);
      
      //check accessory mode support (2x max) and attempt to restart in accessory mode 
      DeviceList updatedList = new DeviceList();
      boolean accessoryMode = false;
      int counter = 1;
      while (!accessoryMode && counter <= 2) {
         System.out.println("Accessory mode compatability check: Attempt " + counter);
         if ((descriptor.idVendor() == 0x18D1) && (descriptor.idProduct() == 0x2D00 || descriptor.idProduct() == 0x2D01)) {
            accessoryMode = true;
            System.out.println("Device in accessory mode");
            break;
         }
         else {
            //holders for metadata/incoming data
            String computerManufacturer = "LENOVO";
            String computerModel = "20EN0017US";
            String computerDescription = "HAL9000";
            String computerVersion = "N1EET79W"; //BIOS version 
            String computerURI = "50-7B-9D-CF-3B-80";//MAC address
            String computerSerialNumber = "PC0CGX2Q";
            ByteBuffer protocol = ByteBuffer.allocateDirect(2);
            //getProtocol(CR51): bmrequestType((dir,type,recipient)device->host,vendor,device) = 11000000 = 192),brequest(51),value(0),index(0),data(16 bits returned in protocol bytebuffer)
            result = LibUsb.controlTransfer(handle,(byte)192,(byte)51,(short)0,(short)0,protocol,(long)5000);
            if(result < 0)
               throw new LibUsbException("GetProtocol control transfer failed",result);
               
            if(protocol.get(0) + protocol.get(1) > 0){ 
               //send computer metadata(CR52)
               //manufacturer
               ByteBuffer computerMetadata = ByteBuffer.allocateDirect(64);
               computerMetadata.put(computerManufacturer.getBytes());
               result = LibUsb.controlTransfer(handle,(byte)64, (byte)52, (short)0, (short)0,computerMetadata,(long)5000);
               if(result < 0) throw new LibUsbException("Manufacturer metadata control transfer failed",result);
               //model
               computerMetadata.clear();
               computerMetadata.put(computerModel.getBytes());
               LibUsb.controlTransfer(handle,(byte)64, (byte)52, (short)0, (short)1,computerMetadata,(long)5000);
               //description
               computerMetadata.clear();
               computerMetadata.put(computerDescription.getBytes());
               LibUsb.controlTransfer(handle,(byte)64, (byte)52, (short)0, (short)2,computerMetadata,(long)5000);
               //version
               computerMetadata.clear();
               computerMetadata.put(computerVersion.getBytes());
               LibUsb.controlTransfer(handle,(byte)64, (byte)52, (short)0, (short)3,computerMetadata,(long)5000);
               //URI 
               computerMetadata.clear();
               computerMetadata.put(computerURI.getBytes());
               LibUsb.controlTransfer(handle,(byte)64, (byte)52, (short)0, (short)4,computerMetadata,(long)5000);
               //serial number
               computerMetadata.clear();
               computerMetadata.put(computerSerialNumber.getBytes());
               LibUsb.controlTransfer(handle,(byte)64, (byte)52, (short)0, (short)5,computerMetadata,(long)5000);
               //restart in accessory mode(CR53)
               ByteBuffer empty = ByteBuffer.allocateDirect(0);
               System.out.println("Device attempting to restart in accessory mode...");  
               result = LibUsb.controlTransfer(handle, (byte)64, (byte)53, (short)0,(short)0,empty,(long)5000);
               if(result < 0)
                  throw new LibUsbException("Accessory mode restart failed",result);
               //device in accessory mode,re-search
               System.out.println(descriptor.idVendor());
               System.out.println(descriptor.idProduct());
               int numAcc = 0;
               LibUsb.getDeviceList(null,updatedList);
               for(Device d : updatedList){
                  LibUsb.getDeviceDescriptor(d,descriptor);
                  System.out.println("DEVICE: \nIdVendor: " + descriptor.idVendor() + "\nProductID: " + descriptor.idProduct() + device.toString() + "\n");
                  if ((descriptor.idVendor() == 0x18D1) && (descriptor.idProduct() == 0x2D00 || descriptor.idProduct() == 0x2D01)){
                     device = d;
                     numAcc++;
                     
                  }
               }
            }
            counter++;
         }
      }
      if(!accessoryMode)
         throw new LibUsbException("Accessory mode not supported on this device",-1);
      
      LibUsb.freeDeviceList(list,true);
      LibUsb.close(handle);
      DeviceHandle updatedHandle = new DeviceHandle();
      result = LibUsb.open(device,updatedHandle);
      if(result != LibUsb.SUCCESS)
         throw new LibUsbException("Unable to open USB new device handle", result);
      /*
      Device-side Operation -- automatic launch since device shows up as an accessory:
      user prompted for permission automatically()
      Device consantly tries to read in data and display
      */
      
      //search for endpoints
      System.out.println("Searching for endpoints...");
      ConfigDescriptor configDescriptor = new ConfigDescriptor();
      LibUsb.getActiveConfigDescriptor(device,configDescriptor);
      Interface [] infaces = configDescriptor.iface();
      Interface inface = null;
      InterfaceDescriptor setting = null;
      EndpointDescriptor epOutBulk= null;
      EndpointDescriptor epInBulk = null;
      boolean found = false;
      for(int i = 0; i < infaces.length; i++) {
         Interface infaceTemp = infaces[i];
         InterfaceDescriptor[] altSettings = infaceTemp.altsetting();
         for(int j = 0; j < altSettings.length; j++){
            InterfaceDescriptor altSettingTemp = altSettings[j];
            EndpointDescriptor[] endpoints = altSettingTemp.endpoint();
            for(int k = 0; k < endpoints.length; k++){
               EndpointDescriptor epTemp = endpoints[k];
               System.out.println(epTemp.toString().substring(0,8) + " " + k + epTemp.toString().substring(8));
               if(epTemp.bmAttributes() == LibUsb.TRANSFER_TYPE_BULK) {
                  int epAddress = (int)epTemp.bEndpointAddress();
                  if (epAddress > 0)
                     epOutBulk = epTemp;
                  else
                     epInBulk = epTemp;
               }
            }
            if(epOutBulk != null && epInBulk != null) {
               inface = infaceTemp;
               setting = altSettingTemp;
               found = true;
               System.out.println("Endpoints found");
               break;
            }
         }
         if(found)
            break;
      }
      if(!found)
         throw new LibUsbException("No suitable endpoints", -1);
     
      // Check if kernel driver must be detached
      boolean detach = (LibUsb.hasCapability(LibUsb.CAP_SUPPORTS_DETACH_KERNEL_DRIVER)) && (LibUsb.kernelDriverActive(updatedHandle,/*setting.bInterfaceNumber()*/0) == 1);
      // Detach the kernel driver
      if (detach)
      {
         result = LibUsb.detachKernelDriver(updatedHandle,/*setting.bInterfaceNumber()*/0);
         if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to detach kernel driver", result);
      }
      //send bulk transfer
      System.out.println("Attempting bulk transfer...");
      result = LibUsb.claimInterface(updatedHandle,/*setting.bInterfaceNumber()*/0);
      ByteBuffer buffer = ByteBuffer.allocateDirect(64);
      buffer.put(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 });
      IntBuffer transferred = IntBuffer.allocate(1);
      result = LibUsb.bulkTransfer(updatedHandle,epOutBulk.bEndpointAddress(),buffer,transferred, 15000);
      if (result != LibUsb.SUCCESS)
         throw new LibUsbException("Bulk transfer failed: ", result);
      else
         System.out.println("Bulk transfer successful! -> " + buffer.capacity() + " bytes sent");
      
      /*Device-side operation 
         Display data
      */
         
      //Receiving data from HiKey = OPTIONAL 
      // /*
   //       Device-side Operation (polling/interrupt transfer needed?)                                                                                                                                                                                                                                                                                    
   //       Data reception/processing/return
   //       */
   //       class DeviceDataThread extends Thread{
   //          public void run(){
   //             //run method on device-side to man/return data
   //             //polling is automatic
   //             
   //          }
   //       }
   //       DeviceDataThread d = new DeviceDataThread();
   //       d.start();
   //       //receive input data and display (check that all data is now data++) 
      
      //mop up
      // Attach the kernel driver again if needed
      if (detach)
      {
         result = LibUsb.attachKernelDriver(updatedHandle, /*setting.bInterfaceNumber()*/0);
         if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to re-attach kernel driver", result);
      }
      System.out.println("Mop up");
      result = LibUsb.releaseInterface(updatedHandle,/*setting.bInterfaceNumber()*/0);
      LibUsb.freeDeviceList(updatedList,true);
      LibUsb.close(updatedHandle);
      LibUsb.unrefDevice(device);
      LibUsb.exit(null); 
      System.out.println("Program complete");  
   }
}