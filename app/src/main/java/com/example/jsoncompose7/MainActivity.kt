package com.example.jsoncompose7

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import com.example.jsoncompose7.model.ApiService
import com.example.jsoncompose7.model.Comment
import com.example.jsoncompose7.model.Photo
import com.example.jsoncompose7.repository.CommentRepository
import com.example.jsoncompose7.repository.PhotoRepository
import com.example.jsoncompose7.ui.theme.JsonCompose7Theme
import com.example.jsoncompose7.viewmodel.CommentViewModel
import com.example.jsoncompose7.viewmodel.PhotoViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    private lateinit var analytics: FirebaseAnalytics
    private val photoViewModel: PhotoViewModel by viewModels {
        PhotoViewModelFactory(PhotoRepository(ApiService.create()))
    }

    private val commentViewModel: CommentViewModel by viewModels {
        CommentViewModelFactory(CommentRepository(ApiService.create()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analytics = Firebase.analytics
        setContent {
            JsonCompose7Theme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "photo_list") {
                    composable("photo_list") {
                        PhotoListScreen(photoViewModel, navController, analytics)
                    }
                    composable("photo_detail/{photoId}") { backStackEntry ->
                        val photoId = backStackEntry.arguments?.getString("photoId")?.toInt() ?: return@composable
                        PhotoDetailScreen(photoId, photoViewModel, commentViewModel, analytics)
                    }
                }
            }
        }
    }
}

class PhotoViewModelFactory(private val repository: PhotoRepository) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhotoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PhotoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class CommentViewModelFactory(private val repository: CommentRepository) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CommentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CommentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoListScreen(photoViewModel: PhotoViewModel, navController: NavHostController, analytics: FirebaseAnalytics) {
    val photos by photoViewModel.photos.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photo Album") }
            )
        },
        content = { paddingValues ->
            PhotoGrid(photos, navController, analytics, Modifier.padding(paddingValues))
        }
    )
}

@Composable
fun PhotoGrid(photos: List<Photo>, navController: NavHostController, analytics: FirebaseAnalytics, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(photos) { photo ->
            PhotoCard(photo, navController, analytics)
        }
    }
}

@Composable
fun PhotoCard(photo: Photo, navController: NavHostController, analytics: FirebaseAnalytics) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
            .height(125.dp)
            .clickable {
                navController.navigate("photo_detail/${photo.id}")
                // Firebase Analytics'e olayı gönder
                val bundle = Bundle().apply {
                    putString(FirebaseAnalytics.Param.ITEM_ID, photo.id.toString())
                    putString(FirebaseAnalytics.Param.ITEM_NAME, photo.title)
                    putString(FirebaseAnalytics.Param.CONTENT_TYPE, "photo")
                }
                analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Image(
                painter = rememberImagePainter(
                    data = photo.thumbnailUrl,
                    builder = {
                        crossfade(true)
                        placeholder(R.drawable.ic_placeholder)
                        error(R.drawable.ic_placeholder)
                    }
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(50.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.fillMaxSize()) {
                Text(text = photo.title, style = MaterialTheme.typography.bodyMedium)
                Text(text = "Album ID: ${photo.albumId}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(photoId: Int, photoViewModel: PhotoViewModel, commentViewModel: CommentViewModel, analytics: FirebaseAnalytics) {
    val comments by commentViewModel.comments.collectAsState()
    LaunchedEffect(photoId) {
        commentViewModel.fetchComments(photoId)
    }
    val photo = photoViewModel.photos.value.firstOrNull { it.id == photoId }

    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AddCommentDialog(photoId, commentViewModel, { showDialog = false }, analytics)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photo Details") },
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Comment", modifier = Modifier.size(24.dp))
                        Text("Add Comment", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            )
        },
        content = { paddingValues ->
            photo?.let { photo ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    Text(text = "Title: ${photo.title}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Album ID: ${photo.albumId}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        painter = rememberImagePainter(
                            data = photo.url,
                            builder = {
                                crossfade(true)
                                placeholder(R.drawable.ic_placeholder)
                                error(R.drawable.ic_placeholder)
                            }
                        ),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Comments", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    CommentSection(comments)
                }
            } ?: run {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    )
}

@Composable
fun AddCommentDialog(photoId: Int, commentViewModel: CommentViewModel, onDismiss: () -> Unit, analytics: FirebaseAnalytics) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                if (name.isNotEmpty() && email.isNotEmpty() && body.isNotEmpty()) {
                    val newComment = Comment(postId = photoId, id = 0, name = name, email = email, body = body)
                    commentViewModel.addComment(newComment)
                    // Firebase Analytics'e olayı gönder
                    val bundle = Bundle().apply {
                        putString("photo_id", photoId.toString())
                        putString("comment_name", name)
                    }
                    analytics.logEvent("add_comment", bundle)
                    onDismiss()
                }
            }) {
                Text("Add Comment")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Add Comment") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") }
                )
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") }
                )
                TextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Comment") }
                )
            }
        }
    )
}

@Composable
fun CommentSection(comments: List<Comment>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(comments) { comment ->
            CommentCard(comment)
        }
    }
}

@Composable
fun CommentCard(comment: Comment) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
            .height(125.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = comment.name, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = comment.body, style = MaterialTheme.typography.bodySmall)
        }
    }
}
