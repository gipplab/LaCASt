package gov.nist.drmf.interpreter.common;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class provides some global constants.
 *
 * Created by Andre Greiner-Petter on 02.11.2016.
 */
public class GlobalConstants {
    // path variable to the ReferenceData directory
    public static final Path REFERENCE_DATA_PATH =
            Paths.get("libs", "ReferenceData");

}
