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
import javafx.util.Pair;
import javassist.*;
import pl.jkarczewski.JarEditor.helpers.*;

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
    private ListView<ClassWrapper> parentsListContainer;
    private ObservableList<ClassWrapper> parentsList;

    @FXML
    private TextArea newMethodTextArea;
    @FXML
    private Button newMethodButton;
    @FXML
    private Button generateClassesButton;
    @FXML
    private Button invokeMethodButton;
    @FXML
    private Button newEmptyConstructorButton;
    @FXML
    private Button setParentButton;

    @FXML
    private Label logLabel;

    @FXML
    private Label executedMethodName;
    @FXML
    private Label executedMethodMemoryUsed;
    @FXML
    private Label executedMethodReturn;

    private FileChooser fileChooser;
    private File selectedFile;
    private ClassPool classPool;
    private boolean errorFlag = false;
    private boolean classesGenerated = false;
    private Map<ClassWrapper, ClassWrapper> parentsChildrenMap = new HashMap<>();


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // DEVELOPMENT CHANGES
        jarChoseField.setText(getClass().getResource("/kalkulator.jar").getFile());
        newMethodTextArea.setText("public void test() { \n\tSystem.out.println(\"New method printing stuff\");\n }");
        // END OF DEVELOPMENT CHANGES
        refreshButtons();
        initUIElements();
        jarUIState(false);

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
                parentsList.clear();
                while (it.hasNext()) {
                    JarEntry jarEntry = it.next();
                    String str = jarEntry.getRealName();

                    if (str.endsWith(".class")) {
                        str = str.replace("/", ".");
                        str = str.replace(".class", "");

                        ClassWrapper classWrapper = new ClassWrapper(classPool, str);
                        classesList.add(classWrapper);
                        parentsList.add(classWrapper);
                    }
                }

                jarUIState(true);
                refreshButtons();

            } catch (NotFoundException | IOException e) {
                e.printStackTrace();
                logException(e);
            }
        });

        classesListContainer.getSelectionModel().selectedItemProperty().addListener(
                (observableValue, oldVal, newVal) -> {
                    refreshUILists(newVal);
                    refreshButtons();
                }
        );

        methodsListContainer.getSelectionModel().selectedItemProperty().addListener(
                ((observable -> refreshButtons()))
        );

        constructorsListContainer.getSelectionModel().selectedItemProperty().addListener(
                ((observable -> refreshButtons()))
        );

        parentsListContainer.getSelectionModel().selectedItemProperty().addListener(
                ((observable -> refreshButtons()))
        );

        newMethodButton.setOnAction(actionEvent -> {
            String newMethodText = newMethodTextArea.getText();

            ClassWrapper classWrapper = classesListContainer.getSelectionModel().getSelectedItem();
            CtClass ctClass = classWrapper.getCtClass();

            try {
                CtMethod ctMethod = CtNewMethod.make(newMethodText, ctClass);
                ctClass.addMethod(ctMethod);
            } catch (Exception e) {
                e.printStackTrace();
                logException(e);
            }

            refreshUILists(classWrapper);
        });

        newEmptyConstructorButton.setOnAction(actionEvent -> {
            ClassWrapper classWrapper = classesListContainer.getSelectionModel().getSelectedItem();
            CtClass ctClass = classWrapper.getCtClass();

            try {
                CtConstructor ctConstructor = CtNewConstructor.make("public " + ctClass.getSimpleName() + "() {}", ctClass);
                ctClass.addConstructor(ctConstructor);
            } catch (Exception e) {
                e.printStackTrace();
                logException(e);
            }

            refreshUILists(classWrapper);
        });

        generateClassesButton.setOnAction(actionEvent -> {

            for (ClassWrapper classWrapper: classesList) {
                LinkedList<ClassWrapper> listOfDependancy = new LinkedList<>();

                while (parentsChildrenMap.containsKey(classWrapper)) {
                    listOfDependancy.addFirst(classWrapper);
                    classWrapper = parentsChildrenMap.get(classWrapper);
                }
                listOfDependancy.addFirst(classWrapper);

                while (!listOfDependancy.isEmpty()) {
                    try {
                        classWrapper = listOfDependancy.removeFirst();

                        if (!classWrapper.getIsGenerated()) {
                            classWrapper.generateClass();
                        }
                    } catch (CannotCompileException e) {
                        e.printStackTrace();
                        logException(e);
                    }
                }
            }

            classesGenerated = true;
            refreshButtons();
        });

        setParentButton.setOnAction(actionEvent -> {
            ClassWrapper child = classesListContainer.getSelectionModel().getSelectedItem();
            ClassWrapper parent = parentsListContainer.getSelectionModel().getSelectedItem();

            if (child == parent) {
                logMessage("Dziecko nie moze byc swoim rodzicem!");
                return;
            }

            try {
                child.getCtClass().setSuperclass(parent.getCtClass());
                parentsChildrenMap.put(child, parent);
            } catch (CannotCompileException e) {
                e.printStackTrace();
                logException(e);
            }

            logMessage("Ustawiono rodzica: " + parent);
            refreshUILists(child);
        });

        invokeMethodButton.setOnAction(actionEvent -> {

            ClassWrapper classWrapper = classesListContainer.getSelectionModel().getSelectedItem();
            ConstructorWrapper constructorWrapper = constructorsListContainer.getSelectionModel().getSelectedItem();
            MethodWrapper methodWrapper = methodsListContainer.getSelectionModel().getSelectedItem();

            try {
                List<Pair<Class<?>, Object>> argsConstructor = new LinkedList<>();
                List<Pair<Class<?>, Object>> argsMethod = new LinkedList<>();

                errorFlag = false;

                if (constructorWrapper.getCtConstructor().getParameterTypes().length > 0) {
                    argsConstructor = openArgumentsWindow("Podaj argumenty konstruktora: ",
                            constructorWrapper.getCtConstructor().getParameterTypes());
                }

                if (errorFlag)
                    return;

                if (methodWrapper.getCtMethod().getParameterTypes().length > 0) {
                    argsMethod = openArgumentsWindow("Podaj argumenty metody: ",
                            methodWrapper.getCtMethod().getParameterTypes());
                }

                if (errorFlag)
                    return;

                // Wykonaj konstruktor
                Class<?> c = classWrapper.getCl();

                Class<?>[] classes = new Class<?>[argsConstructor.size()];

                for (int i = 0; i < classes.length; i++) {
                    classes[i] = argsConstructor.get(i).getKey();
                }

                Object o = c
                        .getConstructor(classes)
                        .newInstance(argsConstructor.stream().map(Pair::getValue).toArray());

                // Wykonaj metodę
                classes = new Class<?>[argsMethod.size()];

                for (int i = 0; i < classes.length; i++) {
                    classes[i] = argsMethod.get(i).getKey();
                }

                Runtime runtime = Runtime.getRuntime();
                runtime.gc();
                long usedMemoryBefore = runtime.totalMemory() - runtime.freeMemory();

                Object a = c
                        .getMethod(methodWrapper.getCtMethod().getName(), classes)
                        .invoke(o, argsMethod.stream().map(Pair::getValue).toArray());

                long usedMemoryAfter = runtime.totalMemory() - runtime.freeMemory();

                executedMethodName.setText(methodWrapper.toString());
                executedMethodMemoryUsed.setText(usedMemoryAfter - usedMemoryBefore + " Bytes");
                executedMethodReturn.setText(String.valueOf(a));

            } catch (Exception e) {
                e.printStackTrace();
                logException(e);
            }
        });


    }

    private void jarUIState(boolean disable) {
        jarChoseField.setDisable(disable);
        jarChoseButton.setDisable(disable);
        jarChoseConfirm.setDisable(disable);

        generateClassesButton.setDisable(!disable);
    }

    private void refreshButtons() {
        generateClassesButton.setDisable(classesGenerated);

        boolean classNotSelected = classesListContainer.getSelectionModel().getSelectedItem() == null;
        boolean methodNotSelected = methodsListContainer.getSelectionModel().getSelectedItem() == null;
        boolean constructorNotSelected = constructorsListContainer.getSelectionModel().getSelectedItem() == null;
        boolean parentNotSelected = parentsListContainer.getSelectionModel().getSelectedItems() == null;

        if (classesGenerated) {
            newMethodButton.setDisable(true);
            newEmptyConstructorButton.setDisable(true);
            setParentButton.setDisable(true);
            invokeMethodButton.setDisable(classNotSelected ||  methodNotSelected || constructorNotSelected);

        } else {
            newMethodButton.setDisable(classNotSelected);
            newEmptyConstructorButton.setDisable(classNotSelected);
            setParentButton.setDisable(classNotSelected || parentNotSelected);
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

        parentsList = FXCollections.observableArrayList();
        parentsListContainer.setItems(parentsList);
    }

    private void logMessage(String message) {
        logLabel.setText(message);
    }

    private void logException(Exception ex) {
        logMessage(ex.getClass().getName() + ": " + ex.getMessage());
    }

    private void refreshUILists(ClassWrapper classWrapper) {
        methodsList.clear();
        methodsList.setAll(classWrapper.getMethods());

        contructorsList.clear();
        contructorsList.setAll(classWrapper.getConstructors());
    }

    private List<Pair<Class<?>, Object>> openArgumentsWindow(String title, CtClass[] argsClasses) {

        List<TextField> inputsList = new LinkedList<>();
        List<String> inputsTypes = new LinkedList<>();

        VBox vBox = new VBox();

        vBox.getChildren().add(new Label(title));

        for (CtClass ctClass : argsClasses) {
            TextField textField = new TextField();
            vBox.getChildren().addAll(new Label(ctClass.getName()), textField);

            inputsList.add(textField);
            inputsTypes.add(ctClass.getName());
        }

        Button button = new Button("Wykonaj");

        vBox.getChildren().add(button);

        Scene scene = new Scene(vBox);
        Stage dialog = new Stage();

        List<Pair<Class<?>, Object>> argsList = new LinkedList<>();

        button.setOnAction(actionEvent -> {
            for (int i = 0; i < argsClasses.length; i++) {
                String inputText = inputsList.get(i).getText().trim();
                String argType = inputsTypes.get(i);

                try {
                    argsList.add(PrimitivesHelper.translateNameToPair(argType, inputText));
                } catch (Exception e) {
                    dialog.close();
                    logException(e);
                    e.printStackTrace();
                    errorFlag = true;
                }
            }

            dialog.close();
        });

        dialog.initOwner(invokeMethodButton.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(title);
        dialog.setScene(scene);
        dialog.showAndWait();

        return argsList;
    }
}
