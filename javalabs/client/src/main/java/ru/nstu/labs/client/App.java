package ru.nstu.labs.client;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ru.nstu.labs.client.dialog.BatchEditDialog;
import ru.nstu.labs.core.csv.CsvBatchRepository;
import ru.nstu.labs.core.exception.CsvParseException;
import ru.nstu.labs.core.model.ArchivedBatch;
import ru.nstu.labs.core.model.Batch;
import ru.nstu.labs.core.model.ImportedBatch;

public class App extends Application {

  private final ObservableList<Batch> masterData = FXCollections.observableArrayList();
  private final TableView<Batch> tableView = new TableView<>();
  private final CsvBatchRepository repository = new CsvBatchRepository();

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) {
    stage.setTitle("Склад партий (Лабораторная работа №1)");

    initTable();

    Button btnAdd = new Button("Добавить партию");
    Button btnEdit = new Button("Изменить");
    Button btnLoad = new Button("Загрузить CSV");
    Button btnSave = new Button("Экспорт в CSV");

    btnAdd.getStyleClass().add("btn-citrus");
    btnEdit.getStyleClass().add("btn-amber");

    btnEdit.setDisable(true);

    tableView
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, oldSel, newSel) -> {
              if (newSel == null) {
                btnEdit.setDisable(true);
              } else {
                btnEdit.setDisable(newSel instanceof ArchivedBatch);
              }
            });

    btnAdd.setOnAction(
        e -> {
          BatchEditDialog dialog = new BatchEditDialog(null);
          dialog
              .showAndWait()
              .ifPresent(
                  batch -> {
                    masterData.remove(batch);
                    masterData.add(batch);
                  });
        });

    btnEdit.setOnAction(
        e -> {
          Batch selected = tableView.getSelectionModel().getSelectedItem();
          if (selected != null && !(selected instanceof ArchivedBatch)) {
            BatchEditDialog dialog = new BatchEditDialog(selected);
            dialog
                .showAndWait()
                .ifPresent(
                    updated -> {
                      int index = masterData.indexOf(selected);
                      if (index >= 0) {
                        masterData.set(index, updated);
                      }
                    });
          }
        });

    btnLoad.setOnAction(
        e -> {
          FileChooser fc = new FileChooser();
          fc.setTitle("Загрузить партии из CSV");
          fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV файлы", "*.csv"));
          File file = fc.showOpenDialog(stage);
          if (file != null) {
            try {
              CsvBatchRepository.LoadResult result = repository.load(file.toPath());
              masterData.setAll(result.items());

              if (!result.errors().isEmpty()) {
                showErrorSummaryDialog(result.errors());
              }
            } catch (IOException ex) {
              showSimpleError(
                  "Ошибка ввода-вывода", "Не удалось прочитать файл: " + ex.getMessage());
            }
          }
        });

    btnSave.setOnAction(
        e -> {
          FileChooser fc = new FileChooser();
          fc.setTitle("Сохранить партии в CSV");
          fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV файлы", "*.csv"));
          File file = fc.showSaveDialog(stage);
          if (file != null) {
            try {
              repository.save(file.toPath(), masterData);
            } catch (IOException ex) {
              showSimpleError(
                  "Ошибка ввода-вывода", "Не удалось сохранить файл: " + ex.getMessage());
            }
          }
        });

    HBox controls = new HBox(12, btnAdd, btnEdit, btnLoad, btnSave);
    controls.getStyleClass().add("bottom-bar");

    BorderPane root = new BorderPane();
    root.setStyle("-fx-background-color: #121214;");
    root.setCenter(tableView);
    root.setBottom(controls);
    root.setPadding(new Insets(14));

    Scene scene = new Scene(root, 1050, 580);
    scene.setFill(Color.valueOf("#121214"));

    var stylesheet = App.class.getResource("style.css");
    if (stylesheet != null) {
      scene.getStylesheets().add(stylesheet.toExternalForm());
    }

    stage.setScene(scene);
    stage.show();
  }

  private void initTable() {
    tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

    TableColumn<Batch, Batch> typeCol = new TableColumn<>("Тип");
    typeCol.setMinWidth(110);
    typeCol.setMaxWidth(130);
    typeCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
    typeCol.setCellFactory(
        col ->
            new TableCell<>() {
              private final Label badge = new Label();

              @Override
              protected void updateItem(Batch batch, boolean empty) {
                super.updateItem(batch, empty);
                if (empty || batch == null) {
                  setGraphic(null);
                } else {
                  badge.getStyleClass().setAll("badge");
                  if (batch instanceof ArchivedBatch) {
                    badge.setText("АРХИВ");
                    badge.getStyleClass().add("badge-archived");
                  } else if (batch instanceof ImportedBatch) {
                    badge.setText("ИМПОРТ");
                    badge.getStyleClass().add("badge-imported");
                  } else {
                    badge.setText("БАЗОВАЯ");
                    badge.getStyleClass().add("badge-base");
                  }
                  setGraphic(badge);
                }
              }
            });

    TableColumn<Batch, String> skuCol = new TableColumn<>("Артикул");
    skuCol.setMinWidth(110);
    skuCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSku()));

    TableColumn<Batch, String> nameCol = new TableColumn<>("Название");
    nameCol.setMinWidth(180);
    nameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));

    TableColumn<Batch, Number> qtyCol = new TableColumn<>("Количество");
    qtyCol.setMinWidth(100);
    qtyCol.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQuantity()));

    TableColumn<Batch, String> cellCol = new TableColumn<>("Ячейка");
    cellCol.setMinWidth(90);
    cellCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCell()));

    TableColumn<Batch, LocalDate> dateCol = new TableColumn<>("Дата завоза");
    dateCol.setMinWidth(110);
    dateCol.setCellValueFactory(
        cell -> new SimpleObjectProperty<>(cell.getValue().getDeliveryDate()));

    TableColumn<Batch, String> countryCol = new TableColumn<>("Страна");
    countryCol.setMinWidth(110);
    countryCol.setCellValueFactory(
        cell -> {
          if (cell.getValue() instanceof ImportedBatch imp) {
            return new SimpleStringProperty(imp.getCountry());
          }
          return new SimpleStringProperty("-");
        });

    TableColumn<Batch, String> customsCol = new TableColumn<>("Таможенный код");
    customsCol.setMinWidth(130);
    customsCol.setCellValueFactory(
        cell -> {
          if (cell.getValue() instanceof ImportedBatch imp) {
            return new SimpleStringProperty(imp.getCustomsCode());
          }
          return new SimpleStringProperty("-");
        });

    tableView
        .getColumns()
        .addAll(typeCol, skuCol, nameCol, qtyCol, cellCol, dateCol, countryCol, customsCol);
    tableView.setItems(masterData);
  }

  private void showErrorSummaryDialog(List<CsvParseException> errors) {
    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle("Предупреждение при загрузке");
    alert.setHeaderText("Некоторые строки были пропущены (" + errors.size() + " шт.)");

    StringBuilder sb = new StringBuilder();
    for (CsvParseException ex : errors) {
      sb.append(ex.getMessage())
          .append("\n Исходная строка: ")
          .append(ex.getRawLine())
          .append("\n\n");
    }

    TextArea textArea = new TextArea(sb.toString());
    textArea.setEditable(false);
    textArea.setWrapText(true);
    textArea.setMaxWidth(Double.MAX_VALUE);
    textArea.setMaxHeight(Double.MAX_VALUE);

    alert.getDialogPane().setExpandableContent(textArea);
    alert.getDialogPane().setExpanded(true);
    alert.showAndWait();
  }

  private void showSimpleError(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }
}
