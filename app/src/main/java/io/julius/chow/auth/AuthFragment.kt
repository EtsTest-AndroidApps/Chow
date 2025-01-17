package io.julius.chow.auth


import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.firebase.ui.auth.AuthUI
import com.google.android.material.snackbar.Snackbar
import io.julius.chow.R
import io.julius.chow.base.extension.observe
import io.julius.chow.domain.model.UserType
import io.julius.chow.util.Event
import kotlinx.android.synthetic.main.fragment_auth.*

/**
 * A simple [Fragment] subclass.
 *
 */
class AuthFragment : Fragment(), View.OnClickListener {

    private lateinit var authViewModel: AuthViewModel
    private var userCategory: MutableLiveData<UserType> = MutableLiveData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authViewModel = ViewModelProviders.of(activity!!).get(AuthViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_auth, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        avatar_restaurant.setOnClickListener(this)
        avatar_customer.setOnClickListener(this)

        button_phone_login.setOnClickListener {
            when (userCategory.value) {
                UserType.CUSTOMER, UserType.RESTAURANT -> initiatePhoneAuth()
                else -> Snackbar.make(view, "Select an account type.", Snackbar.LENGTH_LONG).apply {
                    this.view.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent))
                    show()
                }
            }
        }

        observe(userCategory) {
            when (userCategory.value) {
                UserType.CUSTOMER, UserType.RESTAURANT -> button_phone_login.isEnabled = true
                else -> button_phone_login.isEnabled = false
            }
        }

        observe(authViewModel.authContractData) {
            it.getContentIfNotHandled()?.let { data ->
                when (data) {
                    is AuthViewContract.ProgressDisplay -> {
                        if (data.display) progress_bar.visibility = View.VISIBLE else progress_bar.visibility =
                            View.INVISIBLE

                        // Enable/Disable data views
                        button_phone_login.isEnabled = !data.display
                        avatar_customer.isEnabled = !data.display
                        avatar_restaurant.isEnabled = !data.display
                    }

                    is AuthViewContract.MessageDisplay -> {
                        Toast.makeText(context, data.message, Toast.LENGTH_SHORT).show()
                    }

                    is AuthViewContract.NavigateToSignUp -> {
                        if (userCategory.value == UserType.CUSTOMER) {
                            Navigation.findNavController(activity!!, R.id.navigation_host_fragment)
                                .navigate(R.id.action_signUpDetailsFragment)
                        } else {
                            Navigation.findNavController(activity!!, R.id.navigation_host_fragment)
                                .navigate(R.id.action_restaurantAdditionalDetailsFragment)
                        }
                    }

                    is AuthViewContract.NavigateToHome -> {
                        // Navigate to the MainActivity and finish this current activity
                        if (userCategory.value == UserType.CUSTOMER) {
                            Navigation.findNavController(activity!!, R.id.navigation_host_fragment)
                                .navigate(R.id.action_authFragment_to_mainActivity)
                        } else {
                            Navigation.findNavController(activity!!, R.id.navigation_host_fragment)
                                .navigate(R.id.action_authFragment_to_restaurantMainActivity)
                        }
                        activity?.finish()
                    }
                }
            }
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.avatar_customer -> {
                avatar_customer.alpha = 1f
                avatar_restaurant.alpha = 0.5f
                userCategory.value = UserType.CUSTOMER
            }

            R.id.avatar_restaurant -> {
                avatar_restaurant.alpha = 1f
                avatar_customer.alpha = 0.5f
                userCategory.value = UserType.RESTAURANT
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Hide progress bar
        authViewModel.authContractData.value = Event(AuthViewContract.ProgressDisplay(false))

        if (resultCode != RESULT_OK) {
            Toast.makeText(context, "Authentication failed. Try again.", Toast.LENGTH_SHORT).show()
            return
        }

        when (requestCode) {
            RC_PHONE_SIGN_IN -> {
                when (userCategory.value) {
                    UserType.CUSTOMER -> authViewModel.authCurrentUser()
                    UserType.RESTAURANT -> authViewModel.authCurrentRestaurant()
                }
            }
        }
    }

    private fun initiatePhoneAuth() {
        // Display progress bar
        authViewModel.authContractData.value = Event(AuthViewContract.ProgressDisplay(true))

        // Choose authentication providers
        val providers = listOf(AuthUI.IdpConfig.Builder(AuthUI.PHONE_VERIFICATION_PROVIDER).build())

        // Create and launch sign-in intent
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setLogo(R.mipmap.ic_launcher)
                .build(),
            RC_PHONE_SIGN_IN
        )
    }

    companion object {
        private const val RC_PHONE_SIGN_IN = 123
    }
}
