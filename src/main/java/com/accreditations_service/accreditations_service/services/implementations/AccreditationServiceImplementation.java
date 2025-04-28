package com.accreditations_service.accreditations_service.services.implementations;

import com.accreditations_service.accreditations_service.dtos.AccreditationDTO;
import com.accreditations_service.accreditations_service.dtos.CreateAccreditationRequest;
import com.accreditations_service.accreditations_service.dtos.SalePointDTO;
import com.accreditations_service.accreditations_service.exceptions.AccreditationException;
import com.accreditations_service.accreditations_service.exceptions.SalePointException;
import com.accreditations_service.accreditations_service.models.Accreditation;
import com.accreditations_service.accreditations_service.repositories.AccreditationRepository;
import com.accreditations_service.accreditations_service.services.AccreditationService;
import com.accreditations_service.accreditations_service.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(AccreditationServiceImplementation.class);

    @Autowired
    private AccreditationRepository accreditationRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public Accreditation saveAccreditation(Accreditation accreditation) {
        logger.info(Constants.SAVING_ACCREDITATION, accreditation);

        Accreditation savedAccreditation = accreditationRepository.save(accreditation);

        logger.info(Constants.ACCREDITATION_SAVED_SUCCESSFULLY);
        return savedAccreditation;
    }

    @Override
    public ResponseEntity<Set<AccreditationDTO>> getAllAccreditations() {
        logger.info(Constants.GET_ALL_ACCREDITATIONS);

        Set<AccreditationDTO> accreditations = accreditationRepository.findAll()
                .stream()
                .map(accreditation -> new AccreditationDTO(accreditation.getId(),accreditation.getSalePointId(),accreditation.getSalePointName(), accreditation.getAmount(), accreditation.getReceiptDate()))
                .collect(Collectors.toSet());

        logger.info(Constants.GET_ALL_ACCREDITATIONS_SUCCESSFULLY);

        return new ResponseEntity<>(accreditations, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<AccreditationDTO> getAccreditationById(Long id) throws AccreditationException {
        logger.info(Constants.GET_ACCREDITATION, id);

        Accreditation accreditation = accreditationRepository.findById(id)
                .orElseThrow(() -> new AccreditationException(Constants.ACCREDITATION_NOT_FOUND + id, HttpStatus.NOT_FOUND));

        logger.info(Constants.GET_ACCREDITATION_SUCCESSFULLY);

        AccreditationDTO accreditationDTO = new AccreditationDTO(accreditation.getId(), accreditation.getSalePointId(), accreditation.getSalePointName(), accreditation.getAmount(), accreditation.getReceiptDate());
        return new ResponseEntity<>(accreditationDTO, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<AccreditationDTO> createAccreditation(CreateAccreditationRequest newAccreditation) throws SalePointException {
        logger.info(Constants.CREATING_ACCREDITATION, newAccreditation);
        LocalDateTime now = LocalDateTime.now();

        String salePointName = getSalePointName(newAccreditation.salePointId());

        Accreditation accreditation = new Accreditation();
        accreditation.setSalePointId(newAccreditation.salePointId());
        accreditation.setAmount(newAccreditation.amount());
        accreditation.setSalePointName(salePointName);
        accreditation.setReceiptDate(newAccreditation.receiptDate());
        accreditation.setCreatedAt(now);

        saveAccreditation(accreditation);

        logger.info(Constants.ACCREDITATION_CREATED_SUCCESSFULLY);

        AccreditationDTO accreditationDTO = new AccreditationDTO(accreditation.getId(), accreditation.getSalePointId(), accreditation.getSalePointName(), accreditation.getAmount(), accreditation.getReceiptDate());
        return new ResponseEntity<>(accreditationDTO, HttpStatus.CREATED);
    }

    private String getSalePointName(Long salePointId) throws SalePointException {
        logger.info(Constants.GET_SALE_POINT_NAME, salePointId);
        String salePointServiceUrl = "http://sales-point-service:8082/api/sales-point/" + salePointId;

        try {
            ResponseEntity<SalePointDTO> response = restTemplate.getForEntity(salePointServiceUrl, SalePointDTO.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.info(Constants.GET_SALE_POINT_NAME_SUCCESSFULLY);
                return response.getBody().name();
            } else {
                throw new SalePointException(Constants.SALE_POINT_NOT_FOUND + salePointId, HttpStatus.NOT_FOUND);
            }
        } catch (HttpClientErrorException.NotFound e) {
            throw new SalePointException(Constants.SALE_POINT_NOT_FOUND + salePointId, HttpStatus.NOT_FOUND);
        } catch (HttpClientErrorException e) {
            throw new SalePointException(Constants.ERROR_CALLING_SALE_POINT_SERVICE + e.getMessage(), HttpStatus.BAD_GATEWAY);
        } catch (ResourceAccessException e) {
            throw new SalePointException(Constants.COULD_NOT_CONNECT_SALE_POINT_SERVICE, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
