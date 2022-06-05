package sinc2.util;

import java.util.List;

/**
 * Helper functions for array operations.
 *
 * @since 2.0
 */
public class ArrayOperation {

    /**
     * Convert an "Integer" list to an "int" array.
     */
    public static int[] toArray(List<Integer> list){
        int[] array = new int[list.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
