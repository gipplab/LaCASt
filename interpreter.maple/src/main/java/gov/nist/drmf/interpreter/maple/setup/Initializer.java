package gov.nist.drmf.interpreter.maple.setup;

import java.lang.reflect.Field;

/**
 * Created by AndreG-P on 17.02.2017.
 */
public class Initializer {
    private static final String MAPLE_BIN = "C:\\Program Files\\Maple 2017\\bin.X86_64_WINDOWS";
    private static final String java_lib_path = "java.library.path";

    /**
     * TODO test and fix for Linux
     * Hacky version to load Maple's native libraries.
     * Probably won't work on Linux systems!
     *
     * @return true if everything went fine.
     */
    public static boolean loadMapleNatives() {
        try {
            System.setProperty( java_lib_path, MAPLE_BIN );
            Field field = ClassLoader.class.getDeclaredField("sys_paths");
            field.setAccessible(true);
            field.set(null, null);
            return true;
        } catch ( NoSuchFieldException | IllegalAccessException e ){
            e.printStackTrace();
            return false;
        }
    }

}
