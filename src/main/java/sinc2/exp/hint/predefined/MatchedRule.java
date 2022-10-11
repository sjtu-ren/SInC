package sinc2.exp.hint.predefined;

import sinc2.rule.Eval;

/**
 * This class is used for recording rule information matched from some template.
 *
 * @since 2.0
 */
public class MatchedRule {
    public final String rule;
    public final Eval eval;
    public final double factCoverage;

    public MatchedRule(String rule, Eval eval, double factCoverage) {
        this.rule = rule;
        this.eval = eval;
        this.factCoverage = factCoverage;
    }
}
