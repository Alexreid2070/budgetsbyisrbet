package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.isrbet.budgetsbyisrbet.databinding.FragmentHomeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.CoroutineContext


class HomeFragment : Fragment(), CoroutineScope {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private val defaultsModel: DefaultsViewModel by viewModels()
    private val transactionModel: TransactionViewModel by viewModels()
    private val categoryModel: CategoryViewModel by viewModels()
    private val spenderModel: SpenderViewModel by viewModels()
    private val budgetModel: BudgetViewModel by viewModels()
    private val scheduledPaymentModel: ScheduledPaymentViewModel by viewModels()
    private val retirementUserModel: RetirementViewModel by viewModels()
    private val userModel: AppUserViewModel by viewModels()
    private val hintModel: HintViewModel by viewModels()
    private val translationModel: TranslationViewModel by viewModels()
    private var gestureDetector: GestureDetectorCompat? = null
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        userModel.clearCallback()
        // Inflate the layout for this fragment - DON'T seem to need this inflate.  In fact, if I call it, it'll call Main's onCreateView multiple times
//        inflater.inflate(R.layout.fragment_home, container, false)

        if (inDarkMode(requireContext()))
            binding.constraintLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.black))

        gestureDetector = GestureDetectorCompat(requireActivity(), object :
            GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                event1: MotionEvent,
                event2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (event2.y > event1.y) {
                    // negative for up, positive for down
                    if (!binding.scrollView.canScrollVertically(-1)) { // ie can't scroll down anymore
                        if (binding.expansionAreaLayout.visibility == View.GONE)
                            onExpandClicked()
                        else // already expanded and user swiped down, so open Settings
                            findNavController().navigate(R.id.SettingsTabsFragment)
                    }
                } else if (event2.y < event1.y) {
                    if (!binding.scrollView.canScrollVertically(1)) { // ie can't scroll up anymore
                        if (binding.expansionAreaLayout.visibility == View.VISIBLE)
                            onExpandClicked()
                    }
                }
//                }
                return true
            }
        })

        val mainActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data = result.data
                    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                    try {
                        // Google Sign In was successful, authenticate with Firebase
                        val account = task.getResult(ApiException::class.java)!!
                        Timber.tag("Alex").d("firebaseAuthWithGoogle:%s", account.id)
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
                        "in registerforactivityresult, result was not OK %s", result.resultCode
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
        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
        binding.signInButton.setOnClickListener {
            onSignIn(mainActivityResultLauncher)
        }
        auth = Firebase.auth

        binding.scheduledPaymentField.setOnClickListener {
            val action =
                HomeFragmentDirections.actionHomeFragmentToSettingsTabsFragment()
            action.targetTab = 3
            findNavController().navigate(action)
//            findNavController().navigate(R.id.ScheduledPaymentFragment)
        }
        binding.transactionAddFab.setOnClickListener {
            findNavController().navigate(R.id.TransactionFragment)
        }
        binding.transactionAddFab.setOnLongClickListener {
            MyApplication.displayToast("Long click")
            true
        }
        binding.expandButton.setOnClickListener {
            onExpandClicked()
        }
        binding.settingsButton.setOnClickListener {
            findNavController().navigate(R.id.SettingsTabsFragment)
        }
        binding.loanButton.setOnClickListener {
            findNavController().navigate(R.id.LoanFragment)
        }
        binding.helpButton.setOnClickListener {
            findNavController().navigate(R.id.HelpFragment)
        }
        binding.adminButton.setOnClickListener {
            findNavController().navigate(R.id.AdminFragment)
        }

        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.scrollView.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
                if (p1 != null) {
                    try {
                        // this call is in a "try" because if the user swipes partially over the tracker on the home page, it crashes.  So we want to ignore that gesture
                        gestureDetector?.onTouchEvent(p1)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return false
                    }
                }
                return false
            }
        })
//        setupDataCallbacks()

        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account?.email == null) {
            // user is logged out
            (activity as MainActivity).setLoggedOutMode(true)
            binding.expandButton.isEnabled = false
            binding.signInButton.visibility = View.VISIBLE
            binding.quoteField.text = ""
            binding.transactionAddFab.visibility = View.GONE
            binding.expandButton.visibility = View.GONE
            binding.homeScreenMessage.visibility = View.VISIBLE
            binding.homeScreenMessage.text = getString(R.string.you_must_sign_in)
            binding.signInButton.setSize(SignInButton.SIZE_WIDE)
        } else {
            binding.signInButton.visibility = View.GONE
            binding.homeScreenMessage.text = ""
            binding.homeScreenMessage.visibility = View.GONE
        }
//        Log.d("Alex", "account.email is " + account?.email + " and name is " + account?.givenName + " and uid " + MyApplication.userUID)
        MyApplication.userGivenName = account?.givenName.toString()
        MyApplication.userFamilyName = account?.familyName.toString()
        MyApplication.userAccount = account?.account
        if (account != null) {
            MyApplication.userPhotoURL = account.photoUrl.toString()
            Glide.with(requireContext()).load(MyApplication.userPhotoURL)
                .thumbnail(0.5f)
                .crossFade()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.imgProfilePic)
        }
        setAdminMode(account?.email == "alexreid2070@gmail.com")
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        signIn(currentUser)
        binding.imgProfilePic.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.are_you_sure))
                .setMessage(String.format(getString(R.string.are_you_sure_that_you_want_to_sign_out),
                    MyApplication.userGivenName, MyApplication.userFamilyName))
                .setPositiveButton(getString(R.string.sign_out)) { _, _ -> signout() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }  // nothing should happen, other than dialog closes
                .show()
        }
        if (gHomePageExpansionAreaExpanded) {
            Timber.tag("Alex").d("in Home Fragment, gHomePageExpansionAreaExpanded is true")
            expandTop()
        }
        // this next block allows the floating action button to move up and down (it starts constrained to bottom)
        val set = ConstraintSet()
        val constraintLayout = binding.constraintLayout
        set.clone(constraintLayout)
        set.clear(R.id.budget_add_fab, ConstraintSet.TOP)
        set.applyTo(constraintLayout)
    }

    private fun setScheduledPaymentText() {
        val spText = ScheduledPaymentViewModel.getScheduledPaymentsInNextDays(DefaultsViewModel.getDefaultSPLookahead())
        if (spText == "") {
            binding.scheduledPaymentField.visibility = View.GONE
        } else {
            binding.scheduledPaymentField.visibility = View.VISIBLE
        }
        binding.scheduledPaymentField.text = spText
    }

    private fun setupDataCallbacks() {
        DefaultsViewModel.singleInstance.setCallback(object : DataUpdatedCallback {
            override fun onDataUpdate() {
                if (MyApplication.userEmail != MyApplication.currentUserEmail) {
                    binding.quoteField.visibility = View.VISIBLE
                    binding.quoteField.text = String.format(
                        getString(R.string.currently_impersonating),
                        MyApplication.currentUserEmail
                    )
                } else if (DefaultsViewModel.getDefaultQuote()) {
                    binding.quoteField.visibility = View.VISIBLE
                    binding.quoteField.text = getQuote()
                }
                alignPageWithDataState("DefaultViewModel")
            }
        })
        CategoryViewModel.singleInstance.setCallback(object : DataUpdatedCallback {
            override fun onDataUpdate() {
                alignPageWithDataState("CategoryViewModel")
            }
        })

        SpenderViewModel.singleInstance.setCallback(object : DataUpdatedCallback {
            override fun onDataUpdate() {
                (activity as MainActivity).multipleUserMode(SpenderViewModel.multipleUsers())
                alignPageWithDataState("SpenderViewModel")
            }
        })
        HintViewModel.singleInstance.setCallback(object : DataUpdatedCallback {
            override fun onDataUpdate() {
                alignPageWithDataState("HintViewModel")
            }
        })
        TransactionViewModel.singleInstance.setCallback(object : DataUpdatedCallback {
            override fun onDataUpdate() {
                alignPageWithDataState("TransactionViewModel")
            }
        })
        val budListObserver = Observer<MutableList<Budget>> {
            alignPageWithDataState("BudgetViewModel")
        }
        BudgetViewModel.observeList(this, budListObserver)
/*        BudgetViewModel.singleInstance.setCallback(object : DataUpdatedCallback {
            override fun onDataUpdate() {
                alignPageWithDataState("BudgetViewModel")
            }
        }) */
        val spListObserver = Observer<MutableList<ScheduledPayment>> {
            (activity as MainActivity).multipleUserMode(SpenderViewModel.multipleUsers())
            alignPageWithDataState("SpenderViewModel")
            setScheduledPaymentText()
        }
        ScheduledPaymentViewModel.observeList(this, spListObserver)
/*        ScheduledPaymentViewModel.singleInstance.setCallback(object : DataUpdatedCallback {
            override fun onDataUpdate() {
                setScheduledPaymentText()
                alignPageWithDataState("ScheduledPaymentViewModel")
            }
        }) */
        RetirementViewModel.singleInstance.setCallback(object : DataUpdatedCallback {
            override fun onDataUpdate() {
                alignPageWithDataState("RetirementViewModel")
            }
        })
    }

    private fun onExpandClicked() {
        if (binding.expansionAreaLayout.visibility == View.GONE) { // ie expand the section
            expandTop()
        } else { // ie retract the section
            retractTop()
        }
    }

    private fun expandTop() {
        binding.expandButton.setImageResource(R.drawable.ic_baseline_expand_less_24)
        binding.expansionAreaLayout.visibility = View.VISIBLE
        gHomePageExpansionAreaExpanded = true
    }

    private fun retractTop() {
        binding.expandButton.setImageResource(R.drawable.ic_baseline_expand_more_24)
        binding.expansionAreaLayout.visibility = View.GONE
        gHomePageExpansionAreaExpanded = false
    }

    private fun getQuote(): String {
        return if (MyApplication.userEmail != MyApplication.currentUserEmail)
            "Currently impersonating " + MyApplication.currentUserEmail
        else
            MyApplication.getQuote()
    }

    private fun onSignIn(mainActivityResultLauncher: ActivityResultLauncher<Intent>) {
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
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
        if (account != null) {
            if (MyApplication.userUID == "") {  // ie don't want to override this if Admin is impersonating another user...
                MyApplication.userUID = account.uid
                MyApplication.originalUserUID = account.uid
//                Log.d("Alex", "Just set userUID to " + account.uid)
            }
            if (MyApplication.currentUserEmail == "")  // ie don't want to override this if Admin is impersonating another user...
                MyApplication.currentUserEmail = account.email ?: ""
            MyApplication.userPhotoURL = account.photoUrl.toString()
            Glide.with(requireContext()).load(MyApplication.userPhotoURL)
                .thumbnail(0.5f)
                .crossFade()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.imgProfilePic)
        }
        if (account == null) {
            binding.transactionAddFab.visibility = View.GONE
            binding.expandButton.visibility = View.GONE
            binding.signInButton.visibility = View.VISIBLE
            binding.signInButton.setSize(SignInButton.SIZE_WIDE)
            binding.homeScreenMessage.visibility = View.VISIBLE
            binding.homeScreenMessage.text = getString(R.string.you_must_sign_in)
        } else {
            binding.transactionAddFab.visibility = View.VISIBLE
            binding.expandButton.visibility = View.VISIBLE
            binding.signInButton.visibility = View.GONE
            binding.homeScreenMessage.text = ""
            binding.homeScreenMessage.visibility = View.GONE
//            setScheduledPaymentText()
            if (DefaultsViewModel.isLoaded() && DefaultsViewModel.getDefaultQuote()) {
                binding.quoteField.text = getQuote()
                if (account.uid == "null")
                    binding.quoteField.text = getString(R.string.something_went_wrong)
            }
            if (account.email == "alexreid2070@gmail.com")
                setAdminMode(true)
            if (!MyApplication.haveLoadedDataForThisUser) {
                // check if I should load my own UID, or if I'm a JoinUser
                val joinListener = object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.value != null) {  // found the JoinUser node
                            MyApplication.userUID = dataSnapshot.value.toString()
                        }
                        loadEverything()
                  }

                    override fun onCancelled(dataSnapshot: DatabaseError) {
                        MyApplication.displayToast(getString(R.string.user_authorization_failed) + " 112.")
                    }
                }
                val dbRef =
                    MyApplication.databaseref.child("Users/" + MyApplication.userUID)
                        .child("Info")
                        .child("0")
                        .child("JoinUser")
                dbRef.addListenerForSingleValueEvent(joinListener)
            } else {
                setScheduledPaymentText()
            }
        }
        alignPageWithDataState("end of OVC")
    }

    private fun loadEverything() {
        hintModel.loadHints()
        defaultsModel.loadDefaults()
        categoryModel.loadCategories()
        spenderModel.loadSpenders()
        budgetModel.loadBudgetNews()
//        budgetModel.loadBudgets()
        scheduledPaymentModel.loadScheduledPayments()
        retirementUserModel.loadRetirementUsers()
        transactionModel.loadTransactions()
        translationModel.loadTranslations()
        setupDataCallbacks()
        MyApplication.haveLoadedDataForThisUser = true
        MyApplication.database.getReference("Users/" + MyApplication.userUID)
            .child("Info")
            .child(SpenderViewModel.myIndex().toString())
            .child("LastSignIn")
            .child("date")
            .setValue(gCurrentDate.toString())
        MyApplication.database.getReference("Users/" + MyApplication.userUID)
            .child("Info")
            .child(SpenderViewModel.myIndex().toString())
            .child("LastSignIn")
            .child("time").setValue(gCurrentDate.toString())
    }

    private fun alignPageWithDataState(iTag: String)  {
//        Timber.tag("Alex").d("alignpage: $iTag userUID ${MyApplication.userUID}")
        if (MyApplication.userUID != "") {
            binding.homeScreenMessage.text = ""
            binding.homeScreenMessage.visibility = View.GONE
        }

        if (MyApplication.userUID != "" && CategoryViewModel.isLoaded() &&
            SpenderViewModel.isLoaded()
            && ScheduledPaymentViewModel.isLoaded()
            && TransactionViewModel.isLoaded()
            && BudgetViewModel.isLoaded() &&
            DefaultsViewModel.isLoaded() &&
            HintViewModel.isLoaded() &&
            RetirementViewModel.isLoaded()
        ) {
            if (thisIsANewUser()) {
                binding.quoteField.visibility = View.VISIBLE
                binding.quoteField.text = getString(R.string.need_to_do_setup)
//                binding.transactionAddFab.isEnabled = false
                setupNewUser()
            } else {
                (activity as MainActivity).setLoggedOutMode(false)
                binding.expandButton.isEnabled = true
//                setScheduledPaymentText()
//                if (DefaultsViewModel.getDefaultQuote()) {
//                    binding.quoteField.visibility = View.VISIBLE
//                    binding.quoteField.text = getQuote()
//                }
                binding.homeScreenMessage.text = ""
                binding.homeScreenMessage.visibility = View.GONE
                val trackerFragment: TrackerFragment =
                    childFragmentManager.findFragmentById(R.id.home_tracker_fragment) as TrackerFragment
                trackerFragment.initCurrentBudgetMonth()
                launch {
                    trackerFragment.loadBarChart()
                }
                DefaultsViewModel.confirmCategoryDetailsListIsComplete()
                HintViewModel.showHint(parentFragmentManager, cHINT_HOME)
                CategoryViewModel.singleInstance.clearCallback()
                SpenderViewModel.singleInstance.clearCallback()
                TransactionViewModel.singleInstance.clearCallback()
//                BudgetViewModel.singleInstance.clearCallback()
  //              ScheduledPaymentViewModel.singleInstance.clearCallback()
                RetirementViewModel.singleInstance.clearCallback()
                TranslationViewModel.singleInstance.clearCallback()
                DefaultsViewModel.singleInstance.clearCallback()
            }
        } else {
            (activity as MainActivity).setLoggedOutMode(true)
            binding.expandButton.isEnabled = false
        }
    }

    @Suppress("HardCodedStringLiteral")
    private fun setupNewUser() {
        AppUserViewModel.addUserKey()
        CategoryViewModel.updateCategory(0, "Housing", "Hydro", cDiscTypeNondiscretionary,2, true,false)
        CategoryViewModel.updateCategory(0, "Housing", "Insurance", cDiscTypeNondiscretionary,2, true, false)
        CategoryViewModel.updateCategory(0, "Housing", "Internet", cDiscTypeNondiscretionary,2, true,false)
        CategoryViewModel.updateCategory(0, "Housing", "Maintenance", cDiscTypeDiscretionary,2, true,false)
        CategoryViewModel.updateCategory(0, "Housing", "Mortgage", cDiscTypeNondiscretionary,2, true,false)
        CategoryViewModel.updateCategory(0, "Housing", "Property Taxes", cDiscTypeNondiscretionary, 2, true,false)
        CategoryViewModel.updateCategory(0, "Housing", "Rent", cDiscTypeNondiscretionary,2, true,false)
        CategoryViewModel.updateCategory(0, "Life", "Cellphone", cDiscTypeDiscretionary,2, true,false)
        CategoryViewModel.updateCategory(0, "Life", "Charity & Gifts", cDiscTypeDiscretionary,2, true,false)
        CategoryViewModel.updateCategory(0, "Life", "Clothing", cDiscTypeDiscretionary,2, true,false)
        CategoryViewModel.updateCategory(0, "Life", "Entertainment", cDiscTypeDiscretionary,2, true,false)
        CategoryViewModel.updateCategory(0, "Life", "Fitness", cDiscTypeDiscretionary,2, true,false)
        CategoryViewModel.updateCategory(0, "Life", "Groceries", cDiscTypeDiscretionary,2, true,false)
        CategoryViewModel.updateCategory(0, "Life", "Health & Dental", cDiscTypeDiscretionary,2, true,false)
        CategoryViewModel.updateCategory(0, "Life", "Hobbies", cDiscTypeDiscretionary,2, true,false)
        CategoryViewModel.updateCategory(0, "Life", "Home", cDiscTypeDiscretionary,2, true,false)
        CategoryViewModel.updateCategory(0, "Life", "Personal Care", cDiscTypeDiscretionary,2, true,false)
        CategoryViewModel.updateCategory(0, "Life", "Restaurants", cDiscTypeDiscretionary,2, true,false)
        CategoryViewModel.updateCategory(0, "Life", "Travel", cDiscTypeDiscretionary,2, true,false)
        CategoryViewModel.updateCategory(0, "Transportation", "Car Payment", cDiscTypeNondiscretionary,2, true, false)
        CategoryViewModel.updateCategory(0, "Transportation", "Gas", cDiscTypeDiscretionary,2, true,false)
        CategoryViewModel.updateCategory(0, "Transportation", "Insurance", cDiscTypeNondiscretionary,2, true,false)
        CategoryViewModel.updateCategory(0, "Transportation", "Maintenance", cDiscTypeDiscretionary,2, true,false)
        CategoryViewModel.updateCategory(0, "Transportation", "Miscellaneous", cDiscTypeDiscretionary,2, true,false)
        DefaultsViewModel.setColour("Housing", -12400683, false)
        DefaultsViewModel.setColour("Life", -1072612, false)
        DefaultsViewModel.setColour("Transportation", -3751917, false)
        DefaultsViewModel.setPriority("Housing", 0, false)
        DefaultsViewModel.setPriority("Life", 1, false)
        DefaultsViewModel.setPriority("Transportation", 2, false)
        val cat = CategoryViewModel.getID("Life", "Groceries")
        DefaultsViewModel.updateDefaultString("Category", cat.toString())
        DefaultsViewModel.updateDefaultInt("Spender", 0)
        SpenderViewModel.addLocalSpender(Spender(MyApplication.userGivenName, MyApplication.userEmail, 100,1))
        SpenderViewModel.addSpender(0, Spender(MyApplication.userGivenName, MyApplication.userEmail, 100,1))
    }

    private fun signout() {
        Timber.tag("Alex").d("Signout")
        BudgetViewModel.clear()
        CategoryViewModel.clear()
        DefaultsViewModel.clear()
        TransactionViewModel.clear()
        ScheduledPaymentViewModel.clear()
        TranslationViewModel.clear()
        RetirementViewModel.clear()
        SpenderViewModel.clear()
        HintViewModel.clear()
        Firebase.auth.signOut()
        mGoogleSignInClient.signOut()
        MyApplication.userUID = ""
        MyApplication.currentUserEmail = ""
        MyApplication.userFamilyName = ""
        MyApplication.userPhotoURL = ""
        MyApplication.adminMode = false
        (activity as MainActivity).setLoggedOutMode(true)
        binding.expandButton.isEnabled = false
        binding.transactionAddFab.visibility = View.GONE
        binding.expandButton.visibility = View.GONE
        binding.signInButton.visibility = View.VISIBLE
        binding.signInButton.setSize(SignInButton.SIZE_WIDE)
        binding.quoteField.text = ""
        binding.scheduledPaymentField.visibility = View.GONE
        val trackerFragment: TrackerFragment =
            childFragmentManager.findFragmentById(R.id.home_tracker_fragment) as TrackerFragment
        trackerFragment.hideBarChart()
        MyApplication.haveLoadedDataForThisUser = false
        onExpandClicked()
        binding.adminButton.visibility = View.GONE
    }

    private fun setAdminMode(inAdminMode: Boolean) {
        if (inAdminMode)
            binding.adminButton.visibility = View.VISIBLE
        else
            binding.adminButton.visibility = View.GONE
        MyApplication.adminMode = inAdminMode
    }

    override fun onDestroy() {
        super.onDestroy()
        CategoryViewModel.singleInstance.clearCallback()
        SpenderViewModel.singleInstance.clearCallback()
        TransactionViewModel.singleInstance.clearCallback()
//        BudgetViewModel.singleInstance.clearCallback()
  //      ScheduledPaymentViewModel.singleInstance.clearCallback()
        RetirementViewModel.singleInstance.clearCallback()
        TranslationViewModel.singleInstance.clearCallback()
        DefaultsViewModel.singleInstance.clearCallback()
        _binding = null
    }
}