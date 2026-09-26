package org.code.api.infrastructure.storage;

import org.code.api.services.AttachmentCleanupService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Polls committed attachment cleanup jobs without coupling HTTP requests to deletion retries.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "irr.attachments.cleanup-enabled", havingValue = "true", matchIfMissing = true)
public class AttachmentCleanupScheduler {
    private final AttachmentCleanupService cleanup;
    /** @param cleanup durable deletion use case */
    public AttachmentCleanupScheduler(AttachmentCleanupService cleanup) { this.cleanup = cleanup; }
    /** Retries a bounded batch after each polling delay. */
    @Scheduled(fixedDelayString = "${irr.attachments.cleanup-delay-ms:60000}", initialDelayString = "${irr.attachments.cleanup-delay-ms:60000}")
    public void run() { cleanup.processPending(); }
}
