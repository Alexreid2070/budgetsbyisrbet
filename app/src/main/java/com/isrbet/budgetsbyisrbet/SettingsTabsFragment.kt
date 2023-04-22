package com.isrbet.budgetsbyisrbet

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.isrbet.budgetsbyisrbet.databinding.FragmentSettingsTabsBinding

class SettingsTabsFragment : Fragment() {
    private var _binding: FragmentSettingsTabsBinding? = null
    private val binding get() = _binding!!
    private val args: SettingsTabsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsTabsBinding.inflate(inflater, container, false)

        val adapter = TabsAdapter(activity)
        adapter.addFragment(SettingsFragment(), "Settings")
        adapter.addFragment(CategoryFragment(), "Categories")
        val budFra = BudgetViewAllFragment().apply {
            arguments = Bundle().apply {
                putString("categoryID", args.categoryID)
                putString("year", args.year)
                putString("month", args.month)
            }
        }
        adapter.addFragment(budFra, "Budgets")
        adapter.addFragment(ScheduledPaymentFragment(), "Scheduled Payments")
        val f1 = ScheduledPaymentFragment()
        f1.arguments = this.arguments

        binding.viewPager.adapter = adapter
        binding.viewPager.currentItem = args.targetTab
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = adapter.getTabTitle(position)
        }.attach()
        binding.viewPager.adapter = adapter
        for (i in 0 until binding.tabLayout.tabCount) {
            val tab = binding.tabLayout.getTabAt(i)
            tab?.customView = createCustomTabView(tab?.text.toString(), 15, android.R.color.black)
            if (tab != null)
                if (i == args.targetTab)
                    setTabActive(tab)
                else
                    setTabInactive(tab)
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    setTabActive(tab)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    setTabInactive(tab)
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

        })

        // Inflate the layout for this fragment
        inflater.inflate(R.layout.fragment_settings_tabs, container, false)
        return binding.root
    }

    private fun createCustomTabView(tabText: String, tabSizeSp: Int, textColor: Int): View? {
        val tabCustomView: View = layoutInflater.inflate(R.layout.tab_customview, null)
        val tabTextView = tabCustomView.findViewById<TextView>(R.id.tabTV)
        tabTextView.text = tabText
        tabTextView.textSize = tabSizeSp.toFloat()
        tabTextView.setTextColor(ContextCompat.getColor(tabCustomView.context, textColor))
        return tabCustomView
    }
    private fun setTabActive(tab: TabLayout.Tab) {
        val tabCustomView = tab.customView
        val tabTextView = tabCustomView!!.findViewById<TextView>(R.id.tabTV)
//        tabTextView.textSize = 17F
        tabTextView.setTextColor(ContextCompat.getColor(tabCustomView.context, R.color.black))
        tabTextView.setTypeface(null, Typeface.BOLD)
    }
    private fun setTabInactive(tab: TabLayout.Tab) {
        val tabCustomView = tab.customView
        val tabTextView = tabCustomView!!.findViewById<TextView>(R.id.tabTV)
        tabTextView.textSize = 15F
        tabTextView.setTextColor(ContextCompat.getColor(tabCustomView.context, R.color.darker_gray))
        tabTextView.setTypeface(null, Typeface.NORMAL)
    }
}