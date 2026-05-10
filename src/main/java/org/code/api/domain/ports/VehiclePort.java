package org.code.api.domain.ports;

import org.code.api.dto.logistic.vehicle.request.VehicleBulkCreateRequestDTO;
import org.code.api.dto.logistic.vehicle.request.VehicleBulkUpdateRequestDTO;
import org.code.api.dto.logistic.vehicle.request.VehicleCreateRequestDTO;
import org.code.api.dto.logistic.vehicle.request.VehicleUpdateRequestDTO;
import org.code.api.dto.logistic.vehicle.response.VehicleResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Porta de entrada (Inbound Port) para operações sobre Veículos da frota.
 *
 * <p>Todas as operações de leitura filtram pelo {@code creator_id} do usuário
 * autenticado, garantindo isolamento de dados multilocatário.</p>
 */
public interface VehiclePort {

    // ── Operações unitárias ──────────────────────────────────────────────────

    VehicleResponseDTO create(VehicleCreateRequestDTO data);

    VehicleResponseDTO getById(UUID id);

    VehicleResponseDTO update(UUID id, VehicleUpdateRequestDTO data);

    void deactivate(UUID id);

    // ── Listagem com filtragem dinâmica ──────────────────────────────────────

    /**
     * Lista veículos do usuário autenticado com filtros opcionais.
     *
     * @param licensePlate filtro parcial por placa (LIKE %term%), nullable
     * @param model        filtro parcial por modelo (LIKE %term%), nullable
     * @param pageable     parâmetros de paginação e ordenação
     * @return página de veículos filtrados
     */
    Page<VehicleResponseDTO> list(String licensePlate, String model, Pageable pageable);

    // ── Operações em massa (Bulk) ────────────────────────────────────────────

    /**
     * Insere múltiplos veículos em uma única transação atômica.
     * Se qualquer item falhar na validação, toda a transação é revertida.
     */
    List<VehicleResponseDTO> bulkCreate(VehicleBulkCreateRequestDTO data);

    /**
     * Atualiza múltiplos veículos em uma única transação atômica.
     * Se qualquer item falhar na validação, toda a transação é revertida.
     */
    List<VehicleResponseDTO> bulkUpdate(VehicleBulkUpdateRequestDTO data);
}
