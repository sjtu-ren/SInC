package sinc2.kb.compact;

import sinc2.common.Argument;
import sinc2.kb.KbRelation;
import sinc2.kb.Record;
import sinc2.util.ArrayOperation;
import sinc2.util.LittleEndianIntIO;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class stores the records in a relation as an integer table.
 *
 * @since 2.1
 */
public class SimpleRelation extends IntTable {

    /** The threshold for pruning useful constants */
    public static double MIN_CONSTANT_COVERAGE = 0.25;
    protected static final int BITS_PER_INT = Integer.BYTES * 8;

    /** The flags are used to denote whether a record has been marked entailed */
    protected final int[] entailmentFlags;
    /** Relation name */
    protected final String name;
    /** The ID number of the relation */
    protected final int id;

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
    public SimpleRelation(String name, int id, int[][] records) {
        super(records);
        this.name = name;
        this.id = id;
        entailmentFlags = new int[totalRows / BITS_PER_INT + ((0 == totalRows % BITS_PER_INT) ? 0 : 1)];
    }

    /**
     * Create a relation from a relation file
     * @throws IOException
     */
    public SimpleRelation(String name, int id, int arity, int totalRecords, String kbPtah) throws IOException {
        super(loadFile(name, arity, totalRecords, kbPtah));
        this.name = name;
        this.id = id;
        entailmentFlags = new int[totalRows / BITS_PER_INT + ((0 == totalRows % BITS_PER_INT) ? 0 : 1)];
    }

    /**
     * Set a record as entailed if it is in the relation.
     */
    public void setAsEntailed(int[] record) {
        int idx = whereIs(record);
        if (NOT_FOUND != idx) {
            entailmentFlags[idx / BITS_PER_INT] |= 0x1 << (idx % BITS_PER_INT);
        }
    }

    /**
     * Set a record as not entailed if it is in the relation.
     */
    public void setAsNotEntailed(int[] record) {
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

    /**
     * Find the promising constants according to current records.
     */
    public int[][] getPromisingConstants() {
        int[][] promising_constants_by_cols = new int[totalCols][];
        final int threshold = (int) Math.ceil(totalRows * MIN_CONSTANT_COVERAGE);
        for (int col = 0; col < totalCols; col++) {
            List<Integer> promising_constants = new ArrayList<>();
            int[] values = valuesByCols[col];
            int[] start_offsets = startOffsetsByCols[col];
            for (int i = 0; i < values.length; i++) {
                if (threshold <= start_offsets[i + 1] - start_offsets[i]) {
                    promising_constants.add(values[i]);
                }
            }
            promising_constants_by_cols[col] = ArrayOperation.toArray(promising_constants);
        }
        return promising_constants_by_cols;
    }
}
