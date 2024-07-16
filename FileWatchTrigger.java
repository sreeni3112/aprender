import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;

@Service
public class FileWatcherService {

    @Value("${watch.directory}")
    private String directoryPath;

    @PostConstruct
    public void init() throws IOException {
        Path path = Paths.get(directoryPath);
        WatchService watchService = FileSystems.getDefault().newWatchService();

        path.register(
            watchService,
            StandardWatchEventKinds.ENTRY_MODIFY
        );

        new Thread(() -> {
            while (true) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException e) {
                    return;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();

                    System.out.println("File modified: " + filename);
                    // Call your method to handle the file change event
                    handleFileChange(filename);
                }

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        }).start();
    }

    private void handleFileChange(Path filename) {
        // Implement your logic to handle the file change event here
        // Example: you can call a service method to process the file
        // ExampleService.processFile(filename);
        System.out.println("Handling file change: " + filename);
    }
}
