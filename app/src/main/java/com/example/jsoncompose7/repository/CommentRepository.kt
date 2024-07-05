package com.example.jsoncompose7.repository

import com.example.jsoncompose7.model.ApiService
import com.example.jsoncompose7.model.Comment

class CommentRepository(private val apiService: ApiService) {
    suspend fun getComments(postId: Int): List<Comment> {
        return apiService.getComments(postId)
    }
}
