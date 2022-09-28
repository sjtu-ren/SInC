package sinc2.impl.base;

import sinc2.SincRecovery;
import sinc2.kb.*;
import sinc2.rule.Rule;

import java.util.*;

/**
 * Recover the KB by iteratively apply the hypothesis until a fixed point.
 *
 * @since 2.0
 */
public class SincRecoveryBasic extends SincRecovery {
    @Override
    public NumeratedKb recover(CompressedKb compressedKb, String decompressedName) throws KbException {
        /* Create a KB with necessary facts */
        NumeratedKb decompressed_kb = new NumeratedKb(compressedKb, decompressedName);

        /* Iteratively find new entailments until a fixed point */
        int new_facts;
        do {
            new_facts = 0;
            for (Rule rule: compressedKb.getHypothesis()) {
                Set<Record> inferred_records = Inference.infer(rule, decompressed_kb);
                int target_relation_num = rule.getHead().functor;
                for (Record record: inferred_records) {
                    if (!compressedKb.hasCounterexample(target_relation_num, record.args)) {
                        decompressed_kb.addRecord(target_relation_num, record);
                        new_facts++;
                    }
                }
            }
        } while (0 < new_facts);

        return decompressed_kb;
    }
}
