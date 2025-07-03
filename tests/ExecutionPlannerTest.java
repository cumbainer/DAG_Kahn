import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Planner stress test")
class ExecutionPlannerTest {
    private static final int TOTAL_SCRIPT_COUNT = 10_0000;
    private static final int MAX_DEPENDENCIES_PER_SCRIPT = 40;
    private static final long RANDOM_SEED = 42L;

    private static List<VulnerabilityScript> generateRandomDag() {
        Random random = new Random(RANDOM_SEED);
        List<VulnerabilityScript> scripts = new ArrayList<>(TOTAL_SCRIPT_COUNT);

        for (int scriptId = 0; scriptId < TOTAL_SCRIPT_COUNT; scriptId++) {
            int dependencyCount = random.nextInt(MAX_DEPENDENCIES_PER_SCRIPT + 1);
            List<Integer> dependencyIds = new ArrayList<>(dependencyCount);

            for (int k = 0; k < dependencyCount; k++) {
                if (scriptId == 0) break;
                int parentId = random.nextInt(scriptId);
                dependencyIds.add(parentId);
            }
            scripts.add(new VulnerabilityScript(scriptId, dependencyIds));
        }
        return scripts;
    }

    @Test
    @DisplayName("Planner handles 10 000 scripts quickly & emits an optimal plan")
    void plannerProducesOptimalPlanUnderLoad() {
        List<VulnerabilityScript> scripts = generateRandomDag();
        ExecutionPlanner planner = new ExecutionPlanner();

        ExecutionPlan plan = assertTimeoutPreemptively(
                Duration.ofSeconds(10),
                () -> planner.plan(scripts),
                "Planner took too long; algorithm should be O(V + E)");
        assertEquals(TOTAL_SCRIPT_COUNT, plan.totalScripts(), "Every script must appear exactly once in the plan");
        assertTrue(PlanValidator.isOptimal(plan, scripts), "Planner failed optimal-wave validation");
        assertTrue(plan.warningsOnMissingDependency().isEmpty(), "No missing-dependency warnings expected");

        Instant start = Instant.now();
        new WaveExecutor(scripts).execute(plan);
        Instant end = Instant.now();

        ExecutionReport.print(plan, scripts, start, end);
    }
}
