//Благодаря этому механизму, объект ArchivedBatch можно создать и заполнить данными при чтении из CSV-файла

package ru.nstu.labs.core.model;

import java.time.LocalDate;

public final class ArchivedBatch extends Batch {

  public ArchivedBatch(String sku, String name, int quantity, String cell, LocalDate deliveryDate) {
    super(sku, name, quantity, cell, deliveryDate);
  }

  @Override
  public void setSku(String sku) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setName(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setQuantity(int quantity) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setCell(String cell) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setDeliveryDate(LocalDate deliveryDate) {
    throw new UnsupportedOperationException();
  }
}
