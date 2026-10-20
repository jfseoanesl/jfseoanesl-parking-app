package com.parking.presentation.controller;

import com.parking.domain.time.TimeProvider;
import com.parking.infrastructure.backup.BackupFile;
import com.parking.infrastructure.backup.DatabaseBackupService;
import com.parking.infrastructure.security.ParkingUserPrincipal;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/** RF-073 / ADR-009: respaldo completo de la base de datos (solo Administrador). */
@RestController
@RequestMapping("/api/admin/backup")
@PreAuthorize("hasRole('ADMIN')")
public class BackupController {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");

    private final DatabaseBackupService backupService;
    private final TimeProvider timeProvider;

    public BackupController(DatabaseBackupService backupService, TimeProvider timeProvider) {
        this.backupService = backupService;
        this.timeProvider = timeProvider;
    }

    /** Descarga el respaldo como archivo JSON (contiene datos sensibles: usuarios, cobros, auditoría). */
    @GetMapping
    public ResponseEntity<BackupFile> export(@AuthenticationPrincipal ParkingUserPrincipal principal) {
        BackupFile backup = backupService.export(principal.id(), principal.getUsername());
        String filename = "parking-respaldo_" + backup.exportedAt().atZone(timeProvider.zone()).format(FILE_STAMP) + ".json";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(backup);
    }

    /** Reemplaza TODOS los datos por los del respaldo, en una sola transacción. */
    @PostMapping("/import")
    public ImportResult importBackup(@RequestBody BackupFile backup,
                                     @AuthenticationPrincipal ParkingUserPrincipal principal) {
        Map<String, Integer> counts = backupService.importReplacing(backup, principal.id());
        return new ImportResult(backupService.schemaVersion(), counts);
    }

    public record ImportResult(String schemaVersion, Map<String, Integer> counts) {
    }
}
