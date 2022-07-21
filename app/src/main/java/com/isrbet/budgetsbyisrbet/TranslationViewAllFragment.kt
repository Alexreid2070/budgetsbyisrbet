package com.isrbet.budgetsbyisrbet

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.isrbet.budgetsbyisrbet.databinding.FragmentTranslationViewAllBinding

class TranslationViewAllFragment : Fragment() {
    private var _binding: FragmentTranslationViewAllBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentTranslationViewAllBinding.inflate(inflater, container, false)
        inflater.inflate(R.layout.fragment_translation_view_all, container, false)
        return binding.root
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        val adapter = TranslationAdapter(requireContext(), TranslationViewModel.getTranslations())

        val listView: ListView = requireActivity().findViewById(R.id.translation_list_view)
        listView.adapter = adapter

        listView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ -> // value of item that is clicked
                val itemValue = listView.getItemAtPosition(position) as Translation
                val cdf = TranslationEditDialogFragment.newInstance(itemValue.before, itemValue.after, itemValue.key) // what do I pass here? zzz
                cdf.setTranslationEditDialogFragmentListener(object: TranslationEditDialogFragment.TranslationEditDialogFragmentListener {
                    override fun onNewDataSaved() {
                        val myAdapter = TranslationAdapter(requireContext(), TranslationViewModel.getTranslations())
                        listView.adapter = myAdapter
                        myAdapter.notifyDataSetChanged()
                    }
                })
                cdf.show(parentFragmentManager, getString(R.string.edit_translation))
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
