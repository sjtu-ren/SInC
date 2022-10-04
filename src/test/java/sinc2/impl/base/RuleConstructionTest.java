package sinc2.impl.base;

import org.junit.jupiter.api.Test;
import sinc2.kb.KbException;
import sinc2.kb.NumeratedKb;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

public class RuleConstructionTest {
    @Test
    void test1() throws KbException, IOException {
        NumeratedKb kb = new NumeratedKb("wn2021", "datasets/numerated");
        int hypernyms = kb.name2Num("hypernyms");
        int hyponyms = kb.name2Num("hyponyms");
        CachedRule rule = new CachedRule(hypernyms, kb.getRelationArity(hypernyms), new HashSet<>(), new HashMap<>(), kb);
        assertEquals("hypernyms(?,?):-", rule.toDumpString(kb.getNumerationMap()));
        rule.updateCacheIndices();
        rule.cvt2Uvs2NewLv(hyponyms, kb.getRelationArity(hyponyms), 0, 0, 0);
        assertEquals("hypernyms(X0,?):-hyponyms(X0,?)", rule.toDumpString(kb.getNumerationMap()));
        rule.updateCacheIndices();
        rule.cvt2Uvs2NewLv(hyponyms, kb.getRelationArity(hyponyms), 1, 0, 1);
        assertEquals("hypernyms(X0,X1):-hyponyms(X0,?),hyponyms(?,X1)", rule.toDumpString(kb.getNumerationMap()));
    }

    @Test
    void test2() throws KbException, IOException {
        NumeratedKb kb = new NumeratedKb("wn2021", "datasets/numerated");
        int hypernyms = kb.name2Num("hypernyms");
        int hyponyms = kb.name2Num("hyponyms");
        CachedRule rule = new CachedRule(hypernyms, kb.getRelationArity(hypernyms), new HashSet<>(), new HashMap<>(), kb);
        assertEquals("hypernyms(?,?):-", rule.toDumpString(kb.getNumerationMap()));
        rule.updateCacheIndices();
        rule.cvt2Uvs2NewLv(hyponyms, kb.getRelationArity(hyponyms), 0, 0, 0);
        assertEquals("hypernyms(X0,?):-hyponyms(X0,?)", rule.toDumpString(kb.getNumerationMap()));
        rule.updateCacheIndices();
        rule.cvt2Uvs2NewLv(hyponyms, kb.getRelationArity(hyponyms), 0, 1, 1);
        assertEquals("hypernyms(X0,?):-hyponyms(X0,X1),hyponyms(X1,?)", rule.toDumpString(kb.getNumerationMap()));
        rule.updateCacheIndices();
        rule.cvt2Uvs2NewLv(0, 1, 2, 1);
        assertEquals("hypernyms(X0,X2):-hyponyms(X0,X1),hyponyms(X1,X2)", rule.toDumpString(kb.getNumerationMap()));
    }
}
