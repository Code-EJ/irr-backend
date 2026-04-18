package org.code.api.infrastructure.repositories;

import java.util.Optional;
import org.code.api.domain.models.logistic.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório Spring Data JPA para a entidade {@link org.code.api.domain.models.logistic.Vehicle}.
 *
 * <p>Atua como o Adaptador de Persistência (Outbound Port) na Arquitetura Hexagonal,
 * isolando a camada de serviço da infraestrutura de banco de dados SQL.
 * Oferece suporte automático a paginação, ordenação e abstração completa de queries nativas.
 *
 * @implNote Métodos customizados de busca são declarados aqui para suportar o padrão
 * "Fail-Fast" nos serviços, permitindo validações de integridade antes das operações de escrita.
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {
    boolean existsByPlaca(String placa);

    Optional<Vehicle> findByPlaca(String placa);
}
