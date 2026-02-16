package com.wenmin.prometheus.common.page;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private List<T> records;
    private long current;
    private long size;
    private long total;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.records = page.getRecords();
        result.current = page.getCurrent();
        result.size = page.getSize();
        result.total = page.getTotal();
        return result;
    }

    public static <T> PageResult<T> of(List<T> records, long total) {
        PageResult<T> result = new PageResult<>();
        result.records = records;
        result.total = total;
        result.current = 1;
        result.size = records.size();
        return result;
    }
}
