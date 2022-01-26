package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import com.google.android.material.navigation.NavigationView
import com.isrbet.budgetsbyisrbet.databinding.FragmentHomeBinding
import com.isrbet.budgetsbyisrbet.databinding.FragmentTransactionBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        inflater.inflate(R.layout.fragment_home, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.quoteField.text = MyApplication.getQuote()

        view.findViewById<Button>(R.id.expenditure_button)
            ?.setOnClickListener { iview: View ->
                iview.findNavController().navigate(R.id.TransactionFragment)
            }
        view.findViewById<Button>(R.id.view_all_button)
            ?.setOnClickListener { iview: View ->
                iview.findNavController().navigate(R.id.ViewTransactionsFragment)
            }
        view.findViewById<Button>(R.id.dashboard_button)
            ?.setOnClickListener { iview: View ->
                iview.findNavController().navigate(R.id.DashboardFragment)
            }
        view.findViewById<TextView>(R.id.quote_field)?.setOnTouchListener(object :
            OnSwipeTouchListener(requireContext()) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                Log.d("Alex", "swiped left")
                val navController = view.findNavController()
                navController.navigate(R.id.DashboardFragment)
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                Log.d("Alex", "swiped right")
                val navController = view.findNavController()
                navController.navigate(R.id.ViewTransactionsFragment)
            }

            override fun onSwipeBottom() {
                super.onSwipeBottom()
                Log.d("Alex", "swiped bottom, want to show menu")
                (activity as MainActivity).openDrawer()
            }

            override fun onSwipeTop() {
                super.onSwipeTop()
                Log.d("Alex", "swiped top, want to go to Add Transaction")
                val navController = view.findNavController()
                navController.navigate(R.id.TransactionFragment)
            }
        })

        alignExpenditureMenuWithDataState()

        CategoryViewModel.singleInstance.setCallback(object: CategoryDataUpdatedCallback {
            override fun onDataUpdate() {
                alignExpenditureMenuWithDataState()
            }
        })
        SpenderViewModel.singleInstance.setCallback(object: SpenderDataUpdatedCallback {
            override fun onDataUpdate() {
                alignExpenditureMenuWithDataState()
                if (SpenderViewModel.singleUser()) {
                    (activity as MainActivity).singleUserMode()
                }
            }
        })
    }

    private fun alignExpenditureMenuWithDataState() {
        if (MyApplication.userUID != "")
            binding.homeScreenMessage.text = ""

        if (MyApplication.userUID != "" && CategoryViewModel.getCount() > 0 && SpenderViewModel.getActiveCount() > 0) {
            Log.d("Alex", "true")
            binding.expenditureButton.visibility = View.VISIBLE
            binding.viewAllButton.visibility = View.VISIBLE
            binding.dashboardButton.visibility = View.VISIBLE
            val mDrawerLayout = view?.findViewById<DrawerLayout>(R.id.drawer_layout)
            if (mDrawerLayout != null) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            }
            binding.quoteField.visibility = View.VISIBLE
            binding.homeScreenMessage.text = ""
        } else {
            Log.d("Alex", "false")
            binding.expenditureButton.visibility = View.GONE
            binding.viewAllButton.visibility = View.GONE
            binding.dashboardButton.visibility = View.GONE
            val mDrawerLayout = view?.findViewById<DrawerLayout>(R.id.drawer_layout)
            mDrawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            binding.quoteField.visibility = View.GONE
            binding.homeScreenMessage.text = "You must sign in using your Google account to proceed.  Click below to continue."

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CategoryViewModel.singleInstance.clearCallback()
        SpenderViewModel.singleInstance.clearCallback()
        _binding = null
    }
}