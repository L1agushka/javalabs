package ru.nstu.labs.client.view;

import java.util.function.Consumer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import ru.nstu.labs.core.model.Batch;
import ru.nstu.labs.core.structure.OpenAddressingHashTable.BucketSnapshot;
import ru.nstu.labs.core.structure.OpenAddressingHashTable.NodeState;

public class HashTableCanvas extends Canvas {

  private static final double CELL_WIDTH = 130.0;
  private static final double CELL_HEIGHT = 80.0;
  private static final double GAP = 10.0;
  private static final double PADDING = 12.0;

  private BucketSnapshot<String, Batch>[] lastSnapshots;
  private int[] activeVisited = new int[0];
  private int selectedIndex = -1;
  private String lastOpInfo = "Готов";
  private int lastProbesCount = 0;

  private Consumer<BucketSnapshot<String, Batch>> onCellSelected;
  private Consumer<Double> onScrollRequested;

  public HashTableCanvas() {
    setWidth(440);
    setHeight(520);

    setOnMouseClicked(
        e -> {
          if (lastSnapshots == null || lastSnapshots.length == 0) return;
          int clickedIdx = getCellIndexAt(e.getX(), e.getY());
          if (clickedIdx >= 0 && clickedIdx < lastSnapshots.length) {
            selectedIndex = clickedIdx;
            renderInstant();
            if (onCellSelected != null) {
              onCellSelected.accept(lastSnapshots[clickedIdx]);
            }
          }
        });
  }

  public void setOnCellSelected(Consumer<BucketSnapshot<String, Batch>> listener) {
    this.onCellSelected = listener;
  }

  public void setOnScrollRequested(Consumer<Double> scrollConsumer) {
    this.onScrollRequested = scrollConsumer;
  }

  public void render(
      BucketSnapshot<String, Batch>[] snapshots,
      int[] visitedIndices,
      int probes,
      String operationInfo) {

    this.lastSnapshots = snapshots;
    this.activeVisited = visitedIndices != null ? visitedIndices : new int[0];
    this.lastOpInfo = operationInfo != null ? operationInfo : "Ожидание";
    this.lastProbesCount = probes;

    if (activeVisited.length > 0 && onScrollRequested != null) {
      int targetIdx = activeVisited[activeVisited.length - 1];
      scrollToIndex(targetIdx);
    }

    renderInstant();
  }

  public void renderInstant() {
    if (lastSnapshots == null || lastSnapshots.length == 0) return;

    GraphicsContext gc = getGraphicsContext2D();
    double width = Math.max(getWidth(), 300);

    int cols = Math.max(1, (int) ((width - PADDING * 2 + GAP) / (CELL_WIDTH + GAP)));
    int rows = (lastSnapshots.length + cols - 1) / cols;

    double neededHeight = PADDING * 2 + 40 + rows * (CELL_HEIGHT + GAP);
    if (getHeight() < neededHeight) {
      setHeight(neededHeight);
    }

    gc.setFill(Color.web("#16161a"));
    gc.fillRect(0, 0, getWidth(), getHeight());

    gc.setFill(Color.web("#a1a1aa"));
    gc.setFont(Font.font("Monospace", FontWeight.NORMAL, 12));
    String header =
        String.format(
            "Capacity: %d | %s | Проб: %d", lastSnapshots.length, lastOpInfo, lastProbesCount);
    gc.fillText(header, PADDING, 22);

    for (int i = 0; i < lastSnapshots.length; i++) {
      int col = i % cols;
      int row = i / cols;
      double x = PADDING + col * (CELL_WIDTH + GAP);
      double y = 40 + PADDING + row * (CELL_HEIGHT + GAP);

      BucketSnapshot<String, Batch> b = lastSnapshots[i];
      boolean isVisited = false;
      boolean isTargetHit = false;
      int probeStep = -1;

      for (int p = 0; p < activeVisited.length; p++) {
        if (activeVisited[p] == i) {
          isVisited = true;
          probeStep = p + 1;
          if (p == activeVisited.length - 1) {
            isTargetHit = true;
          }
          break;
        }
      }

      boolean isSelected = (i == selectedIndex);
      drawCell(gc, x, y, b, isVisited, isTargetHit, probeStep, isSelected);
    }
  }

  private void drawCell(
      GraphicsContext gc,
      double x,
      double y,
      BucketSnapshot<String, Batch> bucket,
      boolean isVisited,
      boolean isTargetHit,
      int probeStep,
      boolean isSelected) {

    // Фоновая заливка под темно-оранжевую палитру
    Color bg =
        switch (bucket.state()) {
          case EMPTY -> Color.web("#1c1917"); // графит
          case OCCUPIED -> Color.web("#26201b"); // глубокий кофейно-шоколадный
          case DELETED -> Color.web("#331616"); // приглушенный бордовый
        };

    gc.setFill(bg);
    gc.fillRoundRect(x, y, CELL_WIDTH, CELL_HEIGHT, 8, 8);

    // Рамки ячеек
    if (isSelected || isTargetHit) {
      gc.setStroke(Color.web("#ea580c")); // Цитрусовый оранжевый
      gc.setLineWidth(2.8);
    } else if (isVisited) {
      gc.setStroke(Color.web("#f59e0b")); // Янтарный
      gc.setLineWidth(2.2);
    } else if (bucket.state() == NodeState.OCCUPIED) {
      gc.setStroke(Color.web("#443528")); // Спокойная янтарная рамка
      gc.setLineWidth(1.2);
    } else {
      gc.setStroke(Color.web("#292524")); // Тонкая граница для пустых
      gc.setLineWidth(1.0);
    }
    gc.strokeRoundRect(x, y, CELL_WIDTH, CELL_HEIGHT, 8, 8);

    // Индекс [i]
    gc.setFill(Color.web("#78716c"));
    gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 10));
    gc.fillText("[" + bucket.index() + "]", x + 8, y + 15);

    // Бейдж шага пробы #1
    if (isVisited) {
      gc.setFill(isTargetHit ? Color.web("#ea580c") : Color.web("#f59e0b"));
      gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 11));
      gc.fillText("#" + probeStep, x + CELL_WIDTH - 26, y + 15);
    }

    if (bucket.state() == NodeState.OCCUPIED && bucket.value() != null) {
      // 1. Полный артикул SKU (крупно и золотисто-янтарно)
      gc.setFill(Color.web("#fbbf24"));
      gc.setFont(Font.font("Monospace", FontWeight.BOLD, 11.5));
      gc.fillText(bucket.key(), x + 8, y + 36);

      // 2. Наименование товара
      gc.setFill(Color.web("#e7e5e4"));
      gc.setFont(Font.font("SansSerif", FontWeight.NORMAL, 10));
      String name = bucket.value().getName();
      if (name.length() > 16) name = name.substring(0, 15) + "…";
      gc.fillText(name, x + 8, y + 53);

      // 3. Количество и ячейка
      gc.setFill(Color.web("#a8a29e"));
      gc.setFont(Font.font("Monospace", FontWeight.NORMAL, 9.5));
      gc.fillText(
          "кол: " + bucket.value().getQuantity() + " | " + bucket.value().getCell(), x + 8, y + 69);

    } else if (bucket.state() == NodeState.DELETED) {
      gc.setFill(Color.web("#ef4444"));
      gc.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
      gc.fillText("[DELETED]", x + 8, y + 46);

      gc.setFill(Color.web("#991b1b"));
      gc.setFont(Font.font("SansSerif", FontWeight.NORMAL, 9.5));
      gc.fillText("tombstone", x + 8, y + 62);

    } else {
      gc.setFill(Color.web("#57534e"));
      gc.setFont(Font.font("Monospace", FontWeight.NORMAL, 10.5));
      gc.fillText("свободно", x + 8, y + 48);
    }
  }

  private void scrollToIndex(int index) {
    if (lastSnapshots == null || lastSnapshots.length == 0) return;
    double width = Math.max(getWidth(), 300);
    int cols = Math.max(1, (int) ((width - PADDING * 2 + GAP) / (CELL_WIDTH + GAP)));
    int totalRows = (lastSnapshots.length + cols - 1) / cols;
    int targetRow = index / cols;
    double scrollV = (double) targetRow / Math.max(1, totalRows - 1);
    onScrollRequested.accept(Math.min(1.0, Math.max(0.0, scrollV)));
  }

  private int getCellIndexAt(double mouseX, double mouseY) {
    double width = Math.max(getWidth(), 300);
    int cols = Math.max(1, (int) ((width - PADDING * 2 + GAP) / (CELL_WIDTH + GAP)));

    double localY = mouseY - 40 - PADDING;
    double localX = mouseX - PADDING;
    if (localY < 0 || localX < 0) return -1;

    int col = (int) (localX / (CELL_WIDTH + GAP));
    int row = (int) (localY / (CELL_HEIGHT + GAP));

    int idx = row * cols + col;
    if (idx < 0 || idx >= lastSnapshots.length) return -1;

    double cellX = col * (CELL_WIDTH + GAP);
    double cellY = row * (CELL_HEIGHT + GAP);
    if (localX - cellX <= CELL_WIDTH && localY - cellY <= CELL_HEIGHT) {
      return idx;
    }
    return -1;
  }
}
