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

  // Реактивный список элементов в памяти. При любых изменениях (add, remove)
  // TableView сама мгновенно перерисовывает строки без ручных вызовов refresh().
  private final ObservableList<Batch> masterData = FXCollections.observableArrayList();

  // Основной визуальный компонент таблицы JavaFX.
  private final TableView<Batch> tableView = new TableView<>();

  // Репозиторий для парсинга и сериализации CSV-файлов.
  private final CsvBatchRepository repository = new CsvBatchRepository();

  public static void main(String[] args) {
    // Старт жизненного цикла JavaFX приложения (инициализирует графическую подсистему и вызывает start()).
    launch(args);
  }

  @Override
  public void start(Stage stage) {
    stage.setTitle("Склад партий (Лабораторная работа №1)");

    // Настраиваем колонки и привязываем masterData к таблице.
    initTable();

    Button btnAdd = new Button("Добавить партию");
    Button btnEdit = new Button("Изменить");
    Button btnLoad = new Button("Загрузить CSV");
    Button btnSave = new Button("Экспорт в CSV");

    // CSS-классы для стилизации кнопок через style.css.
    btnAdd.getStyleClass().add("btn-citrus");
    btnEdit.getStyleClass().add("btn-amber");

    // При старте ничего не выбрано, кнопка изменения должна быть заблокирована.
    btnEdit.setDisable(true);

    // Слушатель выбора строки в таблице.
    tableView
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, oldSel, newSel) -> {
              if (newSel == null) {
                // Если кликнули в пустоту или сняли выделение — блокируем кнопку.
                btnEdit.setDisable(true);
              } else {
                // Выполнение требования лабы: блокируем кнопку "Изменить",
                // если выбранная сущность — read-only (ArchivedBatch).
                btnEdit.setDisable(newSel instanceof ArchivedBatch);
              }
            });

    // Обработчик кнопки «Добавить»
    btnAdd.setOnAction(
        e -> {
          // Передаем null, сообщая диалогу, что открывается режим создания новой записи.
          BatchEditDialog dialog = new BatchEditDialog(null);
          dialog
              .showAndWait()
              .ifPresent(
                  batch -> {
                    // Реализация логики Upsert (вставка/обновление):
                    // Так как equals() в Batch сравнивает по SKU, remove удалит старую запись,
                    // если партия с таким артикулом уже существовала в списке.
                    masterData.remove(batch);
                    masterData.add(batch);
                  });
        });

    // Обработчик кнопки «Изменить»
    btnEdit.setOnAction(
        e -> {
          Batch selected = tableView.getSelectionModel().getSelectedItem();
          // Дополнительная проверка безопасности перед открытием модального окна.
          if (selected != null && !(selected instanceof ArchivedBatch)) {
            // Передаем выбранный объект — диалог открывается с предзаполненными полями.
            BatchEditDialog dialog = new BatchEditDialog(selected);
            dialog
                .showAndWait()
                .ifPresent(
                    updated -> {
                      // Находим индекс старого объекта и заменяем его новым экземпляром на том же месте.
                      int index = masterData.indexOf(selected);
                      if (index >= 0) {
                        masterData.set(index, updated);
                      }
                    });
          }
        });

    // Обработчик кнопки «Загрузить CSV»
    btnLoad.setOnAction(
        e -> {
          FileChooser fc = new FileChooser();
          fc.setTitle("Загрузить партии из CSV");
          fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV файлы", "*.csv"));
          File file = fc.showOpenDialog(stage);

          if (file != null) {
            try {
              // Загружаем данные: репозиторий возвращает LoadResult (список валидных объектов + ошибки).
              CsvBatchRepository.LoadResult result = repository.load(file.toPath());
              
              // Полностью перезаписываем текущие данные валидными записями из файла.
              masterData.setAll(result.items());

              // Если были битые строки — показываем диалог со списком ошибок (требование на "Максимум").
              if (!result.errors().isEmpty()) {
                showErrorSummaryDialog(result.errors());
              }
            } catch (IOException ex) {
              // Системные ошибки ввода-вывода (нет доступа, диск поврежден и т.д.).
              showSimpleError(
                  "Ошибка ввода-вывода", "Не удалось прочитать файл: " + ex.getMessage());
            }
          }
        });

    // Обработчик кнопки «Экспорт в CSV»
    btnSave.setOnAction(
        e -> {
          FileChooser fc = new FileChooser();
          fc.setTitle("Сохранить партии в CSV");
          fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV файлы", "*.csv"));
          File file = fc.showSaveDialog(stage);

          if (file != null) {
            try {
              // Сохраняем текущие строки из masterData на диск.
              repository.save(file.toPath(), masterData);
            } catch (IOException ex) {
              showSimpleError(
                  "Ошибка ввода-вывода", "Не удалось сохранить файл: " + ex.getMessage());
            }
          }
        });

    // Панель кнопок управления снизу (отступ 12px между кнопками).
    HBox controls = new HBox(12, btnAdd, btnEdit, btnLoad, btnSave);
    controls.getStyleClass().add("bottom-bar");

    // Главный контейнер разметки: таблица по центру, панель кнопок снизу.
    BorderPane root = new BorderPane();
    root.setStyle("-fx-background-color: #121214;");
    root.setCenter(tableView);
    root.setBottom(controls);
    root.setPadding(new Insets(14));

    Scene scene = new Scene(root, 1050, 580);
    scene.setFill(Color.valueOf("#121214"));

    // Подключение внешней таблицы стилей.
    var stylesheet = App.class.getResource("style.css");
    if (stylesheet != null) {
      scene.getStylesheets().add(stylesheet.toExternalForm());
    }

    stage.setScene(scene);
    stage.show();
  }

  // Конфигурация структуры колонок таблицы TableView
  private void initTable() {
    // Последняя колонка автоматически растягивается, заполняя свободное пространство.
    tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

    // Колонка "Тип" с кастомной отрисовкой бейджей.
    TableColumn<Batch, Batch> typeCol = new TableColumn<>("Тип");
    typeCol.setMinWidth(110);
    typeCol.setMaxWidth(130);

    // CellValueFactory достает сам объект Batch целиком для анализа его типа.
    typeCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));

    // CellFactory отвечает за отображение: заменяет сырой текст на цветной компонент Label.
    typeCol.setCellFactory(
        col ->
            new TableCell<>() {
              private final Label badge = new Label();

              @Override
              protected void updateItem(Batch batch, boolean empty) {
                super.updateItem(batch, empty);
                // Очистка ячейки, если строка пустая (механизм переиспользования ячеек JavaFX).
                if (empty || batch == null) {
                  setGraphic(null);
                } else {
                  badge.getStyleClass().setAll("badge");
                  // Определение типа через сопоставление классов:
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

    // Обычные текстовые/числовые колонки: достают поля объекта через геттеры.
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

    // Полиморфная колонка: у базовых и архивных партий нет поля страны.
    // Если объект ImportedBatch — достаем страну, иначе ставим прочерк "-".
    TableColumn<Batch, String> countryCol = new TableColumn<>("Страна");
    countryCol.setMinWidth(110);
    countryCol.setCellValueFactory(
        cell -> {
          if (cell.getValue() instanceof ImportedBatch imp) {
            return new SimpleStringProperty(imp.getCountry());
          }
          return new SimpleStringProperty("-");
        });

    // Полиморфная колонка: аналогично для таможенного кода.
    TableColumn<Batch, String> customsCol = new TableColumn<>("Таможенный код");
    customsCol.setMinWidth(130);
    customsCol.setCellValueFactory(
        cell -> {
          if (cell.getValue() instanceof ImportedBatch imp) {
            return new SimpleStringProperty(imp.getCustomsCode());
          }
          return new SimpleStringProperty("-");
        });

    // Регистрируем колонки в таблице и привязываем источник данных.
    tableView
        .getColumns()
        .addAll(typeCol, skuCol, nameCol, qtyCol, cellCol, dateCol, countryCol, customsCol);
    tableView.setItems(masterData);
  }

  // Окно предупреждения при загрузке CSV с поврежденными строками (Критерий «Максимум»).
  private void showErrorSummaryDialog(List<CsvParseException> errors) {
    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle("Предупреждение при загрузке");
    alert.setHeaderText("Некоторые строки были пропущены (" + errors.size() + " шт.)");

    // Формируем детальный отчет: сообщение ошибки + оригинальная строка из файла.
    StringBuilder sb = new StringBuilder();
    for (CsvParseException ex : errors) {
      sb.append(ex.getMessage())
          .append("\n Исходная строка: ")
          .append(ex.getRawLine())
          .append("\n\n");
    }

    // Помещаем текст в многострочное нередактируемое поле с автопереносом.
    TextArea textArea = new TextArea(sb.toString());
    textArea.setEditable(false);
    textArea.setWrapText(true);
    textArea.setMaxWidth(Double.MAX_VALUE);
    textArea.setMaxHeight(Double.MAX_VALUE);

    // Встраиваем текстовую область в выпадающую панель диалогового окна.
    alert.getDialogPane().setExpandableContent(textArea);
    alert.getDialogPane().setExpanded(true);
    alert.showAndWait();
  }

  // Вспомогательный метод для показа стандартных диалогов критических ошибок.
  private void showSimpleError(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }
}
