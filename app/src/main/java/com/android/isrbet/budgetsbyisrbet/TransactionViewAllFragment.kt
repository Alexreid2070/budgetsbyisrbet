package com.isrbet.budgetsbyisrbet

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.isrbet.budgetsbyisrbet.databinding.FragmentTransactionViewAllBinding
import com.isrbet.budgetsbyisrbet.MyApplication.Companion.transactionSearchText

class TransactionViewAllFragment : Fragment() {
    private var _binding: FragmentTransactionViewAllBinding? = null
    private val binding get() = _binding!!
//    private val layoutManager: RecyclerView.LayoutManager? = null
//    private val adapter: RecyclerView.Adapter<RecyclerAdapter.ViewHolder>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTransactionViewAllBinding.inflate(inflater, container, false)

        setHasOptionsMenu(true)
        // Inflate the layout for this fragment
        inflater.inflate(R.layout.fragment_transaction_view_all, container, false)
        return binding.root
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        binding.transactionSearch.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val recyclerView: RecyclerView = requireActivity().findViewById(R.id.recycler_view)
                val adapter:TransactionRecyclerAdapter = recyclerView.adapter as TransactionRecyclerAdapter
                adapter.getFilter().filter(newText)
                if (newText != "") {
                    transactionSearchText = newText.toString()
                    binding.transactionSearch.visibility = View.VISIBLE
                }
                return false
            }
        })

//        val eModel = (activity as MainActivity).getMyExpenditureModel()
        super.onViewCreated(itemView, savedInstanceState)
        //binding.recycler_view.apply {
        val recyclerView: RecyclerView = requireActivity().findViewById(R.id.recycler_view)
        recyclerView.apply {
            // set a LinearLayoutManager to handle Android RecyclerView behavior
            recyclerView.layoutManager = LinearLayoutManager(requireActivity())
            var expList = ExpenditureViewModel.getCopyOfExpenditures()
            expList.sortBy { it.date }

            // this nifty line passes a lambda (simple function) to the adapter which is called each time the row is clicked.
            recyclerView.adapter = TransactionRecyclerAdapter(requireContext(), expList) { item ->
                Log.d("Alex", "I clicked item " + item.mykey)
                MyApplication.transactionFirstInList = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

                if (item.type == "T") {
                    val action =
                        TransactionViewAllFragmentDirections.actionViewTransactionsFragmentToTransferFragment()
                            .setTransactionID(item.mykey)
                    this@TransactionViewAllFragment.findNavController().navigate(action)
                }    else {
                    val action =
                        TransactionViewAllFragmentDirections.actionViewTransactionsFragmentToTransactionFragment()
                            .setTransactionID(item.mykey)
                    this@TransactionViewAllFragment.findNavController().navigate(action)
                }
            };
        }
        val adapter:TransactionRecyclerAdapter = recyclerView.adapter as TransactionRecyclerAdapter
        // for some reason "binding.buttonToday.setOnClickListener doesn't work, but the following does
        getView()?.findViewById<Button>(R.id.button_year_forward)?.setOnClickListener { view: View ->
            var getNewPosition = adapter.getPositionOf(
                (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition(),
                "+year")
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(getNewPosition, 0)
        }

        getView()?.findViewById<Button>(R.id.button_month_forward)?.setOnClickListener { view: View ->
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(adapter.getPositionOf(
                (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition(),
                "+month"), 0)
        }

        getView()?.findViewById<Button>(R.id.button_page_forward)?.setOnClickListener { view: View ->
            val firstItem = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            val itemsOnPage = recyclerView.childCount
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(firstItem+itemsOnPage-1, 0)
        }

        getView()?.findViewById<Button>(R.id.button_today)?.setOnClickListener { view: View ->
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(adapter.getPositionOf(
                (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition(),
                "today"), 0)
        }

        getView()?.findViewById<Button>(R.id.button_page_backward)?.setOnClickListener { view: View ->
            val firstItem = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            val itemsOnPage = recyclerView.childCount
            if (firstItem - itemsOnPage < 0)
                (recyclerView.layoutManager as LinearLayoutManager).scrollToPosition(0)
            else
                (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(firstItem-itemsOnPage, 0)
        }

        getView()?.findViewById<Button>(R.id.button_month_backward)?.setOnClickListener { view: View ->
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(adapter.getPositionOf(
                (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition(),
                "-month"), 0)
        }

        getView()?.findViewById<Button>(R.id.button_year_backward)?.setOnClickListener { view: View ->
                (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(adapter.getPositionOf(
                (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition(),
                "-year"), 0)
        }

        view?.findViewById<LinearLayout>(R.id.RecyclerLinearLayout)?.setOnTouchListener(object : OnSwipeTouchListener(requireContext()) {
//        view?.setOnTouchListener(object : OnSwipeTouchListener(requireContext()) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                Log.d("Alex", "swiped left")
                (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(adapter.getPositionOf(
                    (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition(),
                    "+month"), 0)
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                Log.d("Alex", "swiped right")
                (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(adapter.getPositionOf(
                    (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition(),
                    "-month"), 0)
            }
        })

        Log.d("Alex", "transactionSearchText is " + transactionSearchText)
        if (transactionSearchText == "")
            binding.transactionSearch.visibility = View.GONE
        else {
            binding.transactionSearch.visibility = View.VISIBLE
            binding.transactionSearch.setQuery(transactionSearchText, true)
        }
        Log.d("Alex", "transactionFirstInList is " + MyApplication.transactionFirstInList.toString())
        if (MyApplication.transactionFirstInList == 0)
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(adapter.getCount() - 1, 0)
        else
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(MyApplication.transactionFirstInList, 0)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        for (i in 0 until menu.size()) {
            if (menu.getItem(i).getItemId() === R.id.SearchTransaction)
                menu.getItem(i).setVisible(true)
            else
                menu.getItem(i).setVisible(false)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
/*        val navController = findNavController()
            return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)

  */
        if (item.itemId == R.id.SearchTransaction) {
            if (binding.transactionSearch.visibility == View.GONE) {
                binding.transactionSearch.visibility = View.VISIBLE
                val searchView = binding.transactionSearch as SearchView
                searchView.requestFocus()
            } else {
                binding.transactionSearch.visibility = View.GONE
                val recyclerView: RecyclerView = requireActivity().findViewById(R.id.recycler_view)
                val adapter: TransactionRecyclerAdapter = recyclerView.adapter as TransactionRecyclerAdapter
                adapter.getFilter().filter("")
                // clear filter
                val searchView = binding.transactionSearch as SearchView
                searchView.setQuery("", false)
                searchView.clearFocus()
            }
        } else if (item.itemId == android.R.id.home) {
            Log.d("Alex", "i want to go home")
            // for some reason "back" doesn't work from this fragment.  This forces it to work.
            requireActivity().onBackPressed()
        } else
            Log.d("Alex", "what was this " + item.itemId)
        return true
    }

/*
    override fun onResume() {
        super.onResume()
        val recyclerView: RecyclerView = requireActivity().findViewById(R.id.recycler_view)
        recyclerView.adapter!!.notifyDataSetChanged() // doesn't do anything
    }
*/

    override fun onPause() {
        super.onPause()
        hideKeyboard(requireContext(), requireView())
        transactionSearchText = ""
    }
    override fun onDestroyView() {
        super.onDestroyView()
        hideKeyboard(requireContext(), requireView())
        Log.d("Alex", "Leaving Transaction View")
        _binding = null
    }

}