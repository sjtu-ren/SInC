package sinc.exp;

import sinc.SincRecovery;
import sinc.common.*;
import sinc.impl.cached.MemKB;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class CompareQuery {

    public static final String MINIMIZED_DB_DIR = "./datasets/compressed/";

    protected static final SincRecovery FS_MINIMIZED = loadMinimizedDB(MINIMIZED_DB_DIR + "fs.result");
    protected static final SincRecovery FM_MINIMIZED = loadMinimizedDB(MINIMIZED_DB_DIR + "fm.result");
    protected static final SincRecovery D_MINIMIZED = loadMinimizedDB(MINIMIZED_DB_DIR + "d.result");
    protected static final SincRecovery E_MINIMIZED = loadMinimizedDB(MINIMIZED_DB_DIR + "e.result");
    protected static final SincRecovery DBf_MINIMIZED = loadMinimizedDB(MINIMIZED_DB_DIR + "dbf.result");
    protected static final SincRecovery S_MINIMIZED = loadMinimizedDB(MINIMIZED_DB_DIR + "s.result");
    protected static final SincRecovery U_MINIMIZED = loadMinimizedDB(MINIMIZED_DB_DIR + "u.result");
    protected static final SincRecovery FB_MINIMIZED = loadMinimizedDB(MINIMIZED_DB_DIR + "fb.result");
    protected static final SincRecovery WN_MINIMIZED = loadMinimizedDB(MINIMIZED_DB_DIR + "wn.result");
    protected static final SincRecovery NELL_MINIMIZED = loadMinimizedDB(MINIMIZED_DB_DIR + "nell.result");
    protected static final MemKB FS_DB = loadDB(Dataset.FAMILY_SIMPLE);
    protected static final MemKB FM_DB = loadDB(Dataset.FAMILY_MEDIUM);
    protected static final MemKB D_DB = loadDB(Dataset.DUNUR);
    protected static final MemKB E_DB = loadDB(Dataset.ELTI);
    protected static final MemKB DBf_DB = loadDB(Dataset.DBPEDIA_FACTBOOK);
    protected static final MemKB S_DB = loadDB(Dataset.STUDENT_LOAN);
    protected static final MemKB U_DB = loadDB(Dataset.UMLS);
    protected static final MemKB FB_DB = loadDB(Dataset.FB15K);
    protected static final MemKB WN_DB = loadDB(Dataset.WN18);
    protected static final MemKB NELL_DB = loadDB(Dataset.NELL);

    protected static final Dataset[] DATASETS = new Dataset[]{
            Dataset.FAMILY_SIMPLE, Dataset.FAMILY_MEDIUM, Dataset.DUNUR, Dataset.ELTI, Dataset.DBPEDIA_FACTBOOK,
            Dataset.STUDENT_LOAN, Dataset.UMLS, Dataset.FB15K, Dataset.WN18, Dataset.NELL
    };
//    protected static final Dataset[] DATASETS = new Dataset[]{
//            Dataset.FAMILY_SIMPLE
//    };
    protected static final Map<Dataset, SincRecovery> MINIMIZED_DB_MAP = new HashMap<>();
    protected static final Map<Dataset, MemKB> DB_MAP = new HashMap<>();
    static {
        MINIMIZED_DB_MAP.put(Dataset.FAMILY_SIMPLE, FM_MINIMIZED);
        MINIMIZED_DB_MAP.put(Dataset.FAMILY_MEDIUM, FS_MINIMIZED);
        MINIMIZED_DB_MAP.put(Dataset.DUNUR, D_MINIMIZED);
        MINIMIZED_DB_MAP.put(Dataset.ELTI, E_MINIMIZED);
        MINIMIZED_DB_MAP.put(Dataset.DBPEDIA_FACTBOOK, DBf_MINIMIZED);
        MINIMIZED_DB_MAP.put(Dataset.STUDENT_LOAN, S_MINIMIZED);
        MINIMIZED_DB_MAP.put(Dataset.UMLS, U_MINIMIZED);
        MINIMIZED_DB_MAP.put(Dataset.FB15K, FB_MINIMIZED);
        MINIMIZED_DB_MAP.put(Dataset.WN18, WN_MINIMIZED);
        MINIMIZED_DB_MAP.put(Dataset.NELL, NELL_MINIMIZED);

        DB_MAP.put(Dataset.FAMILY_SIMPLE, FM_DB);
        DB_MAP.put(Dataset.FAMILY_MEDIUM, FS_DB);
        DB_MAP.put(Dataset.DUNUR, D_DB);
        DB_MAP.put(Dataset.ELTI, E_DB);
        DB_MAP.put(Dataset.DBPEDIA_FACTBOOK, DBf_DB);
        DB_MAP.put(Dataset.STUDENT_LOAN, S_DB);
        DB_MAP.put(Dataset.UMLS, U_DB);
        DB_MAP.put(Dataset.FB15K, FB_DB);
        DB_MAP.put(Dataset.WN18, WN_DB);
        DB_MAP.put(Dataset.NELL, NELL_DB);
    }

    protected static final int REPEAT = 100;

    public static void main(String[] args) {
        compareSelection();
        compareProduction();
    }

    protected static SincRecovery loadMinimizedDB(String filePath) {
        try {
            List<Rule> hypothesis = new ArrayList<>();
            Set<Predicate> necessary_facts = new HashSet<>();
            Set<Predicate> counterexamples = new HashSet<>();
            Set<String> delta_constants = new HashSet<>();
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            /* Read Hypothesis */
            while (true) {
                line = reader.readLine();
                if (null == line || 0 == line.length()) {
                    break;
                }
                List<Predicate> rule_structure = CompareDupDetectionByBruteForce.parseRule(line);
                hypothesis.add(new BareRule(rule_structure, new HashSet<>()));
            }

            /* Read Necessary */
            while (true) {
                line = reader.readLine();
                if (null == line || 0 == line.length()) {
                    break;
                }
                final String[] components = line.split("\t");
                final Predicate predicate = new Predicate(components[0], components.length - 1);
                for (int i = 1; i < components.length; i++) {
                    predicate.args[i - 1] = new Constant(Rule.CONSTANT_ARG_ID, components[i]);
                }
                necessary_facts.add(predicate);
            }

            /* Read Counterexamples */
            while (true) {
                line = reader.readLine();
                if (null == line || 0 == line.length()) {
                    break;
                }
                final String[] components = line.split("\t");
                final Predicate predicate = new Predicate(components[0], components.length - 1);
                for (int i = 1; i < components.length; i++) {
                    predicate.args[i - 1] = new Constant(Rule.CONSTANT_ARG_ID, components[i]);
                }
                counterexamples.add(predicate);
            }

            /* Read delta constants */
            while (true) {
                line = reader.readLine();
                if (null == line || 0 == line.length()) {
                    break;
                }
                delta_constants.add(line);

            }
            return new SincRecovery(hypothesis, necessary_facts, counterexamples, delta_constants);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected static MemKB loadDB(Dataset dataset) {
        try {
            MemKB kb = new MemKB();
            BufferedReader reader = new BufferedReader(new FileReader(dataset.getPath()));
            String line;
            while (null != (line = reader.readLine())) {
                final String[] components = line.split("\t");
                final Predicate predicate = new Predicate(components[0], components.length - 1);
                for (int i = 1; i < components.length; i++) {
                    predicate.args[i - 1] = new Constant(Rule.CONSTANT_ARG_ID, components[i]);
                }
                kb.addFact(predicate);
            }
            return kb;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected static void compareSelection() {
        System.out.println(">>> Test Select");
        for (Dataset dataset: DATASETS) {
//            long cost_on_original = 0;
//            long cost_on_minimized = 0;
//            double[] ratios = new double[REPEAT];
            double[] u_sec = new double[REPEAT];
            for (int i = 0; i < REPEAT; i++) {
                /* Select an argument for selection */
                MemKB original_kb = DB_MAP.get(dataset);
                SincRecovery minimized_kb = MINIMIZED_DB_MAP.get(dataset);
                List<String> functors = original_kb.getAllFunctors();
                int rand_int = ThreadLocalRandom.current().nextInt(0, functors.size());
                String functor = functors.get(rand_int);
                int arity = original_kb.getFunctor2ArityMap().get(functor);
                int index = ThreadLocalRandom.current().nextInt(0, arity);
                String value = original_kb.getAllFacts(functor).iterator().next().args[index].name;

                /* Select in the original DB */
                long cost_on_original = selectOnOriginalDb(original_kb, functor, index, value);

                /* Select in the minimized DB */
                long cost_on_minimized = selectOnMinimizedDb(minimized_kb, functor, index, value);

//                ratios[i] = cost_on_original * 100.0 / cost_on_minimized;
                u_sec[i] = cost_on_minimized / 1000.0;
            }
//            System.out.printf("Test on [%s]: %s\n", dataset.getShortName(), Arrays.toString(u_sec));
            System.out.printf("Test on [%s]: %.2f\n", dataset.getShortName(), median(u_sec));
        }
        System.out.println();
    }

    private static double median(double[] array) {
        if (0 == array.length) {
            return 0;
        }
        Arrays.sort(array);
        int idx = array.length / 2;
        if (0 == array.length % 2) {
            return (array[idx] + array[idx-1])/2;
        } else {
            return array[idx];
        }
    }

    protected static void compareProduction() {
        System.out.println(">>> Test Product");
        for (Dataset dataset: DATASETS) {
            /* Select an argument for selection */
            MemKB original_kb = DB_MAP.get(dataset);
            SincRecovery minimized_kb = MINIMIZED_DB_MAP.get(dataset);
            List<String> functors = original_kb.getAllFunctors();
            int rand_int1 = ThreadLocalRandom.current().nextInt(0, functors.size());
            int rand_int2 = ThreadLocalRandom.current().nextInt(0, functors.size());
            String functor1 = functors.get(rand_int1);
            String functor2 = functors.get(rand_int2);
            int arity1 = original_kb.getFunctor2ArityMap().get(functor1);
            int arity2 = original_kb.getFunctor2ArityMap().get(functor2);
            int index1 = ThreadLocalRandom.current().nextInt(0, arity1);
            int index2 = ThreadLocalRandom.current().nextInt(0, arity2);

//            long cost_on_original = 0;
//            long cost_on_minimized = 0;
//            double[] ratios = new double[REPEAT];
            double[] u_sec = new double[REPEAT];
            for (int i = 0; i < REPEAT; i++) {
                /* Select in the original DB */
                long cost_on_original = productOnOriginalDb(original_kb, functor1, index1, arity1, functor2, index2, arity2);

                /* Select in the minimized DB */
                long cost_on_minimized = productOnMinimizedDb(minimized_kb, functor1, index1, arity1, functor2, index2, arity2);

//                ratios[i] = cost_on_original * 100.0 / cost_on_minimized;
                u_sec[i] = cost_on_minimized / 1000.0;
            }
//            System.out.printf("Test on [%s]: %s\n", dataset.getShortName(), Arrays.toString(u_sec));
            System.out.printf("Test on [%s]: %.2f\n", dataset.getShortName(), median(u_sec));
        }
    }

    protected static long selectOnOriginalDb(MemKB DB, String functor, int index, String value) {
        long time_start = System.nanoTime();
        Set<Predicate> result = new HashSet<>();
        for (Predicate predicate: DB.getAllFacts(functor)) {
            if (value.equals(predicate.args[index].name)) {
                result.add(predicate);
            }
        }
        long time_done = System.nanoTime();
        return time_done - time_start;
    }

    protected static long selectOnMinimizedDb(SincRecovery minimizedDB, String functor, int index, String value) {
        long time_start = System.nanoTime();
        Set<Rule> program = new HashSet<>();
        for (Rule rule: minimizedDB.getHypothesis()) {
            Predicate head_pred = rule.getHead();
            if (functor.equals(head_pred.functor)) {
                Rule new_rule = selectOnRule(rule, index, value);
                if (null != new_rule) {
                    program.add(new_rule);
                }
            }
        }
        long time_done = System.nanoTime();
        SincRecovery minimized_db_copied = new SincRecovery(
                new ArrayList<>(program), minimizedDB.getNecessaryFacts(),
                minimizedDB.getCounterExamples(), minimizedDB.getConstants()
        );
        long time_start2 = System.nanoTime();
        Set<Predicate> result = new HashSet<>();
        for (Predicate predicate: minimizedDB.getKb().getAllFacts(functor)) {
            if (value.equals(predicate.args[index].name)) {
                result.add(predicate);
            }
        }
        if (!program.isEmpty()) {
            minimized_db_copied.recover();
        }
        long time_done2 = System.nanoTime();
        return time_done - time_start + (time_done2 - time_start2);
    }

    protected static Rule selectOnRule(Rule rule, int index, String value) {
        Argument argument = rule.getHead().args[index];
        if (null == argument) {
            Rule new_rule = rule.clone();
            new_rule.getHead().args[index] = new Constant(Rule.CONSTANT_ARG_ID, value);
            new_rule.updateStructure();
            return new_rule;
        } else if (argument.isVar) {
            final int vid = argument.id;
            Rule new_rule = rule.clone();
            for (int pred_idx = 0; pred_idx < new_rule.length(); pred_idx++) {
                Predicate predicate = new_rule.getPredicate(pred_idx);
                for (int arg_idx = 0; arg_idx < predicate.args.length; arg_idx++) {
                    argument = predicate.args[arg_idx];
                    if (null != argument && argument.isVar && vid == argument.id) {
                        predicate.args[arg_idx] = new Constant(Rule.CONSTANT_ARG_ID, value);
                    }
                }
            }
            new_rule.updateStructure();
            return new_rule;
        } else {
            if (value.equals(argument.name)) {
                return rule.clone();
            } else {
                return null;
            }
        }
    }

    protected static long productOnOriginalDb(
            MemKB DB, String functor1, int index1, int arity1, String functor2, int index2, int arity2
    ) {
        long time_start = System.nanoTime();
        Set<Predicate> result = new HashSet<>();
        /* Build indices */
        Map<String, Set<Predicate>> table1 = new HashMap<>();
        Map<String, Set<Predicate>> table2 = new HashMap<>();
        for (Predicate predicate: DB.getAllFacts(functor1)) {
            table1.compute(predicate.args[index1].name, (k, v) -> {
                if (null == v) {
                    v = new HashSet<>();
                }
                v.add(predicate);
                return v;
            });
        }
        for (Predicate predicate: DB.getAllFacts(functor2)) {
            table2.compute(predicate.args[index2].name, (k, v) -> {
                if (null == v) {
                    v = new HashSet<>();
                }
                v.add(predicate);
                return v;
            });
        }

        /* Do the product */
        final String new_functor = "product";
        final int new_arity = arity1 + arity2;
        for (Map.Entry<String, Set<Predicate>> entry: table1.entrySet()) {
            Set<Predicate> set2 = table2.get(entry.getKey());
            if (null != set2) {
                for (Predicate predicate1: entry.getValue()) {
                    for (Predicate predicate2: set2) {
                        Predicate new_predicate = new Predicate(new_functor, new_arity);
                        System.arraycopy(predicate1.args, 0, new_predicate.args, 0, arity1);
                        System.arraycopy(predicate2.args, 0, new_predicate.args, arity1, arity2);
                        result.add(new_predicate);
                    }
                }
            }
        }
        long time_done = System.nanoTime();
        return time_done - time_start;
    }

    protected static long productOnMinimizedDb(
            SincRecovery minimizedDB, String functor1, int index1, int arity1, String functor2, int index2, int arity2
    ) {
        long time_start = System.nanoTime();
        Set<Rule> rules1 = new HashSet<>();
        Set<Rule> rules2 = new HashSet<>();
        for (Rule rule: minimizedDB.getHypothesis()) {
            if (functor1.equals(rule.getHead().functor)) {
                rules1.add(rule);
            }
            if (functor2.equals(rule.getHead().functor)) {
                rules2.add(rule);
            }
        }
        Predicate head1 = new Predicate(functor1, arity1);
        for (int i = 0; i < head1.arity(); i++) {
            head1.args[i] = new Variable(i);
        }
        BareRule self_rule1 = new BareRule(List.of(head1, new Predicate(head1)), new HashSet<>());
        Predicate head2 = new Predicate(functor2, arity2);
        for (int i = 0; i < head2.arity(); i++) {
            head2.args[i] = new Variable(i);
        }
        BareRule self_rule2 = new BareRule(List.of(head2, new Predicate(head2)), new HashSet<>());
        rules1.add(self_rule1);
        rules2.add(self_rule2);

        Set<Rule> program = new HashSet<>();
        for (Rule rule1: rules1) {
            for (Rule rule2: rules2) {
                Rule new_rule = productOnRules(rule1, index1, arity1, rule2, index2, arity2);
                if (null != new_rule) {
                    program.add(new_rule);
                }
            }
        }
        long time_done = System.nanoTime();
        SincRecovery minimized_db_copied = new SincRecovery(
                new ArrayList<>(program), minimizedDB.getNecessaryFacts(),
                minimizedDB.getCounterExamples(), minimizedDB.getConstants()
        );
        long time_start2 = System.nanoTime();
        if (!program.isEmpty()) {
            minimized_db_copied.recover();
        }
        long time_done2 = System.nanoTime();
        return time_done - time_start + (time_done2 - time_start2);
    }

    protected static Rule productOnRules(Rule rule1, int index1, int arity1, Rule rule2, int index2, int arity2) {
        int id_offset = rule2.usedBoundedVars();
        List<Predicate> updated_rule1_structure = new ArrayList<>(rule1.length());
        for (int pred_idx = 0; pred_idx < rule1.length(); pred_idx++) {
            Predicate predicate = rule1.getPredicate(pred_idx);
            Predicate new_predicate = new Predicate(predicate.functor, predicate.arity());
            for (int arg_idx = 0; arg_idx < new_predicate.arity(); arg_idx++) {
                Argument argument = predicate.args[arg_idx];
                if (null != argument && argument.isVar) {
                    new_predicate.args[arg_idx] = new Variable(argument.id + id_offset);
                } else {
                    new_predicate.args[arg_idx] = argument;
                }
            }
            updated_rule1_structure.add(new_predicate);
        }

        String new_functor = "product";
        int new_arity = arity1 + arity2;
        Argument argument1 = updated_rule1_structure.get(Rule.HEAD_PRED_IDX).args[index1];
        Argument argument2 = rule2.getHead().args[index2];
        if (null == argument1) {
            Predicate new_head = new Predicate(new_functor, new_arity);
            System.arraycopy(updated_rule1_structure.get(Rule.HEAD_PRED_IDX).args, 0, new_head.args, 0, arity1);
            System.arraycopy(rule2.getHead().args, 0, new_head.args, arity1, arity2);
            if (null == argument2) {
                Variable new_var = new Variable(rule1.usedBoundedVars() + rule2.usedBoundedVars());
                new_head.args[index1] = new_var;
                new_head.args[arity1 + index2] = new_var;
            } else {
                new_head.args[index1] = argument2;
            }
            List<Predicate> new_rule_structure = new ArrayList<>();
            new_rule_structure.add(new_head);
            for (int pred_idx = 1; pred_idx < updated_rule1_structure.size(); pred_idx++) {
                new_rule_structure.add(updated_rule1_structure.get(pred_idx));
            }
            for (int pred_idx = 1; pred_idx < rule2.length(); pred_idx++) {
                new_rule_structure.add(new Predicate(rule2.getPredicate(pred_idx)));
            }
            return new BareRule(new_rule_structure, new HashSet<>());
        } else if (argument1.isVar) {
            Predicate new_head = new Predicate(new_functor, new_arity);
            if (null == argument2) {
                System.arraycopy(updated_rule1_structure.get(Rule.HEAD_PRED_IDX).args, 0, new_head.args, 0, arity1);
                System.arraycopy(rule2.getHead().args, 0, new_head.args, arity1, arity2);
                new_head.args[arity1 + index2] = argument1;
            } else {
                for (Predicate predicate: updated_rule1_structure) {
                    for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                        Argument argument = predicate.args[arg_idx];
                        if (null != argument && argument.isVar && argument1.id == argument.id) {
                            predicate.args[arg_idx] = argument2;
                        }
                    }
                }
                System.arraycopy(updated_rule1_structure.get(Rule.HEAD_PRED_IDX).args, 0, new_head.args, 0, arity1);
                System.arraycopy(rule2.getHead().args, 0, new_head.args, arity1, arity2);
            }
            List<Predicate> new_rule_structure = new ArrayList<>();
            new_rule_structure.add(new_head);
            for (int pred_idx = 1; pred_idx < updated_rule1_structure.size(); pred_idx++) {
                new_rule_structure.add(updated_rule1_structure.get(pred_idx));
            }
            for (int pred_idx = 1; pred_idx < rule2.length(); pred_idx++) {
                new_rule_structure.add(new Predicate(rule2.getPredicate(pred_idx)));
            }
            return new BareRule(new_rule_structure, new HashSet<>());
        } else {
            if (null == argument2) {
                Predicate new_head = new Predicate(new_functor, new_arity);
                System.arraycopy(updated_rule1_structure.get(Rule.HEAD_PRED_IDX).args, 0, new_head.args, 0, arity1);
                System.arraycopy(rule2.getHead().args, 0, new_head.args, arity1, arity2);
                new_head.args[arity1 + index2] = argument1;
                List<Predicate> new_rule_structure = new ArrayList<>();
                new_rule_structure.add(new_head);
                for (int pred_idx = 1; pred_idx < updated_rule1_structure.size(); pred_idx++) {
                    new_rule_structure.add(updated_rule1_structure.get(pred_idx));
                }
                for (int pred_idx = 1; pred_idx < rule2.length(); pred_idx++) {
                    new_rule_structure.add(new Predicate(rule2.getPredicate(pred_idx)));
                }
                return new BareRule(new_rule_structure, new HashSet<>());
            } else if (argument2.isVar) {
                Predicate new_head = new Predicate(new_functor, new_arity);
                System.arraycopy(updated_rule1_structure.get(Rule.HEAD_PRED_IDX).args, 0, new_head.args, 0, arity1);
                Predicate head2 = rule2.getHead();
                for (int arg_idx = 0; arg_idx < arity2; arg_idx++) {
                    Argument argument = head2.args[arg_idx];
                    new_head.args[arity1 + arg_idx] = (null != argument && argument.isVar && argument2.id == argument.id) ?
                            argument1 : argument;
                }
                List<Predicate> new_rule_structure = new ArrayList<>();
                new_rule_structure.add(new_head);
                for (int pred_idx = 1; pred_idx < updated_rule1_structure.size(); pred_idx++) {
                    new_rule_structure.add(updated_rule1_structure.get(pred_idx));
                }
                for (int pred_idx = 1; pred_idx < rule2.length(); pred_idx++) {
                    Predicate predicate = rule2.getPredicate(pred_idx);
                    Predicate new_predicate = new Predicate(predicate.functor, predicate.arity());
                    for (int arg_idx = 0; arg_idx < new_predicate.arity(); arg_idx++) {
                        Argument argument = predicate.args[arg_idx];
                        new_predicate.args[arg_idx] = (null != argument && argument.isVar && argument2.id == argument.id) ?
                                argument1 : argument;
                    }
                    new_rule_structure.add(new_predicate);
                }
                return new BareRule(new_rule_structure, new HashSet<>());
            } else {
                if (argument1.name.equals(argument2.name)) {
                    Predicate new_head = new Predicate(new_functor, new_arity);
                    System.arraycopy(updated_rule1_structure.get(Rule.HEAD_PRED_IDX).args, 0, new_head.args, 0, arity1);
                    System.arraycopy(rule2.getHead().args, 0, new_head.args, arity1, arity2);
                    List<Predicate> new_rule_structure = new ArrayList<>();
                    new_rule_structure.add(new_head);
                    for (int pred_idx = 1; pred_idx < updated_rule1_structure.size(); pred_idx++) {
                        new_rule_structure.add(updated_rule1_structure.get(pred_idx));
                    }
                    for (int pred_idx = 1; pred_idx < rule2.length(); pred_idx++) {
                        new_rule_structure.add(new Predicate(rule2.getPredicate(pred_idx)));
                    }
                    return new BareRule(new_rule_structure, new HashSet<>());
                } else {
                    return null;
                }
            }
        }
    }
}
