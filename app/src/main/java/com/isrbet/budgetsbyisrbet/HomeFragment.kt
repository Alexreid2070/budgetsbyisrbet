package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.icu.util.Calendar
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
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
    private val expenditureModel: ExpenditureViewModel by viewModels()
    private val categoryModel: CategoryViewModel by viewModels()
    private val spenderModel: SpenderViewModel by viewModels()
    private val budgetModel: BudgetViewModel by viewModels()
    private val recurringTransactionModel: RecurringTransactionViewModel by viewModels()
    private val userModel: UserViewModel by viewModels()
    private val chatModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryModel.clearCallback() // calling this causes the object to exist by this point.  For some reason it is not there otherwise
        spenderModel.clearCallback() // ditto, see above
        userModel.clearCallback() // ditto, see above
        chatModel.clearCallback() // ditto, see above
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment - DON'T seem to need this inflate.  In fact, if I call it, it'll call Main's onCreateView multiple times
//        inflater.inflate(R.layout.fragment_home, container, false)

        val mainActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if(result.resultCode == Activity.RESULT_OK){
                val data = result.data
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    // Google Sign In was successful, authenticate with Firebase
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d("Alex", "firebaseAuthWithGoogle:" + account.id)
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    // Google Sign In failed, update UI appropriately
                    Log.w("Alex", "Google sign in failed", e)
                }
            } else
                Log.d("Alex", "in registerforactivityresult, result was not OK")
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

        // These 2 lines open up the notification settings app so that the user can give access to notifications permissions
        // to this app
//        var intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
//        startActivity(intent);
        //     MyApplication.myCustomNotificationListenerService = CustomNotificationListenerService()

        // This next line checks if the app has permission to look at notifications
        if (Settings.Secure.getString((activity as MainActivity).contentResolver,"enabled_notification_listeners").contains(
                (activity as MainActivity).applicationContext.packageName
            )) {
            // yes, it has permission
//            Toast.makeText(this, "App has permission to look at notifications", Toast.LENGTH_SHORT).show()
        }
        else {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
            if (Settings.Secure.getString((activity as MainActivity).contentResolver,"enabled_notification_listeners").contains(
                    (activity as MainActivity).applicationContext.packageName
                )) {
                Log.d("Alex", "After asking, it's true")
            } else
                Log.d("Alex", "After asking, it's false")
        }
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.expenditure_button)
            ?.setOnClickListener { iview: View ->
                iview.findNavController().navigate(R.id.TransactionFragment)
            }
        view.findViewById<Button>(R.id.view_all_button)
            ?.setOnClickListener { iview: View ->
                iview.findNavController().navigate(R.id.ViewTransactionsFragment)
            }
        view.findViewById<Button>(R.id.dashboard_button)
            ?.setOnClickListener { iview: View ->
                iview.findNavController().navigate(R.id.DashboardFragment)
            }
        view.findViewById<TextView>(R.id.quote_field)?.setOnTouchListener(object :
            OnSwipeTouchListener(requireContext()) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                Log.d("Alex", "swiped left")
                val navController = view.findNavController()
                navController.navigate(R.id.DashboardFragment)
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                Log.d("Alex", "swiped right")
                val navController = view.findNavController()
                navController.navigate(R.id.ViewTransactionsFragment)
            }

            override fun onSwipeBottom() {
                super.onSwipeBottom()
                Log.d("Alex", "swiped bottom, want to show menu")
                (activity as MainActivity).openDrawer()
            }

            override fun onSwipeTop() {
                super.onSwipeTop()
                Log.d("Alex", "swiped top, want to go to Add Transaction")
                val navController = view.findNavController()
                navController.navigate(R.id.TransactionFragment)
            }
        })

//        alignExpenditureMenuWithDataState()

        CategoryViewModel.singleInstance.setCallback(object: CategoryDataUpdatedCallback {
            override fun onDataUpdate() {
                Log.d("Alex", "in Category onDataUpdate callback")
                alignExpenditureMenuWithDataState()
            }
        })
        SpenderViewModel.singleInstance.setCallback(object: SpenderDataUpdatedCallback {
            override fun onDataUpdate() {
                Log.d("Alex", "in Spender onDataUpdate callback")
                alignExpenditureMenuWithDataState()
                if (SpenderViewModel.singleUser()) {
                    (activity as MainActivity).singleUserMode()
                }
            }
        })
        ChatViewModel.singleInstance.setCallback(object: ChatDataUpdatedCallback {
            override fun onDataUpdate() {
                Log.d("Alex", "in Chat onDataUpdate callback")
                tryToUpdateChatIcon()
            }
        })

        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account?.email == null) {
            // turn off all menu/buttons
//            (activity as MainActivity).setDrawerMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            (activity as MainActivity).setLoggedOutMode(true)
            binding.signInButton.visibility = View.VISIBLE
            binding.quoteField.text = ""
            binding.homeScreenMessage.text = "You must sign in using your Google account to proceed.  Click below to continue."
            binding.signInButton.setSize(SignInButton.SIZE_WIDE)
//            findViewById<TextView>(R.id.homeScreenMessage).text = "You must sign in using your Google account to proceed.  Click below to continue."
            // NEED TO CALL Home Fragment to make adjustments
        } else {
            binding.signInButton.visibility = View.GONE
            binding.homeScreenMessage.text = ""
            binding.quoteField.text = MyApplication.getQuote()
            // NEED TO CALL Home Fragment to make adjustments
/*            var quoteField = findViewById<TextView>(R.id.quote_field)
            if (quoteField != null)
                quoteField.visibility = View.VISIBLE
            var homeScreenMessage = findViewById<TextView>(R.id.homeScreenMessage)
            if (homeScreenMessage != null)
                homeScreenMessage.text = "" */
        }
        Log.d("Alex", "account.email is " + account?.email + " and name is " + account?.givenName)
        MyApplication.userGivenName = account?.givenName.toString()
        (activity as MainActivity).setAdminMode(account?.email == "alexreid2070@gmail.com")
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        signIn(currentUser)
    }

    private fun onSignIn(mainActivityResultLauncher: ActivityResultLauncher<Intent>) {
        Log.d("Alex", "onSignIn")
        binding.homeScreenMessage.text = ""
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
        Log.d("Alex", "in signIn, account is " + account?.email)
        MyApplication.userEmail = account?.email.toString()
        if (MyApplication.userUID == "")  // ie don't want to override this if Admin is impersonating another user...
            MyApplication.userUID = account?.uid.toString()
        if (MyApplication.currentUserEmail == "")  // ie don't want to override this if Admin is impersonating another user...
            MyApplication.currentUserEmail = account?.email.toString()
        if (account == null) {
            binding.signInButton.visibility = View.VISIBLE
            binding.signInButton.setSize(SignInButton.SIZE_WIDE)
            binding.homeScreenMessage.text = "You must sign in using your Google account to proceed.  Click below to continue."
        }
        else {
            binding.signInButton.visibility = View.GONE
            binding.homeScreenMessage.text = ""
            binding.quoteField.text = MyApplication.getQuote()
            if (account.email == "alexreid2070@gmail.com")
                (activity as MainActivity).setAdminMode(true)
            requireActivity().invalidateOptionsMenu()
            Log.d("Alex", "Should I load? " + !MyApplication.haveLoadedDataForThisUser)
            if (!MyApplication.haveLoadedDataForThisUser) {
                getLastReadChatsInfo()
                defaultsModel.loadDefaults()
                expenditureModel.loadExpenditures()
                categoryModel.loadCategories()
                spenderModel.loadSpenders()
                budgetModel.loadBudgets()
                recurringTransactionModel.loadRecurringTransactions(activity as MainActivity)
                chatModel.loadChats()
                MyApplication.haveLoadedDataForThisUser = true
                val dateNow = Calendar.getInstance()
                MyApplication.database.getReference("Users/"+MyApplication.userUID)
                    .child("Info")
                    .child("LastSignIn")
                    .child("date")
                    .setValue(giveMeMyDateFormat(dateNow))
                MyApplication.database.getReference("Users/"+MyApplication.userUID)
                    .child("Info")
                    .child("LastSignIn")
                    .child("time").setValue(giveMeMyTimeFormat(dateNow))
            }
        }
        alignExpenditureMenuWithDataState()
    }

    private fun alignExpenditureMenuWithDataState() {
        if (MyApplication.userUID != "")
            binding.homeScreenMessage.text = ""

        if (MyApplication.userUID != "" && CategoryViewModel.getCount() > 0 && SpenderViewModel.getActiveCount() > 0) {
            Log.d("Alex", "alignExpenditureMenu true")
            binding.expenditureButton.visibility = View.VISIBLE
            binding.viewAllButton.visibility = View.VISIBLE
            binding.dashboardButton.visibility = View.VISIBLE
//            (activity as MainActivity).setDrawerMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            (activity as MainActivity).setLoggedOutMode(false)
            binding.quoteField.visibility = View.VISIBLE
            binding.quoteField.text = MyApplication.getQuote()
            binding.homeScreenMessage.text = ""
        } else {
            Log.d("Alex", "alignExpenditureMenu false")
            binding.expenditureButton.visibility = View.GONE
            binding.viewAllButton.visibility = View.GONE
            binding.dashboardButton.visibility = View.GONE
//            (activity as MainActivity).setDrawerMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            (activity as MainActivity).setLoggedOutMode(true)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        for (i in 0 until menu.size()) {
            if (menu.getItem(i).itemId == R.id.SignOut) {
                menu.getItem(i).isVisible = true
                menu.getItem(i).title = "Sign Out (" + MyApplication.userEmail + ")"
            } else if (menu.getItem(i).itemId == R.id.ChatFragment) {
                when (thereAreUnreadMessages()) {
                    -1 -> {
                        menu.getItem(i).isVisible = false
                    }
                    0 -> {
/*                        menu.getItem(i).icon = null
                        menu.getItem(i).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                        menu.getItem(i).isVisible = true */
                        menu.getItem(i).isVisible = false
                    }
                    1 -> {
                        menu.getItem(i).icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_fas_envelope)
                        menu.getItem(i).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                        menu.getItem(i).isVisible = true
                    }
                }
            } else
                menu.getItem(i).isVisible = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.SignOut) {
            Log.d("Alex", "sign out attempted")
            BudgetViewModel.clear()
            CategoryViewModel.clear()
            DefaultsViewModel.clear()
            ExpenditureViewModel.clear()
            RecurringTransactionViewModel.clear()
            SpenderViewModel.clear()
            Firebase.auth.signOut()
            mGoogleSignInClient.signOut()
            MyApplication.userUID = ""
            requireActivity().invalidateOptionsMenu()
//            (activity as MainActivity).setDrawerMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            (activity as MainActivity).setLoggedOutMode(true)
            binding.signInButton.visibility = View.VISIBLE
            binding.signInButton.setSize(SignInButton.SIZE_WIDE)
            binding.quoteField.text = ""
            binding.expenditureButton.visibility = View.GONE
            binding.viewAllButton.visibility = View.GONE
            binding.dashboardButton.visibility = View.GONE
            MyApplication.haveLoadedDataForThisUser = false
            return true
        } else {
            val navController = findNavController()
            return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
        }
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
            }
        }
        val dbRef =
            MyApplication.databaseref.child("Users/" + MyApplication.userUID)
                .child("Info")
                .child("LastReadChats")
        dbRef.addListenerForSingleValueEvent(infoListener)
    }

    fun tryToUpdateChatIcon() {
        // this is called when lastSignedIn date/time are found, and also when chats are loaded.  When both are done then do something
        if (ChatViewModel.getCount() > 0 && MyApplication.lastReadChatsDate != "") {
            Log.d("Alex", "should be updating chat icon now")
            activity?.invalidateOptionsMenu()
        }
    }

    fun thereAreUnreadMessages() : Int {
        if (ChatViewModel.getCount() > 0 && MyApplication.lastReadChatsDate != "") {
            val tChat = ChatViewModel.getLastChat()
            Log.d("Alex", MyApplication.lastReadChatsDate + " " + MyApplication.lastReadChatsTime + " " + tChat.date + " " + tChat.time)
            if (MyApplication.lastReadChatsDate < tChat.date ||
                (MyApplication.lastReadChatsDate == tChat.date && MyApplication.lastReadChatsTime < tChat.time))
                    return 1 // there are unread chats
            else
                return 0 // there are no unread chats
        } else
            return -1    // don't show icon
    }

    override fun onDestroy() {
        super.onDestroy()
        CategoryViewModel.singleInstance.clearCallback()
        SpenderViewModel.singleInstance.clearCallback()
        _binding = null
    }
}

