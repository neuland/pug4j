package de.neuland.pug4j.parser;

import de.neuland.pug4j.exceptions.PugTemplateLoaderException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathHelper {
    public String resolvePath(String parentFileName, String templateName, String basePathString) {
        if(basePathString.startsWith("/"))
            throw new PugTemplateLoaderException("basePath " + basePathString + " must be relative");

        if(parentFileName.startsWith("/")){
            parentFileName = Paths.get(basePathString + parentFileName.substring(1)).normalize().toString();
        }
        if(templateName.startsWith("/")){
            return Paths.get(basePathString + templateName.substring(1)).normalize().toString();
        }

        Path parent = Paths.get(parentFileName).getParent();
        if(parent==null)
            return templateName;
        Path resolve = parent.resolve(Paths.get(templateName)).normalize();
        return resolve.toString();


    }
    private String resolveAbsolutePath(String filename,String basePath) {
        if(Paths.get(filename).isAbsolute()){
            return filename;
        }else{
            if(!Paths.get(basePath).isAbsolute()){
                throw new PugTemplateLoaderException("Can't resolve absolute path for '"+filename+"' if basePath has not been set.");
            }
            return Paths.get(basePath).resolve(filename).normalize().toString();
        }
    }

}
