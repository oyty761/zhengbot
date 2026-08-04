package com.guzheng.composition.model;

import java.util.List;

public record JptScore(String title, String composer, int tempo, String meter,
                       int ticks, String tuning, List<JptNote> notes) {
    public record JptNote(int t, int dur, int string, String pitch,
                          int velocity, String technique, String hand) {}
}
