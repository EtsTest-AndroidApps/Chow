package io.julius.chow.main.profile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.julius.chow.domain.Result
import io.julius.chow.domain.interactor.Interactor
import io.julius.chow.domain.interactor.auth.SignOutInteractor
import io.julius.chow.domain.interactor.profile.GetUserInteractor
import io.julius.chow.domain.model.UserModel
import io.julius.chow.mapper.UserMapper
import io.julius.chow.model.User
import io.julius.chow.util.Event
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class ProfileViewModel @Inject constructor(
    getUserInteractor: GetUserInteractor,
    private val signOutInteractor: SignOutInteractor
) :
    ViewModel() {

    // LiveData object for view state interaction
    val profileViewContract: MutableLiveData<Event<ProfileViewContract>> = MutableLiveData()

    // public LiveData variable to expose the user
    val user = MutableLiveData<User>()

    private val disposable = CompositeDisposable()

    init {
        // Display progress bar
        profileViewContract.postValue(Event(ProfileViewContract.ProgressDisplay(true)))

        getUserInteractor.execute(true) {
            disposable.add((it as Flowable<*>).subscribe({ result ->
                // Hide progress bar
                profileViewContract.postValue(Event(ProfileViewContract.ProgressDisplay(false)))

                when (result) {
                    is Result.Success<*> -> {
                        // Map to layer specific model and pass to view.
                        user.postValue(UserMapper.mapFromModel(result.data as UserModel))
                    }

                    is Result.Failure -> {
                        // Display error message to user
                        profileViewContract.postValue(
                            Event(ProfileViewContract.MessageDisplay(result.exception.toString()))
                        )
                    }
                }
            }, { throwable ->
                // Hide progress bar
                profileViewContract.postValue(Event(ProfileViewContract.ProgressDisplay(false)))

                // Display error message to user
                profileViewContract.postValue(
                    Event(ProfileViewContract.MessageDisplay(throwable.localizedMessage.toString()))
                )
            }))
        }
    }

    fun signOut() {
        signOutInteractor.execute(Interactor.None()) {
            profileViewContract.postValue(
                Event(ProfileViewContract.NavigateToAuth)
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        disposable.dispose()
    }
}