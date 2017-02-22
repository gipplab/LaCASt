package gov.nist.drmf.interpreter.maple.listener;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.EngineCallBacks;

import java.util.logging.Logger;

/**
 * Created by AndreG-P on 21.02.2017.
 */
public class MapleListener implements EngineCallBacks {
    private Logger log = Logger.getLogger( MapleListener.class.toString() );

    private boolean logging;

    private boolean interrupter;

    private String proc_name;

    public MapleListener( String procedure_name, boolean logging ){
        this.proc_name = procedure_name;
        this.logging = logging;
        this.interrupter = false;
    }

    @Override
    public void textCallBack(Object o, int i, String s) throws MapleException {
        if ( logging ) {
            String str = getConstName(i) + "; Object: " + o + "; String: " + s;
            log.info(str);
        }
    }

    @Override
    public void errorCallBack(Object o, int i, String s) throws MapleException {
        if ( logging ) {
            String str = "ERROR";
            if ( i >= 0 ) str += " occurred at offset " + i;
            if ( o != null ) str += "; Object: " + o;
            str += "; " + s;
            log.severe(str);
        }
    }

    @Override
    public void statusCallBack(Object o, long l, long l1, double v) throws MapleException {
        if ( logging ) {
            String str = "Status update: " + o;
            str += "; Bytes used: " + l;
            str += "; Bytes allocated: " + l1;
            str += "; Maple-CPU-Time: " + v;
            log.info(str);
        }
    }

    @Override
    public String readLineCallBack(Object o, boolean b) throws MapleException {
        if ( logging ) {
            String str = "Cannot handle readline():";
            log.severe(str);
        }
        return null;
    }

    @Override
    public boolean redirectCallBack(Object o, String s, boolean b) throws MapleException {
        if ( logging ) {
            String str = "Cannot handle writeto or appendto!";
            log.severe(str);
        }
        return false;
    }

    @Override
    public String callBackCallBack(Object o, String s) throws MapleException {
        String call = "eval(parse(\"" + proc_name + "(ToInert('" + s + "'))\"));";
        if ( logging ) {
            String str = "Pack input into conversion: " + call;
            log.info(str);
        }

        return call;
    }

    @Override
    public boolean queryInterrupt(Object o) throws MapleException {
        if ( interrupter ) {
            if ( logging ) {
                String str = "Interrupted query!";
                log.info(str);
            }
            this.interrupter = false;
            return true;
        } else return interrupter;
    }

    @Override
    public String streamCallBack(Object o, String s, String[] strings) throws MapleException {
        if ( logging ){
            String str = "Cannot handle streams!";
            log.severe(str);
        }
        return null;
    }

    private String getConstName(int i){
        switch (i){
            case MAPLE_TEXT_DEBUG:  return "MAPLE_TEXT_DEBUG";
            case MAPLE_TEXT_DIAG:   return "MAPLE_TEXT_DIAG";
            case MAPLE_TEXT_HELP:   return "MAPLE_TEXT_HELP";
            case MAPLE_TEXT_MISC:   return "MAPLE_TEXT_MISC";
            case MAPLE_TEXT_OUTPUT: return "MAPLE_TEXT_OUTPUT";
            case MAPLE_TEXT_QUIT:   return "MAPLE_TEXT_QUIT";
            case MAPLE_TEXT_STATUS: return "MAPLE_TEXT_STATUS";
            case MAPLE_TEXT_WARNING:return "MAPLE_TEXT_WARNING";
            default: return "ERROR: Unknown status!";
        }
    }

    public void interrupt(){
        if ( logging ) log.info("Request interruption!");
        this.interrupter = true;
    }
}
