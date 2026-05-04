package com.flipverse.data

import com.flipverse.data.domain.NomenclatureRepository
import com.flipverse.shared.RequestState
import com.flipverse.shared.domain.FlipNomenclatures
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest

class NomenclatureRepositoryImpl() : NomenclatureRepository {
    override fun getCurrentUserId(): String? {
        return Firebase.auth.currentUser?.uid
    }

    override fun readSuggestedFlipAccountsFlow(): Flow<RequestState<List<FlipNomenclatures.SuggestedFlipAccounts>>> =
        channelFlow {
            try {

                val database = Firebase.firestore
                database.collection(collectionPath = "suggested_flip_accounts")
                    .snapshots
                    .collectLatest { query ->
                        val accounts = query.documents.map { document ->
                            FlipNomenclatures.SuggestedFlipAccounts(
                                id = document.id,
                                name = document.get(field = "name"),
                                author = document.get(field = "author"),
                                isSelected = document.get(field = "isSelected"),
                                logoResId = document.get(field = "logoResId"),
                                category = document.get(field = "category"),
                            )
                        }
                        send(RequestState.Success(data = accounts.map { it.copy(author = it.author.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) }))
                    }

            } catch (e: Exception) {
                send(RequestState.Error("Error while reading the default Flip Accounts from the database: ${e.message}"))
            }
        }

    override fun readInterestsFlow(): Flow<RequestState<List<FlipNomenclatures.FlipInterests>>> =
        channelFlow {
            try {

                val database = Firebase.firestore
                database.collection(collectionPath = "interests")
                    .snapshots
                    .collectLatest { query ->
                        val interests = query.documents.map { document ->
                            FlipNomenclatures.FlipInterests(
                                name = document.get(field = "interests")
                            )
                        }
                        send(RequestState.Success(data = interests))
                    }

            } catch (e: Exception) {
                send(RequestState.Error("Error while reading the Flip Interests from the database: ${e.message}"))
            }
        }

    override fun readGenresFlow(): Flow<RequestState<List<FlipNomenclatures.FlipGenres>>> =
        channelFlow {
            try {

                val database = Firebase.firestore
                database.collection(collectionPath = "genres")
                    .snapshots
                    .collectLatest { query ->
                        val genres = query.documents.map { document ->
                            FlipNomenclatures.FlipGenres(
                                genres = document.get(field = "genres")
                            )
                        }
                        send(RequestState.Success(data = genres))
                    }

            } catch (e: Exception) {
                send(RequestState.Error("Error while reading the Flip Interests from the database: ${e.message}"))
            }
        }


}
