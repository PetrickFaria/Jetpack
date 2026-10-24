package br.com.petrickfaria.jetpack.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JetpackClientSoundsTest {
    @Test void reconcilesMissingLoopsWithoutRestartingAnActiveLoop() {
        org.junit.jupiter.api.Assertions.assertFalse(JetpackClientSounds.shouldRestartLoop(0, false));
        org.junit.jupiter.api.Assertions.assertFalse(JetpackClientSounds.shouldRestartLoop(39, false));
        org.junit.jupiter.api.Assertions.assertTrue(JetpackClientSounds.shouldRestartLoop(40, false));
        org.junit.jupiter.api.Assertions.assertFalse(JetpackClientSounds.shouldRestartLoop(400, true));
    }

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
