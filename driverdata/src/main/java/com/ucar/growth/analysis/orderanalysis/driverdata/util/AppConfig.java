package com.ucar.growth.analysis.orderanalysis.driverdata.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by zfx on 2016/10/10.
 */
public class AppConfig {
    private static AppConfig ourInstance = new AppConfig();

    public static AppConfig getInstance() {
        return ourInstance;
    }
    private Properties properties;

    private AppConfig() {
        properties = new Properties();
        try {
            InputStream in = new BufferedInputStream(getClass()
                    .getClassLoader()
                    .getResourceAsStream("Config.properties"));
            properties.load(in);
        }catch (IOException e){
            e.printStackTrace();
        }

    }
    public String get(String key){
        return properties.getProperty(key);
    }

    public static void main(String[] args){
        AppConfig appConfig = AppConfig.getInstance();
        System.out.println(appConfig.get("appName"));
    }
}
