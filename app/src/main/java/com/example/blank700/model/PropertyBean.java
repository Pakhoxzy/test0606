package com.example.blank700.model;

import android.content.Context;

import com.example.blank700.musicplayer.R;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class PropertyBean {
    public static String[] THEMES;
    private static String DEFAULT_THEME;

    private Context context;

    private String theme;

    public PropertyBean(Context context){
        this.context=context;
        THEMES=context.getResources().getStringArray(R.array.theme);
        DEFAULT_THEME=THEMES[0];
        this.loadTheme();
    }

    //读取主题，保存在configuration.cfg中
    private void loadTheme(){
        Properties properties=new Properties();
        try {
            FileInputStream stream=context.openFileInput("configuration.cfg");
            properties.load(stream);
            theme=properties.getProperty("theme").toString();

        } catch (Exception e){
            this.saveTheme(DEFAULT_THEME);
        }
    }

    //保存主题，保存在configuration.cfg中
    private boolean saveTheme(String theme){
        Properties properties=new Properties();
        properties.put("theme",theme);
        try {
            FileOutputStream stream=context.openFileOutput("configuration.cfg",context.MODE_PRIVATE);
            properties.store(stream," ");
            return true;
        }catch (Exception e){
            return false;
        }
    }

    public void setAndSaveTheme(String theme){
        this.theme=theme;
        this.saveTheme(theme);
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }
}
