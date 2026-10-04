package org.code.api.routes.application;

import static org.code.api.routes.application.RouteAccess.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.code.api.routes.api.GeographyContract.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintains minimal shared territory identities without runtime dataset/provider dependencies.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Service
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class GeographyService {
  private final JdbcTemplate jdbc;
  private final RouteAccess access;

  /** Resolves an existing municipality selection without loading its entire reference catalog. */
  @Transactional(readOnly = true)
  public MunicipalityResponse municipality(UUID id) {
    access.organization();
    var rows =
        jdbc.query(
            "SELECT * FROM municipality WHERE id=?",
            (r, n) ->
                new MunicipalityResponse(
                    id,
                    r.getString("ibge_code"),
                    r.getString("name"),
                    r.getString("state_code"),
                    r.getObject("created_at", OffsetDateTime.class)),
            id);
    if (rows.isEmpty()) throw missing("Municipality");
    return rows.getFirst();
  }

  /** Resolves a paginated picker's current neighborhood by stable identity. */
  @Transactional(readOnly = true)
  public NeighborhoodResponse neighborhood(UUID id) {
    access.organization();
    var rows =
        jdbc.query(
            "SELECT * FROM neighborhood WHERE id=?",
            (r, n) ->
                new NeighborhoodResponse(
                    id,
                    r.getObject("municipality_id", UUID.class),
                    r.getString("name"),
                    r.getObject("created_at", OffsetDateTime.class)),
            id);
    if (rows.isEmpty()) throw missing("Neighborhood");
    return rows.getFirst();
  }

  private static final Map<String, String> PREFIX =
      Map.ofEntries(
          Map.entry("RO", "11"),
          Map.entry("AC", "12"),
          Map.entry("AM", "13"),
          Map.entry("RR", "14"),
          Map.entry("PA", "15"),
          Map.entry("AP", "16"),
          Map.entry("TO", "17"),
          Map.entry("MA", "21"),
          Map.entry("PI", "22"),
          Map.entry("CE", "23"),
          Map.entry("RN", "24"),
          Map.entry("PB", "25"),
          Map.entry("PE", "26"),
          Map.entry("AL", "27"),
          Map.entry("SE", "28"),
          Map.entry("BA", "29"),
          Map.entry("MG", "31"),
          Map.entry("ES", "32"),
          Map.entry("RJ", "33"),
          Map.entry("SP", "35"),
          Map.entry("PR", "41"),
          Map.entry("SC", "42"),
          Map.entry("RS", "43"),
          Map.entry("MS", "50"),
          Map.entry("MT", "51"),
          Map.entry("GO", "52"),
          Map.entry("DF", "53"));

  /** Lists stable shared municipality references after active organization access verification. */
  @Transactional(readOnly = true)
  public Page<MunicipalityResponse> municipalities(Pageable page) {
    access.organization();
    var rows =
        jdbc.query(
            "SELECT * FROM municipality ORDER BY name,id LIMIT ? OFFSET ?",
            (r, n) ->
                new MunicipalityResponse(
                    r.getObject("id", UUID.class),
                    r.getString("ibge_code"),
                    r.getString("name"),
                    r.getString("state_code"),
                    r.getObject("created_at", OffsetDateTime.class)),
            page.getPageSize(),
            page.getOffset());
    return new PageImpl<>(
        rows, page, jdbc.queryForObject("SELECT count(*) FROM municipality", Long.class));
  }

  /**
   * Registers only a shape/UF-consistent operator-reviewed identity; does not claim dataset
   * certification.
   */
  @Transactional
  public MunicipalityResponse createMunicipality(MunicipalityRequest request) {
    access.global();
    if (!request.ibgeCode().startsWith(PREFIX.getOrDefault(request.stateCode(), "invalid")))
      throw invalid("Municipality IBGE prefix must match the state code");
    UUID id = UUID.randomUUID();
    try {
      jdbc.update(
          "INSERT INTO municipality(id,ibge_code,name,state_code,created_by) VALUES (?,?,?,?,?)",
          id,
          request.ibgeCode(),
          request.name().strip(),
          request.stateCode(),
          access.actor());
    } catch (DuplicateKeyException duplicate) {
      throw conflict("Municipality IBGE code already exists");
    }
    return jdbc.queryForObject(
        "SELECT * FROM municipality WHERE id=?",
        (r, n) ->
            new MunicipalityResponse(
                id,
                r.getString("ibge_code"),
                r.getString("name"),
                r.getString("state_code"),
                r.getObject("created_at", OffsetDateTime.class)),
        id);
  }

  /** Lists bounded neighborhood references for a selected stable municipality. */
  @Transactional(readOnly = true)
  public Page<NeighborhoodResponse> neighborhoods(UUID municipality, Pageable page) {
    access.organization();
    requireMunicipality(municipality);
    var rows =
        jdbc.query(
            "SELECT * FROM neighborhood WHERE municipality_id=? ORDER BY name,id LIMIT ? OFFSET ?",
            (r, n) ->
                new NeighborhoodResponse(
                    r.getObject("id", UUID.class),
                    municipality,
                    r.getString("name"),
                    r.getObject("created_at", OffsetDateTime.class)),
            municipality,
            page.getPageSize(),
            page.getOffset());
    return new PageImpl<>(
        rows,
        page,
        jdbc.queryForObject(
            "SELECT count(*) FROM neighborhood WHERE municipality_id=?", Long.class, municipality));
  }

  /** Registers a globally curated neighborhood with normalized uniqueness. */
  @Transactional
  public NeighborhoodResponse createNeighborhood(NeighborhoodRequest request) {
    access.global();
    requireMunicipality(request.municipalityId());
    UUID id = UUID.randomUUID();
    try {
      jdbc.update(
          "INSERT INTO neighborhood(id,municipality_id,name,created_by) VALUES (?,?,?,?)",
          id,
          request.municipalityId(),
          request.name().strip(),
          access.actor());
    } catch (DuplicateKeyException duplicate) {
      throw conflict("Neighborhood name already exists in this municipality");
    }
    return jdbc.queryForObject(
        "SELECT created_at FROM neighborhood WHERE id=?",
        (r, n) ->
            new NeighborhoodResponse(
                id,
                request.municipalityId(),
                request.name().strip(),
                r.getObject(1, OffsetDateTime.class)),
        id);
  }

  /** Resolves a known identity without guessing territory from free text. */
  public void requireMunicipality(UUID id) {
    if (!Boolean.TRUE.equals(
        jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM municipality WHERE id=?)", Boolean.class, id)))
      throw missing("Municipality");
  }
}
