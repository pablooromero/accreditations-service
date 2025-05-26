package com.accreditations_service.accreditations_service.services;

import com.accreditations_service.accreditations_service.dtos.AccreditationDTO;
import com.accreditations_service.accreditations_service.dtos.CreateAccreditationRequest;
import com.accreditations_service.accreditations_service.models.Accreditation;
import org.springframework.http.ResponseEntity;

import java.util.Set;

public interface AccreditationService {
    Accreditation saveAccreditation(Accreditation accreditation);

    ResponseEntity<Set<AccreditationDTO>> getAllAccreditations();

    ResponseEntity<AccreditationDTO> getAccreditationById(Long id);

    ResponseEntity<AccreditationDTO> getAccreditationByIdUser(Long userId, Long id);

    ResponseEntity<AccreditationDTO> createAccreditation(String email, CreateAccreditationRequest newAccreditation);

    void validateAccreditationOwner(Long userId, Long accreditationUserId);
}
