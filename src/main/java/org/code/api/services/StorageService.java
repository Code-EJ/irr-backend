package org.code.api.services;

import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

/**
 * Boundary for persistent attachment bytes, separate from database metadata.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public interface StorageService {
    /**
     * Stores bytes using a server-generated name.
     * @param file uploaded bytes; its original name is metadata only
     * @return absolute storage path used by the current legacy metadata model
     * @throws IOException if storage fails
     */
    String store(MultipartFile file) throws IOException;

    /**
     * Removes bytes belonging to the adapter's configured storage directory.
     * @param storedPath a path previously returned by this adapter
     * @throws IllegalArgumentException if the path is outside that directory
     */
    void delete(String storedPath);
}
