package gov.nist.drmf.interpreter.cas.mlp;

import gov.nist.drmf.interpreter.common.GlobalConstants;

import java.util.HashMap;

/**
 * @author Andre Greiner-Petter
 */
public class LineAnalyzer {
    private HashMap<String, Integer> idx_map;

    private String separator_symbol;

    private String[] values;

    private String cas_prefix;

    public LineAnalyzer( String cas_prefix, String separator_symbol, String... headers) throws NullPointerException {
        if ( headers == null ) throw new NullPointerException("Header should not be null!");
        this.separator_symbol = separator_symbol;
        idx_map = new HashMap<>();
        for ( int i = 0; i < headers.length; i++ )
            idx_map.put( headers[i], i );
        this.cas_prefix = cas_prefix; // TODO
    }

    public void setLine( String[] elements ){
        values = elements;
    }

    String getCasPrefix(){
        return cas_prefix;
    }

    public String getValue( String key ){
        try {
            String value = values[ idx_map.get(key) ];
            if ( value.startsWith(GlobalConstants.LINK_PREFIX ) )
                value = value.substring( GlobalConstants.LINK_PREFIX.length() );
            else if ( value.startsWith( GlobalConstants.LINK_S_PREFIX ) )
                value = value.substring( GlobalConstants.LINK_S_PREFIX.length() );
            return value;
        } catch ( NullPointerException | IndexOutOfBoundsException e ){
            return "";
        }
    }
}
