package com.isrbet.budgetsbyisrbet

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.isrbet.budgetsbyisrbet.databinding.FragmentSettingsBinding
import android.widget.CheckBox
import androidx.navigation.findNavController

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    var oldFirstUser: String = ""
    var oldSecondUser: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        var spenderOne = SpenderViewModel.getSpender(0)
        var spenderTwo = SpenderViewModel.getSpender(1)
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
        if (spenderOne?.name.toString() == DefaultsViewModel.getDefault(cDEFAULT_SPENDER))
            binding.settingsFirstUserCheckbox.setChecked(true)
        else if (spenderTwo?.name.toString() == DefaultsViewModel.getDefault(cDEFAULT_SPENDER))
            binding.settingsSecondUserCheckbox.setChecked(true)
        else if (DefaultsViewModel.getDefault(cDEFAULT_SPENDER) == "Joint")
            binding.settingsJointUserCheckbox.setChecked(true)

        oldFirstUser = spenderOne?.name.toString()
        oldSecondUser = spenderTwo?.name.toString()

        setHasOptionsMenu(true)

        inflater.inflate(R.layout.fragment_settings, container, false)
        return binding.root
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        binding.settingsEditCategoriesButton.setOnClickListener { view ->
            view.findNavController().navigate(R.id.SettingsEditCategoryFragment)
        }
        binding.settingsSaveButton.setOnClickListener {
            onSaveButtonClicked()
        }
        var myFirstCheckBox = requireActivity().findViewById(R.id.settings_first_user_checkbox) as CheckBox
        myFirstCheckBox.setOnClickListener(View.OnClickListener {
            if (myFirstCheckBox.isChecked) {
                binding.settingsSecondUserCheckbox.setChecked(false)
                binding.settingsJointUserCheckbox.setChecked(false)
            }
        })
        var mySecondCheckBox = requireActivity().findViewById(R.id.settings_second_user_checkbox) as CheckBox
        mySecondCheckBox.setOnClickListener(View.OnClickListener {
            if (mySecondCheckBox.isChecked) {
                binding.settingsFirstUserCheckbox.setChecked(false)
                binding.settingsJointUserCheckbox.setChecked(false)
            }
        })
        var myJointCheckBox = requireActivity().findViewById(R.id.settings_joint_user_checkbox) as CheckBox
        myJointCheckBox.setOnClickListener(View.OnClickListener {
            if (myJointCheckBox.isChecked) {
                binding.settingsFirstUserCheckbox.setChecked(false)
                binding.settingsSecondUserCheckbox.setChecked(false)
            }
        })

        var defaultCategorySpinner =
            requireActivity().findViewById<Spinner>(R.id.settings_default_category)
        val arrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            CategoryViewModel.getCategoryAndSubcategoryList()
        )
        defaultCategorySpinner.adapter = arrayAdapter
        defaultCategorySpinner.setSelection(arrayAdapter.getPosition(DefaultsViewModel.getDefault(
            cDEFAULT_CATEGORY) + "-" + DefaultsViewModel.getDefault(cDEFAULT_SUBCATEGORY)))
        if (arrayAdapter != null) {
            arrayAdapter.notifyDataSetChanged()
        }
        val integrateField = requireActivity().findViewById(R.id.settings_integrate_with_TD_Spend) as EditText
        integrateField.setText(DefaultsViewModel.getDefault(cDEFAULT_INTEGRATEWITHTDSPEND))
        val showRedField = requireActivity().findViewById(R.id.settings_red_percentage) as EditText
        showRedField.setText(DefaultsViewModel.getDefault(cDEFAULT_SHOWRED))
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        for (i in 0 until menu.size()) {
            menu.getItem(i).setVisible(false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onSaveButtonClicked () {
        if (!textIsSafe(binding.settingsFirstUserName.text.toString())) {
            showErrorMessage(getParentFragmentManager(), "The text contains unsafe characters.  They must be removed.")
            focusAndOpenSoftKeyboard(requireContext(), binding.settingsFirstUserName)
            return
        }
        if (!textIsSafe(binding.settingsSecondUserName.text.toString())) {
            showErrorMessage(getParentFragmentManager(), "The text contains unsafe characters.  They must be removed.")
            focusAndOpenSoftKeyboard(requireContext(), binding.settingsSecondUserName)
            return
        }
        // need to reject if all the fields aren't entered correctly
        // first user cannot be blank
        if (binding.settingsFirstUserName.text.toString() == "") {
            showErrorMessage(getParentFragmentManager(), getString(R.string.missingFirstUserName))
            focusAndOpenSoftKeyboard(requireContext(), binding.settingsFirstUserName)
            return
        }
        if (binding.settingsFirstPercentage.text.toString() == "") {
            showErrorMessage(getParentFragmentManager(), getString(R.string.missingFirstUserPercentage))
            focusAndOpenSoftKeyboard(requireContext(), binding.settingsFirstPercentage)
            return
        }
        // if second name or % is entered, then both must be entered
        if ((binding.settingsSecondUserName.text.toString() != "" ||
                    binding.settingsSecondPercentage.text.toString() != "" ||
                    binding.settingsSecondUserCheckbox.isChecked) &&
            (binding.settingsSecondUserName.text.toString() == "" ||
                    binding.settingsSecondPercentage.text.toString() == "")) {
            showErrorMessage(getParentFragmentManager(), getString(R.string.missingSecondUserInformation))
            focusAndOpenSoftKeyboard(requireContext(), binding.settingsSecondUserName)
            return
        }
        // %'s must add to 100
        if ((binding.settingsSecondPercentage.text.toString() == "" && binding.settingsFirstPercentage.text.toString().toInt() != 100) ||
            (binding.settingsSecondPercentage.text.toString() != "" && binding.settingsFirstPercentage.text.toString().toInt() +
            binding.settingsSecondPercentage.text.toString().toInt() != 100)) {
            showErrorMessage(getParentFragmentManager(), getString(R.string.percentagesDontAddTo100))
            focusAndOpenSoftKeyboard(requireContext(), binding.settingsFirstPercentage)
            return
        }
        // at least one user must have its default checked
        if (!binding.settingsFirstUserCheckbox.isChecked && !binding.settingsSecondUserCheckbox.isChecked && !binding.settingsJointUserCheckbox.isChecked) {
            showErrorMessage(getParentFragmentManager(), getString(R.string.oneUserMustBeDefault))
            focusAndOpenSoftKeyboard(requireContext(), binding.settingsFirstUserCheckbox)
            return
        }
        // only one user must have its default checked
        var cChecked: Int = 0
        if (binding.settingsFirstUserCheckbox.isChecked)
            cChecked++
        if (binding.settingsSecondUserCheckbox.isChecked)
            cChecked++
        if (binding.settingsJointUserCheckbox.isChecked)
            cChecked++
        if (cChecked > 1) {
            showErrorMessage(getParentFragmentManager(), getString(R.string.onlyOneUserMustBeDefault))
            focusAndOpenSoftKeyboard(requireContext(), binding.settingsFirstUserCheckbox)
            return
        }
        // Integrate must be Yes or No
        if (binding.settingsIntegrateWithTDSpend.text.toString() != "Yes" && binding.settingsIntegrateWithTDSpend.text.toString() != "No") {
            showErrorMessage(getParentFragmentManager(), getString(R.string.fieldMustBeYesOrNo))
            focusAndOpenSoftKeyboard(requireContext(), binding.settingsIntegrateWithTDSpend)
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
        val defaultCategorySpinner = requireActivity().findViewById(R.id.settings_default_category) as Spinner

        Log.d("Alex", "Sub-category is " + defaultCategorySpinner.selectedItem.toString())
        val dash = defaultCategorySpinner.selectedItem.toString().indexOf("-")
        val defCat = defaultCategorySpinner.selectedItem.toString().substring(0, dash)
        val defSubCat = defaultCategorySpinner.selectedItem.toString().substring(dash + 1,defaultCategorySpinner.selectedItem.toString().length)
        if (defCat != DefaultsViewModel.getDefault(cDEFAULT_CATEGORY))
            DefaultsViewModel.updateDefault(cDEFAULT_CATEGORY, defCat)
        if (defSubCat != DefaultsViewModel.getDefault(cDEFAULT_SUBCATEGORY))
            DefaultsViewModel.updateDefault(cDEFAULT_SUBCATEGORY, defSubCat)

        // check default Integrate with TD Spend
        val integrateField = requireActivity().findViewById(R.id.settings_integrate_with_TD_Spend) as EditText
        if (integrateField.text.toString() != DefaultsViewModel.getDefault(cDEFAULT_INTEGRATEWITHTDSPEND))
            DefaultsViewModel.updateDefault(cDEFAULT_INTEGRATEWITHTDSPEND, integrateField.text.toString())

        // check default showRed
        val showRedField = requireActivity().findViewById(R.id.settings_red_percentage) as EditText
        var defShowRed: Int = 0
        if (showRedField.text.toString() != "")
            defShowRed = showRedField.text.toString().toInt()
        if (defShowRed != DefaultsViewModel.getDefault(cDEFAULT_SHOWRED).toInt())
            DefaultsViewModel.updateDefault(cDEFAULT_SHOWRED, defShowRed.toString())
        val mp: MediaPlayer = MediaPlayer.create(context, R.raw.impact_jaw_breaker)
        mp.start()
    }
}