/*
 * yidingliu.com Inc. * Copyright (c) 2016 All Rights Reserved.
 */

package com.yidingliu.dev.knowldegelibrary.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Md5网络参数加密专用
 *
 * @author hzm
 * @Date 2016/9/28 0028
 * @modifyInfo1 Administrator-2016/9/28 0028
 * @modifyContent
 */
public class Md5Utils {



    protected static char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6',
                                          '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    protected static MessageDigest messagedigest = null;
    static {
        try {
            messagedigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsaex) {
            System.err.println(Md5Utils.class.getName()
                                       + "初始化错误");
            nsaex.printStackTrace();
        }
    }

    public static String getFileMD5String (File file ) throws IOException {
        FileInputStream in = new FileInputStream( file);
        FileChannel     ch = in.getChannel ();
        MappedByteBuffer byteBuffer = ch.map ( FileChannel.MapMode.READ_ONLY, 0,
                                               file.length() );
        messagedigest.update(byteBuffer);
        in.close();
        return bufferToHex(messagedigest.digest()).toLowerCase();
    }

    public static String getMD5String(String s) {
        return getMD5String(s.getBytes()).toLowerCase();
    }

    public static String getMD5String(byte[] bytes) {
        messagedigest.update(bytes);
        return bufferToHex(messagedigest.digest()).toLowerCase();
    }

    private static String bufferToHex(byte bytes[]) {
        return bufferToHex(bytes, 0, bytes.length).toLowerCase();
    }

    private static String bufferToHex(byte bytes[], int m, int n) {
        StringBuffer stringbuffer = new StringBuffer(2 * n);
        int k = m + n;
        for (int l = m; l < k; l++) {
            appendHexPair(bytes[l], stringbuffer);
        }
        return stringbuffer.toString().toLowerCase();
    }

    private static void appendHexPair(byte bt, StringBuffer stringbuffer) {
        char c0 = hexDigits[(bt & 0xf0) >> 4];
        char c1 = hexDigits[bt & 0xf];
        stringbuffer.append(c0);
        stringbuffer.append(c1);
    }

    public static boolean checkPassword(String password, String md5PwdStr) {
        String s = getMD5String(password);
        return s.equals(md5PwdStr);
    }

}
