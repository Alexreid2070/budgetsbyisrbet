package com.isrbet.budgetsbyisrbet

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.isrbet.budgetsbyisrbet.databinding.FragmentCategoryDetailsBinding
import yuku.ambilwarna.AmbilWarnaDialog
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener


class CategoryDetailsFragment : Fragment() {
    private var _binding: FragmentCategoryDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentCategoryDetailsBinding.inflate(inflater, container, false)
        inflater.inflate(R.layout.fragment_category, container, false)

        linearLayoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = linearLayoutManager

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter =
            CategoryDetailsAdapter(requireContext(), DefaultsViewModel.getCategoryDetails(), { item ->
                // this is called when a row is clicked
                openColorPickerDialogue(item.name) },
                { item ->
                    resetColor(item.name) }
            )

        val listView: RecyclerView = requireActivity().findViewById(R.id.recycler_view)
        listView.adapter = adapter

        val callback = object :
            ItemTouchHelper.SimpleCallback(ItemTouchHelper.DOWN or ItemTouchHelper.UP, 0) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition
                val lAdapter: CategoryDetailsAdapter =
                    recyclerView.adapter as CategoryDetailsAdapter
                lAdapter.notifyItemMoved(fromPos, toPos)

                DefaultsViewModel.reorderCategory(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                return
            }
        }
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(binding.recyclerView)
    }

    private fun resetColor(iCategory: String) {
        DefaultsViewModel.setColour(iCategory, 0, false)
        val lAdapter: CategoryDetailsAdapter =
            binding.recyclerView.adapter as CategoryDetailsAdapter
        lAdapter.refresh()
    }
    private fun openColorPickerDialogue(iCategory: String) {

        // the callback needs 3 parameters
        // one is the context, second is default color,
        DefaultsViewModel.getCategoryDetail(iCategory).color.let {
            AmbilWarnaDialog(
                requireContext(), it,
                object : OnAmbilWarnaListener {
                    override fun onCancel(dialog: AmbilWarnaDialog) {
                        // do nothing
                    }

                    override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
                        DefaultsViewModel.setColour(iCategory, color, false)
                        val lAdapter: CategoryDetailsAdapter =
                            binding.recyclerView.adapter as CategoryDetailsAdapter
                        lAdapter.refresh()
                    }
                })
        }.show()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class CategoryDetailsAdapter (private val context: Context, private var data: MutableList<CategoryDetail>,
                              private val colorListener: (CategoryDetail) -> Unit = {},
                              private val resetListener: (CategoryDetail) -> Unit = {}):
    RecyclerView.Adapter<CategoryDetailsAdapter.CategoryDetailViewHolder>() {

    override fun getItemId(pos: Int): Long {
        return pos.toLong()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refresh() {
        data = DefaultsViewModel.getCategoryDetails()
        notifyDataSetChanged()
    }

    // class for holding the cached view
    class CategoryDetailViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var vhName: TextView = view.findViewById(R.id.row_category_name)
        var vhColor: ImageView = view.findViewById(R.id.row_category_color)
        var vhReset: ImageView = view.findViewById(R.id.row_category_color_reset)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryDetailViewHolder {
        val view =
            LayoutInflater.from(context).inflate(R.layout.row_category_details, parent, false)
        return CategoryDetailViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryDetailViewHolder, position: Int) {
        val cData = data[position]

        holder.vhName.text = cData.name
        holder.vhName.setBackgroundColor(cData.color)
        holder.vhColor.setBackgroundColor(cData.color)
        holder.vhReset.setBackgroundColor(cData.color)
//        holder.itemView.setOnClickListener { colorListener(cData) }
        holder.vhColor.setOnClickListener { colorListener(cData) }
        holder.vhReset.setOnClickListener { resetListener(cData) }
    }

    override fun getItemCount(): Int {
        return data.size
    }
}