package com.isrbet.budgetsbyisrbet

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val quote = requireActivity().findViewById(R.id.quote_field) as TextView

        quote.text = MyApplication.getQuote()

            view.findViewById<Button>(R.id.expenditure_button)
                ?.setOnClickListener { view: View ->
                    view.findNavController().navigate(R.id.TransactionFragment)
                }
            view.findViewById<Button>(R.id.view_all_button)
                ?.setOnClickListener { view: View ->
                    view.findNavController().navigate(R.id.ViewTransactionsFragment)
                }
            view.findViewById<Button>(R.id.dashboard_button)
                ?.setOnClickListener { view: View ->
                    view.findNavController().navigate(R.id.DashboardFragment)
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

        CategoryViewModel.singleInstance.setCallback(object: CategoryDataUpdatedCallback {
            override fun onDataUpdate() {
                alignExpenditureMenuWithDataState()
            }
        })
        SpenderViewModel.singleInstance.setCallback(object: SpenderDataUpdatedCallback {
            override fun onDataUpdate() {
                alignExpenditureMenuWithDataState()
            }
        })
        super.onViewCreated(view, savedInstanceState)
    }

    private fun alignExpenditureMenuWithDataState() {
        if (MyApplication.userUID != "" && CategoryViewModel.getCount() > 0 && SpenderViewModel.getCount() > 0) {
            Log.d("Alex", "true")
            view?.findViewById<Button>(R.id.expenditure_button)?.visibility = View.VISIBLE
            view?.findViewById<Button>(R.id.view_all_button)?.visibility = View.VISIBLE
            view?.findViewById<Button>(R.id.dashboard_button)?.visibility = View.VISIBLE
            val mDrawerLayout = view?.findViewById<DrawerLayout>(R.id.drawer_layout)
            if (mDrawerLayout != null) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            }
        } else {
            Log.d("Alex", "false")
            view?.findViewById<Button>(R.id.expenditure_button)?.visibility = View.GONE
            view?.findViewById<Button>(R.id.view_all_button)?.visibility = View.GONE
            view?.findViewById<Button>(R.id.dashboard_button)?.visibility = View.GONE
            val mDrawerLayout = view?.findViewById<DrawerLayout>(R.id.drawer_layout)
            mDrawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CategoryViewModel.singleInstance.clearCallback()
        SpenderViewModel.singleInstance.clearCallback()
    }
}