package emulator;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import util.RomUtil;

import java.util.Optional;

public class Main extends Application {

    private CPU cpu;
    private Renderer renderer;
    private final int scale = 7;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Pane root = FXMLLoader.load(getClass().getResource("emulator.fxml"));
        primaryStage.setTitle("Chip8");
        primaryStage.setScene(new Scene(root, 64 * scale, 32 * scale));
        primaryStage.setResizable(false);
        primaryStage.sizeToScene();
        Canvas canvas = new Canvas(64 * scale, 32 * scale);
        canvas.getGraphicsContext2D().setFill(Color.BLACK);
        canvas.getGraphicsContext2D().fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        root.getChildren().add(canvas);

        try {
            initialize(canvas);
        } catch (Exception e) {
            System.out.println("Couldn't initialize emulator");
            e.printStackTrace();
            showAlert("Couldn't initialize emulator\n" + e.getClass(), e, Optional.empty(), Optional.empty());
        }

        primaryStage.addEventHandler(KeyEvent.KEY_PRESSED, event -> cpu.keyPressed(event.getCode()));
        primaryStage.addEventHandler(KeyEvent.KEY_RELEASED, event -> cpu.keyReleased(event.getCode()));

        primaryStage.show();
        run();
    }

    private void run() {
        Timeline gameLoop = new Timeline();
        gameLoop.setCycleCount(Timeline.INDEFINITE);

        KeyFrame kf = new KeyFrame(
                Duration.seconds(0.017),
                actionEvent -> {
                    try {
                        cpu.executeCycle();
                    } catch (RuntimeException e) {
                        gameLoop.stop();
                        System.out.println("Execution halted unexpectedly");
                        e.printStackTrace();
                        showAlert("Execution halted unexpectedly", e, Optional.of(cpu.getPC()), Optional.of(cpu.getCycleNumber()));
                    }
                    if (cpu.isDrawFlag()) {
                        renderer.updateCanvas();
                        cpu.setDrawFlag(false);
                    }
                });

        gameLoop.getKeyFrames().add(kf);
        System.out.println("\nExecution started:\n------------------");
        gameLoop.play();
    }

    private void initialize(Canvas canvas) throws Exception {
        byte[] romData = RomUtil.LoadRom("games/PONG");

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

    private void showAlert(String message, Exception e, Optional<Short> PC, Optional<Long> cycleNumber) {
        Alert alert;
        if (PC.isPresent() && cycleNumber.isPresent()) {
            alert = new Alert(Alert.AlertType.ERROR, message + "\n" + e.getMessage() +
                    "\nPC: " + String.format("%04X", PC.get()) +
                    "\nCycle number: " + cycleNumber.get());
        } else {
            alert = new Alert(Alert.AlertType.ERROR, message + "\n" + e.getMessage());
        }

        alert.resultProperty().addListener((observable, oldValue, newValue) -> {
            Platform.exit();
        });

        alert.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
