package com.anhdaden

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Category(
    val category_id: String? = null,
    val category_name: String? = null,
    val parent_id: Int? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LiveStream(
    val num: Int? = null,
    val name: String? = null,
    val stream_type: String? = null,
    val stream_id: Int? = null,
    val stream_icon: String? = null,
    val epg_channel_id: String? = null,
    val added: String? = null,
    val category_id: String? = null,
    val custom_sid: String? = null,
    val tv_archive: Int? = null,
    val direct_source: String? = null,
    val tv_archive_duration: Int? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VodStream(
    val num: Int? = null,
    val name: String? = null,
    val stream_type: String? = null,
    val stream_id: Int? = null,
    val stream_icon: String? = null,
    val rating: String? = null,
    val rating_5based: Double? = null,
    val added: String? = null,
    val category_id: String? = null,
    val container_extension: String? = null,
    val custom_sid: String? = null,
    val direct_source: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SeriesItem(
    val num: Int? = null,
    val name: String? = null,
    val series_id: Int? = null,
    val cover: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val last_modified: String? = null,
    val rating: String? = null,
    val rating_5based: Double? = null,
    val category_id: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SeriesInfo(
    val seasons: List<SeasonInfo>? = null,
    val info: SeriesDetailInfo? = null,
    val episodes: Map<String, List<EpisodeInfo>>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SeriesDetailInfo(
    val name: String? = null,
    val cover: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val rating: String? = null,
    val rating_5based: Double? = null,
    val category_id: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SeasonInfo(
    val air_date: String? = null,
    val episode_count: Int? = null,
    val id: Int? = null,
    val name: String? = null,
    val overview: String? = null,
    val season_number: Int? = null,
    val cover: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EpisodeInfo(
    val id: Int? = null,
    val ep_num: String? = null,
    val name: String? = null,
    val container_extension: String? = null,
    val info: EpisodeDetailInfo? = null,
    val subtitles: List<Any>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EpisodeDetailInfo(
    val movie_image: String? = null,
    val plot: String? = null,
    val duration_secs: Int? = null,
    val duration: String? = null
)

enum class ContentType {
    LIVE, VOD, SERIES
}

data class ContentData(
    val type: ContentType,
    val id: String,
    val name: String = "",
    val extension: String = "",
    val categoryId: String = "",
    val posterUrl: String = "",
    val plot: String = ""
)
