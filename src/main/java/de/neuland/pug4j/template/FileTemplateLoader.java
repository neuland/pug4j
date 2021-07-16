package de.neuland.pug4j.template;

import de.neuland.pug4j.exceptions.PugTemplateLoaderException;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileTemplateLoader implements TemplateLoader {

    private Charset encoding = StandardCharsets.UTF_8;
	private String templateLoaderPath = "/";
	private String extension = "pug";
	private String basePath="";

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
		if(!Files.isDirectory(Paths.get(templateLoaderPath))){
			throw new PugTemplateLoaderException("Directory '"+ templateLoaderPath +"' does not exist.");
		}
		if(templateLoaderPath.endsWith("/"))
			this.templateLoaderPath = templateLoaderPath;
		else
			this.templateLoaderPath = templateLoaderPath+"/";

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
		File templateSource = getFile(name);
		return templateSource.lastModified();
	}

	@Override
	public Reader getReader(String name) throws IOException {
		File templateSource = getFile(name);
		return new InputStreamReader(new FileInputStream(templateSource), encoding);
	}

	private File getFile(String name) {
		if(!StringUtils.isBlank(templateLoaderPath))
			if(Paths.get(name).isAbsolute()) {
				return Paths.get(templateLoaderPath+basePath+name.substring(1)).toFile();
			}else
				return Paths.get(templateLoaderPath).resolve(name).toFile();
		else
			return Paths.get(name).toFile();
	}

	public String getExtension() {
		return extension;
	}

	@Override
	public String getBase() {
		return basePath;
	}

	public void setBase(String basePath) {
		if (basePath.endsWith("/"))
			this.basePath = basePath;
		else
			this.basePath = basePath+"/";
	}
}