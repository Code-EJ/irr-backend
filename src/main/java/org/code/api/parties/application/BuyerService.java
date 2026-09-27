package org.code.api.parties.application;

import java.util.UUID;
import org.code.api.domain.models.sale.Buyer;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.code.api.domain.ports.OrganizationScope;
import org.code.api.infrastructure.repositories.BuyerRepository;
import org.code.api.infrastructure.repositories.UserRepository;
import org.code.api.parties.api.BuyerContract.Request;
import org.code.api.parties.api.BuyerContract.Response;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Maintains buyers within an explicitly verified organization and preserves referenced history.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Service
@lombok.RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class BuyerService {
  private final BuyerRepository records;
  private final UserRepository users;
  private final OrganizationScope scope;
  private final AuthenticatedUserProvider actor;
  private final JdbcTemplate jdbc;

  /** Lists active records belonging to the selected organization. */
  @Transactional(readOnly = true)
  public Page<Response> list(Pageable page) {
    return records.findAllByOrganizationId(scope.organizationId(), page).map(this::response);
  }

  /** Resolves one record without revealing foreign organization data. */
  @Transactional(readOnly = true)
  public Response get(UUID id) {
    return response(owned(id, scope.organizationId()));
  }

  /** Creates a record; only an active organization manager can mutate reference data. */
  @Transactional
  public Response create(Request request) {
    UUID org = manager();
    var record =
        Buyer.builder()
            .organizationId(org)
            .creator(users.getReferenceById(actor.getCurrentUserId()))
            .isActive(true)
            .build();
    apply(record, request);
    return response(records.saveAndFlush(record));
  }

  /** Replaces editable fields while retaining actor and creation history. */
  @Transactional
  public Response update(UUID id, Request request) {
    var record = owned(id, manager());

    apply(record, request);
    return response(records.saveAndFlush(record));
  }

  /** Deactivates only unreferenced records to preserve operational history. */
  @Transactional
  public void deactivate(UUID id) {
    UUID org = manager();
    var record = owned(id, org);
    requireUnreferenced(id, org);
    record.setIsActive(false);
    records.saveAndFlush(record);
  }

  private UUID manager() {
    UUID org = scope.organizationId();
    if (!scope.manager())
      throw new AccessDeniedException("Organization manager permission is required");
    return org;
  }

  private Buyer owned(UUID id, UUID org) {
    return records
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyer not found"));
  }

  private void requireUnreferenced(UUID id, UUID org) {
    if (Boolean.TRUE.equals(
        jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM sale WHERE buyer_id=? AND organization_id=?)",
            Boolean.class,
            id,
            org)))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Referenced buyers must remain available for operational history");
  }

  private void apply(Buyer record, Request request) {
    record.setName(request.name().strip());
    record.setDocument(
        request.document() == null || request.document().isBlank()
            ? null
            : request.document().strip());
  }

  private Response response(Buyer record) {
    return new Response(
        record.getId(),
        record.getName(),
        record.getDocument(),
        record.getIsActive(),
        record.getCreatedAt(),
        record.getUpdatedAt());
  }
}
