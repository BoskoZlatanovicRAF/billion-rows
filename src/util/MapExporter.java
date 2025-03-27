package util;


import core.StationData;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class MapExporter {
    private static final Object exportLock = new Object(); // da ne bi bilo konflikta sa periodicnim izvestajem

    public static void exportToCsv(Map<Character, StationData> mapSnapshot, String path) {
        synchronized (exportLock) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
                writer.write("Letter,Station count,Sum");
                writer.newLine();

                for (Map.Entry<Character, StationData> entry : mapSnapshot.entrySet()) {
                    char key = entry.getKey();

                    if (!Character.toString(key).toLowerCase().matches("[a-z]")) {
                        continue;
                    }
                    StationData data = entry.getValue();
                    writer.write(String.format("%c,%d,%.2f", key, data.getCount(), data.getSum()));
                    writer.newLine();
                }

                System.out.println("Map exported to " + path);

            } catch (IOException e) {
                System.out.println("Failed to export map: " + e.getMessage());
            }
        }
    }

    public static Object getExportLock() {
        return exportLock;
    }
}
