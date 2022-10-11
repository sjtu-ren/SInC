package sinc2.exp.hint.predefined;

import sinc2.kb.Record;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Template miner for:
 * 2. Reflexive:
 *   h(X, X) :-
 *
 * @since 2.0
 */
public class ReflexiveMiner extends TemplateMiner {
    @Override
    public List<MatchedRule> matchTemplate(
            List<Set<Record>> relations, List<Set<Record>> positiveEntailments, List<String> functorNames
    ) {
        List<MatchedRule> matched_rules = new ArrayList<>();
        for (int h = 0; h < relations.size(); h++) {
            Set<Record> head = relations.get(h);
            if (head.isEmpty() || 2 != head.iterator().next().args.length) {
                continue;
            }

            /* Find entailments */
            Set<Record> entailments = new HashSet<>();
            for (Record record: head) {
                if (record.args[0] == record.args[1]) {
                    entailments.add(record);
                }
            }

            /* Match head & check validness */
            checkThenAdd(
                    head, positiveEntailments.get(h), entailments, matched_rules,
                    String.format("%s(X,X):-", functorNames.get(h))
            );
        }
        return matched_rules;
    }

    @Override
    public int templateLength() {
        return 1;
    }

    @Override
    public String templateName() {
        return "Reflexive";
    }
}
