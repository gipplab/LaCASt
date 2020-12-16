package gov.nist.drmf.interpreter.pom.data;

import gov.nist.drmf.interpreter.common.constants.Keys;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * The headers of translation files (such as in DLMF_Maple.csv).
 */
public enum ForwardTranslationFileHeaders implements FeatureFiller {
    DLMF(true, (c) -> Keys.KEY_DLMF),
    TRANSLATION(false, (c) -> c),
    ALTERNATIVE_TRANSLATIONS(false, (c) -> c + Keys.KEY_ALTERNATIVE_SUFFX),
    COMMENT(false, (c) -> Keys.KEY_DLMF + c + Keys.KEY_COMMENT_SUFFIX),
    PACKAGE(false, (c) -> c + Keys.KEY_EXTRA_PACKAGE_SUFFIX);

    private final boolean mandatory;
    private final InfoHeader infoHeader;

    ForwardTranslationFileHeaders(boolean mandatory, InfoHeader infoHeader) {
        this.infoHeader = infoHeader;
        this.mandatory = mandatory;
    }

    @Override
    public boolean isMandatory() {
        return mandatory;
    }

    @Override
    public String getKey(String cas) {
        return infoHeader.getKey(cas);
    }

    @Override
    public Stream<FeatureFiller> allValues() {
        return Arrays.stream(ForwardTranslationFileHeaders.values());
    }
}
