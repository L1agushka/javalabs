//Editable — интерфейс, который определяет, что объект можно проверять перед редактированием. 
// Метод validate() возвращает список ошибок при некорректных данных. 
// Классы, реализующие этот интерфейс, должны самостоятельно реализовать проверку своих полей.

package ru.nstu.labs.core.model;

import java.util.List;

public interface Editable {
  List<String> validate();
}
