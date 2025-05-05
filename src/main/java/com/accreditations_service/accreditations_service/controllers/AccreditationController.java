package com.accreditations_service.accreditations_service.controllers;

import com.accreditations_service.accreditations_service.config.JwtUtils;
import com.accreditations_service.accreditations_service.dtos.AccreditationDTO;
import com.accreditations_service.accreditations_service.dtos.CreateAccreditationRequest;
import com.accreditations_service.accreditations_service.exceptions.AccreditationException;
import com.accreditations_service.accreditations_service.exceptions.SalePointException;
import com.accreditations_service.accreditations_service.exceptions.UserException;
import com.accreditations_service.accreditations_service.services.AccreditationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Tag(name = "Accreditations", description = "Accreditations Controller")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("api/accreditations")
public class AccreditationController {

    @Autowired
    private AccreditationService accreditationService;

    @Autowired
    private JwtUtils  jwtUtils;

    @Operation(summary = "Get all accreditations", description = "Returns all accreditations stored in the system")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of all accreditations",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AccreditationDTO.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping("/admin")
    public ResponseEntity<Set<AccreditationDTO>> getAllAccreditations() {
        return accreditationService.getAllAccreditations();
    }


    @Operation(summary = "Get accreditation by ID", description = "Get accreditation details by its unique ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Accreditation found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AccreditationDTO.class))),
            @ApiResponse(responseCode = "404", description = "Accreditation not found",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping("/admin/{id}")
    public ResponseEntity<AccreditationDTO> getAccreditationById(@PathVariable Long id) throws AccreditationException {
        return accreditationService.getAccreditationById(id);
    }


    @Operation(summary = "Get accreditation by user ID", description = "Get accreditation by user ID and accreditation ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Accreditation found for the user",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AccreditationDTO.class))),
            @ApiResponse(responseCode = "404", description = "Accreditation not found",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Unauthorized access",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AccreditationDTO> getAccreditationById(@PathVariable long id, HttpServletRequest request) throws AccreditationException {
        Long userId = jwtUtils.extractId(request.getHeader("Authorization"));

        return accreditationService.getAccreditationByIdUser(userId, id);
    }



    @Operation(summary = "Create a new accreditation", description = "Creates a new accreditation for a user, given the sale point ID and amount")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Accreditation created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AccreditationDTO.class))),
            @ApiResponse(responseCode = "404", description = "Sale Point not found",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Bad request, invalid data",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @PostMapping
    public ResponseEntity<AccreditationDTO> createAccreditation(@RequestBody CreateAccreditationRequest newAccreditation, HttpServletRequest request) throws SalePointException, UserException {
        String email = jwtUtils.getEmailFromToken(request.getHeader("Authorization"));
        return accreditationService.createAccreditation(email, newAccreditation);
    }
}
