package com.risk.backend.controller;

import com.risk.backend.security.ErmUserDetails;
import com.risk.backend.service.intf.ErmAssetService;
import com.risk.backend.service.intf.ErmSecurityMeasureService;
import com.risk.backend.service.intf.ErmThreatService;
import com.risk.backend.service.intf.ErmVulnerabilityService;
import com.risk.backend.model.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/erm")
public class ErmIdentificationController {

    private final ErmAssetService assetService;
    private final ErmThreatService threatService;
    private final ErmVulnerabilityService vulnerabilityService;
    private final ErmSecurityMeasureService securityMeasureService;

    public ErmIdentificationController(ErmAssetService assetService,
                                       ErmThreatService threatService,
                                       ErmVulnerabilityService vulnerabilityService,
                                       ErmSecurityMeasureService securityMeasureService) {
        this.assetService = assetService;
        this.threatService = threatService;
        this.vulnerabilityService = vulnerabilityService;
        this.securityMeasureService = securityMeasureService;
    }

    // --- assets ---
    @GetMapping("/assets")
    public Result assets(@AuthenticationPrincipal ErmUserDetails user) {
        return Result.success(assetService.listAssets(user.getTenantId()));
    }

    @PostMapping("/assets")
    public Result createAsset(@AuthenticationPrincipal ErmUserDetails user, @Valid @RequestBody AssetBody body) {
        try {
            long id = assetService.createAsset(user.getTenantId(), body.name(), body.category(), body.criticality(),
                    body.description(), body.ownerLabel(), body.locationLabel());
            return Result.success(java.util.Map.of("id", id));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/assets/{id}")
    public Result updateAsset(@AuthenticationPrincipal ErmUserDetails user, @PathVariable long id,
                              @Valid @RequestBody AssetUpdateBody body) {
        try {
            assetService.updateAsset(user.getTenantId(), id, body.name(), body.category(), body.criticality(),
                    body.description(), body.ownerLabel(), body.locationLabel());
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/assets/{id}")
    public Result deleteAsset(@AuthenticationPrincipal ErmUserDetails user, @PathVariable long id) {
        try {
            assetService.deleteAsset(user.getTenantId(), id);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    // --- threats ---
    @GetMapping("/threats")
    public Result threats(@AuthenticationPrincipal ErmUserDetails user) {
        return Result.success(threatService.listThreats(user.getTenantId()));
    }

    @PostMapping("/threats")
    public Result createThreat(@AuthenticationPrincipal ErmUserDetails user, @Valid @RequestBody ThreatBody body) {
        long id = threatService.createThreat(user.getTenantId(), body.name(), body.category(), body.description(), body.sourceLabel());
        return Result.success(java.util.Map.of("id", id));
    }

    @PutMapping("/threats/{id}")
    public Result updateThreat(@AuthenticationPrincipal ErmUserDetails user, @PathVariable long id,
                               @Valid @RequestBody ThreatBody body) {
        try {
            threatService.updateThreat(user.getTenantId(), id, body.name(), body.category(), body.description(), body.sourceLabel());
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/threats/{id}")
    public Result deleteThreat(@AuthenticationPrincipal ErmUserDetails user, @PathVariable long id) {
        try {
            threatService.deleteThreat(user.getTenantId(), id);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    // --- vulnerabilities ---
    @GetMapping("/vulnerabilities")
    public Result vulns(@AuthenticationPrincipal ErmUserDetails user) {
        return Result.success(vulnerabilityService.listVulns(user.getTenantId()));
    }

    @PostMapping("/vulnerabilities")
    public Result createVuln(@AuthenticationPrincipal ErmUserDetails user, @Valid @RequestBody VulnBody body) {
        long id = vulnerabilityService.createVuln(user.getTenantId(), body.name(), body.severity(), body.description(), body.relatedAssetId());
        return Result.success(java.util.Map.of("id", id));
    }

    @PutMapping("/vulnerabilities/{id}")
    public Result updateVuln(@AuthenticationPrincipal ErmUserDetails user, @PathVariable long id,
                             @Valid @RequestBody VulnBody body) {
        try {
            vulnerabilityService.updateVuln(user.getTenantId(), id, body.name(), body.severity(), body.description(), body.relatedAssetId());
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/vulnerabilities/{id}")
    public Result deleteVuln(@AuthenticationPrincipal ErmUserDetails user, @PathVariable long id) {
        try {
            vulnerabilityService.deleteVuln(user.getTenantId(), id);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    // --- security measures ---
    @GetMapping("/measures")
    public Result measures(@AuthenticationPrincipal ErmUserDetails user) {
        return Result.success(securityMeasureService.listMeasures(user.getTenantId()));
    }

    @PostMapping("/measures")
    public Result createMeasure(@AuthenticationPrincipal ErmUserDetails user, @Valid @RequestBody MeasureBody body) {
        long id = securityMeasureService.createMeasure(user.getTenantId(), body.name(), body.measureType(), body.description(), body.effectivenessNote());
        return Result.success(java.util.Map.of("id", id));
    }

    @PutMapping("/measures/{id}")
    public Result updateMeasure(@AuthenticationPrincipal ErmUserDetails user, @PathVariable long id,
                                @Valid @RequestBody MeasureBody body) {
        try {
            securityMeasureService.updateMeasure(user.getTenantId(), id, body.name(), body.measureType(), body.description(), body.effectivenessNote());
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/measures/{id}")
    public Result deleteMeasure(@AuthenticationPrincipal ErmUserDetails user, @PathVariable long id) {
        try {
            securityMeasureService.deleteMeasure(user.getTenantId(), id);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    public record AssetBody(
            @NotBlank String name,
            String category,
            Integer criticality,
            String description,
            String ownerLabel,
            String locationLabel
    ) {}

    public record AssetUpdateBody(
            @NotBlank String name,
            String category,
            @NotNull Integer criticality,
            String description,
            String ownerLabel,
            String locationLabel
    ) {}

    public record ThreatBody(
            @NotBlank String name,
            String category,
            String description,
            String sourceLabel
    ) {}

    public record VulnBody(
            @NotBlank String name,
            String severity,
            String description,
            Long relatedAssetId
    ) {}

    public record MeasureBody(
            @NotBlank String name,
            String measureType,
            String description,
            String effectivenessNote
    ) {}
}
