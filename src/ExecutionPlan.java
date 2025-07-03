
import java.util.List;

public record ExecutionPlan(List<List<Integer>> executionWaves, List<String> warningsOnMissingDependency) {
    public int totalScripts() {
        return executionWaves.stream().mapToInt(List::size).sum();
    }
}
