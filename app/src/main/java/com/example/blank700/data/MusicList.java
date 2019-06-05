package com.example.blank700.data;

import java.util.ArrayList;

public class MusicList {
    private static ArrayList<Music> musicArray = new ArrayList<Music>();

    private MusicList() {
    }

    public static ArrayList<Music> getMusicList() {
        return musicArray;
    }
}
