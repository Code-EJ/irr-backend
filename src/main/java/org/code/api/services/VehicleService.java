package org.code.api.services;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.code.api.domain.exception.VehicleError;
import org.code.api.domain.models.logistic.Vehicle;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.code.api.domain.ports.VehiclePort;
import org.code.api.dto.logistic.vehicle.request.VehicleCreateRequestDTO;
import org.code.api.dto.logistic.vehicle.request.VehicleUpdateRequestDTO;
import org.code.api.dto.logistic.vehicle.response.VehicleResponseDTO;
import org.code.api.infrastructure.repositories.UserRepository;
import org.code.api.infrastructure.repositories.VehicleRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/// Serviço responsável pela orquestração das regras de negócio relacionadas à entidade {@link Vehicle}.
/// Implementa a porta de entrada {@link VehiclePort} seguindo os princípios de Arquitetura Hexagonal.
///
/// <p>Esta classe gerencia a persistência, validação de regras de domínio (como unicidade de placa
/// e checagem de estado) e integração com o contexto de segurança para auditoria (autoria de criação).
///
@Service
@RequiredArgsConstructor
public class VehicleService implements VehiclePort {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserProvider userProvider;

    /**
     * Cria registro de veículo no sistema.
     *
     * <p>O método normaliza os dados de entrada e verifica proativamente a existência prévia da placa.
     * O usuário criador é extraído automaticamente do contexto de segurança da requisição atual.
     *
     * @param data DTO contendo os dados necessários para a criação do veículo (placa e modelo).
     * @return {@link VehicleResponseDTO} contendo os dados do veículo recém-criado, incluindo o ID gerado.
     * @throws VehicleError.PlateAlreadyExists se a placa informada já estiver registrada.
     * @throws org.code.api.domain.exception.AuthError.Unauthorized se não houver um usuário autenticado no contexto.
     * @implNote A captura da {@link DataIntegrityViolationException} no momento do salvamento garante resiliência
     * contra Race Conditions em ambientes multithread, delegando a garantia de unicidade absoluta
     * (Unique Constraint) ao banco de dados.
     */
    @Override
    @Transactional
    public VehicleResponseDTO create(VehicleCreateRequestDTO data) {

        String placa = normalizePlate(data.placa());
        String modelo = normalizeModel(data.modelo());

        if (vehicleRepository.existsByPlaca(placa)) {
            throw new VehicleError.PlateAlreadyExists(placa);
        }

        try {
            UUID userId = (UUID) userProvider.getCurrentUserId();

            User createdBy = userRepository.getReferenceById(userId);

            Vehicle vehicle = vehicleRepository.save(
                    Vehicle.builder()
                            .placa(placa)
                            .modelo(modelo)
                            .ativo(true)
                            .createdBy(createdBy)
                            .build()
            );

            return toResponse(vehicle);
        } catch (DataIntegrityViolationException e) {
            throw new VehicleError.PlateAlreadyExists(data.placa());
        }
    }

    /**
     * Retorna uma lista paginada de todos os veículos cadastrados.
     *
     * @param pageable Objeto contendo as informações de paginação e ordenação (página, tamanho, sort).
     * @return Uma {@link Page} contendo os DTOs de resposta dos veículos.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<VehicleResponseDTO> list(Pageable pageable) {
        return vehicleRepository.findAll(pageable).map(this::toResponse);
    }

    /**
     * Busca um veículo específico pelo seu identificador único.
     *
     * @param id O identificador (Primary Key) do veículo procurado.
     * @return {@link VehicleResponseDTO} com os dados do veículo encontrado.
     * @throws VehicleError.NotFound se nenhum veículo corresponder ao ID informado.
     */
    @Override
    @Transactional(readOnly = true)
    public VehicleResponseDTO getById(Integer id) {
        Vehicle vehicle = vehicleRepository
                .findById(id)
                .orElseThrow(() -> new VehicleError.NotFound(id));
        return toResponse(vehicle);
    }

    /**
     * Atualiza os dados de um veículo existente.
     *
     * <p>Garante que veículos inativos não possam ser modificados e otimiza a validação de
     * unicidade realizando consultas no banco apenas se a placa for efetivamente alterada.
     *
     * @param id O identificador do veículo a ser atualizado.
     * @param data DTO contendo os novos dados do veículo.
     * @return {@link VehicleResponseDTO} com os dados atualizados.
     * @throws VehicleError.NotFound se o veículo não for encontrado.
     * @throws VehicleError.InactiveVehicle se o veículo estiver com o status inativo.
     * @throws VehicleError.PlateAlreadyExists se a nova placa já pertencer a outro veículo.
     * @implNote Utiliza a mesma estratégia de tratamento de concorrência (Race Condition) que o método {@code create}.
     */
    @Override
    @Transactional
    public VehicleResponseDTO update(
            Integer id,
            VehicleUpdateRequestDTO data
    ) {

        Vehicle vehicle = vehicleRepository
                .findById(id)
                .orElseThrow(() -> new VehicleError.NotFound(id));

        if (!vehicle.isActive()){
            throw new VehicleError.InactiveVehicle(vehicle.getId());
        }

        String novaPlaca = normalizePlate(data.placa());
        String novoModelo = normalizeModel(data.modelo());

        // Só verifica se uma placa já existe no banco de dados, se o campo placa estiver sendo alterado. Isso otimiza o tempo de query.
        if(!vehicle.getPlaca().equals(novaPlaca)) {
            vehicleRepository.findByPlaca(novaPlaca).ifPresent(found-> {
                throw new VehicleError.PlateAlreadyExists(novaPlaca);
            });
        }

        vehicle.update(novaPlaca, novoModelo, true);

        try {
            Vehicle updatedVehicle = vehicleRepository.save(vehicle);
            return toResponse(updatedVehicle);
        } catch (DataIntegrityViolationException e) {
            throw new VehicleError.PlateAlreadyExists(data.placa());
        }

    }

    /**
     * Realiza a inativação (exclusão lógica) de um veículo do sistema.
     *
     * <p>Este método é protegido por proxy e exige privilégios administrativos.
     *
     * @param id O identificador do veículo a ser inativado.
     * @throws VehicleError.NotFound se o veículo não for encontrado.
     * @throws VehicleError.InactiveVehicle se o veículo já estiver inativo.
     * @throws org.springframework.security.access.AccessDeniedException se o usuário não possuir a role 'ADMINISTRADOR'.
     * * @implNote O salvamento final no banco de dados é garantido automaticamente pelo mecanismo de
     * Dirty Checking do Hibernate ao final da transação.
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void deactivate(Integer id) {
        Vehicle vehicle = vehicleRepository
                .findById(id)
                .orElseThrow(() -> new VehicleError.NotFound(id));

        if (!vehicle.isActive()) {
            throw new VehicleError.InactiveVehicle(vehicle.getId());
        }

        vehicle.deactivate();
        vehicleRepository.save(vehicle);
    }

    /**
     * Normaliza a placa do veículo removendo espaços em branco nas extremidades e convertendo para maiúsculas.
     */
    private String normalizePlate(String placa) {
        return placa.trim().toUpperCase();
    }

    /**
     * Normaliza o modelo do veículo removendo espaços em branco nas extremidades.
     */
    private String normalizeModel(String modelo) {
        return modelo.trim();
    }

    private VehicleResponseDTO toResponse(Vehicle vehicle) {
        return new VehicleResponseDTO(
                vehicle.getId(),
                vehicle.getPlaca(),
                vehicle.getModelo(),
                vehicle.isActive(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt(),
                vehicle.getCreatedBy() != null
                        ? vehicle.getCreatedBy().getId().toString()
                        : null
        );
    }
}
