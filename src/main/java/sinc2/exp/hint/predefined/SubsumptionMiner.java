package sinc2.exp.hint.predefined;

import sinc2.kb.Record;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Template miner for:
 * 3. Subsumption:
 *   h(X0, ..., Xk) :- p(X0, ..., Xk); [(h, p)]
 *
 * @since 2.0
 */
public class SubsumptionMiner extends TemplateMiner {

    protected int arity = 0;

    @Override
    public List<MatchedRule> matchTemplate(
            List<Set<Record>> relations, List<Set<Record>> positiveEntailments, List<String> functorNames
    ) {
        List<MatchedRule> matched_rules = new ArrayList<>();
        for (int p = 0; p < relations.size(); p++) {
            Set<Record> relation_p = relations.get(p);
            if (relation_p.isEmpty()) {
                continue;
            }
            arity = relation_p.iterator().next().args.length;

            /* Match head & check validness */
            for (int h = 0; h < relations.size(); h++) {
                if (h == p) {
                    continue;
                }
                Set<Record> head = relations.get(h);
                if (arity != head.iterator().next().args.length) {
                    continue;
                }
                checkThenAdd(
                        head, positiveEntailments.get(h), relation_p, matched_rules,
                        subsumptionRuleString(h, p, functorNames)
                );
            }
        }
        return matched_rules;
    }

    protected String subsumptionRuleString(int h, int p, List<String> functorNames) {
        StringBuilder builder = new StringBuilder();
        builder.append('(').append("X0");   // Assumes that the arity here is no less than 1
        for (int i = 1; i < arity; i++) {
            builder.append(',').append('X').append(i);
        }
        builder.append(')');
        String arg_str = builder.toString();
        return String.format("%s%s:-%s%s", functorNames.get(h), arg_str, functorNames.get(p), arg_str);
    }

    @Override
    public int templateLength() {
        return arity * 2;
    }

    @Override
    public String templateName() {
        return "Subsumption";
    }
}
