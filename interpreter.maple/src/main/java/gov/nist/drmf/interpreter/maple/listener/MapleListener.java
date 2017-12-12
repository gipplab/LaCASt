package gov.nist.drmf.interpreter.maple.listener;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.EngineCallBacks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by AndreG-P on 21.02.2017.
 */
public class MapleListener implements EngineCallBacks {
    private Logger log = LogManager.getLogger( MapleListener.class.toString() );

    private boolean logging;

    private boolean interrupter;
    private boolean auto_interrupt;
    private double auto_interrupt_threshold;
    private double last_process_time;

    public MapleListener( boolean logging ){
        this.logging = logging;
        this.interrupter = false;
        this.auto_interrupt = false;
    }

    @Override
    public void textCallBack(Object o, int i, String s) throws MapleException {
        if ( logging ) {
            String str = getConstName(i) + "; Object: " + o + "; String: " + s;
            log.debug(str);
        }
    }

    @Override
    public void errorCallBack(Object o, int i, String s) throws MapleException {
        if ( logging ) {
            String str = "ERROR[";
            str += i >= 0 ? "Offset: " + i + "; ": "";
            str += o != null ? "Object: " + o + "; ": "";
            str += s + "]";
            log.error(str);
        }
    }

    @Override
    public void statusCallBack(Object o, long l, long l1, double v) throws MapleException {
        if ( auto_interrupt && last_process_time < 0 ){
            last_process_time = v;
        }

        if ( logging ) {
            String str = "STATUS_UPDATE[";
            str += o != null ? "Object: " + o + "; " : "";
            str += "Usage: " + l + "KB; ";
            str += "System Allocation: " + l1 + "KB; ";
            str += "Maple-CPU-Time: " + v + "sec]";
            log.info(str);
        }

        if ( auto_interrupt && (v - last_process_time > auto_interrupt_threshold) ){
            log.debug("Exceed auto interrupt threshold!");
            last_process_time = -1;
            interrupt();
        }
    }

    @Override
    public String readLineCallBack(Object o, boolean b) throws MapleException {
        if ( logging ) {
            String str = "Cannot handle readline():";
            log.warn(str);
        }
        return null;
    }

    @Override
    public boolean redirectCallBack(Object o, String s, boolean b) throws MapleException {
        if ( logging ) {
            String str = "Cannot handle writeto or appendto!";
            log.warn(str);
        }
        return false;
    }

    @Override
    public String callBackCallBack(Object o, String s) throws MapleException {
        if ( logging ) {
            String str = "Stopped callback! This program doesn't allows callbacks. " +
                    "Ask Jürgen Gerhard, if you can't believe it!";
            log.debug(str);
        }
        return null;
    }

    @Override
    public boolean queryInterrupt( Object o ) throws MapleException {
        if ( logging ) log.debug("Check Query Interruption Request (" + interrupter + ").");
        if ( interrupter ) {
            if ( logging ) {
                String str = "Interrupted query!";
                log.debug(str);
            }
            this.interrupter = false;
            return true;
        } else return interrupter;
    }

    @Override
    public String streamCallBack(Object o, String s, String[] strings) throws MapleException {
        if ( logging ){
            String str = "Cannot handle streams!";
            log.warn(str);
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
        if ( logging ) log.debug("Request interruption!");
        this.interrupter = true;
    }

    public void activateAutoInterrupt( double threshold ){
        this.auto_interrupt = true;
        this.auto_interrupt_threshold = threshold;
    }

    public void deactivateAutoInterrupt(){
        this.auto_interrupt = false;
        this.auto_interrupt_threshold = 0;
    }

    public void timerReset(){
        this.last_process_time = -1;
    }
}
