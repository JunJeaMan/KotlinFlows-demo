package org.scarlet.flows.migration.viewmodeltoview.case4

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import org.scarlet.flows.CoroutineTestRule
import org.scarlet.flows.migration.viewmodeltoview.AuthManager
import org.scarlet.flows.migration.viewmodeltoview.Repository
import org.scarlet.flows.model.User
import org.scarlet.util.Resource
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.scarlet.flows.model.Recipe.Companion.mFavorites

@ExperimentalCoroutinesApi
class ViewModelFlowTest {
    // SUT
    lateinit var viewModel: ViewModelFlow

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @MockK
    lateinit var repository: Repository

    @MockK
    lateinit var authManager: AuthManager

    @Before
    fun init() {
        MockKAnnotations.init(this)

        every { authManager.observeUser() } returns flowOf(User("A001", "Peter Parker", 33))

        every {
            repository.getFavoriteRecipesFlow(any())
        } returns flowOf(Resource.Success(mFavorites))

        viewModel = ViewModelFlow(repository, authManager)
    }

    @Test
    fun `test flow without turbine`() = runTest {
        // Arrange (Given)
        // Act (When)
        val resource = viewModel.favorites.take(2).toList()

        // Assert (Then)
        assertThat(resource).containsExactly(
            Resource.Loading, Resource.Success(mFavorites)
        )
    }

    @Test
    fun `test flow wih turbine`() = runTest {
        // Arrange (Given)
        // Act (When)
        viewModel.favorites.test {
            // Assert (Then)
            assertThat(awaitItem()).isEqualTo(Resource.Loading)
            assertThat(awaitItem()).isEqualTo(Resource.Success(mFavorites))
        }
    }
}