package sinc2.kb.compact;

import sinc2.kb.KbRelation;
import sinc2.util.LittleEndianIntIO;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * This class stores the records in a relation as an integer table.
 *
 * @since 2.1
 */
public class SimpleRelation extends IntTable {

    protected static final int BITS_PER_INT = Integer.BYTES * 8;

    /** The flags are used to denote whether a record has been marked entailed */
    protected final int[] entailmentFlags;

    /**
     * This method loads a relation file as a 2D array of integers. Please refer to "KbRelation" for the file format.
     *
     * @param name         The name of the relation
     * @param arity        The arity of the relation
     * @param totalRecords The number of records in the relation
     * @param kbPtah       The path to the KB that the relation belongs to
     * @throws IOException
     * @see KbRelation
     */
    static protected int[][] loadFile(String name, int arity, int totalRecords, String kbPtah) throws IOException {
        File rel_file = KbRelation.getRelFilePath(kbPtah, name, arity, totalRecords).toFile();
        FileInputStream fis = new FileInputStream(rel_file);
        byte[] buffer = new byte[Integer.BYTES];
        int[][] records = new int[totalRecords][];
        for (int i = 0; i < totalRecords; i++) {
            int[] record = new int[arity];
            for (int arg_idx = 0; arg_idx < arity && Integer.BYTES == fis.read(buffer); arg_idx++) {
                record[arg_idx] = LittleEndianIntIO.byteArray2LeInt(buffer);
            }
            records[i] = record;
        }
        fis.close();
        return records;
    }

    /**
     * Create a relation directly from a list of records
     */
    public SimpleRelation(int[][] records) {
        super(records);
        entailmentFlags = new int[totalRows / BITS_PER_INT + ((0 == totalRows % BITS_PER_INT) ? 0 : 1)];
    }

    /**
     * Create a relation from a relation file
     * @throws IOException
     */
    public SimpleRelation(String name, int arity, int totalRecords, String kbPtah) throws IOException {
        super(loadFile(name, arity, totalRecords, kbPtah));
        entailmentFlags = new int[totalRows / BITS_PER_INT + ((0 == totalRows % BITS_PER_INT) ? 0 : 1)];
    }

    /**
     * Set a record as entailed if it is in the relation.
     */
    public void setEntailed(int[] record) {
        int idx = whereIs(record);
        if (NOT_FOUND != idx) {
            entailmentFlags[idx / BITS_PER_INT] |= 0x1 << (idx % BITS_PER_INT);
        }
    }

    /**
     * Set a record as not entailed if it is in the relation.
     */
    public void setNotEntailed(int[] record) {
        int idx = whereIs(record);
        if (NOT_FOUND != idx) {
            entailmentFlags[idx / BITS_PER_INT] &= ~(0x1 << (idx % BITS_PER_INT));
        }
    }

    /**
     * Check whether a record is in the relation and is entailed.
     */
    public boolean isEntailed(int[] record) {
        int idx = whereIs(record);
        return (NOT_FOUND != idx) && 0 != (entailmentFlags[idx / BITS_PER_INT] & (0x1 << (idx % BITS_PER_INT)));
    }
}
