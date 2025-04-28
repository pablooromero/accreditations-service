package com.accreditations_service.accreditations_service.services;

import com.accreditations_service.accreditations_service.dtos.AccreditationDTO;
import com.accreditations_service.accreditations_service.dtos.CreateAccreditationRequest;
import com.accreditations_service.accreditations_service.exceptions.AccreditationException;
import com.accreditations_service.accreditations_service.exceptions.SalePointException;
import com.accreditations_service.accreditations_service.models.Accreditation;
import org.springframework.http.ResponseEntity;

import java.util.Set;

public interface AccreditationService {
    Accreditation saveAccreditation(Accreditation accreditation);

    ResponseEntity<Set<AccreditationDTO>> getAllAccreditations();

    ResponseEntity<AccreditationDTO> getAccreditationById(Long id) throws AccreditationException;

    ResponseEntity<AccreditationDTO> createAccreditation(CreateAccreditationRequest newAccreditation) throws SalePointException;
}
