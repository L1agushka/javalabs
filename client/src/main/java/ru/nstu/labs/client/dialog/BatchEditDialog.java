package ru.nstu.labs.client.dialog;
// модальное диалоговое окно на джаваfx добавление новой партии или редактирование существующей
import java.time.LocalDate;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;  
import javafx.scene.control.DatePicker; // поля графических компонентов 
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import ru.nstu.labs.core.model.Batch;
import ru.nstu.labs.core.model.ImportedBatch;

public class BatchEditDialog extends Dialog<Batch> {

  private final TextField skuField = new TextField();
  private final TextField nameField = new TextField();
  private final TextField quantityField = new TextField();
  private final TextField cellField = new TextField();
  private final DatePicker datePicker = new DatePicker(LocalDate.now());
  private final ComboBox<String> typeBox = new ComboBox<>();
  private final TextField countryField = new TextField();
  private final TextField customsCodeField = new TextField();

  public BatchEditDialog(Batch existing) { // конструктор принимает параметр Batch existing если передан нул диалог открывается в режиме создания, если обьект - редактирования
    setTitle(existing == null ? "Добавление партии" : "Редактирование партии");
    setHeaderText(
        existing == null ? "Введите параметры новой партии" : "Измените параметры партии");

    ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
    getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

    GridPane grid = new GridPane();
    grid.setHgap(10); // компоновка кнопок
    grid.setVgap(10);
    grid.setPadding(new Insets(20, 150, 10, 10));
// управление полями ввода если выбрана импортная партия поля страна и таможенный код разблокируются
    typeBox.getItems().addAll("Базовая партия", "Импортная партия");
    typeBox.setValue("Базовая партия");

    countryField.setDisable(true);
    customsCodeField.setDisable(true);

    typeBox.setOnAction(
        e -> {
          boolean isImported = "Импортная партия".equals(typeBox.getValue());
          countryField.setDisable(!isImported);
          customsCodeField.setDisable(!isImported);
        });

    grid.add(new Label("Тип:"), 0, 0);
    grid.add(typeBox, 1, 0);
    grid.add(new Label("Артикул:"), 0, 1);
    grid.add(skuField, 1, 1);
    grid.add(new Label("Название:"), 0, 2);
    grid.add(nameField, 1, 2);
    grid.add(new Label("Количество:"), 0, 3);
    grid.add(quantityField, 1, 3);
    grid.add(new Label("Ячейка:"), 0, 4);
    grid.add(cellField, 1, 4);
    grid.add(new Label("Дата завоза:"), 0, 5);
    grid.add(datePicker, 1, 5);
    grid.add(new Label("Страна:"), 0, 6);
    grid.add(countryField, 1, 6);
    grid.add(new Label("Таможенный код:"), 0, 7);
    grid.add(customsCodeField, 1, 7);
// предзаполнение формы в режиме редактирования
    if (existing != null) {
      typeBox.setDisable(true); // запрещает менять тип уже сущ. партии
      skuField.setText(existing.getSku());
      skuField.setDisable(true); // артикул блокируется от изменения тк он выступает уникальным идентификатором партии в equals и hashCode
      nameField.setText(existing.getName());
      quantityField.setText(String.valueOf(existing.getQuantity()));
      cellField.setText(existing.getCell());
      datePicker.setValue(existing.getDeliveryDate());

      if (existing instanceof ImportedBatch imported) { // одновременно проверяет тип и обьявляет типизированную переменную imported без явного приведения (ImportedBatch) existing. Если партия импортная заполняются поля страны и таможенного кода.
        typeBox.setValue("Импортная партия");
        countryField.setDisable(false);
        customsCodeField.setDisable(false);
        countryField.setText(imported.getCountry());
        customsCodeField.setText(imported.getCustomsCode());
      }
    }

    getDialogPane().setContent(grid);

    setResultConverter( // преобразование результата и валидация 
        dialogButton -> {
          if (dialogButton == saveButtonType) { // нажата кнопка сохранить строка кол-ва парсится в инт при нечисловом приравнивается к нулю 
            int qty = 0;
            try {
              qty = Integer.parseInt(quantityField.getText().trim());
            } catch (NumberFormatException ignored) {
            }

            Batch batch; // В зависимости от значения typeBox вызывается конструктор ImportedBatch или Batch
            if ("Импортная партия".equals(typeBox.getValue())) {
              batch =
                  new ImportedBatch(
                      skuField.getText().trim(),
                      nameField.getText().trim(),
                      qty,
                      cellField.getText().trim(),
                      datePicker.getValue(),
                      countryField.getText().trim(),
                      customsCodeField.getText().trim());
            } else {
              batch =
                  new Batch(
                      skuField.getText().trim(),
                      nameField.getText().trim(),
                      qty,
                      cellField.getText().trim(),
                      datePicker.getValue());
            }

            List<String> errors = batch.validate(); // валидация на пустые строки итд
            if (!errors.isEmpty()) {
              return null;
            }
            return batch;
          }
          return null;
        });
  }
}
