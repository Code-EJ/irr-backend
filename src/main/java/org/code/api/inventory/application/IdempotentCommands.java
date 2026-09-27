package org.code.api.inventory.application;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Supplier;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.code.api.domain.ports.OrganizationScope;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
/**
 * Commits business effects and their replay response together, scoped by organization.
 * A changed payload cannot reuse a successful command key. Failed transactions leave no receipt.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Service
public class IdempotentCommands {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final HttpServletRequest request;
    private final OrganizationScope scope;
    private final AuthenticatedUserProvider actor;
    /**
     * @param jdbc transaction-aware persistence
     * @param json deterministic record serialization
     * @param request incoming command headers
     * @param scope current verified organization
     * @param actor audit actor
     */
    public IdempotentCommands(JdbcTemplate jdbc,ObjectMapper json,HttpServletRequest request,OrganizationScope scope,AuthenticatedUserProvider actor) {
        this.jdbc=jdbc;this.json=json;this.request=request;this.scope=scope;this.actor=actor;
    }
    /**
     * Executes a command once per organization and key, preserving its original response.
     * @param <T> response type
     * @param kind command discriminator
     * @param payload validated command fields, including target identity for updates
     * @param type response class
     * @param action transactional business effect
     * @return the committed or replayed result
     */
    @Transactional
    public <T> T execute(String kind,Object payload,Class<T> type,Supplier<T> action) {
        UUID organization=scope.organizationId();
        String key=request.getHeader("Idempotency-Key");
        if(key==null || key.isBlank() || key.length()>128) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Idempotency-Key (1-128 characters) is required");
        try {
            String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((kind+":"+json.writeValueAsString(payload)).getBytes(StandardCharsets.UTF_8)));
            var receipts=jdbc.queryForList("SELECT request_hash,response::text FROM command_receipt WHERE organization_id=? AND request_key=?",organization,key);
            if(!receipts.isEmpty()) {
                var saved=receipts.getFirst();
                if(!hash.equals(saved.get("request_hash"))) throw new ResponseStatusException(HttpStatus.CONFLICT,"Idempotency key was already used for a different command");
                return json.readerFor(type).without(com.fasterxml.jackson.databind.DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE).readValue((String)saved.get("response"));
            }
            T result=action.get();
            jdbc.update("INSERT INTO command_receipt(id,organization_id,request_key,command_type,request_hash,response,actor_id) VALUES (?,?,?,?,?,CAST(? AS jsonb),?)",UUID.randomUUID(),organization,key,kind,hash,json.writeValueAsString(result),actor.getCurrentUserId());
            return result;
        } catch(com.fasterxml.jackson.core.JsonProcessingException | java.security.NoSuchAlgorithmException error) { throw new IllegalStateException("Cannot serialize the command receipt",error); }
    }
}
