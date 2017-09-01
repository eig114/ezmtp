package com.itu.ezmtp;

import be.derycke.pieter.com.COMException;
import jmtp.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by tudiyarov.i on 31.08.2017.
 */
public class MTPUtil {

    private static PortableDeviceToHostImpl32 deviceToHostImpl32 = new PortableDeviceToHostImpl32();

    public static void getFileFromMTP(String objectId, String destPath, PortableDevice pDevice) throws COMException {
        deviceToHostImpl32.copyFromPortableDeviceToHost(objectId, destPath, pDevice);
    }

    public static boolean getFileFromMTP(PortableDevice pDevice, PortableDeviceContainerObject rootObject, String sourcePath, String destPath) throws COMException {
        PortableDeviceContainerObject object = getObjectByPath(rootObject, sourcePath);
        if (object != null) {
            getFileFromMTP(object.getID(), destPath, pDevice);
            return true;
        } else return false;
    }

    public static PortableDeviceAudioObject addFileToMTP(File sourceFile, PortableDeviceContainerObject rootObject, String path) throws IOException {
        String pattern = Pattern.quote(System.getProperty("file.separator"));
        String[] splitPath = path.split(pattern);
        List<String> lst = Arrays.stream(splitPath).limit(splitPath.length - 1).collect(Collectors.toList());
        String directory = String.join(File.separator, lst);

        if (directory.isEmpty()) {
            return null;
        } else if (splitPath.length == 1) {
            return rootObject.addAudioObject(sourceFile);
        } else if (ensurePathExists(rootObject, directory)) {
            PortableDeviceContainerObject parent = getObjectByPath(rootObject, directory);
            if (parent == null) {
                return null;
            } else {
                File newFileWrapper = new File(sourceFile.getAbsolutePath()) {
                    @Override
                    public String getName() {
                        return splitPath[splitPath.length - 1];
                    }
                };
                return parent.addAudioObject(newFileWrapper);
            }
        } else return null;
    }


    public static boolean ensurePathExists(PortableDeviceContainerObject rootObject, String path) {
        String pattern = Pattern.quote(System.getProperty("file.separator"));
        String[] splitPath = path.split(pattern);

        if (splitPath.length == 0) return rootObject instanceof PortableDeviceFolderObject;

        PortableDeviceObject nextObject = Arrays.stream(rootObject.getChildObjects())
                .filter(child -> splitPath[0].equals(child.getName()))
                .findFirst()
                .orElse(rootObject.createFolderObject(splitPath[0]));

        return splitPath.length == 1 ||
                (nextObject instanceof PortableDeviceContainerObject && ensurePathExists((PortableDeviceContainerObject) nextObject, splitPath[1]));
    }

    public static PortableDeviceContainerObject getObjectByPath(PortableDeviceContainerObject rootObject, String path) {
        String pattern = Pattern.quote(System.getProperty("file.separator"));
        String[] splitPath = path.split(pattern, 2);

        if (splitPath.length == 0) return null;

        PortableDeviceObject nextObject = Arrays.stream(rootObject.getChildObjects())
                .filter(child -> splitPath[0].equals(child.getName()))
                .findFirst()
                .orElse(null);

        if (nextObject instanceof PortableDeviceContainerObject) {
            PortableDeviceContainerObject containerObject = (PortableDeviceContainerObject) nextObject;
            if (splitPath.length > 1) {
                return getObjectByPath(containerObject, splitPath[1]);
            } else return containerObject;

        } else return null;
    }
}
