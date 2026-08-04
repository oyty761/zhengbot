package com.guzheng.composition.service;

import com.guzheng.composition.model.JptScore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JptCodecTest {
    private final JptCodec codec=new JptCodec();
    private static final String VALID="""
            JPT 1.0
            META title="小星星 片段" composer="佚名" tempo=90 meter=4/4 ticks=480 tuning=D-pentatonic
            NOTE t=0 dur=480 string=16 pitch=D3 velocity=80 technique=pluck hand=R
            NOTE t=480 dur=480 string=18 pitch=A2 velocity=76 technique=pluck hand=L
            END
            """;

    @Test void roundTripsWithoutChangingNotes(){JptScore first=codec.parse(VALID);JptScore second=codec.parse(codec.write(first));assertEquals("小星星 片段",second.title());assertEquals(first.notes(),second.notes());}
    @Test void rejectsPitchThatDoesNotMatchString(){String bad=VALID.replace("string=16 pitch=D3","string=16 pitch=D6");assertThrows(IllegalArgumentException.class,()->codec.parse(bad));}
    @Test void rejectsRecordsAfterEnd(){assertThrows(IllegalArgumentException.class,()->codec.parse(VALID+"NOTE t=0 dur=1 string=1\n"));}
}
