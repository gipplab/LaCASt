package gov.nist.drmf.interpreter.cas;


import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.GlobalConstants;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;


public class DlmfBatchTranslator {

	public static final String MLPERROR = "mlperror";

	public static void main( String[] args ) throws Exception {
		// create Options object
		Options options = new Options();
		options.addRequiredOption( "i", "input", true, "directory" );
		options.addRequiredOption( "o", "output", true, "directory" );
		options.addOption( "f", "filter", true, "filter" );
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse( options, args );
		final String d = cmd.getOptionValue( "i" );
		final File indir = new File( d );
		Collection<File> files = FileUtils.listFiles(
			indir,
			new RegexFileFilter( "^(.*?).s.tex" ),
			DirectoryFileFilter.DIRECTORY
		);
		final Path outPutDir = FileUtils.getFile( cmd.getOptionValue( "o" ) ).toPath();
		GlobalConstants.CAS_KEY = "Maple";//TODO: Check with AGP
		SemanticLatexTranslator translator = SemanticToCASInterpreter.getParser( false );
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
		final String prefix = indir.getPath().replace( "/", "-" );
		files.forEach( file -> {
			try {
				final String sTeX = FileUtils.readFileToString( file );
				translator.reset();
				translator.translate( sTeX );
				final String translatedExpression = translator.getTranslatedExpression();
				if ( mlpfilter ) {
					if ( translator.isMlpError() ) {
						final File f = Paths.get(
							finalOutPutDir.toString(),
							file.getPath().replace( "/", "-" ).replace( prefix, "" )
						).toFile();
						FileUtils.write( f, sTeX );
					}
				} else {
					final File f = Paths.get(
						finalOutPutDir.toString(),
						file.getPath().replace( "/", "-" ).replace( prefix, "" )
							+ ".maple" ).toFile();
					FileUtils.write( f, translatedExpression );

				}

			} catch ( IOException e ) {
				e.printStackTrace();
			}
		} );
	}
}
