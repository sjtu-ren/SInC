package sinc.exp;

import sinc.common.Constant;
import sinc.common.Dataset;
import sinc.common.KbStatistics;
import sinc.common.Predicate;
import sinc.impl.cached.MemKB;
import sinc.util.MultiSet;
import sinc.util.graph.BaseGraphNode;
import sinc.util.graph.Tarjan;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class SplitDataset extends MemKB {

    static final int CONST_ID = 0;

//    static class ColumnInfo {
//        final String functor;
//        final int index;
//
//        public ColumnInfo(String functor, int index) {
//            this.functor = functor;
//            this.index = index;
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (this == o) return true;
//            if (o == null || getClass() != o.getClass()) return false;
//            ColumnInfo that = (ColumnInfo) o;
//            return index == that.index && functor.equals(that.functor);
//        }
//
//        @Override
//        public int hashCode() {
//            return Objects.hash(functor, index);
//        }
//    }

    static class ConnectivityInfo {
        final double threshold;
        final Integer[] sccSizes;

        public ConnectivityInfo(double threshold, Integer[] sccSizes) {
            this.threshold = threshold;
            this.sccSizes = sccSizes;
        }
    }

    Map<BaseGraphNode<String>, Set<BaseGraphNode<String>>> connectivityGraph;  // Nodes representing functors

    void loadKB(Dataset dataset) throws IOException {
        System.out.println("Load Dataset...");
        BufferedReader reader = new BufferedReader(new FileReader(dataset.getPath()));
        String line;
        while (null != (line = reader.readLine())) {
            final String[] components = line.split("\t");
            final Predicate predicate = new Predicate(components[0], components.length - 1);
            for (int i = 1; i < components.length; i++) {
                predicate.args[i - 1] = new Constant(CONST_ID, components[i]);
            }
            addFact(predicate);
        }
    }

    List<ConnectivityInfo> constructConnectivityGraph(int steps) {
        System.out.println("Construct Connectivity Graph...");
        /* Initiate Graph */
        connectivityGraph = new HashMap<>();
        Map<String, BaseGraphNode<String>> node_map = new HashMap<>();
        for (String functor: functor2ArityMap.keySet()) {
            BaseGraphNode<String> node = new BaseGraphNode<>(functor);
            node_map.put(functor, node);
            connectivityGraph.put(node, new HashSet<>());
        }

        /* Calculate Similarities */
        System.out.println("\tcalculate similarities...");
        class SimInfo {
            final String func1, func2;
            final double similarity;

            public SimInfo(String func1, String func2, double similarity) {
                this.func1 = func1;
                this.func2 = func2;
                this.similarity = similarity;
            }
        }
        List<SimInfo> similarities = new ArrayList<>();
        Map.Entry<String, MultiSet<String>[]>[] entries = functor2ArgSetsMap.entrySet().toArray(new Map.Entry[0]);
        for (int i = 0; i < entries.length; i++) {
            String functor1 = entries[i].getKey();
            MultiSet<String>[] arg_sets1 = entries[i].getValue();
            for (int j = i + 1; j < entries.length; j++) {
                String functor2 = entries[j].getKey();
                MultiSet<String>[] arg_sets2 = entries[j].getValue();
                double max_sim = 0;
                for (MultiSet<String> art_set1 : arg_sets1) {
                    for (MultiSet<String> arg_set2 : arg_sets2) {
                        double similarity = art_set1.jaccardSimilarity(arg_set2);
//                        max_sim = Math.max(max_sim, similarity);
                        max_sim = (max_sim >= similarity) ? max_sim : similarity;
                    }
                }
                similarities.add(new SimInfo(functor1, functor2, max_sim));
            }
        }
        similarities.sort(Comparator.comparingDouble(s -> s.similarity));

        /* Add Edges */
        System.out.println("\tadd edges...");
        List<ConnectivityInfo> connectivity_info_list = new ArrayList<>();
        connectivity_info_list.add(new ConnectivityInfo(0, new Integer[]{functor2ArityMap.size()}));  // At the beginning, there is always one SCC containing all nodes

        final double max_sim = similarities.get(similarities.size() - 1).similarity;
        final double step_size = max_sim / steps;
        double threshold = step_size;  // Start from the first step

        for (SimInfo sim_info : similarities) {
            if (threshold <= sim_info.similarity) {
                BaseGraphNode<String> node1 = node_map.get(sim_info.func1);
                BaseGraphNode<String> node2 = node_map.get(sim_info.func2);
                connectivityGraph.get(node1).add(node2);
                connectivityGraph.get(node2).add(node1);
            }
        }

        int edge_idx = 0;
        while (threshold < max_sim) {
            System.out.printf("\tuse threshold: %f\n", threshold);

            /* Calculate on the graph structure */
            Tarjan<BaseGraphNode<String>> tarjan = new Tarjan<>(connectivityGraph);
            List<Set<BaseGraphNode<String>>> sccs = tarjan.run();
            Integer[] scc_sizes = new Integer[sccs.size()];
            for (int i = 0; i < scc_sizes.length; i++) {
                scc_sizes[i] = sccs.get(i).size();
            }
            Arrays.sort(scc_sizes, Collections.reverseOrder());
            connectivity_info_list.add(new ConnectivityInfo(threshold, scc_sizes));

            /* Update threshold & remove edges */
            threshold += step_size;
            for (; edge_idx < similarities.size() && similarities.get(edge_idx).similarity < threshold; edge_idx++) {
                SimInfo sim_info = similarities.get(edge_idx);
                BaseGraphNode<String> node1 = node_map.get(sim_info.func1);
                BaseGraphNode<String> node2 = node_map.get(sim_info.func2);
                connectivityGraph.get(node1).remove(node2);
                connectivityGraph.get(node2).remove(node1);
            }

            /* clear node marks */
            for (BaseGraphNode<String> node: node_map.values()) {
                node.index = BaseGraphNode.NO_TARJAN_INDEX;
                node.lowLink = BaseGraphNode.NO_TARJAN_LOW_LINK;
                node.onStack = false;
            }
        }

        return connectivity_info_list;
    }

    void dump(List<ConnectivityInfo> connectivityList, String fname) throws IOException {
        System.out.println("Dump result...");
        PrintWriter writer = new PrintWriter(fname);
        writer.print('[');
        ConnectivityInfo info = connectivityList.get(0);
        writer.printf("{\"%s\":%f,\"%s\":%s}", "threshold", info.threshold, "sccSizes", Arrays.toString(info.sccSizes));
        for (int i = 1; i < connectivityList.size(); i++) {
            info = connectivityList.get(i);
            writer.printf(",\n{\"%s\":%f,\"%s\":%s}", "threshold", info.threshold, "sccSizes", Arrays.toString(info.sccSizes));
        }
        writer.print(']');
        writer.close();
    }

    void run(Dataset dataset, int steps) throws IOException {
        long time_start = System.currentTimeMillis();
        loadKB(dataset);
        dump(constructConnectivityGraph(steps), dataset.getName() + "_connectivity.json");
        long time_done = System.currentTimeMillis();
        System.out.printf("Time: %d ms", time_done - time_start);
    }

    public static void main(String[] args) throws IOException {
        SplitDataset splitDataset = new SplitDataset();
        splitDataset.run(Dataset.CODEX, 10);
    }
}
