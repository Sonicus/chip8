package emulator;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import util.RomUtil;

public class Main extends Application {

    private byte[] romData;
    private CPU cpu;
    private Renderer renderer;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Pane root = FXMLLoader.load(getClass().getResource("emulator.fxml"));
        Canvas canvas = new Canvas(64, 32);
        primaryStage.setTitle("Chip8");
        primaryStage.setScene(new Scene(root, 64, 32));
        root.getChildren().add(canvas);
        primaryStage.setResizable(false);
        primaryStage.show();

        try {
            initialize(canvas);
        } catch (Exception e) {
            System.out.println("Couldn't initialize emulator");
            e.printStackTrace();
        }

        try {
            run();
        }catch (Exception e){
            System.out.println("Execution halted unexpectedly");
            e.printStackTrace();
        }
    }

    private void initialize(Canvas canvas) throws Exception {
        romData = RomUtil.LoadRom("games/PONG");

        System.out.println("Rom Data:\n---------");
        int index = 0;
        for (byte b : romData) {
            System.out.print(String.format("%02X ", b));
            index++;
            if (index % 8 == 0) {
                System.out.print("\n");
            }
        }

        cpu = new CPU(romData);
        renderer = new Renderer(cpu, canvas);
    }

    private void run() throws Exception{
        System.out.println("\nExecution started:\n------------------");
        while(true){
            cpu.executeCycle();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
