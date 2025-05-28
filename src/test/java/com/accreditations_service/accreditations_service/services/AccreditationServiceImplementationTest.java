package com.accreditations_service.accreditations_service.services;

import com.accreditations_service.accreditations_service.dtos.AccreditationDTO;
import com.accreditations_service.accreditations_service.dtos.AccreditationPdfEvent;
import com.accreditations_service.accreditations_service.dtos.CreateAccreditationRequest;
import com.accreditations_service.accreditations_service.exceptions.AccreditationException;
import com.accreditations_service.accreditations_service.exceptions.SalePointException;
import com.accreditations_service.accreditations_service.exceptions.UserException;
import com.accreditations_service.accreditations_service.models.Accreditation;
import com.accreditations_service.accreditations_service.repositories.AccreditationRepository;
import com.accreditations_service.accreditations_service.services.implementations.AccreditationServiceImplementation;
import com.accreditations_service.accreditations_service.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccreditationServiceImplementationTest {

    @Mock
    private AccreditationRepository accreditationRepository;

    @Mock
    private SalePointClientService salePointClientService;

    @Mock
    private UserClientService userClientService;

    @Mock
    private AccreditationEventPublisherService accreditationEventPublisherService;

    @InjectMocks
    private AccreditationServiceImplementation accreditationService;

    private Accreditation accreditation1;
    private CreateAccreditationRequest createRequest;
    private String testEmail;
    private Long testUserId;
    private Long testSalePointId;
    private String testSalePointName;


    @BeforeEach
    void setUp() {
        testEmail = "user@example.com";
        testUserId = 1L;
        testSalePointId = 100L;
        testSalePointName = "Test Sale Point";

        accreditation1 = new Accreditation(
                1L, testSalePointId, testUserId, 150.75, testSalePointName,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().minusHours(2), null, null
        );
        createRequest = new CreateAccreditationRequest(testSalePointId, 150.75, LocalDateTime.now().minusDays(1));
    }

    @Test
    @DisplayName("saveAccreditation - Debería guardar y devolver la acreditación")
    void saveAccreditation_shouldSaveAndReturnAccreditation() {
        Accreditation inputAccreditation = new Accreditation();
        inputAccreditation.setAmount(100.0);
        when(accreditationRepository.save(any(Accreditation.class))).thenReturn(accreditation1);

        Accreditation result = accreditationService.saveAccreditation(inputAccreditation);

        assertNotNull(result);
        assertEquals(accreditation1.getId(), result.getId());
        verify(accreditationRepository, times(1)).save(inputAccreditation);
    }

    @Test
    @DisplayName("getAllAccreditations - Debería devolver un Set de AccreditationDTOs")
    void getAllAccreditations_shouldReturnSetOfAccreditationDTOs() {
        Accreditation accreditation2 = new Accreditation(
                2L, 101L, 2L, 200.0, "Punto B",
                LocalDateTime.now(), LocalDateTime.now(), null, null
        );
        when(accreditationRepository.findAll()).thenReturn(List.of(accreditation1, accreditation2));

        ResponseEntity<Set<AccreditationDTO>> response = accreditationService.getAllAccreditations();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertThat(response.getBody()).extracting(AccreditationDTO::id).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("getAccreditationById - Cuando existe, debería devolver AccreditationDTO")
    void getAccreditationById_whenExists_shouldReturnAccreditationDTO() throws AccreditationException {
        when(accreditationRepository.findById(1L)).thenReturn(Optional.of(accreditation1));

        ResponseEntity<AccreditationDTO> response = accreditationService.getAccreditationById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(accreditation1.getId(), response.getBody().id());
        assertEquals(accreditation1.getSalePointName(), response.getBody().salePointName());
    }

    @Test
    @DisplayName("getAccreditationById - Cuando no existe, debería lanzar AccreditationException")
    void getAccreditationById_whenNotExists_shouldThrowAccreditationException() {
        when(accreditationRepository.findById(99L)).thenReturn(Optional.empty());

        AccreditationException exception = assertThrows(AccreditationException.class, () -> {
            accreditationService.getAccreditationById(99L);
        });
        assertEquals(Constants.ACCREDITATION_NOT_FOUND + 99L, exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    @DisplayName("getAccreditationByIdUser - Cuando existe y es propietario, debería devolver AccreditationDTO")
    void getAccreditationByIdUser_whenExistsAndIsOwner_shouldReturnAccreditationDTO() throws AccreditationException {
        when(accreditationRepository.findById(accreditation1.getId())).thenReturn(Optional.of(accreditation1));

        ResponseEntity<AccreditationDTO> response = accreditationService.getAccreditationByIdUser(testUserId, accreditation1.getId());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(accreditation1.getId(), response.getBody().id());
    }

    @Test
    @DisplayName("getAccreditationByIdUser - Cuando existe pero no es propietario, debería lanzar AccreditationException (UNAUTHORIZED)")
    void getAccreditationByIdUser_whenExistsAndNotOwner_shouldThrowAccreditationException() {
        Long otherUserId = 999L;
        when(accreditationRepository.findById(accreditation1.getId())).thenReturn(Optional.of(accreditation1));

        AccreditationException exception = assertThrows(AccreditationException.class, () -> {
            accreditationService.getAccreditationByIdUser(otherUserId, accreditation1.getId());
        });
        assertEquals(Constants.NOT_PERM, exception.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getHttpStatus());
    }

    @Test
    @DisplayName("getAccreditationByIdUser - Cuando no existe la acreditación, debería lanzar AccreditationException (NOT_FOUND)")
    void getAccreditationByIdUser_whenAccreditationNotExists_shouldThrowAccreditationException() {
        when(accreditationRepository.findById(99L)).thenReturn(Optional.empty());

        AccreditationException exception = assertThrows(AccreditationException.class, () -> {
            accreditationService.getAccreditationByIdUser(testUserId, 99L);
        });
        assertEquals(Constants.ACCREDITATION_NOT_FOUND + 99L, exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    @DisplayName("validateAccreditationOwner - Cuando IDs coinciden, no debería lanzar excepción")
    void validateAccreditationOwner_whenIdsMatch_shouldNotThrowException() {
        assertDoesNotThrow(() -> {
            accreditationService.validateAccreditationOwner(testUserId, testUserId);
        });
    }

    @Test
    @DisplayName("validateAccreditationOwner - Cuando IDs no coinciden, debería lanzar AccreditationException")
    void validateAccreditationOwner_whenIdsDoNotMatch_shouldThrowAccreditationException() {
        AccreditationException exception = assertThrows(AccreditationException.class, () -> {
            accreditationService.validateAccreditationOwner(testUserId, 999L);
        });
        assertEquals(Constants.NOT_PERM, exception.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getHttpStatus());
    }


    @Test
    @DisplayName("createAccreditation - Con datos válidos, debería crear y publicar evento")
    void createAccreditation_withValidData_shouldCreateAndPublishEvent() throws SalePointException, UserException {
        when(salePointClientService.getSalePointName(createRequest.salePointId())).thenReturn(testSalePointName);
        when(userClientService.getUserIdFromEmail(testEmail)).thenReturn(testUserId);

        Accreditation savedAccreditation = new Accreditation();
        savedAccreditation.setId(5L);
        savedAccreditation.setSalePointId(createRequest.salePointId());
        savedAccreditation.setUserId(testUserId);
        savedAccreditation.setAmount(createRequest.amount());
        savedAccreditation.setSalePointName(testSalePointName);
        savedAccreditation.setReceiptDate(createRequest.receiptDate());
        savedAccreditation.setCreatedAt(LocalDateTime.now());

        when(accreditationRepository.save(any(Accreditation.class))).thenReturn(savedAccreditation);
        doNothing().when(accreditationEventPublisherService).publishAccreditationEvent(any(AccreditationPdfEvent.class), anyLong());

        ResponseEntity<AccreditationDTO> response = accreditationService.createAccreditation(testEmail, createRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(savedAccreditation.getId(), response.getBody().id());
        assertEquals(testSalePointName, response.getBody().salePointName());
        assertEquals(testUserId, response.getBody().userId());

        verify(salePointClientService, times(1)).getSalePointName(createRequest.salePointId());
        verify(userClientService, times(1)).getUserIdFromEmail(testEmail);

        ArgumentCaptor<Accreditation> accreditationCaptor = ArgumentCaptor.forClass(Accreditation.class);
        verify(accreditationRepository, times(1)).save(accreditationCaptor.capture());
        assertEquals(createRequest.amount(), accreditationCaptor.getValue().getAmount());
        assertEquals(testUserId, accreditationCaptor.getValue().getUserId());

        ArgumentCaptor<AccreditationPdfEvent> eventCaptor = ArgumentCaptor.forClass(AccreditationPdfEvent.class);
        verify(accreditationEventPublisherService, times(1)).publishAccreditationEvent(eventCaptor.capture(), eq(savedAccreditation.getId()));
        assertEquals(testEmail, eventCaptor.getValue().getTo());
        assertEquals(savedAccreditation.getId(), eventCaptor.getValue().getAccreditationData().getAccreditationId());
    }

    @Test
    @DisplayName("createAccreditation - Cuando SalePointClientService falla, debería lanzar SalePointException")
    void createAccreditation_whenSalePointClientFails_shouldThrowSalePointException() throws SalePointException, UserException {
        SalePointException salePointEx = new SalePointException("Error SP", HttpStatus.SERVICE_UNAVAILABLE);
        when(salePointClientService.getSalePointName(createRequest.salePointId())).thenThrow(salePointEx);

        SalePointException thrown = assertThrows(SalePointException.class, () -> {
            accreditationService.createAccreditation(testEmail, createRequest);
        });
        assertEquals(salePointEx, thrown);
        verify(userClientService, never()).getUserIdFromEmail(anyString());
        verify(accreditationRepository, never()).save(any(Accreditation.class));
        verify(accreditationEventPublisherService, never()).publishAccreditationEvent(any(), anyLong());
    }

    @Test
    @DisplayName("createAccreditation - Cuando UserClientService falla, debería lanzar UserException")
    void createAccreditation_whenUserClientFails_shouldThrowUserException() throws SalePointException, UserException {
        when(salePointClientService.getSalePointName(createRequest.salePointId())).thenReturn(testSalePointName);
        UserException userEx = new UserException("Error User", HttpStatus.SERVICE_UNAVAILABLE);
        when(userClientService.getUserIdFromEmail(testEmail)).thenThrow(userEx);

        UserException thrown = assertThrows(UserException.class, () -> {
            accreditationService.createAccreditation(testEmail, createRequest);
        });
        assertEquals(userEx, thrown);
        verify(accreditationRepository, never()).save(any(Accreditation.class));
        verify(accreditationEventPublisherService, never()).publishAccreditationEvent(any(), anyLong());
    }

    @Test
    @DisplayName("createAccreditation - Cuando EventPublisher falla, la acreditación se crea y se loguea el error")
    void createAccreditation_whenEventPublisherFails_shouldStillCreateAccreditation() throws SalePointException, UserException {
        when(salePointClientService.getSalePointName(createRequest.salePointId())).thenReturn(testSalePointName);
        when(userClientService.getUserIdFromEmail(testEmail)).thenReturn(testUserId);

        Accreditation savedAccreditation = new Accreditation();
        savedAccreditation.setId(5L);
        savedAccreditation.setSalePointId(createRequest.salePointId());
        savedAccreditation.setUserId(testUserId);
        savedAccreditation.setAmount(createRequest.amount());
        savedAccreditation.setSalePointName(testSalePointName);
        savedAccreditation.setReceiptDate(createRequest.receiptDate());
        savedAccreditation.setCreatedAt(LocalDateTime.now());

        when(accreditationRepository.save(any(Accreditation.class))).thenReturn(savedAccreditation);
        doThrow(new RuntimeException("Error simulado al publicar evento"))
                .when(accreditationEventPublisherService).publishAccreditationEvent(any(AccreditationPdfEvent.class), anyLong());

        ResponseEntity<AccreditationDTO> response = accreditationService.createAccreditation(testEmail, createRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(savedAccreditation.getId(), response.getBody().id());

        verify(accreditationEventPublisherService, times(1)).publishAccreditationEvent(any(AccreditationPdfEvent.class), eq(savedAccreditation.getId()));
    }
}