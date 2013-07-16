package ch.cyberduck.core;

/**
 * @version $Id: PathKindDetector.java 9998 2012-10-10 14:55:01Z dkocher $
 */
public interface PathKindDetector {

    public int detect(String path);
}
