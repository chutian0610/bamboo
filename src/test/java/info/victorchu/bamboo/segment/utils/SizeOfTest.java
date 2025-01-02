package info.victorchu.bamboo.segment.utils;

import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.ClassLayout;

import static info.victorchu.bamboo.segment.utils.SizeOf.instanceSize;
import static java.lang.Math.toIntExact;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class SizeOfTest
{
    @Test
    public void test()
    {
        // Generally, instance size delegates to JOL
        assertThat(instanceSize(BasicClass.class))
                .isEqualTo(toIntExact(ClassLayout.parseClass(BasicClass.class).instanceSize()));
    }

    @SuppressWarnings("unused")
    private static class BasicClass
    {
        private String a;
        private int b;
        private long c;
        private boolean d;
    }
}