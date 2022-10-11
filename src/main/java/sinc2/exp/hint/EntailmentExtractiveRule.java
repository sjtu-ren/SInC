package sinc2.exp.hint;

import sinc2.common.Predicate;
import sinc2.impl.base.CacheEntry;
import sinc2.impl.base.CachedRule;
import sinc2.kb.KbException;
import sinc2.kb.KbRelation;
import sinc2.kb.NumeratedKb;
import sinc2.rule.Fingerprint;
import sinc2.rule.Rule;
import sinc2.util.MultiSet;

import java.util.*;

/**
 * A subclass of "CachedRule" that can explicitly extract entailments to other data structures.
 *
 * @since 2.0
 */
public class EntailmentExtractiveRule extends CachedRule {

    public EntailmentExtractiveRule(
            int headRelNum, int arity, Set<Fingerprint> fingerprintCache,
            Map<MultiSet<Integer>, Set<Fingerprint>> category2TabuSetMap, NumeratedKb kb
    ) {
        super(headRelNum, arity, fingerprintCache, category2TabuSetMap, kb);
    }

    public EntailmentExtractiveRule(
            List<Predicate> structure, Set<Fingerprint> fingerprintCache,
            Map<MultiSet<Integer>, Set<Fingerprint>> category2TabuSetMap, NumeratedKb kb
    ) {
        super(structure, fingerprintCache, category2TabuSetMap, kb);
    }

    public EntailmentExtractiveRule(CachedRule another) {
        super(another);
    }

    @Override
    public EntailmentExtractiveRule clone() {
        return new EntailmentExtractiveRule(this);
    }

    /**
     * Extract positive entailments to a relation.
     */
    public void extractPositiveEntailments(KbRelation relation) throws KbException {
        for (CacheEntry entry: posCache) {
            relation.addRecords(entry.entry.get(0).complSet);
        }
    }

    /**
     * Override the original version to thread safe.
     */
    @Override
    protected void add2TabuSet() {
        final MultiSet<Integer> functor_mset = new MultiSet<>();
        for (int pred_idx = Rule.FIRST_BODY_PRED_IDX; pred_idx < structure.size(); pred_idx++) {
            functor_mset.add(structure.get(pred_idx).functor);
        }
        final Set<Fingerprint> tabu_set = category2TabuSetMap.computeIfAbsent(
                functor_mset, k -> Collections.synchronizedSet(new HashSet<>())
        );
        tabu_set.add(fingerprint);
    }
}
