package com.example.aoaconnect;

import org.usb4java.*;
import java.nio.*;

public class UsbHost{
   class DataThread extends Thread{
      //volatile data type renders application thread-safe; in multithreading, multiple threads modify copies of same variables such that other threads may be unaware of such changes leading to data 
      //inconsistency but if data is volatile it automatically reflects changes made in other threads
      private volatile boolean abort;
      private UsbHost host;
      public void abort(){
         this.abort = true;
      }    
      
      @Override
      public void run(){
         host = new UsbHost();
         while(!this.abort){
            int result = LibUsb.handleEventsTimeout(null,250000);
            if(result < 0)
               throw new LibUsbException("Unable to handle events",result);
            ByteBuffer buffer = BufferUtils.allocateByteBuffer(8);
            buffer.put(new byte[] {1,2,3,4,5,6,7,8});
            Transfer transfer = LibUsb.allocTransfer();
            TransferCallback callback = new TransferCallback(){
               @Override
               public void processTransfer(Transfer transfer){
                  System.out.println(transfer.actualLength() + " bytes sent");
                  LibUsb.freeTransfer(transfer);
               }
            };
            LibUsb.fillBulkTransfer(transfer,host.getHandle(),host.getEpOut().bEndpointAddress(),buffer,callback,null,5000);
            result = LibUsb.submitTransfer(transfer);
            if(result < 0)
               throw new LibUsbException("Unable to submit transfer",result);
         }
      } 
   }
   
   public static void main(String[] args) {
      UsbHost host = new UsbHost();
      try {
         //find device,force device into accessory mode,find device in accessory mode,update variables(device,descriptor,handle) to relate to accessory device
         host.init();
         host.findDevice();
         host.accessoryMode();
         /*
      Device-side Operation -- automatic launch since device shows up as an accessory:
      user prompted for permission automatically
      Device consantly tries to read in data and display (and write back)
      */
         host.findEndpoints();
         //host.bulkTransfer();
         dt.start();
         host.mopUp();
      } catch (Exception e) {
         e.printStackTrace();
      }
      
   }

   private Device device;
   private DeviceDescriptor descriptor;
   private DeviceHandle handle;
   private DeviceList list;
   private EndpointDescriptor epOut;
   private EndpointDescriptor epIn;
   
   private static DataThread dt;

   private final int IDVENDOR = 0X18D1;
   private final int IDPRODUCT = 0X2D00;
   private final int ALTIDPRODUCT = 0X2D01;
   private final String MANUFACTURER = "LENOVO";
   private final String MODEL = "20EN0017US";
   private final String DESCRIPTION = "HAL9000";
   private final String VERSION = "N1EET79W"; //BIOS version
   private final String URI = "50-7B-9D-CF-3B-80";//MAC address
   private final String SERIALNUMBER = "PC0CGX2Q";

   public UsbHost() {
      device = null;
      descriptor = new DeviceDescriptor();
      handle = new DeviceHandle();
      list = new DeviceList();
      
      dt = new DataThread();
   }
   public DeviceHandle getHandle(){
      return this.handle;
   }
   
   public EndpointDescriptor getEpOut(){
      return this.epOut;
   }
   public void init() throws LibUsbException {
      int result = LibUsb.init(null);
      if (result != LibUsb.SUCCESS)
         throw new LibUsbException("Unable to initialize libusb", result);
      System.out.println("LibUsb session initialized\n");
   }

   public void findDevice() throws LibUsbException {
      int result = LibUsb.getDeviceList(null, list);
      if (result < 0)
         throw new LibUsbException("Unable to get device list", result);
      for (Device d : list) {
         DeviceDescriptor descriptor = new DeviceDescriptor();
         result = LibUsb.getDeviceDescriptor(d, descriptor);
         if (result != LibUsb.SUCCESS)
            throw new LibUsbException("Unable to read device descriptor", result);
         if (descriptor.idVendor() == 0x18D1) {
            device = d;
            break;
         }
      }
      if (device == null)
         throw new LibUsbException("No appropriate devices connected", -1);
      else
         System.out.println("Device found: " + device.toString());
   
      result = LibUsb.getDeviceDescriptor(device, descriptor);
      if (result < 0)
         throw new LibUsbException("Unable to get DeviceDescriptor", result);
      result = LibUsb.open(device, handle);
      if (result != LibUsb.SUCCESS)
         throw new LibUsbException("Unable to open USB device handle", result);
   
      System.out.print("Device found\nDeviceDescriptor retrieved\nHandle opened\n");
   }

   public void accessoryMode() {
      //put hikey into accessory mode and update device, descriptor, and handle variables
      boolean accessoryMode = false;
      int attempts = 1;
      while (!accessoryMode && attempts <= 2) {
         if ((descriptor.idVendor() == IDVENDOR) && (descriptor.idProduct() == IDPRODUCT || descriptor.idProduct() == ALTIDPRODUCT)) {
            accessoryMode = true;
            System.out.println("Device in accessory mode");
            updateVars();
            return;
         } else {
            ByteBuffer protocol = ByteBuffer.allocateDirect(2);
            int result = LibUsb.controlTransfer(handle, (byte) 192, (byte) 51, (short) 0, (short) 0, protocol, (long) 5000);
            if (result < 0)
               throw new LibUsbException("GetProtocol control transfer failed", result);
            if (protocol.get(0) + protocol.get(1) > 0) {
               //CR52
               //manufacturer
               ByteBuffer hostMetadata = ByteBuffer.allocateDirect(64);
               hostMetadata.put(MANUFACTURER.getBytes());
               result = LibUsb.controlTransfer(handle, (byte) 64, (byte) 52, (short) 0, (short) 0, hostMetadata, (long) 5000);
               if (result < 0)
                  throw new LibUsbException("Manufacturer metadata control transfer failed", result);
            
               //model
               hostMetadata.clear();
               hostMetadata.put(MODEL.getBytes());
               result = LibUsb.controlTransfer(handle, (byte) 64, (byte) 52, (short) 0, (short) 1, hostMetadata, (long) 5000);
               if (result < 0)
                  throw new LibUsbException("Model metadata control transfer failed", result);
               //description
               hostMetadata.clear();
               hostMetadata.put(DESCRIPTION.getBytes());
               result = LibUsb.controlTransfer(handle,(byte)64, (byte)52, (short)0, (short)2,hostMetadata,(long)5000);
               if (result < 0)
                  throw new LibUsbException("Description metadata control transfer failed", result);
               //version
               hostMetadata.clear();
               hostMetadata.put(VERSION.getBytes());
               LibUsb.controlTransfer(handle,(byte)64, (byte)52, (short)0, (short)3,hostMetadata,(long)5000);
               if (result < 0)
                  throw new LibUsbException("Version metadata control transfer failed", result);
               //URI
               hostMetadata.clear();
               hostMetadata.put(URI.getBytes());
               LibUsb.controlTransfer(handle,(byte)64, (byte)52, (short)0, (short)4,hostMetadata,(long)5000);
               if (result < 0)
                  throw new LibUsbException("URI metadata control transfer failed", result);
               //serial number
               hostMetadata.clear();
               hostMetadata.put(SERIALNUMBER.getBytes());
               result = LibUsb.controlTransfer(handle,(byte)64, (byte)52, (short)0, (short)5,hostMetadata,(long)5000);
               if (result < 0)
                  throw new LibUsbException("Serial number metadata control transfer failed", result);
               //restart in accessory mode(CR53)
               ByteBuffer empty = ByteBuffer.allocateDirect(0);
               System.out.println("Device attempting to restart in accessory mode...");
               result = LibUsb.controlTransfer(handle, (byte)64, (byte)53, (short)0,(short)0,empty,(long)5000);
               if(result < 0)
                  throw new LibUsbException("Accessory mode restart failed",result);
            
            }
         }
         attempts++;
      }
      
   }
   
   //helper method: find accessory device and update device,descriptor, and handle variables
   private void updateVars(){
      DeviceList updatedList = new DeviceList();
      int result = LibUsb.getDeviceList(null,updatedList);
      if(result < 0)
         throw new LibUsbException("Unable to retrieve updated device list",result);
   
      for(Device d : updatedList){
         LibUsb.getDeviceDescriptor(d,descriptor);
         //System.out.println("DEVICE: \nIdVendor: " + descriptor.idVendor() + "\nProductID: " + descriptor.idProduct() + device.toString() + "\n");
         if ((descriptor.idVendor() == IDVENDOR) && (descriptor.idProduct() == IDPRODUCT || descriptor.idProduct() == ALTIDPRODUCT)){
            device = d;
            break;
         }
      }
      System.out.println("Accessory mode device found");
      LibUsb.freeDeviceList(list,true);
      LibUsb.close(handle);
      
      //create new descriptor/handle
      DeviceDescriptor accDescriptor = new DeviceDescriptor();
      descriptor = accDescriptor;
      result = LibUsb.getDeviceDescriptor(device, descriptor);
      if(result < 0)
         throw new LibUsbException("Unable to pair descriptor to accessory device",result);
      DeviceHandle accHandle = new DeviceHandle();
      handle = accHandle;
      result = LibUsb.open(device,handle);
      if(result < 0)
         throw new LibUsbException("Unable to pair handle with accessory device",result);
   }
   
   public void findEndpoints(){
      System.out.println("Searching for endpoints...");
      ConfigDescriptor configDescriptor = new ConfigDescriptor();
      LibUsb.getActiveConfigDescriptor(device,configDescriptor);
      Interface [] infaces = configDescriptor.iface();
      Interface inface = null;
      InterfaceDescriptor setting = null;
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
                     epOut = epTemp;
                  else
                     epIn = epTemp;
               }
            }
            if(epOut != null && epIn != null) {
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
         throw new LibUsbException("No suitable endpoints found", -1);
         
     //detach kernel driver to make endpoints available
      boolean detach = (LibUsb.hasCapability(LibUsb.CAP_SUPPORTS_DETACH_KERNEL_DRIVER)) && (LibUsb.kernelDriverActive(handle,/*setting.bInterfaceNumber()*/0) == 1);
      if (detach){
         int result = LibUsb.detachKernelDriver(handle,/*setting.bInterfaceNumber()*/0);
         if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to detach kernel driver", result);
      }
   }
   
   //synchronous
   // public void bulkTransfer(){
//       System.out.println("Attempting bulk transfer...");
//       int result = LibUsb.claimInterface(handle,/*setting.bInterfaceNumber()*/0);
//       ByteBuffer buffer = ByteBuffer.allocateDirect(64);
//       buffer.put(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 });
//       IntBuffer transferred = IntBuffer.allocate(1);
//       result = LibUsb.bulkTransfer(handle,epOut.bEndpointAddress(),buffer,transferred, 15000);
//       if (result != LibUsb.SUCCESS)
//          throw new LibUsbException("Bulk transfer failed: ", result);
//       else
//          System.out.println("Bulk transfer successful! -> " + buffer.capacity() + " bytes sent");   
//      
//      //reattach kernel driver if necessary
//       boolean detach = (LibUsb.hasCapability(LibUsb.CAP_SUPPORTS_DETACH_KERNEL_DRIVER)) && (LibUsb.kernelDriverActive(handle,/*setting.bInterfaceNumber()*/0) == 1);
//       if (detach)
//       {
//          result = LibUsb.attachKernelDriver(handle, /*setting.bInterfaceNumber()*/0);
//          if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to re-attach kernel driver", result);
//       }
//    }   
   public void mopUp() throws Exception{
      System.out.println("Mop up");
      int result = LibUsb.releaseInterface(handle,/*setting.bInterfaceNumber()*/0);
      if(result < 0)
         throw new LibUsbException("Interface release failed",result);
      dt.abort();
      dt.join();
      LibUsb.freeDeviceList(list,true);
      LibUsb.close(handle);
      LibUsb.unrefDevice(device);
      LibUsb.exit(null); 
   }
}
