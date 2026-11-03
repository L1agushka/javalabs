package ru.nstu.labs.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type",
    defaultImpl = Batch.class)
@JsonSubTypes({
  @JsonSubTypes.Type(value = Batch.class, name = "BATCH"),
  @JsonSubTypes.Type(value = ArchivedBatch.class, name = "ARCHIVED"),
  @JsonSubTypes.Type(value = ImportedBatch.class, name = "IMPORTED")
})
public class Batch implements Editable, Serializable {

  @Serial private static final long serialVersionUID = 1L;

  protected String sku;
  protected String name;
  protected int quantity;
  protected String cell;
  protected LocalDate deliveryDate;

  /** Заметка кладовщика; добавлена в схеме v2. */
  protected String note;

  /** Дата последней актуализации карточки; добавлена в схеме v3. */
  protected LocalDate lastUpdated;

  @JsonCreator
  public Batch(
      @JsonProperty("sku") String sku,
      @JsonProperty("name") String name,
      @JsonProperty("quantity") int quantity,
      @JsonProperty("cell") String cell,
      @JsonProperty("deliveryDate") LocalDate deliveryDate,
      @JsonProperty("note") String note,
      @JsonProperty("lastUpdated") LocalDate lastUpdated) {
    this.sku = sku;
    this.name = name;
    this.quantity = quantity;
    this.cell = cell;
    this.deliveryDate = deliveryDate;
    this.note = note;
    this.lastUpdated = lastUpdated;
  }

  public Batch(String sku, String name, int quantity, String cell, LocalDate deliveryDate) {
    this(sku, name, quantity, cell, deliveryDate, null, null);
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

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public LocalDate getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(LocalDate lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  /** Служебный метод мигратора: не является пользовательским редактированием. */
  public void applyMigrationDefaults(String migratedNote, LocalDate migratedLastUpdated) {
    if (note == null) note = migratedNote;
    if (lastUpdated == null) lastUpdated = migratedLastUpdated;
  }

  @Override
  public List<String> validate() {
    List<String> errors = new ArrayList<>();
    if (sku == null || sku.isBlank()) errors.add("Артикул не может быть пустым");
    if (name == null || name.isBlank()) errors.add("Название не может быть пустым");
    if (quantity < 0) errors.add("Количество не может быть отрицательным");
    if (cell == null || cell.isBlank()) errors.add("Ячейка не может быть пустой");
    if (deliveryDate == null) errors.add("Дата завоза должна быть указана");
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

  @Override
  public String toString() {
    return "Batch{sku='"
        + sku
        + "', name='"
        + name
        + "', quantity="
        + quantity
        + ", cell='"
        + cell
        + "', deliveryDate="
        + deliveryDate
        + "}";
  }
}
