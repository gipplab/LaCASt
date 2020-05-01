package gov.nist.drmf.interpreter.mlp.data;

import gov.nist.drmf.interpreter.common.constants.Keys;

/**
 * Created by AndreG-P on 29.03.2017.
 */
public enum DLMFTranslationHeaders {
    cas("", ""),
    dlmf_comment( Keys.KEY_DLMF + "-", Keys.KEY_COMMENT_SUFFIX ),
    cas_comment( "", "-" + Keys.KEY_DLMF + Keys.KEY_COMMENT_SUFFIX ),
    cas_alternatives( "", Keys.KEY_ALTERNATIVE_SUFFX ),
    cas_branch_cuts("", "-" + Keys.FEATURE_BRANCH_CUTS ),
    cas_link("", Keys.KEY_LINK_SUFFIX ),
    cas_constraint("", "-" + Keys.FEATURE_CONSTRAINTS),
    cas_package("", Keys.KEY_EXTRA_PACKAGE_SUFFIX);

    private String pre, suf;

    DLMFTranslationHeaders( String pre, String suf ){
        this.pre = pre;
        this.suf = suf;
    }

    public String getCSVKey( String cas_prefix ){
        return pre + cas_prefix + suf;
    }

    public String getFeatureKey( String cas_prefix ){
        if (this.equals(cas_comment))
            return  cas_prefix + Keys.KEY_COMMENT_SUFFIX;
        return cas_prefix + suf;
    }
}
