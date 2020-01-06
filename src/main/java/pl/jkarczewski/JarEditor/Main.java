package pl.jkarczewski.JarEditor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/app.fxml"));
        stage.setTitle("JarEditor");
        stage.setScene(new Scene(root));
        stage.setResizable(false);
        stage.show();
    }
}
