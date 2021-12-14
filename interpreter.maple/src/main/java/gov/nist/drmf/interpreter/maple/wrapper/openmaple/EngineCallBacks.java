package gov.nist.drmf.interpreter.maple.wrapper.openmaple;

import gov.nist.drmf.interpreter.maple.wrapper.MapleException;

import java.lang.reflect.InvocationHandler;

/**
 * This interface is essentially a copy of OpenMaple's call back interface.
 * This is to ensure that our callback classes are compatible with the OpenMaple's
 * interface.
 *
 * Compatible with Maple's 2020 OpenMaple.
 */
public interface EngineCallBacks {
    int MAPLE_TEXT_OUTPUT = 1;
    int MAPLE_TEXT_DIAG = 2;
    int MAPLE_TEXT_MISC = 3;
    int MAPLE_TEXT_HELP = 4;
    int MAPLE_TEXT_QUIT = 5;
    int MAPLE_TEXT_WARNING = 6;
    int MAPLE_TEXT_STATUS = 7;
    int MAPLE_TEXT_DEBUG = 8;

    void textCallBack(Object var1, int var2, String var3) throws MapleException;

    void errorCallBack(Object var1, int var2, String var3) throws MapleException;

    void statusCallBack(Object var1, long var2, long var4, double var6) throws MapleException;

    String readLineCallBack(Object var1, boolean var2) throws MapleException;

    boolean redirectCallBack(Object var1, String var2, boolean var3) throws MapleException;

    String callBackCallBack(Object var1, String var2) throws MapleException;

    boolean queryInterrupt(Object var1) throws MapleException;

    String streamCallBack(Object var1, String var2, String[] var3) throws MapleException;
}
