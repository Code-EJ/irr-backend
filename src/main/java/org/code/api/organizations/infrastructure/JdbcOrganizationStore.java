package org.code.api.organizations.infrastructure;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.code.api.organizations.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Implements scoped organization persistence with transactional PostgreSQL row locks.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Repository
public class JdbcOrganizationStore implements OrganizationStore {
  private final JdbcTemplate jdbc;
  private static final RowMapper<Organization> ORGANIZATION =
      (rs, row) ->
          new Organization(
              rs.getObject("id", UUID.class),
              rs.getString("name"),
              rs.getString("organization_type"),
              rs.getObject("created_at", OffsetDateTime.class));
  private static final RowMapper<Membership> MEMBERSHIP =
      (rs, row) ->
          new Membership(
              rs.getObject("organization_id", UUID.class),
              rs.getObject("user_id", UUID.class),
              MembershipRole.valueOf(rs.getString("role")),
              rs.getBoolean("is_active"));
  private static final String VISIBLE =
      " FROM organization o JOIN organization_membership m ON m.organization_id=o.id JOIN users u"
          + " ON u.id=m.user_id WHERE o.is_active AND m.is_active AND u.is_active AND u.id=?";

  /**
   * @param jdbc transaction-aware SQL adapter
   */
  public JdbcOrganizationStore(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** {@inheritDoc} */
  public List<Organization> list(UUID actor, int limit, long offset) {
    return jdbc.query(
        "SELECT o.*" + VISIBLE + " ORDER BY o.name,o.id LIMIT ? OFFSET ?",
        ORGANIZATION,
        actor,
        limit,
        offset);
  }

  /** {@inheritDoc} */
  public long count(UUID actor) {
    return jdbc.queryForObject("SELECT count(*)" + VISIBLE, Long.class, actor);
  }

  /** {@inheritDoc} */
  public Optional<Organization> accessible(UUID organization, UUID actor) {
    return jdbc
        .query("SELECT o.*" + VISIBLE + " AND o.id=?", ORGANIZATION, actor, organization)
        .stream()
        .findFirst();
  }

  /** {@inheritDoc} */
  public Optional<Membership> access(UUID organization, UUID actor) {
    return jdbc
        .query("SELECT m.*" + VISIBLE + " AND o.id=?", MEMBERSHIP, actor, organization)
        .stream()
        .findFirst();
  }

  /** {@inheritDoc} */
  public Organization create(UUID id, String name, String type, UUID actor) {
    jdbc.update(
        "INSERT INTO organization(id,name,organization_type,created_by) VALUES (?,?,?,?)",
        id,
        name,
        type,
        actor);
    return jdbc.queryForObject("SELECT * FROM organization WHERE id=?", ORGANIZATION, id);
  }

  /** {@inheritDoc} */
  public void lock(UUID organization) {
    if (jdbc.queryForList(
            "SELECT id FROM organization WHERE id=? AND is_active FOR UPDATE",
            UUID.class,
            organization)
        .isEmpty()) throw new OrganizationAccessDenied();
  }

  /** {@inheritDoc} */
  public boolean activeUser(UUID user) {
    return jdbc.queryForObject(
            "SELECT count(*) FROM users WHERE id=? AND is_active", Long.class, user)
        == 1;
  }

  /** {@inheritDoc} */
  public Optional<Membership> membership(UUID organization, UUID user) {
    return jdbc
        .query(
            "SELECT * FROM organization_membership WHERE organization_id=? AND user_id=?",
            MEMBERSHIP,
            organization,
            user)
        .stream()
        .findFirst();
  }

  /** {@inheritDoc} */
  public void grant(UUID organization, UUID user, MembershipRole role, UUID actor) {
    jdbc.update(
        "INSERT INTO organization_membership(organization_id,user_id,role,granted_by) VALUES"
            + " (?,?,?,?) ON CONFLICT (organization_id,user_id) DO UPDATE SET"
            + " role=EXCLUDED.role,is_active=true,granted_by=EXCLUDED.granted_by,updated_at=CURRENT_TIMESTAMP",
        organization,
        user,
        role.name(),
        actor);
  }

  /** {@inheritDoc} */
  public void revoke(UUID organization, UUID user) {
    jdbc.update(
        "UPDATE organization_membership SET is_active=false,updated_at=CURRENT_TIMESTAMP WHERE"
            + " organization_id=? AND user_id=?",
        organization,
        user);
  }

  /** {@inheritDoc} */
  public void audit(
      UUID organization, UUID actor, UUID subject, String action, String before, String after) {
    jdbc.update(
        "INSERT INTO"
            + " organization_access_audit(id,organization_id,actor_id,subject_user_id,action,previous_role,assigned_role)"
            + " VALUES (?,?,?,?,?,?,?)",
        UUID.randomUUID(),
        organization,
        actor,
        subject,
        action,
        before,
        after);
  }
}
