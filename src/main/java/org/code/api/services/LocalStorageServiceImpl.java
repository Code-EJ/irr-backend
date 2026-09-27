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
   *
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
    } catch (IOException exception) {
      try {
        Files.deleteIfExists(destination);
      } catch (IOException cleanup) {
        exception.addSuppressed(cleanup);
      }
      throw exception;
    }
    return destination.toString();
  }

  /** {@inheritDoc} */
  @Override
  public byte[] read(String storedPath) throws IOException {
    Path target = resolve(storedPath);
    if (Files.size(target) > 10 * 1024 * 1024)
      throw new IOException("Stored attachment exceeds the read limit");
    return Files.readAllBytes(target);
  }

  private Path resolve(String storedPath) {
    Path target = Path.of(storedPath).toAbsolutePath().normalize();
    if (!directory.equals(target.getParent()) || Files.isSymbolicLink(target)) {
      throw new IllegalArgumentException("Storage path is outside the configured directory");
    }
    return target;
  }

  /** {@inheritDoc} */
  @Override
  public void delete(String storedPath) {
    Path target = resolve(storedPath);
    try {
      Files.deleteIfExists(target);
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot delete stored attachment", exception);
    }
  }
}
