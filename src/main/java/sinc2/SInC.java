package sinc2;

import sinc2.common.Predicate;
import sinc2.common.SincException;
import sinc2.kb.*;
import sinc2.rule.Rule;
import sinc2.util.graph.FeedbackVertexSetSolver;
import sinc2.util.graph.GraphNode;
import sinc2.util.graph.Tarjan;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

/**
 * The abstract class for SInC. The overall compression procedure is implemented here.
 *
 * @since 1.0
 */
public abstract class SInC {

    /** The log file name in the compressed KB */
    public static final String LOG_FILE_NAME = "log.meta";
    /** The output content (stdout/stderr) file name */
    public static final String STD_OUTPUT_FILE_NAME = "std.meta";
    /** The command string to interrupt the compression workflow */
    public static final String INTERRUPT_CMD = "stop";

    /**
     * A flag denoting the interruption of the workflow. It is shared read-only by all worker threads and shared
     * write-only to the daemon thread.
     */
    public static boolean interrupted = false;

    /** The axiom node refers to the "⊥" node in the dependency graph */
    protected static final GraphNode<Predicate> AXIOM_NODE = new GraphNode<>(new Predicate(0, 0));

    /* Runtime configurations */
    /** SInC configuration */
    protected final SincConfig config;
    /** The logger */
    protected final PrintWriter logger;

    /* Compression related data */
    /** The input KB */
    protected NumeratedKb kb;
    /** The compressed KB */
    protected CompressedKb compressedKb;
    /** A mapping from predicates to the nodes in the dependency graph */
    protected final Map<Predicate, GraphNode<Predicate>> predicate2NodeMap = new HashMap<>();
    /**
     * The dependency graph, in the form of an adjacent list.
     * Note: In the implementation, the set of neighbours in the adjacent list refers to the in-neighbours instead of
     * out-neighbours.
     */
    protected final Map<GraphNode<Predicate>, Set<GraphNode<Predicate>>> dependencyGraph = new HashMap<>();
    /** The performance monitor */
    protected final PerformanceMonitor monitor = new PerformanceMonitor();

    /**
     * Create a SInC object with configurations.
     *
     * @param config The configurations
     * @throws SincException Dump path creation failure
     */
    public SInC(SincConfig config) throws SincException {
        this.config = config;

        /* Create writer objects to log and std output files */
        File dump_kb_dir = Paths.get(config.dumpPath, config.dumpName).toFile();
        if (!dump_kb_dir.exists() && !dump_kb_dir.mkdirs()) {
            throw new SincException("Dump path creation failed.");
        }
        PrintWriter writer;
        try {
            writer = new PrintWriter(Paths.get(config.dumpPath, config.dumpName, LOG_FILE_NAME).toFile());
        } catch (IOException e) {
            writer = new PrintWriter(System.out);
        }
        this.logger = writer;
        PrintStream stream;
        try {
            stream = new PrintStream(Paths.get(config.dumpPath, config.dumpName, STD_OUTPUT_FILE_NAME).toFile());
        } catch (IOException e) {
            stream = null;
        }
        if (null != stream) {
            System.setOut(stream);
            System.setErr(stream);
        }

        Rule.MIN_FACT_COVERAGE = config.minFactCoverage;
        KbRelation.MIN_CONSTANT_COVERAGE = config.minConstantCoverage;
    }

    /**
     * Load a KB (in the format of Numerated KB) and return the KB
     */
    protected NumeratedKb loadKb() throws KbException, IOException {
        NumeratedKb kb =  new NumeratedKb(config.kbName, config.basePath);
        kb.updatePromisingConstants();
        return kb;
    }

    /**
     * The relations that will be the targets of rule mining procedures. By default, all relations are the targets.
     * This function can be overridden to customize the target list.
     */
    protected List<Integer> getTargetRelations() {
        List<Integer> relations = new ArrayList<>();
        for (KbRelation relation: kb.getRelations()) {
            relations.add(relation.getNumeration());
        }
        return relations;
    }

    /**
     * Determine the necessary set.
     */
    protected void dependencyAnalysis() throws KbException {
        /* The KB has already been updated by the relation miners. Here we only need to find the nodes with no in-degree
         * and those in the MFVS solution */
        /* Find all nodes that are not entailed */
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
            monitor.sccVertices += scc.size();
            monitor.fvsVertices += fvs.size();
        }
        monitor.sccNumber = sccs.size();
    }

    /**
     * Recover from the compressed KB to verify the correctness of the compression.
     *
     * @return Whether the compressed KB can be recovered to the original one.
     */
    public boolean recover() {
        /* Todo: Implement Here */
        return false;
    }

    /**
     * Dump the compressed KB
     */
    protected void dumpCompressedKb() throws IOException {
        compressedKb.dump(config.dumpPath);
    }

    protected void showMonitor() {
        monitor.show(logger);
    }

    public CompressedKb getCompressedKb() {
        return compressedKb;
    }

    protected abstract RelationMiner createRelationMiner(int targetRelationNum);

    protected void showConfig() {
        logger.printf("Base Path:\t%s\n", config.basePath);
        logger.printf("KB Name:\t%s\n", config.kbName);
        logger.printf("Dump Path:\t%s\n", config.dumpPath);
        logger.printf("Dump Name:\t%s\n", config.dumpName);
        logger.printf("Beamwidth:\t%s\n", config.beamwidth);
        logger.printf("Threads:\t%s\n", config.threads);
        logger.printf("Eval Metric:\t%s\n", config.evalMetric);
        logger.printf("Min Fact Coverage:\t%s\n", config.minFactCoverage);
        logger.printf("Min Constant Coverage:\t%s\n", config.minConstantCoverage);
        logger.printf("Stop Compression Ratio:\t%s\n", config.stopCompressionRatio);
        logger.printf("Validation:\t%s\n", config.validation);
        logger.println();
    }

    /**
     * The compress procedure.
     */
    private void compress() {
        showConfig();

        /* Load KB */
        long time_start = System.currentTimeMillis();
        try {
            kb = loadKb();
        } catch (KbException | IOException e) {
            e.printStackTrace(logger);
            logError("KB load failed, abort.");
            return;
        }
        monitor.kbSize = kb.totalRecords();
        monitor.kbFunctors = kb.totalRelations();
        monitor.kbConstants = kb.getAllConstants().size();
        compressedKb = new CompressedKb(config.dumpName, kb);
        long time_kb_loaded = System.currentTimeMillis();
        monitor.kbLoadTime = time_kb_loaded - time_start;

        /* Run relation miners on each relation */
        try {
            final List<Integer> target_relations = getTargetRelations();
            for (int i = 0; i < target_relations.size(); i++) {
                Integer relation_num = target_relations.get(i);
                RelationMiner relation_miner = createRelationMiner(relation_num);
                relation_miner.run();
                KbRelation ce_relation = compressedKb.getCounterexampleRelation(relation_num);
                ce_relation.addRecords(relation_miner.getCounterexamples());
                for (Rule r: relation_miner.getHypothesis()) {
                    compressedKb.addHypothesisRule(r);
                    monitor.hypothesisSize += r.length();
                }
                logInfo(String.format("Relation mining done (%d/%d): %s", i+1, target_relations.size(), kb.num2Name(relation_num)));
                monitor.hypothesisRuleNumber += relation_miner.getHypothesis().size();
            }
        } catch (KbException e) {
            e.printStackTrace(logger);
            logError("Relation Miner failed. Interrupt");
            interrupted = true;
        }
        long time_hypothesis_found = System.currentTimeMillis();
        monitor.hypothesisMiningTime = time_hypothesis_found - time_kb_loaded;

        /* Dependency analysis */
        try {
            dependencyAnalysis();
        } catch (KbException e) {
            e.printStackTrace(logger);
            logError("Dependency Analysis failed. Abort.");

            /* Log the hypothesis */
            logInfo("\n### Hypothesis Found ###");
            for (Rule rule : compressedKb.getHypothesis()) {
                logInfo(rule.toString(kb.getNumerationMap()));
            }
            logger.println();

            monitor.necessaryFacts = compressedKb.totalNecessaryRecords();
            monitor.counterexamples = compressedKb.totalCounterexamples();
            monitor.supplementaryConstants = compressedKb.totalSupplementaryConstants();
            long time_dependency_resolved = System.currentTimeMillis();
            monitor.dependencyAnalysisTime = time_dependency_resolved - time_hypothesis_found;
            monitor.totalTime = time_dependency_resolved - time_start;
            showMonitor();
            return;
        }
        monitor.necessaryFacts = compressedKb.totalNecessaryRecords();
        monitor.counterexamples = compressedKb.totalCounterexamples();
        monitor.supplementaryConstants = compressedKb.totalSupplementaryConstants();
        long time_dependency_resolved = System.currentTimeMillis();
        monitor.dependencyAnalysisTime = time_dependency_resolved - time_hypothesis_found;

        /* Log the hypothesis */
        logInfo("\n### Hypothesis Found ###");
        for (Rule rule : compressedKb.getHypothesis()) {
            logInfo(rule.toString(kb.getNumerationMap()));
        }
        logger.println();

        /* Dump the compressed KB */
        try {
            dumpCompressedKb();
        } catch (IOException e) {
            e.printStackTrace(logger);
            logError("Compressed KB dump failed. Abort.");
            return;
        }
        long time_kb_dumped = System.currentTimeMillis();
        monitor.dumpTime = time_kb_dumped - time_dependency_resolved;

        /* 检查结果 */
        if (config.validation) {
            if (!recover()) {
                logError("Validation Failed");
            }
        }
        long time_validated = System.currentTimeMillis();
        monitor.validationTime = time_validated - time_kb_dumped;

        /* Todo: Upload to neo4j in debug mode */
        long time_neo4j = System.currentTimeMillis();
        monitor.neo4jTime = time_neo4j - time_validated;

        monitor.totalTime = time_neo4j - time_start;
        showMonitor();
    }

    protected void logInfo(String msg) {
        logger.println(msg);
    }

    protected void logError(String msg) {
        logger.print("[ERROR] ");
        logger.println(msg);
    }

    /**
     * Run the compression and an interruption daemon.
     */
    public final void run() {
        interrupted = false;
        Thread task = new Thread(this::compress);
        task.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            while (!interrupted && task.isAlive()) {
                if (System.in.available() > 0) {
                    String line = reader.readLine();
                    if (INTERRUPT_CMD.equals(line)) {
                        break;
                    }
                }
                Thread.sleep(1000);
            }
            logger.flush();
        } catch (Exception e) {
            e.printStackTrace(logger);
        } finally {
            interrupted = true;
            try {
                task.join();
            } catch (InterruptedException e) {
                e.printStackTrace(logger);
            }
            logger.flush();
        }
    }
}
