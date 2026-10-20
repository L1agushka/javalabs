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
    defaultImpl = Batch.class
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Batch.class, name = "BATCH"),
    @JsonSubTypes.Type(value = ArchivedBatch.class, name = "ARCHIVED"),
    @JsonSubTypes.Type(value = ImportedBatch.class, name = "IMPORTED")
})
public class Batch implements Editable, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  protected String sku;
  protected String name;
  protected int quantity;
  protected String cell;
  protected LocalDate deliveryDate;

  @JsonCreator
  public Batch(
      @JsonProperty("sku") String sku,
      @JsonProperty("name") String name,
      @JsonProperty("quantity") int quantity,
      @JsonProperty("cell") String cell,
      @JsonProperty("deliveryDate") LocalDate deliveryDate) {
    this.sku = sku;
    this.name = name;
    this.quantity = quantity;
    this.cell = cell;
    this.deliveryDate = deliveryDate;
  }

  public String getSku() { return sku; }
  public void setSku(String sku) { this.sku = sku; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public int getQuantity() { return quantity; }
  public void setQuantity(int quantity) { this.quantity = quantity; }

  public String getCell() { return cell; }
  public void setCell(String cell) { this.cell = cell; }

  public LocalDate getDeliveryDate() { return deliveryDate; }
  public void setDeliveryDate(LocalDate deliveryDate) { this.deliveryDate = deliveryDate; }

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
    return "Batch{sku='" + sku + "', name='" + name + "', quantity=" + quantity + ", cell='" + cell + "', deliveryDate=" + deliveryDate + "}";
  }
}
