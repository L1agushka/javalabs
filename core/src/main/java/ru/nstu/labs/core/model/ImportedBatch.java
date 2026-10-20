package ru.nstu.labs.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.time.LocalDate;
import java.util.List;

public class ImportedBatch extends Batch {

  @Serial
  private static final long serialVersionUID = 1L;

  private String country;
  private String customsCode;

  @JsonCreator
  public ImportedBatch(
      @JsonProperty("sku") String sku,
      @JsonProperty("name") String name,
      @JsonProperty("quantity") int quantity,
      @JsonProperty("cell") String cell,
      @JsonProperty("deliveryDate") LocalDate deliveryDate,
      @JsonProperty("country") String country,
      @JsonProperty("customsCode") String customsCode) {
    super(sku, name, quantity, cell, deliveryDate);
    this.country = country;
    this.customsCode = customsCode;
  }

  public String getCountry() { return country; }
  public void setCountry(String country) { this.country = country; }

  public String getCustomsCode() { return customsCode; }
  public void setCustomsCode(String customsCode) { this.customsCode = customsCode; }

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
