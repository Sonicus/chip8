package emulator;

import javafx.scene.canvas.Canvas;

class Renderer {
    private CPU cpu;
    private Canvas canvas;

    Renderer(CPU cpu, Canvas canvas){
        this.cpu = cpu;
        this.canvas = canvas;

    }
}
