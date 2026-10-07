package ru.nstu.labs.core.benchmark;

public class BenchmarkRunner {
  public static void main(String[] args) {
    System.out.println("Выполнение замеров производительности и тестирования гонки потоков...");
    BenchmarkService service = new BenchmarkService();
    String report = service.runFullReport();
    System.out.println(report);
  }
}
