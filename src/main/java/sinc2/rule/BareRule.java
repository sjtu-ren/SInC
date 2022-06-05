package sinc2.rule;

import sinc2.common.Predicate;
import sinc2.util.MultiSet;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A rule implementation with barely nothing but the structure and fingerprint operations. This class is used for testing
 * basic operations in the abstract class 'Rule' or for manipulating the rule structure only.
 *
 * @since 1.0
 */
public class BareRule extends Rule {
    public BareRule(int headFunctor, int arity, Set<Fingerprint> fingerprintCache, Map<MultiSet<Integer>, Set<Fingerprint>> category2TabuSetMap) {
        super(headFunctor, arity, fingerprintCache, category2TabuSetMap);
    }

    public BareRule(List<Predicate> structure, Set<Fingerprint> fingerprintCache, Map<MultiSet<Integer>, Set<Fingerprint>> category2TabuSetMap) {
        super(structure, fingerprintCache, category2TabuSetMap);
    }

    public BareRule(Rule another) {
        super(another);
    }

    @Override
    public BareRule clone() {
        return new BareRule(this);
    }

    @Override
    protected Eval calculateEval() {
        return new Eval(null, 0, 0, length);
    }

    @Override
    protected double factCoverage() {
        return 1.0;
    }

    @Override
    protected UpdateStatus cvt1Uv2ExtLvHandlerPreCvg(int predIdx, int argIdx, int varId) {
        return UpdateStatus.NORMAL;
    }

    @Override
    protected UpdateStatus cvt1Uv2ExtLvHandlerPostCvg(int predIdx, int argIdx, int varId) {
        return UpdateStatus.NORMAL;
    }

    @Override
    protected UpdateStatus cvt1Uv2ExtLvHandlerPreCvg(Predicate newPredicate, int argIdx, int varId) {
        return UpdateStatus.NORMAL;
    }

    @Override
    protected UpdateStatus cvt1Uv2ExtLvHandlerPostCvg(Predicate newPredicate, int argIdx, int varId) {
        return UpdateStatus.NORMAL;
    }

    @Override
    protected UpdateStatus cvt2Uvs2NewLvHandlerPreCvg(int predIdx1, int argIdx1, int predIdx2, int argIdx2) {
        return UpdateStatus.NORMAL;
    }

    @Override
    protected UpdateStatus cvt2Uvs2NewLvHandlerPostCvg(int predIdx1, int argIdx1, int predIdx2, int argIdx2) {
        return UpdateStatus.NORMAL;
    }

    @Override
    protected UpdateStatus cvt2Uvs2NewLvHandlerPreCvg(Predicate newPredicate, int argIdx1, int predIdx2, int argIdx2) {
        return UpdateStatus.NORMAL;
    }

    @Override
    protected UpdateStatus cvt2Uvs2NewLvHandlerPostCvg(Predicate newPredicate, int argIdx1, int predIdx2, int argIdx2) {
        return UpdateStatus.NORMAL;
    }

    @Override
    protected UpdateStatus cvt1Uv2ConstHandlerPreCvg(int predIdx, int argIdx, int constant) {
        return UpdateStatus.NORMAL;
    }

    @Override
    protected UpdateStatus cvt1Uv2ConstHandlerPostCvg(int predIdx, int argIdx, int constant) {
        return UpdateStatus.NORMAL;
    }

    @Override
    protected UpdateStatus rmAssignedArgHandlerPreCvg(int predIdx, int argIdx) {
        return UpdateStatus.NORMAL;
    }

    @Override
    protected UpdateStatus rmAssignedArgHandlerPostCvg(int predIdx, int argIdx) {
        return UpdateStatus.NORMAL;
    }

    @Override
    public Predicate[][] getEvidence() {
        return new Predicate[0][];
    }

    @Override
    public Set<Predicate> getCounterexamples() {
        return new HashSet<>();
    }
}
