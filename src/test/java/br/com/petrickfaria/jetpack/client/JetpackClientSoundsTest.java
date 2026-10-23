package br.com.petrickfaria.jetpack.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JetpackClientSoundsTest {
    @Test
    void startsResumesAndStopsOneLoopWithoutCreatingOverlappingInstances() {
        assertEquals(JetpackClientSounds.PlaybackAction.START,
                JetpackClientSounds.playbackAction(true, false, true));
        assertEquals(JetpackClientSounds.PlaybackAction.START,
                JetpackClientSounds.playbackAction(true, true, true));
        assertEquals(JetpackClientSounds.PlaybackAction.RESUME,
                JetpackClientSounds.playbackAction(true, true, false));
        assertEquals(JetpackClientSounds.PlaybackAction.FADE_OUT,
                JetpackClientSounds.playbackAction(false, true, false));
        assertEquals(JetpackClientSounds.PlaybackAction.NO_OP,
                JetpackClientSounds.playbackAction(false, false, true));
    }
}
