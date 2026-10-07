package ru.nstu.labs.client;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ru.nstu.labs.client.async.GenerateDataTask;
import ru.nstu.labs.client.async.LoadCsvTask;
import ru.nstu.labs.client.dialog.BatchEditDialog;
import ru.nstu.labs.client.view.HashTableCanvas;
import ru.nstu.labs.core.benchmark.BenchmarkService;
import ru.nstu.labs.core.csv.CsvBatchRepository;
import ru.nstu.labs.core.exception.CsvParseException;
import ru.nstu.labs.core.model.ArchivedBatch;
import ru.nstu.labs.core.model.Batch;
import ru.nstu.labs.core.model.ImportedBatch;
import ru.nstu.labs.core.structure.BatchStorage;
import ru.nstu.labs.core.structure.HashTableBatchStorage;
import ru.nstu.labs.core.structure.OpenAddressingHashTable.NodeState;
import ru.nstu.labs.core.structure.TableResult;

public class App extends Application {

  private final BatchStorage storage = new HashTableBatchStorage(16, 0.75);
  private final ObservableList<Batch> masterData = FXCollections.observableArrayList();
  private final TableView<Batch> tableView = new TableView<>();
  private final HashTableCanvas canvas = new HashTableCanvas();
  private final CsvBatchRepository repository = new CsvBatchRepository();

  private final ProgressBar progressBar = new ProgressBar(0);
  private final Label statusLabel = new Label("Готов к работе");
  private final Button btnCancel = new Button("Отмена");
  private final Label lblLoadFactor = new Label("Заполнение: 0.0%");

  private final Label inspHeader = new Label("ИНСПЕКТОР ЯЧЕЙКИ");
  private final Label inspMath = new Label("Кликните по любой ячейке холста для анализа коллизий.");
  private final Label inspDetails = new Label("");

  private Task<?> activeTask;

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) {
    stage.setTitle("Склад партий (Лабораторная работа №2 — Хеш-таблица)");

    initTable();
    initCanvas();

    TextField tfSkuSearch = new TextField();
    tfSkuSearch.setPromptText("Артикул (SKU)...");
    tfSkuSearch.setPrefWidth(140);
    tfSkuSearch.getStyleClass().add("dark-input");

    Button btnFind = new Button("Найти");
    Button btnDeleteSku = new Button("Удалить");
    Button btnGenerate = new Button("Сгенерировать");
    Button btnBenchmark = new Button("Бенчмарк и Data Race");

    progressBar.setVisible(false);
    btnCancel.setVisible(false);
    btnCancel.setOnAction(
        e -> {
          if (activeTask != null && activeTask.isRunning()) {
            activeTask.cancel();
          }
        });

    Label lblSearch = new Label("Поиск:");
    lblSearch.setStyle("-fx-text-fill: #a1a1aa; -fx-font-weight: bold;");
    statusLabel.setStyle("-fx-text-fill: #38bdf8;");

    HBox topBar =
        new HBox(
            8,
            lblSearch,
            tfSkuSearch,
            btnFind,
            btnDeleteSku,
            btnGenerate,
            btnBenchmark,
            progressBar,
            btnCancel,
            statusLabel);
    topBar.setAlignment(Pos.CENTER_LEFT);
    topBar.setPadding(new Insets(10, 14, 10, 14));
    topBar.setStyle(
        "-fx-background-color: #16161a; -fx-border-color: #27272a; -fx-border-width: 0 0 1 0;");

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
              btnEdit.setDisable(newSel == null || newSel instanceof ArchivedBatch);
            });

    btnFind.setOnAction(
        e -> {
          String sku = tfSkuSearch.getText().trim();
          if (sku.isEmpty()) return;

          TableResult<Batch> res = storage.get(sku);
          canvas.render(
              storage.getSnapshots(), res.visitedIndices(), res.probesCount(), "Поиск: " + sku);

          if (res.found()) {
            tableView.getSelectionModel().select(res.value());
            tableView.scrollTo(res.value());
            statusLabel.setText(String.format("Найден '%s' за %d проб", sku, res.probesCount()));
            updateInspectorFound(res.value(), res);
          } else {
            statusLabel.setText(
                String.format("Артикул '%s' не найден (%d проб)", sku, res.probesCount()));
            inspHeader.setText("АРТИКУЛ НЕ НАЙДЕН");
            inspHeader.setStyle(
                "-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 11px;");
            inspMath.setText(
                "Проверено шагов: " + res.probesCount() + " до первой свободной ячейки.");
            inspDetails.setText("Элемент отсутствует в текущем массиве.");
          }
        });

    btnDeleteSku.setOnAction(
        e -> {
          String sku = tfSkuSearch.getText().trim();
          if (sku.isEmpty()) {
            Batch sel = tableView.getSelectionModel().getSelectedItem();
            if (sel != null) sku = sel.getSku();
          }
          if (sku == null || sku.isEmpty()) return;

          TableResult<Batch> res = storage.remove(sku);
          if (res.found()) {
            syncTableFromStorage();
            updateLoadFactorUi();
            canvas.render(
                storage.getSnapshots(),
                res.visitedIndices(),
                res.probesCount(),
                "Удаление: " + sku);
            statusLabel.setText(String.format("Удален '%s' (проб: %d)", sku, res.probesCount()));
            inspHeader.setText("ЭЛЕМЕНТ УДАЛЕН: [" + sku + "]");
            inspHeader.setStyle(
                "-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 11px;");
            inspMath.setText("Ячейка переведена в статус [DELETED] (tombstone).");
            inspDetails.setText("Цепочки пробирования для других элементов сохранены.");
          } else {
            canvas.render(
                storage.getSnapshots(),
                res.visitedIndices(),
                res.probesCount(),
                "Не найден: " + sku);
            statusLabel.setText("Элемент для удаления не найден");
          }
        });

    btnAdd.setOnAction(
        e -> {
          BatchEditDialog dialog = new BatchEditDialog(null);
          dialog
              .showAndWait()
              .ifPresent(
                  batch -> {
                    TableResult<Batch> res = storage.put(batch);
                    syncTableFromStorage();
                    updateLoadFactorUi();
                    canvas.render(
                        storage.getSnapshots(),
                        res.visitedIndices(),
                        res.probesCount(),
                        "Вставка: " + batch.getSku());
                    statusLabel.setText(
                        String.format(
                            "Добавлен '%s' (%d проб)", batch.getSku(), res.probesCount()));
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
                      TableResult<Batch> res = storage.put(updated);
                      syncTableFromStorage();
                      canvas.render(
                          storage.getSnapshots(),
                          res.visitedIndices(),
                          res.probesCount(),
                          "Обновление: " + updated.getSku());
                    });
          }
        });

    btnGenerate.setOnAction(
        e -> {
          TextInputDialog input = new TextInputDialog("30");
          input.setTitle("Генерация данных");
          input.setHeaderText("Асинхронная генерация набора данных");
          input.setContentText("Введите количество записей:");

          Optional<String> res = input.showAndWait();
          res.ifPresent(
              str -> {
                try {
                  int count = Integer.parseInt(str.trim());
                  if (count <= 0) return;

                  GenerateDataTask task = new GenerateDataTask(count, storage);
                  activeTask = task;
                  bindTaskUi(task, btnGenerate);

                  task.setOnSucceeded(
                      ev -> {
                        unbindTaskUi(btnGenerate);
                        syncTableFromStorage();
                        updateLoadFactorUi();
                        canvas.render(
                            storage.getSnapshots(), new int[0], 0, "Сгенерировано: " + count);
                      });

                  task.setOnCancelled(
                      ev -> {
                        unbindTaskUi(btnGenerate);
                        syncTableFromStorage();
                        updateLoadFactorUi();
                        canvas.render(storage.getSnapshots(), new int[0], 0, "Генерация отменена");
                        statusLabel.setText("Генерация прервана пользователем");
                      });

                  task.setOnFailed(
                      ev -> {
                        unbindTaskUi(btnGenerate);
                        statusLabel.setText(
                            "Ошибка генерации: " + task.getException().getMessage());
                      });

                  Thread thread = new Thread(task);
                  thread.setDaemon(true);
                  thread.start();

                } catch (NumberFormatException ignored) {
                }
              });
        });

    btnBenchmark.setOnAction(
        e -> {
          Task<String> benchTask =
              new Task<>() {
                @Override
                protected String call() {
                  updateMessage("Выполняются замеры 10^4 / 10^5 и тест Data Race...");
                  updateProgress(-1, 1);
                  return new BenchmarkService().runFullReport();
                }
              };

          activeTask = benchTask;
          bindTaskUi(benchTask, btnBenchmark);

          benchTask.setOnSucceeded(
              ev -> {
                unbindTaskUi(btnBenchmark);
                statusLabel.setText("Замеры успешно завершены");
                showBenchmarkResultDialog(benchTask.getValue());
              });

          benchTask.setOnFailed(
              ev -> {
                unbindTaskUi(btnBenchmark);
                statusLabel.setText("Ошибка бенчмарка: " + benchTask.getException().getMessage());
              });

          Thread th = new Thread(benchTask);
          th.setDaemon(true);
          th.start();
        });

    btnLoad.setOnAction(
        e -> {
          FileChooser fc = new FileChooser();
          fc.setTitle("Загрузить партии из CSV");
          fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV файлы", "*.csv"));
          File file = fc.showOpenDialog(stage);
          if (file != null) {
            LoadCsvTask loadTask = new LoadCsvTask(file, repository);
            activeTask = loadTask;
            bindTaskUi(loadTask, btnLoad);

            loadTask.setOnSucceeded(
                ev -> {
                  unbindTaskUi(btnLoad);
                  CsvBatchRepository.LoadResult result = loadTask.getValue();
                  storage.clear();
                  for (Batch b : result.items()) {
                    storage.put(b);
                  }
                  syncTableFromStorage();
                  updateLoadFactorUi();
                  canvas.render(
                      storage.getSnapshots(),
                      new int[0],
                      0,
                      "Загружен CSV (" + result.items().size() + " шт.)");

                  if (!result.errors().isEmpty()) {
                    showErrorSummaryDialog(result.errors());
                  }
                });

            loadTask.setOnCancelled(
                ev -> {
                  unbindTaskUi(btnLoad);
                  statusLabel.setText("Загрузка CSV отменена пользователем");
                });

            loadTask.setOnFailed(
                ev -> {
                  unbindTaskUi(btnLoad);
                  showSimpleError(
                      "Ошибка загрузки",
                      "Не удалось прочитать файл: " + loadTask.getException().getMessage());
                });

            Thread thread = new Thread(loadTask);
            thread.setDaemon(true);
            thread.start();
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

    HBox controls = new HBox(14, btnAdd, btnEdit, btnLoad, btnSave);
    controls.getStyleClass().add("bottom-bar");
    controls.setAlignment(Pos.CENTER_LEFT);
    controls.setMinHeight(60);

    ScrollPane canvasScroll = new ScrollPane(canvas);
    canvasScroll.setFitToWidth(true);
    canvasScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    canvasScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

    canvas.setOnScrollRequested(canvasScroll::setVvalue);

    canvasScroll
        .viewportBoundsProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              if (newVal.getWidth() > 50) {
                canvas.setWidth(newVal.getWidth());
                canvas.renderInstant();
              }
            });

    Label canvasHeader = new Label("Хеш-таблица (Canvas)");
    canvasHeader.setStyle("-fx-text-fill: #a1a1aa; -fx-font-weight: bold;");

    lblLoadFactor.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");

    HBox canvasHud = new HBox(12, canvasHeader, lblLoadFactor);
    canvasHud.setAlignment(Pos.CENTER_LEFT);
    canvasHud.setPadding(new Insets(0, 0, 6, 0));

    inspHeader.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 11px;");
    inspMath.setStyle("-fx-text-fill: #fbbf24; -fx-font-family: monospace; -fx-font-size: 11px;");
    inspMath.setWrapText(true);
    inspDetails.setStyle(
        "-fx-text-fill: #d6d3d1; -fx-font-family: monospace; -fx-font-size: 11px;");
    inspDetails.setWrapText(true);

    VBox inspectorCard = new VBox(4, inspHeader, inspMath, inspDetails);
    inspectorCard.setPadding(new Insets(10, 14, 10, 14));
    inspectorCard.setMinHeight(75);
    inspectorCard.setStyle(
        "-fx-background-color: #1c1917; -fx-border-color: #443528; -fx-border-radius: 6; -fx-background-radius: 6;");

    canvas.setOnCellSelected(
        snapshot -> {
          if (snapshot.state() == NodeState.EMPTY) {
            inspHeader.setText(String.format("ЯЧЕЙКА [%d]: СВОБОДНА (EMPTY)", snapshot.index()));
            inspHeader.setStyle(
                "-fx-text-fill: #78716c; -fx-font-weight: bold; -fx-font-size: 11px;");
            inspMath.setText("Ключ отсутствует, цепочка поиска прерывается здесь.");
            inspDetails.setText("Статус ячейки: не занята.");
          } else if (snapshot.state() == NodeState.DELETED) {
            inspHeader.setText(String.format("ЯЧЕЙКА [%d]: УДАЛЕНА (DELETED)", snapshot.index()));
            inspHeader.setStyle(
                "-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 11px;");
            inspMath.setText("Tombstone: элемент удален, но поиск продолжается дальше.");
            inspDetails.setText("Не разрывает цепочку линейного пробирования.");
          } else {
            String key = snapshot.key();
            int cap = storage.capacity();
            int idealIdx = (key.hashCode() & 0x7fffffff) % cap;
            int offset = (snapshot.index() - idealIdx + cap) % cap;

            inspHeader.setText(String.format("ЯЧЕЙКА [%d]: ЗАНЯТА — %s", snapshot.index(), key));
            inspHeader.setStyle(
                "-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 11px;");

            String colInfo = offset == 0 ? " (БЕЗ КОЛЛИЗИЙ)" : " (КОЛЛИЗИЯ, + " + offset + " шаг.)";
            inspMath.setText(
                String.format("h(k)%%C = %d ➔ смещение: +%d ячеек%s", idealIdx, offset, colInfo));
            inspDetails.setText(
                String.format(
                    "Товар: %s | Кол-во: %d | Ячейка: %s",
                    snapshot.value().getName(),
                    snapshot.value().getQuantity(),
                    snapshot.value().getCell()));
          }
        });

    VBox canvasBox = new VBox(6, canvasHud, canvasScroll, inspectorCard);
    canvasBox.setPadding(new Insets(8));
    canvasBox.setStyle("-fx-background-color: #121214;");
    VBox.setVgrow(canvasScroll, Priority.ALWAYS);

    SplitPane splitPane = new SplitPane(tableView, canvasBox);
    splitPane.setOrientation(Orientation.HORIZONTAL);
    splitPane.setDividerPositions(0.58);

    BorderPane root = new BorderPane();
    root.setStyle("-fx-background-color: #121214;");
    root.setTop(topBar);
    root.setCenter(splitPane);
    root.setBottom(controls);
    root.setPadding(new Insets(6, 10, 6, 10));

    Scene scene = new Scene(root, 1300, 740);
    scene.setFill(Color.valueOf("#121214"));

    var stylesheet = App.class.getResource("style.css");
    if (stylesheet != null) {
      scene.getStylesheets().add(stylesheet.toExternalForm());
    }

    stage.setMinWidth(1150);
    stage.setMinHeight(680);
    stage.setScene(scene);
    stage.show();

    updateLoadFactorUi();
    canvas.render(storage.getSnapshots(), new int[0], 0, "Инициализация");
  }

  private void updateInspectorFound(Batch batch, TableResult<Batch> res) {
    int cap = storage.capacity();
    int idealIdx = (batch.getSku().hashCode() & 0x7fffffff) % cap;
    inspHeader.setText("НАЙДЕН: " + batch.getSku());
    inspHeader.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 11px;");
    inspMath.setText(
        String.format("Идеальный индекс: %d ➔ Найдено за %d проб(ы)", idealIdx, res.probesCount()));
    inspDetails.setText(
        String.format(
            "Товар: %s | Количество: %d | Ячейка: %s",
            batch.getName(), batch.getQuantity(), batch.getCell()));
  }

  private void updateLoadFactorUi() {
    double lf = storage.loadFactor();
    lblLoadFactor.setText(String.format("Заполнение: %.1f%% (Порог 75%%)", lf * 100));
    if (lf >= 0.70) {
      lblLoadFactor.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
    } else if (lf >= 0.50) {
      lblLoadFactor.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
    } else {
      lblLoadFactor.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
    }
  }

  private void bindTaskUi(Task<?> task, Button triggerButton) {
    progressBar.progressProperty().bind(task.progressProperty());
    statusLabel.textProperty().bind(task.messageProperty());
    progressBar.setVisible(true);
    btnCancel.setVisible(true);
    triggerButton.setDisable(true);
  }

  private void unbindTaskUi(Button triggerButton) {
    progressBar.progressProperty().unbind();
    statusLabel.textProperty().unbind();
    progressBar.setVisible(false);
    btnCancel.setVisible(false);
    triggerButton.setDisable(false);
    activeTask = null;
  }

  private void syncTableFromStorage() {
    masterData.setAll(storage.toArray());
  }

  private void initCanvas() {
    canvas
        .widthProperty()
        .addListener(e -> canvas.render(storage.getSnapshots(), new int[0], 0, null));
  }

  private void initTable() {
    tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

    TableColumn<Batch, Batch> typeCol = new TableColumn<>("Тип");
    typeCol.setMinWidth(105);
    typeCol.setPrefWidth(115);
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
    skuCol.setMinWidth(120);
    skuCol.setPrefWidth(130);
    skuCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSku()));

    TableColumn<Batch, String> nameCol = new TableColumn<>("Название");
    nameCol.setMinWidth(180);
    nameCol.setPrefWidth(210);
    nameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));

    TableColumn<Batch, Number> qtyCol = new TableColumn<>("Количество");
    qtyCol.setMinWidth(95);
    qtyCol.setPrefWidth(105);
    qtyCol.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQuantity()));

    TableColumn<Batch, String> cellCol = new TableColumn<>("Ячейка");
    cellCol.setMinWidth(80);
    cellCol.setPrefWidth(90);
    cellCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCell()));

    TableColumn<Batch, LocalDate> dateCol = new TableColumn<>("Дата завоза");
    dateCol.setMinWidth(110);
    dateCol.setPrefWidth(120);
    dateCol.setCellValueFactory(
        cell -> new SimpleObjectProperty<>(cell.getValue().getDeliveryDate()));

    TableColumn<Batch, String> countryCol = new TableColumn<>("Страна");
    countryCol.setMinWidth(100);
    countryCol.setPrefWidth(115);
    countryCol.setCellValueFactory(
        cell -> {
          if (cell.getValue() instanceof ImportedBatch imp) {
            return new SimpleStringProperty(imp.getCountry());
          }
          return new SimpleStringProperty("-");
        });

    TableColumn<Batch, String> customsCol = new TableColumn<>("Таможенный код");
    customsCol.setMinWidth(140);
    customsCol.setPrefWidth(155);
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

  private void showBenchmarkResultDialog(String report) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Результаты замеров и Data Race");
    alert.setHeaderText("Бенчмарк производительности и многопоточный тест (ЛР2)");

    TextArea textArea = new TextArea(report);
    textArea.setEditable(false);
    textArea.setFont(javafx.scene.text.Font.font("Monospace", 12));
    textArea.setWrapText(false);
    textArea.setPrefWidth(740);
    textArea.setPrefHeight(480);

    alert.getDialogPane().setContent(textArea);
    alert.setResizable(true);
    alert.showAndWait();
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
