package org.code.api.domain.ports;

import org.code.api.dto.logistic.vehicle.request.VehicleCreateRequestDTO;
import org.code.api.dto.logistic.vehicle.request.VehicleUpdateRequestDTO;
import org.code.api.dto.logistic.vehicle.response.VehicleResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Porta (‘interface’) que define as operações relacionadas a veículos.
 * Representa o contrato entre o domínio e as implementações externas.
 */
public interface VehiclePort {

    /**
     * Cria um veículo com base nos dados fornecidos.
     *
     * @param data Objeto {@link VehicleCreateRequestDTO} contendo as informações do veículo a ser criado.
     * @return Um {@link VehicleResponseDTO} representando o veículo criado.
     */
    VehicleResponseDTO create(VehicleCreateRequestDTO data);

    /**
     * Lista os veículos de forma paginada.
     *
     * @param pageable Objeto {@link Pageable} contendo as informações de paginação.
     * @return Uma página de {@link VehicleResponseDTO} contendo os veículos encontrados.
     */
    Page<VehicleResponseDTO> list(Pageable pageable);

    /**
     * Obtém os detalhes de um veículo pelo seu ID.
     *
     * @param id Identificador único do veículo.
     * @return Um {@link VehicleResponseDTO} representando o veículo encontrado.
     */
    VehicleResponseDTO getById(Integer id);

    /**
     * Atualiza as informações de um veículo existente.
     *
     * @param id Identificador único do veículo a ser atualizado.
     * @param data Objeto {@link VehicleUpdateRequestDTO} contendo os novos dados do veículo.
     * @return Um {@link VehicleResponseDTO} representando o veículo atualizado.
     */
    VehicleResponseDTO update(
        Integer id,
        VehicleUpdateRequestDTO data
    );

    /**
     * Desativa um veículo pelo seu ID.
     *
     * @param id Identificador único do veículo a ser desativado.
     */
    void deactivate(Integer id);
}
