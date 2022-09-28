package sinc2.exp;

import org.junit.jupiter.api.Test;
import sinc2.common.ParsedArg;
import sinc2.common.ParsedPred;
import sinc2.rule.Rule;
import sinc2.rule.RuleParseException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleDuplicationCheckerTest {
    @Test
    void testPositive1() throws RuleParseException {
        String[][] rule_pairs = new String[][]{
                new String[]{
                        "p(a,?,b):-",
                        "p(a,?,b):-"
                },
                new String[]{
                        "p(X,Y):-",
                        "p(Y,X):-"
                },
                new String[]{
                        "p(X,Y,Z):-q(Y,Z,X)",
                        "p(Y,Z,X):-q(Z,X,Y)"
                },
                new String[]{
                        "pred(X,con,Y):-body(tom,?)",
                        "pred(Y,con,X):-body(tom,?)"
                },
                new String[]{
                        "p(X,Y):-q(X,Z),q(Z,W),q(W,R),q(R,Y)",
                        "p(Y,X):-q(Y,R),q(R,Z),q(Z,W),q(W,X)"
                }
        };
        int failed = 0;
        for (int i = 0; i < rule_pairs.length; i++) {
            List<ParsedPred> rule1 = Rule.parseStructure(rule_pairs[i][0]);
            List<ParsedPred> rule2 = Rule.parseStructure(rule_pairs[i][1]);
            if (!RuleDuplicationChecker.matchRules(rule1, rule2)) {
                System.out.printf("Should match but not (@%d):\n", i);
                System.out.println(Rule.toString(rule1));
                System.out.println(Rule.toString(rule2));
                failed++;
            }
        }
        assertEquals(0, failed);
    }

    @Test
    void testNegative1() throws RuleParseException {
        String[][] rule_pairs = new String[][]{
                new String[]{
                        "parent(X,Y):-father(X,X),father(?,Y)",
                        "parent(X,Y):-father(X,Y),father(?,X)"
                },
                new String[]{
                        "parent(X,Y):-parent(X,Z),father(Z,W),father(?,R),mother(W,R)",
                        "parent(X,Y):-parent(X,Z),father(Z,R),father(?,W),mother(W,R)"
                },
                new String[]{
                        "p(X,Y,Z):-q(Y,Z,X)",
                        "p(Y,Z,X):-q(X,Z,Y)"
                },
                new String[]{
                        "p(X,Y):-q(X,Y)",
                        "p(X,Y):-q(X,?),q(?,Y)"
                }
        };
        int failed = 0;
        for (int i = 0; i < rule_pairs.length; i++) {
            List<ParsedPred> rule1 = Rule.parseStructure(rule_pairs[i][0]);
            List<ParsedPred> rule2 = Rule.parseStructure(rule_pairs[i][1]);
            if (RuleDuplicationChecker.matchRules(rule1, rule2)) {
                System.out.printf("Should not but match (@%d):\n", i);
                System.out.println(Rule.toString(rule1));
                System.out.println(Rule.toString(rule2));
                failed++;
            }
        }
        assertEquals(0, failed);
    }

    @Test
    void testPositive2() {
        String[][] rule_pair_strs = new String[][]{
                new String[]{
                        "sister(X0,X1):-uncle(X1,?),aunt(X0,?),gender(?,X1)",
                        "sister(X1,X0):-gender(?,X0),uncle(X0,?),aunt(X1,?)"
                }
        };
        List<ParsedPred>[][] rule_pairs = new List[][]{
                new List[] {
                        new ArrayList<>(List.of(
                                new ParsedPred("sister", new ParsedArg[]{ParsedArg.variable(0), ParsedArg.variable(1)}),
                                new ParsedPred("uncle", new ParsedArg[]{ParsedArg.variable(1), null}),
                                new ParsedPred("aunt", new ParsedArg[]{ParsedArg.variable(0), null}),
                                new ParsedPred("gender", new ParsedArg[]{null, ParsedArg.variable(1)})
                        )),
                        new ArrayList<>(List.of(
                                new ParsedPred("sister", new ParsedArg[]{ParsedArg.variable(1), ParsedArg.variable(0)}),
                                new ParsedPred("gender", new ParsedArg[]{null, ParsedArg.variable(0)}),
                                new ParsedPred("uncle", new ParsedArg[]{ParsedArg.variable(0), null}),
                                new ParsedPred("aunt", new ParsedArg[]{ParsedArg.variable(1), null})
                        ))
                }
        };
        int failed = 0;
        for (int i = 0; i < rule_pairs.length; i++) {
            List<ParsedPred> rule1 = rule_pairs[i][0];
            List<ParsedPred> rule2 = rule_pairs[i][1];
            assertEquals(rule_pair_strs[i][0], Rule.toString(rule1));
            assertEquals(rule_pair_strs[i][1], Rule.toString(rule2));
            if (!RuleDuplicationChecker.matchRules(rule1, rule2)) {
                System.out.printf("Should match but not (@%d):\n", i);
                System.out.println(Rule.toString(rule1));
                System.out.println(Rule.toString(rule2));
                failed++;
            }
        }
        assertEquals(0, failed);
    }
}