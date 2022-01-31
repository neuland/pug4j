package de.neuland.pug4j.template;

import de.neuland.pug4j.exceptions.PugTemplateLoaderException;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Paths;

/**
 * Loads a Pug template from Classpath
 * It is useful when Pug templates are in the same JAR or WAR
 * 
 * @author emiguel
 *
 */
public class ClasspathTemplateLoader implements TemplateLoader {

    private FileTemplateLoader fileTemplateLoader;
    private String getResourcePath(String path){
        try {
            return Paths.get(Thread.currentThread().getContextClassLoader().getResource(path).toURI()).toString();
        } catch (URISyntaxException e) {
            throw new PugTemplateLoaderException("Path '"+ path +"' does not exist.");
        }
    }
    public ClasspathTemplateLoader() {
        String path = getResourcePath("");
        fileTemplateLoader = new FileTemplateLoader(path);
    }

    public ClasspathTemplateLoader(Charset encoding) {
        String path = getResourcePath("");
        fileTemplateLoader = new FileTemplateLoader(path,encoding);
    }

    public ClasspathTemplateLoader(Charset encoding, String extension) {
        String path = getResourcePath("");
        fileTemplateLoader = new FileTemplateLoader(path,encoding,extension);
    }
    public ClasspathTemplateLoader(String templateLoaderPath) {
        String path = getResourcePath(templateLoaderPath);
        fileTemplateLoader = new FileTemplateLoader(path);
    }

    public ClasspathTemplateLoader(String templateLoaderPath, Charset encoding) {
        String path = getResourcePath(templateLoaderPath);
        fileTemplateLoader = new FileTemplateLoader(path,encoding);
    }

    public ClasspathTemplateLoader(String templateLoaderPath, String extension) {
        String path = getResourcePath(templateLoaderPath);
        fileTemplateLoader = new FileTemplateLoader(path,extension);
    }

    public ClasspathTemplateLoader(String templateLoaderPath, Charset encoding, String extension) {
        String path = getResourcePath(templateLoaderPath);
        fileTemplateLoader = new FileTemplateLoader(path,encoding,extension);
    }

    public long getLastModified(String name) {
        return -1;
    }

    @Override
    public Reader getReader(String name) throws IOException {
        return fileTemplateLoader.getReader(name);
    }

    @Override
    public String getExtension() {
        return fileTemplateLoader.getExtension();
    }

    @Override
    public String getBase() {
        return fileTemplateLoader.getBase();
    }

    public void setBase(String basePath){
        fileTemplateLoader.setBase(basePath);
    }
}
