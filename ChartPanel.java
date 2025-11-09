import javax.swing.*;
import java.awt.*;

/**
 * Спеціалізована панель (наша "канва") для малювання
 * стовпчастої діаграми результатів.
 * Використовує Graphics2D для якісного рендерингу.
 *
 * ОНОВЛЕНО: Додано форматування часу (хв/сек/мс).
 */
public class ChartPanel extends JPanel {

    // ... (Константи кольорів залишаються без змін) ...
    private static final Color COLOR_GRID = new Color(70, 70, 70);
    private static final Color COLOR_TEXT = new Color(187, 187, 187);
    private static final Color COLOR_BAR_SERIAL = new Color(60, 100, 160);
    private static final Color COLOR_BAR_PARALLEL = new Color(170, 80, 80);
    private static final Color COLOR_SPEEDUP = new Color(140, 130, 70);

    private final double serialTimeMs;
    private final double parallelTimeMs;
    private final double speedUp;

    public ChartPanel(double serialTimeMs, double parallelTimeMs, double speedUp) {
        // ... (Конструктор залишається без змін) ...
        this.serialTimeMs = serialTimeMs;
        this.parallelTimeMs = parallelTimeMs;
        this.speedUp = speedUp;
        
        setBackground(UIManager.getColor("Panel.background"));
    }

    /**
     * НОВИЙ ДОПОМІЖНИЙ МЕТОД
     * Форматує час (мс) у читабельний рядок (хв, сек, мс).
     */
    private String formatTime(double totalMilliseconds) {
        // Якщо час менший за 1 секунду
        if (totalMilliseconds < 1000) {
            return String.format("%.2f мс", totalMilliseconds);
        }

        double totalSeconds = totalMilliseconds / 1000.0;
        
        // Якщо час менший за 1 хвилину
        if (totalSeconds < 60) {
            return String.format("%.3f сек", totalSeconds);
        }
        
        // Якщо час 1 хвилина або більше
        long minutes = (long) (totalSeconds / 60.0);
        double secondsFraction = totalSeconds % 60.0;
        
        return String.format("%d хв %.3f сек", minutes, secondsFraction);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g;

        // ... (Налаштування згладжування без змін) ...
        g2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // ... (Геометрія та малювання осей без змін) ...
        int padding = 60;
        int chartWidth = width - 2 * padding;
        int chartHeight = height - 2 * padding;
        int barWidth = chartWidth / 4;
        double maxTime = serialTimeMs;
        int serialBarHeight = (int) ((serialTimeMs / maxTime) * chartHeight);
        int parallelBarHeight = (int) ((parallelTimeMs / maxTime) * chartHeight);
        g2d.setColor(COLOR_GRID);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        int yAxis = height - padding;
        g2d.drawLine(padding, yAxis, width - padding, yAxis);
        int xAxis = padding;
        g2d.drawLine(xAxis, padding, xAxis, height - padding);

        // --- ОНОВЛЕНО: Малюємо стовпець "Послідовно" ---
        int xSerial = padding + (chartWidth / 4) - (barWidth / 2);
        int ySerial = yAxis - serialBarHeight;
        g2d.setColor(COLOR_BAR_SERIAL);
        g2d.fillRect(xSerial, ySerial, barWidth, serialBarHeight);
        
        g2d.setColor(COLOR_TEXT);
        g2d.drawString("Послідовно", xSerial + barWidth / 2 - 40, yAxis + 20);
        
        // ОНОВЛЕНО: Використовуємо форматування
        // та центруємо текст над стовпцем
        String serialTimeText = formatTime(serialTimeMs);
        FontMetrics fm = g2d.getFontMetrics();
        int serialTextWidth = fm.stringWidth(serialTimeText);
        g2d.drawString(serialTimeText,
                xSerial + (barWidth / 2) - (serialTextWidth / 2),
                ySerial - 10);

        // --- ОНОВЛЕНО: Малюємо стовпець "Паралельно" ---
        int xParallel = padding + (chartWidth * 3 / 4) - (barWidth / 2);
        int yParallel = yAxis - parallelBarHeight;
        g2d.setColor(COLOR_BAR_PARALLEL);
        g2d.fillRect(xParallel, yParallel, barWidth, parallelBarHeight);

        g2d.setColor(COLOR_TEXT);
        g2d.drawString("Паралельно", xParallel + barWidth / 2 - 40, yAxis + 20);
        
        // ОНОВЛЕНО: Використовуємо форматування
        // та центруємо текст над стовпцем
        String parallelTimeText = formatTime(parallelTimeMs);
        int parallelTextWidth = fm.stringWidth(parallelTimeText);
        g2d.drawString(parallelTimeText,
                xParallel + (barWidth / 2) - (parallelTextWidth / 2),
                yParallel - 10);
        
        // ... (Малювання тексту "Прискорення" без змін) ...
        g2d.setColor(COLOR_SPEEDUP);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        String speedUpText = String.format("Прискорення: %.2f x", speedUp);
        int textWidth = g2d.getFontMetrics().stringWidth(speedUpText);
        g2d.drawString(speedUpText, (width - textWidth) / 2, padding - 15);
    }
}