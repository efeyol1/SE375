package SE375_LAB;

import java.io.*;
import java.util.*;
import java.lang.Thread;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/*
 * Average running times over 50 runs (fill in after running on your machine):
 *   Version A (lock both HashMaps at once)   : ~___ ms
 *   Version B (lock one HashMap at a time)   : ~___ ms
 */
public class TransportStats implements Runnable {

    static class DistrictStats {
        long totalPassengers;
        double avgPassengers;

        DistrictStats(long totalPassengers, double avgPassengers) {
            this.totalPassengers = totalPassengers;
            this.avgPassengers = avgPassengers;
        }
    }

    private final File file;
    private final HashMap<String, DistrictStats> dateMap;
    private final HashMap<String, Integer> dateCountMap;
    private final HashMap<String, DistrictStats> districtMap;
    private final HashMap<String, Integer> districtCountMap;
    private final ReentrantLock dateLock;
    private final ReentrantLock districtLock;
    private final boolean lockBothAtOnce;

    public TransportStats(File file,
                          HashMap<String, DistrictStats> dateMap,
                          HashMap<String, Integer> dateCountMap,
                          HashMap<String, DistrictStats> districtMap,
                          HashMap<String, Integer> districtCountMap,
                          ReentrantLock dateLock,
                          ReentrantLock districtLock,
                          boolean lockBothAtOnce) {
        this.file             = file;
        this.dateMap          = dateMap;
        this.dateCountMap     = dateCountMap;
        this.districtMap      = districtMap;
        this.districtCountMap = districtCountMap;
        this.dateLock         = dateLock;
        this.districtLock     = districtLock;
        this.lockBothAtOnce   = lockBothAtOnce;
    }

    @Override
    public void run() {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length < 4) continue;

                String date     = parts[0].trim();
                String district = parts[1].trim();
                int passengers  = Integer.parseInt(parts[3].trim());

                if (lockBothAtOnce) {
                    // Version A: lock both HashMaps at the same time
                    dateLock.lock();
                    districtLock.lock();
                    try {
                        updateMap(dateMap, dateCountMap, date, passengers);
                        updateMap(districtMap, districtCountMap, district, passengers);
                    } finally {
                        districtLock.unlock();
                        dateLock.unlock();
                    }
                } else {
                    // Version B: lock one HashMap, process it, unlock it, then lock the next one
                    dateLock.lock();
                    try {
                        updateMap(dateMap, dateCountMap, date, passengers);
                    } finally {
                        dateLock.unlock();
                    }

                    districtLock.lock();
                    try {
                        updateMap(districtMap, districtCountMap, district, passengers);
                    } finally {
                        districtLock.unlock();
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Error reading file: " + file.getName() + " - " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Invalid number in file: " + file.getName() + " - " + e.getMessage());
        }
    }

    private static void updateMap(HashMap<String, DistrictStats> map,
                                  HashMap<String, Integer> countMap,
                                  String key, int passengers) {
        DistrictStats existing = map.get(key);
        if (existing == null) {
            map.put(key, new DistrictStats(passengers, 0));
            countMap.put(key, 1);
        } else {
            map.put(key, new DistrictStats(existing.totalPassengers + passengers, 0));
            countMap.put(key, countMap.get(key) + 1);
        }
    }

    public static void main(String[] args) throws InterruptedException {

        File dataDir = new File("Lab/SE375_LAB/SE375_LAB_DATA");
        File[] allFiles = dataDir.listFiles();

        List<File> csvList = new ArrayList<>();
        if (allFiles != null) {
            for (File f : allFiles) {
                if (f.getName().toLowerCase().endsWith(".csv")) {
                    csvList.add(f);
                }
            }
        }

        if (csvList.isEmpty()) {
            System.out.println("No CSV data files found in: " + dataDir.getPath());
            return;
        }

        // ── Run Version A and Version B once to print results ──────────────
        System.out.println("===== Version A: Lock both HashMaps at once =====");
        runVersion(csvList, true, true);

        System.out.println("\n===== Version B: Lock one HashMap at a time =====");
        runVersion(csvList, false, true);

        // ── Benchmark both versions over 50 runs ───────────────────────────
        final int RUNS = 50;
        System.out.println("\n--- Benchmarking " + RUNS + " runs ---");

        long totalA = 0;
        for (int i = 0; i < RUNS; i++) totalA += runVersion(csvList, true,  false);

        long totalB = 0;
        for (int i = 0; i < RUNS; i++) totalB += runVersion(csvList, false, false);

        System.out.printf("%nVersion A avg: %.3f ms%n", (double) totalA / RUNS / 1_000_000.0);
        System.out.printf("Version B avg: %.3f ms%n",  (double) totalB / RUNS / 1_000_000.0);
    }

    static long runVersion(List<File> csvList, boolean lockBothAtOnce, boolean printResults)
            throws InterruptedException {

        HashMap<String, DistrictStats> dateMap          = new LinkedHashMap<>();
        HashMap<String, Integer>       dateCountMap     = new LinkedHashMap<>();
        HashMap<String, DistrictStats> districtMap      = new LinkedHashMap<>();
        HashMap<String, Integer>       districtCountMap = new LinkedHashMap<>();
        ReentrantLock dateLock     = new ReentrantLock();
        ReentrantLock districtLock = new ReentrantLock();

        ExecutorService pool = Executors.newFixedThreadPool(10);

        long startTime = System.nanoTime();

        for (File file : csvList) {
            pool.submit(new TransportStats(
                    file,
                    dateMap, dateCountMap,
                    districtMap, districtCountMap,
                    dateLock, districtLock,
                    lockBothAtOnce));
        }

        pool.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        long endTime = System.nanoTime();
        long elapsedTime = endTime - startTime;

        if (printResults) {
            System.out.println("Threads are complete. Here are the statistics for the number of passengers:");
            System.out.println("\n-- By Date --");
            printResults(dateMap, dateCountMap);
            System.out.println("\n-- By District --");
            printResults(districtMap, districtCountMap);
        }

        return elapsedTime;
    }

    static void printResults(HashMap<String, DistrictStats> resultsMap, HashMap<String, Integer> countMap) {
        System.out.println();
        System.out.printf("%-15s %-20s %-15s%n", "Date", "Total Passengers", "Avg Passengers");
        System.out.printf("%-15s %-20s %-15s%n", "----", "----------------", "--------------");

        for (Map.Entry<String, DistrictStats> entry : resultsMap.entrySet()) {
            String key = entry.getKey();
            DistrictStats stats = entry.getValue();
            int count = countMap.getOrDefault(key, 1);
            double avg = (count > 0) ? (double) stats.totalPassengers / count : 0.0;
            System.out.printf("%-15s %-20d %-15.2f%n", key, stats.totalPassengers, avg);
        }
    }
}