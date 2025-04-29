package com.accreditations_service.accreditations_service.controllers;

import com.accreditations_service.accreditations_service.config.JwtUtils;
import com.accreditations_service.accreditations_service.dtos.AccreditationDTO;
import com.accreditations_service.accreditations_service.dtos.CreateAccreditationRequest;
import com.accreditations_service.accreditations_service.exceptions.AccreditationException;
import com.accreditations_service.accreditations_service.exceptions.SalePointException;
import com.accreditations_service.accreditations_service.exceptions.UserException;
import com.accreditations_service.accreditations_service.services.AccreditationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("api/accreditations")
public class AccreditationController {

    @Autowired
    private AccreditationService accreditationService;

    @Autowired
    private JwtUtils  jwtUtils;

    @GetMapping("/admin")
    public ResponseEntity<Set<AccreditationDTO>> getAllAccreditations() {
        return accreditationService.getAllAccreditations();
    }

    @GetMapping("/admin/{id}")
    public ResponseEntity<AccreditationDTO> getAccreditationById(@PathVariable Long id) throws AccreditationException {
        return accreditationService.getAccreditationById(id);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccreditationDTO> getAccreditationById(@PathVariable long id, HttpServletRequest request) throws AccreditationException {
        Long userId = jwtUtils.extractId(request.getHeader("Authorization"));

        return accreditationService.getAccreditationByIdUser(userId, id);
    }

    @PostMapping
    public ResponseEntity<AccreditationDTO> createAccreditation(@RequestBody CreateAccreditationRequest newAccreditation, HttpServletRequest request) throws SalePointException, UserException {
        String email = jwtUtils.getEmailFromToken(request.getHeader("Authorization"));
        return accreditationService.createAccreditation(email, newAccreditation);
    }
}
