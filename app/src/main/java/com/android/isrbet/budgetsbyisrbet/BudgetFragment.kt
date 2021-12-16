package com.isrbet.budgetsbyisrbet

import BudgetDialogViewModel
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.*
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.isrbet.budgetsbyisrbet.databinding.FragmentBudgetBinding
import java.lang.Math.abs

class BudgetFragment : Fragment() {
    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    private val budgetDialogViewModel: BudgetDialogViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_budget, container, false)
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        val dateNow = Calendar.getInstance()
        val currentYear = dateNow.get(Calendar.YEAR)
        val currentMonth = dateNow.get(Calendar.MONTH) + 1
        val m1 = BudgetMonth(currentYear,currentMonth)
        val m2 = BudgetMonth(currentYear,currentMonth)
        m2.addMonth()
        val m3 = BudgetMonth(currentYear,currentMonth)
        m3.addMonth(2)
        val m4 = BudgetMonth(currentYear,currentMonth)
        m4.addMonth(3)

        val adapter = BudgetAdapter(requireContext(), BudgetViewModel.getBudgetInputRows(m1))

        val listView: ListView = requireActivity().findViewById(R.id.budget_list_view)
        val m1View = requireActivity().findViewById(R.id.m1Heading) as TextView
        val m2View = requireActivity().findViewById(R.id.m2Heading) as TextView
        val m3View = requireActivity().findViewById(R.id.m3Heading) as TextView
        val m4View = requireActivity().findViewById(R.id.m4Heading) as TextView
        m1View.setText(m1.toString())
        m2View.setText(m2.toString())
        m3View.setText(m3.toString())
        m4View.setText(m4.toString())

        listView.setAdapter(adapter)

        listView.onItemClickListener = object : AdapterView.OnItemClickListener {

            override fun onItemClick(parent: AdapterView<*>, view: View,
                                     position: Int, id: Long) {

                // value of item that is clicked
                val itemValue = listView.getItemAtPosition(position) as BudgetInputRow

                // Toast the values
                Toast.makeText(requireContext(),
                    "Position :$position\nItem Value : $itemValue", Toast.LENGTH_LONG)
                    .show()
                Log.d("Alex", "m1 is " +m1.toString() + " m1view " + m1View.text.toString())
                var bdf = BudgetDialogFragment.newInstance(itemValue.category, m1View.text.toString(), itemValue.m1, m2View.text.toString(), itemValue.m2, m3View.text.toString(), itemValue.m3, m4View.text.toString(),  itemValue.m4)
                bdf.show(getParentFragmentManager(), "Edit Budget Values")
                // Create the observer which updates the UI.
                val m1Observer = Observer<Double> { newM1 ->
                    itemValue.m1 = budgetDialogViewModel.mym1.value.toString()
                    Toast.makeText(activity, "You should manually refresh this screen in a few seconds", Toast.LENGTH_SHORT).show()
                }
                budgetDialogViewModel.mym1.observe(viewLifecycleOwner, m1Observer)
                val m2Observer = Observer<Double> { newM2 ->
                    itemValue.m2 = budgetDialogViewModel.mym2.value.toString()
                    Toast.makeText(activity, "You should manually refresh this screen in a few seconds", Toast.LENGTH_SHORT).show()
                }
                budgetDialogViewModel.mym2.observe(viewLifecycleOwner, m2Observer)
                val m3Observer = Observer<Double> { newM3 ->
                    itemValue.m3 = budgetDialogViewModel.mym3.value.toString()
                    Toast.makeText(activity, "You should manually refresh this screen in a few seconds", Toast.LENGTH_SHORT).show()
                }
                budgetDialogViewModel.mym3.observe(viewLifecycleOwner, m3Observer)
                val m4Observer = Observer<Double> { newM4 ->
                    itemValue.m4 = budgetDialogViewModel.mym4.value.toString()
                    Toast.makeText(activity, "You should manually refresh this screen in a few seconds", Toast.LENGTH_SHORT).show()
                }
                budgetDialogViewModel.mym4.observe(viewLifecycleOwner, m4Observer)
            }
        }
        val refreshSwiper: SwipeRefreshLayout = requireActivity().findViewById(R.id.budget_swipe_refresh)
        refreshSwiper.setOnRefreshListener {
            refreshData()
        }

        BudgetViewModel.singleInstance.setCallback(object: BudgetDataUpdatedCallback {
            override fun onDataUpdate() {
                Log.d("Alex", "got a callback that budget data was updated")
                refreshData()
            }
        })

        // for some reason "binding.buttonToday.setOnClickListener doesn't work, but the following does
        getView()?.findViewById<Button>(R.id.button_budget_year_backward)?.setOnClickListener { view: View ->
            updateHeaders(-12)
            refreshData()
        }
        getView()?.findViewById<Button>(R.id.button_budget_year_forward)?.setOnClickListener { view: View ->
            updateHeaders(12)
            refreshData()
        }
        getView()?.findViewById<Button>(R.id.button_budget_4month_backward)?.setOnClickListener { view: View ->
            updateHeaders(-4)
            refreshData()
        }
        getView()?.findViewById<Button>(R.id.button_budget_4month_forward)?.setOnClickListener { view: View ->
            updateHeaders(4)
            refreshData()
        }
        getView()?.findViewById<Button>(R.id.button_budget_month_backward)?.setOnClickListener { view: View ->
            updateHeaders(-1)
            refreshData()
        }
        getView()?.findViewById<Button>(R.id.button_budget_month_forward)?.setOnClickListener { view: View ->
            updateHeaders(1)
            refreshData()
        }
        getView()?.findViewById<Button>(R.id.button_budget_today)?.setOnClickListener { view: View ->
            updateHeaders(0)
            refreshData()
        }
    }

    fun updateHeaders(offset:Int) {
        val tvM1: TextView = requireActivity().findViewById(R.id.m1Heading)
        val tvM2: TextView = requireActivity().findViewById(R.id.m2Heading)
        val tvM3: TextView = requireActivity().findViewById(R.id.m3Heading)
        val tvM4: TextView = requireActivity().findViewById(R.id.m4Heading)
        var newM1 = BudgetMonth(tvM1.text.toString())
        if (offset == 0) {
            val dateNow = Calendar.getInstance()
            newM1.year = dateNow.get(Calendar.YEAR)
            newM1.month = dateNow.get(Calendar.MONTH)
        } else if (offset < 0) {
            newM1.decrementMonth(abs(offset))
        } else {
            newM1.addMonth(offset)
        }
        var newM2 = BudgetMonth(newM1)
        newM2.addMonth()
        var newM3 = BudgetMonth(newM2)
        newM3.addMonth()
        var newM4 = BudgetMonth(newM3)
        newM4.addMonth()

        tvM1.text = newM1.toString()
        tvM2.text = newM2.toString()
        tvM3.text = newM3.toString()
        tvM4.text = newM4.toString()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        for (i in 0 until menu.size()) {
            if (menu.getItem(i).getItemId() === R.id.Refresh)
                menu.getItem(i).setVisible(true)
            else if (menu.getItem(i).getItemId() === R.id.ShowMe)
                menu.getItem(i).setVisible(true)
            else
                menu.getItem(i).setVisible(false)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId === R.id.Refresh) {
            refreshData()
            return true
        } else if (item.itemId === R.id.ShowMe) {
                BudgetViewModel.showMe()
                return true
        } else {
            val navController = findNavController()
            return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
        }
    }

    fun refreshData() {
        val firstMonth: TextView = requireActivity().findViewById(R.id.m1Heading)
        val adapter = BudgetAdapter(requireContext(), BudgetViewModel.getBudgetInputRows(BudgetMonth(firstMonth.text.toString())))
        val listView: ListView = requireActivity().findViewById(R.id.budget_list_view)
        listView.setAdapter(adapter)
        val refreshSwiper: SwipeRefreshLayout = requireActivity().findViewById(R.id.budget_swipe_refresh)
        refreshSwiper.isRefreshing = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        BudgetViewModel.singleInstance.setCallback(null)
        _binding = null
    }
}