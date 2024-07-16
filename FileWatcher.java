import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;

public class FileWatcher {

    private static final String WATCH_DIR = "/path/to/watched/directory"; // Replace with your directory path
    private static final int[] COLUMN_LENGTHS = {10, 20, 30}; // Example column lengths

    public static void main(String[] args) throws IOException, InterruptedException {
        // Obtain the directory to watch
        Path dir = Paths.get(WATCH_DIR);

        // Initialize WatchService and register directory
        WatchService watcher = FileSystems.getDefault().newWatchService();
        dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);

        System.out.println("Watching directory: " + dir);

        // Process events indefinitely
        while (true) {
            WatchKey key;
            try {
                // Wait for the next event
                key = watcher.take();
            } catch (InterruptedException e) {
                return;
            }

            // Process all events in the key
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                // Handle overflow event
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                // The filename is the context of the event
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path filename = ev.context();
                Path child = dir.resolve(filename);

                // Read the file content
                try (BufferedReader reader = new BufferedReader(new FileReader(child.toFile()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Process each line according to column lengths
                        processLine(line);
                    }
                } catch (IOException e) {
                    System.err.println("Error reading file: " + e.getMessage());
                }
            }

            // Reset the key to receive further notifications
            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
    }

    private static void processLine(String line) {
        // Assuming COLUMN_LENGTHS array defines lengths for each column
        int startIndex = 0;
        for (int length : COLUMN_LENGTHS) {
            if (startIndex >= line.length()) {
                break; // If line length is shorter than expected
            }
            String columnData = line.substring(startIndex, Math.min(startIndex + length, line.length()));
            System.out.println("Column data: " + columnData);
            startIndex += length;
        }
    }
}
