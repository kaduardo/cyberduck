// InvalidDateException.java
// $Id: InvalidDateException.java 9666 2012-08-22 13:16:34Z dkocher $
// (c) COPYRIGHT MIT, INRIA and Keio, 2000.
// Please first read the full copyright statement in file COPYRIGHT.html
package org.w3c.util;

/**
 * @author Benoît Mahé (bmahe@w3.org)
 * @version $Revision: 9666 $
 */
public class InvalidDateException extends Exception {
    private static final long serialVersionUID = -9012791102239300978L;

    public InvalidDateException(String msg) {
        super(msg);
    }
}
