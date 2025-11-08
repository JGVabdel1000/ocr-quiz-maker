package be.esi.prj;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class App extends Application{
    @Override
    public void start(Stage stage) throws Exception {
        URL resource = App.class.getResource("fxml/login.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader(resource);
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        stage.setTitle("Login");
        stage.setScene(scene);
        stage.show();
    }
}
