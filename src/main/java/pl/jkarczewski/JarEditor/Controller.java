package pl.jkarczewski.JarEditor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javassist.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
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
    private ListView<ConstructorWrapper> constructorsListContainer;
    private ObservableList<ConstructorWrapper> contructorsList;

    @FXML
    private TextArea newMethodTextArea;
    @FXML
    private Button newMethodButton;

    @FXML
    private Button generateClassButton;
    @FXML
    private Button invokeMethodButton;

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
        newMethodButton.setDisable(true);
        invokeMethodButton.setDisable(true);
        generateClassButton.setDisable(true);

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
                logMessage(e.getMessage());
            }
        });

        classesListContainer.getSelectionModel().selectedItemProperty().addListener(
                (observableValue, oldVal, newVal) -> {
                    refreshUILists(newVal);
                    refreshButtons();
                }
        );

        methodsListContainer.getSelectionModel().selectedItemProperty().addListener(
                ((observable, oldValue, newValue) -> refreshButtons())
        );

        constructorsListContainer.getSelectionModel().selectedItemProperty().addListener(
                ((observable, oldValue, newValue) -> refreshButtons())
        );

        newMethodButton.setOnAction(actionEvent -> {
            String newMethodText = newMethodTextArea.getText();

            ClassWrapper classWrapper = classesListContainer.getSelectionModel().getSelectedItem();
            CtClass ctClass = classWrapper.getCtClass();

            try {
                CtMethod ctMethod = CtNewMethod.make(newMethodText, ctClass);

//                CtConstructor ctConstructor = CtNewConstructor.make("public " + ctClass.getSimpleName() + "() {}", ctClass);

//                ctClass.addConstructor(ctConstructor);

                ctClass.addMethod(ctMethod);

//                Class<?> c = ctClass.toClass();
//
//                ctClass.defrost();
//
//                Object o = c.getDeclaredConstructor(new Class[]{int.class, int.class}).newInstance("4", 4);
//
//                Object a = c.getMethod("dodawanie").invoke(o);
//
//                System.err.println(a);


            } catch (Exception e) {
                e.printStackTrace();
                logMessage(e.toString());
            }

            refreshUILists(classWrapper);

        });

        generateClassButton.setOnAction(actionEvent -> {
            ClassWrapper classWrapper = classesListContainer.getSelectionModel().getSelectedItem();
            try {
                classWrapper.generateClass();
            } catch (CannotCompileException e) {
                e.printStackTrace();
                logMessage(e.toString());
            }

            refreshButtons();
        });

        invokeMethodButton.setOnAction(actionEvent -> {

            CtClass ctClass = classesListContainer.getSelectionModel().getSelectedItem().getCtClass();
            CtConstructor ctConstructor = constructorsListContainer.getSelectionModel().getSelectedItem().getCtConstructor();
            CtMethod ctMethod = methodsListContainer.getSelectionModel().getSelectedItem().getCtMethod();

            try {
                Class<?> c = ctClass.getClass();

                System.err.println(Arrays.toString(ctConstructor.getParameterTypes()));
                System.err.println(Arrays.toString(ctMethod.getParameterTypes()));
                System.err.println(ctMethod.getReturnType());

                List<Class<?>> argsConstructor = new LinkedList<>();
                List<Class<?>> argsMethod = new LinkedList<>();

                if (ctConstructor.getParameterTypes().length > 0) {
                    argsConstructor = openArgumentsWindow("Podaj argumenty konstruktora: ", ctConstructor.getParameterTypes());
                }

                if (ctMethod.getParameterTypes().length > 0) {
                    argsMethod = openArgumentsWindow("Podaj argumenty metody: ", ctMethod.getParameterTypes());
                }

            } catch (NotFoundException e) {
                e.printStackTrace();
                logMessage(e.getMessage());
            }
        });
    }

    private void refreshButtons() {
        ClassWrapper selectedClass = classesListContainer.getSelectionModel().getSelectedItem();

        if (selectedClass == null) {
            newMethodButton.setDisable(true);
            generateClassButton.setDisable(true);
            invokeMethodButton.setDisable(true);

            return;
        }

        if (selectedClass.getIsGenerated()) {
            newMethodButton.setDisable(true);
            generateClassButton.setDisable(true);

            invokeMethodButton.setDisable(methodsListContainer.getSelectionModel().getSelectedItem() == null ||
                    constructorsListContainer.getSelectionModel().getSelectedItem() == null);
        } else {
            newMethodButton.setDisable(false);
            generateClassButton.setDisable(false);
            invokeMethodButton.setDisable(true);
        }
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
        constructorsListContainer.setItems(contructorsList);
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

    private List<Class<?>> openArgumentsWindow(String title, CtClass[] ctClasses) {
        VBox vBox = new VBox();

        vBox.getChildren().add(new Label(title));

        List<TextField> inputsList = new LinkedList<>();

        for (CtClass ctClass : ctClasses) {

            TextField textField = new TextField();

            inputsList.add(textField);

            vBox.getChildren().addAll(new Label(ctClass.getName()), textField);
        }

        Button button = new Button("Wykonaj");

        vBox.getChildren().add(button);

        Scene scene = new Scene(vBox);
        Stage dialog = new Stage();

        button.setOnAction(actionEvent -> {
            dialog.close();
        });

        dialog.initOwner(invokeMethodButton.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(title);
        dialog.setScene(scene);
        dialog.showAndWait();

        return null;
    }
}
