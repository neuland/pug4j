package de.neuland.pug4j.template;

import de.neuland.pug4j.exceptions.PugTemplateLoaderException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

public class FileTemplateLoader implements TemplateLoader {

    private final String separator = FileSystems.getDefault().getSeparator();
    private Charset encoding = StandardCharsets.UTF_8;
    private Path templateLoaderPath = null;
    private String extension = "pug";
    private String basePath = "";
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public FileTemplateLoader() {
    }

    public FileTemplateLoader(Charset encoding) {
        this.encoding = encoding;
    }

    public FileTemplateLoader(Charset encoding, String extension) {
        this(encoding);
        this.extension = extension;
    }

    public FileTemplateLoader(String templateLoaderPath) {
        this(Paths.get(FilenameUtils.separatorsToSystem(templateLoaderPath)));
    }

    public FileTemplateLoader(String templateLoaderPath, Charset encoding) {
        this(templateLoaderPath);
        this.encoding = encoding;
    }

    public FileTemplateLoader(String templateLoaderPath, String extension) {
        this(templateLoaderPath);
        this.extension = extension;
    }

    public FileTemplateLoader(String templateLoaderPath, Charset encoding, String extension) {
        this(templateLoaderPath, extension);
        this.encoding = encoding;
    }

    public FileTemplateLoader(Path templateLoaderPath) {
        if (templateLoaderPath == null || !Files.isDirectory(templateLoaderPath)) {
            throw new PugTemplateLoaderException("Directory '" + templateLoaderPath + "' does not exist.");
        }
        this.templateLoaderPath = templateLoaderPath.toAbsolutePath();
    }

    public FileTemplateLoader(Path templateLoaderPath, Charset encoding) {
        this(templateLoaderPath);
        this.encoding = encoding;
    }

    public FileTemplateLoader(Path templateLoaderPath, String extension) {
        this(templateLoaderPath);
        this.extension = extension;
    }

    public FileTemplateLoader(Path templateLoaderPath, Charset encoding, String extension) {
        this(templateLoaderPath, extension);
        this.encoding = encoding;
    }

    public long getLastModified(String name) throws IOException {
        Path filepath = getFilepath(name);
        return Files.getLastModifiedTime(filepath).to(TimeUnit.MILLISECONDS);
    }

    @Override
    public Reader getReader(String name) throws IOException {
        if (name == null) {
            throw new IllegalArgumentException("Filename not provided!");
        }
        Path filepath = getFilepath(name);
        logger.debug("Template: " + name + " resolved filepath is " + filepath.toAbsolutePath());
        final InputStream inputStream = Files.newInputStream(filepath);
        return new InputStreamReader(inputStream, encoding);
    }

    private String ensurePugExtension(String templateName) {
        if (StringUtils.isBlank(FilenameUtils.getExtension(templateName))) {
            return templateName + "." + getExtension();
        }
        return templateName;
    }

    private Path getFilepath(String name) {
        name = FilenameUtils.separatorsToSystem(name);
        name = ensurePugExtension(name);
        if (templateLoaderPath != null) {
            if (name.startsWith(separator)) {
                // absolute-rooted within loader: apply basePath and strip leading separator
                String rel = name.substring(1);
                Path p = templateLoaderPath.resolve(basePath).resolve(rel).normalize();
                return p;
            } else {
                return templateLoaderPath.resolve(name).normalize();
            }
        } else {
            return Paths.get(name);
        }
    }

    public String getExtension() {
        return extension;
    }

    @Override
    public String getBase() {
        return basePath;
    }

    public void setBase(String basePath) {
        if (basePath.endsWith("/") || "".equals(basePath))
            this.basePath = basePath;
        else
            this.basePath = basePath + "/";
    }
}