package org.code.api.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import org.code.api.domain.enums.UserRole;
import org.code.api.domain.models.user.Session;
import org.code.api.domain.ports.TokenPort;
import org.code.api.organizations.application.OrganizationService;
import org.code.api.organizations.domain.MembershipRole;
import org.code.api.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Verifies explicit membership, immediate revocation, audit transactions and concurrent grants.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@AutoConfigureMockMvc
class OrganizationMembershipIT extends PostgresIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @Autowired TokenPort tokens;
  @Autowired OrganizationService organizations;
  @Autowired PlatformTransactionManager transactions;

  /** Verifies creation does not implicitly grant membership, even to the administrator creator. */
  @Test
  void organizationVisibilityRequiresExplicitMembership() throws Exception {
    UUID admin = user("ADMINISTRATOR"), member = user("REPRESENTATIVE");
    UUID org = create(admin);
    mvc.perform(get("/api/organizations/{id}", org).header("Authorization", token(admin)))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/organizations/{id}", org).header("Authorization", token(member)))
        .andExpect(status().isNotFound());
    grant(admin, org, member, "MEMBER");
    mvc.perform(get("/api/organizations/{id}", org).header("Authorization", token(member)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("id").value(org.toString()));
    mvc.perform(get("/api/organizations").header("Authorization", token(member)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("totalElements").value(1));
    mvc.perform(get("/api/organizations").header("Authorization", token(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("totalElements").value(0));
  }

  /** Verifies only platform administrators can create organizations or change associations. */
  @Test
  void localManagerRoleDoesNotGrantPlatformAdministration() throws Exception {
    UUID admin = user("ADMINISTRATOR"),
        member = user("REPRESENTATIVE"),
        other = user("REPRESENTATIVE");
    UUID org = create(admin);
    grant(admin, org, member, "MANAGER");
    mvc.perform(
            post("/api/organizations")
                .header("Authorization", token(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        Map.of("name", "Forbidden", "organizationType", "Association"))))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/api/organizations/{id}/members/{user}", org, other)
                .header("Authorization", token(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("role", "MEMBER"))))
        .andExpect(status().isForbidden());
    mvc.perform(
            delete("/api/organizations/{id}/members/{user}", org, member)
                .header("Authorization", token(member)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/organizations")).andExpect(status().isUnauthorized());
  }

  /** Verifies revocation takes effect with the same bearer token and retains the audit trail. */
  @Test
  void revocationIsImmediateAuditedAndIdempotent() throws Exception {
    UUID admin = user("ADMINISTRATOR"), member = user("REPRESENTATIVE");
    UUID org = create(admin);
    grant(admin, org, member, "MEMBER");
    grant(admin, org, member, "MEMBER");
    String bearer = token(member);
    for (int i = 0; i < 2; i++)
      mvc.perform(
              delete("/api/organizations/{id}/members/{user}", org, member)
                  .header("Authorization", token(admin)))
          .andExpect(status().isNoContent());
    mvc.perform(get("/api/organizations/{id}", org).header("Authorization", bearer))
        .andExpect(status().isNotFound());
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM organization_access_audit WHERE organization_id=?",
                Integer.class,
                org))
        .isEqualTo(3);
    assertThat(
            jdbc.queryForObject(
                "SELECT is_active FROM organization_membership WHERE organization_id=? AND"
                    + " user_id=?",
                Boolean.class,
                org,
                member))
        .isFalse();
    grant(admin, org, member, "MANAGER");
    mvc.perform(get("/api/organizations/{id}", org).header("Authorization", bearer))
        .andExpect(status().isOk());
  }

  /**
   * Verifies inactive organizations/accounts do not grant scope, even with an active membership
   * row.
   */
  @Test
  void inactiveOrganizationAndAccountAreRejected() throws Exception {
    UUID admin = user("ADMINISTRATOR"),
        member = user("REPRESENTATIVE"),
        inactive = user("REPRESENTATIVE");
    UUID org = create(admin);
    grant(admin, org, member, "MEMBER");
    jdbc.update("UPDATE users SET is_active=false WHERE id=?", inactive);
    mvc.perform(
            put("/api/organizations/{id}/members/{user}", org, inactive)
                .header("Authorization", token(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("role", "MEMBER"))))
        .andExpect(status().isNotFound());
    jdbc.update("UPDATE organization SET is_active=false WHERE id=?", org);
    mvc.perform(get("/api/organizations/{id}", org).header("Authorization", token(member)))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/organizations").header("Authorization", token(member)))
        .andExpect(jsonPath("totalElements").value(0));
  }

  /**
   * Verifies rollback restores access and does not leave an audit event for an uncommitted change.
   */
  @Test
  void revocationAndAuditShareOneTransaction() throws Exception {
    UUID admin = user("ADMINISTRATOR"), member = user("REPRESENTATIVE");
    UUID org = create(admin);
    grant(admin, org, member, "MEMBER");
    identify(admin);
    try {
      new TransactionTemplate(transactions)
          .executeWithoutResult(
              tx -> {
                organizations.revoke(org, member);
                tx.setRollbackOnly();
              });
    } finally {
      SecurityContextHolder.clearContext();
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM organization_access_audit WHERE organization_id=? AND"
                    + " action='REVOKE'",
                Integer.class,
                org))
        .isZero();
    mvc.perform(get("/api/organizations/{id}", org).header("Authorization", token(member)))
        .andExpect(status().isOk());
  }

  /** Verifies concurrent grants serialize into one association and an ordered change history. */
  @Test
  void concurrentGrantsCannotDuplicateMembership() throws Exception {
    UUID admin = user("ADMINISTRATOR"), member = user("REPRESENTATIVE"), org = create(admin);
    var barrier = new CyclicBarrier(2);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var futures = new java.util.ArrayList<Future<?>>();
      for (MembershipRole role : MembershipRole.values())
        futures.add(
            pool.submit(
                () -> {
                  identify(admin);
                  try {
                    barrier.await(10, TimeUnit.SECONDS);
                    organizations.grant(org, member, role);
                  } catch (Exception error) {
                    throw new IllegalStateException(error);
                  } finally {
                    SecurityContextHolder.clearContext();
                  }
                }));
      for (var future : futures) future.get(20, TimeUnit.SECONDS);
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM organization_membership WHERE organization_id=? AND"
                    + " user_id=?",
                Integer.class,
                org,
                member))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM organization_access_audit WHERE organization_id=? AND"
                    + " action='GRANT'",
                Integer.class,
                org))
        .isEqualTo(2);
  }

  /** Verifies unbounded pages and invalid organization fields are rejected. */
  @Test
  void validatesPaginationAndOrganizationFields() throws Exception {
    UUID admin = user("ADMINISTRATOR");
    mvc.perform(
            get("/api/organizations").header("Authorization", token(admin)).param("size", "101"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/organizations")
                .header("Authorization", token(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        Map.of("name", " ", "organizationType", "Association"))))
        .andExpect(status().isBadRequest());
  }

  /**
   * Organization lifecycle changes are audited and revoke scoped visibility without deleting
   * history.
   */
  @Test
  void organizationUpdateAndDeactivationAreAdministratorOnly() throws Exception {
    UUID admin = user("ADMINISTRATOR"), member = user("REPRESENTATIVE"), org = create(admin);
    grant(admin, org, member, "MANAGER");
    String body =
        json.writeValueAsString(
            Map.of("name", "Updated organization", "organizationType", "Association"));
    mvc.perform(
            put("/api/organizations/{id}", org)
                .header("Authorization", token(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/api/organizations/{id}", org)
                .header("Authorization", token(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("name").value("Updated organization"));
    mvc.perform(
            get("/api/organizations/{id}/membership", org).header("Authorization", token(member)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("role").value("MANAGER"));
    mvc.perform(delete("/api/organizations/{id}", org).header("Authorization", token(admin)))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/organizations/{id}", org).header("Authorization", token(member)))
        .andExpect(status().isNotFound());
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM organization_access_audit WHERE organization_id=?",
                Integer.class,
                org))
        .isEqualTo(4);
    assertThat(
            jdbc.queryForObject(
                "SELECT details->>'name' FROM organization_access_audit WHERE organization_id=? AND"
                    + " action='UPDATE'",
                String.class,
                org))
        .isEqualTo("Updated organization");
  }

  /** Development seeding runs once and does not restore revoked membership on a restart. */
  @Test
  void developmentOrganizationSeedPreservesRevocation() {
    UUID admin = user("ADMINISTRATOR"), org = UUID.randomUUID();
    var initializer =
        new org.code.api.infrastructure.development.DevelopmentOrganizationInitializer(
            jdbc, org, "Local fixture", admin + "@example.test");
    var tx = new TransactionTemplate(transactions);
    tx.executeWithoutResult(status -> initializer.run(null));
    jdbc.update("UPDATE organization_membership SET is_active=false WHERE organization_id=?", org);
    tx.executeWithoutResult(status -> initializer.run(null));
    assertThat(
            jdbc.queryForObject(
                "SELECT is_active FROM organization_membership WHERE organization_id=?",
                Boolean.class,
                org))
        .isFalse();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM organization_access_audit WHERE organization_id=?",
                Integer.class,
                org))
        .isEqualTo(2);
  }

  private UUID user(String role) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO users(id,email,password_hash,full_name,user_role) VALUES"
            + " (?,?,'fixture','Fixture',?)",
        id,
        id + "@example.test",
        role);
    return id;
  }

  private String token(UUID id) {
    return "Bearer "
        + tokens.createToken(
            Session.builder()
                .id(id)
                .email(id + "@example.test")
                .userRole(UserRole.ADMINISTRATOR)
                .build());
  }

  private UUID create(UUID actor) throws Exception {
    var response =
        mvc.perform(
                post("/api/organizations")
                    .header("Authorization", token(actor))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(
                            Map.of(
                                "name",
                                "Organization " + UUID.randomUUID(),
                                "organizationType",
                                "Association"))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    return UUID.fromString(json.readTree(response.getContentAsString()).get("id").asText());
  }

  private void grant(UUID actor, UUID org, UUID user, String role) throws Exception {
    mvc.perform(
            put("/api/organizations/{id}/members/{user}", org, user)
                .header("Authorization", token(actor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("role", role))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("active").value(true));
  }

  private void identify(UUID id) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                id, null, List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))));
  }
}
