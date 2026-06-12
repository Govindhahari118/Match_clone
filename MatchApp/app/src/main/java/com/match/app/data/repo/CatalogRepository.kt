package com.match.app.data.repo

import com.match.app.data.remote.CommunityGroupDto
import com.match.app.data.remote.MatchApiProvider
import com.match.app.data.remote.RegionPresetDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backend-driven catalogue (regions + community groups).  Calls fail soft and
 * return [Result] so screens can fall back to bundled presets when the backend
 * is unreachable.
 */
@Singleton
class CatalogRepository @Inject constructor(
    private val apiProvider: MatchApiProvider
) {
    /** Returns the structured region presets from /api/meta/regions */
    suspend fun fetchPresets(): Result<List<RegionPresetDto>> = runCatching {
        apiProvider.api().regions().data?.presets ?: emptyList()
    }

    suspend fun fetchCommunityGroups(): Result<List<CommunityGroupDto>> = runCatching {
        apiProvider.api().communityGroups().groups
    }

    suspend fun ping(): Result<String> = runCatching {
        apiProvider.api().health().status ?: "ok"
    }
}
