package com.gss.countrycodepicker;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

public class FileUtils {

    public static String getSmsCountryCodeList(Context context) {
        String filename = "smsCountryCodeList";
        return context.getCacheDir() + File.separator + "readHistories" + File.separator + filename;
    }

    /**
     * 获取字符串缓存
     */
    public static String getStringCache(String cachePath) {
        try {
            return getFromCache(cachePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getFromCache(String cachePath) throws Exception {
        String res;

        FileInputStream fis = new FileInputStream(cachePath);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int i;
        byte[] b = new byte[1024];
        while ((i = fis.read(b)) != -1) {
            bos.write(b, 0, i);
        }

        res = bos.toString();

        bos.close();
        fis.close();

        return res;
    }
}
