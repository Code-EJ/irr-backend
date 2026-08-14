package org.code.api.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class SubtipologiaNaoEncontradaException extends RuntimeException {
    public SubtipologiaNaoEncontradaException(UUID id) {
        super("Material (subtipologia) não encontrado com o ID: " + id);
    }

    public SubtipologiaNaoEncontradaException(String mensagem) {
        super(mensagem);
    }
}
