package org.code.api.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class LocalStorageServiceImpl implements StorageService {

    // Define uma pasta temporária no sistema para simular o servidor de arquivos
    private final String DIRETORIO_SERVIDOR_MOCK = System.getProperty("user.home") + File.separator + "servidor_externo_mock";

    @Override
    public String armazenar(MultipartFile arquivo) throws IOException {
        Path pastaDestino = Paths.get(DIRETORIO_SERVIDOR_MOCK);
        
        // Cria a pasta caso ela não exista
        if (!Files.exists(pastaDestino)) {
            Files.createDirectories(pastaDestino);
        }

        // Garante que arquivos com o mesmo nome não se sobresscrevam no servidor externo
        String nomeUnico = UUID.randomUUID().toString() + "_" + arquivo.getOriginalFilename();
        Path caminhoCompleto = pastaDestino.resolve(nomeUnico);

        // Copia os bytes do arquivo recebido para o diretório do "servidor"
        Files.copy(arquivo.getInputStream(), caminhoCompleto);

        // Retorna o caminho absoluto String que representa a localização no servidor
        return caminhoCompleto.toAbsolutePath().toString();
    }

    @Override
    public void deletar(String caminhoArquivo) {
        try {
            Files.deleteIfExists(Paths.get(caminhoArquivo));
        } catch (IOException e) {
            throw new RuntimeException("Erro ao remover arquivo físico do servidor mock", e);
        }
    }
}
