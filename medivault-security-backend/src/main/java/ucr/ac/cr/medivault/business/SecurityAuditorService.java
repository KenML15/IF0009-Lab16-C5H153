package ucr.ac.cr.medivault.business;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ucr.ac.cr.medivault.data.AuditLogRepository;
import ucr.ac.cr.medivault.domain.AuditLog;

@Service
public class SecurityAuditorService {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditorService.class);

    private final AuditLogRepository repo;

    public SecurityAuditorService(AuditLogRepository repo) {
        this.repo = repo;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String username, String ip, String accion,
                          String recurso, int codigo, String detalle) {
        try {
            repo.save(new AuditLog(username, ip, accion, recurso, codigo, detalle));
        } catch (Exception e) {
            // La auditoria nunca debe romper la peticion del usuario
            log.error("No se pudo registrar el evento de auditoria", e);
        }
    }
}