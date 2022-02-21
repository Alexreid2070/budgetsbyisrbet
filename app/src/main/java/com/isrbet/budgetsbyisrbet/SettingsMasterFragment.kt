package com.isrbet.budgetsbyisrbet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.isrbet.budgetsbyisrbet.databinding.FragmentSettingsMasterBinding

class SettingsMasterFragment : Fragment() {
    private var _binding: FragmentSettingsMasterBinding? = null
    private val binding get() = _binding!!
    private lateinit var settingsMasterAdapter: SettingsMasterAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingsMasterBinding.inflate(inflater, container, false)

        settingsMasterAdapter = SettingsMasterAdapter(childFragmentManager, lifecycle)
        settingsMasterAdapter.addFragment(SettingsFragment(), "Settings")
        settingsMasterAdapter.addFragment(CategoryFragment(), "Categories")
        settingsMasterAdapter.addFragment(BudgetFragment(), "Budget")
        settingsMasterAdapter.addFragment(RecurringTransactionFragment(), "Recurring Transactions")
        binding.viewPager.adapter = settingsMasterAdapter

        var tabLayout = binding.tabLayout
/*        tabLayout.addTab(tabLayout.newTab().setText("Preferences"))
        tabLayout.addTab(tabLayout.newTab().setText("Categories"))
        tabLayout.addTab(tabLayout.newTab().setText("Budgets"))
        tabLayout.addTab(tabLayout.newTab().setText("Recurring Transactions"))
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL
        for (i in 0 until binding.tabLayout.tabCount) {
            binding.tabLayout.getTabAt(i)?.tabLabelVisibility = TabLayout.TAB_LABEL_VISIBILITY_LABELED
        } */
        Log.d("Alex", "num of tabs is " + tabLayout.tabCount)
/*        viewPager.addOnPageChangeListener(TabLayoutOnPageChangeListener(tabLayout))
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.viewPager.currentItem = tab.position
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        }) */
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = settingsMasterAdapter.getTabTitle(position)
        }.attach()
        return inflater.inflate(R.layout.fragment_settings_master, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

internal class SettingsMasterAdapter(supportFragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(supportFragmentManager, lifecycle) {
    private val mFragmentList: MutableList<Fragment> = ArrayList()
    private val mFragmentTitleList: MutableList<String> = ArrayList()

    override fun createFragment(position: Int): Fragment {
        Log.d("Alex", "create Fragment called $position")
        return mFragmentList[position]
    }

    override fun getItemCount(): Int {
        Log.d("Alex", "getItemCount called " + mFragmentList.size)
        return mFragmentList.size
    }

    fun getTabTitle(position: Int): String {
        return mFragmentTitleList[position]
    }

    fun addFragment(fragment: Fragment, title: String) {
        // add each fragment and its title to the array list
        mFragmentList.add(fragment)
        mFragmentTitleList.add(title)
    }
}