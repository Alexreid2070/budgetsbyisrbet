package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
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
    private var gestureDetector: GestureDetectorCompat? = null
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Inflate the layout for this fragment - DON'T seem to need this inflate.  In fact, if I call it, it'll call Main's onCreateView multiple times
//        inflater.inflate(R.layout.fragment_home, container, false)

        if (inDarkMode(requireContext()))
            binding.constraintLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.black))

        gestureDetector = GestureDetectorCompat(requireActivity(), object :
            GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                event1: MotionEvent?,
                event2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (event1 != null) {
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
                }
//                }
                return true
            }
        })

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

        Glide.with(requireContext()).load(MyApplication.userPhotoURL)
            .thumbnail(0.5f)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(binding.imgProfilePic)

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
            expandTop()
        }
        // this next block allows the floating action button to move up and down (it starts constrained to bottom)
        val set = ConstraintSet()
        val constraintLayout = binding.constraintLayout
        set.clone(constraintLayout)
        set.clear(R.id.budget_add_fab, ConstraintSet.TOP)
        set.applyTo(constraintLayout)

        if (MyApplication.adminMode)
            binding.adminButton.visibility = View.VISIBLE
        else
            binding.adminButton.visibility = View.GONE

        startLoad()
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
        val defaultObserver = Observer<Boolean> {
            if (MyApplication.amCurrentlyImpersonating()) {
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
        DefaultsViewModel.observeDefaults(this, defaultObserver)
        val catListObserver = Observer<MutableList<Category>> {
            alignPageWithDataState("CategoryViewModel")
        }
        CategoryViewModel.observeList(this, catListObserver)
        val spenderListObserver = Observer<MutableList<Spender>> {
            (activity as MainActivity).multipleUserMode(SpenderViewModel.multipleUsers())
            alignPageWithDataState("SpenderViewModel")
        }
        SpenderViewModel.observeList(this, spenderListObserver)
        val hintListObserver = Observer<MutableList<Hint>> {
            alignPageWithDataState("HintViewModel")
        }
        HintViewModel.observeList(this, hintListObserver)
        val transactionListObserver = Observer<MutableList<Transaction>> {
            alignPageWithDataState("TransactionViewModel")
        }
        TransactionViewModel.observeList(this, transactionListObserver)
        val budListObserver = Observer<MutableList<Budget>> {
            alignPageWithDataState("BudgetViewModel")
        }
        BudgetViewModel.observeList(this, budListObserver)
        val spListObserver = Observer<MutableList<ScheduledPayment>> {
            (activity as MainActivity).multipleUserMode(SpenderViewModel.multipleUsers())
            alignPageWithDataState("SpenderViewModel")
            setScheduledPaymentText()
        }
        ScheduledPaymentViewModel.observeList(this, spListObserver)
        val retListObserver = Observer<MutableList<RetirementData>> {
            alignPageWithDataState("RetirementViewModel")
        }
        RetirementViewModel.observeList(this, retListObserver)
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
        return if (MyApplication.amCurrentlyImpersonating())
            "Currently impersonating " + MyApplication.currentUserEmail
        else
            MyApplication.getQuote()
    }

    private fun startLoad() {
        if (DefaultsViewModel.isLoaded() && DefaultsViewModel.getDefaultQuote()) {
            binding.quoteField.text = getQuote()
        }
        if (!MyApplication.haveLoadedDataForThisUser) {
            // check if I should load my own UID, or if I'm a JoinUser
            val joinListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.value != null) {  // found the JoinUser node
                        MyApplication.userUID = dataSnapshot.value.toString()
                    }
                    (activity as MainActivity).loadEverything()
                    setupDataCallbacks()
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
        alignPageWithDataState("end of OVC")
    }

    private fun alignPageWithDataState(iTag: String)  {
        Timber.tag("Alex").d("alignpage: $iTag userUID ${MyApplication.userUID} ${CategoryViewModel.isLoaded()} ${SpenderViewModel.isLoaded()} " +
                "${ScheduledPaymentViewModel.isLoaded()} ${TransactionViewModel.isLoaded()} " +
                "${BudgetViewModel.isLoaded()} ${DefaultsViewModel.isLoaded()} " +
                "${HintViewModel.isLoaded()} ${RetirementViewModel.isLoaded()}")
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
                binding.homeScreenMessage.text = ""
                binding.homeScreenMessage.visibility = View.GONE
                val trackerFragment: TrackerFragment =
                    childFragmentManager.findFragmentById(R.id.home_tracker_fragment) as TrackerFragment
                trackerFragment.initCurrentBudgetMonth()
                launch {
                    trackerFragment.loadBarChart()
                }
                HintViewModel.showHint(parentFragmentManager, cHINT_HOME)
            }
        } else {
            (activity as MainActivity).setLoggedOutMode(true)
            binding.expandButton.isEnabled = false
        }
    }

    @Suppress("HardCodedStringLiteral")
    private fun setupNewUser() {
        AppUserViewModel.addUserKey()
        CategoryViewModel.updateCategory(0, "Housing", "Hydro", cDiscTypeNondiscretionary,2, true,false, false)
        CategoryViewModel.updateCategory(0, "Housing", "Insurance", cDiscTypeNondiscretionary,2, true, false, false)
        CategoryViewModel.updateCategory(0, "Housing", "Internet", cDiscTypeNondiscretionary,2, true,false, false)
        CategoryViewModel.updateCategory(0, "Housing", "Maintenance", cDiscTypeDiscretionary,2, true,false, false)
        CategoryViewModel.updateCategory(0, "Housing", "Mortgage", cDiscTypeNondiscretionary,2, true,false, false)
        CategoryViewModel.updateCategory(0, "Housing", "Property Taxes", cDiscTypeNondiscretionary, 2, true,false, false)
        CategoryViewModel.updateCategory(0, "Housing", "Rent", cDiscTypeNondiscretionary,2, true,false, false)
        CategoryViewModel.updateCategory(0, "Life", "Cellphone", cDiscTypeDiscretionary,2, true,false, false)
        CategoryViewModel.updateCategory(0, "Life", "Charity & Gifts", cDiscTypeDiscretionary,2, true,false, false)
        CategoryViewModel.updateCategory(0, "Life", "Clothing", cDiscTypeDiscretionary,2, true,false, false)
        CategoryViewModel.updateCategory(0, "Life", "Entertainment", cDiscTypeDiscretionary,2, true,false, false)
        CategoryViewModel.updateCategory(0, "Life", "Fitness", cDiscTypeDiscretionary,2, true,false, false)
        CategoryViewModel.updateCategory(0, "Life", "Groceries", cDiscTypeDiscretionary,2, true,false, false)
        CategoryViewModel.updateCategory(0, "Life", "Health & Dental", cDiscTypeDiscretionary,2, true,false, false)
        CategoryViewModel.updateCategory(0, "Life", "Hobbies", cDiscTypeDiscretionary,2, true,false, false)
        CategoryViewModel.updateCategory(0, "Life", "Home", cDiscTypeDiscretionary,2, true,false, false)
        CategoryViewModel.updateCategory(0, "Life", "Personal Care", cDiscTypeDiscretionary,2, true,false, false)
        CategoryViewModel.updateCategory(0, "Life", "Restaurants", cDiscTypeDiscretionary,2, true,false, false)
        CategoryViewModel.updateCategory(0, "Life", "Travel", cDiscTypeDiscretionary,2, true,false, false)
        CategoryViewModel.updateCategory(0, "Transportation", "Car Payment", cDiscTypeNondiscretionary,2, true, false, false)
        CategoryViewModel.updateCategory(0, "Transportation", "Gas", cDiscTypeDiscretionary,2, true,false, false)
        CategoryViewModel.updateCategory(0, "Transportation", "Insurance", cDiscTypeNondiscretionary,2, true,false, false)
        CategoryViewModel.updateCategory(0, "Transportation", "Maintenance", cDiscTypeDiscretionary,2, true,false, false)
        CategoryViewModel.updateCategory(0, "Transportation", "Miscellaneous", cDiscTypeDiscretionary,2, true,false, false)
        DefaultsViewModel.setCategoryColour("Housing", -12400683, false)
        DefaultsViewModel.setCategoryColour("Life", -1072612, false)
        DefaultsViewModel.setCategoryColour("Transportation", -3751917, false)
        DefaultsViewModel.setCategoryPriority("Housing", 0, false)
        DefaultsViewModel.setCategoryPriority("Life", 1, false)
        DefaultsViewModel.setCategoryPriority("Transportation", 2, false)
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
        (activity as MainActivity).mGoogleSignInClient.signOut()
        MyApplication.userUID = ""
        MyApplication.currentUserEmail = ""
        MyApplication.userFamilyName = ""
        MyApplication.userPhotoURL = ""
        MyApplication.adminMode = false
        (activity as MainActivity).setLoggedOutMode(true)
        MyApplication.haveLoadedDataForThisUser = false
        findNavController().navigate(R.id.SignInFragment)
    }

    override fun onDestroy() {
        Timber.tag("Alex").d("onDestroy homeFragment")
        super.onDestroy()
        _binding = null
    }
}