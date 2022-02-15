package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.isrbet.budgetsbyisrbet.databinding.FragmentSettingsBinding
import android.widget.CheckBox
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import com.google.android.material.color.MaterialColors

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private var oldFirstUser: String = ""
    private var oldSecondUser: String = ""

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        val spenderOne = SpenderViewModel.getSpender(0, true)
        val spenderTwo = SpenderViewModel.getSpender(1, false)
        binding.settingsFirstUserName.setText(spenderOne?.name.toString())
        binding.firstUserEmail.text = spenderOne?.email
        if (spenderTwo != null) {
            binding.switchSecondUserActive.isChecked = spenderTwo.isActive == 1
            binding.settingsSecondUserName.setText(spenderTwo.name)
            binding.secondUserEmail.text = spenderTwo.email
        } else {
            binding.switchSecondUserActive.isChecked = false
            binding.settingsSecondUserName.setText("")
        }

        binding.splitSlider.value = SpenderViewModel.getSpenderSplit(0).toFloat()
        binding.splitName1.text = SpenderViewModel.getSpenderName(0)
        binding.splitName2.text = SpenderViewModel.getSpenderName(1)
        binding.splitName1Pct.text = SpenderViewModel.getSpenderSplit(0).toString()
        binding.splitName2Pct.text = SpenderViewModel.getSpenderSplit(1).toString()
        binding.splitSlider.addOnChangeListener { slider, value, fromUser ->
            binding.splitName1Pct.setText(binding.splitSlider.value.toInt().toString())
            binding.splitName2Pct.setText((100-binding.splitSlider.value.toInt()).toString())
        }

        var ctr = 400
        val spenderRadioGroup = binding.defaultSpenderRadioGroup
        spenderRadioGroup.removeAllViews()

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
            newRadioButton.setText(spender)
            newRadioButton.id = ctr++
            spenderRadioGroup.addView(newRadioButton)
            Log.d("Alex", "spender.name is $spender?.name and default is ${DefaultsViewModel.getDefault(
                cDEFAULT_SPENDER)}")
            if (spender == DefaultsViewModel.getDefault(cDEFAULT_SPENDER)) {
                Log.d("Alex", "found default paidby " + spender)
                spenderRadioGroup.check(newRadioButton.id)
            }
        }

        oldFirstUser = spenderOne?.name.toString()
        oldSecondUser = spenderTwo?.name.toString()

        setHasOptionsMenu(true)

        inflater.inflate(R.layout.fragment_settings, container, false)
        return binding.root
    }

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

        binding.settingsSaveButton.setOnClickListener {
            onSaveButtonClicked()
        }
        val defaultCategorySpinner =
            requireActivity().findViewById<Spinner>(R.id.settingsCategorySpinner)
        val arrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            CategoryViewModel.getCategoryAndSubcategoryList()
        )
        defaultCategorySpinner.adapter = arrayAdapter
        defaultCategorySpinner.setSelection(arrayAdapter.getPosition(DefaultsViewModel.getDefault(
            cDEFAULT_FULLCATEGORYNAME)))
        arrayAdapter.notifyDataSetChanged()
        val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), "1F")
        binding.settingsCategorySpinner.setBackgroundColor(Color.parseColor(hexColor))
        binding.settingsCategorySpinner.setPopupBackgroundResource(R.drawable.spinner)
        binding.switchSound.isChecked = DefaultsViewModel.getDefault(cDEFAULT_SOUND) != "Off"
        binding.switchQuote.isChecked = DefaultsViewModel.getDefault(cDEFAULT_QUOTE) != "Off"
        binding.switchIntegrateWithTD.isChecked =
            DefaultsViewModel.getDefault(cDEFAULT_INTEGRATEWITHTDSPEND) != "Off"
        binding.redPercentageSlider.value = DefaultsViewModel.getDefault(cDEFAULT_SHOWRED).toFloat()
        binding.redPercentage.text = DefaultsViewModel.getDefault(cDEFAULT_SHOWRED).toFloat().toInt().toString()

        if (binding.settingsSecondUserName.text.toString() == "") {
            binding.splitSlider.isEnabled = false
            for (i in 0 until binding.defaultSpenderRadioGroup.getChildCount()) {
                (binding.defaultSpenderRadioGroup.getChildAt(i) as RadioButton).isEnabled = false
            }
        }
        binding.switchSecondUserActive.setOnCheckedChangeListener { buttonView, isChecked ->
            if (binding.switchSecondUserActive.isChecked) {
                binding.secondUserLayout.visibility = View.VISIBLE
                binding.splitSliderLayout.visibility = View.VISIBLE
                binding.splitLayout.visibility = View.VISIBLE
                binding.spenderLayout.visibility = View.VISIBLE
                binding.splitSlider.value = 50.0F
            } else {
                binding.secondUserLayout.visibility = View.GONE
                binding.splitSliderLayout.visibility = View.GONE
                binding.splitLayout.visibility = View.GONE
                binding.spenderLayout.visibility = View.GONE
            }
        }
        binding.redPercentageSlider.addOnChangeListener { _, _, _ ->
            binding.redPercentage.text = binding.redPercentageSlider.value.toInt().toString()
        }

        binding.settingsSecondUserName.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
                if (binding.settingsSecondUserName.text.toString() != "") {
                    binding.splitSlider.isEnabled = true
                    for (i in 0 until binding.defaultSpenderRadioGroup.getChildCount()) {
                        (binding.defaultSpenderRadioGroup.getChildAt(i) as RadioButton).isEnabled = true
                    }
                }
            }
            override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun afterTextChanged(arg0: Editable) {}
        })

        binding.settingsFirstUserName.requestFocus()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        for (i in 0 until menu.size()) {
            menu.getItem(i).isVisible = (menu.getItem(i).itemId == R.id.ViewTranslationsFragment &&
                    DefaultsViewModel.getDefault(cDEFAULT_INTEGRATEWITHTDSPEND) == "On")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.ViewTranslationsFragment -> {
                view?.findNavController()?.navigate(R.id.ViewTranslationsFragment)
                true
            }
            else -> {
                val navController = findNavController()
                item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onSaveButtonClicked () {
        if (!textIsAlphaOrSpace(binding.settingsFirstUserName.text.toString())) {
//            showErrorMessage(getParentFragmentManager(), "The text contains unsafe characters.  They must be removed.")
            binding.settingsFirstUserName.error = "The text contains non-alphabetic characters."
            focusAndOpenSoftKeyboard(requireContext(), binding.settingsFirstUserName)
            return
        }
        if (!textIsAlphaOrSpace(binding.settingsSecondUserName.text.toString())) {
//            showErrorMessage(getParentFragmentManager(), "The text contains unsafe characters.  They must be removed.")
            binding.settingsSecondUserName.error = "The text contains non-alphabetic characters."
            focusAndOpenSoftKeyboard(requireContext(), binding.settingsSecondUserName)
            return
        }
        // need to reject if all the fields aren't entered correctly
        // first user cannot be blank
        if (binding.settingsFirstUserName.text.toString() == "") {
//            showErrorMessage(getParentFragmentManager(), getString(R.string.missingFirstUserName))
            binding.settingsFirstUserName.error = getString(R.string.missingFirstUserName)
            focusAndOpenSoftKeyboard(requireContext(), binding.settingsFirstUserName)
            return
        }
        if (binding.switchSecondUserActive.isChecked && binding.settingsSecondUserName.text.toString() == "") {
            binding.settingsSecondUserName.error = getString(R.string.missingSecondUserName)
            focusAndOpenSoftKeyboard(requireContext(), binding.settingsSecondUserName)
            return
        }

        var spenderChecked = SpenderViewModel.getSpenderName(0)
        var splitSliderValue = 100
        if (binding.switchSecondUserActive.isChecked) {
            val defaultSpenderRadioGroup = binding.defaultSpenderRadioGroup
            val spenderCheckedID = defaultSpenderRadioGroup.checkedRadioButtonId
            Log.d("Alex", "spenderCheckedID $spenderCheckedID")
            if (spenderCheckedID == -1) { // must have been a name change
                val but = binding.defaultSpenderRadioGroup.getChildAt(0) as RadioButton
                but.error = getString(R.string.oneUserMustBeDefault)
                focusAndOpenSoftKeyboard(requireContext(), binding.settingsFirstUserName)
                return
            } else {
                val buttonSpenderChecked =
                    requireActivity().findViewById(spenderCheckedID) as RadioButton
                spenderChecked = buttonSpenderChecked.text.toString()
            }
            splitSliderValue = binding.splitSlider.value.toInt()
        }
        val secondNameActive = if (binding.switchSecondUserActive.isChecked) 1 else 0
        // at this point the user information is valid
        // delete existing Spenders and add new info
        SpenderViewModel.deleteSpender(oldFirstUser)
        SpenderViewModel.deleteSpender(oldSecondUser)
        SpenderViewModel.addSpender(Spender(binding.settingsFirstUserName.text.toString(), binding.firstUserEmail.text.toString(), splitSliderValue, 1))
        if (binding.settingsSecondUserName.text.toString() != "") {
            SpenderViewModel.addSpender(Spender(binding.settingsSecondUserName.text.toString(), binding.secondUserEmail.text.toString(), 100-splitSliderValue, secondNameActive))
        }
        DefaultsViewModel.updateDefault(cDEFAULT_SPENDER,spenderChecked)

        // check category default
        val defaultCategorySpinner = requireActivity().findViewById(R.id.settingsCategorySpinner) as Spinner

        if (defaultCategorySpinner.selectedItem != null) {
            Log.d("Alex", "Sub-category is " + defaultCategorySpinner.selectedItem.toString())
            val defaultCategory = Category(defaultCategorySpinner.selectedItem.toString())
            if (defaultCategory.categoryName != DefaultsViewModel.getDefault(cDEFAULT_CATEGORY))
                DefaultsViewModel.updateDefault(cDEFAULT_CATEGORY, defaultCategory.categoryName)
            if (defaultCategory.subcategoryName != DefaultsViewModel.getDefault(cDEFAULT_SUBCATEGORY))
                DefaultsViewModel.updateDefault(cDEFAULT_SUBCATEGORY, defaultCategory.subcategoryName)
        }
        // check default Integrate with TD Spend
        Log.d("Alex", "td switch is " + binding.switchIntegrateWithTD.showText)
        val integrateField = if (binding.switchIntegrateWithTD.isChecked) "On" else "Off"
        if (integrateField != DefaultsViewModel.getDefault(cDEFAULT_INTEGRATEWITHTDSPEND)) {
            DefaultsViewModel.updateDefault(cDEFAULT_INTEGRATEWITHTDSPEND, integrateField)
        }
        // check default Sound
        val soundField = if (binding.switchSound.isChecked) "On" else "Off"
        if (soundField != DefaultsViewModel.getDefault(cDEFAULT_SOUND))
            DefaultsViewModel.updateDefault(cDEFAULT_SOUND, soundField)
        // check default Quote
        val quoteField = if (binding.switchQuote.isChecked) "On" else "Off"
        if (quoteField != DefaultsViewModel.getDefault(cDEFAULT_QUOTE))
            DefaultsViewModel.updateDefault(cDEFAULT_QUOTE, quoteField)

        // check default showRed
        if (binding.redPercentageSlider.value.toInt() != DefaultsViewModel.getDefault(cDEFAULT_SHOWRED).toFloat().toInt())
            DefaultsViewModel.updateDefault(cDEFAULT_SHOWRED, binding.redPercentageSlider.value.toInt().toString())
        MyApplication.playSound(context, R.raw.impact_jaw_breaker)
    }
}