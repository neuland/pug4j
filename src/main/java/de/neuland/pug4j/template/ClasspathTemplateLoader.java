package de.neuland.pug4j.template;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;

/**
 * Loads a Pug template from Classpath
 * It is useful when Pug templates are in the same JAR or WAR
 * 
 * @author emiguel
 *
 */
public class ClasspathTemplateLoader implements TemplateLoader {

    private FileTemplateLoader fileTemplateLoader;

    public ClasspathTemplateLoader() {
        String path = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        fileTemplateLoader = new FileTemplateLoader(path);
    }

    public ClasspathTemplateLoader(Charset encoding) {
        String path = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        fileTemplateLoader = new FileTemplateLoader(path,encoding);
    }

    public ClasspathTemplateLoader(Charset encoding, String extension) {
        String path = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        fileTemplateLoader = new FileTemplateLoader(path,encoding,extension);
    }
    public ClasspathTemplateLoader(String templateLoaderPath) {
        String path = Thread.currentThread().getContextClassLoader().getResource(templateLoaderPath).getPath();
        fileTemplateLoader = new FileTemplateLoader(path);
    }

    public ClasspathTemplateLoader(String templateLoaderPath, Charset encoding) {
        String path = Thread.currentThread().getContextClassLoader().getResource(templateLoaderPath).getPath();
        fileTemplateLoader = new FileTemplateLoader(path,encoding);
    }

    public ClasspathTemplateLoader(String templateLoaderPath, String extension) {
        String path = Thread.currentThread().getContextClassLoader().getResource(templateLoaderPath).getPath();
        fileTemplateLoader = new FileTemplateLoader(path,extension);
    }

    public ClasspathTemplateLoader(String templateLoaderPath, Charset encoding, String extension) {
        String path = Thread.currentThread().getContextClassLoader().getResource(templateLoaderPath).getPath();
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
