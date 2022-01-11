package com.isrbet.budgetsbyisrbet

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.navigation.NavigationView
import kotlin.random.Random

class HomeFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val quote = requireActivity().findViewById(R.id.quote_field) as TextView

        quote.setText(MyApplication.getQuote())

        val account = GoogleSignIn.getLastSignedInAccount(requireActivity())
        if (account?.email == null) {
            requireActivity().findViewById<Button>(R.id.expenditure_button).visibility = View.GONE
            requireActivity().findViewById<Button>(R.id.view_all_button).visibility = View.GONE
            requireActivity().findViewById<Button>(R.id.dashboard_button).visibility = View.GONE
            requireActivity().findViewById<TextView>(R.id.quote_field).visibility = View.GONE
        } else {
            getView()?.findViewById<Button>(R.id.expenditure_button)
                ?.setOnClickListener { view: View ->
                    view?.findNavController()?.navigate(R.id.TransactionFragment)
                }
            getView()?.findViewById<Button>(R.id.view_all_button)
                ?.setOnClickListener { view: View ->
                    view?.findNavController()?.navigate(R.id.ViewTransactionsFragment)
                }
            getView()?.findViewById<Button>(R.id.dashboard_button)
                ?.setOnClickListener { view: View ->
                    view?.findNavController()?.navigate(R.id.DashboardFragment)
                }
            getView()?.findViewById<TextView>(R.id.quote_field)?.setOnTouchListener(object : OnSwipeTouchListener(requireContext()) {
                override fun onSwipeLeft() {
                    super.onSwipeLeft()
                    Log.d("Alex", "swiped left")
                }
                override fun onSwipeRight() {
                    super.onSwipeRight()
                    Log.d("Alex", "swiped right")
                }
                override fun onSwipeBottom() {
                    super.onSwipeBottom()
                    Log.d("Alex", "swiped bottom, want to show menu")
                    (activity as MainActivity?)?.openDrawer()
                }

                override fun onSwipeTop() {
                    super.onSwipeTop()
                    Log.d("Alex", "swiped top, want to go to Add Transaction")
                    view?.findNavController()?.navigate(R.id.TransactionFragment)
                }
            })
        }

        alignExpenditureMenuWithDataState() // ie turn off Expenditure menu item until data has arrived
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

    fun alignExpenditureMenuWithDataState() {
        val navigationView = requireActivity().findViewById<NavigationView>(R.id.nav_view)
        val menuNav = navigationView.getMenu()
        val expMenu = menuNav.findItem(R.id.TransactionFragment);

        if (CategoryViewModel.getCount() > 0 && SpenderViewModel.getCount() > 0) {
            requireActivity().findViewById<Button>(R.id.expenditure_button).visibility = View.VISIBLE
            requireActivity().findViewById<Button>(R.id.view_all_button).visibility = View.VISIBLE
            requireActivity().findViewById<Button>(R.id.dashboard_button).visibility = View.VISIBLE
            expMenu.setEnabled(true)
        } else {
            requireActivity().findViewById<Button>(R.id.expenditure_button).visibility = View.GONE
            requireActivity().findViewById<Button>(R.id.view_all_button).visibility = View.GONE
            requireActivity().findViewById<Button>(R.id.dashboard_button).visibility = View.GONE
            expMenu.setEnabled(false)
        }
    }

    fun ivebeentold() {
        Log.d("Alex", "ive been told")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        CategoryViewModel.singleInstance.clearCallback()
        SpenderViewModel.singleInstance.clearCallback()
    }
}