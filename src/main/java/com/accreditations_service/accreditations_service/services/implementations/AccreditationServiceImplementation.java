package com.accreditations_service.accreditations_service.services.implementations;

import com.accreditations_service.accreditations_service.config.RabbitMQConfig;
import com.accreditations_service.accreditations_service.dtos.AccreditationDTO;
import com.accreditations_service.accreditations_service.dtos.AccreditationDataForPdf;
import com.accreditations_service.accreditations_service.dtos.AccreditationPdfEvent;
import com.accreditations_service.accreditations_service.dtos.CreateAccreditationRequest;
import com.accreditations_service.accreditations_service.exceptions.AccreditationException;
import com.accreditations_service.accreditations_service.exceptions.SalePointException;
import com.accreditations_service.accreditations_service.exceptions.UserException;
import com.accreditations_service.accreditations_service.models.Accreditation;
import com.accreditations_service.accreditations_service.repositories.AccreditationRepository;
import com.accreditations_service.accreditations_service.services.AccreditationEventPublisherService;
import com.accreditations_service.accreditations_service.services.AccreditationService;
import com.accreditations_service.accreditations_service.services.SalePointClientService;
import com.accreditations_service.accreditations_service.services.UserClientService;
import com.accreditations_service.accreditations_service.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccreditationServiceImplementation implements AccreditationService {

    private final AccreditationRepository accreditationRepository;

    private final SalePointClientService  salePointClientService;

    private final UserClientService userClientService;

    private final AccreditationEventPublisherService  accreditationEventPublisherService;

    @Override
    public Accreditation saveAccreditation(Accreditation accreditation) {
        log.info(Constants.SAVING_ACCREDITATION, accreditation);

        Accreditation savedAccreditation = accreditationRepository.save(accreditation);

        log.info(Constants.ACCREDITATION_SAVED_SUCCESSFULLY);
        return savedAccreditation;
    }

    @Override
    public ResponseEntity<Set<AccreditationDTO>> getAllAccreditations() {
        log.info(Constants.GET_ALL_ACCREDITATIONS);

        Set<AccreditationDTO> accreditations = accreditationRepository.findAll()
                .stream()
                .map(accreditation -> new AccreditationDTO(accreditation.getId(),accreditation.getSalePointId(), accreditation.getUserId(), accreditation.getSalePointName(), accreditation.getAmount(), accreditation.getReceiptDate()))
                .collect(Collectors.toSet());

        log.info(Constants.GET_ALL_ACCREDITATIONS_SUCCESSFULLY);

        return new ResponseEntity<>(accreditations, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<AccreditationDTO> getAccreditationById(Long id) throws AccreditationException {
        log.info(Constants.GET_ACCREDITATION, id);

        Accreditation accreditation = accreditationRepository.findById(id)
                .orElseThrow(() -> new AccreditationException(Constants.ACCREDITATION_NOT_FOUND + id, HttpStatus.NOT_FOUND));

        log.info(Constants.GET_ACCREDITATION_SUCCESSFULLY);

        AccreditationDTO accreditationDTO = new AccreditationDTO(accreditation.getId(), accreditation.getSalePointId(), accreditation.getUserId(), accreditation.getSalePointName(), accreditation.getAmount(), accreditation.getReceiptDate());
        return new ResponseEntity<>(accreditationDTO, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<AccreditationDTO> getAccreditationByIdUser(Long userId, Long id) throws AccreditationException {
        log.info(Constants.GET_ACCREDITATION, id);

        Accreditation accreditation = accreditationRepository.findById(id)
                .orElseThrow(() -> new AccreditationException(Constants.ACCREDITATION_NOT_FOUND + id, HttpStatus.NOT_FOUND));

        validateAccreditationOwner(userId, accreditation.getUserId());

        log.info(Constants.GET_ACCREDITATION_SUCCESSFULLY);

        AccreditationDTO accreditationDTO = new AccreditationDTO(accreditation.getId(), accreditation.getSalePointId(), accreditation.getUserId(), accreditation.getSalePointName(), accreditation.getAmount(), accreditation.getReceiptDate());
        return new ResponseEntity<>(accreditationDTO, HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<AccreditationDTO> createAccreditation(String email, CreateAccreditationRequest newAccreditation) throws SalePointException, UserException {
        log.info(Constants.CREATING_ACCREDITATION, newAccreditation);
        LocalDateTime now = LocalDateTime.now();

        String salePointName = salePointClientService.getSalePointName(newAccreditation.salePointId());
        Long userId = userClientService.getUserIdFromEmail(email);

        Accreditation accreditation = new Accreditation();
        accreditation.setSalePointId(newAccreditation.salePointId());
        accreditation.setUserId(userId);
        accreditation.setAmount(newAccreditation.amount());
        accreditation.setSalePointName(salePointName);
        accreditation.setReceiptDate(newAccreditation.receiptDate());
        accreditation.setCreatedAt(now);

        Accreditation savedAccreditation = saveAccreditation(accreditation);
        log.info(Constants.ACCREDITATION_CREATED_SUCCESSFULLY + " ID: {}", savedAccreditation.getId());

        AccreditationDataForPdf pdfData = new AccreditationDataForPdf(
                savedAccreditation.getId(),
                savedAccreditation.getSalePointName(),
                savedAccreditation.getUserId(),
                email,
                savedAccreditation.getAmount(),
                savedAccreditation.getReceiptDate(),
                savedAccreditation.getCreatedAt()
        );
        AccreditationPdfEvent pdfEvent = new AccreditationPdfEvent(
                email,
                "Confirmación de Acreditación - Comprobante N° " + savedAccreditation.getId(),
                "Estimado/a Usuario,\n\nSu acreditación ha sido procesada exitosamente. Adjuntamos el comprobante correspondiente.\n\nSaludos.",
                pdfData
        );

        try {
            accreditationEventPublisherService.publishAccreditationEvent(pdfEvent, savedAccreditation.getId());
        } catch (Exception e) {
            log.error("Error al enviar evento de PDF a RabbitMQ para acreditación ID {}: {}", savedAccreditation.getId(), e.getMessage(), e);
        }

        AccreditationDTO accreditationDTO = new AccreditationDTO(
                savedAccreditation.getId(),
                savedAccreditation.getSalePointId(),
                savedAccreditation.getUserId(),
                savedAccreditation.getSalePointName(),
                savedAccreditation.getAmount(),
                savedAccreditation.getReceiptDate()
        );
        return new ResponseEntity<>(accreditationDTO, HttpStatus.CREATED);
    }

    @Override
    public void validateAccreditationOwner(Long userId, Long accreditationUserId) throws AccreditationException {
        log.info(Constants.VALIDATE_ACCREDITATION_OWNER, userId, accreditationUserId);
        if (!Objects.equals(userId, accreditationUserId)){
            log.error(Constants.VALIDATE_ACCREDITATION_OWNER_ERROR, userId);
            throw new AccreditationException(Constants.NOT_PERM, HttpStatus.UNAUTHORIZED);
        }

        log.info(Constants.VALIDATE_ACCREDITATION_OWNER_SUCCESSFULLY);
    }
}
