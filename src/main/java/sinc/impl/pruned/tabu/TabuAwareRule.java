package sinc.impl.pruned.tabu;

import sinc.common.RuleFingerPrint;
import sinc.impl.cached.MemKB;
import sinc.impl.cached.recal.RecalculateCachedRule;

import java.util.Set;

public class TabuAwareRule extends RecalculateCachedRule {

    private final Set<RuleFingerPrint> tabuFingerprintSet;
    public long tabuCheckCostInNano = 0;
    public int tabuCompares = 0;

    public TabuAwareRule(
            String headFunctor, Set<RuleFingerPrint> cache, MemKB kb, Set<RuleFingerPrint> tabuFingerprintSet
    ) {
        super(headFunctor, cache, kb);
        this.tabuFingerprintSet = tabuFingerprintSet;
    }

    public TabuAwareRule(TabuAwareRule another) {
        super(another);
        this.tabuFingerprintSet = another.tabuFingerprintSet;
    }

    @Override
    public TabuAwareRule clone() {
        final long time_start = System.nanoTime();
        TabuAwareRule r =  new TabuAwareRule(this);
        final long time_done = System.nanoTime();
        cacheMonitor.cloneCostInNano += time_done - time_start;
        return r;
    }

    protected boolean tabuHit() {
        boolean hit = false;
        final long check_begin = System.nanoTime();
        for (RuleFingerPrint rfp: tabuFingerprintSet) {
            tabuCompares++;
            if (rfp.predecessorOf(this.fingerPrint)) {
                hit = true;
                break;
            }
        }
        final long check_done = System.nanoTime();
        tabuCheckCostInNano = check_done - check_begin;
        return hit;
    }

    public UpdateStatus boundFreeVar2ExistingVar(
            final int predIdx, final int argIdx, final int varId
    ) {
        /* 改变Rule结构 */
        fingerPrint = boundFreeVar2ExistingVarUpdateStructure(predIdx, argIdx, varId);

        /* 检查是否被tabu剪枝 */
        if (tabuHit()) {
            return UpdateStatus.TABU_PRUNED;
        }

        /* 检查是否命中Cache */
        if (!searchedFingerprints.add(fingerPrint)) {
            return UpdateStatus.DUPLICATED;
        }

        /* 检查合法性 */
        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        /* 执行handler */
        final UpdateStatus status = boundFreeVar2ExistingVarHandler(predIdx, argIdx, varId);
        if (UpdateStatus.NORMAL != status) {
            return status;
        }

        /* 更新Eval */
        this.eval = calculateEval();
        return UpdateStatus.NORMAL;
    }

    public UpdateStatus boundFreeVar2ExistingVar(
            final String functor, final int arity, final int argIdx, final int varId
    ) {
        /* 改变Rule结构 */
        fingerPrint = boundFreeVar2ExistingVarUpdateStructure(functor, arity, argIdx, varId);

        /* 检查是否被tabu剪枝 */
        if (tabuHit()) {
            return UpdateStatus.TABU_PRUNED;
        }

        /* 检查是否命中Cache */
        if (!searchedFingerprints.add(fingerPrint)) {
            return UpdateStatus.DUPLICATED;
        }

        /* 检查合法性 */
        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        /* 执行handler */
        final UpdateStatus status = boundFreeVar2ExistingVarHandler(structure.get(structure.size() - 1), argIdx, varId);
        if (UpdateStatus.NORMAL != status) {
            return status;
        }

        /* 更新Eval */
        this.eval = calculateEval();
        return UpdateStatus.NORMAL;
    }

    public UpdateStatus boundFreeVars2NewVar(
            final int predIdx1, final int argIdx1, final int predIdx2, final int argIdx2
    ) {
        /* 改变Rule结构 */
        fingerPrint = boundFreeVars2NewVarUpdateStructure(predIdx1, argIdx1, predIdx2, argIdx2);

        /* 检查是否被tabu剪枝 */
        if (tabuHit()) {
            return UpdateStatus.TABU_PRUNED;
        }

        /* 检查是否命中Cache */
        if (!searchedFingerprints.add(fingerPrint)) {
            return UpdateStatus.DUPLICATED;
        }

        /* 检查合法性 */
        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        /* 执行handler */
        final UpdateStatus status = boundFreeVars2NewVarHandler(predIdx1, argIdx1, predIdx2, argIdx2);
        if (UpdateStatus.NORMAL != status) {
            return status;
        }

        /* 更新Eval */
        this.eval = calculateEval();
        return UpdateStatus.NORMAL;
    }

    public UpdateStatus boundFreeVars2NewVar(
            final String functor, final int arity, final int argIdx1, final int predIdx2, final int argIdx2
    ) {
        /* 改变Rule结构 */
        fingerPrint = boundFreeVars2NewVarUpdateStructure(functor, arity, argIdx1, predIdx2, argIdx2);

        /* 检查是否被tabu剪枝 */
        if (tabuHit()) {
            return UpdateStatus.TABU_PRUNED;
        }

        /* 检查是否命中Cache */
        if (!searchedFingerprints.add(fingerPrint)) {
            return UpdateStatus.DUPLICATED;
        }

        /* 检查合法性 */
        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        /* 执行handler */
        final UpdateStatus status = boundFreeVars2NewVarHandler(
                structure.get(structure.size() - 1), argIdx1, predIdx2, argIdx2
        );
        if (UpdateStatus.NORMAL != status) {
            return status;
        }

        /* 更新Eval */
        this.eval = calculateEval();
        return UpdateStatus.NORMAL;
    }

    public UpdateStatus boundFreeVar2Constant(final int predIdx, final int argIdx, final String constantSymbol) {
        /* 改变Rule结构 */
        fingerPrint = boundFreeVar2ConstantUpdateStructure(predIdx, argIdx, constantSymbol);

        /* 检查是否被tabu剪枝 */
        if (tabuHit()) {
            return UpdateStatus.TABU_PRUNED;
        }

        /* 检查是否命中Cache */
        if (!searchedFingerprints.add(fingerPrint)) {
            return UpdateStatus.DUPLICATED;
        }

        /* 检查合法性 */
        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        /* 执行handler */
        final UpdateStatus status = boundFreeVar2ConstantHandler(predIdx, argIdx, constantSymbol);
        if (UpdateStatus.NORMAL != status) {
            return status;
        }

        /* 更新Eval */
        this.eval = calculateEval();
        return UpdateStatus.NORMAL;
    }
}
