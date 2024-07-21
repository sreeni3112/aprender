import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class FileWatcherService1 {

    private final RedisTemplate<String, String> redisTemplate;
    private final String LOCK_KEY_PREFIX = "file_processing_lock:";
    private final String WATCH_DIRECTORY = "/path/to/watched/directory"; // Replace with your directory path

    private final AtomicBoolean processing = new AtomicBoolean(false);

    @Autowired
    public FileWatcherService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(fixedDelay = 10000) // Example: Polling every 10 seconds
    public void watchDirectory() {
        if (processing.compareAndSet(false, true)) {
            try {
                File directory = new File(WATCH_DIRECTORY);
                File[] files = directory.listFiles();

                if (files != null) {
                    List<File> filesToProcess = new ArrayList<>();

                    for (File file : files) {
                        if (file.isFile() && file.getName().endsWith(".txt")) { // Example filter for text files
                            filesToProcess.add(file);
                        }
                    }

                    distributeFiles(filesToProcess);
                }
            } finally {
                processing.set(false);
            }
        }
    }

    private void distributeFiles(List<File> files) {
        for (File file : files) {
            if (tryLock(file.getName())) {
                processFile(file);
            }
        }
    }

    private void processFile(File file) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(file.getAbsolutePath()));
            System.out.println("File content of " + file.getName() + ":");
            lines.forEach(System.out::println);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            releaseLock(file.getName());
        }
    }

    private boolean tryLock(String fileName) {
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY_PREFIX + fileName, "locked", Duration.ofMinutes(1));
        return locked != null && locked;
    }

    private void releaseLock(String fileName) {
        redisTemplate.delete(LOCK_KEY_PREFIX + fileName);
    }
}