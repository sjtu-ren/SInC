package sinc.impl.pruned.tabu;

import sinc.SInC;
import sinc.SincConfig;
import sinc.common.Rule;
import sinc.common.RuleFingerPrint;
import sinc.impl.cached.recal.SincWithRecalculateCache;

import java.util.*;

public class SincWithTabuPruning extends SincWithRecalculateCache {

    /* 每次迭代只保留下次生成的长度的tabu rules */
    private Set<RuleFingerPrint> tabuFingerprintSet = new HashSet<>();
    private final TabuMonitor tabuMonitor = new TabuMonitor();

    public SincWithTabuPruning(SincConfig config, String kbPath, String dumpPath, String logPath) {
        super(config, kbPath, dumpPath, logPath);
    }

    @Override
    protected Rule getStartRule(String headFunctor, Set<RuleFingerPrint> cache) {
        return new TabuAwareRule(headFunctor, cache, kb, tabuFingerprintSet);
    }

    @Override
    protected void targetDone(String functor) {
        /* 在每个Head变换之后都需要Change Tabu set */
        tabuMonitor.tabusInDiffHeadFunctor.add(tabuFingerprintSet.size());
        tabuMonitor.totalTabus += tabuFingerprintSet.size();
        tabuFingerprintSet = new HashSet<>();
    }

    @Override
    protected void recordRuleStatus(Rule rule, Rule.UpdateStatus updateStatus) {
        super.recordRuleStatus(rule, updateStatus);
        TabuAwareRule tabu_rule = (TabuAwareRule) rule;
        tabuMonitor.tabuCheckCostInNano += tabu_rule.tabuCheckCostInNano;
        tabuMonitor.totalTabuCompares += tabu_rule.tabuCompares;
        if (updateStatus == Rule.UpdateStatus.INSUFFICIENT_COVERAGE) {
            tabuFingerprintSet.add(rule.getFingerPrint());
        }
    }

    @Override
    protected void showMonitor() {
        super.showMonitor();
        tabuMonitor.show(logger);
    }
}
