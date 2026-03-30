package org.code.api.dto.driver;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverRequestDTO {
    
    //@NotBlank(message = "Nome é obrigatório")
    //@Size(max = 191, message = "Nome deve ter no máximo 191 caracteres")
    private String nome;
    
    //@NotBlank(message = "CPF é obrigatório")
    //@Size(max = 191, message = "CPF deve ter no máximo 191 caracteres")
    private String cpf;



}