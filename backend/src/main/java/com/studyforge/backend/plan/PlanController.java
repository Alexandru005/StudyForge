package com.studyforge.backend.plan;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.studyforge.backend.auth.CurrentUser;

/**
 * Pagina 2 (generare plan) + listare/descarcare pentru pagina 4.
 */
@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService service;

    public PlanController(PlanService service) {
        this.service = service;
    }

    /**
     * multipart/form-data:
     * - file: PDF-ul cu programa + evaluari (obligatoriu)
     * - startDate: YYYY-MM-DD (optional, implicit azi; se rotunjeste la lunea saptamanii)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PlanDtos.PlanDetails> create(
            @RequestPart("file") MultipartFile file,
            @RequestParam(name = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(file, startDate, CurrentUser.id(jwt)));
    }

    @GetMapping
    public List<PlanDtos.PlanSummary> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(CurrentUser.id(jwt));
    }

    @GetMapping("/{id}")
    public PlanDtos.PlanDetails get(@PathVariable("id") UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.get(id, CurrentUser.id(jwt));
    }

    /**
     * ?download=true -> descarcare; altfel se deschide in browser.
     * Pentru link-uri directe: ?access_token=TOKEN (vezi SecurityConfig).
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable("id") UUID id,
            @RequestParam(name = "download", defaultValue = "false") boolean download,
            @AuthenticationPrincipal Jwt jwt) {
        return service.pdf(id, CurrentUser.id(jwt)).toResponse(download);
    }
}
