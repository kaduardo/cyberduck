package ch.cyberduck.core.editor;

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.FactoryException;
import ch.cyberduck.core.local.LaunchServicesApplicationFinder;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @version $Id: MultipleEditorFactoryTest.java 10440 2012-10-18 17:04:04Z dkocher $
 */
public class MultipleEditorFactoryTest extends AbstractTestCase {

    @BeforeClass
    public static void register() {
        LaunchServicesApplicationFinder.register();
        MultipleEditorFactory.register();
    }

    @Test(expected = FactoryException.class)
    public void testEdit() {
        MultipleEditorFactory f = new MultipleEditorFactory();
        f.create();
    }
}
