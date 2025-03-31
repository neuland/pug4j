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
    private String templateLoaderPath = "";
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
        templateLoaderPath = FilenameUtils.separatorsToSystem(templateLoaderPath);
        if (!Files.isDirectory(Paths.get(templateLoaderPath))) {
            throw new PugTemplateLoaderException("Directory '" + templateLoaderPath + "' does not exist.");
        }

        if (!templateLoaderPath.endsWith(separator)) {
            templateLoaderPath += separator;
        }
        this.templateLoaderPath = templateLoaderPath;
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
        String filePath;
        if (!StringUtils.isBlank(templateLoaderPath)) {
            if (name.startsWith(separator)) {
                filePath = FilenameUtils.normalize(templateLoaderPath + basePath + name.substring(1));
            } else {
                filePath = FilenameUtils.normalize(templateLoaderPath + name);
            }
        } else {
            filePath = name;
        }
        return Paths.get(filePath);
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