package com.coolkid.coolkidrss.model.response;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author coolk
 * @version 1.0
 * @date 2024/8/12
 */
public class Page<T> {
    @Getter
    protected List<T> records;
    @Getter
    protected Long total;
    @Getter
    protected long size;
    @Getter
    protected long current;
    protected boolean optimizeCountSql;
    protected boolean searchCount;
    protected boolean optimizeJoinOfCountSql;
    protected Long maxLimit;
    protected String countId;

    public Page() {
        this.records = new ArrayList<>();
        this.total = 0L;
        this.size = 10L;
        this.current = 1L;
        this.optimizeCountSql = true;
        this.searchCount = true;
        this.optimizeJoinOfCountSql = true;
    }

    public Page(long current, long size) {
        this(current, size, 0L);
    }

    public Page(long current, long size, long total) {
        this(current, size, total, true);
    }

    public Page(long current, long size, boolean searchCount) {
        this(current, size, 0L, searchCount);
    }

    public Page(long current, long size, long total, boolean searchCount) {
        this.records = new ArrayList<>();
        this.total = 0L;
        this.size = 10L;
        this.current = 1L;
        this.optimizeCountSql = true;
        this.searchCount = true;
        this.optimizeJoinOfCountSql = true;
        if (current > 1L) {
            this.current = current;
        }

        this.size = size;
        this.total = total;
        this.searchCount = searchCount;
    }

    public boolean hasPrevious() {
        return this.current > 1L;
    }

    public boolean hasNext() {
        return this.current < this.getPages();
    }

    public Page<T> setRecords(List<T> records) {
        this.records = records;
        return this;
    }
    public Page<T> setTotal(long total) {
        this.total = total;
        return this;
    }

    public Page<T> setSize(long size) {
        this.size = size;
        return this;
    }

    public Page<T> setCurrent(long current) {
        this.current = current;
        return this;
    }

    public String countId() {
        return this.countId;
    }

    public Long maxLimit() {
        return this.maxLimit;
    }



    public boolean optimizeCountSql() {
        return this.optimizeCountSql;
    }

    public static <T> Page<T> of(long current, long size, long total, boolean searchCount) {
        return new Page<>(current, size, total, searchCount);
    }

    public boolean optimizeJoinOfCountSql() {
        return this.optimizeJoinOfCountSql;
    }

    public Page<T> setSearchCount(boolean searchCount) {
        this.searchCount = searchCount;
        return this;
    }

    public Page<T> setOptimizeCountSql(boolean optimizeCountSql) {
        this.optimizeCountSql = optimizeCountSql;
        return this;
    }

    public long getPages() {
        Long total2 = this.getTotal();
        if (Objects.isNull(total2)) {
            return 0L;
        }
        if (this.getSize() == 0L) {
            return 0L;
        } else {
            long pages = total2 / this.getSize();
            if (total2 % this.getSize() != 0L) {
                ++pages;
            }
            return pages;
        }
    }

    public long offset() {
        long current2 = this.getCurrent();
        return current2 <= 1L ? 0L : Math.max((current2 - 1L) * this.getSize(), 0L);
    }

    public static <T> Page<T> of(long current, long size) {
        return of(current, size, 0L);
    }

    public static <T> Page<T> of(long current, long size, long total) {
        return of(current, size, total, true);
    }

    public static <T> Page<T> of(long current, long size, boolean searchCount) {
        return of(current, size, 0L, searchCount);
    }

}

