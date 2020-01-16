package gov.nist.drmf.interpreter.common;

import gov.nist.drmf.interpreter.evaluation.numeric.NumericalConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class DLMFLinker {

    private static final Logger LOG = LogManager.getLogger(DLMFLinker.class.getName());

    private final HashMap<String, String> link_librarie;

    private static DLMFLinker linker;

    private DLMFLinker( Path definitionFile ) {
        link_librarie = new HashMap<>();
        LOG.debug("Start Link-Lib Init...");

        try ( BufferedReader br = Files.newBufferedReader(definitionFile) ){
            br.lines()
                    .sequential()
                    .map( l -> l.split("=>") )
                    .filter( l -> l.length == 2 )
                    .forEach( l -> {
                        String t = l[1].trim().substring("DLMF:/".length());
                        link_librarie.put( l[0].trim(), "http://dlmf.nist.gov/"+t );
                    } );
        } catch ( Exception e ){
            e.printStackTrace();
            return;
        }
    }

    public String getLink( String label ){
        return link_librarie.get(label);
    }

    public static DLMFLinker getLinkerInstance(){
        if ( linker == null ){
            Path p = NumericalConfig.config().getLabelSet();
            linker = new DLMFLinker(p);
        }
        return linker;
    }

    public static void main(String[] args) {
        Path base = Paths.get("/home/andreg-p/Howard/");
        Path linksP = base.resolve("BruceLabelLinks.txt");
        Path formulaP = base.resolve("lessformulas.txt");
        Path out = base.resolve("line-links-lessform.txt");

        DLMFLinker linker = new DLMFLinker(linksP);
//        System.out.println(linker.getLink("eq:AL.DE.sd.3"));

        Pattern lp = Pattern.compile(".*\\\\label\\{(.*)}.*\\\\ccode\\{(.*)}.*");
//        String tl = "\\det \\left[ \\frac{1}{a_j - b_k} \\right] = (-1)^{n(n-1)/2} \\* \\prod_{1 \\le j < k \\le n} (a_k - a_j)(b_k - b_j) \\Bigg/ \\prod^n_{j,k=1} (a_j - b_k) \\label{eq:AL.DE.sd.3} \\ccode{AL}";
//        Matcher m = lp.matcher(tl);
//        m.matches();
//        System.out.println(m.group(1));
//        System.out.println(m.group(2));
//        System.out.println(linker.getLink(m.group(1)));

        int[] lcounter = new int[]{1};

        try ( BufferedWriter bw = Files.newBufferedWriter(out) ) {
            Files.lines(formulaP)
                    .sequential() // otherwise numbers are not correct
                    .map( l -> {
                        Matcher m = lp.matcher(l);
                        if ( m.matches() ) {
                            String code = m.group(2);
                            String link = linker.getLink(m.group(1));
                            l = lcounter[0] + " [" + code + "]: " + link;
                            if ( link == null ) l += " " + m.group(1);
                        } else {
                            l = lcounter[0] + " [-]: null";
                        }
                        lcounter[0]++;
                        System.out.print("\r" + lcounter[0]);
                        return l;
                    })
                    .forEach(str -> {
                        try {
                            bw.write(str);
                            bw.newLine();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
