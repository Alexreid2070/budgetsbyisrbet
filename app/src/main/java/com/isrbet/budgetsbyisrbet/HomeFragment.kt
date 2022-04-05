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
import androidx.core.content.ContextCompat
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
import java.lang.Exception


class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private val defaultsModel: DefaultsViewModel by viewModels()
    private val expenditureModel: ExpenditureViewModel by viewModels()
    private val categoryModel: CategoryViewModel by viewModels()
    private val spenderModel: SpenderViewModel by viewModels()
    private val budgetModel: BudgetViewModel by viewModels()
    private val recurringTransactionModel: RecurringTransactionViewModel by viewModels()
    private val userModel: AppUserViewModel by viewModels()
    private val chatModel: ChatViewModel by viewModels()
    private val hintModel: HintViewModel by viewModels()
    private val translationModel: TranslationViewModel by viewModels()
    private var gestureDetector: GestureDetectorCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryModel.clearCallback() // calling this causes the object to exist by this point.  For some reason it is not there otherwise
        spenderModel.clearCallback() // ditto, see above
        userModel.clearCallback() // ditto, see above
        chatModel.clearCallback() // ditto, see above
        expenditureModel.clearCallback()
        budgetModel.clearCallback()
        recurringTransactionModel.clearCallback()
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
//                if (event1 != null && event2 != null) { // not sure why they'd be null, but sometimes they are...
                if (event2.y > event1.y) {
                    // negative for up, positive for down
                    Log.d("Alex", "swiped down " + binding.scrollView.canScrollVertically(-1))
                    if (!binding.scrollView.canScrollVertically(-1)) { // ie can't scroll down anymore
                        if (binding.expansionAreaLayout.visibility == View.GONE)
                            onExpandClicked()
                        else // already expanded and user swiped down, so open Settings
                            findNavController().navigate(R.id.SettingsFragment)
                    }
                } else if (event2.y < event1.y) {
                    Log.d("Alex", "swiped up " + binding.scrollView.canScrollVertically(1))
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

        /*
        // This next line checks if the app has permission to look at notifications
        if (Settings.Secure.getString(
                (activity as MainActivity).contentResolver,
                "enabled_notification_listeners"
            ).contains(
                (activity as MainActivity).applicationContext.packageName
            )
        ) {
            // yes, it has permission
//            Toast.makeText(this, "App has permission to look at notifications", Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
            if (Settings.Secure.getString(
                    (activity as MainActivity).contentResolver,
                    "enabled_notification_listeners"
                ).contains(
                    (activity as MainActivity).applicationContext.packageName
                )
            ) {
                Log.d("Alex", "After asking, it's true")
            } else
                Log.d("Alex", "After asking, it's false")

        } */
        binding.transactionAddFab.setOnClickListener {
            findNavController().navigate(R.id.TransactionFragment)
        }
        binding.expandButton.setOnClickListener {
            onExpandClicked()
        }
        binding.settingsButton.setOnClickListener {
            findNavController().navigate(R.id.SettingsFragment)
        }
        binding.helpButton.setOnClickListener {
            findNavController().navigate(R.id.HelpFragment)
        }
        binding.chatButton.setOnClickListener {
            findNavController().navigate(R.id.ChatFragment)
        }
        binding.adminButton.setOnClickListener {
            findNavController().navigate(R.id.AdminFragment)
        }
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

/*        view.findViewById<Button>(R.id.expenditure_button)
            ?.setOnClickListener { iview: View ->
                iview.findNavController().navigate(R.id.TransactionFragment)
            }
        view.findViewById<Button>(R.id.view_all_button)
            ?.setOnClickListener { iview: View ->
                iview.findNavController().navigate(R.id.TransactionViewAllFragment)
            }
        view.findViewById<Button>(R.id.dashboard_button)
            ?.setOnClickListener { iview: View ->
                iview.findNavController().navigate(R.id.DashboardFragment)
            }
  */
        val scrollView = view.findViewById<NestedScrollView>(R.id.scroll_view)
/*        scrollView.viewTreeObserver?.addOnScrollChangedListener {
            if (!scrollView.canScrollVertically(1)) {
                Log.d("Alex", "Can't scroll vertically")
            }
            else
                Log.d("Alex", "Can scroll vertically")
        }


        scrollView.setOnScrollChangeListener (object: View.OnScrollChangeListener {
            override fun onScrollChange(p0: View?, p1: Int, p2: Int, p3: Int, p4: Int) {
                val bottom =
                    scrollView.getChildAt(scrollView.childCount - 1).height - scrollView.height - attr.scrollY

                if (attr.scrollY == 0) {
                    Log.d("Alex", "top detected in scroll")
                }
                if (bottom == 0) {
                    Log.d("Alex", "bottom detected in scroll")
                }
            }
        })



        scrollView.viewTreeObserver.addOnScrollChangedListener {
            if (scrollView.scrollY < scrollView.getChildAt(0).bottom - scrollView.height - 0) {
                Log.d("Alex", "top detected in scroll")
            } else {
                Log.d("Alex", "bottom detected in scroll")
            }
        }

        scrollView.setOnClickListener(object :
            View.OnClickListener {
            override fun onClick(p0: View?) {
                Log.d("Alex", "clicked " + scrollView.canScrollVertically(1))
            }
        })
*/
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

//        alignExpenditureMenuWithDataState()

        CategoryViewModel.singleInstance.setCallback(object : DataUpdatedCallback {
            override fun onDataUpdate() {
                alignExpenditureMenuWithDataState()
            }
        })
        SpenderViewModel.singleInstance.setCallback(object : DataUpdatedCallback {
            override fun onDataUpdate() {
                alignExpenditureMenuWithDataState()
                if (SpenderViewModel.singleUser()) {
                    (activity as MainActivity).singleUserMode()
                }
            }
        })
        ChatViewModel.singleInstance.setCallback(object : DataUpdatedCallback {
            override fun onDataUpdate() {
                tryToUpdateChatIcon()
            }
        })
        HintViewModel.singleInstance.setCallback(object : DataUpdatedCallback {
            override fun onDataUpdate() {
                alignExpenditureMenuWithDataState()
            }
        })
        ExpenditureViewModel.singleInstance.setCallback(object : DataUpdatedCallback {
            override fun onDataUpdate() {
                alignExpenditureMenuWithDataState()
            }
        })
        BudgetViewModel.singleInstance.setCallback(object : DataUpdatedCallback {
            override fun onDataUpdate() {
                alignExpenditureMenuWithDataState()
            }
        })
        RecurringTransactionViewModel.singleInstance.setCallback(object : DataUpdatedCallback {
            override fun onDataUpdate() {
                alignExpenditureMenuWithDataState()
            }
        })

        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account?.email == null) {
            // turn off all menu/buttons
//            (activity as MainActivity).setDrawerMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            (activity as MainActivity).setLoggedOutMode(true)
            binding.expandButton.isEnabled = false
            binding.signInButton.visibility = View.VISIBLE
            binding.quoteField.text = ""
            binding.quoteLabel.visibility = View.GONE
            binding.homeScreenMessage.visibility = View.VISIBLE
            binding.homeScreenMessage.text =
                "You must sign in using your Google account to proceed.  Click below to continue."
            binding.signInButton.setSize(SignInButton.SIZE_WIDE)
        } else {
            binding.signInButton.visibility = View.GONE
            binding.homeScreenMessage.text = ""
            binding.homeScreenMessage.visibility = View.GONE
            if (DefaultsViewModel.getDefault(cDEFAULT_QUOTE) == "On") {
                binding.quoteLabel.visibility = View.VISIBLE
                if (MyApplication.userEmail != MyApplication.currentUserEmail)
                    binding.quoteField.text =
                        "Currently impersonating " + MyApplication.currentUserEmail
                else
                    binding.quoteField.text = getQuote()
            }
        }
        Log.d("Alex", "aa5 useruid is '" + MyApplication.userUID + "'")
        Log.d("Alex", "account.email is " + account?.email + " and name is " + account?.givenName)
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
                .setTitle("Are you sure?")
                .setMessage("Are you sure that you want to sign out?")
                .setPositiveButton("Sign Out") { _, _ -> signout() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }  // nothing should happen, other than dialog closes
                .show()
        }
        binding.signoutText.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Are you sure?")
                .setMessage("Are you sure that you want to sign out?")
                .setPositiveButton("Sign Out") { _, _ -> signout() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }  // nothing should happen, other than dialog closes
                .show()
        }
    }

    private fun onExpandClicked() {
        if (binding.expansionAreaLayout.visibility == View.GONE) { // ie expand the section
            binding.expandButtonLayout.setBackgroundColor(
                MaterialColors.getColor(
                    requireContext(),
                    R.attr.colorPrimary,
                    Color.BLACK
                )
            )
            binding.expandButton.setImageResource(R.drawable.ic_baseline_expand_less_24)
            binding.expansionAreaLayout.visibility = View.VISIBLE
        } else { // ie retract the section
            binding.expandButtonLayout.setBackgroundColor(
                MaterialColors.getColor(
                    requireContext(),
                    R.attr.colorSecondary,
                    Color.BLACK
                )
            )
            binding.expandButton.setImageResource(R.drawable.ic_baseline_expand_more_24)
            binding.expansionAreaLayout.visibility = View.GONE
            doSomethingIfThereAreUnreadMessages()
        }
    }

    private fun getQuote(): String {
        return if (MyApplication.userEmail != MyApplication.currentUserEmail)
            "Currently impersonating " + MyApplication.currentUserEmail
        else
            MyApplication.getQuote()
    }

    private fun onSignIn(mainActivityResultLauncher: ActivityResultLauncher<Intent>) {
        Log.d("Alex", "onSignIn")
//        binding.homeScreenMessage.text = ""
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

    @SuppressLint("SetTextI18n")
    private fun signIn(account: FirebaseUser?) {
        Log.d(
            "Alex",
            "in signIn, account is " + account?.email + " and uid is " + MyApplication.userUID
        )
        MyApplication.userEmail = account?.email.toString()
        if (account != null) {
            if (MyApplication.userUID == "") {  // ie don't want to override this if Admin is impersonating another user...
                MyApplication.userUID = account.uid
                MyApplication.originalUserUID = account.uid
                Log.d("Alex", "Just set userUID to " + account.uid)
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
            binding.signInButton.visibility = View.VISIBLE
            binding.signInButton.setSize(SignInButton.SIZE_WIDE)
            binding.homeScreenMessage.visibility = View.VISIBLE
            binding.homeScreenMessage.text =
                "You must sign in using your Google account to proceed.  Click below to continue."
        } else {
            binding.signInButton.visibility = View.GONE
            binding.homeScreenMessage.text = ""
            binding.homeScreenMessage.visibility = View.GONE
            if (DefaultsViewModel.getDefault(cDEFAULT_QUOTE) == "On") {
                binding.quoteLabel.visibility = View.VISIBLE
                binding.quoteField.text = getQuote()
                if (account.uid == "null")
                    binding.quoteField.text = "SOMETHING WENT WRONG.  Please sign out and back in."
            }
            if (account.email == "alexreid2070@gmail.com")
                setAdminMode(true)
            doSomethingIfThereAreUnreadMessages()
            Log.d("Alex", "Should I load? " + !MyApplication.haveLoadedDataForThisUser)
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
                        MyApplication.displayToast("User authorization failed 112.")
                    }
                }
                val dbRef =
                    MyApplication.databaseref.child("Users/" + MyApplication.userUID)
                        .child("Info")
                        .child("JoinUser")
                dbRef.addListenerForSingleValueEvent(joinListener)

       //         loadEverything()
            }
        }
        alignExpenditureMenuWithDataState()
    }

    private fun loadEverything() {
        Log.d("Alex", "uid is " + MyApplication.userUID)
        getLastReadChatsInfo()
        hintModel.loadHints()
        defaultsModel.loadDefaults()
        categoryModel.loadCategories()
        spenderModel.loadSpenders()
        budgetModel.loadBudgets()
        recurringTransactionModel.loadRecurringTransactions(activity as MainActivity)
        expenditureModel.loadExpenditures()
        chatModel.loadChats()
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

    @SuppressLint("SetTextI18n")
    private fun alignExpenditureMenuWithDataState() {
        if (MyApplication.userUID != "") {
            binding.homeScreenMessage.text = ""
            binding.homeScreenMessage.visibility = View.GONE
        }

        if (MyApplication.userUID != "" && CategoryViewModel.isLoaded() && SpenderViewModel.isLoaded()
            && RecurringTransactionViewModel.isLoaded()
            && ExpenditureViewModel.isLoaded() && BudgetViewModel.isLoaded() &&
            DefaultsViewModel.isLoaded() && HintViewModel.isLoaded()
        ) {
            if (thisIsANewUser()) {
                Log.d("Alex", "This is a new user")
            } else {
                Log.d("Alex", "This is not a new user")
            }
//            binding.expenditureButton.visibility = View.VISIBLE
//            binding.viewAllButton.visibility = View.VISIBLE
//            binding.dashboardButton.visibility = View.VISIBLE
            (activity as MainActivity).setLoggedOutMode(false)
            binding.expandButton.isEnabled = true
            if (DefaultsViewModel.getDefault(cDEFAULT_QUOTE) == "On") {
                binding.quoteLabel.visibility = View.VISIBLE
                binding.quoteField.visibility = View.VISIBLE
                if (thisIsANewUser())
                    binding.quoteField.text = "THIS IS A NEW USER.  NEED TO DO SETUP."
                else
                    binding.quoteField.text = getQuote()
            }
            binding.homeScreenMessage.text = ""
            binding.homeScreenMessage.visibility = View.GONE
            val trackerFragment: TrackerFragment =
                childFragmentManager.findFragmentById(R.id.home_tracker_fragment) as TrackerFragment
            trackerFragment.loadBarChart()
            DefaultsViewModel.confirmCategoryDetailsListIsComplete()
            HintViewModel.showHint(requireContext(), binding.transactionAddFab, "Home")
        } else {
            (activity as MainActivity).setLoggedOutMode(true)
            binding.expandButton.isEnabled = false
        }
    }

    private fun signout() {
        Log.d("Alex", "sign out attempted")
        BudgetViewModel.clear()
        CategoryViewModel.clear()
        ChatViewModel.clear()
        DefaultsViewModel.clear()
        ExpenditureViewModel.clear()
        RecurringTransactionViewModel.clear()
        SpenderViewModel.clear()
        Firebase.auth.signOut()
        mGoogleSignInClient.signOut()
        MyApplication.userUID = ""
        MyApplication.currentUserEmail = ""
        MyApplication.userFamilyName = ""
        MyApplication.userPhotoURL = ""
        MyApplication.adminMode = false
        doSomethingIfThereAreUnreadMessages()
        (activity as MainActivity).setLoggedOutMode(true)
        binding.expandButton.isEnabled = false
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

    private fun getLastReadChatsInfo() {
        val infoListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.value != null) { // nothing exists at this node so we can add it
                    dataSnapshot.children.forEach {
                        when (it.key) {
                            "date" -> MyApplication.lastReadChatsDate = it.value.toString()
                            "time" -> MyApplication.lastReadChatsTime = it.value.toString()
                        }
                    }
                    tryToUpdateChatIcon()
                }
            }

            override fun onCancelled(dataSnapshot: DatabaseError) {
                MyApplication.displayToast("User authorization failed 113.")
            }
        }
        val dbRef =
            MyApplication.databaseref.child("Users/" + MyApplication.userUID)
                .child("Info")
                .child(SpenderViewModel.myIndex().toString())
                .child("LastReadChats")
        dbRef.addListenerForSingleValueEvent(infoListener)
    }

    fun tryToUpdateChatIcon() {
        // this is called when lastSignedIn date/time are found, and also when chats are loaded.  When both are done then do something
        if (ChatViewModel.getCount() > 0 && MyApplication.lastReadChatsDate != "") {
            doSomethingIfThereAreUnreadMessages()
        }
    }

    private fun doSomethingIfThereAreUnreadMessages() {
        if (ChatViewModel.getCount() > 0 && MyApplication.lastReadChatsDate != "") {
            val tChat = ChatViewModel.getLastChat()
            if (MyApplication.lastReadChatsDate < tChat.date ||
                (MyApplication.lastReadChatsDate == tChat.date && MyApplication.lastReadChatsTime < tChat.time)) {
                // there are unread chats
                binding.expandButton.setImageDrawable(ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_fas_envelope))
                binding.chatImage.setImageDrawable(ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_fas_envelope))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CategoryViewModel.singleInstance.clearCallback()
        SpenderViewModel.singleInstance.clearCallback()
        ChatViewModel.singleInstance.clearCallback()
        ExpenditureViewModel.singleInstance.clearCallback()
        BudgetViewModel.singleInstance.clearCallback()
        RecurringTransactionViewModel.singleInstance.clearCallback()
        DefaultsViewModel.singleInstance.clearCallback()
        _binding = null
    }
}