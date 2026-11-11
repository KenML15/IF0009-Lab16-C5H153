package ucr.ac.cr.medivault.data;

import org.springframework.data.repository.Repository;
import ucr.ac.cr.medivault.domain.AuditLog;

import java.util.List;

public interface AuditLogRepository extends Repository<AuditLog, Long> {
    AuditLog save(AuditLog log);
    List<AuditLog> findAllByOrderByFechaEventoDesc();
}