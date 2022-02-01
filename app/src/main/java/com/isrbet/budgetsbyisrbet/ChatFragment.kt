package com.isrbet.budgetsbyisrbet

import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import android.widget.ListView
import android.widget.Toast
import com.isrbet.budgetsbyisrbet.databinding.FragmentChatBinding

class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        // Inflate the layout for this fragment
        inflater.inflate(R.layout.fragment_chat, container, false)
        return binding.root
    }

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        val adapter = ChatAdapter(requireContext(), ChatViewModel.getCopyOfChats())
        val listView: ListView = requireActivity().findViewById(R.id.chat_list_view)
        listView.adapter = adapter
        listView.setSelection(listView.count-1)

        binding.sendIcon.setOnClickListener {
            sendChat()
        }
        binding.chatInput.setOnEditorActionListener { _, actionID, _ ->
            Log.d("Alex", "actionID is $actionID")
            when (actionID) {
                EditorInfo.IME_ACTION_DONE -> { sendChat(); true}
                else -> false
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        for (i in 0 until menu.size()) {
            menu.getItem(i).isVisible = false
        }
    }

    override fun onPause() {
        super.onPause()
        val dateNow = Calendar.getInstance()
        MyApplication.database.getReference("Users/"+MyApplication.userUID)
            .child("Info")
            .child("LastReadChats")
            .child("date")
            .setValue(giveMeMyDateFormat(dateNow))
        MyApplication.lastReadChatsDate = giveMeMyDateFormat(dateNow)
        MyApplication.database.getReference("Users/"+MyApplication.userUID)
            .child("Info")
            .child("LastReadChats")
            .child("time").setValue(giveMeMyTimeFormat(dateNow))
        MyApplication.lastReadChatsTime = giveMeMyTimeFormat(dateNow)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun sendChat() {
        if (!textIsSafe(binding.chatInput.text.toString())) {
//            showErrorMessage(getParentFragmentManager(), "The text contains unsafe characters.  They must be removed.")
            binding.chatInput.error="The text contains unsafe characters!"
            focusAndOpenSoftKeyboard(requireContext(), binding.chatInput)
            return
        }
        val dateNow = Calendar.getInstance()
        val tChat = Chat(
            giveMeMyDateFormat(dateNow),
            giveMeMyTimeFormat(dateNow),
            MyApplication.userGivenName,
            binding.chatInput.text.toString())
        ChatViewModel.addChat(tChat)
        val adapter = ChatAdapter(requireContext(), ChatViewModel.getCopyOfChats())
        val listView: ListView = requireActivity().findViewById(R.id.chat_list_view)
        listView.adapter = adapter
        adapter.notifyDataSetChanged()
        listView.setSelection(listView.count-1)
        binding.chatInput.setText("")
        hideKeyboard(requireContext(), requireView())
        Toast.makeText(activity, "Chat added", Toast.LENGTH_SHORT).show()
        MyApplication.playSound(context, R.raw.impact_jaw_breaker)
    }
}
