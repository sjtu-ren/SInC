package sinc.exp;

import org.junit.jupiter.api.Test;
import sinc.common.*;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CompareQueryTest {
    @Test
    void testSelectOnRule() {
        final int index = 2;
        final String value = "bob";

        /* family(X, Y, Z) :- mother(X, Z), father(Y, Z) */
        Predicate head1 = new Predicate("family", 3);
        head1.args[0] = new Variable(0);
        head1.args[1] = new Variable(1);
        head1.args[2] = new Variable(2);
        Predicate body11 = new Predicate("mother", 2);
        body11.args[0] = new Variable(0);
        body11.args[1] = new Variable(2);
        Predicate body12 = new Predicate("father", 2);
        body12.args[0] = new Variable(1);
        body12.args[1] = new Variable(2);
        Rule rule1 = new BareRule(List.of(head1, body11, body12), new HashSet<>());
        assertEquals("family(X0,X1,X2):-mother(X0,X2),father(X1,X2)", rule1.toDumpString());
        Rule selected_rule1 = CompareQuery.selectOnRule(rule1, index, value);
        assertEquals("family(X0,X1,bob):-mother(X0,bob),father(X1,bob)", selected_rule1.toDumpString());

        /* family(X, Y, ?) :- mother(X, Z), father(Y, Z) */
        Predicate head2 = new Predicate("family", 3);
        head2.args[0] = new Variable(0);
        head2.args[1] = new Variable(1);
        head2.args[2] = null;
        Predicate body21 = new Predicate("mother", 2);
        body21.args[0] = new Variable(0);
        body21.args[1] = new Variable(2);
        Predicate body22 = new Predicate("father", 2);
        body22.args[0] = new Variable(1);
        body22.args[1] = new Variable(2);
        Rule rule2 = new BareRule(List.of(head2, body21, body22), new HashSet<>());
        assertEquals("family(X0,X1,?):-mother(X0,X2),father(X1,X2)", rule2.toDumpString());
        Rule selected_rule2 = CompareQuery.selectOnRule(rule2, index, value);
        assertEquals("family(X0,X1,bob):-mother(X0,X2),father(X1,X2)", selected_rule2.toDumpString());

        /* family(X, Y, alice) :- mother(X, Z), father(Y, Z) */
        Predicate head3 = new Predicate("family", 3);
        head3.args[0] = new Variable(0);
        head3.args[1] = new Variable(1);
        head3.args[2] = new Constant(Rule.CONSTANT_ARG_ID, "alice");
        Predicate body31 = new Predicate("mother", 2);
        body31.args[0] = new Variable(0);
        body31.args[1] = new Variable(2);
        Predicate body32 = new Predicate("father", 2);
        body32.args[0] = new Variable(1);
        body32.args[1] = new Variable(2);
        Rule rule3 = new BareRule(List.of(head3, body31, body32), new HashSet<>());
        assertEquals("family(X0,X1,alice):-mother(X0,X2),father(X1,X2)", rule3.toDumpString());
        assertNull(CompareQuery.selectOnRule(rule3, index, value));

        /* family(X, Y, bob) :- mother(X, Z), father(Y, Z) */
        Predicate head4 = new Predicate("family", 3);
        head4.args[0] = new Variable(0);
        head4.args[1] = new Variable(1);
        head4.args[2] = new Constant(Rule.CONSTANT_ARG_ID, "bob");
        Predicate body41 = new Predicate("mother", 2);
        body41.args[0] = new Variable(0);
        body41.args[1] = new Variable(2);
        Predicate body42 = new Predicate("father", 2);
        body42.args[0] = new Variable(1);
        body42.args[1] = new Variable(2);
        Rule rule4 = new BareRule(List.of(head4, body41, body42), new HashSet<>());
        assertEquals("family(X0,X1,bob):-mother(X0,X2),father(X1,X2)", rule4.toDumpString());
        Rule selected_rule4 = CompareQuery.selectOnRule(rule2, index, value);
        assertEquals("family(X0,X1,bob):-mother(X0,X2),father(X1,X2)", selected_rule4.toDumpString());
    }

    @Test
    void testProductOnRules() {
        final int index1 = 1;
        final int index2 = 0;

        /* family(X, Y, Z) :- mother(X, Z), father(Y, Z) */
        /* brother(X, Y) :- father(Z, X), father(Z, Y), isMale(X) */
        Predicate head11 = new Predicate("family", 3);
        head11.args[0] = new Variable(0);
        head11.args[1] = new Variable(1);
        head11.args[2] = new Variable(2);
        Predicate body111 = new Predicate("mother", 2);
        body111.args[0] = new Variable(0);
        body111.args[1] = new Variable(2);
        Predicate body112 = new Predicate("father", 2);
        body112.args[0] = new Variable(1);
        body112.args[1] = new Variable(2);
        Rule rule11 = new BareRule(List.of(head11, body111, body112), new HashSet<>());
        assertEquals("family(X0,X1,X2):-mother(X0,X2),father(X1,X2)", rule11.toDumpString());
        Predicate head12 = new Predicate("brother", 2);
        head12.args[0] = new Variable(0);
        head12.args[1] = new Variable(1);
        Predicate body121 = new Predicate("father", 2);
        body121.args[0] = new Variable(2);
        body121.args[1] = new Variable(0);
        Predicate body122 = new Predicate("father", 2);
        body122.args[0] = new Variable(2);
        body122.args[1] = new Variable(1);
        Predicate body123 = new Predicate("isMale", 1);
        body123.args[0] = new Variable(0);
        Rule rule12 = new BareRule(List.of(head12, body121, body122, body123), new HashSet<>());
        assertEquals("brother(X0,X1):-father(X2,X0),father(X2,X1),isMale(X0)", rule12.toDumpString());
        Rule product_rule1 = CompareQuery.productOnRules(rule11, index1, 3, rule12, index2, 2);
        assertEquals(
                "product(X3,X0,X5,X0,X1):-mother(X3,X5),father(X0,X5),father(X2,X0),father(X2,X1),isMale(X0)",
                product_rule1.toDumpString()
        );

        /* family(X, ?, Z) :- mother(X, Z), father(?, Z) */
        /* brother(X, Y) :- father(Z, X), father(Z, Y), isMale(X) */
        Predicate head21 = new Predicate("family", 3);
        head21.args[0] = new Variable(0);
        head21.args[1] = null;
        head21.args[2] = new Variable(2);
        Predicate body211 = new Predicate("mother", 2);
        body211.args[0] = new Variable(0);
        body211.args[1] = new Variable(2);
        Predicate body212 = new Predicate("father", 2);
        body212.args[0] = null;
        body212.args[1] = new Variable(2);
        Rule rule21 = new BareRule(List.of(head21, body211, body212), new HashSet<>());
        assertEquals("family(X0,?,X2):-mother(X0,X2),father(?,X2)", rule21.toDumpString());
        Predicate head22 = new Predicate("brother", 2);
        head22.args[0] = new Variable(0);
        head22.args[1] = new Variable(1);
        Predicate body221 = new Predicate("father", 2);
        body221.args[0] = new Variable(2);
        body221.args[1] = new Variable(0);
        Predicate body222 = new Predicate("father", 2);
        body222.args[0] = new Variable(2);
        body222.args[1] = new Variable(1);
        Predicate body223 = new Predicate("isMale", 1);
        body223.args[0] = new Variable(0);
        Rule rule22 = new BareRule(List.of(head22, body221, body222, body223), new HashSet<>());
        assertEquals("brother(X0,X1):-father(X2,X0),father(X2,X1),isMale(X0)", rule22.toDumpString());
        Rule product_rule2 = CompareQuery.productOnRules(rule21, index1, 3, rule22, index2, 2);
        assertEquals(
                "product(X3,X0,X5,X0,X1):-mother(X3,X5),father(?,X5),father(X2,X0),father(X2,X1),isMale(X0)",
                product_rule2.toDumpString()
        );

        /* family(X, Y, Z) :- mother(X, Z), father(Y, Z) */
        /* brother(bob, Y) :- father(Z, bob), father(Z, Y), isMale(bob) */
        Predicate head31 = new Predicate("family", 3);
        head31.args[0] = new Variable(0);
        head31.args[1] = new Variable(1);
        head31.args[2] = new Variable(2);
        Predicate body311 = new Predicate("mother", 2);
        body311.args[0] = new Variable(0);
        body311.args[1] = new Variable(2);
        Predicate body312 = new Predicate("father", 2);
        body312.args[0] = new Variable(1);
        body312.args[1] = new Variable(2);
        Rule rule31 = new BareRule(List.of(head31, body311, body312), new HashSet<>());
        assertEquals("family(X0,X1,X2):-mother(X0,X2),father(X1,X2)", rule31.toDumpString());
        Predicate head32 = new Predicate("brother", 2);
        head32.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "bob");
        head32.args[1] = new Variable(1);
        Predicate body321 = new Predicate("father", 2);
        body321.args[0] = new Variable(2);
        body321.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "bob");
        Predicate body322 = new Predicate("father", 2);
        body322.args[0] = new Variable(2);
        body322.args[1] = new Variable(1);
        Predicate body323 = new Predicate("isMale", 1);
        body323.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "bob");
        Rule rule32 = new BareRule(List.of(head32, body321, body322, body323), new HashSet<>());
        assertEquals("brother(bob,X1):-father(X2,bob),father(X2,X1),isMale(bob)", rule32.toDumpString());
        Rule product_rule3 = CompareQuery.productOnRules(rule31, index1, 3, rule32, index2, 2);
        assertEquals(
                "product(X3,bob,X5,bob,X1):-mother(X3,X5),father(bob,X5),father(X2,bob),father(X2,X1),isMale(bob)",
                product_rule3.toDumpString()
        );

        /* family(X, ?, Z) :- mother(X, Z), father(?, Z) */
        /* brother(?, Y) :- father(Z, ?), father(Z, Y), isMale(?) */
        Predicate head41 = new Predicate("family", 3);
        head41.args[0] = new Variable(0);
        head41.args[1] = null;
        head41.args[2] = new Variable(2);
        Predicate body411 = new Predicate("mother", 2);
        body411.args[0] = new Variable(0);
        body411.args[1] = new Variable(2);
        Predicate body412 = new Predicate("father", 2);
        body412.args[0] = null;
        body412.args[1] = new Variable(2);
        Rule rule41 = new BareRule(List.of(head41, body411, body412), new HashSet<>());
        assertEquals("family(X0,?,X2):-mother(X0,X2),father(?,X2)", rule41.toDumpString());
        Predicate head42 = new Predicate("brother", 2);
        head42.args[0] = null;
        head42.args[1] = new Variable(1);
        Predicate body421 = new Predicate("father", 2);
        body421.args[0] = new Variable(2);
        body421.args[1] = null;
        Predicate body422 = new Predicate("father", 2);
        body422.args[0] = new Variable(2);
        body422.args[1] = new Variable(1);
        Predicate body423 = new Predicate("isMale", 1);
        body423.args[0] = null;
        Rule rule42 = new BareRule(List.of(head42, body421, body422, body423), new HashSet<>());
        assertEquals("brother(?,X1):-father(X2,?),father(X2,X1),isMale(?)", rule42.toDumpString());
        Rule product_rule4 = CompareQuery.productOnRules(rule41, index1, 3, rule42, index2, 2);
        assertEquals(
                "product(X3,X6,X5,X6,X1):-mother(X3,X5),father(?,X5),father(X2,?),father(X2,X1),isMale(?)",
                product_rule4.toDumpString()
        );

        /* family(X, bob, Z) :- mother(X, Z), father(bob, Z) */
        /* brother(?, Y) :- father(Z, ?), father(Z, Y), isMale(?) */
        Predicate head51 = new Predicate("family", 3);
        head51.args[0] = new Variable(0);
        head51.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "bob");
        head51.args[2] = new Variable(2);
        Predicate body511 = new Predicate("mother", 2);
        body511.args[0] = new Variable(0);
        body511.args[1] = new Variable(2);
        Predicate body512 = new Predicate("father", 2);
        body512.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "bob");
        body512.args[1] = new Variable(2);
        Rule rule51 = new BareRule(List.of(head51, body511, body512), new HashSet<>());
        assertEquals("family(X0,bob,X2):-mother(X0,X2),father(bob,X2)", rule51.toDumpString());
        Predicate head52 = new Predicate("brother", 2);
        head52.args[0] = null;
        head52.args[1] = new Variable(1);
        Predicate body521 = new Predicate("father", 2);
        body521.args[0] = new Variable(2);
        body521.args[1] = null;
        Predicate body522 = new Predicate("father", 2);
        body522.args[0] = new Variable(2);
        body522.args[1] = new Variable(1);
        Predicate body523 = new Predicate("isMale", 1);
        body523.args[0] = null;
        Rule rule52 = new BareRule(List.of(head52, body521, body522, body523), new HashSet<>());
        assertEquals("brother(?,X1):-father(X2,?),father(X2,X1),isMale(?)", rule52.toDumpString());
        Rule product_rule5 = CompareQuery.productOnRules(rule51, index1, 3, rule52, index2, 2);
        assertEquals(
                "product(X3,bob,X5,bob,X1):-mother(X3,X5),father(bob,X5),father(X2,?),father(X2,X1),isMale(?)",
                product_rule5.toDumpString()
        );

        /* family(X, bob, Z) :- mother(X, Z), father(bob, Z) */
        /* brother(bob, Y) :- father(Z, bob), father(Z, Y), isMale(bob) */
        Predicate head61 = new Predicate("family", 3);
        head61.args[0] = new Variable(0);
        head61.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "bob");
        head61.args[2] = new Variable(2);
        Predicate body611 = new Predicate("mother", 2);
        body611.args[0] = new Variable(0);
        body611.args[1] = new Variable(2);
        Predicate body612 = new Predicate("father", 2);
        body612.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "bob");
        body612.args[1] = new Variable(2);
        Rule rule61 = new BareRule(List.of(head61, body611, body612), new HashSet<>());
        assertEquals("family(X0,bob,X2):-mother(X0,X2),father(bob,X2)", rule61.toDumpString());
        Predicate head62 = new Predicate("brother", 2);
        head62.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "bob");
        head62.args[1] = new Variable(1);
        Predicate body621 = new Predicate("father", 2);
        body621.args[0] = new Variable(2);
        body621.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "bob");
        Predicate body622 = new Predicate("father", 2);
        body622.args[0] = new Variable(2);
        body622.args[1] = new Variable(1);
        Predicate body623 = new Predicate("isMale", 1);
        body623.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "bob");
        Rule rule62 = new BareRule(List.of(head62, body621, body622, body623), new HashSet<>());
        assertEquals("brother(bob,X1):-father(X2,bob),father(X2,X1),isMale(bob)", rule62.toDumpString());
        Rule product_rule6 = CompareQuery.productOnRules(rule61, index1, 3, rule62, index2, 2);
        assertEquals(
                "product(X3,bob,X5,bob,X1):-mother(X3,X5),father(bob,X5),father(X2,bob),father(X2,X1),isMale(bob)",
                product_rule6.toDumpString()
        );

        /* family(X, bob, Z) :- mother(X, Z), father(bob, Z) */
        /* brother(tom, Y) :- father(Z, tom), father(Z, Y), isMale(tom) */
        Predicate head71 = new Predicate("family", 3);
        head71.args[0] = new Variable(0);
        head71.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "bob");
        head71.args[2] = new Variable(2);
        Predicate body711 = new Predicate("mother", 2);
        body711.args[0] = new Variable(0);
        body711.args[1] = new Variable(2);
        Predicate body712 = new Predicate("father", 2);
        body712.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "bob");
        body712.args[1] = new Variable(2);
        Rule rule71 = new BareRule(List.of(head71, body711, body712), new HashSet<>());
        assertEquals("family(X0,bob,X2):-mother(X0,X2),father(bob,X2)", rule71.toDumpString());
        Predicate head72 = new Predicate("brother", 2);
        head72.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "tom");
        head72.args[1] = new Variable(1);
        Predicate body721 = new Predicate("father", 2);
        body721.args[0] = new Variable(2);
        body721.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "tom");
        Predicate body722 = new Predicate("father", 2);
        body722.args[0] = new Variable(2);
        body722.args[1] = new Variable(1);
        Predicate body723 = new Predicate("isMale", 1);
        body723.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "tom");
        Rule rule72 = new BareRule(List.of(head72, body721, body722, body723), new HashSet<>());
        assertEquals("brother(tom,X1):-father(X2,tom),father(X2,X1),isMale(tom)", rule72.toDumpString());
        assertNull(CompareQuery.productOnRules(rule71, index1, 3, rule72, index2, 2));
    }
}