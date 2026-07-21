package com.voicegram.app.manager;

import com.voicegram.app.model.VoiceModel;
import com.voicegram.app.service.VoiceService;

public class VoiceManager {
    private VoiceService voiceService;
    
    public VoiceManager() {
        this.voiceService = new VoiceService();
    }
    
    public void processVoice(VoiceModel model) {
        voiceService.processVoice();
    }
}
