package com.barberbooking.api.users;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BarberProfileRepository extends JpaRepository<BarberProfile, UUID> {

    // JOIN FETCH trae el User asociado en la misma consulta: BarberProfile.user
    // es LAZY, y sin esto cada acceso a getUser() fuera de una transacción
    // lanzaría LazyInitializationException (y con @Transactional a secas,
    // caería en N+1 — una query por cada barbero).
    @Query("SELECT bp FROM BarberProfile bp JOIN FETCH bp.user WHERE bp.active = true")
    List<BarberProfile> findAllActiveWithUser();

    @Query("SELECT bp FROM BarberProfile bp JOIN FETCH bp.user WHERE bp.userId = :id")
    Optional<BarberProfile> findByIdWithUser(UUID id);
}
