import java.time.Instant;
import java.util.List;
import java.util.Set;

public class App {

    public static void main(String[] args) {
        var scripts = Set.of(
                new VulnerabilityScript(1, List.of(4, 5)),
                new VulnerabilityScript(2, List.of(1)),
                new VulnerabilityScript(3, List.of(4, 5, 1)),
                new VulnerabilityScript(4, List.of(5)),
                new VulnerabilityScript(5, List.of())
        );

        ExecutionPlanner planner = new ExecutionPlanner();

        ExecutionPlan plan = planner.plan(scripts);

        Instant start = Instant.now();
        new WaveExecutor(scripts).execute(plan);
        Instant end = Instant.now();

        ExecutionReport.print(plan, List.copyOf(scripts), start, end);
    }

}
