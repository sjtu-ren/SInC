package sinc.impl.pruned.tabu;

import sinc.common.RuleFingerPrint;
import sinc.impl.cached.MemKB;
import sinc.impl.cached.recal.RecalculateCachedRule;
import sinc.util.MultiSet;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TabuAwareRule extends RecalculateCachedRule {

    private final Map<MultiSet<String>, Set<RuleFingerPrint>> category2TabuSetMap;
    public long tabuCheckCostInNano = 0;
    public int tabuCompares = 0;

    public TabuAwareRule(
            String headFunctor, Set<RuleFingerPrint> cache, MemKB kb,
            Map<MultiSet<String>, Set<RuleFingerPrint>> category2TabuSetMap
    ) {
        super(headFunctor, cache, kb);
        this.category2TabuSetMap = category2TabuSetMap;
    }

    public TabuAwareRule(TabuAwareRule another) {
        super(another);
        this.category2TabuSetMap = another.category2TabuSetMap;
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
        for (int subset_size = 0; subset_size < structure.size(); subset_size++) {
            for (MultiSet<String> category_subset : categorySubsets(subset_size)) {
                final Set<RuleFingerPrint> tabu_set = category2TabuSetMap.get(category_subset);
                if (null == tabu_set) continue;
                for (RuleFingerPrint rfp : tabu_set) {
                    tabuCompares++;
                    if (rfp.predecessorOf(this.fingerPrint)) {
                        hit = true;
                        break;
                    }
                }
                if (hit) break;
            }
        }
        final long check_done = System.nanoTime();
        tabuCheckCostInNano = check_done - check_begin;
        return hit;
    }

    protected Set<MultiSet<String>> categorySubsets(int subsetSize) {
        final Set<MultiSet<String>> subsets = new HashSet<>();
        if (0 == subsetSize) {
            subsets.add(new MultiSet<>());
        } else {
            templateSubsetsHandler(subsets, new String[subsetSize], subsetSize - 1, 1);
        }
        return subsets;
    }

    protected void templateSubsetsHandler(Set<MultiSet<String>> subsets, String[] template, int depth, int startIdx) {
        if (0 < depth) {
            for (int pred_idx = startIdx; pred_idx < structure.size(); pred_idx++) {
                template[depth] = structure.get(pred_idx).functor;
                templateSubsetsHandler(subsets, template, depth-1, pred_idx+1);
            }
        } else {
            for (int pred_idx = startIdx; pred_idx < structure.size(); pred_idx++) {
                template[depth] = structure.get(pred_idx).functor;
                subsets.add(new MultiSet<>(template));
            }
        }
    }

    public UpdateStatus boundFreeVar2ExistingVar(
            final int predIdx, final int argIdx, final int varId
    ) {
        /* 改变Rule结构 */
        fingerPrint = boundFreeVar2ExistingVarUpdateStructure(predIdx, argIdx, varId);

        /* 检查合法性 */
        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        /* 检查是否命中Cache */
        if (!searchedFingerprints.add(fingerPrint)) {
            return UpdateStatus.DUPLICATED;
        }

        /* 检查是否被tabu剪枝 */
        if (tabuHit()) {
            return UpdateStatus.TABU_PRUNED;
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

        /* 检查合法性 */
        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        /* 检查是否命中Cache */
        if (!searchedFingerprints.add(fingerPrint)) {
            return UpdateStatus.DUPLICATED;
        }

        /* 检查是否被tabu剪枝 */
        if (tabuHit()) {
            return UpdateStatus.TABU_PRUNED;
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

        /* 检查合法性 */
        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        /* 检查是否命中Cache */
        if (!searchedFingerprints.add(fingerPrint)) {
            return UpdateStatus.DUPLICATED;
        }

        /* 检查是否被tabu剪枝 */
        if (tabuHit()) {
            return UpdateStatus.TABU_PRUNED;
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

        /* 检查合法性 */
        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        /* 检查是否命中Cache */
        if (!searchedFingerprints.add(fingerPrint)) {
            return UpdateStatus.DUPLICATED;
        }

        /* 检查是否被tabu剪枝 */
        if (tabuHit()) {
            return UpdateStatus.TABU_PRUNED;
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

        /* 检查合法性 */
        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        /* 检查是否命中Cache */
        if (!searchedFingerprints.add(fingerPrint)) {
            return UpdateStatus.DUPLICATED;
        }

        /* 检查是否被tabu剪枝 */
        if (tabuHit()) {
            return UpdateStatus.TABU_PRUNED;
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
