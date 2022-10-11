package sinc2.exp.hint.predefined;

import sinc2.kb.Record;

import java.util.*;

/**
 * Template miner for:
 * 1. Type Inference:
 *   h(X) :- p(..., Xi, ...)
 *
 * @since 2.0
 */
public class TypeInferenceMiner extends TemplateMiner {
    @Override
    public List<MatchedRule> matchTemplate(
            List<Set<Record>> relations, List<Set<Record>> positiveEntailments, List<String> functorNames
    ) {
        List<MatchedRule> matched_rules = new ArrayList<>();

        /* Check if there are type relations */
        List<Set<Record>> type_relations = new ArrayList<>();
        List<String> type_functor_names = new ArrayList<>();
        List<Set<Record>> type_pos_entails = new ArrayList<>();
        for (int i = 0; i < relations.size(); i++) {
            Set<Record> relation = relations.get(i);
            if (!relation.isEmpty() && 1 == relation.iterator().next().args.length) {
                type_relations.add(relation);
                type_functor_names.add(functorNames.get(i));
                type_pos_entails.add(positiveEntailments.get(i));
            }
        }
        if (type_relations.isEmpty()) {
            return matched_rules;
        }

        for (int p = 0; p < relations.size(); p++) {
            Set<Record> relation_p = relations.get(p);
            if (relation_p.isEmpty()) {
                continue;
            }
            int arity = relation_p.iterator().next().args.length;
            if (1 >= arity) {
                /* If the arity of p is also 1, the patterns are collided with subsumption */
                continue;
            }

            /* Match for every argument */
            for (int arg_idx = 0; arg_idx < arity; arg_idx++) {
                /* Find entailments */
                Set<Record> entailments = new HashSet<>();
                for (Record record: relation_p) {
                    entailments.add(new Record(new int[]{record.args[arg_idx]}));
                }

                /* Match types */
                for (int h = 0; h < type_relations.size(); h++) {
                    checkThenAdd(
                            type_relations.get(h), type_pos_entails.get(h), entailments, matched_rules,
                            typeInferenceRuleString(type_functor_names.get(h), functorNames.get(p), arg_idx, arity)
                    );
                }
            }
        }
        return matched_rules;
    }

    protected String typeInferenceRuleString(String headFunctor, String bodyFunctorName, int bodyArgIdx, int bodyArity) {
        return headFunctor + "(X):-" + bodyFunctorName + '(' + "?,".repeat(bodyArgIdx) + 'X' +
                ",?".repeat(bodyArity - bodyArgIdx - 1) + ')';
    }

    @Override
    public int templateLength() {
        return 1;
    }

    @Override
    public String templateName() {
        return "TypeInference";
    }
}
