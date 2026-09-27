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
    private final org.code.api.services.AttachmentOrphanReconciler orphans;
    /** @param cleanup durable deletion use case */
    public AttachmentCleanupScheduler(AttachmentCleanupService cleanup, org.code.api.services.AttachmentOrphanReconciler orphans) { this.cleanup = cleanup; this.orphans=orphans; }
    /** Retries a bounded batch after each polling delay. */
    @Scheduled(fixedDelayString = "${irr.attachments.cleanup-delay-ms:60000}", initialDelayString = "${irr.attachments.cleanup-delay-ms:60000}")
    public void run() { cleanup.processPending(); }
    /** Periodically queues old crash orphans; current metadata and in-flight uploads are protected. */
    @Scheduled(fixedDelayString="${irr.attachments.orphan-delay-ms:3600000}",initialDelayString="${irr.attachments.orphan-delay-ms:3600000}")
    public void reconcile() throws java.io.IOException { orphans.reconcile(); }
}
