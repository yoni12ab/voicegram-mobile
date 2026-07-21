package com.voicegram.app.model;

public class VoiceModel {
    private String id;
    private String content;
    
    public VoiceModel(String id, String content) {
        this.id = id;
        this.content = content;
    }
    
    public String getId() {
        return id;
    }
    
    public String getContent() {
        return content;
    }
}
