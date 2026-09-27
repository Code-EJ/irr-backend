package org.code.api.infrastructure.development;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
/**
 * Creates one explicit development organization and administrator membership on its first startup.
 * Restarts preserve membership revocation and organization lifecycle changes instead of silently restoring access.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Component @Profile("development") @Order(1)
@ConditionalOnProperty(name="irr.development.bootstrap-enabled",havingValue="true")
public class DevelopmentOrganizationInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final UUID organization;
    private final String name;
    private final String email;
    /** Configures explicit development identities without logging credentials. */
    public DevelopmentOrganizationInitializer(JdbcTemplate jdbc,
        @Value("${irr.development.organization-id}") UUID organization,
        @Value("${irr.development.organization-name}") String name,
        @Value("${irr.development.admin-email}") String email) { this.jdbc=jdbc;this.organization=organization;this.name=name;this.email=email.strip().toLowerCase(Locale.ROOT); }
    /** Seeds organization and membership audit only when the configured organization is new. */
    @Override @Transactional public void run(ApplicationArguments arguments) {
        if(name.isBlank() || name.length()>255) throw new IllegalStateException("Configure a development organization name of 1-255 characters");
        UUID admin=jdbc.queryForObject("SELECT id FROM users WHERE email=? AND user_role='ADMINISTRATOR' AND is_active",UUID.class,email);
        int inserted=jdbc.update("INSERT INTO organization(id,name,organization_type,created_by) VALUES (?,?,'Development',?) ON CONFLICT(id) DO NOTHING",organization,name.strip(),admin);
        if(inserted==0) {
            UUID creator=jdbc.queryForObject("SELECT created_by FROM organization WHERE id=?",UUID.class,organization);
            if(!admin.equals(creator)) throw new IllegalStateException("Development organization ID belongs to another administrator");
            return;
        }
        jdbc.update("INSERT INTO organization_membership(organization_id,user_id,role,granted_by) VALUES (?,?,'MANAGER',?)",organization,admin,admin);
        jdbc.update("INSERT INTO organization_access_audit(id,organization_id,actor_id,action) VALUES (?,?,?,'CREATE')",UUID.randomUUID(),organization,admin);
        jdbc.update("INSERT INTO organization_access_audit(id,organization_id,actor_id,subject_user_id,action,assigned_role) VALUES (?,?,?,?,'GRANT','MANAGER')",UUID.randomUUID(),organization,admin,admin);
    }
}
