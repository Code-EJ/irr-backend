package org.code.api.domain.exception;

import lombok.Getter;

import java.util.UUID;

public class DonationError extends RuntimeException {

    public DonationError(String message) {
        super(message);
    }

    @Getter
    public static class NotFound extends DonationError {
        private final UUID donationId;

        public NotFound(UUID donationId) {
            super("Donation not found");
            this.donationId = donationId;
        }
    }

    public static class InactiveDonation extends DonationError {
        public InactiveDonation(UUID id) {
            super(String.format("Donation with ID %s is inactive and cannot be modified.", id));
        }
    }

    public static class EmptyInputItems extends DonationError {
        public EmptyInputItems() {
            super("A donation must have at least one input item.");
        }
    }

    @Getter
    public static class AttachmentNotFound extends DonationError {
        private final UUID attachmentId;

        public AttachmentNotFound(UUID attachmentId) {
            super("Proof attachment not found");
            this.attachmentId = attachmentId;
        }
    }
}