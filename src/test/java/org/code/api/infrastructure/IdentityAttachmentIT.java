package org.code.api.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.code.api.domain.enums.UserRole;
import org.code.api.domain.models.user.Session;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.TokenPort;
import org.code.api.domain.ports.EncryptionPort;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.code.api.infrastructure.repositories.UserRepository;
import org.code.api.services.DocumentService;
import org.code.api.services.AttachmentCleanupService;
import org.code.api.services.StorageService;
import org.code.api.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Exercises real signatures, PostgreSQL transactions, HTTP authorization and file lifecycle.
 * Each account and attachment is isolated by a generated identity; storage is temporary.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@AutoConfigureMockMvc
class IdentityAttachmentIT extends PostgresIntegrationTest {
    private static final Path UPLOADS = uploads();
    private static final byte[] PDF = "%PDF-1.7 fixture".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired TokenPort tokens;
    @Autowired EncryptionPort encryption;
    @Autowired DocumentService documents;
    @Autowired AttachmentCleanupService cleanup;
    @Autowired StorageService storage;
    @Autowired AuthenticatedUserProvider actors;
    @Autowired PlatformTransactionManager transactions;

    /** @param registry isolated attachment storage binding */
    @DynamicPropertySource static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("irr.storage.directory", UPLOADS::toString);
    }
    /** Verifies provisioning hashes credentials and never returns a new account token. */
    @Test void administratorProvisionsSelectedPartnerRoles() throws Exception {
        String admin = token(account(UserRole.ADMINISTRATOR));
        for (UserRole role : List.of(UserRole.CITY_HALL, UserRole.ORGANIZATION, UserRole.REPRESENTATIVE)) {
            String email = UUID.randomUUID() + "@example.test";
            mvc.perform(post("/api/users").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content(partner(email, role))).andExpect(status().isCreated()).andExpect(jsonPath("userRole").value(role.name()))
                .andExpect(jsonPath("passwordHash").doesNotExist()).andExpect(jsonPath("password").doesNotExist()).andExpect(jsonPath("token").doesNotExist());
            assertThat(encryption.compare(users.findByEmail(email).orElseThrow().getPasswordHash(), "fixture-password")).isTrue();
        }
    }
    /** Verifies anonymous and nonadministrator actors cannot create partners or elevate a role. */
    @Test void provisioningRejectsUnauthorizedAndAdministratorTargets() throws Exception {
        String body = partner(UUID.randomUUID() + "@example.test", UserRole.REPRESENTATIVE);
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        for (UserRole role : List.of(UserRole.CITY_HALL, UserRole.ORGANIZATION, UserRole.REPRESENTATIVE))
            mvc.perform(post("/api/users").header("Authorization", token(account(role))).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/users").header("Authorization", token(account(UserRole.ADMINISTRATOR)))
            .contentType(MediaType.APPLICATION_JSON).content(partner(UUID.randomUUID() + "@example.test", UserRole.ADMINISTRATOR))).andExpect(status().isBadRequest());
    }
    /** Verifies duplicates and BCrypt byte boundaries fail without creating records. */
    @Test void provisioningRejectsDuplicateEmailAndOversizedUtf8Password() throws Exception {
        User existing = account(UserRole.REPRESENTATIVE);
        String admin = token(account(UserRole.ADMINISTRATOR));
        mvc.perform(post("/api/users").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
            .content(partner(existing.getEmail(), UserRole.REPRESENTATIVE))).andExpect(status().isConflict());
        String body = json.writeValueAsString(Map.of("fullName", "Partner", "email", UUID.randomUUID() + "@example.test", "password", "é".repeat(36), "userRole", "REPRESENTATIVE"));
        mvc.perform(post("/api/users").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }
    /** Verifies the retired route cannot provision accounts and no session-prefix bypass remains. */
    @Test void registrationIsRetiredAndSessionPrefixIsProtected() throws Exception {
        String actor = token(account(UserRole.REPRESENTATIVE));
        long count = users.count();
        mvc.perform(post("/api/session/register")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/session/register").header("Authorization", actor).header("X-Organization-Id", tokenOrganizations.get(actor))).andExpect(status().isGone());
        mvc.perform(get("/api/session/unknown")).andExpect(status().isUnauthorized());
        assertThat(users.count()).isEqualTo(count);
    }
    /** Verifies changed roles and disabled users take effect before token expiration. */
    @Test void currentDatabaseIdentityOverridesStaleTokenAndContextDoesNotLeak() throws Exception {
        User admin = account(UserRole.ADMINISTRATOR);
        String bearer = token(admin);
        jdbc.update("UPDATE users SET user_role='REPRESENTATIVE' WHERE id=?", admin.getId());
        mvc.perform(post("/api/users").header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
            .content(partner(UUID.randomUUID() + "@example.test", UserRole.ORGANIZATION))).andExpect(status().isForbidden());
        jdbc.update("UPDATE users SET is_active=false WHERE id=?", admin.getId());
        mvc.perform(get("/api/vehicles").header("Authorization", bearer)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/vehicles")).andExpect(status().isUnauthorized());
    }
    /** Verifies error responses never echo bearer tokens or reveal account existence. */
    @Test void authenticationErrorsAreGeneric() throws Exception {
        mvc.perform(get("/api/vehicles").header("Authorization", "Bearer invalid-secret-token"))
            .andExpect(status().isUnauthorized()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("invalid-secret-token"))));
        User user = account(UserRole.REPRESENTATIVE);
        String known = mvc.perform(post("/api/session/authenticate").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email", user.getEmail(), "password", "wrong-password"))))
            .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String unknown = mvc.perform(post("/api/session/authenticate").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email", "unknown-" + UUID.randomUUID() + "@example.test", "password", "wrong-password"))))
            .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertThat(known).isEqualTo(unknown);
    }
    /** Verifies explicit CORS origin allowlisting and public Swagger security declarations. */
    @Test void corsAndSwaggerMatchRuntimePolicy() throws Exception {
        mvc.perform(options("/api/users").header("Origin", "http://localhost:5173").header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "authorization,content-type")).andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/users").header("Origin", "https://untrusted.example").header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isForbidden());
        var api = json.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(api.at("/paths/~1api~1session~1authenticate/post/security").isEmpty()).isTrue();
        assertThat(api.at("/paths/~1api~1session~1register/post/deprecated").asBoolean()).isTrue();
        assertThat(api.at("/paths/~1api~1documents/post/responses/201/content/application~1json/schema/$ref").asText()).contains("AttachmentResponse");
    }
    /** Verifies current authorities are exposed through the use-case actor port. */
    @Test void actorPortExposesUuidAndCurrentRoles() {
        User user = account(UserRole.ORGANIZATION);
        identify(user);
        try {
            assertThat(actors.getCurrentUserId()).isEqualTo(user.getId());
            assertThat(actors.getCurrentUserRoles()).containsExactly(UserRole.ORGANIZATION);
        } finally { SecurityContextHolder.clearContext(); org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes(); }
    }
    /** Verifies even another administrator cannot read or remove a creator's attachment. */
    @Test void attachmentOwnershipAndSafeMetadataAreEnforced() throws Exception {
        String owner = token(account(UserRole.REPRESENTATIVE));
        UUID id = upload(owner);
        mvc.perform(get("/api/documents/{id}/download", id).header("Authorization", owner).header("X-Organization-Id", tokenOrganizations.get(owner))).andExpect(status().isOk()).andExpect(content().bytes(PDF));
        for (UserRole role : List.of(UserRole.ADMINISTRATOR, UserRole.REPRESENTATIVE)) {
            String foreign = token(account(role));
            mvc.perform(get("/api/documents/{id}/download", id).header("Authorization", foreign).header("X-Organization-Id", tokenOrganizations.get(foreign))).andExpect(status().isNotFound());
            mvc.perform(delete("/api/documents/{id}", id).header("Authorization", foreign).header("X-Organization-Id", tokenOrganizations.get(foreign))).andExpect(status().isNotFound());
        }
    }
    /** Verifies signatures and display filenames are validated before writing any bytes. */
    @Test void unsupportedContentAndUnsafeNamesAreRejected() throws Exception {
        String actor = token(account(UserRole.REPRESENTATIVE));
        mvc.perform(multipart("/api/documents").file(new MockMultipartFile("documento", "payload.pdf", "application/pdf", "<script>".getBytes()))
            .header("Authorization", actor).header("X-Organization-Id", tokenOrganizations.get(actor))).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/documents").file(new MockMultipartFile("documento", "../payload.pdf", "application/pdf", PDF))
            .header("Authorization", actor).header("X-Organization-Id", tokenOrganizations.get(actor))).andExpect(status().isBadRequest());
    }
    /** Verifies metadata commits first and committed cleanup removes the physical file. */
    @Test void deletionIsDurableAndIdempotent() throws Exception {
        String actor = token(account(UserRole.REPRESENTATIVE));
        UUID id = upload(actor);
        String path = stored(id);
        mvc.perform(delete("/api/documents/{id}", id).header("Authorization", actor).header("X-Organization-Id", tokenOrganizations.get(actor))).andExpect(status().isAccepted());
        assertThat(Files.exists(Path.of(path))).isTrue();
        assertThat(queueCount(path)).isEqualTo(1);
        cleanup.processPending();
        assertThat(Files.exists(Path.of(path))).isFalse();
        assertThat(queueCount(path)).isZero();
        cleanup.processPending();
    }
    /** Verifies relational references preserve both metadata and bytes on a rejected deletion. */
    @Test void referencedAttachmentCannotBeDeleted() throws Exception {
        User owner = account(UserRole.REPRESENTATIVE);
        String actor = token(owner);
        UUID id = upload(actor);
        String path = stored(id);
        UUID donor = UUID.randomUUID();
        jdbc.update("INSERT INTO donor(id,name,document,donor_type,creator_id) VALUES (?,'Fixture','000','PF',?)", donor, owner.getId());
        jdbc.update("INSERT INTO donation(total_weight_kg,donor_id,proof_attachment_id,creator_id) VALUES (1,?,?,?)", donor, id, owner.getId());
        mvc.perform(delete("/api/documents/{id}", id).header("Authorization", actor).header("X-Organization-Id", tokenOrganizations.get(actor))).andExpect(status().isConflict());
        assertThat(stored(id)).isEqualTo(path);
        assertThat(Files.exists(Path.of(path))).isTrue();
        assertThat(queueCount(path)).isZero();
    }
    /** Verifies rolled-back deletion never schedules a physical operation. */
    @Test void deletionRollbackPreservesBytesAndMetadata() throws Exception {
        User owner = account(UserRole.REPRESENTATIVE);
        UUID id = upload(token(owner));
        String path = stored(id);
        identify(owner);
        try {
            new TransactionTemplate(transactions).executeWithoutResult(tx -> { documents.delete(id); tx.setRollbackOnly(); });
        } finally { SecurityContextHolder.clearContext(); org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes(); }
        assertThat(stored(id)).isEqualTo(path);
        assertThat(Files.exists(Path.of(path))).isTrue();
        assertThat(queueCount(path)).isZero();
    }
    /** Verifies rolled-back upload compensates its filesystem write. */
    @Test void uploadRollbackRemovesWrittenBytes() throws Exception {
        User owner = account(UserRole.REPRESENTATIVE);
        String[] path = new String[1];
        identify(owner);
        try {
            new TransactionTemplate(transactions).executeWithoutResult(tx -> {
                try { path[0] = stored(documents.upload(pdf()).id()); }
                catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
                tx.setRollbackOnly();
            });
        } finally { SecurityContextHolder.clearContext(); org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes(); }
        assertThat(Files.exists(Path.of(path[0]))).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attachment WHERE storage_url=?", Integer.class, path[0])).isZero();
    }
    /** Verifies failed deletion remains durable and succeeds after the storage fault is corrected. */
    @Test void failedCleanupRemainsQueuedForRetry() throws Exception {
        Path outside = Files.createTempFile("irr-outside-", ".pdf");
        try {
            UUID job = UUID.randomUUID();
            jdbc.update("INSERT INTO attachment_file_deletion(id,storage_path) VALUES (?,?)", job, outside.toString());
            cleanup.processPending();
            assertThat(Files.exists(outside)).isTrue();
            assertThat(jdbc.queryForObject("SELECT attempts FROM attachment_file_deletion WHERE id=?", Integer.class, job)).isEqualTo(1);
            String valid = storage.store(pdf());
            jdbc.update("UPDATE attachment_file_deletion SET storage_path=?, next_attempt_at=CURRENT_TIMESTAMP WHERE id=?", valid, job);
            cleanup.processPending();
            assertThat(Files.exists(Path.of(valid))).isFalse();
            assertThat(queueCount(valid)).isZero();
            assertThatThrownBy(() -> storage.read(outside.toString())).isInstanceOf(IllegalArgumentException.class);
        } finally { Files.deleteIfExists(outside); }
    }
    /** Organization members share evidence, but only the uploader or a manager can rename or delete it. */
    @Test void attachmentMetadataAndSharedAccessRespectManagerBoundary() throws Exception {
        User owner=account(UserRole.REPRESENTATIVE),member=account(UserRole.REPRESENTATIVE);
        String ownerToken=token(owner),memberToken=token(member); UUID id=upload(ownerToken);
        jdbc.update("INSERT INTO organization_membership(organization_id,user_id,role,granted_by) VALUES (?,?,'MEMBER',?)",owner.getId(),member.getId(),owner.getId());
        mvc.perform(get("/api/v1/documents/{id}",id).header("Authorization",memberToken).header("X-Organization-Id",owner.getId())).andExpect(status().isOk()).andExpect(jsonPath("storageUrl").doesNotExist());
        mvc.perform(get("/api/v1/documents").header("Authorization",memberToken).header("X-Organization-Id",owner.getId())).andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(1));
        String body=json.writeValueAsString(Map.of("fileName","Renamed.pdf"));
        mvc.perform(put("/api/v1/documents/{id}",id).header("Authorization",memberToken).header("X-Organization-Id",owner.getId()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/documents/{id}",id).header("Authorization",memberToken).header("X-Organization-Id",owner.getId())).andExpect(status().isForbidden());
        jdbc.update("UPDATE organization_membership SET role='MANAGER' WHERE organization_id=? AND user_id=?",owner.getId(),member.getId());
        mvc.perform(put("/api/v1/documents/{id}",id).header("Authorization",memberToken).header("X-Organization-Id",owner.getId()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(jsonPath("fileName").value("Renamed.pdf"));
        mvc.perform(delete("/api/v1/documents/{id}",id).header("Authorization",memberToken).header("X-Organization-Id",owner.getId())).andExpect(status().isAccepted());
    }
    /** Partner CRUD does not expose secrets or allow administrator creation, modification or lockout. */
    @Test void partnerLifecyclePreservesAdministratorBoundaryAndRevokesDisabledAccess() throws Exception {
        User admin=account(UserRole.ADMINISTRATOR),partner=account(UserRole.REPRESENTATIVE);
        String adminToken=token(admin),partnerToken=token(partner);
        mvc.perform(get("/api/users/me").header("Authorization",partnerToken)).andExpect(status().isOk()).andExpect(jsonPath("passwordHash").doesNotExist());
        mvc.perform(get("/api/users").header("Authorization",partnerToken)).andExpect(status().isForbidden());
        String update=json.writeValueAsString(Map.of("fullName","Updated Partner","email",partner.getEmail(),"userRole","CITY_HALL"));
        mvc.perform(put("/api/users/{id}",partner.getId()).header("Authorization",adminToken).contentType(MediaType.APPLICATION_JSON).content(update)).andExpect(status().isOk()).andExpect(jsonPath("userRole").value("CITY_HALL"));
        mvc.perform(get("/api/users/me").header("Authorization",partnerToken)).andExpect(status().isOk()).andExpect(jsonPath("userRole").value("CITY_HALL"));
        mvc.perform(put("/api/users/{id}",admin.getId()).header("Authorization",adminToken).contentType(MediaType.APPLICATION_JSON).content(update)).andExpect(status().isConflict());
        mvc.perform(delete("/api/users/{id}",admin.getId()).header("Authorization",adminToken)).andExpect(status().isConflict());
        mvc.perform(delete("/api/users/{id}",partner.getId()).header("Authorization",adminToken)).andExpect(status().isNoContent());
        mvc.perform(get("/api/users/me").header("Authorization",partnerToken)).andExpect(status().isUnauthorized());
    }
    @Autowired org.code.api.services.AttachmentOrphanReconciler orphans;
    /** Recovery queues only old generated files without metadata; referenced and recent bytes survive. */
    @Test void orphanReconciliationPreservesReferencedRecentAndUnrelatedFiles() throws Exception {
        String orphan=storage.store(pdf()),recent=storage.store(pdf());
        UUID ownedId=upload(token(account(UserRole.REPRESENTATIVE)));String owned=stored(ownedId);
        var old=java.nio.file.attribute.FileTime.from(java.time.Instant.now().minus(48,java.time.temporal.ChronoUnit.HOURS));
        Files.setLastModifiedTime(Path.of(orphan),old);Files.setLastModifiedTime(Path.of(owned),old);
        Path unrelated=UPLOADS.resolve("operator-note.txt");Files.writeString(unrelated,"Not an attachment");Files.setLastModifiedTime(unrelated,old);
        try {
            orphans.reconcile();orphans.reconcile();
            assertThat(queueCount(orphan)).isEqualTo(1);assertThat(queueCount(owned)).isZero();assertThat(queueCount(recent)).isZero();
            cleanup.processPending();
            assertThat(Files.exists(Path.of(orphan))).isFalse();assertThat(Files.exists(Path.of(owned))).isTrue();assertThat(Files.exists(Path.of(recent))).isTrue();assertThat(Files.exists(unrelated)).isTrue();
        } finally { Files.deleteIfExists(unrelated);Files.deleteIfExists(Path.of(recent)); }
    }
    /** Exclusive reconciliation never races an upload holding the shared transaction lock. */
    @Test void orphanReconciliationSkipsInFlightUploads() throws Exception {
        var entered=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
        try(var pool=java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var future=pool.submit(()->new TransactionTemplate(transactions).executeWithoutResult(tx->{
                jdbc.execute("SELECT pg_advisory_xact_lock_shared(hashtext('irr-attachment-storage'))");entered.countDown();
                try { if(!release.await(10,java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Upload fixture timed out"); }
                catch(InterruptedException error) { Thread.currentThread().interrupt();throw new IllegalStateException(error); }
            }));
            try { assertThat(entered.await(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();assertThat(orphans.reconcile()).isZero(); }
            finally { release.countDown(); }
            future.get(10,java.util.concurrent.TimeUnit.SECONDS);
        }
    }
    private final java.util.Map<String,UUID> tokenOrganizations=new java.util.HashMap<>();
    private User account(UserRole role) {
        User user=users.saveAndFlush(User.builder().email(UUID.randomUUID() + "@example.test").fullName("Fixture")
            .passwordHash(encryption.encrypt("fixture-password")).userRole(role).build());
        jdbc.update("INSERT INTO organization(id,name,organization_type,created_by) VALUES (?,'Fixture','Association',?)",user.getId(),user.getId());
        jdbc.update("INSERT INTO organization_membership(organization_id,user_id,role,granted_by) VALUES (?,?,'MEMBER',?)",user.getId(),user.getId(),user.getId());
        return user;
    }
    private String token(User user) {
        String bearer="Bearer " + tokens.createToken(Session.builder().id(user.getId()).email(user.getEmail()).userRole(user.getUserRole()).build());
        tokenOrganizations.put(bearer,user.getId());
        return bearer;
    }
    private String partner(String email, UserRole role) throws Exception {
        return json.writeValueAsString(Map.of("fullName", "Partner", "email", email, "password", "fixture-password", "userRole", role));
    }
    private MockMultipartFile pdf() { return new MockMultipartFile("documento", "receipt.pdf", "application/octet-stream", PDF); }
    private UUID upload(String actor) throws Exception {
        var response = mvc.perform(multipart("/api/documents").file(pdf()).header("Authorization", actor).header("X-Organization-Id", tokenOrganizations.get(actor)))
            .andExpect(status().isCreated()).andExpect(jsonPath("contentType").value("application/pdf"))
            .andExpect(jsonPath("creator").doesNotExist()).andExpect(jsonPath("storageUrl").doesNotExist())
            .andExpect(jsonPath("passwordHash").doesNotExist()).andReturn().getResponse();
        return UUID.fromString(json.readTree(response.getContentAsString()).get("id").asText());
    }
    private String stored(UUID id) { return jdbc.queryForObject("SELECT storage_url FROM attachment WHERE id=?", String.class, id); }
    private int queueCount(String path) { return jdbc.queryForObject("SELECT count(*) FROM attachment_file_deletion WHERE storage_path=?", Integer.class, path); }
    private void identify(User user) {
        var request=new org.springframework.mock.web.MockHttpServletRequest();
        request.addHeader("X-Organization-Id",user.getId().toString());
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(new org.springframework.web.context.request.ServletRequestAttributes(request));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user.getId(), null,
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getUserRole().name()))));
    }
    private static Path uploads() {
        try { return Files.createTempDirectory("irr-attachment-it-"); }
        catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
    }
}
