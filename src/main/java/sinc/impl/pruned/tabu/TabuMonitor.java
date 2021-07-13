package sinc.impl.pruned.tabu;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabuMonitor {
    private static final int DENOMINATOR = 1000000;

    /* Time Monitor */
    public long tabuCheckCostInNano = 0;

    /* Statistics Monitor */
    public int totalTabus = 0;
    public int totalTabuCompares = 0;
    public List<Integer> tabusInDiffHeadFunctor = new ArrayList<>();

    public void show(PrintWriter writer) {
        writer.println("### Tabu Performance Info ###\n");
        writer.println("--- Statistics ---");
        writer.printf(
                " %10s %10s %10s\n",
                "Check(ms)", "#Tabu", "#Comp"
        );
        writer.printf(
                " %10d %10d %10d\n\n",
                tabuCheckCostInNano / DENOMINATOR,
                totalTabus,
                totalTabuCompares
        );
        writer.print("- #Tabu In Different Head Functors: ");
        writer.println(Arrays.toString(tabusInDiffHeadFunctor.toArray(new Integer[0])));
    }
}
