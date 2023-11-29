package org.scarlet.android.movies.data.repository

import android.util.Log
import org.scarlet.util.Resource
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.*
import org.jetbrains.annotations.VisibleForTesting
import org.scarlet.android.movies.TAG
import org.scarlet.android.movies.data.LocalDataSource
import org.scarlet.android.movies.data.MovieRepository
import org.scarlet.android.movies.data.RemoteDataSource
import org.scarlet.android.movies.model.Movie

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
class MovieDefaultRepository private constructor(
    private val remoteDataSource: RemoteDataSource,
    private val localDataSource: LocalDataSource,
) : MovieRepository {

    // TODO replace deprecated API
    private val queryChannel = ConflatedBroadcastChannel<String>()

    override fun searchMovies(query: String) {
        queryChannel.trySend(query)
    }

    private suspend fun loadFromCache(query: String): List<Movie> =
        when {
            query.isEmpty() -> localDataSource.getAllMovies()
            else -> localDataSource.searchMovies(query)
        }

    private suspend fun loadFromApi(query: String): Resource<List<Movie>> =
        when {
            query.isEmpty() -> remoteDataSource.getPopularMovies()
            else -> remoteDataSource.searchMovies(query)
        }

    override fun getMovies(): Flow<Resource<List<Movie>>> = flow {
        queryChannel.openSubscription().consumeEach { query ->
            // Load from cache first
            emit(Resource.Success(loadFromCache(query)))

            // Load from API
            val newMovies = loadFromApi(query)
            emit(newMovies)
            saveToDB(newMovies)
        }
    }.catch { ex ->
        emit(Resource.Error(ex.message))
    }

    private suspend fun saveToDB(resource: Resource<List<Movie>>) {
        when (resource) {
            is Resource.Success -> localDataSource.insertAll(resource.data!!)
            else -> Log.d(TAG, "[MovieDefaultRepository] saveToDB(Fail)")
        }
    }

    /* Another Version: Always read from cache */

    private fun loadFlowFromCache(query: String): Flow<List<Movie>> =
        when {
            query.isEmpty() -> localDataSource.getAllMoviesFlow()
            else -> localDataSource.searchMoviesFlow(query)
        }

    override fun getMoviesV2(): Flow<Resource<List<Movie>>> = flow {
        // Do concurrently
        // - Fetch movies from local DB and emit it:
        //     if query is empty, call localDataSource.getAllMoviesFlow()
        //     else call localDataSource.searchMoviesFlow(query)
        // - Fetch movies from remote API and save then to local DB:
        //    if query is empty, call remoteDataSource.getPopularMovies()
        //    else call remoteDataSource.searchMovies(query)

        TODO()
    }

    @VisibleForTesting
    override fun reset() {
        INSTANCE = null
    }

    companion object {
        @Volatile
        private var INSTANCE: MovieRepository? = null

        fun getInstance(remoteDataSource: RemoteDataSource, localDataSource: LocalDataSource) =
            INSTANCE ?: synchronized(this) {
                MovieDefaultRepository(remoteDataSource, localDataSource).also {
                    INSTANCE = it
                }
            }
    }
}
