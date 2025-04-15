package com.accreditations_service.accreditations_service.services.implementations;

import com.accreditations_service.accreditations_service.dtos.AccreditationDTO;
import com.accreditations_service.accreditations_service.dtos.CreateAccreditationRequest;
import com.accreditations_service.accreditations_service.dtos.SalePointDTO;
import com.accreditations_service.accreditations_service.exceptions.SalePointException;
import com.accreditations_service.accreditations_service.models.Accreditation;
import com.accreditations_service.accreditations_service.repositories.AccreditationRepository;
import com.accreditations_service.accreditations_service.services.AccreditationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AccreditationServiceImplementation implements AccreditationService {

    @Autowired
    private AccreditationRepository accreditationRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public Accreditation saveAccreditation(Accreditation accreditation) {
        return accreditationRepository.save(accreditation);
    }

    @Override
    public ResponseEntity<Set<AccreditationDTO>> getAllAccreditations() {
        Set<AccreditationDTO> accreditations = accreditationRepository.findAll()
                .stream()
                .map(accreditation -> new AccreditationDTO(accreditation.getId(),accreditation.getSalePointId(),accreditation.getSalePointName(), accreditation.getAmount(), accreditation.getReceiptDate()))
                .collect(Collectors.toSet());

        return new ResponseEntity<>(accreditations, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<AccreditationDTO> createAccreditation(CreateAccreditationRequest newAccreditation) throws SalePointException {
        LocalDateTime now = LocalDateTime.now();
        String salePointName = getSalePointName(newAccreditation.salePointId());

        Accreditation accreditation = new Accreditation();
        accreditation.setSalePointId(newAccreditation.salePointId());
        accreditation.setAmount(newAccreditation.amount());
        accreditation.setSalePointName(salePointName);
        accreditation.setReceiptDate(newAccreditation.receiptDate());
        accreditation.setCreatedAt(now);

        saveAccreditation(accreditation);

        AccreditationDTO accreditationDTO = new AccreditationDTO(accreditation.getId(), accreditation.getSalePointId(), accreditation.getSalePointName(), accreditation.getAmount(), accreditation.getReceiptDate());

        return new ResponseEntity<>(accreditationDTO, HttpStatus.CREATED);
    }

    private String getSalePointName(Long salePointId) throws SalePointException {
        String salePointServiceUrl = "http://localhost:8080/api/sale-points/" + salePointId;

        try {
            ResponseEntity<SalePointDTO> response = restTemplate.getForEntity(salePointServiceUrl, SalePointDTO.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody().name();
            } else {
                throw new SalePointException("SalePoint not found", HttpStatus.NOT_FOUND);
            }
        } catch (HttpClientErrorException.NotFound e) {
            throw new SalePointException("SalePoint with id " + salePointId + " not found", HttpStatus.NOT_FOUND);
        } catch (HttpClientErrorException e) {
            throw new SalePointException("Error calling sale-point-service: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        } catch (ResourceAccessException e) {
            throw new SalePointException("Could not connect to sale-point-service", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
