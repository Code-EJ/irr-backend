package org.code.api.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PrensagemNaoEncontradaException extends RuntimeException {
    public PrensagemNaoEncontradaException(UUID id) {
        super("Registro de prensagem não encontrado com o ID: " + id);
    }

    public PrensagemNaoEncontradaException(String mensagem) {
        super(mensagem);
    }
}
