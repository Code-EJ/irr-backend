package org.code.api.controllers;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/motorista")
public class DriverController {

    @PostConstruct
    public void init() {
        System.out.println("Inicializando http controller de motoristas.");
    }

    @GetMapping("/ok")
    public ResponseEntity<Object> ok() {
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("ok", true));
    }
}
