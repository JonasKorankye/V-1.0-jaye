package com.flipverse.data.domain

import com.flipverse.shared.RequestState
import com.flipverse.shared.domain.FlipNomenclatures
import kotlinx.coroutines.flow.Flow


/**
 * Read Nomenclature i.e. Static Data from DataSource
 *
 */
interface NomenclatureRepository {
    fun getCurrentUserId():String?
    fun readSuggestedFlipAccountsFlow(): Flow<RequestState<List<FlipNomenclatures.SuggestedFlipAccounts>>>
    fun readInterestsFlow(): Flow<RequestState<List<FlipNomenclatures.FlipInterests>>>
    fun readGenresFlow(): Flow<RequestState<List<FlipNomenclatures.FlipGenres>>>

}