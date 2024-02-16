package com.example.instaapp.ui.search

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.instaapp.R
import com.example.instaapp.adapters.OnFriendClicked
import com.example.instaapp.mvvm.ViewModel
import com.example.instaapp.adapters.UsersAdapter
import com.example.instaapp.modal.User

class DashboardFragment : Fragment(), OnFriendClicked {

    private lateinit var searchEditText: EditText
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var usersAdapter: UsersAdapter
    private var isSearching = false
    private lateinit var viewModel: ViewModel

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        searchEditText = view.findViewById(R.id.searchEditText)
        searchResultsRecyclerView = view.findViewById(R.id.searchResultsRecyclerView)

        val layoutManager = LinearLayoutManager(context)
        usersAdapter = UsersAdapter(requireContext())
        searchResultsRecyclerView.layoutManager = layoutManager
        searchResultsRecyclerView.adapter = usersAdapter

        viewModel = ViewModelProvider(requireActivity()).get(ViewModel::class.java)

        viewModel.getSearchResults().observe(viewLifecycleOwner, Observer { users ->
            usersAdapter.setUserLIST(users)
            usersAdapter.notifyDataSetChanged()
        })


        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val searchText = s.toString().trim()
                if (searchText.isEmpty()) {
                    viewModel.getAllUsers()
                } else {
                    viewModel.performSearch(searchText)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })

        viewModel.getAllUsers().observe(viewLifecycleOwner, Observer { users ->
            usersAdapter.setUserLIST(users)
            usersAdapter.notifyDataSetChanged()
        })

        usersAdapter.setListener(this)

        return view
    }

    override fun onfriendListener(position: Int, user: User) {

    }
}

