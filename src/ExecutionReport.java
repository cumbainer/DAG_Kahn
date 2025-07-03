import java.time.Duration;
import java.time.Instant;
import java.util.IntSummaryStatistics;
import java.util.List;

public final class ExecutionReport {

    public static void print(ExecutionPlan plan,
                             List<VulnerabilityScript> scripts,
                             Instant start, Instant end) {
        System.out.println("\n============== Execution Report ==============");
        plan.warningsOnMissingDependency().forEach(System.out::println);

        int totalWaves = plan.executionWaves().size();
        int totalScripts = plan.totalScripts();
        double avgParallelism = totalScripts / (double) totalWaves;

        boolean optimal = PlanValidator.isOptimal(plan, scripts);

        System.out.printf("""
                Total scripts            : %d
                Total execution waves    : %d
                Scripts per wave         : %s
                Average parallelism      : %.2f
                Optimal wave count?      : %b
                Elapsed                  : %d ms
                ===============================================%n""",
                totalScripts, totalWaves, plan.executionWaves().stream().map(List::size).toList(),
                avgParallelism, optimal, Duration.between(start, end).toMillis());

        IntSummaryStatistics stats = plan.executionWaves().stream().mapToInt(List::size).summaryStatistics();
        System.out.println("Stats: " + stats.toString());
    }
}
