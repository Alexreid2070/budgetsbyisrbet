package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.GestureDetectorCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.material.color.MaterialColors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.isrbet.budgetsbyisrbet.databinding.FragmentHomeBinding


class HomeFragment : Fragment() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryModel.clearCallback() // calling this causes the object to exist by this point.  For some reason it is not there otherwise
        spenderModel.clearCallback() // ditto, see above
        userModel.clearCallback() // ditto, see above
        transactionModel.clearCallback()
        budgetModel.clearCallback()
        scheduledPaymentModel.clearCallback()
        retirementUserModel.clearCallback()
        defaultsModel.clearCallback()
        translationModel.clearCallback()
        hintModel.clearCallback()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment - DON'T seem to need this inflate.  In fact, if I call it, it'll call Main's onCreateView multiple times
//        inflater.inflate(R.layout.fragment_home, container, false)

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
                            findNavController().navigate(R.id.SettingsFragment)
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
                        Log.d("Alex", "firebaseAuthWithGoogle:" + account.id)
                        MyApplication.userGivenName = account.givenName.toString()
                        MyApplication.userFamilyName = account.familyName.toString()
                        firebaseAuthWithGoogle(account.idToken!!)
                    } catch (e: ApiException) {
                        // Google Sign In failed, update UI appropriately
                        Log.w("Alex", "Google sign in failed", e)
                    }
                } else
                    Log.d(
                        "Alex",
                        "in registerforactivityresult, result was not OK " + result.resultCode
                    )
            }

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
        binding.signInButton.setOnClickListener {
            onSignIn(mainActivityResultLauncher)
        }
        auth = Firebase.auth

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
            findNavController().navigate(R.id.SettingsFragment)
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
        binding.retirementButton.setOnClickListener {
            findNavController().navigate(R.id.RetirementFragment)
        }
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val scrollView = view.findViewById<NestedScrollView>(R.id.scroll_view)
        scrollView.setOnTouchListener(object : View.OnTouchListener {
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
            binding.quoteLabel.visibility = View.GONE
            binding.transactionAddFab.visibility = View.GONE
            binding.expandButton.visibility = View.GONE
            binding.homeScreenMessage.visibility = View.VISIBLE
            binding.homeScreenMessage.text = getString(R.string.you_must_sign_in)
            binding.signInButton.setSize(SignInButton.SIZE_WIDE)
        } else {
            binding.signInButton.visibility = View.GONE
            binding.homeScreenMessage.text = ""
            binding.homeScreenMessage.visibility = View.GONE
            if (DefaultsViewModel.getDefaultQuote()) {
                binding.quoteLabel.visibility = View.VISIBLE
                if (MyApplication.userEmail != MyApplication.currentUserEmail)
                    binding.quoteField.text =
                        "Currently impersonating " + MyApplication.currentUserEmail
                else
                    binding.quoteField.text = getQuote()
            }
        }
//        Log.d("Alex", "account.email is " + account?.email + " and name is " + account?.givenName + " and uid " + MyApplication.userUID)
        MyApplication.userGivenName = account?.givenName.toString()
        MyApplication.userFamilyName = account?.familyName.toString()
        if (account != null) {
            MyApplication.userPhotoURL = account.photoUrl.toString()
            Glide.with(requireContext()).load(MyApplication.userPhotoURL)
                .thumbnail(0.5f)
                .crossFade()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.imgProfilePic)
            binding.userName.text = MyApplication.userGivenName + " " + MyApplication.userFamilyName
        }
        setAdminMode(account?.email == "alexreid2070@gmail.com")
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        signIn(currentUser)
        binding.imgProfilePic.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.are_you_sure))
                .setMessage(getString(R.string.are_you_sure_that_you_want_to_sign_out))
                .setPositiveButton(getString(R.string.sign_out)) { _, _ -> signout() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }  // nothing should happen, other than dialog closes
                .show()
        }
        binding.signoutText.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.are_you_sure))
                .setMessage(getString(R.string.are_you_sure_that_you_want_to_sign_out))
                .setPositiveButton(getString(R.string.sign_out)) { _, _ -> signout() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }  // nothing should happen, other than dialog closes
                .show()
        }
        if (homePageExpansionAreaExpanded)
            expandTop()
        // this next block allows the floating action button to move up and down (it starts constrained to bottom)
        val set = ConstraintSet()
        val constraintLayout = binding.constraintLayout
        set.clone(constraintLayout)
        set.clear(R.id.budget_add_fab, ConstraintSet.TOP)
        set.applyTo(constraintLayout)

    }

    private fun setupDataCallbacks() {
        DefaultsViewModel.singleInstance.setCallback(object : DataUpdatedCallback {
            override fun onDataUpdate() {
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
        BudgetViewModel.singleInstance.setCallback(object : DataUpdatedCallback {
            override fun onDataUpdate() {
                alignPageWithDataState("BudgetViewModel")
            }
        })
        ScheduledPaymentViewModel.singleInstance.setCallback(object : DataUpdatedCallback {
            override fun onDataUpdate() {
                alignPageWithDataState("RTViewModel")
                ScheduledPaymentViewModel.generateScheduledPayments(activity as MainActivity)
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
        binding.expandButtonLayout.setBackgroundColor(
            MaterialColors.getColor(
                requireContext(),
                R.attr.colorPrimary,
                Color.BLACK
            )
        )
        binding.expandButton.setImageResource(R.drawable.ic_baseline_expand_less_24)
        binding.expansionAreaLayout.visibility = View.VISIBLE
        homePageExpansionAreaExpanded = true
    }

    private fun retractTop() {
        binding.expandButtonLayout.setBackgroundColor(
            MaterialColors.getColor(
                requireContext(),
                R.attr.colorSecondary,
                Color.BLACK
            )
        )
        binding.expandButton.setImageResource(R.drawable.ic_baseline_expand_more_24)
        binding.expansionAreaLayout.visibility = View.GONE
        homePageExpansionAreaExpanded = false
    }

    private fun getQuote(): String {
        return if (MyApplication.userEmail != MyApplication.currentUserEmail)
            "Currently impersonating " + MyApplication.currentUserEmail
        else
            MyApplication.getQuote()
    }

    private fun onSignIn(mainActivityResultLauncher: ActivityResultLauncher<Intent>) {
        Log.d("Alex", "onSignIn")
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
                    Log.d("Alex", "signInWithCredential:success.  User is " + user.toString())
                    // this code is only hit when a user signs in successfully.
                    signIn(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("Alex", "signInWithCredential:failure", task.exception)
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
            binding.userName.text = MyApplication.userGivenName + " " + MyApplication.userFamilyName
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
            if (DefaultsViewModel.getDefaultQuote()) {
                binding.quoteLabel.visibility = View.VISIBLE
                binding.quoteField.text = getQuote()
                if (account.uid == "null")
                    binding.quoteField.text = "SOMETHING WENT WRONG.  Please sign out and back in."
            }
            if (account.email == "alexreid2070@gmail.com")
                setAdminMode(true)
            if (!MyApplication.haveLoadedDataForThisUser) {
                // check if I should load my own UID, or if I'm a JoinUser
                val joinListener = object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.value != null) {  // found the JoinUser node
                            Log.d("Alex", "dataSnapshot.value is " + dataSnapshot.value)
                            MyApplication.userUID = dataSnapshot.value.toString()
                        }
                        loadEverything()
                  }

                    override fun onCancelled(dataSnapshot: DatabaseError) {
                        MyApplication.displayToast(MyApplication.getString(R.string.user_authorization_failed) + " 112.")
                    }
                }
                val dbRef =
                    MyApplication.databaseref.child("Users/" + MyApplication.userUID)
                        .child("Info")
                        .child("0")
                        .child("JoinUser")
                dbRef.addListenerForSingleValueEvent(joinListener)
            }
        }
        alignPageWithDataState("end of OVC")
    }

    private fun loadEverything() {
        setupDataCallbacks()
        hintModel.loadHints()
        defaultsModel.loadDefaults()
        categoryModel.loadCategories()
        spenderModel.loadSpenders()
        budgetModel.loadBudgets()
        scheduledPaymentModel.loadScheduledPayments()
        retirementUserModel.loadRetirementUsers()
        transactionModel.loadTransactions()
        translationModel.loadTranslations()
        MyApplication.haveLoadedDataForThisUser = true
        val dateNow = Calendar.getInstance()
        MyApplication.database.getReference("Users/" + MyApplication.userUID)
            .child("Info")
            .child(SpenderViewModel.myIndex().toString())
            .child("LastSignIn")
            .child("date")
            .setValue(giveMeMyDateFormat(dateNow))
        MyApplication.database.getReference("Users/" + MyApplication.userUID)
            .child("Info")
            .child(SpenderViewModel.myIndex().toString())
            .child("LastSignIn")
            .child("time").setValue(giveMeMyTimeFormat(dateNow))
    }

    private fun alignPageWithDataState(iTag: String)  {
//        Log.d("Alex", "alignPage $iTag")
        if (MyApplication.userUID != "") {
            binding.homeScreenMessage.text = ""
            binding.homeScreenMessage.visibility = View.GONE
        }

        if (MyApplication.userUID != "" && CategoryViewModel.isLoaded() && SpenderViewModel.isLoaded()
            && ScheduledPaymentViewModel.isLoaded()
            && TransactionViewModel.isLoaded() && BudgetViewModel.isLoaded() &&
            DefaultsViewModel.isLoaded() && HintViewModel.isLoaded()
        ) {
            if (thisIsANewUser()) {
                Log.d("Alex", "This is a new user")
                binding.quoteField.visibility = View.VISIBLE
                binding.quoteField.text = "THIS IS A NEW USER.  NEED TO DO SETUP BEFORE PROCEEDING."
//                binding.transactionAddFab.isEnabled = false
                setupNewUser()
            } else {
                (activity as MainActivity).setLoggedOutMode(false)
                binding.expandButton.isEnabled = true
                if (DefaultsViewModel.getDefaultQuote()) {
                    binding.quoteLabel.visibility = View.VISIBLE
                    binding.quoteField.visibility = View.VISIBLE
                    binding.quoteField.text = getQuote()
                }
                binding.homeScreenMessage.text = ""
                binding.homeScreenMessage.visibility = View.GONE
                val trackerFragment: TrackerFragment =
                    childFragmentManager.findFragmentById(R.id.home_tracker_fragment) as TrackerFragment
                trackerFragment.initCurrentBudgetMonth()
                trackerFragment.loadBarChart()
                DefaultsViewModel.confirmCategoryDetailsListIsComplete()
                HintViewModel.showHint(parentFragmentManager, cHINT_HOME)
                CategoryViewModel.singleInstance.clearCallback()
                SpenderViewModel.singleInstance.clearCallback()
                TransactionViewModel.singleInstance.clearCallback()
                BudgetViewModel.singleInstance.clearCallback()
                ScheduledPaymentViewModel.singleInstance.clearCallback()
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
        Log.d("Alex", "sign out attempted")
        BudgetViewModel.clear()
        CategoryViewModel.clear()
        DefaultsViewModel.clear()
        TransactionViewModel.clear()
        ScheduledPaymentViewModel.clear()
        TranslationViewModel.clear()
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
        binding.quoteLabel.visibility = View.GONE
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
        BudgetViewModel.singleInstance.clearCallback()
        ScheduledPaymentViewModel.singleInstance.clearCallback()
        TranslationViewModel.singleInstance.clearCallback()
        DefaultsViewModel.singleInstance.clearCallback()
        _binding = null
    }
}