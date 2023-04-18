package com.isrbet.budgetsbyisrbet

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.isrbet.budgetsbyisrbet.databinding.FragmentDashboardTabsBinding

class DashboardTabsFragment : Fragment() {
    private var _binding: FragmentDashboardTabsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardTabsBinding.inflate(inflater, container, false)

        val adapter = DashboardTabsAdapter(activity)
        adapter.addFragment(DashboardFragment(), "Dashboard")
        adapter.addFragment(YearOverYearFragment(), "Year Over Year")

        binding.viewPager.adapter = adapter
        binding.viewPager.currentItem = DefaultsViewModel.getDefaultLastDashboardTab()
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = adapter.getTabTitle(position)
        }.attach()
        binding.viewPager.adapter = adapter
        for (i in 0 until binding.tabLayout.tabCount) {
            val tab = binding.tabLayout.getTabAt(i)
            tab?.customView = createCustomTabView(tab?.text.toString(), 15, android.R.color.black)
            if (tab != null)
                if (i == 0)
                    setTabActive(tab)
                else
                    setTabInactive(tab)
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    if (tab.text.toString() == "Dashboard")
                        DefaultsViewModel.updateDefaultInt(cDEFAULT_LAST_DASHBOARD_TAB, 0)
                    else
                        DefaultsViewModel.updateDefaultInt(cDEFAULT_LAST_DASHBOARD_TAB, 1)
                    setTabActive(tab)
                }
//                tab?.customView = createCustomTabView(tab?.text.toString(), 15, android.R.color.black)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    setTabInactive(tab)
                }
//                tab?.customView = createCustomTabView(tab?.text.toString(), 10, android.R.color.darker_gray)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

        })

        // Inflate the layout for this fragment
        inflater.inflate(R.layout.fragment_dashboard_tabs, container, false)
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
        tabTextView.textSize = 17F
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