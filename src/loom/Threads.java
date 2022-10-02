package loom;

import jdk.incubator.concurrent.StructuredTaskScope;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

class Threads {
    public static void main(String[] args) {
        virtualThreads();
//        platformThreads();

        System.out.println("Summed ints: " + sumInts());
    }

    private static void virtualThreads() {
        System.out.println("Creating threads...");
        List<Thread> threads = IntStream.rangeClosed(1, 10_000_000)
                .mapToObj(n -> Thread.ofVirtual()
                                     .name("virtual-" + n)
                                     .start(() -> {
                                          try {
                                              TimeUnit.MILLISECONDS.sleep(100);
                                          } catch (InterruptedException e) {
                                              Thread.currentThread().interrupt();
                                          }
                                      })
                ).toList();

        System.out.println("Waiting for threads...");
        threads.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        System.out.println("Finished");
    }

    private static void platformThreads() {
        System.out.println("Creating threads...");
        List<Thread> threads = IntStream.rangeClosed(1, 1_000_000)
                                        .mapToObj(n -> Thread.ofPlatform()
                                                             .name("platform-" + n)
                                                             .start(() -> {
                                                                 try {
                                                                     TimeUnit.MILLISECONDS.sleep(100);
                                                                 } catch (InterruptedException e) {
                                                                     Thread.currentThread().interrupt();
                                                                 }
                                                             })
                                        ).toList();

        System.out.println("Waiting for threads...");
        threads.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private static int sumInts() {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var futures = IntStream.rangeClosed(1, 1_000_000)
                                   .mapToObj(n -> scope.fork(Threads::heavyComputation))
                                   .toList();

            scope.join();          // Join all threads
            scope.throwIfFailed(); // ... and propagate errors

            // Here, both forks have succeeded, so compose their results
            return futures.stream()
                          .map(Future::resultNow)
                          .mapToInt(Integer::intValue)
                          .sum();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static int heavyComputation() {
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return 1;
    }
}
