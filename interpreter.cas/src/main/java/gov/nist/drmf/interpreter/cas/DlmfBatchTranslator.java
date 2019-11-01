package gov.nist.drmf.interpreter.cas;


import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Set;


public class DlmfBatchTranslator {

	public static final String MLPERROR = "mlperror";

	public static void main( String[] args ) throws Exception {
		// create Options object
		Options options = new Options();
		options.addRequiredOption( "i", "input", true, "directory" );
		options.addRequiredOption( "o", "output", true, "directory" );
		options.addOption("s","subdir",true,"subdirectory for filtering");
		options.addOption( "f", "filter", true, "filter" );
		options.addOption( "e","error",true,"file for error report" );
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse( options, args );
		Collection<File> files = getFiles( cmd );
		final Path outPutDir = FileUtils.getFile( cmd.getOptionValue( "o" ) ).toPath();
		SemanticLatexTranslator translator = SemanticToCASInterpreter.getParser( false, "Maple" );
		translator.setTolerant( true );
		final boolean mlpfilter;
		if ( cmd.hasOption( "f" ) ) {
			if ( cmd.getOptionValue( "f" ).equals( MLPERROR ) ) {
				mlpfilter = true;
			} else {
				mlpfilter = false;
			}
		} else {
			mlpfilter = false;
		}
		Path finalOutPutDir = outPutDir;
		// System.out.println("processing files" + files.size());
		final String prefix = cmd.getOptionValue( "i" ).replace( "/", "-" );
		files.forEach( file -> {
			try {
				final String sTeX = FileUtils.readFileToString( file );
				final String id = file.getPath().replace( "/", "-" ).replace( prefix, "" );
				translator.reset();
				translator.setFileID( id );
				translator.translate( sTeX );
				final String translatedExpression = translator.getTranslatedExpression();
				if ( mlpfilter ) {
					if ( translator.isMlpError() ) {
						final File f = Paths.get( finalOutPutDir.toString(), id ).toFile();
						FileUtils.write( f, sTeX );
					}
				} else {
					final File f = Paths.get( finalOutPutDir.toString(), id + ".maple" ).toFile();
					FileUtils.write( f, translatedExpression );
				}
			} catch ( IOException e ) {
				e.printStackTrace();
			}
		});
		if(cmd.hasOption( 'e' )) {
			final Map<String, Map<Integer, Set<String>>> problemTokens = translator.getProblemTokens();
			XStream xstream = new XStream( new DomDriver() );
			xstream.toXML( problemTokens, new FileWriter( cmd.getOptionValue( "e" ) ) );
		}

	}

	static Collection<File> getFiles( CommandLine cmd ) {
		String d = cmd.getOptionValue( "i" );
		if (cmd.hasOption( 's' )){
			d += "/" + cmd.getOptionValue( 's' );
		}
		final File indir = new File( d );
		return FileUtils.listFiles(
			indir,
			new RegexFileFilter( "^(.*?).s.tex" ),
			DirectoryFileFilter.DIRECTORY
		);
	}
}
