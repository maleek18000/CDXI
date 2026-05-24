package com.anhdaden

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

// ---- Account model ----
data class Link(
    val name: String,
    val mainUrl: String,
    val username: String,
    val password: String,
)

// ---- Content type enum ----
enum class ContentType {
    LIVE, VOD, SERIES
}

// ---- Categories ----
@JsonIgnoreProperties(ignoreUnknown = true)
data class Category(
    @JsonProperty("category_id") val category_id: String = "",
    @JsonProperty("category_name") val category_name: String = "",
    @JsonProperty("parent_id") val parent_id: Int = 0,
)

// ---- Live Streams ----
@JsonIgnoreProperties(ignoreUnknown = true)
data class Stream(
    @JsonProperty("num") val num: Int = 0,
    @JsonProperty("name") val name: String = "",
    @JsonProperty("stream_type") val stream_type: String? = null,
    @JsonProperty("stream_id") val stream_id: Int = 0,
    @JsonProperty("stream_icon") val stream_icon: String? = null,
    @JsonProperty("epg_channel_id") val epg_channel_id: String? = null,
    @JsonProperty("added") val added: String? = null,
    @JsonProperty("category_id") val category_id: String = "",
    @JsonProperty("custom_sid") val custom_sid: String? = null,
    @JsonProperty("tv_archive") val tv_archive: Int = 0,
    @JsonProperty("direct_source") val direct_source: String? = null,
    @JsonProperty("tv_archive_duration") val tv_archive_duration: Int = 0,
)

// ---- VOD Streams ----
@JsonIgnoreProperties(ignoreUnknown = true)
data class VodStream(
    @JsonProperty("num") val num: Int = 0,
    @JsonProperty("name") val name: String = "",
    @JsonProperty("stream_type") val stream_type: String? = null,
    @JsonProperty("stream_id") val stream_id: Int = 0,
    @JsonProperty("stream_icon") val stream_icon: String? = null,
    @JsonProperty("rating") val rating: String? = null,
    @JsonProperty("rating_5based") val rating_5based: Double? = null,
    @JsonProperty("added") val added: String? = null,
    @JsonProperty("category_id") val category_id: String = "",
    @JsonProperty("container_extension") val container_extension: String? = null,
    @JsonProperty("custom_sid") val custom_sid: String? = null,
    @JsonProperty("direct_source") val direct_source: String? = null,
)

// ---- Series ----
@JsonIgnoreProperties(ignoreUnknown = true)
data class SeriesItem(
    @JsonProperty("num") val num: Int = 0,
    @JsonProperty("name") val name: String = "",
    @JsonProperty("series_id") val series_id: Int = 0,
    @JsonProperty("cover") val cover: String? = null,
    @JsonProperty("plot") val plot: String? = null,
    @JsonProperty("cast") val cast: String? = null,
    @JsonProperty("director") val director: String? = null,
    @JsonProperty("genre") val genre: String? = null,
    @JsonProperty("releaseDate") val releaseDate: String? = null,
    @JsonProperty("last_modified") val last_modified: String? = null,
    @JsonProperty("rating") val rating: String? = null,
    @JsonProperty("rating_5based") val rating_5based: Double? = null,
    @JsonProperty("category_id") val category_id: String = "",
)

// ---- Series Info ----
@JsonIgnoreProperties(ignoreUnknown = true)
data class SeriesInfo(
    @JsonProperty("seasons") val seasons: List<SeasonInfo>? = null,
    @JsonProperty("info") val info: SeriesDetailInfo? = null,
    @JsonProperty("episodes") val episodes: Map<String, List<EpisodeInfo>>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SeriesDetailInfo(
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("cover") val cover: String? = null,
    @JsonProperty("plot") val plot: String? = null,
    @JsonProperty("cast") val cast: String? = null,
    @JsonProperty("director") val director: String? = null,
    @JsonProperty("genre") val genre: String? = null,
    @JsonProperty("releaseDate") val releaseDate: String? = null,
    @JsonProperty("rating") val rating: String? = null,
    @JsonProperty("rating_5based") val rating_5based: Double? = null,
    @JsonProperty("category_id") val category_id: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SeasonInfo(
    @JsonProperty("air_date") val air_date: String? = null,
    @JsonProperty("episode_count") val episode_count: Int? = null,
    @JsonProperty("id") val id: Int? = null,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("overview") val overview: String? = null,
    @JsonProperty("season_number") val season_number: Int? = null,
    @JsonProperty("cover") val cover: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EpisodeInfo(
    @JsonProperty("id") val id: Int = 0,
    @JsonProperty("ep_num") val ep_num: String? = null,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("container_extension") val container_extension: String? = null,
    @JsonProperty("info") val info: EpisodeDetailInfo? = null,
    @JsonProperty("subtitles") val subtitles: List<Any>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EpisodeDetailInfo(
    @JsonProperty("movie_image") val movie_image: String? = null,
    @JsonProperty("plot") val plot: String? = null,
    @JsonProperty("duration_secs") val duration_secs: Int? = null,
    @JsonProperty("duration") val duration: String? = null,
)

// ---- Data passed through url field ----
data class Data(
    val type: ContentType,
    val streamId: Int,
    val name: String = "",
    val categoryId: String = "",
    val posterUrl: String? = null,
    val extension: String? = null,
    val plot: String? = null,
)
