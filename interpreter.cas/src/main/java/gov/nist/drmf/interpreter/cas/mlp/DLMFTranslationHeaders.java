package gov.nist.drmf.interpreter.cas.mlp;

import gov.nist.drmf.interpreter.common.Keys;

/**
 * Created by AndreG-P on 29.03.2017.
 */
public enum DLMFTranslationHeaders {
    dlmf_comment( Keys.KEY_DLMF, Keys.KEY_COMMENT_SUFFIX ),
    cas_alternatives( "", Keys.KEY_ALTERNATIVE_SUFFX ),
    cas_branch_cuts("", "-" + Keys.FEATURE_BRANCH_CUTS ),
    cas_link("", Keys.KEY_LINK_SUFFIX ),
    cas_constraint("", "-" + Keys.FEATURE_CONSTRAINTS);

    private String pre, suf;

    DLMFTranslationHeaders( String pre, String suf ){
        this.pre = pre;
        this.suf = suf;
    }

    String getKey( String cas_prefix ){
        return pre + cas_prefix + suf;
    }
}
