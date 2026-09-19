package com.barberbooking.api.catalog;

import com.barberbooking.api.common.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class CatalogService {

    private final ServiceRepository serviceRepository;

    public List<ServiceResponse> list(boolean includeInactive) {
        var services = includeInactive ? serviceRepository.findAll() : serviceRepository.findByActiveTrue();
        return services.stream().map(ServiceResponse::from).toList();
    }

    public ServiceResponse get(UUID id) {
        return ServiceResponse.from(findOrThrow(id));
    }

    @Transactional
    public ServiceResponse create(ServiceRequest request) {
        Service service = new Service();
        applyRequest(service, request);
        serviceRepository.save(service);
        return ServiceResponse.from(service);
    }

    @Transactional
    public ServiceResponse update(UUID id, ServiceRequest request) {
        Service service = findOrThrow(id);
        applyRequest(service, request);
        return ServiceResponse.from(service);
    }

    @Transactional
    public void deactivate(UUID id) {
        Service service = findOrThrow(id);
        // Soft delete: se preserva la fila porque citas pasadas la referencian.
        service.setActive(false);
    }

    @Transactional
    public void activate(UUID id) {
        Service service = findOrThrow(id);
        service.setActive(true);
    }

    private void applyRequest(Service service, ServiceRequest request) {
        service.setName(request.name());
        service.setDescription(request.description());
        service.setDurationMinutes(request.durationMinutes());
        service.setPrice(request.price());
        if (service.getId() == null) {
            service.setActive(true);
        }
    }

    private Service findOrThrow(UUID id) {
        return serviceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado: " + id));
    }
}
