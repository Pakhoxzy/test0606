package com.example.blank700.data;

public class Music {
    private String musicName;
    private String musicArtist;
    private String musicPath;
    private String musicDuration;

    public Music(String musicName, String musicArtist, String musicPath, String musicDuration) {
        this.musicName = musicName;
        this.musicArtist = musicArtist;
        this.musicPath = musicPath;
        this.musicDuration = musicDuration;
    }

    public String getMusicName() {
        return this.musicName;
    }

    public void setMusicName(String musicName) {
        this.musicName = musicName;
    }

    public String getMusicArtist() {
        return this.musicArtist;
    }

    public void setMusicArtist(String musicArtist) {
        this.musicArtist = musicArtist;
    }

    public String getMusicPath() {
        return this.musicPath;
    }

    public void setMusicPath(String musicPath) {
        this.musicPath = musicPath;
    }

    public String getMusicDuration() {
        return this.musicDuration;
    }

    public void setMusicDuration(String musicDuration) {
        this.musicDuration = musicDuration;
    }

}
