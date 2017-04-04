package gov.nist.drmf.interpreter.examples;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.util.*;
import java.nio.file.Paths;

import gov.nist.drmf.interpreter.mlp.extensions.MacrosLexicon;
import mlp.FeatureSet;
import mlp.ParseException;
import mlp.PomParser;
import mlp.PomTaggedExpression;

import gov.nist.drmf.interpreter.common.GlobalPaths;

/**
 *
 * @author youssef
 */
public class MLP {

    public static final String GLOBAL_LEXICON_PATH =
            "libs\\ReferenceData";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        start();
    }

    public static void start() {
         // the folder where the reference data (e.g., lexicons) are
        String refDataDir= GLOBAL_LEXICON_PATH;

        System.out.println(Paths.get(".").toAbsolutePath().toString());
        System.out.println(GlobalPaths.PATH_REFERENCE_DATA.toAbsolutePath().toString());
        
        String[] eqs=equationsForTesting(); //equations to test the translation on
        
        //mock macros for testing purposes
        String[] macros={"\\newcommand{\\sinq}{\\sin_q +1}",
            "\\newcommand{\\mysin}[2]{\\sin^{#1}(#2)}",
            "{\\mycos}[2][{[a_1]+{b_2}-c^3}] {\\cos_q #1 + #2}",
            "{\\mysin}[2]{\\sin #1 + #2}",
            "\\myfrac[3]{\\frac{\\mysin{#1}{#2}}{#2+#3}}",
            "\\myvec[2][x]{{\\mathbf{#1}}=(#1_1,#1_2,\\ldots,#1_{#2})}"
                };
       
             
        try{
            //create a PomParser object, providing it with the 
            //location of the lexicons
            PomParser parser = new PomParser(refDataDir);
            parser.addMacros(macros);   //make the macros known to the translation

            MacrosLexicon.init();
            parser.addLexicons( MacrosLexicon.getDLMFMacroLexicon() );

            // provide next an equation to parse
            String eq = eqs[0];
            //eq = "\\Mathieuce{123 a}@@{\\sqrt{2}b}{\\frac{1}{2}}";
            eq = "\\JacobiP{\\alpha}{\\beta}{n}@{\\cos@{a\\Theta}}";
            //eq = "\\FerrersP[\\frac{1}{2}]{\\nu}@{z}";
//            eq = "(ab^2c 1+2) \\cdot \\CatalansConstant 2";
//            eq="\\left ( \\left ( y \\right ] \\right)";
//            eq = "\\sqrt \\frac{2}{4}";
//            eq = "ab13d";
            eq = "3 \\mod 4";
//            eq = "\\LegendreP[0]{1}@{2}";

            // parse/tag the equation and print it out 
            PomTaggedExpression pe = parser.parse(eq);
            //Map<String,String> features = pe.getRoot().getNamedFeatures();
            //for ( String key : features.keySet() )
            //    System.out.println(key + ": " + features.get(key));
            print(eq+":\n"+pe.toString());

//            List<FeatureSet> l = pe.getComponents().get(0).getRoot().getAlternativeFeatureSets();
//            for ( FeatureSet f : l ){
//                System.out.println(f.toString("    "));
//            }
        }
        catch(ParseException | IOException e){
                print("Caught an exception: "+e.getMessage());
        }  
        
        
        //LexiconUtility.createCleanFilesAndLexicons(refDataDir); //(re)create clean files and lexicons 
        //LexiconUtility.getUnicodeMathStats(refDataDir); //generate statistics about the latex-and-unicode lexicon

    }
    
    static String[] equationsForTesting(){
        String[] eqs={
            "abc 12+30-\\sin ax12+\\frac12",
            "x+(\\frac12-\\frac45)-x",
            "< = > - + = 12 3 4 14ab cd q a4 . ,",
            "([[(]{x/y}",
            "b_{4}",
            "\\infty",
            "u_{x-y}+(\\frac12-\\frac45)-\\mathit{x}",
            "_{a-b}",
            "x_i",  
            "x_i^3",
            "\\left( hi \\right]", 
            "{1+2}/3",
            "\\left( \\right)",   
            "p\\longleftarrow \\Gamma>>5",
            "( x_1+y^n-z_i^2) +[v_{i+j}-\\frac12] +\\frac{x+y}{x-y}",
            "p \\longleftarrow Gamma>>5",
            "p\\longleftarrow \\Gamma>>5",
            "3 < 8 \\longrightarrow \\cuberoot{3} <2", 
            "2+a-\\iint", 
            "\\bigcirc",
            "\\rightharpoonup \\sin +\\arctan-\\sinh+\\sn-\\Gamma(z)", 
            "\\to \\ni \\in \\pm \\mp \\ne \\notin",
            "\\pmod \\varepsilon \\vartheta \\Diracdelta",
            "\\infty \\exp \\int \\rangle \\langle \\dag",
            "\\Leftarrow \\longleftarrow \\longmapsto \\Gamma+Gamma",
            "(x+y)^2 - [u+v^{a+b}] - s/t+33",
            "\\left(x+y\\right)^2", 
            "xa_i^3+(1-2)/3",
            "Ai^2(z)+Bi^2(z)-sin z+Gi(z)-sn(x) \\in H", 
            "x+(\\frac12-\\frac45)-x",
             "12x + x14",   
            "x+2-A",
            "x+ 2-\\int A",
            "(x+y)^2!/z_2'!!+1",
            "p \\longleftarrow Gamma>>5", 
            "\\left( x+y \\right)",
            "\\left(x+y\\right]-{1+2}/3",
            "\\overline{\\underline{\\mathbf{\\mathit{12}}}}",
            "\\overline{\\underline{\\boldsymbol{\\mathit{\\Gamma}}}}",
            "\\overline{\\underline{\\boldsymbol{\\mathit{10+12}}}}",  
            "[1 2]",
            "5-25.47+3",         
            "\\left( 1+2\\right]",
            "\\left( hi \\right]",
            "\\left\\lgroup 1+2\\right\\langle",
            "\\left\\backslash 1+2\\right]",
            "\\left\\backslash 1+2\\right\\bracevert",
            "\\left\\backslash 1+2\\right.",
            "\\left. 1+2\\right\\backslash",
            "\\backslash \\lmoustache \\langle \\downarrow",
            "\\left\\backslash \\lmoustache \\langle \\right\\downarrow",
            "\\Uparrow", 
            "\\left\\Uparrow  Uparrow \\right\\Downarrow",  
            "( [ ) ] { }",
            "\\lbrack ( [ ) ] {\\rbrack }",
            "\\left\\lbrack  1 \\right\\rbrack", 
            "\\left\\{ hi \\right\\}", 
            "\\left\\lbrace hi \\right\\rbrace",  
            "\\% ` !/? @ \\# \\$ \\^{ } \\& * ( ) - \\_ [ ] | ; : ' \" , < > . ",
            "\\left? 2 \\right;",
            "\\{ ( \\} \\rbrace \\lbrace", 
            "\\{a\\}", 
            "{2}",
            "\\left{ hi \\right}",  
            "\\left\\{ hi \\right\\}",  
            "\\lbrace a\\rbrace", 
            "...",
            "1 \\cdots n \\dotsi 100 ... 200",
            "\\left\\lceil 1/2 \\right\\lfloor",
            "1 \\mathcomma 2 \\mathcolon 3 \\mathoctothorpe",
            "2+a-\\lVert \\join \\iint", 
            "2+3-\\lVert \\join \\iint", 
            "\\rightharpoonup \\lVert \\sin ", 
            "\\lceil 1/2 \\lfloor",  
            "|1/2\\vert < 3-1",
            "x + * < << >> <<< >>>",
            "! \\$  \\{ \\} 125 B Ai",
            "\\left| 3 \\right) ",   
            "\\| \\left\\| 2\\right\\|",   
            "2 | 4, 5 || 10, 6 \\| 12 ",
            "\\Complex",
            //"1+2}",      // ignores the unmatched "}"
            //"{1+3",      //does not parse because the "{" is unmatched
            //"5-{1+3",    //does not parse because the "{" is unmatched
            "\\+ \\- \\>",       // does not recognize those tokens
            "\\forall x\\in R,x>0 or x\\le 0",
            "\\ddddot{\\underline{\\sqrt{2}}}",
            "\\binom{4}{2}",
            "\\iiint_1^2",
            "\\acos+\\arccoth +\\strange",
            "\\hat{2}+3",
            "\\stackrel{2}{3}-\\frac{1}{24}",
            "\\sqrt 4+\\sqrt{24}",
            "\\stackrel 2{3}",
            "\\stackrel 2 4",
            "\\frac2=",
            "\\frac=+",
            "\\stackrel2=",
            "\\stackrel 24",
            "\\sqrt 2 3",
            "\\iiint_1^2", 
            "\\frac1 4",  
            "\\frac 1 {24}",  
            "\\frac1 a b",  
            "\\frac1 24",  
            "\\frac 1 24",  
            "\\frac14",  
            "\\frac1 ab",  
            "\\frac a1",  
            "\\frac14689",  
            "\\stackrel14689",
            "\\binom14689", 
            "\\frac ba6a89", 
            "\\frac ab",
            "\\frac1\\sin",
            "\\frac\\sin25",
            "\\frac\\sin256",
            "\\frac\\sin2a56",
            "\\frac\\sin ba56",
            "\\frac\\sin \\%23",
            "\\sqrt 2 4", 
            "\\sqrt2467", 
            "\\sqrt ab", 
            "\\sqrt \\sin", 
            "\\sqrt {12ab}", 
            "[1 2)",
            "[1+2]",
            "\\binom\\cdots\\sin",  
            //"\\cfrac\\cdots\\mupgamma", 
            //"\\cfrac\\cdots\\left.",  //problem
            //"\\cfrac\\cdots\\left\\{", //problem
            //"\\cfrac\\cdots\\left[",  //problem
            "\\overline{\\underline{\\mathbf{\\mathit{12}}}}",
            "\\overline{\\underline{\\boldsymbol{\\mathit{\\Gamma}}}}",
            "\\overline{\\underline{\\boldsymbol{\\mathit{10+12}}}}",
            "\\mathit12", 
            "\\mathit 125", 
            "\\mathit abc", 
            "\\boldsymbol\\nabla", 
            "\\mathit xGamma", 
            "\\boldsymbol 25", 
            "\\hat 2+3",   
            "\\frac`'",
            "\\.5 \\dot 2 \\ddot3",
            "\\. {\\mathfrak{12 25}} \\'9",
            "\\. {\\mathfrak{12 25}} \\\"9",
            "\\. {\\mathfrak{12 25}} \\^9",
            "\\. {\\mathfrak{12 25}} \\`9",
            "\\. {\\mathfrak{12 25}} \\~9",
            "\\. {\\mathfrak{12 25}} \\=9",
            /*  // this commented-out block uses disallowed text accents
            "\\. {\\mathfrak{12 25}} \\t9",
            "\\. {\\mathfrak{12 25}} \\c9",
            "\\. {\\mathfrak{12 25}} \\r9",
            "\\. {\\mathfrak{12 25}} \\u9",
            "\\. {\\mathfrak{12 25}} \\v9",
            "\\. {\\mathfrak{12 25}} \\H9",
            "\\. {\\mathfrak{12 25}} \\d9",
            */
            "\\Big( \\Bigg\\} \\bigg[ \\bigg\\rceil \\Big\\lrcorner \\bigg\\langle",
            "\\bigg| \\big\\lVert \\big/ \\big\\backslash \\bigg\\lmoustache ",
            "\\bigg\\updownarrow",
            "10\\equiv 0 \\pmod{5}",
            "10\\equiv 0 \\pmod\\gamma",
            "10\\equiv 0 \\pmod 512",
            "10\\equiv 0 \\pmod abc",
            "10\\equiv 0 \\pmod {abc+de}", 
            "a^235",     
            "\\frac\\ \\;",
            "2_{3+17}",
            "2^357",
            "a^235_xy978", 
            "2^\\nabla",
            "2^3_4+2^3_456",
            "2_3^4 2_3^456",
            "2_3^4 2_3^{456}",
            "9_7^8 2_3 2_a3^4 2_3^ab56",
            "9_\\mathfrak 217",
            "9_\\mathfrak{A}",  
            "\\mathfrak{AB}",
            "\\mathfrak{2+3}",
            "9_\\mathfrak{AB cd}", 
            "9_\\mathfrak{2+3}", 
            "9_\\boldsymbol{\\mathcal ABC+CD}",
            "2^{3 10}_4  2^3_456",
            "\\| \\not= \\not< \\not> \\not| \\not\\|",  
            "\\not\\equiv \\not\\sim \\not= \\not< ;", 
            "= \\not= \\not\\mbfomega",
            "\\left\\backslash 2\\right.", 
            "\\not\\nabla",
            "\\not+ \\not- \\not\\times ",
            "1,2\\;4",
            "\\not {ab+cd+sin-Ai}",  //recongizes cd, sin and Ai as functions
            "\\not {ab}",
            "\\not {ab+cd}",
            "\\not {123}",
            "\\not123",
            "\\not x \\not xx",
            "\\backslash",
            "1;2",   
            "10\\bmod 5=0; 10=0 \\pmod 5", 
            "\\not; \\not\\|",  
            "{x^2}",   
            "\\mathcal{x_2}",
            "\\left( x^2 \\right)",
            "\\left( x_1+y^n-z_i^2 +v_{i+j}-\\frac12 +\\frac{x+y}{x-y}\\right]", 
            "\\not \\not \\nabla",   //handle better the tagging of multiple negations 
            "x_\\sqrt{3}+y_2",
            "x_\\sqrt+y_2", 
            "\\sqrt{3} \\sqrt 2 \\sqrt \\% \\sqrt 236",
            "\\mathit\\#", 
            "\\mathfrak{AB+CD}", 
            //"\\sqrt[12-4",  // invalid input 
            "\\sqrt[3]{4}",  
            "12 \\sqrt[35] 4 12^321",  
            "2 \\sqrt[3-4] 7", 
            "2 \\sqrt[3+{[}-4] 7",   
            "2 \\sqrt[3+{[}-4] {123+4}", 
            "2 \\sqrt[3+{[}-4] \\%12", 
            "2 \\sqrt[3+{[}-4] \\nabla12",   //works
            "4132 \\sqrt[5 {\\sqrt[3] 9} {\\sqrt[6] 7} 4] 8", //works
            "4132 \\sqrt[5 {\\sqrt[3 \\sin 12] 9} {\\sqrt[6] 7} 4] 8",
            "2 \\sqrt[3 \\left] 4\\right. 9 5] 7 12", 
            //"5-{1+3",    //does not parse because the "{" is unmatched
            //"\\binom 5", // expects another argument
            //"\\underbrace",  // expects an argument
            "\\mathit{232} \\mathit{X} \\mathit{\\gamma} \\mathit{\\Gamma} \\mathit{\\nabla}",
            "\\mathbf{232} \\mathbf{X} \\mathbf{\\gamma} \\mathbf{\\Gamma} \\mathbf{\\nabla}",
            "\\boldsymbol{232} \\boldsymbol{X} \\boldsymbol{\\gamma} \\boldsymbol{\\Gamma} \\boldsymbol{\\nabla}",
            "\\mathrm{232} \\mathrm{X} \\mathrm{\\gamma} \\mathrm{\\Gamma} \\mathrm{\\nabla}",
            "\\mathnormal{232} \\mathnormal{X} \\mathnormal{\\gamma} \\mathnormal{\\Gamma} \\mathnormal{\\nabla}",            
            "\\mathfrak{232} \\mathfrak{X} \\mathfrak{\\gamma} \\mathfrak{\\Gamma} \\mathfrak{\\nabla}",
            "\\mathbb{232} \\mathbb{X} \\mathbb{\\gamma} \\mathbb{\\Gamma} \\mathbb{\\nabla}",            
            "\\mathtt{232} \\mathtt{X} \\mathtt{\\gamma} \\mathtt{\\Gamma} \\mathtt{\\nabla}",            
            "\\mathsf{232} \\mathsf{X} \\mathsf{\\gamma} \\mathsf{\\Gamma} \\mathsf{\\nabla}",
            "\\mathcal{232} \\mathcal{X} \\mathcal{x} \\mathcal{\\gamma} \\mathcal{\\Gamma} \\mathcal{\\nabla}",
            "\\boldsymbol{232} \\boldsymbol{X} \\boldsymbol{x} \\boldsymbol{\\gamma} \\boldsymbol{\\Gamma} \\boldsymbol{\\nabla}",
            "\\boldsymbol{\\mathcal{ABC12}}",
            "\\mathit{232 X \\gamma \\Gamma \\nabla}",  
            "\\mathfrak{AB+CD \\mathit{12} abc \\nabla}", 
            "\\mathbb{\\mathfrak{AB+CD \\mathit{12} abc \\nabla}}", 
            "\\boldsymbol{\\mathfrak{AB+\\mathtt{CD} \\mathit{12} abc \\gamma \\nabla}}", 

            "12 34 \\begin{matrix} 1 & 2\\\\ 3 & 4 \\end{matrix}",
            "\\begin{matrix} 1 \\end{matrix}",
            "matrix",
            "\\begin{matrix} 10 & 12 & 15 \\\\ 3-2 & 14 \\\\ \\end{matrix}",
            "\\begin{matrix} 10 & 12 & 15 \\\\ 32 & 14 \\\\ \\end{matrix}",
            "\\begin{matrix} \\end{matrix}", 
            "\\begin{matrix} 10 & 12 & 15 \\\\ 32 & 14  \\end{matrix}",
            "\\begin{matrix} 10 & 12 & 15 \\\\ 32 & 14 \\\\ \\\\ \\\\  \\end{matrix}",
            "\\begin{pmatrix} 10 & 12 & 15 \\\\ 32 & 14  \\end{pmatrix}",
            "\\begin{eqnarray} xx+yy & = & 15 \\\\ xx-yy &=& 14 \\\\  \\end{eqnarray}",
            "sn \\begin{array}{| L ||ns} 10 & 12 & 15 \\\\ 32 & 14  \\end{array}",
            "\\begin{cases} -xx & \\text{if } xx <0 \\\\ xx & "
                    + "\\text{if } xx\\ge 0 \\\\ \\end{cases}", 
            "\\begin{cases} 10 & 12 & 15 \\\\ 32 & 14 \\\\ \\end{cases}",
            "\\begin{align} xx & = & 15 \\\\ yy &=& 14 \\\\  \\end{align}",
            "cases align matrix array equation sn", 
            "\\sum^5_{\\substack{0<i<5  j\\subseteq i}}",
            "12 \\\\ \\newline 14",  //problem with \\\\
            "12 \\\\* \\newline \\pagebreak \\linebreak 13",
            "12 \\nopagebreak \\nolinebreak  13 ",
            "\\dag \\dagger \\ddag \\ddagger",
            "12\\div 10 ",
            "\\today \\lq \\rq \\i \\j \\l \\L \\topfraction \\LaTeXe",
            "2 \\today \\width \\year \\le \\* {3}",
            "2 \\thanks{x^2}",
            "abcd\\footnote{note} defg\\footnote 123",
            "2+\\fbox{this 2-4=-2} ",
            "2+\\fbox   {this 2-4=-2} 5 abcd \\text{trying this} \\text 2345", 
            "\\mbox    23 \\text ",
            "23 \\framebox[1+3cm][l]{hello there} \\makebox[2cm]{well hello!}",
            "24 \\fbox[2]{madam hi}",
            "24 \\framebox[2] abcd \\makebox[2][r] ",
            "2 \\centering{hello}",
            "23 \\framebox[1+3cm][l]{hello there} \\makebox[2cm]{well hello!}",
            "22\\smash{2^{3^{4}}}",
            "2\\date \\ifpdf \\sloppy \\medskip \\centering{23} 5",
            "\\alph \\Alph \\frq \\frqq \\depth \\dq \\Alph",
            "2\\textdegree 4\\textcelsius",
            "{\\bf 22} {\\em abcd} \\textsf{hello}",
            "{\\Large{23}} \\footnotesize 3",
            "12 \\nopagebreak[3] \\sin 13 ", 
            "12 \\nopagebreak 13 ", 
            "12 \\nolinebreak \\nopagebreak[3] 13 ", 
            " 19 \\nopagebreak[3] 13 ",
            "12 \\nolinebreak[4] 19 \\sqrt[3]{4} 10 13 ", 
            "12 \\nolinebreak[4] 19 \\nopagebreak[3] 13 ",  
            "12 \\nopagebreak[3] \\nolinebreak 13 ",  
            "12 \\pagestyle{plain} 13 \\thispagestyle{nothing} 14 \\index{text}",
            "12 \\hspace{1cm} \\hspace*{1cm} 14 \\vspace * {2cm} 13 \\glossary{sin}", 
            "12 \\hyphenation{mytext} 13 \\roman{counter}",
            "12 \\theoremstyle{definition} 13 \\linebreak[4] 14 \\nolinebreak{4} 15",
            "\\hfill 3 \\_ 4 \\* \\/ \\+ \\% \\# \\$ \\& \\\\* \\stop",
            "\\+ \\\' \\` \\= \\~ \\\" \\. \\^ \\\\ 22", 
            "\\sum^5_{\\substack{0<iii<5 \\\\  jjj\\subseteq iii}}",
            "12 \\kill 14",
            "\\begin{matrix} 10 & 12 & 15 \\\\ 3-2 & 14 \\\\ \\end{matrix}", 
            "\\sqrt[\\left[3\\right.]abc12", 
            "\\sqrt[3] 564",
            "\\sqrt[3] \\#64",
            "\\sqrt[3]{\\#64}",
            "12 \\myfrac{10}{14}{16} \\dum 3",
            "\\myvec[z]{n}", 
            "\\myvec[z]{n} 1",
            "\\myvec{10} ",
            "\\myvec[12]{10} ",
            "\\myvec[12]{10}",
            "\\myvec[him]{her}",
            "\\mysin{22}{him} her",
            "\\myfrac{10}{14}{16}",
            "\\begin{matrix} 10 & 12 & 15 \\\\ 3-2 & 14 \\\\ \\end{matrix}", 
            "\\sin \\Ai"   
        };
        
        return eqs;
    }
    
    static void print(String s){
        System.out.println(s);
    }
    
    static void print(char s){
        System.out.println(s);
    }
    
    static void printList(List<?> list) {
        for (Object elem : list)
            print(elem + " ");
    }
    
    static void print(String[] s){
        for(int i=0;i<s.length;i++)
            System.out.println("; string["+i+"]="+ s[i]);
    }
     
    
}