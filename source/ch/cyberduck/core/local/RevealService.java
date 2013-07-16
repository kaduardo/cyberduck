package ch.cyberduck.core.local;

/**
 * @version $Id: RevealService.java 10545 2012-10-22 15:32:42Z dkocher $
 */
public interface RevealService {
    /**
     * Reveal file in file browser
     *
     * @param file File or folder
     */
    void reveal(Local file);
}
