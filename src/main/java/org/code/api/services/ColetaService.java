package org.code.api.services;

import java.util.List;

import org.code.api.domain.coleta.Coleta;
import org.code.api.dto.collection.request.ColetaRequestDTO;
import org.code.api.dto.collection.response.ColetaResponseDTO;
import org.code.api.infrastructure.repositories.ColetaRepository;
import org.springframework.stereotype.Service;

@Service
public class ColetaService {

    private final ColetaRepository coletaRepository;

    public ColetaService(ColetaRepository coletaRepository) {
        this.coletaRepository = coletaRepository;
    }

    public ColetaResponseDTO criar(ColetaRequestDTO dto) {
        Coleta coleta = new Coleta();

        coleta.setHorarioSaida(dto.horarioSaida());
        coleta.setHorarioChegada(dto.horarioChegada());
        coleta.setRota(dto.rota());
        coleta.setVeiculoId(dto.veiculoId());
        coleta.setQuilometragem(dto.quilometragem());
        coleta.setPesagemKg(dto.pesagemKg());
        coleta.setAtivo(true);

        Coleta coletaSalva = coletaRepository.save(coleta);

        return new ColetaResponseDTO(coletaSalva);
    }

    public List<ColetaResponseDTO> listar() {
        return coletaRepository.findAll()
                .stream()
                .map(ColetaResponseDTO::new)
                .toList();
    }

    public ColetaResponseDTO buscarPorId(Long id) {
        Coleta coleta = coletaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coleta não encontrada"));

        return new ColetaResponseDTO(coleta);
    }

    public ColetaResponseDTO atualizar(Long id, ColetaRequestDTO dto) {
        Coleta coleta = coletaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coleta não encontrada"));

        coleta.setHorarioSaida(dto.horarioSaida());
        coleta.setHorarioChegada(dto.horarioChegada());
        coleta.setRota(dto.rota());
        coleta.setVeiculoId(dto.veiculoId());
        coleta.setQuilometragem(dto.quilometragem());
        coleta.setPesagemKg(dto.pesagemKg());

        Coleta atualizada = coletaRepository.save(coleta);

        return new ColetaResponseDTO(atualizada);
    }

    public void deletar(Long id) {
        Coleta coleta = coletaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coleta não encontrada"));

        coleta.setAtivo(false);
        coletaRepository.save(coleta);
    }
}