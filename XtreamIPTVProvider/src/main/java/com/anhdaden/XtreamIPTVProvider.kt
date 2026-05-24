package com.anhdaden

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink
import com.fasterxml.jackson.databind.type.MapType

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

    override val mainPage = mainPageOf("" to name)

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val home = mutableListOf<HomePageList>()

        // ===== LIVE TV - fetch all at once (usually smaller dataset) =====
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
                        home.add(HomePageList("[LIVE] ${category.category_name}", items, isHorizontalImages = true))
                    }
                }
            }
        } catch (_: Exception) {}

        // ===== VOD (Movies) - fetch per category to avoid timeout =====
        try {
            val vodCategories = fetchCategories("get_vod_categories")
            if (vodCategories != null) {
                vodCategories.forEach { category ->
                    try {
                        val vodStreams = fetchVodStreamsByCategory(category.category_id)
                        if (vodStreams != null && vodStreams.isNotEmpty()) {
                            val items = vodStreams.map { stream ->
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
                            home.add(HomePageList("[MOVIE] ${category.category_name}", items, isHorizontalImages = true))
                        }
                    } catch (_: Exception) {
                        // Skip this category if it fails, try the next one
                    }
                }
            }
        } catch (_: Exception) {}

        // ===== SERIES - fetch per category to avoid timeout =====
        try {
            val seriesCategories = fetchCategories("get_series_categories")
            if (seriesCategories != null) {
                seriesCategories.forEach { category ->
                    try {
                        val seriesItems = fetchSeriesByCategory(category.category_id)
                        if (seriesItems != null && seriesItems.isNotEmpty()) {
                            val items = seriesItems.map { item ->
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
                            home.add(HomePageList("[SERIES] ${category.category_name}", items, isHorizontalImages = true))
                        }
                    } catch (_: Exception) {
                        // Skip this category if it fails, try the next one
                    }
                }
            }
        } catch (_: Exception) {}

        if (home.isEmpty()) {
            home.add(HomePageList("Error", listOf(
                newLiveSearchResponse("No content found. Check your IPTV settings in the plugin.", "")
            )))
        }

        return newHomePageResponse(home, hasNext = false)
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        val results = mutableListOf<SearchResponse>()

        // Search live
        try {
            fetchLiveStreams()?.filter { it.name.contains(query, ignoreCase = true) }?.forEach { stream ->
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

        // Search VOD - fetch all VOD for searching (can't filter by category in search)
        try {
            fetchVodStreams()?.filter { it.name.contains(query, ignoreCase = true) }?.forEach { stream ->
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
            fetchSeriesList()?.filter { it.name.contains(query, ignoreCase = true) }?.forEach { item ->
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
                val streamUrl = "$_mainUrl/$username/$password/${data.streamId}"
                newLiveStreamLoadResponse(data.name, url, streamUrl)
            }

            ContentType.VOD -> {
                val ext = data.extension ?: "mkv"
                val streamUrl = "$_mainUrl/movie/$username/$password/${data.streamId}.$ext"
                newMovieLoadResponse(data.name, url, TvType.Movie, streamUrl) {
                    posterUrl = data.posterUrl
                    plot = data.plot
                }
            }

            ContentType.SERIES -> {
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

                        val streamUrl = "$_mainUrl/series/$username/$password/${epId}.$epExt"

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
        val streamUrl = if (data.startsWith("http")) {
            data
        } else {
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
                ContentType.SERIES -> return false
            }
        }

        if (streamUrl.isEmpty()) return false

        val type = when {
            streamUrl.contains(".m3u8", ignoreCase = true) -> ExtractorLinkType.M3U8
            streamUrl.contains(".mpd", ignoreCase = true) -> ExtractorLinkType.DASH
            else -> null
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
            val response = app.get("$apiURL&action=$action", cacheTime = 0)
            val text = response.text
            if (text.isNullOrEmpty() || !text.trimStart().startsWith("[")) return null
            mapper.readValue(text, Array<Category>::class.java).toList()
        } catch (_: Exception) {
            null
        }
    }

    // Live streams - fetch all at once (typically smaller)
    private var cachedLiveStreams: List<Stream>? = null
    private suspend fun fetchLiveStreams(): List<Stream>? {
        if (cachedLiveStreams != null) return cachedLiveStreams
        return try {
            val response = app.get("$apiURL&action=get_live_streams", cacheTime = 0)
            val text = response.text
            if (text.isNullOrEmpty()) return null
            cachedLiveStreams = mapper.readValue(text, Array<Stream>::class.java).toList()
            cachedLiveStreams
        } catch (_: Exception) {
            null
        }
    }

    // VOD streams - fetch per category (smaller responses, avoid timeout)
    private var cachedVodStreams: List<VodStream>? = null
    private suspend fun fetchVodStreams(): List<VodStream>? {
        if (cachedVodStreams != null) return cachedVodStreams
        return try {
            val response = app.get("$apiURL&action=get_vod_streams", cacheTime = 0)
            val text = response.text
            if (text.isNullOrEmpty()) return null
            cachedVodStreams = parseStreamList(text, VodStream::class.java)
            cachedVodStreams
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun fetchVodStreamsByCategory(categoryId: String): List<VodStream>? {
        return try {
            val response = app.get("$apiURL&action=get_vod_streams&category_id=$categoryId", cacheTime = 0)
            val text = response.text
            if (text.isNullOrEmpty()) return null
            parseStreamList(text, VodStream::class.java)
        } catch (_: Exception) {
            null
        }
    }

    // Series - fetch per category
    private var cachedSeriesList: List<SeriesItem>? = null
    private suspend fun fetchSeriesList(): List<SeriesItem>? {
        if (cachedSeriesList != null) return cachedSeriesList
        return try {
            val response = app.get("$apiURL&action=get_series", cacheTime = 0)
            val text = response.text
            if (text.isNullOrEmpty()) return null
            cachedSeriesList = parseStreamList(text, SeriesItem::class.java)
            cachedSeriesList
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun fetchSeriesByCategory(categoryId: String): List<SeriesItem>? {
        return try {
            val response = app.get("$apiURL&action=get_series&category_id=$categoryId", cacheTime = 0)
            val text = response.text
            if (text.isNullOrEmpty()) return null
            parseStreamList(text, SeriesItem::class.java)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parse a stream list that could be either a JSON array or a JSON object
     * Some IPTV servers return: [{"num":1,...},{"num":2,...}]
     * Others return: {"1":{"num":1,...},"2":{"num":2,...}}
     */
    private fun <T> parseStreamList(text: String, clazz: Class<T>): List<T>? {
        val trimmed = text.trimStart()
        return try {
            if (trimmed.startsWith("[")) {
                // Standard array format
                val arrayType = mapper.typeFactory.constructArrayType(clazz)
                mapper.readValue<List<T>>(text, arrayType)
            } else if (trimmed.startsWith("{")) {
                // Object format - values are the items
                val mapType: MapType = mapper.typeFactory.constructMapType(
                    Map::class.java,
                    String::class.java,
                    clazz
                )
                val map = mapper.readValue<Map<String, T>>(text, mapType)
                map.values.toList()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
