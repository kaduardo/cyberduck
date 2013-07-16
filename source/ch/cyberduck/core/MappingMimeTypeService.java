package ch.cyberduck.core;

import org.apache.commons.lang.StringUtils;
import org.jets3t.service.utils.Mimetypes;

/**
 * @version $Id: MappingMimeTypeService.java 10471 2012-10-19 12:36:31Z dkocher $
 */
public class MappingMimeTypeService implements MimeTypeService {

    @Override
    public String getMime(final String filename) {
        // Reads from mime.types in classpath
        return Mimetypes.getInstance().getMimetype(StringUtils.lowerCase(filename));
    }
}