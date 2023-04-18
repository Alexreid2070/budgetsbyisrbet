package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
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
import timber.log.Timber

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private var oldFirstUser: String = ""
    private var oldSecondUser: String = ""
    private var gestureDetector: GestureDetectorCompat? = null
    private var secondSwipeUp = 0
    private var authorizeButtonStartingState = false
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
                    Timber.tag("Alex").d("Permission is granted")
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                    Timber.tag("Alex").d( "Feature won't be available.")
                }
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val spenderOne = SpenderViewModel.getSpender(0)
        val spenderTwo = SpenderViewModel.getSpender(1)
        if (spenderOne == null) {
            if (binding.settingsFirstUserName.text.toString() != MyApplication.userGivenName)
                binding.settingsFirstUserName.setText(MyApplication.userGivenName)
        } else {
            if (binding.settingsFirstUserName.text.toString() != spenderOne.name)
                binding.settingsFirstUserName.setText(spenderOne.name)
        }
        binding.firstUserEmail.text = spenderOne?.email
        if (spenderTwo != null) {
            if (spenderTwo.isActive == 1) {
                binding.switchSecondUserActive.isChecked = spenderTwo.isActive == 1
                authorizeButtonStartingState = spenderTwo.isActive == 1
                binding.settingsSecondUserName.setText(spenderTwo.name)
                binding.secondUserEmail.setText(spenderTwo.email)
                if (iAmPrimaryUser()) {
                    binding.switchSecondUserActive.visibility = View.VISIBLE
                    binding.switchJoinOtherUserLayout.visibility = View.GONE
                    binding.authorizationKey.text = MyApplication.userUID
                    if (binding.firstUserEmail.text.toString() != binding.secondUserEmail.text.toString()) {
                        binding.shareUIDLayout.visibility = View.VISIBLE
                        binding.shareKeyText.text = String.format(getString(R.string.share_this_authorization_key_with),binding.settingsSecondUserName.text)
                    }
                } else {
                    binding.switchJoinOtherUserLayout.visibility = View.GONE
                    binding.switchSecondUserActive.visibility = View.GONE
                    binding.shareUIDLayout.visibility = View.GONE
                    binding.settingsFirstUserName.isEnabled = false
                    binding.secondUserEmail.isEnabled = false
                    binding.switchDisconnect.visibility = View.VISIBLE
                }
            } else {
                binding.switchSecondUserActive.isChecked = false
                authorizeButtonStartingState = false
                binding.settingsSecondUserName.setText("")
            }
        }

        binding.splitSlider.value = (SpenderViewModel.getSpenderSplit(0) * 100).toFloat()
        binding.splitText.text = getSplitText((SpenderViewModel.getSpenderSplit(0) * 100).toInt(), "")
        binding.splitSlider.addOnChangeListener { _, _, _ ->
            binding.splitText.text = getSplitText(binding.splitSlider.value.toInt(), "")
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
                    spender = getString(R.string.new_user)
                    binding.splitText.text = getSplitText((SpenderViewModel.getSpenderSplit(0)*100).toInt(), "")
                }
            } else {
                spender = getString(R.string.joint)
            }
            val newRadioButton = RadioButton(requireContext())
            newRadioButton.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
//            newRadioButton.buttonTintList=
  //              ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
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
        binding.settingsCategorySpinner.setSelection(arrayAdapter.getPosition(DefaultsViewModel.getDefaultFullCategoryName()))
        arrayAdapter.notifyDataSetChanged()
        val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), cOpacity)
        binding.settingsCategorySpinner.setBackgroundColor(Color.parseColor(hexColor))
        binding.settingsCategorySpinner.setPopupBackgroundResource(R.drawable.spinner)
        binding.switchSound.isChecked = DefaultsViewModel.getDefaultSound()
        binding.switchQuote.isChecked = DefaultsViewModel.getDefaultQuote()
        binding.switchCurrency.isChecked = DefaultsViewModel.getDefaultShowCurrencySymbol()
        binding.spLookaheadText.text =
            String.format(getString(R.string.sp_lookahead), DefaultsViewModel.getDefaultSPLookahead())
        binding.spLookaheadSlider.value = DefaultsViewModel.getDefaultSPLookahead().toFloat()
        if (DefaultsViewModel.getDefaultIntegrateWithTDSpend()) {
            val isGranted = isNotificationServiceEnabled(requireContext())
            if (!isGranted) {
                binding.switchIntegrateWithTD.isChecked = false
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_INTEGRATEWITHTDSPEND, false)
            } else
                binding.switchIntegrateWithTD.isChecked = true
        } else {
            binding.switchIntegrateWithTD.isChecked = false
        }
        if (binding.switchIntegrateWithTD.isChecked)
            binding.manageTranslationsLayout.visibility = View.VISIBLE
        else
            binding.manageTranslationsLayout.visibility = View.GONE
        binding.redPercentageSlider.value = DefaultsViewModel.getDefaultShowRed().toFloat()
        binding.redPercentageText.text =
            String.format(getString(R.string.show_red_at), binding.redPercentageSlider.value.toInt())

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
                    if (!binding.scrollView.canScrollVertically(1)) { // ie can't scroll up anymore
                        secondSwipeUp++
                        if (secondSwipeUp == 2)
                            activity?.onBackPressedDispatcher?.onBackPressed()
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
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        binding.copyButton.setOnClickListener {
            requireContext().copyToClipboard("Auth key", binding.authorizationKey.text.toString())
            Toast.makeText(activity, getString(R.string.authorization_key_has_been_copied_to_your_clipboard), Toast.LENGTH_SHORT).show()
        }
        binding.buttonEditTranslations.setOnClickListener {
            findNavController().navigate(R.id.ViewTranslationsFragment)
        }
        binding.switchSecondUserActive.setOnCheckedChangeListener { _, _ ->
//            if (authorizeButtonStartingState != binding.switchSecondUserActive.isChecked) {
                binding.settingsSaveButton.visibility = View.VISIBLE
                binding.settingsCancelButton.visibility = View.VISIBLE
//            }
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
//                binding.switchJoinOtherUserLayout.visibility = View.VISIBLE
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
//            if (joinOtherUserButtonStartingState != binding.switchJoinOtherUser.isChecked) {
                binding.settingsSaveButton.visibility = View.VISIBLE
                binding.settingsCancelButton.visibility = View.VISIBLE
//            }
            if (binding.switchJoinOtherUser.isChecked) {
                binding.firstNameLayout.visibility = View.GONE
                binding.secondUserLayout.visibility = View.GONE
                binding.splitSliderLayout.visibility = View.GONE
                binding.splitLayout.visibility = View.GONE
                binding.spenderLayout.visibility = View.GONE
                binding.switchSecondUserActive.visibility = View.GONE
                binding.shareUIDLayout.visibility = View.GONE
                binding.settingsCategorySpinnerLayout.visibility = View.GONE
                binding.switchIntegrateWithTD.visibility = View.GONE
                binding.manageTranslationsLayout.visibility = View.GONE
                binding.switchSound.visibility = View.GONE
                binding.switchQuote.visibility = View.GONE
                binding.settingsRedPercentageLayout.visibility = View.GONE
                binding.switchCurrency.visibility = View.GONE
                binding.settingsSpLookaheadLayout.visibility = View.GONE
                binding.line1.visibility = View.GONE
                binding.line2.visibility = View.GONE
                binding.line3.visibility = View.GONE
                binding.line4.visibility = View.GONE
                binding.line5.visibility = View.GONE
                binding.uidLayout.visibility = View.VISIBLE
                binding.secondUserEmail.setText("")
                binding.settingsSecondUserName.setText("")
            } else {
                binding.firstNameLayout.visibility = View.VISIBLE
                binding.secondUserLayout.visibility = View.GONE
                binding.splitSliderLayout.visibility = View.GONE
                binding.splitLayout.visibility = View.GONE
                binding.spenderLayout.visibility = View.GONE
                binding.switchSecondUserActive.visibility = View.VISIBLE
//                binding.shareUIDLayout.visibility = View.VISIBLE
                binding.authorizationKey.text = MyApplication.userUID
                binding.settingsCategorySpinnerLayout.visibility = View.VISIBLE
                binding.switchIntegrateWithTD.visibility = View.VISIBLE
                if (binding.switchIntegrateWithTD.isChecked) {
                    binding.manageTranslationsLayout.visibility = View.VISIBLE
                } else {
                    binding.manageTranslationsLayout.visibility = View.GONE
                }
                binding.switchSound.visibility = View.VISIBLE
                binding.switchQuote.visibility = View.VISIBLE
                binding.settingsRedPercentageLayout.visibility = View.VISIBLE
                binding.switchCurrency.visibility = View.VISIBLE
                binding.settingsSpLookaheadLayout.visibility = View.VISIBLE
                binding.line1.visibility = View.VISIBLE
                binding.line2.visibility = View.VISIBLE
                binding.line3.visibility = View.VISIBLE
                binding.line4.visibility = View.VISIBLE
                binding.line5.visibility = View.VISIBLE
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
            binding.redPercentageText.text =
                String.format(getString(R.string.show_red_at), binding.redPercentageSlider.value.toInt())
        }
        binding.spLookaheadSlider.addOnChangeListener { _, _, _ ->
            binding.spLookaheadText.text =
                String.format(getString(R.string.sp_lookahead), binding.spLookaheadSlider.value.toInt())
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
                            binding.splitText.text = getSplitText((SpenderViewModel.getSpenderSplit(0)*100).toInt(), "")
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
                            binding.splitText.text = getSplitText((SpenderViewModel.getSpenderSplit(0)*100).toInt(), "")
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
                if (chosenCategory != DefaultsViewModel.getDefaultFullCategoryName()) {
                    if (binding.settingsCategorySpinner.selectedItem != null) {
                        val defaultCategory = Category(0, binding.settingsCategorySpinner.selectedItem.toString())
                        if (defaultCategory.id != DefaultsViewModel.getDefaultCategory()) {
                            DefaultsViewModel.updateDefaultInt(
                                cDEFAULT_CATEGORY_ID,
                                defaultCategory.id
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
                DefaultsViewModel.updateDefaultInt(cDEFAULT_SPENDER, SpenderViewModel.getSpenderIndex(newSpender))
                MyApplication.playSound(context, R.raw.impact_jaw_breaker)
            }
        }

            binding.splitSlider.addOnSliderTouchListener( object : Slider.OnSliderTouchListener {
            @SuppressLint("RestrictedApi")
            override fun onStartTrackingTouch(slider: Slider) {
//                Log.d("Alex", "Initial value is " + slider.value)
            }

            @SuppressLint("RestrictedApi")
            override fun onStopTrackingTouch(slider: Slider) {
                if (binding.splitSlider.value.toInt() != (SpenderViewModel.getSpenderSplit(0)*100).toInt()) {
                    SpenderViewModel.updateSpenderSplits(binding.splitSlider.value.toInt())
                    MyApplication.playSound(context, R.raw.impact_jaw_breaker)
                }
            }
        })

        binding.redPercentageSlider.addOnSliderTouchListener( object : Slider.OnSliderTouchListener {
            @SuppressLint("RestrictedApi")
            override fun onStartTrackingTouch(slider: Slider) {
//                Log.d("Alex", "Initial split value is " + slider.value)
            }

            @SuppressLint("RestrictedApi")
            override fun onStopTrackingTouch(slider: Slider) {
                if (binding.redPercentageSlider.value.toInt() != DefaultsViewModel.getDefaultShowRed()) {
                    DefaultsViewModel.updateDefaultInt(cDEFAULT_SHOWRED, binding.redPercentageSlider.value.toInt())
                    MyApplication.playSound(context, R.raw.impact_jaw_breaker)
                }
            }
        })

        binding.spLookaheadSlider.addOnSliderTouchListener( object : Slider.OnSliderTouchListener {
            @SuppressLint("RestrictedApi")
            override fun onStartTrackingTouch(slider: Slider) {
            }

            @SuppressLint("RestrictedApi")
            override fun onStopTrackingTouch(slider: Slider) {
                if (binding.spLookaheadSlider.value.toInt() != DefaultsViewModel.getDefaultSPLookahead()) {
                    DefaultsViewModel.updateDefaultInt(cDEFAULT_SP_LOOKAHEAD, binding.spLookaheadSlider.value.toInt())
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
                        Timber.tag("Alex").d("Success")
                    }
                else {
                    // You can directly ask for the permission.
                    // The registered ActivityResultCallback gets the result of this request.
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
            if (binding.switchIntegrateWithTD.isChecked != DefaultsViewModel.getDefaultIntegrateWithTDSpend()) {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_INTEGRATEWITHTDSPEND, binding.switchIntegrateWithTD.isChecked)
                MyApplication.playSound(context, R.raw.impact_jaw_breaker)
            }
        }
        binding.switchSound.setOnCheckedChangeListener { _, _ ->
            if (binding.switchSound.isChecked != DefaultsViewModel.getDefaultSound()) {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_SOUND, binding.switchSound.isChecked)
                MyApplication.playSound(context, R.raw.impact_jaw_breaker)
            }
        }
        binding.switchQuote.setOnCheckedChangeListener { _, _ ->
            if (binding.switchQuote.isChecked != DefaultsViewModel.getDefaultQuote()) {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_QUOTE, binding.switchQuote.isChecked)
                MyApplication.playSound(context, R.raw.impact_jaw_breaker)
            }
        }
        binding.switchCurrency.setOnCheckedChangeListener { _, _ ->
            if (binding.switchCurrency.isChecked != DefaultsViewModel.getDefaultShowCurrencySymbol()) {
                DefaultsViewModel.updateDefaultBoolean(cDEFAULT_SHOW_CURRENCY_SYMBOL, binding.switchCurrency.isChecked)
                MyApplication.playSound(context, R.raw.impact_jaw_breaker)
            }
        }

        binding.languageRadioGroup.setOnCheckedChangeListener { _, _ ->
            val userChosenLanguage = if (binding.buttonFrench.isChecked)
                getString(R.string.french)
            else
                getString(R.string.english)
//            val selectedId = binding.languageRadioGroup.checkedRadioButtonId
 //           val radioButton = requireActivity().findViewById(selectedId) as RadioButton
   //         val userChosenLanguage = radioButton.text.toString()
            val currentSavedLanguage = MyApplication.prefs.getString("lang", null)
            if ((userChosenLanguage == getString(R.string.english) &&
                currentSavedLanguage != "en-US") ||
                (userChosenLanguage == getString(R.string.french) &&
                        currentSavedLanguage != "fr-CA")) {
                if (userChosenLanguage == getString(R.string.french)) {
                    MyApplication.prefEditor.putString("lang", "fr-CA")
                } else {
                    MyApplication.prefEditor.putString("lang", "en-US")
                }
                MyApplication.prefEditor.commit()
                Toast.makeText(
                    activity,
                    getString(R.string.language_has_been_changed_please_restart),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val lang = MyApplication.prefs.getString("lang", null)
        if (lang == "fr-CA")
            binding.buttonFrench.isChecked = true
        else
            binding.buttonEnglish.isChecked = true

        binding.settingsFirstUserName.requestFocus()
        HintViewModel.showHint(parentFragmentManager, cHINT_PREFERENCES)
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
//                        Log.d("Alex", "Notification permissions are granted")
                        return true
                    }
                }
            }
        }
//        Log.d("Alex", "Notification permissions are NOT granted")
        return false
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onSaveButtonClicked () {
        if (!textIsAlphaOrSpace(binding.settingsFirstUserName.text.toString())) {
            binding.settingsFirstUserName.error = getString(R.string.the_text_contains_non_alphabetic_characters)
            focusAndOpenSoftKeyboard(requireContext(), binding.settingsFirstUserName)
            return
        }
        // need to reject if all the fields aren't entered correctly
        // first user cannot be blank
        if (binding.settingsFirstUserName.text.toString() == "") {
            binding.settingsFirstUserName.error = getString(R.string.value_cannot_be_blank)
            focusAndOpenSoftKeyboard(requireContext(), binding.settingsFirstUserName)
            return
        }
        var splitSliderValue = 100
        if (binding.switchSecondUserActive.isChecked) {
            if (binding.settingsSecondUserName.text.toString() == "") {
                binding.settingsSecondUserName.error = getString(R.string.value_cannot_be_blank)
                focusAndOpenSoftKeyboard(requireContext(), binding.settingsSecondUserName)
                return
            }
            if (!textIsAlphaOrSpace(binding.settingsSecondUserName.text.toString())) {
                binding.settingsSecondUserName.error = getString(R.string.the_text_contains_non_alphabetic_characters)
                focusAndOpenSoftKeyboard(requireContext(), binding.settingsSecondUserName)
                return
            }
            if (!binding.secondUserEmail.text.toString().isEmailValid()) {
                binding.secondUserEmail.error = getString(R.string.this_is_not_a_valid_email_address)
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
                binding.joinUid.error = getString(R.string.value_cannot_be_blank)
                focusAndOpenSoftKeyboard(requireContext(), binding.joinUid)
                return
            }
            splitSliderValue = binding.splitSlider.value.toInt()
        }
        // at this point the user information is valid
        // update existing Spenders to add new info

        if (binding.switchDisconnect.isChecked) {
            binding.switchDisconnect.visibility = View.GONE
            binding.switchDisconnect.isChecked = false // prepare for next time
            binding.secondUserLayout.visibility = View.GONE
            binding.splitSliderLayout.visibility = View.GONE
            binding.splitLayout.visibility = View.GONE
            binding.spenderLayout.visibility = View.GONE
            binding.switchSecondUserActive.visibility = View.GONE
            binding.shareUIDLayout.visibility = View.GONE
            binding.uidLayout.visibility = View.VISIBLE
            binding.secondUserEmail.setText("")
            binding.settingsSecondUserName.setText("")
            MyApplication.userUID = MyApplication.originalUserUID
            MyApplication.databaseref.child("Users/" + MyApplication.userUID)
                .child("Info")
                .child("0")
                .child("JoinUser").removeValue()
            AppUserViewModel.removePrimary()
            switchTo(MyApplication.userUID)
            Toast.makeText(activity, getString(R.string.leaving_other_user), Toast.LENGTH_SHORT).show()
            activity?.onBackPressedDispatcher?.onBackPressed()
        } else if (!binding.switchSecondUserActive.isChecked && !binding.switchJoinOtherUser.isChecked) { // ie single mode
            SpenderViewModel.updateSpender(0, Spender(binding.settingsFirstUserName.text.toString(), binding.firstUserEmail.text.toString(), 100, 1))
            DefaultsViewModel.updateDefaultInt(cDEFAULT_SPENDER,0)
            if (SpenderViewModel.getTotalCount() >1) {
                // single mode, so if previous other users were there, turn them off
                SpenderViewModel.updateSpender(1, Spender(getString(R.string.other), "", 0, 0))
                SpenderViewModel.getSpender(2)?.isActive = 0
                binding.settingsSecondUserName.setText("")
                binding.secondUserEmail.setText("")
            }
            AppUserViewModel.removePrimary()
            AppUserViewModel.removeSecondary()
            activity?.onBackPressedDispatcher?.onBackPressed()
        } else if (binding.switchSecondUserActive.isChecked) {
            SpenderViewModel.updateSpender(0, Spender(binding.settingsFirstUserName.text.toString(), binding.firstUserEmail.text.toString(), splitSliderValue, 1))
            if (SpenderViewModel.getTotalCount() > 1) {
                SpenderViewModel.updateSpender(1, Spender(binding.settingsSecondUserName.text.toString(),
                        binding.secondUserEmail.text.toString(), 100 - splitSliderValue, 1))
            } else {
                SpenderViewModel.addSpender( 1, Spender(binding.settingsSecondUserName.text.toString(),
                        binding.secondUserEmail.text.toString(), 100 - splitSliderValue, 1))
                SpenderViewModel.addLocalSpender( Spender(getString(R.string.joint),"", 0, 1))
            }
            if (binding.secondUserEmail.text.toString() == binding.firstUserEmail.text.toString())
                SpenderViewModel.removeSecondAllowedUser()
            else
                SpenderViewModel.saveAsSecondAllowedUser(binding.secondUserEmail.text.toString())

            if (binding.firstUserEmail.text.toString() != binding.secondUserEmail.text.toString()) {
                binding.authorizationKey.text = MyApplication.userUID
                binding.shareUIDLayout.visibility = View.VISIBLE
                binding.shareKeyText.text = String.format(getString(R.string.share_this_authorization_key_with),
                        binding.settingsSecondUserName.text)
                AppUserViewModel.addSecondary(binding.secondUserEmail.text.toString())
            }
        } else { // binding.switchJoinUser isChecked
            MyApplication.databaseref.child("Users/" + MyApplication.userUID)
                .child("Info")
                .child("0")
                .child("JoinUser").setValue(binding.joinUid.text.toString())
            Toast.makeText(activity, getString(R.string.joining_user), Toast.LENGTH_SHORT).show()
            switchTo(binding.joinUid.text.toString())
            AppUserViewModel.addPrimary(binding.joinUid.text.toString())
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        MyApplication.playSound(context, R.raw.impact_jaw_breaker)
        Toast.makeText(activity, getString(R.string.changes_saved), Toast.LENGTH_SHORT).show()
//        activity?.onBackPressed()
//        (activity as MainActivity).singleUserMode(!binding.switchSecondUserActive.isChecked)
        binding.settingsCancelButton.visibility = View.GONE
        binding.settingsSaveButton.visibility = View.GONE
    }
}