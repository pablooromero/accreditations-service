package com.accreditations_service.accreditations_service.repositories;

import com.accreditations_service.accreditations_service.models.Accreditation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccreditationRepository extends JpaRepository<Accreditation, Long> {
}
