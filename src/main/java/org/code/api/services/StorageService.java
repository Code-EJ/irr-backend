package org.code.api.services;

import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String armazenar(MultipartFile arquivo) throws IOException;
    void deletar(String caminhoArquivo);
}
