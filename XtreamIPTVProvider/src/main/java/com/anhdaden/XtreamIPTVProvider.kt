package com.anhdaden

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink

class XtreamIPTVProvider : MainAPI() {
    override var name = "Xtream IPTV"
    override var mainUrl = "https://xtream.example.com"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Live, TvType.Movie, TvType.TvSeries)
    override var lang = "en"

    private val prefs by lazy { XtreamIPTVPlugin.getPrefs() }

    private fun getServerUrl(): String? = prefs?.getString("server_url", null)
    private fun getUsername(): String? = prefs?.getString("username", null)
    private fun getPassword(): String? = prefs?.getString("password", null)

    private fun getBaseUrl(): String? {
        val server = getServerUrl() ?: return null
        val user = getUsername() ?: return null
        val pass = getPassword() ?: return null
        return "$server/player_api.php?username=$user&password=$pass"
    }

    override val mainPage = listOf(
        MainPageData("live", "Live TV"),
        MainPageData("vod", "Movies"),
        MainPageData("series", "Series")
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val baseUrl = getBaseUrl() ?: return null

        val items = when (request.data) {
            "live" -> {
                val streams = fetchLiveStreams(baseUrl) ?: return null
                streams.mapNotNull { stream ->
                    val streamName = stream.name ?: return@mapNotNull null
                    val streamId = stream.stream_id ?: return@mapNotNull null
                    val data = mapper.writeValueAsString(
                        ContentData(
                            type = ContentType.LIVE,
                            id = streamId.toString(),
                            name = streamName,
                            posterUrl = stream.stream_icon ?: ""
                        )
                    )
                    newLiveSearchResponse(streamName, data) {
                        posterUrl = stream.stream_icon
                    }
                }
            }
            "vod" -> {
                val streams = fetchVodStreams(baseUrl) ?: return null
                streams.mapNotNull { stream ->
                    val streamName = stream.name ?: return@mapNotNull null
                    val streamId = stream.stream_id ?: return@mapNotNull null
                    val data = mapper.writeValueAsString(
                        ContentData(
                            type = ContentType.VOD,
                            id = streamId.toString(),
                            name = streamName,
                            extension = stream.container_extension ?: "mkv",
                            posterUrl = stream.stream_icon ?: ""
                        )
                    )
                    newMovieSearchResponse(streamName, data) {
                        posterUrl = stream.stream_icon
                    }
                }
            }
            "series" -> {
                val seriesList = fetchSeriesList(baseUrl) ?: return null
                seriesList.mapNotNull { item ->
                    val seriesName = item.name ?: return@mapNotNull null
                    val seriesId = item.series_id ?: return@mapNotNull null
                    val data = mapper.writeValueAsString(
                        ContentData(
                            type = ContentType.SERIES,
                            id = seriesId.toString(),
                            name = seriesName,
                            posterUrl = item.cover ?: ""
                        )
                    )
                    newTvSeriesSearchResponse(seriesName, data) {
                        posterUrl = item.cover
                    }
                }
            }
            else -> emptyList()
        }

        return newHomePageResponse(request.name, items, hasNext = false)
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        val baseUrl = getBaseUrl() ?: return null
        val results = mutableListOf<SearchResponse>()

        // Search live streams
        try {
            val liveStreams = fetchLiveStreams(baseUrl)
            liveStreams?.filter { it.name?.contains(query, ignoreCase = true) == true }?.forEach { stream ->
                val streamName = stream.name ?: return@forEach
                val streamId = stream.stream_id ?: return@forEach
                val data = mapper.writeValueAsString(
                    ContentData(
                        type = ContentType.LIVE,
                        id = streamId.toString(),
                        name = streamName,
                        posterUrl = stream.stream_icon ?: ""
                    )
                )
                results.add(newLiveSearchResponse(streamName, data) {
                    posterUrl = stream.stream_icon
                })
            }
        } catch (_: Exception) {}

        // Search VOD
        try {
            val vodStreams = fetchVodStreams(baseUrl)
            vodStreams?.filter { it.name?.contains(query, ignoreCase = true) == true }?.forEach { stream ->
                val streamName = stream.name ?: return@forEach
                val streamId = stream.stream_id ?: return@forEach
                val data = mapper.writeValueAsString(
                    ContentData(
                        type = ContentType.VOD,
                        id = streamId.toString(),
                        name = streamName,
                        extension = stream.container_extension ?: "mkv",
                        posterUrl = stream.stream_icon ?: ""
                    )
                )
                results.add(newMovieSearchResponse(streamName, data) {
                    posterUrl = stream.stream_icon
                })
            }
        } catch (_: Exception) {}

        // Search series
        try {
            val seriesList = fetchSeriesList(baseUrl)
            seriesList?.filter { it.name?.contains(query, ignoreCase = true) == true }?.forEach { item ->
                val seriesName = item.name ?: return@forEach
                val seriesId = item.series_id ?: return@forEach
                val data = mapper.writeValueAsString(
                    ContentData(
                        type = ContentType.SERIES,
                        id = seriesId.toString(),
                        name = seriesName,
                        posterUrl = item.cover ?: ""
                    )
                )
                results.add(newTvSeriesSearchResponse(seriesName, data) {
                    posterUrl = item.cover
                })
            }
        } catch (_: Exception) {}

        return results
    }

    override suspend fun load(url: String): LoadResponse? {
        val contentData = try {
            mapper.readValue(url, ContentData::class.java)
        } catch (e: Exception) {
            return null
        }

        val server = getServerUrl() ?: return null
        val user = getUsername() ?: return null
        val pass = getPassword() ?: return null

        return when (contentData.type) {
            ContentType.LIVE -> {
                val streamUrl = "$server/live/$user/$pass/${contentData.id}.ts"
                newLiveStreamLoadResponse(contentData.name, url, streamUrl)
            }

            ContentType.VOD -> {
                val ext = contentData.extension.ifEmpty { "mkv" }
                val streamUrl = "$server/movie/$user/$pass/${contentData.id}.$ext"
                newMovieLoadResponse(contentData.name, url, TvType.Movie, streamUrl) {
                    posterUrl = contentData.posterUrl.ifEmpty { null }
                }
            }

            ContentType.SERIES -> {
                val baseUrl = getBaseUrl() ?: return null
                val seriesInfoUrl = "$baseUrl&action=get_series_info&series_id=${contentData.id}"
                val response = app.get(seriesInfoUrl)
                val seriesInfo = try {
                    mapper.readValue(response.text, SeriesInfo::class.java)
                } catch (e: Exception) {
                    return null
                }

                val episodes = mutableListOf<Episode>()
                seriesInfo.episodes?.forEach { (seasonNum, episodeList) ->
                    episodeList.forEach { epInfo ->
                        val epId = epInfo.id ?: return@forEach
                        val epName = epInfo.name ?: "Episode ${epInfo.ep_num}"
                        val epExt = epInfo.container_extension ?: "mkv"
                        val seasonInt = seasonNum.toIntOrNull()
                        val epNumInt = epInfo.ep_num?.toIntOrNull()

                        // Build the stream URL for this episode
                        val streamUrl = "$server/series/$user/$pass/${epId}.$epExt"

                        episodes.add(newEpisode(streamUrl) {
                            name = epName
                            season = seasonInt
                            episode = epNumInt
                            posterUrl = epInfo.info?.movie_image
                        })
                    }
                }

                newTvSeriesLoadResponse(
                    seriesInfo.info?.name ?: contentData.name,
                    url,
                    TvType.TvSeries,
                    episodes
                ) {
                    posterUrl = seriesInfo.info?.cover ?: contentData.posterUrl.ifEmpty { null }
                    plot = seriesInfo.info?.plot
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
        val server = getServerUrl() ?: return false

        // For live streams and VOD, the data is the direct stream URL
        if (data.startsWith("http")) {
            val type = when {
                data.contains(".m3u8") -> ExtractorLinkType.M3U8
                data.contains(".mpd") -> ExtractorLinkType.DASH
                else -> null // auto-detect
            }
            callback(
                newExtractorLink(source = name, name = name, url = data, type = type) {
                    referer = server
                    quality = Qualities.Unknown.value
                }
            )
            return true
        }

        return false
    }

    // ---- Helper functions for fetching data from the Xtream Codes API ----

    private suspend fun fetchLiveStreams(baseUrl: String): List<LiveStream>? {
        return try {
            val response = app.get("$baseUrl&action=get_live_streams")
            mapper.readValue(response.text, Array<LiveStream>::class.java).toList()
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchVodStreams(baseUrl: String): List<VodStream>? {
        return try {
            val response = app.get("$baseUrl&action=get_vod_streams")
            mapper.readValue(response.text, Array<VodStream>::class.java).toList()
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchSeriesList(baseUrl: String): List<SeriesItem>? {
        return try {
            val response = app.get("$baseUrl&action=get_series")
            mapper.readValue(response.text, Array<SeriesItem>::class.java).toList()
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchCategories(baseUrl: String, action: String): List<Category>? {
        return try {
            val response = app.get("$baseUrl&action=$action")
            mapper.readValue(response.text, Array<Category>::class.java).toList()
        } catch (e: Exception) {
            null
        }
    }
}
