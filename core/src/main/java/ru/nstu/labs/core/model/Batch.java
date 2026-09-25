//основная сущность предметной области «Склад». 
// Она содержит артикул, название, количество, ячейку и дату завоза. 
// Класс реализует интерфейс Editable, поэтому его экземпляры можно редактировать. 
// Метод validate() проверяет корректность полей и возвращает список ошибок. 
// Артикул используется как уникальный ключ, поэтому в equals() и hashCode() учитывается только sku.


package ru.nstu.labs.core.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Batch implements Editable {
  protected String sku;
  protected String name;
  protected int quantity;
  protected String cell;
  protected LocalDate deliveryDate;

  public Batch(String sku, String name, int quantity, String cell, LocalDate deliveryDate) {
    this.sku = sku;
    this.name = name;
    this.quantity = quantity;
    this.cell = cell;
    this.deliveryDate = deliveryDate;
  }

  public String getSku() {
    return sku;
  }

  public void setSku(String sku) {
    this.sku = sku;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public String getCell() {
    return cell;
  }

  public void setCell(String cell) {
    this.cell = cell;
  }

  public LocalDate getDeliveryDate() {
    return deliveryDate;
  }

  public void setDeliveryDate(LocalDate deliveryDate) {
    this.deliveryDate = deliveryDate;
  }

  @Override
  public List<String> validate() {
    List<String> errors = new ArrayList<>();
    if (sku == null || sku.isBlank()) {
      errors.add("Артикул не может быть пустым");
    }
    if (name == null || name.isBlank()) {
      errors.add("Название не может быть пустым");
    }
    if (quantity < 0) {
      errors.add("Количество не может быть отрицательным");
    }
    if (cell == null || cell.isBlank()) {
      errors.add("Ячейка не может быть пустой");
    }
    if (deliveryDate == null) {
      errors.add("Дата завоза должна быть указана");
    }
    return errors;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Batch batch)) return false;
    return Objects.equals(sku, batch.sku);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sku);
  }
}
