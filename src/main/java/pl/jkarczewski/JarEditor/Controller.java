package pl.jkarczewski.JarEditor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Controller implements Initializable {
    @FXML
    private Button jarChoseButton;
    @FXML
    private Button jarChoseConfirm;
    @FXML
    private TextField jarChoseField;

    @FXML
    private ListView<String> packagesListContainer;
    private ObservableList<String> packagesList;

    @FXML
    private ListView<String> methodListContainer;
    private ObservableList<String> methodsList;

    @FXML
    private Label logLabel;

    private FileChooser fileChooser;
    private File selectedFile;
    private ClassPool pool;
    private URLClassLoader classLoader;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        fileChooser = new FileChooser();

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JAR File", "*.jar"),
                new FileChooser.ExtensionFilter("All", "*")
        );

        packagesList = FXCollections.observableArrayList();
        packagesListContainer.setItems(packagesList);

        methodsList = FXCollections.observableArrayList();
        methodListContainer.setItems(methodsList);

        jarChoseButton.setOnAction(actionEvent -> {
            Node source = (Node) actionEvent.getSource();
            selectedFile = fileChooser.showOpenDialog(source.getScene().getWindow());

            if (selectedFile == null)
                return;

            jarChoseField.setText(selectedFile.getAbsolutePath());
        });

        jarChoseConfirm.setOnAction(actionEvent -> {
            selectedFile = new File(jarChoseField.getText());

            pool = ClassPool.getDefault();

            try {
                classLoader = new URLClassLoader(new URL[]{selectedFile.toURI().toURL()});
                pool.insertClassPath(selectedFile.getAbsolutePath());

                JarFile jarFile = new JarFile(selectedFile.getAbsoluteFile());

                Iterator<JarEntry> it = jarFile.entries().asIterator();

                while (it.hasNext()) {
                    JarEntry jarEntry = it.next();
                    String str = jarEntry.getRealName();

                    // Check only .class files
                    if (str.endsWith(".class")) {
                        str = str.replace("/", ".");
                        str = str.replace(".class", "");
                        packagesList.add(str);
                        Class<?> c = classLoader.loadClass(str);
                        System.err.println(Arrays.toString(c.getDeclaredMethods()));
                    }
                }
            } catch (NotFoundException | IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });

        packagesListContainer.getSelectionModel().selectedItemProperty().addListener(
                (observableValue, oldVal, newVal) -> {
                    try {
                        CtClass ctClass = pool.get(newVal);
                        methodsList.clear();

                        for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
                            methodsList.add(ctMethod.getName());
                        }
                    } catch (NotFoundException e) {
                        e.printStackTrace();
                    }
                });
    }

    private void logMessage(String message) {
        logLabel.setText(message);
    }
}
