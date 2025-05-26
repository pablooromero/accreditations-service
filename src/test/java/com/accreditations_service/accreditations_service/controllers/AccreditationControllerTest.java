package com.accreditations_service.accreditations_service.controllers;

import com.accreditations_service.accreditations_service.utils.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.accreditations_service.accreditations_service.config.SecurityConfig;
import com.accreditations_service.accreditations_service.dtos.AccreditationDTO;
import com.accreditations_service.accreditations_service.dtos.CreateAccreditationRequest;
import com.accreditations_service.accreditations_service.exceptions.AccreditationException;
import com.accreditations_service.accreditations_service.exceptions.ExceptionHandlers;
import com.accreditations_service.accreditations_service.exceptions.SalePointException;
import com.accreditations_service.accreditations_service.exceptions.UserException;
import com.accreditations_service.accreditations_service.services.AccreditationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccreditationController.class)
@Import({SecurityConfig.class, ExceptionHandlers.class})
class AccreditationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccreditationService accreditationService;

    @Autowired
    private ObjectMapper objectMapper;

    private AccreditationDTO accreditationDTO1;
    private AccreditationDTO accreditationDTO2;
    private Set<AccreditationDTO> accreditationSet;
    private CreateAccreditationRequest createRequest;

    @BeforeEach
    void setUp() {
        accreditationDTO1 = new AccreditationDTO(1L, 100L, 200L, "Punto A", 150.0, LocalDateTime.now().minusDays(1));
        accreditationDTO2 = new AccreditationDTO(2L, 101L, 201L, "Punto B", 200.0, LocalDateTime.now().minusDays(2));
        accreditationSet = new HashSet<>();
        accreditationSet.add(accreditationDTO1);
        accreditationSet.add(accreditationDTO2);

        createRequest = new CreateAccreditationRequest(100L, 150.0, LocalDateTime.now().minusDays(1));
    }

    @Test
    @DisplayName("GET /api/accreditations/admin - Debería devolver todas las acreditaciones si es ADMIN")
    void getAllAccreditations_asAdmin_shouldReturnAllAccreditations() throws Exception {
        when(accreditationService.getAllAccreditations()).thenReturn(ResponseEntity.ok(accreditationSet));

        mockMvc.perform(get("/api/accreditations/admin")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(token -> token.claim("role", "ADMIN").subject("admin@example.com").claim("id", "1"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.id == 1 && @.salePointName == 'Punto A')]", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/accreditations/admin - Debería devolver 403 si NO es ADMIN")
    void getAllAccreditations_asNonAdmin_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/accreditations/admin")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(token -> token.claim("role", "USER").subject("user@example.com").claim("id", "2"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/accreditations/admin/{id} - Debería devolver acreditación si es ADMIN y existe")
    void getAccreditationById_asAdmin_whenExists_shouldReturnAccreditation() throws Exception {
        when(accreditationService.getAccreditationById(1L)).thenReturn(ResponseEntity.ok(accreditationDTO1));

        mockMvc.perform(get("/api/accreditations/admin/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(token -> token.claim("role", "ADMIN").subject("admin@example.com").claim("id", "1"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.salePointName", is("Punto A")));
    }

    @Test
    @DisplayName("GET /api/accreditations/admin/{id} - Debería devolver 404 si es ADMIN pero no existe")
    void getAccreditationById_asAdmin_whenNotExists_shouldReturnNotFound() throws Exception {
        when(accreditationService.getAccreditationById(99L))
                .thenThrow(new AccreditationException("Accreditation not found", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/accreditations/admin/99")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(token -> token.claim("role", "ADMIN").subject("admin@example.com").claim("id", "1"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/accreditations/{id} - Debería devolver acreditación para usuario autenticado si es propietario")
    void getAccreditationByIdForUser_whenOwner_shouldReturnAccreditation() throws Exception {
        when(accreditationService.getAccreditationByIdUser(eq(200L), eq(1L)))
                .thenReturn(ResponseEntity.ok(accreditationDTO1));

        mockMvc.perform(get("/api/accreditations/1")
                        .with(jwt().jwt(token -> token.subject("user200@example.com").claim("id", "200").claim("role", "USER"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.userId", is(200)));
    }

    @Test
    @DisplayName("GET /api/accreditations/{id} - Debería devolver 403 (o 401 por la excepción) si usuario no es propietario")
    void getAccreditationByIdForUser_whenNotOwner_shouldReturnForbiddenOrUnauthorized() throws Exception {
        when(accreditationService.getAccreditationByIdUser(eq(999L), eq(1L)))
                .thenThrow(new AccreditationException(Constants.NOT_PERM, HttpStatus.UNAUTHORIZED));

        mockMvc.perform(get("/api/accreditations/1")
                        .with(jwt().jwt(token -> token.subject("user999@example.com").claim("id", "999").claim("role", "USER"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/accreditations/{id} - Debería devolver 401 si no está autenticado")
    void getAccreditationByIdForUser_notAuthenticated_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/accreditations/1"))
                .andExpect(status().isUnauthorized());
    }


    @Test
    @DisplayName("POST /api/accreditations - Debería crear acreditación para usuario autenticado")
    void createAccreditation_asAuthenticatedUser_shouldCreateAccreditation() throws Exception {
        AccreditationDTO createdDto = new AccreditationDTO(3L, createRequest.salePointId(), 200L, "Punto Mock", createRequest.amount(), createRequest.receiptDate());
        when(accreditationService.createAccreditation(eq("user200@example.com"), any(CreateAccreditationRequest.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(createdDto));

        mockMvc.perform(post("/api/accreditations")
                        .with(jwt().jwt(token -> token.subject("user200@example.com").claim("id", "200").claim("role", "USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.salePointName", is("Punto Mock")));
    }

    @Test
    @DisplayName("POST /api/accreditations - Debería devolver 404 si SalePoint no existe (manejado por servicio)")
    void createAccreditation_whenSalePointNotFound_shouldReturnNotFound() throws Exception {
        when(accreditationService.createAccreditation(anyString(), any(CreateAccreditationRequest.class)))
                .thenThrow(new SalePointException(Constants.SALE_POINT_NOT_FOUND, HttpStatus.NOT_FOUND));

        mockMvc.perform(post("/api/accreditations")
                        .with(jwt().jwt(token -> token.subject("user@example.com").claim("id", "123").claim("role", "USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is(Constants.SALE_POINT_NOT_FOUND)));
    }

    @Test
    @DisplayName("POST /api/accreditations - Debería devolver 400 si User no existe (manejado por servicio)")
    void createAccreditation_whenUserNotFound_shouldReturnBadRequestOrNotFound() throws Exception {
        when(accreditationService.createAccreditation(anyString(), any(CreateAccreditationRequest.class)))
                .thenThrow(new UserException(Constants.USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        mockMvc.perform(post("/api/accreditations")
                        .with(jwt().jwt(token -> token.subject("unknown@example.com").claim("id", "999").claim("role", "USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is(Constants.USER_NOT_FOUND)));
    }
}
