import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder; // Використовуємо для паралельної суми

/**
 * Клас, що інкапсулює логіку Практичної роботи №3, Варіант 8.
 *
 * ВЕРСІЯ 3: Алгоритм O(N^2), оптимізований по пам'яті.
 * Ми більше не створюємо гігантський масив int[N][N] для "порядків",
 * а обчислюємо їх "на льоту" у три проходи, щоб уникнути
 * OutOfMemoryError.
 */
public class MatrixLabLogic {

    /**
     * POJO (record) для зберігання результатів обчислень.
     * ordersMatrix тепер завжди буде null, оскільки ми його не зберігаємо.
     */
    public record CalculationResult(
            double sum,
            int maxOrder,
            long durationNs,
            int[][] ordersMatrix) { // Це поле більше не використовується
    }

    /**
     * POJO (record) для зберігання прорахованих діагоналей.
     */
    private record DiagonalCounts(
            Map<Integer, Integer> mainDiagonals,
            Map<Integer, Integer> antiDiagonals) {
    }

    /**
     * Створює квадратну матрицю. (Без змін)
     */
    public static double[][] generateMatrix(int size, double zeroChance) {
        double[][] matrix = new double[size][size];
        Random rand = new Random();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (rand.nextDouble() < zeroChance) {
                    matrix[i][j] = 0.0;
                } else {
                    matrix[i][j] = (rand.nextDouble() * 20.0) - 10.0;
                }
            }
        }
        return matrix;
    }

    /**
     * Крок 1: Послідовно обчислює діагоналі. (Без змін)
     */
    private DiagonalCounts calculateDiagonalCountsSerial(double[][] matrix) {
        int n = matrix.length;
        Map<Integer, Integer> mainDiagonals = new HashMap<>();
        Map<Integer, Integer> antiDiagonals = new HashMap<>();

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (matrix[i][j] != 0.0) {
                    // 1. Головна діагональ
                    int mainKey = i - j;
                    mainDiagonals.put(mainKey, mainDiagonals.getOrDefault(mainKey, 0) + 1);

                    // 2. Побічна діагональ
                    int antiKey = i + j;
                    antiDiagonals.put(antiKey, antiDiagonals.getOrDefault(antiKey, 0) + 1);
                }
            }
        }
        return new DiagonalCounts(mainDiagonals, antiDiagonals);
    }

    /**
     * Допоміжний метод для обчислення порядку ОДНОГО елемента "на льоту".
     * Не вимагає зберігання матриці порядків.
     */
    private int calculateOrderForElement(double[][] matrix,
                                         DiagonalCounts counts, int i, int j) {
        int mainKey = i - j;
        int antiKey = i + j;

        int order = counts.mainDiagonals().getOrDefault(mainKey, 0) +
                    counts.antiDiagonals().getOrDefault(antiKey, 0);

        // Віднімаємо 1, якщо сам елемент ненульовий
        if (matrix[i][j] != 0.0) {
            order--;
        }
        return order;
    }

    /**
     * Послідовна (однопотокова) реалізація (O(N^2), 3 проходи).
     * ОПТИМІЗОВАНО ПО ПАМ'ЯТІ.
     */
    public CalculationResult calculateSerial(double[][] matrix) {
        long startTime = System.nanoTime();
        int n = matrix.length;

        // Крок 1: Порахувати всі діагоналі (O(N^2))
        DiagonalCounts counts = calculateDiagonalCountsSerial(matrix);

        // Крок 2: Знайти maxOrder (O(N^2))
        // БЕЗ СТВОРЕННЯ МАСИВУ `orders`
        int maxOrder = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int order = calculateOrderForElement(matrix, counts, i, j);
                if (order > maxOrder) {
                    maxOrder = order;
                }
            }
        }

        // Крок 3: Просумувати елементи з найбільшим порядком (O(N^2))
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                // Обчислюємо порядок знову
                int order = calculateOrderForElement(matrix, counts, i, j);
                if (order == maxOrder) {
                    sum += matrix[i][j];
                }
            }
        }

        long durationNs = System.nanoTime() - startTime;
        // Передаємо `null` замість масиву
        return new CalculationResult(sum, maxOrder, durationNs, null);
    }

    /**
     * Паралельна (багатопотокова) реалізація.
     * ОПТИМІЗОВАНО ПО ПАМ'ЯТІ.
     */
    public CalculationResult calculateParallel(double[][] matrix, int numThreads) {
        long startTime = System.nanoTime();
        int n = matrix.length;
        
        // Крок 1: Послідовно рахуємо діагоналі (O(N^2)).
        // Цей крок дуже швидкий, його розпаралелення
        // (з ConcurrentHashMap) дасть більше накладних витрат.
        DiagonalCounts counts = calculateDiagonalCountsSerial(matrix);

        // --- Крок 2: Паралельно знаходимо maxOrder ---
        AtomicInteger globalMaxOrder = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<Void>> tasks = new ArrayList<>();

        int rowsPerThread = n / numThreads;
        for (int t = 0; t < numThreads; t++) {
            final int startRow = t * rowsPerThread;
            final int endRow = (t == numThreads - 1) ? n : startRow + rowsPerThread;

            tasks.add(executor.submit(() -> {
                int localMaxOrder = 0;
                for (int i = startRow; i < endRow; i++) {
                    for (int j = 0; j < n; j++) {
                        int order = calculateOrderForElement(matrix, counts, i, j);
                        if (order > localMaxOrder) {
                            localMaxOrder = order;
                        }
                    }
                }
                // (Виправлення для лямбди)
                final int finalThreadMaxOrder = localMaxOrder;
                globalMaxOrder.updateAndGet(currentMax ->
                        Math.max(currentMax, finalThreadMaxOrder));
                return null; // (Виправлення ArrayStoreException)
            }));
        }
        // Чекаємо на завершення Кроку 2
        for (Future<Void> task : tasks) {
            try { task.get(); } catch (Exception e) { e.printStackTrace(); }
        }
        executor.shutdown();

        final int maxOrder = globalMaxOrder.get();

        // --- Крок 3: Паралельно сумуємо ---
        // Використовуємо DoubleAdder для безпечної
        // та ефективної паралельної суми
        DoubleAdder globalSum = new DoubleAdder();
        executor = Executors.newFixedThreadPool(numThreads); // Новий пул потоків
        tasks.clear(); // Очищуємо список

        for (int t = 0; t < numThreads; t++) {
            final int startRow = t * rowsPerThread;
            final int endRow = (t == numThreads - 1) ? n : startRow + rowsPerThread;
            
            tasks.add(executor.submit(() -> {
                for (int i = startRow; i < endRow; i++) {
                    for (int j = 0; j < n; j++) {
                        int order = calculateOrderForElement(matrix, counts, i, j);
                        if (order == maxOrder) {
                            globalSum.add(matrix[i][j]);
                        }
                    }
                }
                return null; // (Виправлення ArrayStoreException)
            }));
        }
        // Чекаємо на завершення Кроку 3
        for (Future<Void> task : tasks) {
            try { task.get(); } catch (Exception e) { e.printStackTrace(); }
        }
        executor.shutdown();

        double sum = globalSum.sum();
        long durationNs = System.nanoTime() - startTime;
        
        return new CalculationResult(sum, maxOrder, durationNs, null);
    }

    /**
     * Main-метод для тестування логіки в консолі.
     */
    public static void main(String[] args) {
        // Використовуємо менший розмір для швидкого тесту в консолі
        final int MATRIX_SIZE = 10000; 
        final double ZERO_CHANCE = 0.3;
        final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();

        System.out.printf("Генерація матриці %d x %d...\n",
                MATRIX_SIZE, MATRIX_SIZE);
        double[][] matrix = generateMatrix(MATRIX_SIZE, ZERO_CHANCE);
        System.out.println("Матриця згенерована.");

        MatrixLabLogic logic = new MatrixLabLogic();

        // --- Послідовний запуск ---
        System.out.println("\n--- Запуск послідовної версії (O(N^2), low memory) ---");
        CalculationResult serialResult = logic.calculateSerial(matrix);
        double serialTimeMs = serialResult.durationNs() / 1_000_000.0;
        System.out.printf("Найбільший порядок: %d\n", serialResult.maxOrder());
        System.out.printf("Сума елементів:     %.4f\n", serialResult.sum());
        System.out.printf("Час виконання:      %.2f мс\n", serialTimeMs);

        // --- Паралельний запуск ---
        System.out.printf("\n--- Запуск паралельної версії (%d потоків, low memory) ---\n",
                THREAD_COUNT);
        CalculationResult parallelResult =
                logic.calculateParallel(matrix, THREAD_COUNT);
        double parallelTimeMs = parallelResult.durationNs() / 1_000_000.0;

        System.out.printf("Найбільший порядок: %d\n", parallelResult.maxOrder());
        System.out.printf("Сума елементів:     %.4f\n", parallelResult.sum());
        System.out.printf("Час виконання:      %.2f мс\n", parallelTimeMs);

        // --- Порівняння ---
        System.out.println("\n--- Результат ---");
        System.out.printf("Прискорення (Speed-up): %.2f x\n",
                (serialTimeMs / parallelTimeMs));

        // Перевірка коректності
        boolean isSumCorrect = Math.abs(serialResult.sum() - parallelResult.sum()) < 1e-9;
        boolean isOrderCorrect = serialResult.maxOrder() == parallelResult.maxOrder();
        if (isSumCorrect && isOrderCorrect) {
            System.out.println("Верифікація: УСПІШНА (результати збігаються).");
        } else {
            System.err.println("Верифікація: ПОМИЛКА (результати НЕ збігаються).");
        }
    }
}