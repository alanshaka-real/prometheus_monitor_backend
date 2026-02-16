package com.wenmin.prometheus.module.panel.controller;

import com.wenmin.prometheus.common.result.R;
import com.wenmin.prometheus.module.panel.entity.PromPanelTemplate;
import com.wenmin.prometheus.module.panel.service.PanelTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prometheus/panel-templates")
@RequiredArgsConstructor
public class PanelTemplateController {

    private final PanelTemplateService panelTemplateService;

    /**
     * List panel templates with optional filters
     */
    @GetMapping
    public R<Map<String, Object>> listPanelTemplates(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String exporterType,
            @RequestParam(required = false) String keyword) {
        return R.ok(panelTemplateService.listPanelTemplates(category, exporterType, keyword));
    }

    /**
     * Get panel template category tree
     */
    @GetMapping("/tree")
    public R<List<Map<String, Object>>> getCategoryTree() {
        return R.ok(panelTemplateService.getCategoryTree());
    }

    /**
     * Get a single panel template by id
     */
    @GetMapping("/{id}")
    public R<PromPanelTemplate> getPanelTemplate(@PathVariable String id) {
        return R.ok(panelTemplateService.getById(id));
    }
}
