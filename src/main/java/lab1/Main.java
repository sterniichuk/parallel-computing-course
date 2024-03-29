package lab1;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final int CPU_CORES = 8;
    private static final int CPU_LOGICAL_CORES = 16;
    private static final int MIN_THREADS = CPU_CORES / 2;
    private static final long SLEEP_TIME = TimeUnit.SECONDS.toMillis(1);

    private static final List<Integer> threadNumbers = List.of(
            1,
            MIN_THREADS,
            CPU_CORES,
            CPU_LOGICAL_CORES,
            CPU_LOGICAL_CORES * 2,
            CPU_LOGICAL_CORES * 4,
            CPU_LOGICAL_CORES * 8,
            CPU_LOGICAL_CORES * 16
    );
    private static final List<Integer> dimensionNumbers = List.of(
            MIN_THREADS * 256 / 2,
            MIN_THREADS * 256,
            MIN_THREADS * 256 * 2,
            MIN_THREADS * 256 * 4,
            MIN_THREADS * 256 * 8,
            MIN_THREADS * 256 * 16
    );

    public static void main(String[] args) {
        System.out.println("Lab1 has started...");
        dimensionNumbers.forEach(x -> {
            work(x);
            sleep();
            System.out.println();
        });
    }

    public static void work(final int size) {
        Matrix matrix = new Matrix(size);
        threadNumbers.forEach(threadNumber -> solve(matrix, threadNumber));
    }

    public static long solve(Matrix matrix, int threadNumber) {
        int size = matrix.size;
        checkParameters(size, threadNumber);
        if (threadNumber <= 1) {
            return executeSingleThread(matrix);
        }
        return executeParallel(matrix, threadNumber);
    }

    public static void checkParameters(int size, int threadNumber) {
        if (size % 2 != 0) {
            throw new IllegalArgumentException("Matrix dimension must be even for ability to swap all rows");
        }
        if (size <= threadNumber) {
            String message = "Number of threads can't be less than dimension of matrix. Dimension: "
                    + size + " threads: " + threadNumber;
            throw new IllegalArgumentException(message);
        }
        if (size % threadNumber != 0) {
            throw new IllegalArgumentException("size % threadNumber != 0");
        }
        if ((size / threadNumber) % 2 != 0) {
            throw new IllegalArgumentException("(size / threadNumber) % 2 != 0");
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static long executeSingleThread(Matrix matrix) {
        var timeCounter = timeCounter(() -> {
            for (int i = 0; i < matrix.size; i += 2) {
                matrix.swapRows(i, i + 1);
            }
        });
        printResult(matrix, 1, timeCounter);
        return timeCounter;
    }

    private static long executeParallel(Matrix matrix, int threadNumber) {
        final int size = matrix.size;
        if (size % threadNumber != 0) {
            throw new IllegalArgumentException("size divided by threadNumber should be integer.\n size = " + size + " threadNumber = " + threadNumber);
        }
        int rowsPerThread = (size / threadNumber);
        List<Thread> threads = new ArrayList<>(threadNumber);
        for (int i = 0; i < size; i += rowsPerThread) {
            final int startIndex = i;
            final int endIndexExclusively = rowsPerThread + i;
            Thread thread = new Thread(() -> {
                for (int j = startIndex; j < endIndexExclusively; j += 2) {
                    matrix.swapRows(j, j + 1);
                }
            });
            threads.add(thread);
        }
        var timeCounter = timeCounter(() -> {
            threads.forEach(Thread::start);
            joinAll(threads);
        });
        printResult(matrix, threadNumber, timeCounter);
        return timeCounter;
    }

    @SuppressWarnings("unused")
    private static void printResult(Matrix matrix, int threadNumber, long time) {
//        System.out.printf("Matrix dimension: %d, number of threads: %d, time: %d\n", matrix.size, threadNumber, time);
//        System.out.printf("%d,%d,%d\n", matrix.size, threadNumber, time);
    }

    public static void joinAll(List<Thread> threads) {
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static long timeCounter(Runnable runnable) {
        long start = System.nanoTime();
        runnable.run();
        long finish = System.nanoTime();
        return (finish - start) / 1000;
    }
}