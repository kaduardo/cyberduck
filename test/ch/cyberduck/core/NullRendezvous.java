package ch.cyberduck.core;

/**
 * @version $Id: NullRendezvous.java 10533 2012-10-22 13:29:55Z dkocher $
 */
public class NullRendezvous extends AbstractRendezvous {
    @Override
    public void init() {
        //
    }

    @Override
    public void quit() {
        //
    }

    public static void register() {
        RendezvousFactory.addFactory(Factory.NATIVE_PLATFORM, new RendezvousFactory() {
            @Override
            protected Rendezvous create() {
                return new NullRendezvous();
            }
        });
    }
}
