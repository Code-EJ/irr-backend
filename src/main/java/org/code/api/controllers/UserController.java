package org.code.api.controllers;

import java.util.List;
import java.util.UUID;
import org.code.api.dto.user.UserRequestDTO;
import org.code.api.dto.user.UserResponseDTO;
import org.code.api.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    
@PostMapping  // Create Users
    public ResponseEntity<UserResponseDTO> createUser(@RequestBody UserRequestDTO request)  {
        UserResponseDTO response = userService.create(request);
        return new ResponseEntity<>(response,HttpStatus.CREATED);
    }


@GetMapping  // Get All Users
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        List<UserResponseDTO> user = userService.findAll();
        return ResponseEntity.ok(user);
    }

@GetMapping("/{id}")  // Get by Id
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable UUID id) {
        UserResponseDTO response = userService.findById(id);
        return ResponseEntity.ok(response);
    }

@GetMapping("/email/{email}")   // Get By email
    public ResponseEntity<UserResponseDTO> getUserByEmail(@PathVariable String email) {
        UserResponseDTO response = userService.findByEmail(email);
        return ResponseEntity.ok(response);
    }

@PutMapping("/{id}")  // Update user
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable UUID id, @RequestBody UserRequestDTO request) {
        UserResponseDTO user = userService.update(id,request);
        return ResponseEntity.ok(user);
    }

@DeleteMapping("/{id}") // Delete user
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}