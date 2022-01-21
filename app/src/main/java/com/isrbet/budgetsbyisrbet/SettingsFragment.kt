package com.isrbet.budgetsbyisrbet

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        val spenderOne = SpenderViewModel.getSpender(0)
        val spenderTwo = SpenderViewModel.getSpender(1)
        binding.settingsFirstUserName.setText(spenderOne?.name.toString())
        binding.settingsFirstPercentage.setText(spenderOne?.split.toString())
        if (spenderTwo != null) {
            binding.settingsSecondUserName.setText(spenderTwo.name)
            binding.settingsSecondPercentage.setText(spenderTwo.split.toString())
        } else {
            binding.settingsSecondUserName.setText("")
            binding.settingsSecondPercentage.setText("")
        }
        if (spenderTwo?.name == "")
            binding.settingsJointRow.visibility = View.GONE
        else
            binding.settingsJointRow.visibility = View.VISIBLE
        when {
            spenderOne?.name.toString() == DefaultsViewModel.getDefault(cDEFAULT_SPENDER) -> binding.settingsFirstUserCheckbox.isChecked =
                true
            spenderTwo?.name.toString() == DefaultsViewModel.getDefault(cDEFAULT_SPENDER) -> binding.settingsSecondUserCheckbox.isChecked =
                true
            DefaultsViewModel.getDefault(cDEFAULT_SPENDER) == "Joint" -> binding.settingsJointUserCheckbox.isChecked =
                true
        }

        oldFirstUser = spenderOne?.name.toString()
        oldSecondUser = spenderTwo?.name.toString()

        setHasOptionsMenu(true)

        inflater.inflate(R.layout.fragment_settings, container, false)
        return binding.root
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        binding.settingsSaveButton.setOnClickListener {
            onSaveButtonClicked()
        }
        val myFirstCheckBox = requireActivity().findViewById(R.id.settings_first_user_checkbox) as CheckBox
        myFirstCheckBox.setOnClickListener({
            if (myFirstCheckBox.isChecked) {
                binding.settingsSecondUserCheckbox.isChecked = false
                binding.settingsJointUserCheckbox.isChecked = false
            }
        })
        myFirstCheckBox.buttonTintList = ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
        val mySecondCheckBox = requireActivity().findViewById(R.id.settings_second_user_checkbox) as CheckBox
        mySecondCheckBox.setOnClickListener({
            if (mySecondCheckBox.isChecked) {
                binding.settingsFirstUserCheckbox.isChecked = false
                binding.settingsJointUserCheckbox.isChecked = false
            }
        })
        mySecondCheckBox.buttonTintList = ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))
        val myJointCheckBox = requireActivity().findViewById(R.id.settings_joint_user_checkbox) as CheckBox
        myJointCheckBox.setOnClickListener({
            if (myJointCheckBox.isChecked) {
                binding.settingsFirstUserCheckbox.isChecked = false
                binding.settingsSecondUserCheckbox.isChecked = false
            }
        })
        myJointCheckBox.buttonTintList = ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK))

        val defaultCategorySpinner =
            requireActivity().findViewById<Spinner>(R.id.settingsCategorySpinner)
        val arrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            CategoryViewModel.getCategoryAndSubcategoryList()
        )
        defaultCategorySpinner.adapter = arrayAdapter
        defaultCategorySpinner.setSelection(arrayAdapter.getPosition(DefaultsViewModel.getDefault(
            cDEFAULT_CATEGORY) + "-" + DefaultsViewModel.getDefault(cDEFAULT_SUBCATEGORY)))
        arrayAdapter.notifyDataSetChanged()
        val hexColor = getColorInHex(MaterialColors.getColor(requireContext(), R.attr.editTextBackground, Color.BLACK), "1F")
        binding.settingsCategorySpinner.setBackgroundColor(Color.parseColor(hexColor))
        binding.settingsCategorySpinner.setPopupBackgroundResource(R.drawable.spinner)
        binding.switchSound.isChecked = DefaultsViewModel.getDefault(cDEFAULT_SOUND) != "Off"
        binding.switchIntegrateWithTD.isChecked =
            DefaultsViewModel.getDefault(cDEFAULT_INTEGRATEWITHTDSPEND) != "Off"
        binding.redPercentageSlider.value = DefaultsViewModel.getDefault(cDEFAULT_SHOWRED).toFloat()
        binding.redPercentage.text = DefaultsViewModel.getDefault(cDEFAULT_SHOWRED).toFloat().toInt().toString()

        if (binding.settingsSecondUserName.text.toString() == "") {
            binding.settingsSecondPercentage.isEnabled = false
            binding.settingsSecondUserCheckbox.isEnabled = false
            binding.settingsJointUserCheckbox.isEnabled = false
        }
        val mySecondName = requireActivity().findViewById(R.id.settings_second_user_name) as EditText
        mySecondName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                if (p0.toString() == "") {
                    binding.settingsSecondPercentage.isEnabled = false
                    binding.settingsSecondUserCheckbox.isEnabled = false
                    binding.settingsJointUserCheckbox.isEnabled = false
                } else {
                    binding.settingsSecondPercentage.isEnabled = true
                    binding.settingsSecondUserCheckbox.isEnabled = true
                    binding.settingsJointUserCheckbox.isEnabled = true
                }
            }
        })
        binding.redPercentageSlider.addOnChangeListener { _, _, _ ->
            binding.redPercentage.text = binding.redPercentageSlider.value.toInt().toString()
        }
        binding.settingsFirstUserName.requestFocus()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        for (i in 0 until menu.size()) {
            menu.getItem(i).isVisible = menu.getItem(i).itemId == R.id.EditCategory
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.EditCategory) {
            view?.findNavController()?.navigate(R.id.SettingsEditCategoryFragment)
            return true
        } else {
            val navController = findNavController()
            return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onSaveButtonClicked () {
        if (!textIsSafe(binding.settingsFirstUserName.text.toString())) {
//            showErrorMessage(getParentFragmentManager(), "The text contains unsafe characters.  They must be removed.")
            binding.settingsFirstUserName.error = "The text contains unsafe characters."
            focusAndOpenSoftKeyboard(requireContext(), binding.settingsFirstUserName)
            return
        }
        if (!textIsSafe(binding.settingsSecondUserName.text.toString())) {
//            showErrorMessage(getParentFragmentManager(), "The text contains unsafe characters.  They must be removed.")
            binding.settingsSecondUserName.error = "The text contains unsafe characters."
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
        if (binding.settingsFirstPercentage.text.toString() == "") {
//            showErrorMessage(getParentFragmentManager(), getString(R.string.missingFirstUserPercentage))
            binding.settingsFirstPercentage.error = getString(R.string.missingFirstUserPercentage)
            focusAndOpenSoftKeyboard(requireContext(), binding.settingsFirstPercentage)
            return
        }
        // if second name or % is entered, then both must be entered
        if ((binding.settingsSecondUserName.text.toString() != "" ||
                    binding.settingsSecondPercentage.text.toString() != "" ||
                    binding.settingsSecondUserCheckbox.isChecked) &&
            (binding.settingsSecondUserName.text.toString() == "" ||
                    binding.settingsSecondPercentage.text.toString() == "")) {
//            showErrorMessage(getParentFragmentManager(), getString(R.string.missingSecondUserInformation))
            binding.settingsSecondUserName.error = getString(R.string.missingSecondUserInformation)
            focusAndOpenSoftKeyboard(requireContext(), binding.settingsSecondUserName)
            return
        }
        // %'s must add to 100
        if ((binding.settingsSecondPercentage.text.toString() == "" && binding.settingsFirstPercentage.text.toString().toInt() != 100) ||
            (binding.settingsSecondPercentage.text.toString() != "" && binding.settingsFirstPercentage.text.toString().toInt() +
            binding.settingsSecondPercentage.text.toString().toInt() != 100)) {
//            showErrorMessage(getParentFragmentManager(), getString(R.string.percentagesDontAddTo100))
            binding.settingsFirstPercentage.error = getString(R.string.percentagesDontAddTo100)
            focusAndOpenSoftKeyboard(requireContext(), binding.settingsFirstPercentage)
            return
        }
        // at least one user must have its default checked
        if (!binding.settingsFirstUserCheckbox.isChecked && !binding.settingsSecondUserCheckbox.isChecked && !binding.settingsJointUserCheckbox.isChecked) {
//            showErrorMessage(getParentFragmentManager(), getString(R.string.oneUserMustBeDefault))
            binding.settingsFirstUserCheckbox.error = getString(R.string.oneUserMustBeDefault)
            focusAndOpenSoftKeyboard(requireContext(), binding.settingsFirstUserCheckbox)
            return
        }
        // only one user must have its default checked
        var cChecked = 0
        if (binding.settingsFirstUserCheckbox.isChecked)
            cChecked++
        if (binding.settingsSecondUserCheckbox.isChecked)
            cChecked++
        if (binding.settingsJointUserCheckbox.isChecked)
            cChecked++
        if (cChecked > 1) {
//            showErrorMessage(getParentFragmentManager(), getString(R.string.onlyOneUserMustBeDefault))
            binding.settingsFirstUserCheckbox.error = getString(R.string.onlyOneUserMustBeDefault)
            focusAndOpenSoftKeyboard(requireContext(), binding.settingsFirstUserCheckbox)
            return
        }

        // at this point the user information is valid
        // delete existing Spenders and add new info
        SpenderViewModel.deleteSpender(oldFirstUser)
        SpenderViewModel.deleteSpender(oldSecondUser)
        SpenderViewModel.addSpender(Spender(binding.settingsFirstUserName.text.toString(), binding.settingsFirstPercentage.text.toString().toInt()))
        if (binding.settingsSecondUserName.text.toString() != "") {
            SpenderViewModel.addSpender(Spender(binding.settingsSecondUserName.text.toString(), binding.settingsSecondPercentage.text.toString().toInt()))
        }
        if (binding.settingsFirstUserCheckbox.isChecked)
            DefaultsViewModel.updateDefault(cDEFAULT_SPENDER, binding.settingsFirstUserName.text.toString())
        else if (binding.settingsSecondUserCheckbox.isChecked)
            DefaultsViewModel.updateDefault(cDEFAULT_SPENDER, binding.settingsSecondUserName.text.toString())
        else
            DefaultsViewModel.updateDefault(cDEFAULT_SPENDER, "Joint")

        // check category default
        val defaultCategorySpinner = requireActivity().findViewById(R.id.settingsCategorySpinner) as Spinner

        if (defaultCategorySpinner.selectedItem != null) {
            Log.d("Alex", "Sub-category is " + defaultCategorySpinner.selectedItem.toString())
            val dash = defaultCategorySpinner.selectedItem.toString().indexOf("-")
            val defCat = defaultCategorySpinner.selectedItem.toString().substring(0, dash)
            val defSubCat = defaultCategorySpinner.selectedItem.toString()
                .substring(dash + 1, defaultCategorySpinner.selectedItem.toString().length)
            if (defCat != DefaultsViewModel.getDefault(cDEFAULT_CATEGORY))
                DefaultsViewModel.updateDefault(cDEFAULT_CATEGORY, defCat)
            if (defSubCat != DefaultsViewModel.getDefault(cDEFAULT_SUBCATEGORY))
                DefaultsViewModel.updateDefault(cDEFAULT_SUBCATEGORY, defSubCat)
        }
        // check default Integrate with TD Spend
        Log.d("Alex", "td switch is " + binding.switchIntegrateWithTD.showText)
        val integrateField = if (binding.switchIntegrateWithTD.isChecked) "On" else "Off"
        if (integrateField != DefaultsViewModel.getDefault(cDEFAULT_INTEGRATEWITHTDSPEND))
            DefaultsViewModel.updateDefault(cDEFAULT_INTEGRATEWITHTDSPEND, integrateField)
        // check default Sound
        val soundField = if (binding.switchSound.isChecked) "On" else "Off"
        if (soundField != DefaultsViewModel.getDefault(cDEFAULT_SOUND))
            DefaultsViewModel.updateDefault(cDEFAULT_SOUND, soundField)

        // check default showRed
        if (binding.redPercentageSlider.value.toInt() != DefaultsViewModel.getDefault(cDEFAULT_SHOWRED).toFloat().toInt())
            DefaultsViewModel.updateDefault(cDEFAULT_SHOWRED, binding.redPercentageSlider.value.toInt().toString())
        MyApplication.playSound(context, R.raw.impact_jaw_breaker)
    }
}