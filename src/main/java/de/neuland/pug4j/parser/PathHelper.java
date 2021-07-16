package de.neuland.pug4j.parser;

import de.neuland.pug4j.exceptions.PugTemplateLoaderException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathHelper {
    public String resolvePath(String parentFileName, String templateName, String basePathString) {
        if(Paths.get(basePathString).isAbsolute())
            throw new PugTemplateLoaderException("basePath " + basePathString + " must be relative");

        if(Paths.get(parentFileName).isAbsolute() && basePathString.length() > 0){
            parentFileName = Paths.get(basePathString + parentFileName.substring(1)).normalize().toString();
        }
        if(Paths.get(templateName).isAbsolute() && basePathString.length() > 0){
            return Paths.get(basePathString + templateName.substring(1)).normalize().toString();
        }

        Path parent = Paths.get(parentFileName).getParent();
        if(parent==null)
            return templateName;
        Path resolve = parent.resolve(Paths.get(templateName)).normalize();
        return resolve.toString();


    }
}
