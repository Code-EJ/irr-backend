package org.code.api.infrastructure.specifications;

import org.code.api.domain.models.base.Vehicle;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * Specifications reutilizáveis para filtragem dinâmica de {@link Vehicle}.
 *
 * <p>Cada método retorna uma {@link Specification} que pode ser combinada
 * com {@code .and()} / {@code .or()} para montar queries dinâmicas sem SQL nativo.</p>
 *
 * <p>Uso típico no Service:</p>
 * <pre>{@code
 * Specification<Vehicle> spec = VehicleSpecification.withCreatorId(userId)
 *     .and(VehicleSpecification.licensePlateContains("ABC"))
 *     .and(VehicleSpecification.modelContains("Fiat"));
 * repository.findAll(spec, pageable);
 * }</pre>
 */
public final class VehicleSpecification {

    private VehicleSpecification() {
        // Utility class — não instanciar
    }

    /**
     * Filtra veículos por creator_id (isolamento multilocatário).
     */
    public static Specification<Vehicle> withCreatorId(UUID creatorId) {
        return (root, query, cb) -> cb.equal(root.get("creator").get("id"), creatorId);
    }

    /**
     * Filtra veículos cuja placa contenha o termo informado (case-insensitive, LIKE %term%).
     */
    public static Specification<Vehicle> licensePlateContains(String licensePlate) {
        return (root, query, cb) ->
                cb.like(cb.upper(root.get("licensePlate")), "%" + licensePlate.trim().toUpperCase() + "%");
    }

    /**
     * Filtra veículos cujo modelo contenha o termo informado (case-insensitive, LIKE %term%).
     */
    public static Specification<Vehicle> modelContains(String model) {
        return (root, query, cb) ->
                cb.like(cb.upper(root.get("model")), "%" + model.trim().toUpperCase() + "%");
    }

    /**
     * Filtra veículos pelo status ativo/inativo.
     *
     * <p>Nota: a entidade possui {@code @SQLRestriction("is_active = true")},
     * portanto este filtro é útil principalmente quando se deseja
     * futuramente consultar inativos via query nativa ou desativar o filtro global.</p>
     */
    public static Specification<Vehicle> withIsActive(Boolean isActive) {
        return (root, query, cb) -> cb.equal(root.get("isActive"), isActive);
    }
}
