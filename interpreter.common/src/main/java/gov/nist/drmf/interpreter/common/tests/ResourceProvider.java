package gov.nist.drmf.interpreter.common.tests;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @see Resource
 * @author Andre Greiner-Petter
 */
public class ResourceProvider implements ArgumentsProvider, AnnotationConsumer<Resource> {
    private String[] paths;

    private ResourceProvider() {
        paths = new String[]{};
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
        String[] arguments = new String[paths.length];
        Class<?> contextClass = extensionContext.getTestClass()
                .orElseThrow(() -> new ClassNotFoundException("Unable to detect the context of the class."));
        for ( int i = 0; i < paths.length; i++ ) {
            arguments[i] = load( contextClass, paths[i] );
        }
        List<String[]> list = new LinkedList<>();
        list.add(arguments);
        return list.stream().map(Arguments::of);
    }

    /**
     * Loads the file from the given context
     * @param context context class to find the correct path
     * @param fileName the file name to load
     * @return the UTF-8 loaded content of the file
     * @throws IOException if the file cannot be found
     */
    public static String load(Class<?> context, String fileName) throws IOException {
        InputStream in = context.getResourceAsStream(fileName);
        if ( in == null ) {
            URL url = context.getResource("");
            Matcher m = Pattern.compile(".*target/test-classes/(.*)").matcher(url.toString());
            String path = m.matches() ? m.group(1)+fileName : url.toString();
            throw new FileNotFoundException("The specified resource does not exist at: " + path);
        }
        return IOUtils.toString(in, StandardCharsets.UTF_8);
    }

    @Override
    public void accept(Resource resource) {
        paths = resource.value();
    }
}
