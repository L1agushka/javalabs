package ru.nstu.labs.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.time.LocalDate;

public final class ArchivedBatch extends Batch {

  @Serial private static final long serialVersionUID = 1L;

  @JsonCreator
  public ArchivedBatch(
      @JsonProperty("sku") String sku,
      @JsonProperty("name") String name,
      @JsonProperty("quantity") int quantity,
      @JsonProperty("cell") String cell,
      @JsonProperty("deliveryDate") LocalDate deliveryDate,
      @JsonProperty("note") String note,
      @JsonProperty("lastUpdated") LocalDate lastUpdated) {
    super(sku, name, quantity, cell, deliveryDate, note, lastUpdated);
  }

  public ArchivedBatch(String sku, String name, int quantity, String cell, LocalDate deliveryDate) {
    this(sku, name, quantity, cell, deliveryDate, null, null);
  }

  @Override
  public void setSku(String sku) {
    throw new UnsupportedOperationException("Редактирование архивной партии запрещено");
  }

  @Override
  public void setName(String name) {
    throw new UnsupportedOperationException("Редактирование архивной партии запрещено");
  }

  @Override
  public void setQuantity(int quantity) {
    throw new UnsupportedOperationException("Редактирование архивной партии запрещено");
  }

  @Override
  public void setCell(String cell) {
    throw new UnsupportedOperationException("Редактирование архивной партии запрещено");
  }

  @Override
  public void setDeliveryDate(LocalDate deliveryDate) {
    throw new UnsupportedOperationException("Редактирование архивной партии запрещено");
  }

  @Override
  public void setNote(String note) {
    throw new UnsupportedOperationException("Редактирование архивной партии запрещено");
  }

  @Override
  public void setLastUpdated(LocalDate lastUpdated) {
    throw new UnsupportedOperationException("Редактирование архивной партии запрещено");
  }
}
