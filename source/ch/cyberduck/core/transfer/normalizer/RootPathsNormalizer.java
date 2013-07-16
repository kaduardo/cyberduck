package ch.cyberduck.core.transfer.normalizer;

/**
 * @version $Id: RootPathsNormalizer.java 10292 2012-10-16 09:36:55Z dkocher $
 */
public interface RootPathsNormalizer<T> {
    public T normalize(T roots);
}
