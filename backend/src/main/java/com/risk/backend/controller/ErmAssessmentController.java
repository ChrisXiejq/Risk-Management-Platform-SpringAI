package com.risk.backend.controller;

import com.risk.backend.security.ErmUserDetails;
import com.risk.backend.service.intf.ErmAssessedRiskService;
import com.risk.backend.service.intf.ErmAssessmentCommandService;
import com.risk.backend.service.intf.ErmAssessmentQueryService;
import com.risk.backend.model.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/erm/assessments")
public class ErmAssessmentController {

    private final ErmAssessmentQueryService assessmentQueryService;
    private final ErmAssessmentCommandService assessmentCommandService;
    private final ErmAssessedRiskService assessedRiskService;

    public ErmAssessmentController(ErmAssessmentQueryService assessmentQueryService,
                                   ErmAssessmentCommandService assessmentCommandService,
                                   ErmAssessedRiskService assessedRiskService) {
        this.assessmentQueryService = assessmentQueryService;
        this.assessmentCommandService = assessmentCommandService;
        this.assessedRiskService = assessedRiskService;
    }

    @GetMapping
    public Result list(@AuthenticationPrincipal ErmUserDetails user,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size) {
        return Result.success(assessmentQueryService.list(user.getTenantId(), page, size));
    }

    @PostMapping
    public Result create(@AuthenticationPrincipal ErmUserDetails user, @Valid @RequestBody CreateAssessmentBody body) {
        try {
            long id = assessmentCommandService.create(user.getTenantId(), user.getUserId(), body.title(), body.framework());
            return Result.success(java.util.Map.of("id", id));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result detail(@AuthenticationPrincipal ErmUserDetails user, @PathVariable long id) {
        try {
            return Result.success(assessmentQueryService.detail(user.getTenantId(), id));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PatchMapping("/{id}")
    public Result patch(@AuthenticationPrincipal ErmUserDetails user, @PathVariable long id,
                        @Valid @RequestBody PatchAssessmentBody body) {
        try {
            assessmentCommandService.updateMeta(user.getTenantId(), id, body.status(), body.summary(), body.chatId());
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/links")
    public Result links(@AuthenticationPrincipal ErmUserDetails user, @PathVariable long id,
                        @RequestBody LinksBody body) {
        try {
            assessmentCommandService.setLinks(user.getTenantId(), id,
                    body.assetIds(), body.threatIds(), body.vulnerabilityIds(), body.measureIds());
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/risks")
    public Result addRisk(@AuthenticationPrincipal ErmUserDetails user, @PathVariable long id,
                          @Valid @RequestBody AssessedRiskBody body) {
        try {
            long rid = assessedRiskService.addRisk(user.getTenantId(), id, body.title(), body.likelihood(), body.impact(),
                    body.notes(), body.treatment());
            return Result.success(java.util.Map.of("id", rid));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{assessmentId}/risks/{riskId}")
    public Result deleteRisk(@AuthenticationPrincipal ErmUserDetails user,
                             @PathVariable long assessmentId,
                             @PathVariable long riskId) {
        try {
            assessedRiskService.deleteRisk(user.getTenantId(), riskId);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    public record CreateAssessmentBody(@NotBlank String title, String framework) {}

    public record PatchAssessmentBody(String status, String summary, String chatId) {}

    public record LinksBody(List<Long> assetIds, List<Long> threatIds, List<Long> vulnerabilityIds, List<Long> measureIds) {}

    public record AssessedRiskBody(
            String title,
            @NotNull Integer likelihood,
            @NotNull Integer impact,
            String notes,
            String treatment
    ) {}
}
