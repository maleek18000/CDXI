package com.anhdaden

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink

class XtreamIPTVProvider(
    private val _mainUrl: String,
    private val _name: String,
    private val username: String,
    private val password: String,
) : MainAPI() {
    override var name = _name
    override var mainUrl = _mainUrl
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Live, TvType.Movie, TvType.TvSeries)
    override var lang = "en"

    private val apiURL = "$_mainUrl/player_api.php?username=$username&password=$password"

    // Cache
    private var cachedLiveStreams: List<Stream>? = null
    private var cachedVodStreams: List<VodStream>? = null
    private var cachedSeriesList: List<SeriesItem>? = null

    // Single placeholder section - we return all categories dynamically
    override val mainPage = mainPageOf("" to name)

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val home = mutableListOf<HomePageList>()

        // Fetch live categories and streams
        try {
            val liveCategories = fetchCategories("get_live_categories")
            val liveStreams = fetchLiveStreams()
            if (liveCategories != null && liveStreams != null) {
                liveCategories.forEach { category ->
                    val streams = liveStreams.filter { it.category_id == category.category_id }
                    if (streams.isNotEmpty()) {
                        val items = streams.map { stream ->
                            val data = Data(
                                type = ContentType.LIVE,
                                streamId = stream.stream_id,
                                name = stream.name,
                                categoryId = stream.category_id,
                                posterUrl = stream.stream_icon
                            )
                            newLiveSearchResponse(stream.name, mapper.writeValueAsString(data)) {
                                posterUrl = stream.stream_icon
                            }
                        }
                        home.add(HomePageList("[Live] ${category.category_name}", items, isHorizontalImages = true))
                    }
                }
            }
        } catch (_: Exception) {}

        // Fetch VOD categories and streams
        try {
            val vodCategories = fetchCategories("get_vod_categories")
            val vodStreams = fetchVodStreams()
            if (vodCategories != null && vodStreams != null) {
                vodCategories.forEach { category ->
                    val streams = vodStreams.filter { it.category_id == category.category_id }
                    if (streams.isNotEmpty()) {
                        val items = streams.map { stream ->
                            val data = Data(
                                type = ContentType.VOD,
                                streamId = stream.stream_id,
                                name = stream.name,
                                categoryId = stream.category_id,
                                posterUrl = stream.stream_icon,
                                extension = stream.container_extension
                            )
                            newMovieSearchResponse(stream.name, mapper.writeValueAsString(data)) {
                                posterUrl = stream.stream_icon
                            }
                        }
                        home.add(HomePageList("[Movie] ${category.category_name}", items, isHorizontalImages = true))
                    }
                }
            }
        } catch (_: Exception) {}

        // Fetch series categories and items
        try {
            val seriesCategories = fetchCategories("get_series_categories")
            val seriesItems = fetchSeriesList()
            if (seriesCategories != null && seriesItems != null) {
                seriesCategories.forEach { category ->
                    val filteredSeries = seriesItems.filter { it.category_id == category.category_id }
                    if (filteredSeries.isNotEmpty()) {
                        val items = filteredSeries.map { item ->
                            val data = Data(
                                type = ContentType.SERIES,
                                streamId = item.series_id,
                                name = item.name,
                                categoryId = item.category_id,
                                posterUrl = item.cover,
                                plot = item.plot
                            )
                            newTvSeriesSearchResponse(item.name, mapper.writeValueAsString(data)) {
                                posterUrl = item.cover
                            }
                        }
                        home.add(HomePageList("[Series] ${category.category_name}", items, isHorizontalImages = true))
                    }
                }
            }
        } catch (_: Exception) {}

        return newHomePageResponse(home, hasNext = false)
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        val results = mutableListOf<SearchResponse>()

        // Search live
        try {
            val liveStreams = fetchLiveStreams()
            liveStreams?.filter { it.name.contains(query, ignoreCase = true) }?.forEach { stream ->
                val data = Data(
                    type = ContentType.LIVE,
                    streamId = stream.stream_id,
                    name = stream.name,
                    categoryId = stream.category_id,
                    posterUrl = stream.stream_icon
                )
                results.add(newLiveSearchResponse(stream.name, mapper.writeValueAsString(data)) {
                    posterUrl = stream.stream_icon
                })
            }
        } catch (_: Exception) {}

        // Search VOD
        try {
            val vodStreams = fetchVodStreams()
            vodStreams?.filter { it.name.contains(query, ignoreCase = true) }?.forEach { stream ->
                val data = Data(
                    type = ContentType.VOD,
                    streamId = stream.stream_id,
                    name = stream.name,
                    categoryId = stream.category_id,
                    posterUrl = stream.stream_icon,
                    extension = stream.container_extension
                )
                results.add(newMovieSearchResponse(stream.name, mapper.writeValueAsString(data)) {
                    posterUrl = stream.stream_icon
                })
            }
        } catch (_: Exception) {}

        // Search series
        try {
            val seriesItems = fetchSeriesList()
            seriesItems?.filter { it.name.contains(query, ignoreCase = true) }?.forEach { item ->
                val data = Data(
                    type = ContentType.SERIES,
                    streamId = item.series_id,
                    name = item.name,
                    categoryId = item.category_id,
                    posterUrl = item.cover,
                    plot = item.plot
                )
                results.add(newTvSeriesSearchResponse(item.name, mapper.writeValueAsString(data)) {
                    posterUrl = item.cover
                })
            }
        } catch (_: Exception) {}

        return results
    }

    override suspend fun load(url: String): LoadResponse? {
        val data = try {
            mapper.readValue(url, Data::class.java)
        } catch (e: Exception) {
            return null
        }

        return when (data.type) {
            ContentType.LIVE -> {
                // Stream URL: {mainUrl}/{username}/{password}/{streamId}
                val streamUrl = "$_mainUrl/$username/$password/${data.streamId}"
                newLiveStreamLoadResponse(data.name, url, streamUrl)
            }

            ContentType.VOD -> {
                // Stream URL: {mainUrl}/movie/{username}/{password}/{streamId}.{extension}
                val ext = data.extension ?: "mkv"
                val streamUrl = "$_mainUrl/movie/$username/$password/${data.streamId}.$ext"
                newMovieLoadResponse(data.name, url, TvType.Movie, streamUrl) {
                    posterUrl = data.posterUrl
                    plot = data.plot
                }
            }

            ContentType.SERIES -> {
                // Fetch series info to get episodes
                val seriesInfoUrl = "$apiURL&action=get_series_info&series_id=${data.streamId}"
                val response = app.get(seriesInfoUrl)
                val seriesInfo = try {
                    mapper.readValue(response.text, SeriesInfo::class.java)
                } catch (e: Exception) {
                    return null
                }

                val episodes = mutableListOf<Episode>()
                seriesInfo.episodes?.forEach { (seasonNum, episodeList) ->
                    episodeList.forEach { epInfo ->
                        val epId = epInfo.id
                        val epName = epInfo.name ?: "Episode ${epInfo.ep_num}"
                        val epExt = epInfo.container_extension ?: "mkv"
                        val seasonInt = seasonNum.toIntOrNull()
                        val epNumInt = epInfo.ep_num?.toIntOrNull()

                        // Stream URL: {mainUrl}/series/{username}/{password}/{episodeId}.{extension}
                        val streamUrl = "$_mainUrl/series/$username/$password/${epId}.$epExt"

                        // Pass the stream URL directly as episode data so loadLinks can use it
                        episodes.add(newEpisode(streamUrl) {
                            name = epName
                            season = seasonInt
                            episode = epNumInt
                            posterUrl = epInfo.info?.movie_image
                        })
                    }
                }

                newTvSeriesLoadResponse(
                    seriesInfo.info?.name ?: data.name,
                    url,
                    TvType.TvSeries,
                    episodes
                ) {
                    posterUrl = seriesInfo.info?.cover ?: data.posterUrl
                    plot = seriesInfo.info?.plot ?: data.plot
                    year = seriesInfo.info?.releaseDate?.take(4)?.toIntOrNull()
                }
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        // data is either a direct stream URL (for episodes) or JSON Data object
        val streamUrl = if (data.startsWith("http")) {
            data
        } else {
            // Parse from Data object (for live/VOD)
            val contentData = try {
                mapper.readValue(data, Data::class.java)
            } catch (e: Exception) {
                return false
            }

            when (contentData.type) {
                ContentType.LIVE -> "$_mainUrl/$username/$password/${contentData.streamId}"
                ContentType.VOD -> {
                    val ext = contentData.extension ?: "mkv"
                    "$_mainUrl/movie/$username/$password/${contentData.streamId}.$ext"
                }
                ContentType.SERIES -> return false // Series episodes pass URL directly
            }
        }

        if (streamUrl.isEmpty()) return false

        // Determine link type from URL
        val type = when {
            streamUrl.contains(".m3u8", ignoreCase = true) -> ExtractorLinkType.M3U8
            streamUrl.contains(".mpd", ignoreCase = true) -> ExtractorLinkType.DASH
            else -> null // auto-infer
        }

        callback(
            newExtractorLink(source = name, name = name, url = streamUrl, type = type) {
                referer = _mainUrl
                quality = Qualities.Unknown.value
                headers = mapOf(
                    "User-Agent" to "VLC/3.0.0",
                    "Referer" to _mainUrl
                )
            }
        )
        return true
    }

    // ---- API fetch helpers ----

    private suspend fun fetchCategories(action: String): List<Category>? {
        return try {
            val response = app.get("$apiURL&action=$action")
            mapper.readValue(response.text, Array<Category>::class.java).toList()
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchLiveStreams(): List<Stream>? {
        if (cachedLiveStreams != null) return cachedLiveStreams
        return try {
            val response = app.get("$apiURL&action=get_live_streams")
            cachedLiveStreams = mapper.readValue(response.text, Array<Stream>::class.java).toList()
            cachedLiveStreams
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchVodStreams(): List<VodStream>? {
        if (cachedVodStreams != null) return cachedVodStreams
        return try {
            val response = app.get("$apiURL&action=get_vod_streams")
            cachedVodStreams = mapper.readValue(response.text, Array<VodStream>::class.java).toList()
            cachedVodStreams
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchSeriesList(): List<SeriesItem>? {
        if (cachedSeriesList != null) return cachedSeriesList
        return try {
            val response = app.get("$apiURL&action=get_series")
            cachedSeriesList = mapper.readValue(response.text, Array<SeriesItem>::class.java).toList()
            cachedSeriesList
        } catch (e: Exception) {
            null
        }
    }
}
