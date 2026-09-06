package com.tvmime.tv.di

import com.streamvault.domain.model.Category
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.ContentType
import com.streamvault.domain.model.LegacyProvider
import com.streamvault.domain.model.ProviderType
import com.streamvault.domain.repository.ChannelRepository
import com.streamvault.domain.repository.ProviderRepository
import com.tvmime.db.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RealProviderRepository @Inject constructor(
    private val database: AppDatabase
) : ProviderRepository by createMock() {
    override fun getProviders(): Flow<List<LegacyProvider>> {
        return database.portalDao().getActivePortals().map { portals ->
            portals.map { LegacyProvider(id = it.id.hashCode().toLong(), name = it.name, type = ProviderType.M3U, serverUrl = it.serverUrl) }
        }
    }
    
    override fun getActiveProvider(): Flow<LegacyProvider?> {
        return database.portalDao().getActivePortal().map { portal ->
            portal?.let { LegacyProvider(id = it.id.hashCode().toLong(), name = it.name, type = ProviderType.M3U, serverUrl = it.serverUrl) }
        }
    }
}
