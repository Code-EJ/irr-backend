package org.code.api.services;

import lombok.RequiredArgsConstructor;
import org.code.api.domain.prensagem.Prensagem;
import org.code.api.domain.prensagem.PrensagemRequestDTO;
import org.code.api.domain.prensagem.PrensagemResponseDTO;
import org.code.api.domain.subtipologia.Subtipologia;
import org.code.api.exceptions.PrensagemNaoEncontradaException;
import org.code.api.exceptions.SubtipologiaNaoEncontradaException;
import org.code.api.exceptions.VolumePrensagemInvalidoException;
import org.code.api.repositories.PrensagemRepository;
import org.code.api.repositories.SubtipologiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrensagemService {

    private final PrensagemRepository prensagemRepository;
    private final SubtipologiaRepository subtipologiaRepository;

    @Transactional
    public PrensagemResponseDTO create(PrensagemRequestDTO dto) {
        validarDadosEntrada(dto);

        Subtipologia subtipologia = subtipologiaRepository.findById(dto.subtipologiaId())
                .orElseThrow(() -> new SubtipologiaNaoEncontradaException(dto.subtipologiaId()));

        Prensagem prensagem = new Prensagem();
        prensagem.setData(dto.data() != null ? dto.data() : LocalDateTime.now());
        prensagem.setVolumeTotal(dto.volumeTotal());
        prensagem.setTipoOrigem(dto.tipoOrigem());
        prensagem.setTipoDestino(dto.tipoDestino());
        prensagem.setSubtipologia(subtipologia);

        BigDecimal estoqueAtual = subtipologia.getVolumeCompactadoEstoque() != null
                ? subtipologia.getVolumeCompactadoEstoque()
                : BigDecimal.ZERO;
        subtipologia.setVolumeCompactadoEstoque(estoqueAtual.add(dto.volumeTotal()));
        subtipologiaRepository.save(subtipologia);

        Prensagem saved = prensagemRepository.save(prensagem);
        return new PrensagemResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<PrensagemResponseDTO> listAll() {
        return prensagemRepository.findAll()
                .stream()
                .map(PrensagemResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public PrensagemResponseDTO getById(UUID id) {
        Prensagem prensagem = prensagemRepository.findById(id)
                .orElseThrow(() -> new PrensagemNaoEncontradaException(id));
        return new PrensagemResponseDTO(prensagem);
    }

    @Transactional
    public PrensagemResponseDTO update(UUID id, PrensagemRequestDTO dto) {
        validarDadosEntrada(dto);

        Prensagem prensagem = prensagemRepository.findById(id)
                .orElseThrow(() -> new PrensagemNaoEncontradaException(id));

        Subtipologia novaSubtipologia = subtipologiaRepository.findById(dto.subtipologiaId())
                .orElseThrow(() -> new SubtipologiaNaoEncontradaException(dto.subtipologiaId()));

        Subtipologia subtipologiaAntiga = prensagem.getSubtipologia();
        BigDecimal volumeAntigo = prensagem.getVolumeTotal();
        BigDecimal novoVolume = dto.volumeTotal();

        if (subtipologiaAntiga.getId().equals(novaSubtipologia.getId())) {
            BigDecimal diferenca = novoVolume.subtract(volumeAntigo);
            BigDecimal estoqueAtual = novaSubtipologia.getVolumeCompactadoEstoque() != null
                    ? novaSubtipologia.getVolumeCompactadoEstoque()
                    : BigDecimal.ZERO;
            novaSubtipologia.setVolumeCompactadoEstoque(estoqueAtual.add(diferenca));
            subtipologiaRepository.save(novaSubtipologia);
        } else {
            BigDecimal estoqueAntigo = subtipologiaAntiga.getVolumeCompactadoEstoque() != null
                    ? subtipologiaAntiga.getVolumeCompactadoEstoque()
                    : BigDecimal.ZERO;
            subtipologiaAntiga.setVolumeCompactadoEstoque(estoqueAntigo.subtract(volumeAntigo).max(BigDecimal.ZERO));
            subtipologiaRepository.save(subtipologiaAntiga);

            BigDecimal estoqueNovo = novaSubtipologia.getVolumeCompactadoEstoque() != null
                    ? novaSubtipologia.getVolumeCompactadoEstoque()
                    : BigDecimal.ZERO;
            novaSubtipologia.setVolumeCompactadoEstoque(estoqueNovo.add(novoVolume));
            subtipologiaRepository.save(novaSubtipologia);
        }

        prensagem.setData(dto.data() != null ? dto.data() : prensagem.getData());
        prensagem.setVolumeTotal(novoVolume);
        prensagem.setTipoOrigem(dto.tipoOrigem());
        prensagem.setTipoDestino(dto.tipoDestino());
        prensagem.setSubtipologia(novaSubtipologia);

        Prensagem updated = prensagemRepository.save(prensagem);
        return new PrensagemResponseDTO(updated);
    }

    @Transactional
    public void delete(UUID id) {
        Prensagem prensagem = prensagemRepository.findById(id)
                .orElseThrow(() -> new PrensagemNaoEncontradaException(id));

        Subtipologia subtipologia = prensagem.getSubtipologia();
        if (subtipologia != null) {
            BigDecimal estoqueAtual = subtipologia.getVolumeCompactadoEstoque() != null
                    ? subtipologia.getVolumeCompactadoEstoque()
                    : BigDecimal.ZERO;
            subtipologia.setVolumeCompactadoEstoque(estoqueAtual.subtract(prensagem.getVolumeTotal()).max(BigDecimal.ZERO));
            subtipologiaRepository.save(subtipologia);
        }

        prensagemRepository.delete(prensagem);
    }

    private void validarDadosEntrada(PrensagemRequestDTO dto) {
        if (dto == null) {
            throw new VolumePrensagemInvalidoException("Os dados da prensagem não podem ser nulos.");
        }
        if (dto.subtipologiaId() == null) {
            throw new SubtipologiaNaoEncontradaException("O identificador do material (subtipologia) é obrigatório.");
        }
        if (dto.volumeTotal() == null || dto.volumeTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new VolumePrensagemInvalidoException("O volume total informado para a prensagem deve ser maior que zero.");
        }
        if (dto.tipoOrigem() == null || dto.tipoDestino() == null) {
            throw new VolumePrensagemInvalidoException("Tipo de origem e tipo de destino são obrigatórios.");
        }
    }
}
