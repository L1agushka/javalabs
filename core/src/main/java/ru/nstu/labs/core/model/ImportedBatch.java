//ImportedBatch — наследник класса Batch, представляющий импортную партию. 
// Он наследует основные поля партии и добавляет два новых: страну и таможенный код.
//  Метод validate() сначала проверяет поля базовой партии через super.validate(), 
// а затем дополнительно проверяет страну и таможенный код.

package ru.nstu.labs.core.model;

import java.time.LocalDate;
import java.util.List;

public class ImportedBatch extends Batch {
  private String country;
  private String customsCode;

  public ImportedBatch(
      String sku,
      String name,
      int quantity,
      String cell,
      LocalDate deliveryDate,
      String country,
      String customsCode) {
    super(sku, name, quantity, cell, deliveryDate);
    this.country = country;
    this.customsCode = customsCode;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public String getCustomsCode() {
    return customsCode;
  }

  public void setCustomsCode(String customsCode) {
    this.customsCode = customsCode;
  }

  @Override
  public List<String> validate() {
    List<String> errors = super.validate();
    if (country == null || country.isBlank()) {
      errors.add("Страна не может быть пустой");
    }
    if (customsCode == null || customsCode.isBlank()) {
      errors.add("Таможенный код не может быть пустым");
    }
    return errors;
  }
}
