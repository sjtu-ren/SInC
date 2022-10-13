package sinc2.kb.compact;

import sinc2.util.ArrayOperation;

import java.util.*;

/**
 * This class is for indexing a large 2D table of integers. The table is sorted according to each column. That is,
 * orderedRowsByCols[i] stores the references of the rows sorted, in ascending order, by the ith argument of each row.
 * valuesByCols[i] will be a 1D array of the values occur in the ith arguments of the rows, no duplication, sorted in
 * ascending order. The first n element (n is the number of rows in the table) in the 1D array startIdxByCols[i] stores
 * the first offset of the row in orderedRowsByCols[i] that the corresponding argument value occurs. That is, if
 * startIdxByCols[i][j]=d, startIdxByCols[i][j+1]=e, and valuesByCols[i][j]=v, that means for these rows:
 *   orderedRowsByCols[i][d-1]
 *   orderedRowsByCols[i][d]
 *   orderedRowsByCols[i][d+1]
 *   ...
 *   orderedRowsByCols[i][e-1]
 *   orderedRowsByCols[i][e]
 * the following holds:
 *   orderedRowsByCols[i][d-1][i]!=v
 *   orderedRowsByCols[i][d][i]=v
 *   orderedRowsByCols[i][d+1][i]=v
 *   ...
 *   orderedRowsByCols[i][e-1][i]=v
 *   orderedRowsByCols[i][e][i]!=v
 * We also append one more element, n, to startIdxByCols[i] indicating the end of the rows.
 *
 * Suppose the memory cost of all rows is M, the total space of this type of index will be no more than 3M. The weakness
 * of this data structure is the query time. The existence query time is about O(log n) if the values in the rows are
 * randomly distributed in at least one column. Therefore, we require that there are NO duplicated rows in the table.
 *
 * @since 2.1
 */
public class IntTable implements Iterable<int[]> {

    /** This value indicate that a certain row is not found in the table */
    public static final int NOT_FOUND = -1;

    /** Row references sorted by each column in ascending order */
    protected final int[][][] sortedRowsByCols;
    /** The index values of each column */
    protected final int[][] valuesByCols;
    /** The starting offset of each index value */
    protected final int[][] startOffsetsByCols;
    /** The column that should be used for existential queries */
    protected final int queryCol;
    /** Total rows in the table */
    protected final int totalRows;
    /** Total cols in the table */
    protected final int totalCols;

    /**
     * Creating a IntTable by an array of rows. There should NOT be any duplicated rows in the array, and all the rows
     * should be in the same length. The array should NOT be empty.
     */
    public IntTable(int[][] rows) {
        totalCols = rows[0].length;
        totalRows = rows.length;
        sortedRowsByCols = new int[totalCols][][];
        valuesByCols = new int[totalCols][];
        startOffsetsByCols = new int[totalCols][];
        int max_values = 0;
        int max_val_idx = 0;
        for (int col = totalCols - 1; col >= 0; col--) {
            /* Sort by values in the column */
            final int _col = col;
            Arrays.sort(rows, Comparator.comparingInt(e -> e[_col]));
            int[][] sorted_rows = rows.clone();
            List<Integer> values = new ArrayList<>(totalRows);
            List<Integer> start_offset = new ArrayList<>(totalRows);

            /* Find the position of each value */
            int current_val = sorted_rows[0][col];
            values.add(current_val);
            start_offset.add(0);
            for (int i = 1; i < totalRows; i++) {
                if (current_val != sorted_rows[i][col]) {
                    current_val = sorted_rows[i][col];
                    values.add(current_val);
                    start_offset.add(i);
                }
            }
            start_offset.add(totalRows);
            sortedRowsByCols[col] = sorted_rows;
            valuesByCols[col] = ArrayOperation.toArray(values);
            startOffsetsByCols[col] = ArrayOperation.toArray(start_offset);

            /* Find the column with the maximum values, and this column will be used for existence query */
            if (max_values < valuesByCols[col].length) {
                max_values = valuesByCols[col].length;
                max_val_idx = col;
            }
        }
        queryCol = max_val_idx;
    }

    /**
     * Check whether a row is in the table.
     */
    public boolean hasRow(int[] row) {
        return 0 <= whereIs(row);
    }

    /**
     * Find the offset, w.r.t. the sorted column used for queries, of the row in the table.
     * @return The offset of the row, or NOT_FOUND if the row is not in the table.
     */
    protected int whereIs(int[] row) {
        final int[] values = valuesByCols[queryCol];
        int idx = Arrays.binarySearch(values, row[queryCol]);
        if (0 > idx) {
            return NOT_FOUND;
        }
        final int[] start_offsets = startOffsetsByCols[queryCol];
        final int[][] rows = sortedRowsByCols[queryCol];
        for (int i = start_offsets[idx]; i < start_offsets[idx + 1]; i++) {
            if (Arrays.equals(row, rows[i])) {
                return i;
            }
        }
        return NOT_FOUND;
    }

    @Override
    public Iterator<int[]> iterator() {
        return Arrays.stream(sortedRowsByCols[0]).iterator();
    }

    /**
     * Get a slice of the table where for every row r in the slice, r[col]=val.
     */
    public int[][] getSlice(int col, int val) {
        int idx = Arrays.binarySearch(valuesByCols[col], val);
        if (0 > idx) {
            return new int[0][];
        }
        final int[] start_offsets = startOffsetsByCols[col];
        int start_offset = start_offsets[idx];
        int length = start_offsets[idx + 1] - start_offset;
        int[][] slice = new int[length][];
        System.arraycopy(sortedRowsByCols[col], start_offset, slice, 0, length);
        return slice;
    }

    /**
     * Select and create a new IntTable. For every row r in the new table, r[col]=val.
     * @return The new table, or NULL if no such row in the original table.
     */
    public IntTable select(int col, int val) {
        int[][] slice = getSlice(col, val);
        if (0 == slice.length) {
            return null;
        }
        return new IntTable(slice);
    }

    static public class MatchedSubTables {
        public final List<IntTable> subTables1;
        public final List<IntTable> subTables2;

        public MatchedSubTables(List<IntTable> subTables1, List<IntTable> subTables2) {
            this.subTables1 = subTables1;
            this.subTables2 = subTables2;
        }
    }

    /**
     * Match the values of two columns in two IntTables. For each of the matched value v, derive a pair of new sub-tables
     * sub_tab1 and sub_tab2, such that: 1) for each row r1 in sub_tab1, r1 is in tab1, r1[col1]=v, and there is no row
     * r1' in tab1 but not in sub_tab1 that r1'[col]=v; 2) for each row r2 in sub_tab2, r2 is in tab2, t2[col2]=v, and
     * there is no row r2' in tab2 but not in sub_tab2 that r2'[col]=v.
     *
     * @return Two arrays of matched sub-tables. Each pair of sub-tables, subTables1[i] and subTables2[i], satisfies the
     * above restrictions.
     */
    static MatchedSubTables matchAsSubTables(IntTable tab1, int col1, IntTable tab2, int col2) {
        final int[] values1 = tab1.valuesByCols[col1];
        final int[] values2 = tab2.valuesByCols[col2];
        final int[] start_offsets1 = tab1.startOffsetsByCols[col1];
        final int[] start_offsets2 = tab2.startOffsetsByCols[col2];
        final int[][] sorted_rows1 = tab1.sortedRowsByCols[col1];
        final int[][] sorted_rows2 = tab2.sortedRowsByCols[col2];
        List<IntTable> sub_tables1 = new ArrayList<>();
        List<IntTable> sub_tables2 = new ArrayList<>();
        int idx1 = 0;
        int idx2 = 0;
        while (idx1 < values1.length && idx2 < values2.length) {
            int val1 = values1[idx1];
            int val2 = values2[idx2];
            if (val1 < val2) {
                idx1++;
            } else if (val1 > val2) {
                idx2++;
            } else {    // val1 == val2
                int start_idx1 = start_offsets1[idx1];
                int length1 = start_offsets1[idx1+1] - start_idx1;
                int[][] slice1 = new int[length1][];
                System.arraycopy(sorted_rows1, start_idx1, slice1, 0, length1);
                sub_tables1.add(new IntTable(slice1));

                int start_idx2 = start_offsets2[idx2];
                int length2 = start_offsets2[idx2+1] - start_idx2;
                int[][] slice2 = new int[length2][];
                System.arraycopy(sorted_rows2, start_idx2, slice2, 0, length2);
                sub_tables2.add(new IntTable(slice2));

                idx1++;
                idx2++;
            }
        }
        return new MatchedSubTables(sub_tables1, sub_tables2);
    }

    public int totalRows() {
        return totalRows;
    }

    public int totalCols() {
        return totalCols;
    }
}
