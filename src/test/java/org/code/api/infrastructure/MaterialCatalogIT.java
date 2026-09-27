package org.code.api.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.code.api.domain.enums.UserRole;
import org.code.api.domain.models.user.Session;
import org.code.api.domain.ports.TokenPort;
import org.code.api.services.MaterialCategoryService;
import org.code.api.dto.material.request.MaterialCategoryCreateRequestDTO;
import org.code.api.dto.material.request.MaterialCategoryUpdateRequestDTO;
import org.code.api.infrastructure.repositories.MaterialCategoryRepository;
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
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies database pagination, current version responses and creator/role boundaries.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@AutoConfigureMockMvc
class MaterialCatalogIT extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired TokenPort tokens;
    @Autowired MaterialCategoryService categories;
    @Autowired MaterialCategoryRepository categoryRepository;
    @Autowired PlatformTransactionManager transactions;

    /** Verifies all hierarchy levels return incremented versions and reject stale edits, including no-ops. */
    @Test void updatesReturnPersistedVersionAndRejectStaleClients() throws Exception {
        UUID owner = user("ADMINISTRATOR");
        UUID category = material("category", owner, null, "Original");
        UUID type = material("type", owner, category, "Original");
        UUID subtype = material("subtype", owner, type, "Original");
        for (var entry : Map.of("categories", category, "types", type, "subtypes", subtype).entrySet()) {
            String url = "/api/materials/" + entry.getKey() + "/" + entry.getValue();
            mvc.perform(put(url).header("Authorization", token(owner)).header("X-Organization-Id", owner).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("name", "Updated", "version", 0))))
                .andExpect(status().isOk()).andExpect(jsonPath("version").value(1));
            for (String name : List.of("Stale edit", "Updated"))
                mvc.perform(put(url).header("Authorization", token(owner)).header("X-Organization-Id", owner).contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("name", name, "version", 0)))).andExpect(status().isConflict());
            mvc.perform(get(url).header("Authorization", token(owner)).header("X-Organization-Id", owner)).andExpect(status().isOk())
                .andExpect(jsonPath("name").value("Updated")).andExpect(jsonPath("version").value(1));
        }
    }
    /** Verifies filters affect page contents and total counts, and exclude foreign and other-parent rows. */
    @Test void filtersRunBeforePaginationAtEveryLevel() throws Exception {
        UUID owner = user("ADMINISTRATOR"), foreign = user("ADMINISTRATOR");
        UUID parent = material("category", owner, null, "Parent");
        UUID otherParent = material("category", owner, null, "Other");
        UUID typeParent = material("type", owner, parent, "Parent");
        UUID otherType = material("type", owner, otherParent, "Other");
        UUID foreignCategory = material("category", foreign, null, "Match foreign");
        UUID foreignType = material("type", foreign, foreignCategory, "Match foreign");
        material("subtype", foreign, foreignType, "Match foreign");
        for (String level : List.of("category", "type", "subtype")) {
            UUID p = level.equals("category") ? null : level.equals("type") ? parent : typeParent;
            material(level, owner, p, "Match A"); material(level, owner, p, "Match B"); material(level, owner, p, "Unrelated");
            String endpoint = level.equals("category") ? "categories" : level + "s";
            var request = get("/api/materials/" + endpoint).header("Authorization", token(owner)).header("X-Organization-Id", owner)
                .param("name", " mAtCh ").param("size", "1").param("page", "1").param("sort", "name,asc");
            if (!level.equals("category")) {
                material(level, owner, level.equals("type") ? otherParent : otherType, "Match outside parent");
                request.param(level.equals("type") ? "categoryId" : "typeId", p.toString());
            }
            mvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(2))
                .andExpect(jsonPath("content[0].name").value("Match B"));
        }
    }
    /** Verifies percent, underscore and escape characters are literal search input. */
    @Test void nameWildcardsDoNotBroadenSearch() throws Exception {
        UUID owner = user("ADMINISTRATOR");
        material("category", owner, null, "100%_!"); material("category", owner, null, "100abcd");
        mvc.perform(get("/api/materials/categories").header("Authorization", token(owner)).header("X-Organization-Id", owner).param("name", "%_!"))
            .andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(1)).andExpect(jsonPath("content[0].name").value("100%_!"));
    }
    /** Verifies creator isolation and the application-service authorization boundary. */
    @Test void foreignAdministratorAndNonadministratorCannotMutateOwnedCatalog() throws Exception {
        UUID owner = user("ADMINISTRATOR"), foreign = user("ADMINISTRATOR");
        UUID category = material("category", owner, null, "Owned");
        mvc.perform(get("/api/materials/categories/{id}", category).header("Authorization", token(foreign)).header("X-Organization-Id", foreign)).andExpect(status().isNotFound());
        identify(user("REPRESENTATIVE"), "REPRESENTATIVE");
        try {
            assertThatThrownBy(() -> categories.create(new MaterialCategoryCreateRequestDTO("Unauthorized")))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        } finally { SecurityContextHolder.clearContext(); org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes(); }
    }
    /** Verifies simultaneous transactions cannot both overwrite the same persisted version. */
    @Test void concurrentTransactionsHaveExactlyOneWinner() throws Exception {
        UUID owner = user("ADMINISTRATOR");
        UUID id = material("category", owner, null, "Concurrent");
        var barrier = new CyclicBarrier(2);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        try (var pool = Executors.newFixedThreadPool(2)) {
            List<Future<?>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < 2; i++) {
                String name = "Winner " + i;
                futures.add(pool.submit(() -> {
                    identify(owner, "ADMINISTRATOR");
                    try {
                        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
                            categoryRepository.findById(id).orElseThrow();
                            try { barrier.await(10, TimeUnit.SECONDS); }
                            catch (Exception exception) { throw new IllegalStateException(exception); }
                            categories.update(id, new MaterialCategoryUpdateRequestDTO(name, 0L));
                        });
                        successes.incrementAndGet();
                    } catch (org.code.api.domain.exception.MaterialError.ConcurrentModification | org.springframework.dao.OptimisticLockingFailureException exception) {
                        conflicts.incrementAndGet();
                    } finally { SecurityContextHolder.clearContext(); org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes(); }
                }));
            }
            for (Future<?> future : futures) future.get(20, TimeUnit.SECONDS);
        }
        assertThat(successes.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT version FROM material_category WHERE id=?", Long.class, id)).isEqualTo(1);
    }
    private UUID user(String role) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id,email,password_hash,full_name,user_role) VALUES (?,?,'fixture','Fixture',?)", id, id + "@example.test", role);
        jdbc.update("INSERT INTO organization(id,name,organization_type,created_by) VALUES (?,'Fixture','Association',?)", id,id);
        jdbc.update("INSERT INTO organization_membership(organization_id,user_id,role,granted_by) VALUES (?,?,?,?)",id,id,role.equals("ADMINISTRATOR")?"MANAGER":"MEMBER",id);
        return id;
    }
    private String token(UUID id) { return "Bearer " + tokens.createToken(Session.builder().id(id).email(id + "@example.test").userRole(UserRole.ADMINISTRATOR).build()); }
    private UUID material(String level, UUID creator, UUID parent, String name) {
        UUID id = UUID.randomUUID();
        if (level.equals("category")) jdbc.update("INSERT INTO material_category(id,name,creator_id,organization_id) VALUES (?,?,?,?)", id, name, creator,creator);
        else jdbc.update("INSERT INTO material_" + level + "(id,name,creator_id,organization_id," + (level.equals("type") ? "category_id" : "type_id") + ") VALUES (?,?,?,?,?)", id, name, creator,creator,parent);
        return id;
    }
    private void identify(UUID id, String role) {
        var request=new org.springframework.mock.web.MockHttpServletRequest();
        request.addHeader("X-Organization-Id",id.toString());
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(new org.springframework.web.context.request.ServletRequestAttributes(request));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(id, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
