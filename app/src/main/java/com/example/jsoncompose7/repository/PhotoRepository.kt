package com.example.jsoncompose7.repository

import com.example.jsoncompose7.model.ApiService
import com.example.jsoncompose7.model.Photo

class PhotoRepository(private val apiService: ApiService) {
    suspend fun getPhotos(): List<Photo> {
        return apiService.getPhotos()
    }
}