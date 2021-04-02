package dev.morphia.internal;

/**
 * @morphia.internal
 */
@SuppressWarnings("CheckStyle")
public final class Util {
    /**
     * @morphia.internal
     */
    public static int v40Driver = 0;

    private Util() {
    }

    /**
     * @param min   the minimum minor version of the 4.x line
     * @param block
     */
    public static void tryInvoke(int min, Runnable block) {
        if (v40Driver <= min) {
            try {
                block.run();
            } catch (NoSuchMethodError e) {
                v40Driver++;
            }
        }
    }
}
