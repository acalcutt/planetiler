package com.onthegomap.planetiler.collection;

import static io.prometheus.client.Collector.NANOSECONDS_PER_SECOND;

import com.onthegomap.planetiler.config.PlanetilerConfig;
import com.onthegomap.planetiler.stats.Counter;
import com.onthegomap.planetiler.stats.ProgressLoggers;
import com.onthegomap.planetiler.stats.Stats;
import com.onthegomap.planetiler.stats.Timer;
import com.onthegomap.planetiler.util.FileUtils;
import com.onthegomap.planetiler.util.Format;
import com.onthegomap.planetiler.worker.Worker;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Performance tests for {@link ExternalMergeSort}. Times how long it takes to write temp features, sort, then read them
 * back with different parameters.
 * <p>
 * Usage: {@code BenchmarkExternalMergeSort &lt;number of GB of features to write&gt;}
 */
public class BenchmarkExternalMergeSort {
  private static final Format FORMAT = Format.defaultInstance();
  private static final int ITEM_SIZE_BYTES = 76;
  private static final int DISK_OVERHEAD_BYTES = Long.BYTES + Integer.BYTES;
  private static final int ITEM_DATA_BYTES = ITEM_SIZE_BYTES - DISK_OVERHEAD_BYTES;
  private static final int MEMORY_OVERHEAD_BYTES = 8 + 16 + 8 + 8 + 24;
  private static final int ITEM_MEMORY_BYTES = MEMORY_OVERHEAD_BYTES + ITEM_DATA_BYTES;
  private static final byte[] TEST_DATA = new byte[ITEM_DATA_BYTES];
  static {
    ThreadLocalRandom.current().nextBytes(TEST_DATA);
  }

  public static void main(String[] args) {
    double gb = args.length < 1 ? 1 : Double.parseDouble(args[0]);
    long number = (long) (gb * 1_000_000_000 / ITEM_SIZE_BYTES);
    Path path = Path.of("./featuretest");
    FileUtils.delete(path);
    FileUtils.deleteOnExit(path);
    var config = PlanetilerConfig.defaults();
    for (int i = 0; i < 3; i++) {
      try {
        List<Results> results = new ArrayList<>();
        for (int chunks : List.of(1, 10, 100, 1_000, 10_000)) {
          results.add(run(path, 1, number, chunks, true, true, true, config));
        }
        for (var result : results) {
          System.err.println(result);
        }
      } finally {
        FileUtils.delete(path);
      }
    }
  }

  private record Results(
    String write, String read, String sort,
    int chunks,
    int writeWorkers, int readWorkers,
    long items, int chunkSizeLimit, boolean gzip, boolean mmap, boolean parallelSort,
    boolean madvise
  ) {}

  private static Results run(Path tmpDir, int writeWorkers, long items, int numChunks,
    boolean mmap, boolean parallelSort, boolean madvise, PlanetilerConfig config) {
    long chunkSizeLimit = items * ITEM_MEMORY_BYTES / numChunks;
    if (chunkSizeLimit > Integer.MAX_VALUE) {
      throw new IllegalStateException("Chunk size too big: " + chunkSizeLimit);
    }
    boolean gzip = false;
    int sortWorkers = Runtime.getRuntime().availableProcessors();
    int readWorkers = 1;
    FileUtils.delete(tmpDir);
    var sorter =
      new ExternalMergeSort(tmpDir, sortWorkers, (int) chunkSizeLimit, gzip, mmap, parallelSort, madvise, config,
        Stats.inMemory());

    var writeTimer = Timer.start();
    doWrites(writeWorkers, items, sorter);
    writeTimer.stop();

    var sortTimer = Timer.start();
    sorter.sort();
    sortTimer.stop();

    var readTimer = Timer.start();
    doReads(readWorkers, items, sorter);
    readTimer.stop();

    return new Results(
      FORMAT.numeric(items * NANOSECONDS_PER_SECOND / writeTimer.elapsed().wall().toNanos()) + "/s",
      FORMAT.numeric(items * NANOSECONDS_PER_SECOND / readTimer.elapsed().wall().toNanos()) + "/s",
      FORMAT.duration(sortTimer.elapsed().wall()),
      sorter.chunks(),
      writeWorkers,
      readWorkers,
      items,
      (int) chunkSizeLimit,
      gzip,
      mmap,
      parallelSort,
      madvise
    );
  }

  private static void doReads(int readWorkers, long items, ExternalMergeSort sorter) {
    var counters = Counter.newMultiThreadCounter();
    var reader = new Worker("read", Stats.inMemory(), readWorkers, () -> {
      var counter = counters.counterForThread();
      for (var ignored : sorter) {
        counter.inc();
      }
    });
    ProgressLoggers loggers = ProgressLoggers.create()
      .addRatePercentCounter("items", items, counters, false)
      .addFileSize(sorter)
      .newLine()
      .addProcessStats()
      .newLine()
      .addThreadPoolStats("reader", reader);
    reader.awaitAndLog(loggers, Duration.ofSeconds(1));
  }

  private static void doWrites(int writeWorkers, long items, ExternalMergeSort sorter) {
    var counters = Counter.newMultiThreadCounter();
    var writer = new Worker("write", Stats.inMemory(), writeWorkers, () -> {
      try (var writerForThread = sorter.writerForThread()) {
        var counter = counters.counterForThread();
        var random = ThreadLocalRandom.current();
        long toWrite = items / writeWorkers;
        for (long i = 0; i < toWrite; i++) {
          writerForThread.accept(new SortableFeature(random.nextLong(), TEST_DATA));
          counter.inc();
        }
      }
    });
    ProgressLoggers loggers = ProgressLoggers.create()
      .addRatePercentCounter("items", items, counters, false)
      .addFileSize(sorter)
      .newLine()
      .addProcessStats()
      .newLine()
      .addThreadPoolStats("writer", writer);
    writer.awaitAndLog(loggers, Duration.ofSeconds(1));
  }
}
