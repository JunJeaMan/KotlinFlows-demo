package org.scarlet.flows.migration.viewmodeltoview.case3

import androidx.lifecycle.*
import org.scarlet.flows.migration.viewmodeltoview.AuthManager
import org.scarlet.flows.migration.viewmodeltoview.Repository
import org.scarlet.flows.model.Recipe
import org.scarlet.util.Resource
import org.scarlet.flows.model.User

/**
 * ###3: One-shot data load with parameters
 */
class ViewModelLive(
    private val repository: Repository,
    private val authManager: AuthManager
) : ViewModel() {

    private val user: LiveData<User> = authManager.observeUser().asLiveData()

    val favorites: LiveData<Resource<List<Recipe>>> =
        user.switchMap { user ->
            liveData {
                emit(Resource.Loading)
                emit(repository.getFavoriteRecipes(user.id))
            }
        }

    // Alternative
//    private val user: Flow<User> = authManager.observeUser()
//
//    val favorites: LiveData<Resource<List<Recipe>>> = TODO()

}