package emulator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import util.RomUtil;

import java.util.Optional;

public class Main extends Application {

    private byte[] romData;
    private CPU cpu;
    private Renderer renderer;
    private int scale = 7;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Pane root = FXMLLoader.load(getClass().getResource("emulator.fxml"));
        Canvas canvas = new Canvas(64 * scale, 32 * scale);
        primaryStage.setTitle("Chip8");
        primaryStage.setScene(new Scene(root, 64 * scale, 32 * scale));
        root.getChildren().add(canvas);
        primaryStage.setResizable(false);
        primaryStage.sizeToScene();
        primaryStage.show();
        canvas.getGraphicsContext2D().setFill(Color.BLACK);
        canvas.getGraphicsContext2D().fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        try {
            initialize(canvas);
        } catch (Exception e) {
            System.out.println("Couldn't initialize emulator");
            e.printStackTrace();
            showAlert("Couldn't initialize emulator", e, Optional.empty(), Optional.empty());
        }

        try {
            run();
        } catch (Exception e) {
            System.out.println("Execution halted unexpectedly");
            e.printStackTrace();
            showAlert("Execution halted unexpectedly", e, Optional.of(cpu.getPC()), Optional.of(cpu.getCycleNumber()));
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
        renderer = new Renderer(cpu, canvas.getGraphicsContext2D(), scale);
    }

    private void run() throws Exception {
        System.out.println("\nExecution started:\n------------------");
        while (true) {
            cpu.executeCycle();
            if (cpu.isDrawFlag()) {
                renderer.updateCanvas();
            }
        }
    }

    private void showAlert(String message, Exception e, Optional<Short> PC, Optional<Long> cycleNumber) {
        Alert alert;
        if (PC.isPresent() && cycleNumber.isPresent()) {
            alert = new Alert(Alert.AlertType.ERROR, message + "\n" + e.getMessage() +
                    "\nPC: " + String.format("%04X", PC.get()) +
                    "\nCycle number: " + cycleNumber.get());
        } else {
            alert = new Alert(Alert.AlertType.ERROR, message + "\n" + e.getMessage());
        }

        alert.showAndWait();

        if (alert.getResult() == ButtonType.OK) {
            Platform.exit();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
