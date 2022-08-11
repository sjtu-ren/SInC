package sinc2;

import org.apache.commons.cli.Options;
import sinc2.common.InterruptedSignal;
import sinc2.common.Predicate;
import sinc2.kb.*;
import sinc2.rule.*;
import sinc2.util.graph.FeedbackVertexSetSolver;
import sinc2.util.graph.GraphNode;
import sinc2.util.graph.Tarjan;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class Main {
    protected static NumeratedKb kb;
    /** The compressed KB */
    protected static CompressedKb compressedKb;
    public static boolean interrupted = false;
    /**
     * The dependency graph, in the form of an adjacent list.
     * Note: In the implementation, the set of neighbours in the adjacent list refers to the in-neighbours instead of
     * out-neighbours.
     */
    protected static final Map<GraphNode<Predicate>, Set<GraphNode<Predicate>>> dependencyGraph = new HashMap<>();

    static class RealMiner extends RelationMiner {

        static final BareRule bad_rule = new BareRule(0, 0, new HashSet<>(), new HashMap<>());
        static {
            bad_rule.returningEval = Eval.MIN;
        }

        public RealMiner(
                NumeratedKb kb, int targetRelation, EvalMetric evalMetric, int beamwidth, double stopCompressionRatio,
                Map<Predicate, GraphNode<Predicate>> predicate2NodeMap, Map<GraphNode<Predicate>,
                Set<GraphNode<Predicate>>> dependencyGraph, PrintWriter logger
        ) {
            super(kb, targetRelation, evalMetric, beamwidth, stopCompressionRatio, predicate2NodeMap, dependencyGraph, logger);
        }

        @Override
        protected Rule getStartRule() {
            Rule rule =  new BareRule(kb.name2Num("diedIn"), kb.getRelationArity(kb.name2Num("diedIn")), new HashSet<>(), new HashMap<>());
            rule.cvt2Uvs2NewLv(kb.name2Num("diedOnDate"), 2, 0, 0, 0);
            return rule;
        }

        @Override
        protected int checkThenAddRule(UpdateStatus updateStatus, Rule updatedRule, Rule originalRule, Rule[] candidates) throws InterruptedSignal {
            return super.checkThenAddRule(updateStatus, updatedRule, bad_rule, candidates);
        }

        @Override
        protected void selectAsBeam(Rule r) {}
    }

    public static void main(String[] args) throws Exception{
        PrintWriter logger = new PrintWriter(System.out);
        /* load kb */
        try {
            kb = new NumeratedKb("yago1", "../KBRedundancies/2-Open-domainExtraction/data/yago1");
        } catch (KbException | IOException e) {
            e.printStackTrace(logger);
            logger.println("[ERROR] KB Load failed, abort.");
            return;
        }
        compressedKb = new CompressedKb("newYago1", kb);
        // get target relations
        List<Integer> relations = new ArrayList<>();
        for (KbRelation relation: kb.getRelations()) {
            if(Objects.equals(relation.getName(), "diedIn") || Objects.equals(relation.getName(), "diedOnDate"))
                relations.add(relation.getNumeration());
        }
        // compress target relation
        try{
            for (Integer relation_num: relations) {
                RelationMiner relation_miner = new RealMiner(kb, relation_num, EvalMetric.CompressionCapacity, 1,
                        0.5, new HashMap<>(), new HashMap<>(), new PrintWriter(System.out));
                relation_miner.run();
                KbRelation ce_relation = compressedKb.getCounterexampleRelation(relation_num);
                ce_relation.addRecords(relation_miner.getCounterexamples());
                for (Rule r: relation_miner.getHypothesis()) {
                    compressedKb.addHypothesisRule(r);
                }
            }
        } catch (KbException e) {
            e.printStackTrace(logger);
            logger.println("[ERROR] Relation Miner failed. Interrupt");
            interrupted = true;
        }

        /* Dependency analysis */
        try {
            for (KbRelation relation: kb.getRelations()) {
                for (Record record: relation.getRecords()) {
                    GraphNode<Predicate> node = new GraphNode<>(new Predicate(relation.getNumeration(), record.args));
                    if (!dependencyGraph.containsKey(node)) {
                        compressedKb.addRecord(relation.getNumeration(), record);
                    }
                }
            }

            /* Find all SCCs */
            final Tarjan<GraphNode<Predicate>> tarjan = new Tarjan<>(dependencyGraph, false);
            final List<Set<GraphNode<Predicate>>> sccs = tarjan.run();
            for (Set<GraphNode<Predicate>> scc: sccs) {
                /* 找出FVS的一个解，并把之放入start_set */
                /* Find a solution of MFVS and add to "necessaries" */
                final FeedbackVertexSetSolver<GraphNode<Predicate>> fvs_solver =
                        new FeedbackVertexSetSolver<>(dependencyGraph, scc);
                final Set<GraphNode<Predicate>> fvs = fvs_solver.run();
                for (GraphNode<Predicate> node: fvs) {
                    compressedKb.addRecord(node.content.functor, node.content.args);
                }
            }
        } catch (KbException e) {
            e.printStackTrace(logger);
            logger.println("[ERROR] Dependency Analysis failed. Abort.");

            /* Log the hypothesis */
            logger.println("\n### Hypothesis Found ###");
            for (Rule rule : compressedKb.getHypothesis()) {
                logger.println(rule);
            }
            logger.println();
            return;
        }

        /* Log the hypothesis */
        logger.println("\n### Hypothesis Found ###");
        for (Rule rule : compressedKb.getHypothesis()) {
            logger.println(rule);
        }
        logger.println();

        /* Dump the compressed KB */
        try {
            compressedKb.dump("../compressedYago1");
        } catch (IOException e) {
            e.printStackTrace(logger);
            logger.println("[ERROR] Compressed KB dump failed. Abort.");
        }
    }
}
