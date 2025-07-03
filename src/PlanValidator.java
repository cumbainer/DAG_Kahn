import java.util.*;
import java.util.stream.Collectors;

public final class PlanValidator {
    private PlanValidator() {
    }

    public static boolean isOptimal(ExecutionPlan plan, Collection<VulnerabilityScript> scripts) {
        if (!containsEveryScriptExactlyOnce(plan, scripts)) {
            return false;
        }

        int criticalPathLen = computeCriticalPathLength(scripts);
        int minimalWaveCount = criticalPathLen + 1;

        return minimalWaveCount == plan.executionWaves().size();
    }

    private static boolean containsEveryScriptExactlyOnce(ExecutionPlan plan, Collection<VulnerabilityScript> scripts) {
        Set<Integer> expectedIds = scripts.stream().map(VulnerabilityScript::scriptId).collect(Collectors.toSet());

        List<Integer> waveIds = plan.executionWaves().stream().flatMap(Collection::stream).toList();
        Set<Integer> uniqueWaveIds = new HashSet<>(waveIds);

        boolean allWavesAreUnique = uniqueWaveIds.size() == waveIds.size();
        return allWavesAreUnique && uniqueWaveIds.equals(expectedIds);
    }

    private static int computeCriticalPathLength(Collection<VulnerabilityScript> scripts) {
        Map<Integer, List<Integer>> dependentsByParent = new HashMap<>();
        Map<Integer, Integer> remainingDepsByScript = new HashMap<>();

        for (VulnerabilityScript script : scripts) {
            remainingDepsByScript.putIfAbsent(script.scriptId(), 0);
            for (int parentId : script.dependencies()) {
                dependentsByParent
                        .computeIfAbsent(parentId, k -> new ArrayList<>())
                        .add(script.scriptId());
                remainingDepsByScript.merge(script.scriptId(), 1, Integer::sum);
            }
        }

        //Topological traversal + DP for longest path
        Queue<Integer> readyQueue = new ArrayDeque<>();
        Map<Integer, Integer> depth = new HashMap<>();   //longest path to node

        remainingDepsByScript.forEach((id, depsLeft) -> {
            if (depsLeft == 0) {
                readyQueue.add(id);
                depth.put(id, 0);
            }
        });

        int maxDepth = 0;
        while (!readyQueue.isEmpty()) {
            int parentId = readyQueue.remove();
            int parentDepth = depth.get(parentId);

            for (int childId : dependentsByParent.getOrDefault(parentId, List.of())) {
                depth.merge(childId, parentDepth + 1, Math::max);

                int remainingDependencies = remainingDepsByScript.merge(childId, -1, Integer::sum);
                if (remainingDependencies == 0) {
                    readyQueue.add(childId);
                }
            }
            maxDepth = Math.max(maxDepth, parentDepth);
        }
        return maxDepth;
    }
}
