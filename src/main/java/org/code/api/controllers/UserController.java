package org.code.api.controllers;

import jakarta.validation.Valid;
import org.code.api.dto.user.CreatePartnerRequest;
import org.code.api.dto.user.UserResponse;
import org.code.api.services.PartnerProvisioningService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Administrator-only partner provisioning; it does not grant self-service role changes.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final PartnerProvisioningService partners;
    /** @param partners administrator provisioning use case */
    public UserController(PartnerProvisioningService partners) { this.partners = partners; }
    /**
     * Creates a partner without issuing a token on their behalf.
     * @param request validated partner attributes
     * @return created partner metadata
     */
    @PostMapping(produces = "application/json")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="201", description="Partner created")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreatePartnerRequest request) {
        return ResponseEntity.status(201).body(partners.create(request));
    }
}
