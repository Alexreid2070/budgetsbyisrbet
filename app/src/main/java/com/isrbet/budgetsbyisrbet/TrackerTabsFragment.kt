package com.isrbet.budgetsbyisrbet

import android.content.res.Configuration
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
import com.isrbet.budgetsbyisrbet.databinding.FragmentTrackerTabsBinding
import timber.log.Timber

const val cLINE_CHART_NAME = "Tracker Line Chart"
class TrackerTabsFragment : Fragment() {
    private var _binding: FragmentTrackerTabsBinding? = null
    private val binding get() = _binding!!
    private val args: TrackerTabsFragmentArgs by navArgs()
    private var activeTabName = ""


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackerTabsBinding.inflate(inflater, container, false)

        val adapter = TabsAdapter(activity)
        val barFr = TrackerFragment().apply {
            arguments = Bundle().apply {
                putString("type", "Bar")
            }
        }
        adapter.addFragment(barFr, "Tracker Bar Chart")
        val pieFr = TrackerFragment().apply {
            arguments = Bundle().apply {
                putString("type", "Pie")
            }
        }
        adapter.addFragment(pieFr, "Tracker Pie Chart")
        val lineFr = TrackerLineChartFragment().apply {
            arguments = Bundle().apply {
                putInt("categoryID", args.categoryID)
            }
        }
        adapter.addFragment(lineFr, cLINE_CHART_NAME)

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
        inflater.inflate(R.layout.fragment_tracker_tabs, container, false)
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
        activeTabName = tab.text.toString()
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
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (activeTabName == cLINE_CHART_NAME)
        setTabLayout()
    }
    private fun setTabLayout() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.tabLayout.visibility = View.GONE
            binding.viewPager.setPadding(0, 0, 0, 0)
            (activity as MainActivity).setBottomNavBarVisibility(false)
            MyApplication.displayToast(getString(R.string.you_have_entered_full_screen_mode))
        } else {
            binding.tabLayout.visibility = View.VISIBLE
            binding.viewPager.setPadding(0, 70, 0, 70)
            (activity as MainActivity).setBottomNavBarVisibility(true)
        }
    }
}