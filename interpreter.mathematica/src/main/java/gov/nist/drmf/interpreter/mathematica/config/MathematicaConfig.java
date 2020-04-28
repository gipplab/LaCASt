package gov.nist.drmf.interpreter.mathematica.config;

import com.wolfram.jlink.KernelLink;
import com.wolfram.jlink.MathLinkException;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.mathematica.extension.MathematicaInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

/**
 * @author Andre Greiner-Petter
 */
public class MathematicaConfig {
    private static final Logger LOG = LogManager.getLogger(MathematicaConfig.class.getName());

    private MathematicaConfig(){}

    public static Path loadMathematicaPath(){
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(GlobalPaths.PATH_MATHEMATICA_CONFIG.toString()));
            String path = props.getProperty(Keys.KEY_MATHEMATICA_MATH_DIR);

            Path mathPath = Paths.get(path);
            Path nativePath = mathPath
                    .getParent()
                    .getParent()
                    .resolve("SystemFiles/Links/JLink/SystemFiles/Libraries/Linux-x86-64/");

//            System.setProperty("java.library.path", nativePath.toString());
//            System.setProperty("JD_LIBRARY_PATH", nativePath.toString());
//            System.out.println("Set java.library.path: " + nativePath.toString());

            Map<String,String> sysVars = getModifiableEnvironment();
            sysVars.put("LD_LIBRARY_PATH", nativePath.toString());
            System.out.println(nativePath.toString());

            return mathPath;
        } catch (IOException e) {
            LOG.fatal( "Cannot write the path into the properties file.", e );
            return null;
        } catch (Exception e) {
            LOG.fatal("Cannot tweak system environment variables at runtime.");
            return null;
        }
    }

    private static Map<String,String> getModifiableEnvironment() throws Exception{
        Class pe = Class.forName("java.lang.ProcessEnvironment");
        Method getenv = pe.getDeclaredMethod("getenv");
        getenv.setAccessible(true);
        Object unmodifiableEnvironment = getenv.invoke(null);
        Class map = Class.forName("java.util.Collections$UnmodifiableMap");
        Field m = map.getDeclaredField("m");
        m.setAccessible(true);
        return (Map) m.get(unmodifiableEnvironment);
    }

    public static void setCharacterEncoding(KernelLink engine){
        try {
            engine.evaluate("$CharacterEncoding = \"ASCII\"");
            engine.discardAnswer();
        } catch (MathLinkException mle) {
            LOG.warn("Cannot change character encoding in Mathematica.");
            engine.clearError();
            engine.newPacket();
        }
    }

    public static boolean isMathematicaPresent() {
        try {
            MathematicaInterface m = MathematicaInterface.getInstance();
            return m != null;
        } catch (Exception | Error e) {
            return false;
        }
    }
}
