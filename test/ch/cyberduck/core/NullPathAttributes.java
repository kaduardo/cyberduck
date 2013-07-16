package ch.cyberduck.core;

/**
 * @version $Id: NullPathAttributes.java 10334 2012-10-16 17:07:38Z dkocher $
 */
public class NullPathAttributes extends PathAttributes {

    public NullPathAttributes() {
        super(Path.FILE_TYPE);
    }

    public NullPathAttributes(int filetype) {
        super(filetype);
    }
}
