package com.barberbooking.api.barbers;

import com.barberbooking.api.common.BusinessRuleException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkingHoursService {

    private final WorkingHoursRepository workingHoursRepository;

    public List<WorkingHoursItem> get(UUID barberId) {
        return workingHoursRepository.findByBarberIdOrderByDayOfWeekAscStartTimeAsc(barberId).stream()
            .map(WorkingHoursItem::from)
            .toList();
    }

    // Reemplaza toda la plantilla semanal de una vez: es más simple de razonar
    // (y de usar desde el frontend) que soportar altas/bajas incrementales.
    @Transactional
    public List<WorkingHoursItem> replace(UUID barberId, List<WorkingHoursItem> items) {
        for (WorkingHoursItem item : items) {
            if (!item.startTime().isBefore(item.endTime())) {
                throw new BusinessRuleException(
                    "El horario de inicio debe ser antes que el de fin (" + item.dayOfWeek() + ")"
                );
            }
        }

        workingHoursRepository.deleteByBarberId(barberId);
        List<WorkingHours> entities = items.stream().map(item -> {
            WorkingHours wh = new WorkingHours();
            wh.setBarberId(barberId);
            wh.setDayOfWeek(item.dayOfWeek());
            wh.setStartTime(item.startTime());
            wh.setEndTime(item.endTime());
            return wh;
        }).toList();
        workingHoursRepository.saveAll(entities);

        return get(barberId);
    }
}
