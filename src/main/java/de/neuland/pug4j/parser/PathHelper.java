package de.neuland.pug4j.parser;

import de.neuland.pug4j.exceptions.PugParserException;
import de.neuland.pug4j.exceptions.PugTemplateLoaderException;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PathHelper {

    public static final Logger LOGGER = LoggerFactory.getLogger(PathHelper.class);

    public String resolvePath(String parentTemplateName, String templateName, String basePath) {
        parentTemplateName = FilenameUtils.separatorsToUnix(parentTemplateName);
        templateName = FilenameUtils.separatorsToUnix(templateName);

        if(FilenameUtils.getPrefixLength(basePath)!=0)
            throw new PugTemplateLoaderException("basePath " + basePath + " must be relative");

        String inputParamsLog = "ParentFilename: " + parentTemplateName + ", TemplateName: " + templateName + ", BasePath:" + basePath;

        if(parentTemplateName.startsWith("/") && basePath.length() > 0){
            parentTemplateName = FilenameUtils.normalize(basePath + parentTemplateName,true);
        }

        if(templateName.startsWith("/") && basePath.length() > 0){
            String path = FilenameUtils.normalize(basePath + templateName,true);
            LOGGER.debug(inputParamsLog + " Result: " + path);
            return path;
        }

        String parent = FilenameUtils.getPath(parentTemplateName);
        if(parent==null) {
            LOGGER.debug(inputParamsLog + " Result: " + templateName);
            return templateName;
        }

        String resolve = FilenameUtils.normalize(FilenameUtils.concat(parent,templateName),true);
        LOGGER.debug(inputParamsLog + " Result: " + resolve);
        return resolve;


    }
}
