import re

with open('tvApp/src/main/java/com/tvmime/tv/di/MockDomainModule.kt', 'r') as f:
    content = f.read()

epg_proxy = """@Provides @Singleton fun provideEpgRepository(database: com.tvmime.db.AppDatabase): com.streamvault.domain.repository.EpgRepository {
        return createMock { method, args ->
            when (method.name) {
                "getProgramsForChannel" -> {
                    val providerId = args?.get(0) as? Long ?: 1L
                    val channelId = args?.get(1) as? String ?: ""
                    val startTime = args?.get(2) as? Long ?: 0L
                    kotlinx.coroutines.flow.map(database.epgDao().getProgramsForChannel(providerId.toString(), channelId, startTime)) { entities ->
                        entities.map { entity ->
                            com.streamvault.domain.model.Program(
                                id = entity.id.hashCode().toLong(),
                                channelId = entity.epgChannelId,
                                title = entity.title,
                                description = entity.description ?: "",
                                startTime = entity.startEpoch,
                                endTime = entity.endEpoch,
                                providerId = providerId
                            )
                        }
                    }
                }
                "getNowPlaying" -> {
                    val providerId = args?.get(0) as? Long ?: 1L
                    val channelId = args?.get(1) as? String ?: ""
                    kotlinx.coroutines.flow.map(database.epgDao().getProgramsForChannel(providerId.toString(), channelId, System.currentTimeMillis(), 1)) { entities ->
                        entities.firstOrNull()?.let { entity ->
                            com.streamvault.domain.model.Program(
                                id = entity.id.hashCode().toLong(),
                                channelId = entity.epgChannelId,
                                title = entity.title,
                                description = entity.description ?: "",
                                startTime = entity.startEpoch,
                                endTime = entity.endEpoch,
                                providerId = providerId,
                                isNowPlaying = true
                            )
                        }
                    }
                }
                else -> null
            }
        }
    }"""

content = re.sub(r'@Provides @Singleton fun provideEpgRepository\(\).*?createMock\(\)', epg_proxy, content, flags=re.DOTALL)

with open('tvApp/src/main/java/com/tvmime/tv/di/MockDomainModule.kt', 'w') as f:
    f.write(content)
