package com.voicegram.app.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class VoiceModelTest {
    
    @Test
    public void testVoiceModelConstructor() {
        VoiceModel model = new VoiceModel("test-id", "test-content");
        assertEquals("test-id", model.getId());
        assertEquals("test-content", model.getContent());
    }
    
    @Test
    public void testVoiceModelGetId() {
        VoiceModel model = new VoiceModel("123", "hello");
        assertEquals("123", model.getId());
    }
    
    @Test
    public void testVoiceModelGetContent() {
        VoiceModel model = new VoiceModel("456", "world");
        assertEquals("world", model.getContent());
    }
    
    @Test
    public void testVoiceModelWithEmptyStrings() {
        VoiceModel model = new VoiceModel("", "");
        assertEquals("", model.getId());
        assertEquals("", model.getContent());
    }
}
