package org.scarlet.flows.migration.viewmodeltoview.case3

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.google.common.truth.Truth.assertThat
import org.scarlet.flows.CoroutineTestRule
import org.scarlet.flows.migration.viewmodeltoview.AuthManager
import org.scarlet.flows.migration.viewmodeltoview.Repository
import org.scarlet.flows.model.Recipe
import org.scarlet.flows.model.User
import org.scarlet.util.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.scarlet.flows.model.Recipe.Companion.mFavorites

@ExperimentalCoroutinesApi
class ViewModelLiveTest {
    // SUT
    lateinit var viewModel: ViewModelLive

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @MockK
    lateinit var repository: Repository

    @MockK
    lateinit var authManager: AuthManager

    @MockK(relaxed = true)
    lateinit var mockObserver: Observer<Resource<List<Recipe>>>

    @Before
    fun init() {
        MockKAnnotations.init(this)

        every { authManager.observeUser() } returns flowOf(User("A001", "Peter Parker", 33))

        coEvery {
            repository.getFavoriteRecipes(any())
        } coAnswers {
            delay(1_000)
            Resource.Success(mFavorites)
        }

        viewModel = ViewModelLive(repository, authManager)
    }

    @Test
    fun `testLiveData - with mock observer`() = runTest {
        // Arrange (Given)
        val liveData = viewModel.favorites
        liveData.observeForever(mockObserver)

        // Act (When)
        advanceUntilIdle()

        // Act (Then)
        verifySequence {
            mockObserver.onChanged(Resource.Loading)
            mockObserver.onChanged(Resource.Success(mFavorites))
        }

        liveData.removeObserver(mockObserver)
    }

    @Test
    fun `testLiveData - with captureValues`() = runTest {
        // Arrange (Given)
        // Act (Then)
        viewModel.favorites.captureValues {
            advanceUntilIdle()

            // Assert (Then)
            assertThat(this.values).containsExactly(
                Resource.Loading,
                Resource.Success(mFavorites)
            )
        }
    }

}