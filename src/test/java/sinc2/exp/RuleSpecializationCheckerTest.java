package sinc2.exp;

import org.junit.jupiter.api.Test;
import sinc2.common.ParsedPred;
import sinc2.rule.Rule;
import sinc2.rule.RuleParseException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleSpecializationCheckerTest {

    @Test
    void testPositive() throws RuleParseException {
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
                },
                new String[]{
                        "sister(?,X):-gender(?,X)",
                        "sister(Y,X):-uncle(X,?),aunt(Y,?),gender(?,X)"
                },
                new String[]{
                        "p(X,?,?):-q(?,?,X)",
                        "p(Y,Z,X):-q(Z,X,Y)"
                },
                new String[]{
                        "pred(X,?,?):-body(tom,X)",
                        "pred(X,con,X):-body(tom,X)"
                }
        };
        int failed = 0;
        for (int i = 0; i < rule_pairs.length; i++) {
            List<ParsedPred> original = Rule.parseStructure(rule_pairs[i][0]);
            List<ParsedPred> specialization = Rule.parseStructure(rule_pairs[i][1]);
            if (!RuleSpecializationChecker.specializationOf(original, specialization)) {
                System.out.printf("Should be specialization but not (@%d):\n", i);
                System.out.println(Rule.toString(original));
                System.out.println(Rule.toString(specialization));
                failed++;
            }
        }
        assertEquals(0, failed);
    }

    @Test
    void testNegative() throws RuleParseException {
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
            List<ParsedPred> original = Rule.parseStructure(rule_pairs[i][0]);
            List<ParsedPred> specialization = Rule.parseStructure(rule_pairs[i][1]);
            if (RuleSpecializationChecker.specializationOf(original, specialization)) {
                System.out.printf("Should not be specialization but returned true (@%d):\n", i);
                System.out.println(Rule.toString(original));
                System.out.println(Rule.toString(specialization));
                failed++;
            }
        }
        assertEquals(0, failed);
    }
}