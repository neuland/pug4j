package de.neuland.pug4j.parser;

import de.neuland.pug4j.exceptions.PugTemplateLoaderException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathHelper {

    public static final Logger LOGGER = LoggerFactory.getLogger(PathHelper.class);

    public String resolvePath(String parentFileName, String templateName, String basePathString) {
        String inputParamsLog = "ParentFilename: " + parentFileName + ", TemplateName: " + templateName + ", BasePath:" + basePathString;
        templateName = FilenameUtils.separatorsToSystem(templateName);
        basePathString = FilenameUtils.separatorsToSystem(basePathString);
        if(FilenameUtils.getPrefixLength(basePathString)!=0)
            throw new PugTemplateLoaderException("basePath " + basePathString + " must be relative");

        if(FilenameUtils.getPrefixLength(parentFileName)!=0 && basePathString.length() > 0){
            parentFileName = Paths.get(basePathString + parentFileName.substring(1)).normalize().toString();
        }
        if(templateName.startsWith(File.separator) && basePathString.length() > 0){
            String path = Paths.get(basePathString + templateName.substring(1)).normalize().toString();
            LOGGER.debug(inputParamsLog + " Result: " + path);
            return path;
        }

        Path parent = Paths.get(parentFileName).getParent();
        if(parent==null) {
            LOGGER.debug(inputParamsLog + " Result: " + templateName);
            return templateName;
        }
        Path resolve = parent.resolve(Paths.get(templateName)).normalize();
        LOGGER.debug(inputParamsLog + " Result: " + resolve.toString());
        return resolve.toString();


    }
}
