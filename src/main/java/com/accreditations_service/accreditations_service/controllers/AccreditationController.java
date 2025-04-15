package com.accreditations_service.accreditations_service.controllers;

import com.accreditations_service.accreditations_service.dtos.AccreditationDTO;
import com.accreditations_service.accreditations_service.dtos.CreateAccreditationRequest;
import com.accreditations_service.accreditations_service.exceptions.SalePointException;
import com.accreditations_service.accreditations_service.models.Accreditation;
import com.accreditations_service.accreditations_service.services.AccreditationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("api/accreditations")
public class AccreditationController {

    @Autowired
    private AccreditationService accreditationService;

    @GetMapping
    public ResponseEntity<Set<AccreditationDTO>> getAllAccreditations() {
        return accreditationService.getAllAccreditations();
    }

    @PostMapping
    public ResponseEntity<AccreditationDTO> createAccreditation(@RequestBody CreateAccreditationRequest newAccreditation) throws SalePointException {
        return accreditationService.createAccreditation(newAccreditation);
    }
}
