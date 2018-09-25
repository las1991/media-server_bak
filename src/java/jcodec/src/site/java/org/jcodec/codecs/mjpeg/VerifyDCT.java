package org.jcodec.codecs.mjpeg;

import java.util.Arrays;

import org.jcodec.common.dct.DCT;
import org.jcodec.common.dct.IntDCT;
import org.jcodec.common.dct.SlowDCT;
import org.jcodec.common.tools.Debug;

public class VerifyDCT extends DCT {

    private static SlowDCT slow = SlowDCT.INSTANCE;
    private static IntDCT fast = IntDCT.INSTANCE;

    static int diffcnt = 0;

    public int[] decode(int[] orig) {
        int[] expected = slow.decode(orig);
        int[] actual = fast.decode(orig);
        if (!Arrays.equals(expected, actual)) {
            System.out.println("\nwhile decoding: ");
            Debug.print8x8(orig);
            System.out.println("expected: ");
            Debug.print8x8(expected);
            System.out.println("actual: ");
            Debug.print8x8(actual);
            System.out.println("diff: ");
            for (int i = 0; i < expected.length; i++) {
                if (i % 8 == 0) {
                    System.out.println();
                }
                System.out.printf("%3d, ", (expected[i] - actual[i]));
            }
            diffcnt++;

            if (diffcnt == 10) {
                throw new IllegalStateException();
            }
        }
        return expected;
    }

}
