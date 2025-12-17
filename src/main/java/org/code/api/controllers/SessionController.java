package org.code.api.controllers;

import org.code.api.infrastructure.RSAConfigProps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/session")
public class SessionController {
  @Autowired
  private RSAConfigProps rsaConfigProps;

  @PostMapping("/authenticate")
  public ResponseEntity<Object> authenticate() {
    System.out.println(rsaConfigProps.getPrivateKey());
    return ResponseEntity.status(HttpStatus.OK).body("hello world dude");
  }
}
