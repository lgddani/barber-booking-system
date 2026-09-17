package com.barberbooking.api.users;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BarberProfileRepository extends JpaRepository<BarberProfile, UUID> {
}
