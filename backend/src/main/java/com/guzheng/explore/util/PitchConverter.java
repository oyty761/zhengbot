package com.guzheng.explore.util;

public final class PitchConverter {

    private static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    private PitchConverter() {
    }

    public static String midiToPitch(int midiNote) {
        if (midiNote < 0 || midiNote > 127) {
            return null;
        }
        int octave = (midiNote / 12) - 1;
        int noteIndex = midiNote % 12;
        return NOTE_NAMES[noteIndex] + octave;
    }
}
