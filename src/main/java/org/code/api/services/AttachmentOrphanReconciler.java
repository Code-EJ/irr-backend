package org.code.api.services;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Recovers crash-orphaned UUID files older than 24 hours through the durable deletion queue.
 * Exclusive PostgreSQL advisory locking excludes in-flight uploads, which hold shared locks.
 * Database or filesystem errors fail closed; this use case never directly deletes bytes.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Service
public class AttachmentOrphanReconciler {
    private final JdbcTemplate jdbc;
    private final AttachmentCleanupService cleanup;
    private final Path directory;
    private String cursor="";
    /** Configures the exclusive attachment storage directory and durable cleanup boundary. */
    public AttachmentOrphanReconciler(JdbcTemplate jdbc,AttachmentCleanupService cleanup,@Value("${irr.storage.directory}") String directory) {
        this.jdbc=jdbc;this.cleanup=cleanup;this.directory=Path.of(directory).toAbsolutePath().normalize();
    }
    /**
     * Checks at most 500 candidate names per call, continuing from a process-local lexical cursor.
     * Restarts may rescan candidates; queue uniqueness makes repeated observations harmless.
     * @return number of orphan candidates submitted for durable cleanup
     * @throws IOException when safe directory inspection is unavailable
     */
    @Transactional
    public synchronized int reconcile() throws IOException {
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT pg_try_advisory_xact_lock(hashtext('irr-attachment-storage'))",Boolean.class))) return 0;
        if(!Files.exists(directory,LinkOption.NOFOLLOW_LINKS)) return 0;
        if(Files.isSymbolicLink(directory)) throw new IOException("Attachment directory must not be a symbolic link");
        java.util.List<Path> candidates;
        try(var paths=Files.list(directory)) {
            candidates=paths.filter(path->path.getFileName().toString().compareTo(cursor)>0)
                .filter(path->isGeneratedName(path.getFileName().toString()))
                .sorted(Comparator.comparing(path->path.getFileName().toString())).limit(500).toList();
        }
        if(candidates.isEmpty()) { cursor="";return 0; }
        Instant cutoff=Instant.now().minus(24,ChronoUnit.HOURS);int queued=0;
        for(Path path:candidates) {
            if(Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS) && Files.getLastModifiedTime(path,LinkOption.NOFOLLOW_LINKS).toInstant().isBefore(cutoff)) {
                boolean referenced=Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM attachment WHERE storage_url=?)",Boolean.class,path.toString()));
                if(!referenced) { cleanup.enqueue(path.toString());queued++; }
            }
        }
        cursor=candidates.getLast().getFileName().toString();return queued;
    }
    private boolean isGeneratedName(String name) {
        try { return UUID.fromString(name).toString().equals(name); } catch(IllegalArgumentException error) { return false; }
    }
}
