package com.accreditations_service.accreditations_service.repositories;

import com.accreditations_service.accreditations_service.models.Accreditation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AccreditationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccreditationRepository accreditationRepository;

    private Accreditation accreditation1;
    private Accreditation accreditation2;

    @BeforeEach
    void setUp() {
        accreditation1 = new Accreditation(
                null,
                100L,
                200L,
                150.75,
                "Punto de Venta Test A",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusHours(2),
                null,
                null
        );

        accreditation2 = new Accreditation(
                null,
                101L,
                201L,
                99.99,
                "Punto de Venta Test B",
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusHours(5),
                null,
                null
        );
    }

    @Test
    @DisplayName("Cuando se guarda una Accreditation, debería poder ser encontrada por ID")
    void whenSaveAccreditation_thenFindById_shouldReturnAccreditation() {
        Accreditation savedAccreditation = entityManager.persistAndFlush(accreditation1);

        Optional<Accreditation> foundAccreditationOptional = accreditationRepository.findById(savedAccreditation.getId());

        assertTrue(foundAccreditationOptional.isPresent());
        Accreditation foundAccreditation = foundAccreditationOptional.get();
        assertThat(foundAccreditation.getSalePointName()).isEqualTo(accreditation1.getSalePointName());
        assertThat(foundAccreditation.getAmount()).isEqualTo(accreditation1.getAmount());
        assertThat(foundAccreditation.getUserId()).isEqualTo(accreditation1.getUserId());
        assertThat(foundAccreditation.getId()).isEqualTo(savedAccreditation.getId());
    }

    @Test
    @DisplayName("findAll debería devolver todas las Accreditations guardadas")
    void findAll_shouldReturnAllSavedAccreditations() {
        entityManager.persist(accreditation1);
        entityManager.persist(accreditation2);
        entityManager.flush();

        List<Accreditation> accreditations = accreditationRepository.findAll();

        assertThat(accreditations).hasSize(2);
        assertThat(accreditations).extracting(Accreditation::getSalePointName)
                .containsExactlyInAnyOrder("Punto de Venta Test A", "Punto de Venta Test B");
    }

    @Test
    @DisplayName("findById debería devolver Optional.empty si no se encuentra la Accreditation")
    void findById_whenNotFound_shouldReturnEmptyOptional() {
        Optional<Accreditation> foundAccreditation = accreditationRepository.findById(9999L);

        assertFalse(foundAccreditation.isPresent());
    }

    @Test
    @DisplayName("deleteById debería eliminar la Accreditation")
    void deleteById_shouldRemoveAccreditation() {
        Accreditation savedAccreditation = entityManager.persistAndFlush(accreditation1);
        Long id = savedAccreditation.getId();

        accreditationRepository.deleteById(id);
        entityManager.flush();
        entityManager.clear();

        Optional<Accreditation> deletedAccreditation = accreditationRepository.findById(id);

        assertFalse(deletedAccreditation.isPresent());
    }

    @Test
    @DisplayName("existsById debería devolver true si la Accreditation existe")
    void existsById_whenExists_shouldReturnTrue() {
        Accreditation savedAccreditation = entityManager.persistAndFlush(accreditation1);

        boolean exists = accreditationRepository.existsById(savedAccreditation.getId());

        assertTrue(exists);
    }

    @Test
    @DisplayName("existsById debería devolver false si la Accreditation no existe")
    void existsById_whenNotExists_shouldReturnFalse() {
        boolean exists = accreditationRepository.existsById(9999L);

        assertFalse(exists);
    }

    @Test
    @DisplayName("Cuando se guarda y actualiza una Accreditation, los cambios deberían persistir")
    void whenSaveAndUpdated_thenChangesShouldPersist() {
        Accreditation savedAccreditation = entityManager.persistAndFlush(accreditation1);
        entityManager.detach(savedAccreditation);

        Optional<Accreditation> accreditationToUpdateOptional = accreditationRepository.findById(savedAccreditation.getId());
        assertTrue(accreditationToUpdateOptional.isPresent());
        Accreditation accreditationToUpdate = accreditationToUpdateOptional.get();

        String newName = "Punto de Venta Actualizado";
        Double newAmount = 200.50;
        LocalDateTime newUpdatedAt = LocalDateTime.now();

        accreditationToUpdate.setSalePointName(newName);
        accreditationToUpdate.setAmount(newAmount);
        accreditationToUpdate.setUpdatedAt(newUpdatedAt);

        accreditationRepository.save(accreditationToUpdate);
        entityManager.flush();
        entityManager.clear();

        Optional<Accreditation> updatedAccreditationOptional = accreditationRepository.findById(savedAccreditation.getId());
        assertTrue(updatedAccreditationOptional.isPresent());
        Accreditation updatedAccreditation = updatedAccreditationOptional.get();

        assertThat(updatedAccreditation.getSalePointName()).isEqualTo(newName);
        assertThat(updatedAccreditation.getAmount()).isEqualTo(newAmount);
    }
}
