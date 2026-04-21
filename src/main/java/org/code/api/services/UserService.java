package org.code.api.services;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.UserPort;
import org.code.api.dto.user.UserRequestDTO;
import org.code.api.dto.user.UserResponseDTO;
import org.code.api.exceptions.DuplicateResourceException;
import org.code.api.exceptions.ResourceNotFoundException;
import org.code.api.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class UserService implements UserPort {
    private final UserRepository userRepository;
    
    @Transactional
    public UserResponseDTO create(UserRequestDTO request) {

        // Verificar se o email existe

        if(userRepository.verifyEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email já cadastrado: " + request.getEmail());
        }

        User user = User.builder()
                    .nome(request.getNome())
                    .email(request.getEmail())
                    .senha(request.getSenha()) // Perguntar sobre relação entre segurança da senha, se deve ser armazenado aqui ou não
                    .build();
                    // .tipo(request.getTipo())
        User savedUser = userRepository.save(user);
        return mapToResponseDTO(savedUser);



    }
    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAll() {
        return userRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponseDTO findById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado utilizando esse ID: " + id));
        return mapToResponseDTO(user);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO findByEmail(String email) {
        User user = userRepository.findbyEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado usando esse Email: " + email));
        return mapToResponseDTO(user);
    }
    @Transactional
    public UserResponseDTO update(UUID id, UserRequestDTO requestDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));

        // Verificar se o novo Email já existe em outro registro
        if (!user.getEmail().equals(requestDTO.getEmail()) && 
            userRepository.verifyEmail(requestDTO.getEmail())) {
            throw new DuplicateResourceException("Email já cadastrado: " + requestDTO.getEmail());
        }

        user.setNome(requestDTO.getNome());
        user.setEmail(requestDTO.getEmail());

        User updatedUser = userRepository.save(user);
        return mapToResponseDTO(updatedUser);
    }

    public void delete(UUID id) {
        if (!userRepository.existsById(id)){
            throw new ResourceNotFoundException("Usuário não encontrado usando ID: " + id);
        }
        userRepository.deleteById(id);
    }



    private UserResponseDTO mapToResponseDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .nome(user.getNome())
                .email(user.getEmail())
                .senha(user.getSenha())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
