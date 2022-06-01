package sinc2.rule;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sinc2.common.Argument;
import sinc2.common.Predicate;
import sinc2.kb.NumerationMap;
import sinc2.util.MultiSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RuleTest {

    static final String FUNCTOR_H = "h";
    static final String FUNCTOR_P = "p";
    static final String FUNCTOR_Q = "q";
    static final String FUNCTOR_FATHER = "father";
    static final String FUNCTOR_PARENT = "parent";
    static final String FUNCTOR_GRANDPARENT = "grandparent";
    static final String CONST_C = "c";
    static final String CONST_D = "d";

    static final int NUM_H = 1;
    static final int NUM_P = 2;
    static final int NUM_Q = 3;
    static final int NUM_FATHER = 4;
    static final int NUM_PARENT = 5;
    static final int NUM_GRANDPARENT = 6;
    static final int NUM_C = 7;
    static final int NUM_D = 8;

    static final NumerationMap map = new NumerationMap();

    @BeforeAll
    static void createNumerationMap() {
        assertEquals(NUM_H, map.mapName(FUNCTOR_H));
        assertEquals(NUM_P, map.mapName(FUNCTOR_P));
        assertEquals(NUM_Q, map.mapName(FUNCTOR_Q));
        assertEquals(NUM_FATHER, map.mapName(FUNCTOR_FATHER));
        assertEquals(NUM_PARENT, map.mapName(FUNCTOR_PARENT));
        assertEquals(NUM_GRANDPARENT, map.mapName(FUNCTOR_GRANDPARENT));
        assertEquals(NUM_C, map.mapName(CONST_C));
        assertEquals(NUM_D, map.mapName(CONST_D));
    }
    
    @Test
    void testConstruction() {
        /* h(X, Y, c) <- p(X), q(?, Y), q(c, X) */
        final Set<Fingerprint> cache = new HashSet<>();
        final Map<MultiSet<Integer>, Set<Fingerprint>> tabu_set = new HashMap<>();
        Rule r = new BareRule(NUM_H, 3, cache, tabu_set);
        assertTrue(r.toString(map).contains("h(?,?,?):-"));
        assertEquals("h(?,?,?):-", r.toDumpString(map));
        assertEquals(0, r.length());
        assertEquals(1, r.predicates());
        assertEquals(0, r.usedLimitedVars());
        assertEquals(1, cache.size());

        assertEquals(UpdateStatus.NORMAL, r.cvt2Uvs2NewLv(NUM_P, 1, 0, 0, 0));
        assertTrue(r.toString(map).contains("h(X0,?,?):-p(X0)"));
        assertEquals("h(X0,?,?):-p(X0)", r.toDumpString(map));
        assertEquals(1, r.length());
        assertEquals(2, r.predicates());
        assertEquals(1, r.usedLimitedVars());
        assertEquals(2, cache.size());

        assertEquals(UpdateStatus.NORMAL, r.cvt2Uvs2NewLv(NUM_Q, 2, 1, 0, 1));
        assertTrue(r.toString(map).contains("h(X0,X1,?):-p(X0),q(?,X1)"));
        assertEquals("h(X0,X1,?):-p(X0),q(?,X1)", r.toDumpString(map));
        assertEquals(2, r.length());
        assertEquals(3, r.predicates());
        assertEquals(2, r.usedLimitedVars());
        assertEquals(3, cache.size());

        assertEquals(UpdateStatus.NORMAL, r.cvt1Uv2ExtLv(NUM_Q, 2, 1, 0));
        assertTrue(r.toString(map).contains("h(X0,X1,?):-p(X0),q(?,X1),q(?,X0)"));
        assertEquals("h(X0,X1,?):-p(X0),q(?,X1),q(?,X0)", r.toDumpString(map));
        assertEquals(3, r.length());
        assertEquals(4, r.predicates());
        assertEquals(2, r.usedLimitedVars());
        assertEquals(4, cache.size());

        assertEquals(UpdateStatus.NORMAL, r.cvt1Uv2Const(3, 0, NUM_C));
        assertTrue(r.toString(map).contains("h(X0,X1,?):-p(X0),q(?,X1),q(c,X0)"));
        assertEquals("h(X0,X1,?):-p(X0),q(?,X1),q(c,X0)", r.toDumpString(map));
        assertEquals(4, r.length());
        assertEquals(4, r.predicates());
        assertEquals(2, r.usedLimitedVars());
        assertEquals(5, cache.size());

        assertEquals(UpdateStatus.NORMAL, r.cvt1Uv2Const(0, 2, NUM_C));
        assertTrue(r.toString(map).contains("h(X0,X1,c):-p(X0),q(?,X1),q(c,X0)"));
        assertEquals("h(X0,X1,c):-p(X0),q(?,X1),q(c,X0)", r.toDumpString(map));
        assertEquals(5, r.length());
        assertEquals(4, r.predicates());
        assertEquals(2, r.usedLimitedVars());
        assertEquals(6, cache.size());

        Predicate predicate_head = new Predicate(NUM_H, 3);
        predicate_head.args[0] = Argument.variable(0);
        predicate_head.args[1] = Argument.variable(1);
        predicate_head.args[2] = Argument.constant(NUM_C);
        Predicate predicate_body1 = new Predicate(NUM_P, 1);
        predicate_body1.args[0] = Argument.variable(0);
        Predicate predicate_body2 = new Predicate(NUM_Q, 2);
        predicate_body2.args[1] = Argument.variable(1);
        Predicate predicate_body3 = new Predicate(NUM_Q, 2);
        predicate_body3.args[0] = Argument.constant(NUM_C);
        predicate_body3.args[1] = Argument.variable(0);

        assertEquals(predicate_head, r.getPredicate(0));
        assertEquals(predicate_head, r.getHead());
        assertEquals(predicate_body1, r.getPredicate(1));
        assertEquals(predicate_body2, r.getPredicate(2));
        assertEquals(predicate_body3, r.getPredicate(3));
    }

    @Test
    void testConstructionAndRemoval1() {
        /* h(X, Y, c) <- p(X), q(?, Y), q(c, X) */
        final Set<Fingerprint> cache = new HashSet<>();
        final Map<MultiSet<Integer>, Set<Fingerprint>> tabu_set = new HashMap<>();
        Rule r = new BareRule(NUM_H, 3, cache, tabu_set);
        assertEquals(UpdateStatus.NORMAL, r.cvt2Uvs2NewLv(NUM_P, 1, 0, 0, 0));
        assertEquals(UpdateStatus.NORMAL, r.cvt2Uvs2NewLv(NUM_Q, 2, 1, 0, 1));
        assertEquals(UpdateStatus.NORMAL, r.cvt1Uv2ExtLv(NUM_Q, 2, 1, 0));
        assertEquals(UpdateStatus.NORMAL, r.cvt1Uv2Const(3, 0, NUM_C));
        assertEquals(UpdateStatus.NORMAL, r.cvt1Uv2Const(0, 2, NUM_C));
        assertTrue(r.toString(map).contains("h(X0,X1,c):-p(X0),q(?,X1),q(c,X0)"));
        assertEquals("h(X0,X1,c):-p(X0),q(?,X1),q(c,X0)", r.toDumpString(map));
        assertEquals(5, r.length());
        assertEquals(4, r.predicates());
        assertEquals(2, r.usedLimitedVars());
        assertEquals(6, cache.size());

        cache.clear();
        assertEquals(UpdateStatus.NORMAL, r.rmAssignedArg(1, 0));
        assertTrue(r.toString(map).contains("h(X0,X1,c):-q(?,X1),q(c,X0)"));
        assertEquals("h(X0,X1,c):-q(?,X1),q(c,X0)", r.toDumpString(map));
        assertEquals(4, r.length());
        assertEquals(3, r.predicates());
        assertEquals(2, r.usedLimitedVars());
        assertEquals(1, cache.size());

        assertEquals(UpdateStatus.NORMAL, r.rmAssignedArg(2, 0));
        assertTrue(r.toString(map).contains("h(X0,X1,c):-q(?,X1),q(?,X0)"));
        assertEquals("h(X0,X1,c):-q(?,X1),q(?,X0)", r.toDumpString(map));
        assertEquals(3, r.length());
        assertEquals(3, r.predicates());
        assertEquals(2, r.usedLimitedVars());
        assertEquals(2, cache.size());

        assertEquals(UpdateStatus.NORMAL, r.rmAssignedArg(2, 1));
        assertTrue(r.toString(map).contains("h(?,X0,c):-q(?,X0)"));
        assertEquals("h(?,X0,c):-q(?,X0)", r.toDumpString(map));
        assertEquals(2, r.length());
        assertEquals(2, r.predicates());
        assertEquals(1, r.usedLimitedVars());
        assertEquals(3, cache.size());

        assertEquals(UpdateStatus.NORMAL, r.rmAssignedArg(0, 2));
        assertTrue(r.toString(map).contains("h(?,X0,?):-q(?,X0)"));
        assertEquals("h(?,X0,?):-q(?,X0)", r.toDumpString(map));
        assertEquals(1, r.length());
        assertEquals(2, r.predicates());
        assertEquals(1, r.usedLimitedVars());
        assertEquals(4, cache.size());

        assertEquals(UpdateStatus.NORMAL, r.rmAssignedArg(0, 1));
        assertTrue(r.toString(map).contains("h(?,?,?):-"));
        assertEquals("h(?,?,?):-", r.toDumpString(map));
        assertEquals(0, r.length());
        assertEquals(1, r.predicates());
        assertEquals(0, r.usedLimitedVars());
        assertEquals(5, cache.size());
        assertEquals(new Predicate(NUM_H, 3), r.getHead());
    }

    @Test
    void testConstructionAndRemoval2() {
        /* h(X, Y, Z) <- p(X), q(Z, Y), q(Z, X) */
        final Set<Fingerprint> cache = new HashSet<>();
        final Map<MultiSet<Integer>, Set<Fingerprint>> tabu_set = new HashMap<>();
        Rule r = new BareRule(NUM_H, 3, cache, tabu_set);
        assertEquals(UpdateStatus.NORMAL, r.cvt2Uvs2NewLv(NUM_P, 1, 0, 0, 0));
        assertEquals(UpdateStatus.NORMAL, r.cvt2Uvs2NewLv(NUM_Q, 2, 1, 0, 1));
        assertEquals(UpdateStatus.NORMAL, r.cvt1Uv2ExtLv(NUM_Q, 2, 1, 0));
        assertEquals(UpdateStatus.NORMAL, r.cvt2Uvs2NewLv(2, 0, 0, 2));
        assertEquals(UpdateStatus.NORMAL, r.cvt1Uv2ExtLv(3, 0, 2));
        assertTrue(r.toString(map).contains("h(X0,X1,X2):-p(X0),q(X2,X1),q(X2,X0)"));
        assertEquals("h(X0,X1,X2):-p(X0),q(X2,X1),q(X2,X0)", r.toDumpString(map));
        assertEquals(5, r.length());
        assertEquals(4, r.predicates());
        assertEquals(3, r.usedLimitedVars());
        assertEquals(6, cache.size());

        cache.clear();
        assertEquals(UpdateStatus.NORMAL, r.rmAssignedArg(0, 0));
        assertTrue(r.toString(map).contains("h(?,X1,X2):-p(X0),q(X2,X1),q(X2,X0)"));
        assertEquals("h(?,X1,X2):-p(X0),q(X2,X1),q(X2,X0)", r.toDumpString(map));
        assertEquals(4, r.length());
        assertEquals(4, r.predicates());
        assertEquals(3, r.usedLimitedVars());
        assertEquals(1, cache.size());

        assertEquals(UpdateStatus.NORMAL, r.rmAssignedArg(3, 1));
        assertTrue(r.toString(map).contains("h(?,X1,X0):-q(X0,X1),q(X0,?)"));
        assertEquals("h(?,X1,X0):-q(X0,X1),q(X0,?)", r.toDumpString(map));
        assertEquals(3, r.length());
        assertEquals(3, r.predicates());
        assertEquals(2, r.usedLimitedVars());
        assertEquals(2, cache.size());

        assertEquals(UpdateStatus.NORMAL, r.rmAssignedArg(1, 1));
        assertTrue(r.toString(map).contains("h(?,?,X0):-q(X0,?),q(X0,?)"));
        assertEquals("h(?,?,X0):-q(X0,?),q(X0,?)", r.toDumpString(map));
        assertEquals(2, r.length());
        assertEquals(3, r.predicates());
        assertEquals(1, r.usedLimitedVars());
        assertEquals(3, cache.size());

        assertNotEquals(UpdateStatus.NORMAL, r.rmAssignedArg(0, 2));
    }

    @Test
    void testCopyConstructor() {
        final Set<Fingerprint> cache = new HashSet<>();
        final Map<MultiSet<Integer>, Set<Fingerprint>> tabu_set = new HashMap<>();
        Rule r1 = new BareRule(NUM_H, 3, cache, tabu_set);
        assertEquals(UpdateStatus.NORMAL, r1.cvt2Uvs2NewLv(NUM_P, 1, 0, 0, 0));
        assertEquals(UpdateStatus.NORMAL, r1.cvt2Uvs2NewLv(NUM_Q, 2, 1, 0, 1));
        assertEquals(UpdateStatus.NORMAL, r1.cvt1Uv2ExtLv(NUM_Q, 2, 1, 0));
        assertTrue(r1.toString(map).contains("h(X0,X1,?):-p(X0),q(?,X1),q(?,X0)"));
        assertEquals("h(X0,X1,?):-p(X0),q(?,X1),q(?,X0)", r1.toDumpString(map));
        assertEquals(3, r1.length());
        assertEquals(4, r1.predicates());
        assertEquals(2, r1.usedLimitedVars());
        assertEquals(4, cache.size());

        Rule r2 = r1.clone();
        assertEquals(UpdateStatus.NORMAL, r2.rmAssignedArg(1, 0));
        assertEquals(UpdateStatus.NORMAL, r2.rmAssignedArg(0, 0));
        assertTrue(r2.toString(map).contains("h(?,X0,?):-q(?,X0)"));
        assertEquals("h(?,X0,?):-q(?,X0)", r2.toDumpString(map));
        assertEquals(1, r2.length());
        assertEquals(2, r2.predicates());
        assertEquals(1, r2.usedLimitedVars());
        assertEquals(6, cache.size());

        assertTrue(r1.toString(map).contains("h(X0,X1,?):-p(X0),q(?,X1),q(?,X0)"));
        assertEquals("h(X0,X1,?):-p(X0),q(?,X1),q(?,X0)", r1.toDumpString(map));
        assertEquals(3, r1.length());
        assertEquals(4, r1.predicates());
        assertEquals(2, r1.usedLimitedVars());
        assertEquals(6, cache.size());
    }

    @Test
    void testStructureConstructor1() {
        /* h(?,?,?) :- */
        BareRule expected_rule = new BareRule(NUM_H, 3, new HashSet<>(), new HashMap<>());
        assertEquals("h(?,?,?):-", expected_rule.toDumpString(map));

        BareRule actual_rule = new BareRule(expected_rule.structure, new HashSet<>(), new HashMap<>());
        structureIdentityCheck(expected_rule, actual_rule);
    }

    @Test
    void testStructureConstructor2() {
        /* h(X,X,?) :- p(X,X) */
        BareRule expected_rule = new BareRule(NUM_H, 3, new HashSet<>(), new HashMap<>());
        assertEquals(UpdateStatus.NORMAL, expected_rule.cvt2Uvs2NewLv(0, 0, 0, 1));
        assertEquals(UpdateStatus.NORMAL, expected_rule.cvt1Uv2ExtLv(NUM_P, 2, 0, 0));
        assertEquals(UpdateStatus.NORMAL, expected_rule.cvt1Uv2ExtLv(1, 1, 0));
        assertEquals("h(X0,X0,?):-p(X0,X0)", expected_rule.toDumpString(map));

        BareRule actual_rule = new BareRule(expected_rule.structure, new HashSet<>(), new HashMap<>());
        structureIdentityCheck(expected_rule, actual_rule);
    }

    @Test
    void testStructureConstructor3() {
        /* h(?,?,X) :- p(X), q(X,Y), q(Y,?) */
        BareRule expected_rule = new BareRule(NUM_H, 3, new HashSet<>(), new HashMap<>());
        assertEquals(UpdateStatus.NORMAL, expected_rule.cvt2Uvs2NewLv(NUM_P, 1,0, 0, 2));
        assertEquals(UpdateStatus.NORMAL, expected_rule.cvt1Uv2ExtLv(NUM_Q, 2, 0, 0));
        assertEquals(UpdateStatus.NORMAL, expected_rule.cvt2Uvs2NewLv(NUM_Q,2, 0, 2, 1));
        assertEquals("h(?,?,X0):-p(X0),q(X0,X1),q(X1,?)", expected_rule.toDumpString(map));

        BareRule actual_rule = new BareRule(expected_rule.structure, new HashSet<>(), new HashMap<>());
        structureIdentityCheck(expected_rule, actual_rule);
    }

    @Test
    void testStructureConstructor4() {
        /* h(?,d,X) :- p(X), q(X,Y), q(Y,c) */
        BareRule expected_rule = new BareRule(NUM_H, 3, new HashSet<>(), new HashMap<>());
        assertEquals(UpdateStatus.NORMAL, expected_rule.cvt2Uvs2NewLv(NUM_P, 1,0, 0, 2));
        assertEquals(UpdateStatus.NORMAL, expected_rule.cvt1Uv2ExtLv(NUM_Q, 2, 0, 0));
        assertEquals(UpdateStatus.NORMAL, expected_rule.cvt2Uvs2NewLv(NUM_Q,2, 0, 2, 1));
        assertEquals(UpdateStatus.NORMAL, expected_rule.cvt1Uv2Const(0,1,NUM_D));
        assertEquals(UpdateStatus.NORMAL, expected_rule.cvt1Uv2Const(3,1,NUM_C));
        assertEquals("h(?,d,X0):-p(X0),q(X0,X1),q(X1,c)", expected_rule.toDumpString(map));

        BareRule actual_rule = new BareRule(expected_rule.structure, new HashSet<>(), new HashMap<>());
        structureIdentityCheck(expected_rule, actual_rule);
    }

    void structureIdentityCheck(BareRule expected, BareRule actual) {
        assertEquals(expected.structure, actual.structure);
        assertEquals(expected.limitedVarCnts, actual.limitedVarCnts);
        assertEquals(expected.fingerprint, actual.fingerprint);
        assertEquals(expected.length, actual.length);
    }
}