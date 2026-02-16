package com.wenmin.prometheus.module.query.vo;

import lombok.Data;

import java.util.List;

@Data
public class TemplateTreeVO {

    private String category;
    private String exporterType;
    private List<SubCategoryGroup> subCategories;

    @Data
    public static class SubCategoryGroup {
        private String subCategory;
        private List<TemplateItem> templates;
    }

    @Data
    public static class TemplateItem {
        private String id;
        private String name;
        private String query;
        private String description;
        private List<String> variables;
        private String unit;
        private String unitFormat;
    }
}
