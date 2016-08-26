package emulator;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

class Renderer {
    private final CPU cpu;
    private final GraphicsContext gc;
    private final int scale;

    Renderer(CPU cpu, GraphicsContext gc, int scale) {
        this.cpu = cpu;
        this.gc = gc;
        this.scale = scale;
    }

    void updateCanvas() {
        int[][] vMem = cpu.getvMem();

        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 32; y++) {
                Color color = vMem[x][y] == 1 ? Color.WHITE : Color.BLACK;
                gc.setFill(color);
                gc.fillRect(x * scale, y * scale, scale, scale);
            }
        }
    }
}
