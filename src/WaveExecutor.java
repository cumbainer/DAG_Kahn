import java.util.*;
import java.util.concurrent.*;

public final class WaveExecutor {

    private final Map<Integer, VulnerabilityScript> catalog;

    public WaveExecutor(Collection<VulnerabilityScript> scripts) {
        catalog = new HashMap<>();
        scripts.forEach(s -> catalog.put(s.scriptId(), s));
    }

    public void execute(ExecutionPlan plan) {
        int idx = 0;
        for (List<Integer> wave : plan.executionWaves()) {
            System.out.printf("%n=== Wave %d : %d script(s) ===%n", idx++, wave.size());

            try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Callable<Void>> tasks = wave.stream()
                        .<Callable<Void>>map(id -> () -> {
                            catalog.get(id).run();
                            return null;
                        })
                        .toList();

                exec.invokeAll(tasks);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }
}
