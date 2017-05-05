package gov.nist.drmf.interpreter.maple;

import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.GlobalPaths;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by AndreG-P on 28.02.2017.
 */
public class QuickTests {
    @Disabled
    @Test
    public void arraysTest(){
        String[] empty = new String[]{};
        assertEquals("[]", Arrays.toString(empty), "Not equal string!" );
        System.out.println("blabla:".split(":").length);
    }

    @Disabled
    @Test
    public void streamTest(){
        //Path p = Paths.get("interpreter.maple", "src", "test", "resources", "Test.txt");

        Path p = GlobalPaths.PATH_REFERENCE_DATA.resolve("Test.txt");
        try (BufferedReader reader = Files.newBufferedReader( p )){
            reader.lines()
                    .forEach( l -> {
                        try { innerReaderTest(reader, 3, l); }
                        catch( IOException ioe ){
                            System.err.println("Error in line " + l);
                        }
                    } );
        } catch ( IOException ioe ){
            ioe.printStackTrace();
        }
    }

    private void innerReaderTest( BufferedReader reader, int length, String line )
            throws IOException{
        System.out.println(line);
        reader.readLine();
        reader.readLine();
    }

    @Disabled
    @Test
    public void modTest(){
        String[] t = new String[]{"a", "b", "c"};
        for ( int i = 0; i < t.length; i++ ){
            int n = (i+1)%t.length;
            System.out.println(t[i] + " = " + t[n]);
        }
    }

    @Test
    public void replace(){
        String s = "$s \\neq 0,1$; $\\realpart{a} > 0$.";
        s = s.replaceAll("[.$]*","");
        System.out.println(s);
    }

    private enum TENUM{
        T(0);

        int n;

        TENUM(int n){
            this.n = n;
        }
    }

    @Test
    public void list(){
        represent = new LinkedList<>();
        lines = new LinkedList<>();
        num = 0;
        for ( int i = 8; i < 22; i++)
            add(i);
        System.out.println(lines);
        System.out.println(represent);
    }

    private int num;
    private LinkedList<Integer> lines;
    private LinkedList<String> represent;

    public void add( int line ){
        num++;
        lines.add(line);
        if ( represent.isEmpty() ){
            represent.add(""+line);
            return;
        }

        String l = represent.getLast();
        if ( l != null && !l.isEmpty() && l.endsWith(""+(line-1)) ){
            represent.removeLast();
            if ( l.contains("-") ){
                String end = (line-1)+"";
                l = l.substring(0, l.length()-end.length());
                l += line;
            } else l += "-" + line;
            represent.addLast(l);
        } else represent.addLast(""+line);
    }

    @Test
    public void singleQuote(){
        String w = "diff($0, [$1$$2]), $0";
        for ( int i = 0; i < 3; i++ ){
            w = w.replace("$"+i,"x");
            System.out.println( w );
        }
    }

    @Test
    public void hardcoreSplitter(){
        Pattern p = Pattern.compile("\\s*(\\\\[lg]eq?|[<>=]).*");
        String w = "0 < y \\leq z \\ge \\cpi, q > 0";
        String[] els = w.split("(\\\\[lg]eq?|[<>=])");
        System.out.println( Arrays.toString(els) );

        LinkedList<String> symbs = new LinkedList<>();
        Matcher m;
        for ( int i = 0; i < els.length; i++ ){
            //System.out.println(w);
            String sub = w.substring(els[i].length());
            //System.out.println(sub);
            m = p.matcher( sub );
            if ( m.matches() ) {
                String symb = m.group(1);
                symbs.add(symb);
                sub = sub.substring(symb.length());
            }
            w = sub;
        }

        System.out.println(symbs);
        String out = "";
        for ( int i = 0; i < els.length-1; i++ ){
            out += els[i];
            out += symbs.removeFirst();
            out += els[i+1];
            if ( i < els.length-2 )
                out += ", ";
        }
        System.out.println(out);
    }

}
