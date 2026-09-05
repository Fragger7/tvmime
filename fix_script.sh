sed -i '' -e '125,150c\
            serviceScope.launch(Dispatchers.IO) {\
                runCatching {\
                    activeSource = null\
                    val db = AppDatabase.getInstance(this@TvMimePlaybackService)\
                    val channelId = request.mediaId\
                    val channel = db.channelDao().getChannelById(channelId) ?: throw IOException("Channel not found in database")\
                    val portal = db.portalDao().getPortalById(channel.portalId) ?: throw IOException("Portal not found")\
                    \
                    val source = PlaybackSource(\
                        sourceId = channel.portalId,\
                        channelId = channel.id,\
                        channelName = channel.name,\
                        streamUrl = "${portal.url}/live/${portal.username}/${portal.password}/${channel.streamId}.${channel.containerExtension}",\
                        headers = mapOf("User-Agent" to "IPTVSmartersPro/1.1.1"),\
                        connectionLimit = 1\
                    )\
                    \
                    activeSource = source\
                    val newItem = request.buildUpon()\
                        .setUri(Uri.parse("tvmime://channel/${channel.id}"))\
                        .build()\
                    listOf(newItem)\
                }.onSuccess {\
                    result.set(it)\
                }.onFailure {\
                    result.setException(it)\
                }\
            }\
' tvApp/src/main/java/com/tvmime/tv/playback/TvMimePlaybackService.kt
