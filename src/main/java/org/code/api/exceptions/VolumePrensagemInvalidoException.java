package org.code.api.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class VolumePrensagemInvalidoException extends RuntimeException {
    public VolumePrensagemInvalidoException(String mensagem) {
        super(mensagem);
    }
}
