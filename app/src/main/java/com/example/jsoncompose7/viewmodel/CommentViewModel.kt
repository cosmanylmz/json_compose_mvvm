package com.example.jsoncompose7.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jsoncompose7.model.Comment
import com.example.jsoncompose7.repository.CommentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CommentViewModel(private val repository: CommentRepository) : ViewModel() {
    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    fun fetchComments(postId: Int) {
        viewModelScope.launch {
            _comments.value = repository.getComments(postId)
        }
    }

    fun addComment(comment: Comment) {
        _comments.value += comment
    }
}
