package gov.nist.drmf.interpreter.common.config;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andre Greiner-Petter
 */
public class ConfigTests {
    @Test
    void defaultGenericConfigTest() {
        GenericLacastConfig clc = GenericLacastConfig.getDefaultConfig();
        assertEquals("localhost", clc.getEsHost());
        assertEquals(9200, clc.getEsPort());
        assertEquals("dlmf-macros", clc.getMacroIndex());
        assertEquals("http://localhost:10044/texvcinfo", clc.getMathoidUrl());
    }

    @Test
    void loadCASSupportTest() {
        CASSupporter supporter = CASSupporter.getSupportedCAS();
        List<String> cas = supporter.getAllCAS();
        assertTrue( cas.size() >= 2 );
        Collections.sort(cas);
        assertEquals( "Maple", cas.get(0) );
        assertEquals( "Mathematica", cas.get(1) );
    }
}
