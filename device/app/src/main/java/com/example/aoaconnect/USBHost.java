package com.example.aoaconnect;

import org.usb4java.*;
import java.nio.*;

public class UsbHost {
    public static void main(String[] args) {
        UsbHost host = new UsbHost();
        try {
            host.init();
            host.findDevice();
            if(host.accessoryMode())
                host.findAccessoryDevice();

        } catch (LibUsbException e) {
            e.printStackTrace();
        }
    }

    private Device device;
    private DeviceDescriptor descriptor;
    private DeviceHandle handle;
    private DeviceList list;
    private EndpointDescriptor epOut;
    private EndpointDescriptor epIn;

    private DeviceHandle updatedHandle;
    private Device

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

    public boolean accessoryMode() {
        boolean accessoryMode = false;
        int attempts = 1;
        while (!accessoryMode && attempts <= 2) {
            if ((descriptor.idVendor() == IDVENDOR) && (descriptor.idProduct() == IDPRODUCT || descriptor.idProduct() == ALTIDPRODUCT)) {
                accessoryMode = true;
                System.out.println("Device in accessory mode");
                break;
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
        if(accessoryMode)
            System.out.println("Device in accessory mode");
        return accessoryMode;
    }
    public void findAccessoryDevice(){
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

    }
}
