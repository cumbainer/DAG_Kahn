import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ExecutionPlanner {

    public ExecutionPlan plan(Collection<VulnerabilityScript> scripts) {
        GraphData graphData = buildDirectedAcyclicGraph(scripts);

        Queue<Integer> readyQueue = initReadyQueue(graphData.remainingDeps);
        List<List<Integer>> waves = generateExecutionWaves(readyQueue, graphData.dependents, graphData.remainingDeps);

        verifyAcyclic(waves, scripts.size());
        return new ExecutionPlan(waves, graphData.warnings);
    }

    private GraphData buildDirectedAcyclicGraph(Collection<VulnerabilityScript> scripts) {
        Map<Integer, VulnerabilityScript> scriptsById = scripts.stream().collect(Collectors.toMap(
                VulnerabilityScript::scriptId,
                Function.identity()
        ));

        Map<Integer, List<Integer>> dependentsByParent = new HashMap<>();
        Map<Integer, Integer> remainingDeps = new HashMap<>();
        List<String> missingDepsWarnings = new ArrayList<>();

        for (VulnerabilityScript script : scripts) {
            remainingDeps.putIfAbsent(script.scriptId(), 0);

            for (int parentId : script.dependencies()) {
                if (!scriptsById.containsKey(parentId)) {
                    missingDepsWarnings.add("⚠ Script %d depends on missing %d"
                            .formatted(script.scriptId(), parentId));
                    continue;
                }
                dependentsByParent
                        .computeIfAbsent(parentId, k -> new ArrayList<>())
                        .add(script.scriptId());

                remainingDeps.merge(script.scriptId(), 1, Integer::sum);
            }
        }
        return new GraphData(dependentsByParent, remainingDeps, missingDepsWarnings);
    }

    //Vertices with indegree 0
    private Queue<Integer> initReadyQueue(Map<Integer, Integer> remainingDeps) {
        Queue<Integer> readyQueue = new ArrayDeque<>();
        remainingDeps.forEach((id, depsLeft) -> {
            if (depsLeft == 0) {
                readyQueue.add(id);
            }
        });
        return readyQueue;
    }

    //Layered Kahn algorithm (Topological sorting on directed acyclic graph)
    private List<List<Integer>> generateExecutionWaves(
            Queue<Integer> readyQueue,
            Map<Integer, List<Integer>> dependentsByParent,
            Map<Integer, Integer> remainingDeps) {
        List<List<Integer>> waves = new ArrayList<>();

        while (!readyQueue.isEmpty()) {
            int waveSize = readyQueue.size();
            List<Integer> currentWave = new ArrayList<>(waveSize);

            for (int i = 0; i < waveSize; i++) {
                int scriptId = readyQueue.remove();
                currentWave.add(scriptId);

                for (int childId
                        : dependentsByParent.getOrDefault(scriptId, List.of())) {

                    int depsLeft = remainingDeps.merge(childId, -1, Integer::sum);
                    if (depsLeft == 0) readyQueue.add(childId);
                }
            }
            waves.add(currentWave);
        }
        return waves;
    }

    private void verifyAcyclic(List<List<Integer>> waves, int totalScripts) {
        int scheduled = waves.stream().mapToInt(List::size).sum();
        if (scheduled != totalScripts) {
            throw new IllegalStateException(
                    "Cyclic dependency detected: %d script(s) in cycle"
                            .formatted(totalScripts - scheduled));
        }
    }

    private record GraphData(Map<Integer, List<Integer>> dependents,
                             Map<Integer, Integer> remainingDeps,
                             List<String> warnings) {
    }
}
