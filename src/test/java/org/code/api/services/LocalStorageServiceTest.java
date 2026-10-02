package org.code.api.services;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Checks storage containment and generated names independently of user filenames.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
class LocalStorageServiceTest {
  @TempDir Path root;

  /** Verifies that untrusted filename cannot escape storage directory. */
  @Test
  void untrustedFilenameCannotEscapeStorageDirectory() throws Exception {
    LocalStorageServiceImpl storage = new LocalStorageServiceImpl(root.toString());
    Path file =
        Path.of(
            storage.store(
                new MockMultipartFile(
                    "file", "../../outside.txt", "text/plain", "content".getBytes())));
    assertThat(file.getParent()).isEqualTo(root.toAbsolutePath());
    assertThat(file.getFileName().toString()).doesNotContain("outside", "/", "\\");
    assertThat(Files.readString(file)).isEqualTo("content");
    storage.delete(file.toString());
    assertThat(file).doesNotExist();
  }

  /** Verifies that deletion outside configured directory is rejected. */
  @Test
  void deletionOutsideConfiguredDirectoryIsRejected() {
    LocalStorageServiceImpl storage =
        new LocalStorageServiceImpl(root.resolve("uploads").toString());
    assertThatThrownBy(() -> storage.delete(root.resolve("unrelated.txt").toString()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
