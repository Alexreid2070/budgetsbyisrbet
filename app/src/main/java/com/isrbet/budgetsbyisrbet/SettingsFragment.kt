package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import com.google.android.material.color.MaterialColors
import com.google.android.material.slider.Slider
import com.isrbet.budgetsbyisrbet.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private var oldFirstUser: String = ""
    private var oldSecondUser: String = ""
    private var gestureDetector: GestureDetectorCompat? = null
    private var secondSwipeUp = 0
    private var authorizeButtonStartingState = false
    private var joinOtherUserButtonStartingState = false
    private var disconnectButtonStartingState = false
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_top)
        returnTransition = null
//        exitTransition = inflater.inflateTransition(R.transition.slide_bottom)
        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    Log.d("Alex", "Permission is granted")
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                    Log.d("Alex", "Feature won't be available, loser.")
                }
            }
    }


    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val spenderOne = SpenderViewModel.getSpender(0)
        val spenderTwo = SpenderViewModel.getSpender(1)
        if (spenderOne == null)
            binding.settingsFirstUserName.setText(MyApplication.userGivenName)
        else
            binding.settingsFirstUserName.setText(spenderOne.name)
        binding.firstUserEmail.text = spenderOne?.email
        if (spenderTwo != null) {
            if (spenderTwo.isActive == 1) {
                binding.switchSecondUserActive.isChecked = spenderTwo.isActive == 1
                authorizeButtonStartingState = spenderTwo.isActive == 1
                binding.settingsSecondUserName.setText(spenderTwo.name)
                binding.secondUserEmail.setText(spenderTwo.email)
                if (iAmPrimaryUser()) {
                    binding.switchSecondUserLayout.visibility = View.VISIBLE
                    binding.switchJoinOtherUserLayout.visibility = View.GONE
                    binding.authorizationKey.text = MyApplication.userUID
                    if (binding.firstUserEmail.text.toString() != binding.secondUserEmail.text.toString())
                        binding.shareUIDLayout.visibility = View.VISIBLE
                } else {
                    binding.switchJoinOtherUserLayout.visibility = View.GONE
                    binding.switchSecondUserLayout.visibility = View.GONE
                    binding.shareUIDLayout.visibility = View.GONE
                    binding.settingsFirstUserName.isEnabled = false
                    binding.secondUserEmail.isEnabled = false
                    binding.switchDisconnectLayout.visibility = View.VISIBLE
                }
            } else {
                binding.switchSecondUserActive.isChecked = false
                authorizeButtonStartingState = false
                binding.settingsSecondUserName.setText("")
            }
        }

        binding.splitSlider.value = SpenderViewModel.getSpenderSplit(0).toFloat()
        binding.splitName1.text = SpenderViewModel.getSpenderName(0)
        binding.splitName2.text = SpenderViewModel.getSpenderName(1)
        binding.splitName1Pct.text = SpenderViewModel.getSpenderSplit(0).toString()
        binding.splitName2Pct.text = SpenderViewModel.getSpenderSplit(1).toString()
        binding.splitSlider.addOnChangeListener { _, _, _ ->
            binding.splitName1Pct.text = binding.splitSlider.value.toInt().toString()
            binding.splitName2Pct.text = (100-binding.splitSlider.value.toInt()).toString()
        }

        var ctr = 400
        val spenderRadioGroup = binding.defaultSpenderRadioGroup
        spenderRadioGroup.removeAllViews()

        val defSpenderName = SpenderViewModel.getDefaultSpenderName()
        for (i in 0..2) { // always do this twice, so we will setup a possible new second user
            var spender: String
            if (i == 0)
                spender = SpenderViewModel.getSpenderName(0)
            else if (i == 1) {
                if (SpenderViewModel.getTotalCount() > 1)
                    spender = SpenderViewModel.getSpenderName(1)
                else {
                    spender = "New User"
                    binding.splitName2Pct.text = "New User"
                }
            } else {
                spender = "Joint"
            }
            val newRadioButton = RadioButton(requireContext())
            newRadioButton.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            newRadioButton.buttonTintList=
                ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
            newRadioButton.text = spender
            newRadioButton.id = ctr++
            spenderRadioGroup.addView(newRadioButton)
            if (spender == defSpenderName || defSpenderName == "") {
                spenderRadioGroup.check(newRadioButton.id)
            }
        }

        oldFirstUser = spenderOne?.name.toString()
        oldSecondUser = spenderTwo?.name.toString()

        inflater.inflate(R.layout.fragment_settings, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        if (binding.switchSecondUserActive.isChecked) {
            binding.secondUserLayout.visibility = View.VISIBLE
            binding.splitSliderLayout.visibility = View.VISIBLE
            binding.splitLayout.visibility = View.VISIBLE
            binding.spenderLayout.visibility = View.VISIBLE
        } else {
            binding.secondUserLayout.visibility = View.GONE
            binding.splitSliderLayout.visibility = View.GONE
            binding.splitLayout.visibility = View.GONE
            binding.spenderLayout.visibility = View.GONE
        }

        val arrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            CategoryViewModel.getCategoryAndSubcategoryList()
        )
        binding.settingsCategorySpinner.adapter = arrayAdapter
        binding.settingsCategorySpinner.setSelection(arrayAdapter.getPosition(DefaultsViewModel.getDefault(
            cDEFAULT_FULLCATEGORYNAME)))
        arrayAdapter.notifyDataSetChanged()
        val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), "1F")
        binding.settingsCategorySpinner.setBackgroundColor(Color.parseColor(hexColor))
        binding.settingsCategorySpinner.setPopupBackgroundResource(R.drawable.spinner)
        binding.switchSound.isChecked = DefaultsViewModel.getDefault(cDEFAULT_SOUND) != "Off"
        binding.switchQuote.isChecked = DefaultsViewModel.getDefault(cDEFAULT_QUOTE) != "Off"
        if (DefaultsViewModel.getDefault(cDEFAULT_INTEGRATEWITHTDSPEND) == "Off")
            binding.switchIntegrateWithTD.isChecked = false
        else {
            val isGranted = isNotificationServiceEnabled(requireContext())
            if (!isGranted) {
                binding.switchIntegrateWithTD.isChecked = false
                DefaultsViewModel.updateDefault(cDEFAULT_INTEGRATEWITHTDSPEND, "Off")
            } else
                binding.switchIntegrateWithTD.isChecked = true
        }
        if (binding.switchIntegrateWithTD.isChecked)
            binding.manageTranslationsLayout.visibility = View.VISIBLE
        else
            binding.manageTranslationsLayout.visibility = View.GONE
        binding.redPercentageSlider.value = DefaultsViewModel.getDefault(cDEFAULT_SHOWRED).toFloat()
        binding.redPercentage.text = DefaultsViewModel.getDefault(cDEFAULT_SHOWRED).toFloat().toInt().toString()

        if (binding.settingsSecondUserName.text.toString() == "") {
            binding.splitSlider.isEnabled = false
            for (i in 0 until binding.defaultSpenderRadioGroup.childCount) {
                (binding.defaultSpenderRadioGroup.getChildAt(i) as RadioButton).isEnabled = false
            }
        }
        gestureDetector = GestureDetectorCompat(requireActivity(), object:
            GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                event1: MotionEvent,
                event2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (event2.y < event1.y) { // swiped up.  If at bottom already, close Settings
                    Log.d("Alex", "swiped up " + binding.scrollView.canScrollVertically(1))
                    if (!binding.scrollView.canScrollVertically(1)) { // ie can't scroll up anymore
                        secondSwipeUp++
                        if (secondSwipeUp == 2)
                            activity?.onBackPressed()
                    }
                }
                else
                    secondSwipeUp = 0
                return true
            }
        })
        binding.scrollView.setOnTouchListener(object: View.OnTouchListener {
            override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
                if (p1 != null) {
                    try {
                        // this call is in a "try" because  sometimes it crashes.  So we want to ignore that gesture
                        gestureDetector?.onTouchEvent(p1)
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                        return false
                    }
                }
                return false
            }
        })

        binding.settingsSaveButton.setOnClickListener {
            onSaveButtonClicked()
        }
        binding.settingsCancelButton.setOnClickListener {
            activity?.onBackPressed()
        }
        binding.copyButton.setOnClickListener {
            requireContext().copyToClipboard("Auth key", binding.authorizationKey.text.toString())
            Toast.makeText(activity, "Authorization key has been copied to your clipboard", Toast.LENGTH_SHORT).show()
        }
        binding.expandCategories.setOnClickListener {
            findNavController().navigate(R.id.CategoryFragment)
        }
        binding.expandBudgets.setOnClickListener {
            findNavController().navigate(R.id.BudgetViewAllFragment)
        }
        binding.expandRecurringTransactions.setOnClickListener {
            findNavController().navigate(R.id.RecurringTransactionFragment)
        }

        binding.buttonEditTranslations.setOnClickListener {
            findNavController().navigate(R.id.ViewTranslationsFragment)
        }
        binding.switchSecondUserActive.setOnCheckedChangeListener { _, _ ->
            if (authorizeButtonStartingState != binding.switchSecondUserActive.isChecked) {
                binding.settingsSaveButton.visibility = View.VISIBLE
                binding.settingsCancelButton.visibility = View.VISIBLE
            }
            if (binding.switchSecondUserActive.isChecked) {
                binding.authorizationKeyLayout.visibility = View.VISIBLE
                binding.secondUserLayout.visibility = View.VISIBLE
                binding.splitSliderLayout.visibility = View.VISIBLE
                binding.splitLayout.visibility = View.VISIBLE
                binding.spenderLayout.visibility = View.VISIBLE
                binding.splitSlider.value = 50.0F
                binding.switchJoinOtherUserLayout.visibility = View.GONE
//                if (binding.firstUserEmail.text.toString() != binding.secondUserEmail.text.toString())
//                    binding.shareUIDLayout.visibility = View.VISIBLE
            } else {
                binding.secondUserLayout.visibility = View.GONE
                binding.shareUIDLayout.visibility = View.GONE
                binding.splitSliderLayout.visibility = View.GONE
                binding.splitLayout.visibility = View.GONE
                binding.spenderLayout.visibility = View.GONE
                binding.switchJoinOtherUserLayout.visibility = View.VISIBLE
/*                if (!binding.switchJoinOtherUser.isChecked) {
                    // update default on screen field
                    val id = binding.defaultSpenderRadioGroup.getChildAt(0).id
                    binding.defaultSpenderRadioGroup.check(id)

                    // update default in db
                    DefaultsViewModel.updateDefault(cDEFAULT_SPENDER,"0")
                }*/
            }
        }
        binding.switchJoinOtherUser.setOnCheckedChangeListener { _, _ ->
            if (joinOtherUserButtonStartingState != binding.switchJoinOtherUser.isChecked) {
                binding.settingsSaveButton.visibility = View.VISIBLE
                binding.settingsCancelButton.visibility = View.VISIBLE
            }
            if (binding.switchJoinOtherUser.isChecked) {
                binding.firstNameLayout.visibility = View.GONE
                binding.secondUserLayout.visibility = View.GONE
                binding.splitSliderLayout.visibility = View.GONE
                binding.splitLayout.visibility = View.GONE
                binding.spenderLayout.visibility = View.GONE
                binding.switchSecondUserLayout.visibility = View.GONE
                binding.shareUIDLayout.visibility = View.GONE
                binding.settingsCategorySpinnerLayout.visibility = View.GONE
                binding.settingsIntegrationWithTdLayout.visibility = View.GONE
                binding.manageTranslationsLayout.visibility = View.GONE
                binding.settingsSoundEffectsLayout.visibility = View.GONE
                binding.settingsQuoteLayout.visibility = View.GONE
                binding.settingsRedPercentageLayout.visibility = View.GONE
                binding.uidLayout.visibility = View.VISIBLE
                binding.secondUserEmail.setText("")
                binding.settingsSecondUserName.setText("")
            } else {
                binding.firstNameLayout.visibility = View.VISIBLE
                binding.secondUserLayout.visibility = View.GONE
                binding.splitSliderLayout.visibility = View.GONE
                binding.splitLayout.visibility = View.GONE
                binding.spenderLayout.visibility = View.GONE
                binding.switchSecondUserLayout.visibility = View.VISIBLE
//                binding.shareUIDLayout.visibility = View.VISIBLE
                binding.authorizationKey.text = MyApplication.userUID
                binding.settingsCategorySpinnerLayout.visibility = View.VISIBLE
                binding.settingsIntegrationWithTdLayout.visibility = View.VISIBLE
                if (binding.switchIntegrateWithTD.isChecked) {
                    binding.manageTranslationsLayout.visibility = View.VISIBLE
                } else {
                    binding.manageTranslationsLayout.visibility = View.GONE
                }
                binding.settingsSoundEffectsLayout.visibility = View.VISIBLE
                binding.settingsQuoteLayout.visibility = View.VISIBLE
                binding.settingsRedPercentageLayout.visibility = View.VISIBLE
                binding.uidLayout.visibility = View.GONE
                binding.joinUid.setText("")
            }
        }
        binding.switchDisconnect.setOnCheckedChangeListener { _, _ ->
            if (disconnectButtonStartingState != binding.switchDisconnect.isChecked) {
                binding.settingsSaveButton.visibility = View.VISIBLE
                binding.settingsCancelButton.visibility = View.VISIBLE
            }
        }

        binding.redPercentageSlider.addOnChangeListener { _, _, _ ->
            binding.redPercentage.text = binding.redPercentageSlider.value.toInt().toString()
        }

        binding.settingsFirstUserName.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
                binding.settingsSaveButton.visibility = View.VISIBLE
                binding.settingsCancelButton.visibility = View.VISIBLE
                if (binding.settingsFirstUserName.text.toString() != "") {
                    for (i in 0 until binding.defaultSpenderRadioGroup.childCount) {
                        if (i == 0) { // ie the first user
                            (binding.defaultSpenderRadioGroup.getChildAt(i) as RadioButton).text =
                                binding.settingsFirstUserName.text
                            binding.splitName1.text = binding.settingsFirstUserName.text
                        }
                    }
                }
            }
            override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun afterTextChanged(arg0: Editable) {}
        })

        binding.settingsSecondUserName.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
                binding.settingsSaveButton.visibility = View.VISIBLE
                binding.settingsCancelButton.visibility = View.VISIBLE
                if (binding.settingsSecondUserName.text.toString() != "") {
                    binding.splitSlider.isEnabled = true

                    for (i in 0 until binding.defaultSpenderRadioGroup.childCount) {
                        if (i == 1) { // ie the second user
                            (binding.defaultSpenderRadioGroup.getChildAt(i) as RadioButton).text =
                                binding.settingsSecondUserName.text
                            binding.splitName2.text = binding.settingsSecondUserName.text
                        }
                        (binding.defaultSpenderRadioGroup.getChildAt(i) as RadioButton).isEnabled = true
                    }
                }
            }
            override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun afterTextChanged(arg0: Editable) {}
        })

        binding.secondUserEmail.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
                binding.settingsSaveButton.visibility = View.VISIBLE
                binding.settingsCancelButton.visibility = View.VISIBLE
            }
            override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun afterTextChanged(arg0: Editable) {}
        })

        binding.settingsCategorySpinner.onItemSelectedListener = object:
            AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val chosenCategory = binding.settingsCategorySpinner.selectedItem.toString()
                if (chosenCategory != DefaultsViewModel.getDefault((cDEFAULT_FULLCATEGORYNAME))) {
                    Log.d("Alex", "Changed category to $chosenCategory")

                    if (binding.settingsCategorySpinner.selectedItem != null) {
                        val defaultCategory = Category(0, binding.settingsCategorySpinner.selectedItem.toString())
                        if (defaultCategory.id.toString() != DefaultsViewModel.getDefault(cDEFAULT_CATEGORY_ID)) {
                            DefaultsViewModel.updateDefault(
                                cDEFAULT_CATEGORY_ID,
                                defaultCategory.id.toString()
                            )
                            MyApplication.playSound(context, R.raw.impact_jaw_breaker)
                        }
                    }
                }
            }
        }

        binding.defaultSpenderRadioGroup.setOnCheckedChangeListener { _, _ ->
            val selectedId = binding.defaultSpenderRadioGroup.checkedRadioButtonId
            val radioButton = requireActivity().findViewById(selectedId) as RadioButton
            val newSpender = radioButton.text.toString()
            if (newSpender != SpenderViewModel.getDefaultSpenderName()) {
                DefaultsViewModel.updateDefault(cDEFAULT_SPENDER, SpenderViewModel.getSpenderIndex(newSpender).toString())
                MyApplication.playSound(context, R.raw.impact_jaw_breaker)
            }
        }

        binding.splitSlider.addOnSliderTouchListener( object : Slider.OnSliderTouchListener {
            @SuppressLint("RestrictedApi")
            override fun onStartTrackingTouch(slider: Slider) {
                Log.d("Alex", "Initial value is " + slider.value)
            }

            @SuppressLint("RestrictedApi")
            override fun onStopTrackingTouch(slider: Slider) {
                if (binding.splitSlider.value.toInt() != SpenderViewModel.getSpenderSplit(0)) {
                    Log.d("Alex", "Ending value is " + slider.value)
                    SpenderViewModel.updateSpenderSplits(binding.splitSlider.value.toInt())
                    MyApplication.playSound(context, R.raw.impact_jaw_breaker)
                }
            }
        })

        binding.redPercentageSlider.addOnSliderTouchListener( object : Slider.OnSliderTouchListener {
            @SuppressLint("RestrictedApi")
            override fun onStartTrackingTouch(slider: Slider) {
                Log.d("Alex", "Initial split value is " + slider.value)
            }

            @SuppressLint("RestrictedApi")
            override fun onStopTrackingTouch(slider: Slider) {
                if (binding.redPercentageSlider.value.toInt() != DefaultsViewModel.getDefault(
                        cDEFAULT_SHOWRED).toInt()) {
                    Log.d("Alex", "Ending red value is " + slider.value)
                    DefaultsViewModel.updateDefault(cDEFAULT_SHOWRED, binding.redPercentageSlider.value.toInt().toString())
                    MyApplication.playSound(context, R.raw.impact_jaw_breaker)
                }
            }
        })

        binding.switchIntegrateWithTD.setOnCheckedChangeListener { _, _ ->
            if (binding.switchIntegrateWithTD.isChecked) {
                binding.manageTranslationsLayout.visibility = View.VISIBLE
                // adding request for permission
                if (isNotificationServiceEnabled(requireContext())) {
                        // You can use the API that requires the permission.
                        Log.d("Alex", "Success")
                    }
                else {
                    // You can directly ask for the permission.
                    // The registered ActivityResultCallback gets the result of this request.
                    Log.d("Alex", "In the else")
                    AlertDialog.Builder(requireContext())
                        .setTitle("Notification permission required")
                        .setMessage("In order for this On Budget app to access TD MySpend notifications, you must grant permission" +
                                " to do so in the following screen.  If you decide not to grant permission, this TD MySpend feature" +
                                "will not work.\n\nTo grant permissions, in the following screen click on On Budget, and then " +
                                "click Allow Notification Access.")
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                        binding.switchIntegrateWithTD.isChecked = false
                        }
                        .show()
                }
            } else {
                binding.manageTranslationsLayout.visibility = View.GONE
            }
            val integrateField = if (binding.switchIntegrateWithTD.isChecked) "On" else "Off"
            if (integrateField != DefaultsViewModel.getDefault(cDEFAULT_INTEGRATEWITHTDSPEND)) {
                Log.d("Alex", "td switch is now $integrateField")
                DefaultsViewModel.updateDefault(cDEFAULT_INTEGRATEWITHTDSPEND, integrateField)
                MyApplication.playSound(context, R.raw.impact_jaw_breaker)
            }
        }
        binding.switchSound.setOnCheckedChangeListener { _, _ ->
            val soundField = if (binding.switchSound.isChecked) "On" else "Off"
            if (soundField != DefaultsViewModel.getDefault(cDEFAULT_SOUND)) {
                Log.d("Alex", "sound is now $soundField")
                DefaultsViewModel.updateDefault(cDEFAULT_SOUND, soundField)
                MyApplication.playSound(context, R.raw.impact_jaw_breaker)
            }
        }
        binding.switchQuote.setOnCheckedChangeListener { _, _ ->
            val quoteField = if (binding.switchQuote.isChecked) "On" else "Off"
            if (quoteField != DefaultsViewModel.getDefault(cDEFAULT_QUOTE)) {
                Log.d("Alex", "quote is now $quoteField")
                DefaultsViewModel.updateDefault(cDEFAULT_QUOTE, quoteField)
                MyApplication.playSound(context, R.raw.impact_jaw_breaker)
            }
        }

        binding.settingsFirstUserName.requestFocus()
    }
    private fun isNotificationServiceEnabled(c: Context): Boolean {
        val pkgName: String = c.packageName
        val flat: String = Settings.Secure.getString(
            c.contentResolver,
            "enabled_notification_listeners"
        )
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":").toTypedArray()
            for (i in names.indices) {
                val cn = ComponentName.unflattenFromString(names[i])
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.packageName)) {
                        Log.d("Alex", "Notification permissions are granted")
                        return true
                    }
                }
            }
        }
        Log.d("Alex", "Notification permissions are NOT granted")
        return false
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onSaveButtonClicked () {
        if (!textIsAlphaOrSpace(binding.settingsFirstUserName.text.toString())) {
            binding.settingsFirstUserName.error = "The text contains non-alphabetic characters."
            focusAndOpenSoftKeyboard(requireContext(), binding.settingsFirstUserName)
            return
        }
        // need to reject if all the fields aren't entered correctly
        // first user cannot be blank
        if (binding.settingsFirstUserName.text.toString() == "") {
            binding.settingsFirstUserName.error = getString(R.string.missingFirstUserName)
            focusAndOpenSoftKeyboard(requireContext(), binding.settingsFirstUserName)
            return
        }
        var splitSliderValue = 100
        if (binding.switchSecondUserActive.isChecked) {
            if (binding.settingsSecondUserName.text.toString() == "") {
                binding.settingsSecondUserName.error = getString(R.string.missingSecondUserName)
                focusAndOpenSoftKeyboard(requireContext(), binding.settingsSecondUserName)
                return
            }
            if (!textIsAlphaOrSpace(binding.settingsSecondUserName.text.toString())) {
                binding.settingsSecondUserName.error =
                    "The text contains non-alphabetic characters."
                focusAndOpenSoftKeyboard(requireContext(), binding.settingsSecondUserName)
                return
            }
            if (!binding.secondUserEmail.text.toString().isEmailValid()) {
                binding.secondUserEmail.error =
                    "This is not a valid email address."
                focusAndOpenSoftKeyboard(requireContext(), binding.secondUserEmail)
                return
            }

            val defaultSpenderRadioGroup = binding.defaultSpenderRadioGroup
            val spenderCheckedID = defaultSpenderRadioGroup.checkedRadioButtonId
            if (spenderCheckedID == -1) { // must have been a name change
                val but = binding.defaultSpenderRadioGroup.getChildAt(0) as RadioButton
                but.error = getString(R.string.oneUserMustBeDefault)
                focusAndOpenSoftKeyboard(requireContext(), binding.settingsFirstUserName)
                return
            }
            splitSliderValue = binding.splitSlider.value.toInt()
        }
        if (binding.switchJoinOtherUser.isChecked) {
            if (binding.joinUid.text.toString() == "") {
                binding.joinUid.error =
                    "Authorization key must not be blank."
                focusAndOpenSoftKeyboard(requireContext(), binding.joinUid)
                return
            }
            splitSliderValue = binding.splitSlider.value.toInt()
        }
        // at this point the user information is valid
        // update existing Spenders to add new info

        if (binding.switchDisconnect.isChecked) {
            binding.switchDisconnectLayout.visibility = View.GONE
            binding.switchDisconnect.isChecked = false // prepare for next time
            binding.secondUserLayout.visibility = View.GONE
            binding.splitSliderLayout.visibility = View.GONE
            binding.splitLayout.visibility = View.GONE
            binding.spenderLayout.visibility = View.GONE
            binding.switchSecondUserLayout.visibility = View.GONE
            binding.shareUIDLayout.visibility = View.GONE
            binding.uidLayout.visibility = View.VISIBLE
            binding.secondUserEmail.setText("")
            binding.settingsSecondUserName.setText("")
            MyApplication.userUID = MyApplication.originalUserUID
            MyApplication.databaseref.child("Users/" + MyApplication.userUID)
                .child("Info")
                .child("JoinUser").removeValue()
            switchTo(MyApplication.userUID)
            Toast.makeText(activity, "Leaving other user.", Toast.LENGTH_SHORT).show()
        } else if (!binding.switchSecondUserActive.isChecked && !binding.switchJoinOtherUser.isChecked) { // ie single mode
            SpenderViewModel.updateSpender(0, Spender(binding.settingsFirstUserName.text.toString(), binding.firstUserEmail.text.toString(), 100, 1))
            DefaultsViewModel.updateDefault(cDEFAULT_SPENDER,"0")
            if (SpenderViewModel.getTotalCount() >1) {
                // single mode, so if previous other users were there, turn them off
                SpenderViewModel.updateSpender(1, Spender("Other", "", 0, 0))
                SpenderViewModel.getSpender(2)?.isActive = 0
                binding.settingsSecondUserName.setText("")
                binding.secondUserEmail.setText("")
            }
        } else if (binding.switchSecondUserActive.isChecked) {
            SpenderViewModel.updateSpender(0, Spender(binding.settingsFirstUserName.text.toString(), binding.firstUserEmail.text.toString(), splitSliderValue, 1))
            if (SpenderViewModel.getTotalCount() > 1) {
                SpenderViewModel.updateSpender(1, Spender(binding.settingsSecondUserName.text.toString(),
                        binding.secondUserEmail.text.toString(), 100 - splitSliderValue, 1))
            } else {
                SpenderViewModel.addSpender( 1, Spender(binding.settingsSecondUserName.text.toString(),
                        binding.secondUserEmail.text.toString(), 100 - splitSliderValue, 1))
                SpenderViewModel.addLocalSpender( Spender("Joint","", 0, 1))
            }
            if (binding.secondUserEmail.text.toString() == binding.firstUserEmail.text.toString())
                SpenderViewModel.removeSecondAllowedUser()
            else
                SpenderViewModel.saveAsSecondAllowedUser(binding.secondUserEmail.text.toString())

            if (binding.firstUserEmail.text.toString() != binding.secondUserEmail.text.toString()) {
                binding.authorizationKey.text = MyApplication.userUID
                binding.shareUIDLayout.visibility = View.VISIBLE
            }
        } else { // binding.switchJoinUser isChecked
            MyApplication.databaseref.child("Users/" + MyApplication.userUID)
                .child("Info")
                .child("JoinUser").setValue(binding.joinUid.text.toString())
            Toast.makeText(activity, "Joining user.", Toast.LENGTH_SHORT).show()
            switchTo(binding.joinUid.text.toString())
//            activity?.onBackPressed()
        }

        MyApplication.playSound(context, R.raw.impact_jaw_breaker)
        Toast.makeText(activity, "Changes saved", Toast.LENGTH_SHORT).show()
//        activity?.onBackPressed()
//        (activity as MainActivity).singleUserMode(!binding.switchSecondUserActive.isChecked)
        binding.settingsCancelButton.visibility = View.GONE
        binding.settingsSaveButton.visibility = View.GONE
    }
}