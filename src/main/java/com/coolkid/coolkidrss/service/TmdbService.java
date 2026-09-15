package com.coolkid.coolkidrss.service;

import com.coolkid.coolkidrss.model.tmdb.TmdbMediaInfo;

import java.util.Optional;

/** TMDB 查询服务。 */
public interface TmdbService {

    /** 根据影视名称和可选年份查询 TMDB 详情。 */
    Optional<TmdbMediaInfo> findByName(String name, Integer releaseYear);
}
