/*
 * Copyright 2007 Pieter De Rycke
 * 
 * This file is part of JMTP.
 * 
 * JTMP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or any later version.
 * 
 * JMTP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU LesserGeneral Public 
 * License along with JMTP. If not, see <http://www.gnu.org/licenses/>.
 */

package jmtp;

import be.derycke.pieter.com.COM;
import be.derycke.pieter.com.COMException;
import be.derycke.pieter.com.COMReference;
import com.itu.utils.NativeUtils;

import java.io.IOException;
import java.util.*;

/**
 * TODO phantomreferences -> gebruiken
 * @author Pieter De Rycke
 */
class PortableDeviceManagerImplWin32 implements PortableDeviceManagerProxy {
    
    static {
//    	TODO load correct version of the dll
    	switch (System.getProperty("os.arch")) {
		case "x86":
            try{
                System.loadLibrary("jmtp32");
            }
            catch (UnsatisfiedLinkError unused){
                try {
                    NativeUtils.loadLibraryFromJar("/jmtp32.dll");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
			break;
		case "amd64":
		    try{
                System.loadLibrary("jmtp64");
            }
            catch (UnsatisfiedLinkError unused){
                try {
                    NativeUtils.loadLibraryFromJar("/jmtp64.dll");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

			break;
		default:
			System.err.println("unknown arch: "+System.getProperty("os.arch"));
			System.exit(-1);
			break;
		}


    }
    
    private COMReference pDeviceManager;
    
    private Map<String, PortableDeviceImplWin32> deviceMap;
    
    public PortableDeviceManagerImplWin32() {
        try {
            pDeviceManager = COM.CoCreateInstance(WPDImplWin32.CLSID_PortableDeviceManager, 
                    0, COM.CLSCTX_INPROC_SERVER, WPDImplWin32.IID_IPortableDeviceManager);
            deviceMap = new HashMap<String, PortableDeviceImplWin32>();
        }
        catch (Exception e) {
            throw new RuntimeException("probleem met de com");
        }
    }
    
    private native String[] getDevicesImpl() throws COMException;
    
    private native void refreshDeviceListImpl() throws COMException;
    
    public PortableDeviceImplWin32[] getDevices() {
        try {
            String[] devices = getDevicesImpl();

            Set<String> deviceSet = new HashSet<String>();
            for (String deviceID : devices) {
                if(!deviceMap.containsKey(deviceID))
                    deviceMap.put(deviceID, new PortableDeviceImplWin32(pDeviceManager, deviceID));
                              
                deviceSet.add(deviceID);
            }
            
            for(String deviceID : deviceMap.keySet())
                if(!deviceSet.contains(deviceID))
                    deviceMap.remove(deviceID);
            
            return deviceMap.values().toArray(new PortableDeviceImplWin32[0]);
        }
        catch (COMException e) {
            e.printStackTrace();
            return new PortableDeviceImplWin32[0];
        }
    }
    
    public void refreshDeviceList() {
        try {
            refreshDeviceListImpl();
        }
        catch(COMException e) {}
    }

    public Iterator<PortableDevice> iterator() {
        return new PortableDeviceIterator();
    }
    
    private class PortableDeviceIterator implements Iterator<PortableDevice> {
        
        private Iterator<String> iterator;
        
        public PortableDeviceIterator() {
            getDevices();   //de map met com.itu.ezmtp.getDevices updaten
            iterator = deviceMap.keySet().iterator();
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public PortableDevice next() {
            String deviceID = iterator.next();
            return deviceMap.get(deviceID);
        }

        public void remove() {
            throw new UnsupportedOperationException("Devices can't be removed");
        }
        
    }
}
