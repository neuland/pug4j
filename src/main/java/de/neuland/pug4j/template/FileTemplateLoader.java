package de.neuland.pug4j.template;

import de.neuland.pug4j.exceptions.PugTemplateLoaderException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileTemplateLoader implements TemplateLoader {

    private Charset encoding = StandardCharsets.UTF_8;
	private String templateLoaderPath = "";
	private String extension = "pug";
	private String basePath="";
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
		Path path = Paths.get(templateLoaderPath);
		if(!Files.isDirectory(path)){
			throw new PugTemplateLoaderException("Directory '"+ templateLoaderPath +"' does not exist.");
		}
		if(templateLoaderPath.endsWith(File.separator))
			this.templateLoaderPath = templateLoaderPath;
		else
			this.templateLoaderPath = templateLoaderPath+File.separator;

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
		this(templateLoaderPath,extension);
		this.encoding = encoding;
	}

	public long getLastModified(String name) {
		name = FilenameUtils.separatorsToSystem(name);
		File templateSource = getFile(name);
		return templateSource.lastModified();
	}

	@Override
	public Reader getReader(String name) throws IOException {
		if(name == null){
			throw new IllegalArgumentException("Filename not provided!");
		}
		name = FilenameUtils.separatorsToSystem(name);
		name = ensurePugExtension(name);
		File templateSource = getFile(name);
		return new InputStreamReader(new FileInputStream(templateSource), encoding);
	}

	private String ensurePugExtension(String templateName) {
		if ( StringUtils.isBlank(FilenameUtils.getExtension(templateName))) {
			return templateName + "." + getExtension();
		}
		return templateName;
	}
	private File getFile(String name) {
		String filepath = getFilepath(name);
		logger.debug("Template: "+name+" resolved filepath is " + filepath);
		return Paths.get(filepath).toFile();
	}

	private String getFilepath(String name){
		if(!StringUtils.isBlank(templateLoaderPath)) {
			if (name.startsWith(File.separator)) {
				return Paths.get(templateLoaderPath + basePath + name.substring(1)).toString();
			} else {
				return Paths.get(templateLoaderPath).resolve(name).toString();
			}
		} else {
			return name;
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
			this.basePath = basePath+"/";
	}
}