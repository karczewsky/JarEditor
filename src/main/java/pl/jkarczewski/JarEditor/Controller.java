package pl.jkarczewski.JarEditor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javassist.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
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
    private ListView<ClassWrapper> classesListContainer;
    private ObservableList<ClassWrapper> classesList;

    @FXML
    private ListView<MethodWrapper> methodsListContainer;
    private ObservableList<MethodWrapper> methodsList;

    @FXML
    private ListView<ConstructorWrapper> constructorListContainer;
    private ObservableList<ConstructorWrapper> contructorsList;

    @FXML
    private TextArea newMethodTextArea;
    @FXML
    private Button newMethodButton;

    @FXML
    private Label logLabel;

    private FileChooser fileChooser;
    private File selectedFile;
    private ClassPool classPool;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // DEVELOPMENT CHANGES
        jarChoseField.setText(getClass().getResource("/kalkulator.jar").getFile());
        newMethodTextArea.setText("public void test() { System.out.println(\"New method printing stuff\"); }");
        // END OF DEVELOPMENT CHANGES

        initUIElements();

        jarChoseButton.setOnAction(actionEvent -> {
            Node source = (Node) actionEvent.getSource();
            selectedFile = fileChooser.showOpenDialog(source.getScene().getWindow());

            if (selectedFile == null)
                return;

            jarChoseField.setText(selectedFile.getAbsolutePath());
        });

        jarChoseConfirm.setOnAction(actionEvent -> {
            String filePathString = jarChoseField.getText().trim();

            if (filePathString.length() == 0)
                return;

            selectedFile = new File(filePathString);

            classPool = ClassPool.getDefault();

            try {
                classPool.insertClassPath(selectedFile.getAbsolutePath());

                JarFile jarFile = new JarFile(selectedFile.getAbsoluteFile());

                Iterator<JarEntry> it = jarFile.entries().asIterator();

                classesList.clear();
                while (it.hasNext()) {
                    JarEntry jarEntry = it.next();
                    String str = jarEntry.getRealName();

                    // Check only .class files
                    if (str.endsWith(".class")) {
                        str = str.replace("/", ".");
                        str = str.replace(".class", "");

                        ClassWrapper classWrapper = new ClassWrapper(classPool, str);
                        classesList.add(classWrapper);
                    }
                }
            } catch (NotFoundException | IOException e) {
                e.printStackTrace();
            }
        });

        classesListContainer.getSelectionModel().selectedItemProperty().addListener(
                (observableValue, oldVal, newVal) -> refreshUILists(newVal)
        );

        newMethodButton.setOnAction(actionEvent -> {
            String newMethodText = newMethodTextArea.getText();

            ClassWrapper classWrapper = classesListContainer.getSelectionModel().getSelectedItem();
            CtClass ctClass = classWrapper.getCtClass();

            try {
                CtMethod ctMethod = CtNewMethod.make(newMethodText, ctClass);

                CtConstructor ctConstructor = CtNewConstructor.make("public " + ctClass.getSimpleName() + "() {}", ctClass);

                ctClass.addConstructor(ctConstructor);

                ctClass.addMethod(ctMethod);

                Class<?> c = ctClass.toClass();

                ctClass.defrost();

                Object o = c.getDeclaredConstructor(new Class[]{int.class, int.class}).newInstance(4, 4);

                Object a = c.getMethod("dodawanie").invoke(o);

                System.err.println(a);

            } catch (Exception e) {
                logMessage(e.toString());
            }

            refreshUILists(classWrapper);
        });
    }

    private void initUIElements() {
        fileChooser = new FileChooser();

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JAR File", "*.jar"),
                new FileChooser.ExtensionFilter("All", "*")
        );

        classesList = FXCollections.observableArrayList();
        classesListContainer.setItems(classesList);

        methodsList = FXCollections.observableArrayList();
        methodsListContainer.setItems(methodsList);

        contructorsList = FXCollections.observableArrayList();
        constructorListContainer.setItems(contructorsList);
    }

    private void logMessage(String message) {
        logLabel.setText(message);
    }

    private void refreshUILists(ClassWrapper classWrapper) {
        methodsList.clear();
        methodsList.setAll(classWrapper.getMethods());

        contructorsList.clear();
        contructorsList.setAll(classWrapper.getConstructors());
    }
}
