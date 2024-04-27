package com.isrbet.budgetsbyisrbet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.totalBytesToDownload
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.isrbet.budgetsbyisrbet.databinding.FragmentSignInBinding
import timber.log.Timber

const val DAYS_FOR_FLEXIBLE_UPDATE = 7
const val cMyRequestCode = 9876

class SignInFragment : Fragment() {
    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var updateListener: InstallStateUpdatedListener

    private var bytesToDownload: Long = 0
    private var bytesDownloaded: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)

        val mainActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data = result.data
                    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                    try {
                        // Google Sign In was successful, authenticate with Firebase
                        val account = task.getResult(ApiException::class.java)!!
                        MyApplication.userGivenName = account.givenName.toString()
                        MyApplication.userFamilyName = account.familyName.toString()
                        MyApplication.userAccount = account.account
                        firebaseAuthWithGoogle(account.idToken!!)
                    } catch (e: ApiException) {
                        // Google Sign In failed, update UI appropriately
                        Timber.tag("Alex").d("Google sign in failed %s", e.toString())
                    }
                } else
                    Timber.tag("Alex").d(
                        "in registerForActivityResult, result was not OK %s", result.resultCode
                    )
            }

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestScopes(Scope(SheetsScopes.SPREADSHEETS))
            .requestEmail()
            .build()

        // Build a GoogleSignInClient with the options specified by gso.
        (activity as MainActivity).setGoogleSignInClient(GoogleSignIn.getClient(requireContext(), gso))
        binding.signInButton.setOnClickListener {
            onSignIn(mainActivityResultLauncher)
        }
        auth = Firebase.auth

        inflater.inflate(R.layout.fragment_sign_in, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account == null) {  // user is logged out
            (activity as MainActivity).setLoggedOutMode(true)
            binding.signInButton.visibility = View.VISIBLE
            binding.signInButton.setSize(SignInButton.SIZE_WIDE)
        } else { // user is logged in
            binding.signInButton.visibility = View.GONE
            MyApplication.userGivenName = account.givenName.toString()
            MyApplication.userFamilyName = account.familyName.toString()
            MyApplication.userAccount = account.account
            MyApplication.userPhotoURL = account.photoUrl.toString()
        }
        setAdminMode(account?.email == "alexreid2070@gmail.com")
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        signIn(currentUser)
    }

    private fun setAdminMode(inAdminMode: Boolean) {
        MyApplication.adminMode = inAdminMode
    }

    private fun onSignIn(mainActivityResultLauncher: ActivityResultLauncher<Intent>) {
        val signInIntent: Intent = (activity as MainActivity).getGoogleSignInClient().signInIntent
        mainActivityResultLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    val user = auth.currentUser
                    // this code is only hit when a user signs in successfully.
                    signIn(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Timber.tag("Alex").d("signInWithCredential:failure + task.exception")
                    signIn(null)
                }
            }
    }

    private fun signIn(account: FirebaseUser?) {
        MyApplication.userEmail = account?.email.toString()
        if (account == null) {
            binding.signInButton.visibility = View.VISIBLE
            binding.signInButton.setSize(SignInButton.SIZE_WIDE)
        } else {
            if (MyApplication.userUID == "") {  // ie don't want to override this if Admin is impersonating another user...
                MyApplication.userUID = account.uid
                MyApplication.originalUserUID = account.uid
            }
            if (MyApplication.currentUserEmail == "")  // ie don't want to override this if Admin is impersonating another user...
                MyApplication.currentUserEmail = account.email ?: ""
            MyApplication.userPhotoURL = account.photoUrl.toString()

            binding.signInButton.visibility = View.GONE
            if (account.email == "alexreid2070@gmail.com")
                setAdminMode(true)
            checkForAppUpdate()
//            findNavController().navigate(R.id.homeFragment)
        }
    }

    private fun checkForAppUpdate() {
        appUpdateManager = AppUpdateManagerFactory.create(requireContext())

        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ||
                    (appUpdateInfo.clientVersionStalenessDays() ?: -1) >= DAYS_FOR_FLEXIBLE_UPDATE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                ) {
                    binding.signInButton.visibility = View.GONE
                    binding.    upgradeLayout.visibility = View.VISIBLE

                    updateListener = InstallStateUpdatedListener { state ->
                        // (Optional) Provide a download progress bar.
                        if (state.installStatus() == InstallStatus.DOWNLOADING) {
                            bytesToDownload = state.totalBytesToDownload
                            bytesDownloaded = state.bytesDownloaded()
                            updateProgressBar()
                        } else if (state.installStatus() == InstallStatus.DOWNLOADED) {
                            // Show a notification and request user confirmation to restart the app.
                            appUpdateManager.unregisterListener(updateListener)
                            enableRestartButton()
                        }
                    }
                    appUpdateManager.registerListener(updateListener)

                    // Request the update.
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
                        AppUpdateType.FLEXIBLE,
                        ::startIntentSenderForResult,
                        cMyRequestCode
                    )
                }
            } else {
                Timber.tag("Alex").d("In else, no update available")
                findNavController().navigate(R.id.homeFragment)
            }
        }
    }

    private fun updateProgressBar() {
        binding.circularProgressBar.setProgressCompat(bytesDownloaded.toInt(), true)
        binding.circularProgressBar.max = bytesToDownload.toInt()
        val pct = ((bytesDownloaded * 1.0 / bytesToDownload) * 100.0).toInt()
        val mb = bytesToDownload / 1000000.0
        binding.downloadText.text = String.format("%d%% of %.2f MB", pct, mb)
    }
    private fun enableRestartButton() {
        binding.downloadText.text = getString(R.string.download_complete)
        binding.restartButton.setOnClickListener {
            appUpdateManager.completeUpdate()
        }
        binding.restartButton.isEnabled = true
    }

    override fun onResume() {
        super.onResume()
        if (this::appUpdateManager.isInitialized) {
            appUpdateManager
                .appUpdateInfo
                .addOnSuccessListener { appUpdateInfo ->
                    if (appUpdateInfo.updateAvailability()
                        == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                    ) {
                        // If an in-app update is already running, resume the update.
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            ::startIntentSenderForResult,
                            cMyRequestCode
                        )
                    }
                    // If the update is downloaded but not installed, notify the user to complete the update.
                    else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        completeUpdate()
                    }
                }
        }
    }

    private fun completeUpdate() {
        bytesDownloaded = bytesToDownload
        binding.upgradeLayout.visibility = View.VISIBLE
        enableRestartButton()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::appUpdateManager.isInitialized && this::updateListener.isInitialized) {
            appUpdateManager.unregisterListener(updateListener)
        }
        _binding = null
    }
}