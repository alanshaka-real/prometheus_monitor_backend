package com.wenmin.prometheus.common.page;

import lombok.Data;

@Data
public class PageQuery {
    private Integer current = 1;
    private Integer size = 10;
}
