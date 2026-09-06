import re

with open('tvApp/src/main/java/com/tvmime/tv/di/MockDomainModule.kt', 'r') as f:
    content = f.read()

# Add AppDatabase to provides
content = content.replace(
    "@Provides @Singleton fun provideCategoryRepository(): com.streamvault.domain.repository.CategoryRepository = createMock()",
    """@Provides @Singleton fun provideCategoryRepository(database: com.tvmime.db.AppDatabase): com.streamvault.domain.repository.CategoryRepository {
        return createMock { method, args ->
            when (method.name) {
                "getCategories" -> {
                    val providerId = args?.get(0) as? Long ?: 1L
                    kotlinx.coroutines.flow.map(database.categoryDao().getCategories(providerId.toString(), "LIVE")) { entities ->
                        entities.map { entity ->
                            com.streamvault.domain.model.Category(
                                id = entity.categoryId.hashCode().toLong(),
                                roomId = entity.categoryId.hashCode().toLong(),
                                name = entity.categoryName,
                                type = com.streamvault.domain.model.ContentType.LIVE,
                                count = 10,
                                providerOrder = entity.sortOrder
                            )
                        }
                    }
                }
                else -> null
            }
        }
    }"""
)

content = content.replace(
    "@Provides @Singleton fun provideChannelRepository(): com.streamvault.domain.repository.ChannelRepository = createMock()",
    """@Provides @Singleton fun provideChannelRepository(database: com.tvmime.db.AppDatabase): com.streamvault.domain.repository.ChannelRepository {
        return createMock { method, args ->
            when (method.name) {
                "getChannelsByCategory" -> {
                    // StreamVault calls getChannelsByCategory(providerId, categoryId)
                    // Unfortunately categoryId in StreamVault is a Long, but in TVMime it's a String.
                    // But wait, the categories we mapped above used `categoryId.hashCode().toLong()`.
                    // We can't reverse a hash easily. We should probably just return ALL channels for the provider, 
                    // or better yet, since it's a proxy hack, we just fetch ALL channels and filter by hash in memory!
                    val providerId = args?.get(0) as? Long ?: 1L
                    val catHash = args?.get(1) as? Long ?: 0L
                    kotlinx.coroutines.flow.map(database.channelDao().getAllChannelsByType(providerId.toString(), "LIVE")) { entities ->
                        entities.filter { it.categoryId.hashCode().toLong() == catHash }.map { entity ->
                            com.streamvault.domain.model.Channel(
                                id = entity.id.hashCode().toLong(),
                                name = entity.name,
                                streamUrl = entity.directSourceUrl,
                                categoryId = catHash,
                                categoryName = "Live TV",
                                providerId = providerId,
                                number = entity.num
                            )
                        }
                    }
                }
                "getChannels" -> {
                    val providerId = args?.get(0) as? Long ?: 1L
                    kotlinx.coroutines.flow.map(database.channelDao().getAllChannelsByType(providerId.toString(), "LIVE")) { entities ->
                        entities.map { entity ->
                            com.streamvault.domain.model.Channel(
                                id = entity.id.hashCode().toLong(),
                                name = entity.name,
                                streamUrl = entity.directSourceUrl,
                                providerId = providerId,
                                number = entity.num
                            )
                        }
                    }
                }
                else -> null
            }
        }
    }"""
)

# Fix imports
content = content.replace("import kotlinx.coroutines.flow.flowOf", "import kotlinx.coroutines.flow.flowOf\nimport kotlinx.coroutines.flow.map")

with open('tvApp/src/main/java/com/tvmime/tv/di/MockDomainModule.kt', 'w') as f:
    f.write(content)
