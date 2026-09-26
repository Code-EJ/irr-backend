package org.code.api.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores attachment bytes under generated names in a configured persistent directory.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Service
public class LocalStorageServiceImpl implements StorageService {
    private final Path directory;
    /**
     * Configures the persistent storage root.
     * @param directory filesystem directory for uploaded bytes
     */
    public LocalStorageServiceImpl(@Value("${irr.storage.directory}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }
    /** {@inheritDoc} */
    @Override
    public String store(MultipartFile file) throws IOException {
        Files.createDirectories(directory);
        Path destination = directory.resolve(UUID.randomUUID().toString());
        try (InputStream input = file.getInputStream()) {
            Files.copy(input, destination);
        }
        return destination.toString();
    }
    /** {@inheritDoc} */
    @Override
    public void delete(String storedPath) {
        Path target = Path.of(storedPath).toAbsolutePath().normalize();
        if (!directory.equals(target.getParent())) {
            throw new IllegalArgumentException("Storage path is outside the configured directory");
        }
        try { Files.deleteIfExists(target); }
        catch (IOException exception) { throw new IllegalStateException("Cannot delete stored attachment", exception); }
    }
}
