package org.cafienne.cmmn.repository.file;

import org.cafienne.akka.actor.CaseSystem;
import org.cafienne.cmmn.definition.DefinitionsDocument;
import org.cafienne.cmmn.definition.InvalidDefinitionException;
import org.cafienne.cmmn.repository.DefinitionProvider;
import org.cafienne.cmmn.repository.MissingDefinitionException;
import org.cafienne.cmmn.repository.WriteDefinitionException;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.util.*;

public class FileBasedDefinitionProvider implements DefinitionProvider {
    private final static Logger logger = LoggerFactory.getLogger(FileBasedDefinitionProvider.class);
    private final Map<String, FileBasedDefinition> cache = new SimpleLRUCache();
    private String deployDirectory = null;
    private final String EXTENSION = ".xml";

    @Override
    public List<String> list(TenantUser user) {
        return listDefinitions();
    }

    /**
     * Load a definitions document from the specified location. Method internally uses a cache to speed
     * up loading of the document; the cache checks for the file's last modified timestamp; if it differs from
     * what's in the cache, the file will be newly parsed and stored in the cache instead.
     *
     * @param name
     * @return
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws InvalidDefinitionException
     */
    @Override
    public DefinitionsDocument read(TenantUser user, String name) throws MissingDefinitionException, InvalidDefinitionException {
        if (! name.endsWith(EXTENSION)) name = name + EXTENSION;
        try {
            long lastModified = -1; // Note, -1 is the default value for reading files from class path (resourceAsStream)
            InputStream contents = null;

            // First check to see if the file is present on the file system (allowing for "hot" deploment)
            File sourceFile = getFile(name);
            if (sourceFile.exists()) {
                contents = new FileInputStream(sourceFile);
                lastModified = sourceFile.lastModified();
            } else {
                // If the file does not exist, we try to load it from the classpath
                contents = DefinitionsDocument.class.getClassLoader().getResourceAsStream(name);
                if (contents == null) {
                    throw new IOException("A file with name " + name + " cannot be found in the deployment directory, nor in the class path");
                }
            }

            // Now check to see if the file is already in our cache, and, if so, check whether it has the same last modified; if not, put the new one in the cache instead
            FileBasedDefinition cacheEntry = cache.get(name);
            if (cacheEntry == null || cacheEntry.lastModified != lastModified) {
                DefinitionsDocument def = new DefinitionsDocument(XMLHelper.getXMLDocument(contents));
                cacheEntry = new FileBasedDefinition(lastModified, def);
                cache.put(name, cacheEntry);
            }
            return cacheEntry.contents;
        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new MissingDefinitionException("Cannot find or load definition " + name, e);
        }
    }

    @Override
    public void write(TenantUser user, String name, DefinitionsDocument definitionsDocument) throws WriteDefinitionException {
        if (! name.endsWith(EXTENSION)) name = name + EXTENSION;

        String prefix = getDeployDirectory() + File.separator;
        String filename = prefix + name;
        File file = new File(filename);
        File prefixFile = new File(prefix);
        logger.debug("Saving definitions document "+name+" to "+file.getAbsolutePath());
        try {
            XMLHelper.persist(definitionsDocument.getDocument(), file.getAbsoluteFile());
        } catch (IOException | TransformerException e) {
            // Make sure deployment directory details do not make it to the outside world.
            String internalMessage = e.getMessage().replace(prefixFile.getAbsolutePath() + File.separator, "");
            throw new WriteDefinitionException("Failed to deploy definitions to '" + name +"': " + internalMessage, e);
        }
    }

    /**
     * Returns the absolute file corresponding with the filename from the deploy.dir.
     *
     * @param fileName
     * @return
     * @throws FileNotFoundException
     */
    private File getFile(String fileName) {
        return new File(getDeployDirectory() + "/" + fileName).getAbsoluteFile();
    }

    /**
     * Returns the directory in which DefinitionDocuments can be found or written to
     * Writes warnings if the directory does not exist.
     * The directory can be configured through the Typesafe config setting 'cafienne.definitions.location'
     * If this setting is not defined, it will take as default value './definitions', i.e. a subfolder
     * under the directory in which the JVM is started.
     *
     * @return
     */
    public String getDeployDirectory() {
        if (deployDirectory == null) {
            deployDirectory = CaseSystem.config().repository().location();
            File file = new File(deployDirectory);
            if (!file.exists()) {
                logger.warn("The deploy directory '" + file.getAbsolutePath() + "' does not exist (location configured is '" + deployDirectory + "'). The case engine will only read definitions from the class path until the deploy directory is created.");
            } else if (!file.isDirectory()) {
                logger.warn("The deploy directory '" + file.getAbsolutePath() + "' exists but is not a directory (location configured is '" + deployDirectory + "'). The case engine will only read definitions from the class path.. Configured location is '" + deployDirectory + "'");
            } else {
                logger.info("Reading case definitions from directory " + file.getAbsolutePath());
            }
        }
        return deployDirectory;
    }

    /**
     * This call lists all files in the configured deploy directory having a .xml extension
     *
     * @return
     */
    public List<String> listDefinitions() {
        List<String> fileNames = new ArrayList();
        try {
            File[] files = new File(getDeployDirectory()).listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].getAbsolutePath().endsWith(EXTENSION)) {
                    fileNames.add(files[i].getName());
                }
            }
        } catch (NullPointerException ex) {
            //TODO when the deploy directory is not defined, a nullpointer is thrown although there is a warning given.
            // IT is allowed to have a system without a deployDirectory defined, cases are served from classpath.
            // The system may run into all kind of errors though for definition operations with the current implementation.
            return fileNames;
        }
        return fileNames;
    }

    public static void main(String[] args) throws Exception {
        FileBasedDefinitionProvider provider = new FileBasedDefinitionProvider();
        Scanner s = new Scanner(System.in);
        System.out.println("Usage: DefinitionsDocument [filename] [num parse]");
        while (s.hasNextLine()) {
            String line = s.nextLine();
            StringTokenizer st = new StringTokenizer(line, " ");
            String fileName = st.nextToken();
            String number = st.nextToken();
            int numTests = Integer.parseInt(number);

            long now = System.currentTimeMillis();
            for (int i = 0; i < numTests; i++) {
                File file = provider.getFile(fileName);
                InputStream contents = new FileInputStream(file);
                DefinitionsDocument dd = new DefinitionsDocument(XMLHelper.getXMLDocument(contents));
            }
            long middle = System.currentTimeMillis();
            for (int i = 0; i < numTests; i++) {
                DefinitionsDocument dd = provider.read(null, fileName);
            }
            long end = System.currentTimeMillis();
            File file = provider.getFile(fileName);
            System.out.printf("Parsing file " + fileName + " of size " + file.length() + " every time took %d, caching took %d\n", (middle - now), (end - middle));
            System.out.println("Usage: DefinitionsDocument [filename] [num parse]");
        }
    }
}
