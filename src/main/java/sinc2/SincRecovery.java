package sinc2;

import sinc2.kb.CompressedKb;
import sinc2.kb.KbException;
import sinc2.kb.NumeratedKb;

/**
 * The recovery class, retuning a compressed KB to the original version.
 * 
 * @since 1.0
 */
public abstract class SincRecovery {
    /**
     * Decompress the KB to the original form.
     *
     * @param decompressedName The name of the decompressed KB.
     */
    abstract public NumeratedKb recover(CompressedKb compressedKb, String decompressedName) throws KbException;
}
