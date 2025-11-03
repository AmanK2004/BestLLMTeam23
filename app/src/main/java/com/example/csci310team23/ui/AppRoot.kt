package com.example.csci310team23.ui

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.csci310team23.AppGraph
import com.example.csci310team23.data.model.Comment
import com.example.csci310team23.data.model.Post
import com.example.csci310team23.data.model.PostSearchType
import com.example.csci310team23.data.model.Prompt
import com.example.csci310team23.data.model.UserProfile
import com.example.csci310team23.data.model.formatRelative
import java.time.LocalDate
import java.util.Calendar

// Common AI agents list
private val AI_AGENTS = listOf(
    "ChatGPT-4",
    "ChatGPT-3.5",
    "Claude 3.5 Sonnet",
    "Claude 3 Opus",
    "Gemini Pro",
    "Gemini Advanced",
    "GPT-4 Turbo",
    "Llama 3",
    "Mistral",
    "Perplexity AI"
)

@Composable
fun AppRoot(
    viewModel: AppViewModel = viewModel(factory = AppViewModel.provideFactory(AppGraph.repository))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentUser = uiState.currentUser

    LaunchedEffect(uiState.errorMessage, uiState.infoMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
        uiState.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    when {
        !authState.hasSeenLanding -> {
            LandingScreen(onGetStarted = viewModel::completeLanding)
        }

        currentUser == null -> {
            AuthScreen(
                authState = authState,
                onModeChange = viewModel::setAuthMode,
                onSignIn = { email, password -> viewModel.signIn(email, password) },
                onRegister = { name, email, studentId, password ->
                    viewModel.register(name, email, studentId, password)
                }
            )
        }

        uiState.requiresProfileSetup -> {
            currentUser.let { user ->
                ProfileSetupScreen(
                    user = user,
                    onComplete = { department, school, birthDate, bio ->
                        viewModel.completeProfile(department, school, birthDate, bio)
                    },
                    onLogout = viewModel::logout
                )
            }
        }

        else -> {
            currentUser.let { user ->
                MainScreen(
                    currentUser = user,
                    uiState = uiState,
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}

@Composable
private fun LandingScreen(onGetStarted: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🤖",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Text(
                text = "USC LLM Community",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = "Connect with fellow Trojans\nShare LLM experiences & prompts",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 48.dp)
            )
            Button(
                onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Get Started", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthScreen(
    authState: AuthUiState,
    onModeChange: (AuthMode) -> Unit,
    onSignIn: (String, String) -> Unit,
    onRegister: (String, String, String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var studentId by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "USC LLM Community",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = authState.mode == AuthMode.SIGN_IN,
                onClick = {
                    localError = null
                    onModeChange(AuthMode.SIGN_IN)
                },
                label = { Text("Sign In") }
            )
            FilterChip(
                selected = authState.mode == AuthMode.REGISTER,
                onClick = {
                    localError = null
                    onModeChange(AuthMode.REGISTER)
                },
                label = { Text("Register") }
            )
        }

        Spacer(modifier = Modifier.size(24.dp))

        if (authState.mode == AuthMode.REGISTER) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
            Spacer(Modifier.size(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("USC Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.9f)
        )
        Spacer(Modifier.size(12.dp))

        if (authState.mode == AuthMode.REGISTER) {
            OutlinedTextField(
                value = studentId,
                onValueChange = { studentId = it.filter { ch -> ch.isDigit() }.take(10) },
                label = { Text("Student ID (10 digits)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
            Spacer(Modifier.size(12.dp))
        }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.9f),
            visualTransformation = PasswordVisualTransformation()
        )

        if (authState.mode == AuthMode.REGISTER) {
            Spacer(Modifier.size(12.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.9f),
                visualTransformation = PasswordVisualTransformation()
            )
        }

        Spacer(modifier = Modifier.size(24.dp))

        val errorMessage = localError ?: authState.errorMessage
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Button(
            enabled = !authState.isProcessing,
            onClick = {
                if (authState.mode == AuthMode.SIGN_IN) {
                    if (email.isBlank() || password.isBlank()) {
                        localError = "Email and password are required"
                        return@Button
                    }
                    localError = null
                    onSignIn(email.trim(), password)
                } else {
                    if (name.isBlank() || email.isBlank() || studentId.length != 10 || password.isBlank()) {
                        localError = "Please fill in all registration fields with valid values"
                        return@Button
                    }
                    if (password != confirmPassword) {
                        localError = "Passwords do not match"
                        return@Button
                    }
                    localError = null
                    onRegister(name.trim(), email.trim(), studentId.trim(), password)
                }
            },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Text(if (authState.mode == AuthMode.SIGN_IN) "Sign In" else "Register")
        }
    }
}

@Composable
private fun ProfileSetupScreen(
    user: UserProfile,
    onComplete: (String, String, LocalDate?, String) -> Unit,
    onLogout: () -> Unit
) {
    var department by rememberSaveable { mutableStateOf(user.department) }
    var school by rememberSaveable { mutableStateOf(user.school) }
    var birthDate by rememberSaveable { mutableStateOf(user.birthDate) }
    var bio by rememberSaveable { mutableStateOf(user.bio) }
    var error by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    birthDate?.let {
        calendar.set(it.year, it.monthValue - 1, it.dayOfMonth)
    }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                birthDate = LocalDate.of(year, month + 1, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Complete Your Profile",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = department,
            onValueChange = { department = it },
            label = { Text("Department") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = school,
            onValueChange = { school = it },
            label = { Text("School") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedButton(
            onClick = { datePickerDialog.show() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(birthDate?.toString() ?: "Select Birth Date")
        }

        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
        )

        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(onClick = {
            if (department.isBlank() || school.isBlank()) {
                error = "Affiliation information is required"
                return@Button
            }
            onComplete(department.trim(), school.trim(), birthDate, bio.trim())
        }) {
            Text("Save Profile")
        }
        OutlinedButton(onClick = onLogout) {
            Text("Sign Out")
        }
    }
}

private enum class MainSection(val title: String) {
    FEED("Feed"),
    TRENDING("Trending"),
    PROMPTS("Prompts"),
    SEARCH("Search"),
    PROFILE("Profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    currentUser: UserProfile,
    uiState: AppUiState,
    viewModel: AppViewModel,
    snackbarHostState: SnackbarHostState
) {
    var selectedSection by rememberSaveable { mutableStateOf(MainSection.TRENDING) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("USC LLM Community") }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                MainSection.values().forEach { section ->
                    val icon = when (section) {
                        MainSection.FEED -> Icons.Filled.Home
                        MainSection.TRENDING -> Icons.Outlined.Whatshot
                        MainSection.PROMPTS -> Icons.Outlined.Article
                        MainSection.SEARCH -> Icons.Filled.Search
                        MainSection.PROFILE -> Icons.Filled.Person
                    }
                    NavigationBarItem(
                        selected = section == selectedSection,
                        onClick = { selectedSection = section },
                        icon = { Icon(icon, contentDescription = section.title) },
                        label = { Text(section.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedSection) {
            MainSection.FEED -> FeedSection(
                modifier = Modifier.padding(innerPadding),
                currentUser = currentUser,
                posts = uiState.posts,
                onCreatePost = { title, body, tag, isDraft ->
                    viewModel.createPost(title, body, tag, isDraft)
                },
                onUpdatePost = viewModel::updatePost,
                onDeletePost = viewModel::deletePost,
                onPublishDraft = viewModel::publishDraft,
                onCreateComment = viewModel::createComment,
                onUpdateComment = viewModel::updateComment,
                onVotePost = viewModel::voteOnPost,
                onVoteComment = viewModel::voteOnComment,
                onWatchTag = viewModel::watchTag,
                onWatchUser = viewModel::watchUser,
                watchedTags = uiState.watchedTags,
                watchedUsers = uiState.watchedUsers
            )

            MainSection.TRENDING -> TrendingSection(
                modifier = Modifier.padding(innerPadding),
                posts = uiState.trending,
                currentUser = currentUser,
                onVotePost = viewModel::voteOnPost,
                onVoteComment = viewModel::voteOnComment
            )

            MainSection.PROMPTS -> PromptSection(
                modifier = Modifier.padding(innerPadding),
                currentUser = currentUser,
                prompts = uiState.prompts,
                onCreatePrompt = { title, description, content, tag, isPrivate ->
                    viewModel.createPrompt(title, description, content, tag, isPrivate)
                },
                onUpdatePrompt = { id, title, description, content, tag, isPrivate ->
                    viewModel.updatePrompt(id, title, description, content, tag, isPrivate)
                },
                onDeletePrompt = viewModel::deletePrompt
            )

            MainSection.SEARCH -> SearchSection(
                modifier = Modifier.padding(innerPadding),
                posts = uiState.searchState.postResults,
                prompts = uiState.searchState.promptResults,
                users = uiState.searchState.userResults,
                onSearchPosts = viewModel::searchPosts,
                onSearchPrompts = viewModel::searchPrompts,
                onSearchUsers = viewModel::searchUsers
            )

            MainSection.PROFILE -> ProfileSection(
                modifier = Modifier.padding(innerPadding),
                user = currentUser,
                onUpdateProfile = viewModel::updateProfile,
                onResetPassword = viewModel::resetPassword,
                onLogout = viewModel::logout
            )
        }
    }
}

@Composable
private fun FeedSection(
    modifier: Modifier = Modifier,
    currentUser: UserProfile?,
    posts: List<Post>,
    onCreatePost: (String, String, String, Boolean) -> Unit,
    onUpdatePost: (Long, String, String, String) -> Unit,
    onDeletePost: (Long) -> Unit,
    onPublishDraft: (Long) -> Unit,
    onCreateComment: (Long, String?, String) -> Unit,
    onUpdateComment: (Long, String?, String) -> Unit,
    onVotePost: (Long, Int) -> Unit,
    onVoteComment: (Long, Int) -> Unit,
    onWatchTag: (String) -> Unit,
    onWatchUser: (Long) -> Unit,
    watchedTags: Set<String>,
    watchedUsers: Set<Long>
) {
    var showCreatePost by rememberSaveable { mutableStateOf(false) }
    var showWatchDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Your Feed",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Posts and drafts from you and your watched content",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showWatchDialog = true }) {
                            Icon(Icons.Filled.BookmarkAdd, "Manage watches")
                        }
                    }

                    Button(
                        onClick = { showCreatePost = !showCreatePost },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (showCreatePost) Icons.Filled.KeyboardArrowUp else Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(if (showCreatePost) "Hide" else "Create New Post")
                    }
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = showCreatePost,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (currentUser != null) {
                    CreatePostCard(
                        onCreatePost = { title, body, tag, isDraft ->
                            onCreatePost(title, body, tag, isDraft)
                            showCreatePost = false
                        }
                    )
                }
            }
        }

        items(posts, key = { it.id }) { post ->
            PostCard(
                post = post,
                currentUser = currentUser,
                onUpdatePost = onUpdatePost,
                onDeletePost = onDeletePost,
                onPublishDraft = onPublishDraft,
                onCreateComment = onCreateComment,
                onUpdateComment = onUpdateComment,
                onVotePost = onVotePost,
                onVoteComment = onVoteComment,
                enableCommentComposer = true,
                showVoting = post.isPublished
            )
        }
        if (posts.isEmpty()) {
            item {
                Text(
                    text = "No posts yet. Create your first post or watch tags/users to see content!",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            }
        }
    }

    if (showWatchDialog) {
        WatchDialog(
            watchedTags = watchedTags,
            watchedUsers = watchedUsers,
            onWatchTag = onWatchTag,
            onWatchUser = onWatchUser,
            onDismiss = { showWatchDialog = false }
        )
    }
}

@Composable
private fun WatchDialog(
    watchedTags: Set<String>,
    watchedUsers: Set<Long>,
    onWatchTag: (String) -> Unit,
    onWatchUser: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var tagInput by remember { mutableStateOf("") }
    var userIdInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        title = { Text("Manage Watched Content") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Watch Tags", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        label = { Text("Tag name") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = {
                        if (tagInput.isNotBlank()) {
                            onWatchTag(tagInput.trim())
                            tagInput = ""
                        }
                    }) {
                        Text("+")
                    }
                }
                watchedTags.forEach { tag ->
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("#$tag")
                        TextButton(onClick = { onWatchTag(tag) }) { Text("Remove") }
                    }
                }

                Spacer(Modifier.size(8.dp))
                Text("Watch Users (by ID)", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = userIdInput,
                        onValueChange = { userIdInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("User ID") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = {
                        userIdInput.toLongOrNull()?.let { id ->
                            onWatchUser(id)
                            userIdInput = ""
                        }
                    }) {
                        Text("+")
                    }
                }
                watchedUsers.forEach { userId ->
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("User #$userId")
                        TextButton(onClick = { onWatchUser(userId) }) { Text("Remove") }
                    }
                }
            }
        }
    )
}

@Composable
private fun TrendingSection(
    modifier: Modifier = Modifier,
    posts: List<Post>,
    currentUser: UserProfile?,
    onVotePost: (Long, Int) -> Unit,
    onVoteComment: (Long, Int) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔥 Trending",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "Top posts from the community based on upvotes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(posts, key = { it.id }) { post ->
            PostCard(
                post = post,
                currentUser = currentUser,
                onUpdatePost = { _, _, _, _ -> },
                onDeletePost = { },
                onPublishDraft = { },
                onCreateComment = { _, _, _ -> },
                onUpdateComment = { _, _, _ -> },
                onVotePost = onVotePost,
                onVoteComment = onVoteComment,
                enableCommentComposer = false,
                showVoting = true
            )
        }
        if (posts.isEmpty()) {
            item {
                Text(
                    text = "No trending posts yet. Be the first to share!",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            }
        }
    }
}

@Composable
private fun PromptSection(
    modifier: Modifier = Modifier,
    currentUser: UserProfile?,
    prompts: List<Prompt>,
    onCreatePrompt: (String, String, String, String, Boolean) -> Unit,
    onUpdatePrompt: (Long, String, String, String, String, Boolean) -> Unit,
    onDeletePrompt: (Long) -> Unit
) {
    var showCreatePrompt by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "📝 LLM Prompts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Curated prompts for various AI models - share or save privately",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { showCreatePrompt = !showCreatePrompt },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (showCreatePrompt) Icons.Filled.KeyboardArrowUp else Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(if (showCreatePrompt) "Hide" else "Create New Prompt")
                    }
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = showCreatePrompt,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (currentUser != null) {
                    CreatePromptCard(
                        onCreatePrompt = { title, description, content, tag, isPrivate ->
                            onCreatePrompt(title, description, content, tag, isPrivate)
                            showCreatePrompt = false
                        }
                    )
                }
            }
        }

        items(prompts, key = { it.id }) { prompt ->
            PromptCard(
                prompt = prompt,
                canEdit = currentUser?.id == prompt.author.id,
                onUpdatePrompt = onUpdatePrompt,
                onDeletePrompt = onDeletePrompt
            )
        }
        if (prompts.isEmpty()) {
            item {
                Text(
                    text = "No prompts shared yet. Create your first prompt!",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchSection(
    modifier: Modifier = Modifier,
    posts: List<Post>,
    prompts: List<Prompt>,
    users: List<UserSearchResult>,
    onSearchPosts: (PostSearchType, String) -> Unit,
    onSearchPrompts: (String) -> Unit,
    onSearchUsers: (String) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedType by rememberSaveable { mutableStateOf(PostSearchType.TAG) }
    var keyword by rememberSaveable { mutableStateOf("") }
    var promptTag by rememberSaveable { mutableStateOf("") }
    var userEmail by rememberSaveable { mutableStateOf("") }
    var postSearched by rememberSaveable { mutableStateOf(false) }
    var promptSearched by rememberSaveable { mutableStateOf(false) }
    var userSearched by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Posts") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Prompts") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Users") }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PostSearchType.values().forEach { type ->
                                FilterChip(
                                    selected = selectedType == type,
                                    onClick = { selectedType = type },
                                    label = { Text(type.name.replace("_", " ")) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = keyword,
                            onValueChange = { keyword = it },
                            label = { Text("Search keyword") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                postSearched = true
                                onSearchPosts(selectedType, keyword)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Search Posts")
                        }
                    }
                }

                items(posts, key = { it.id }) { post ->
                    PostSummaryCard(post = post)
                }
                if (posts.isEmpty()) {
                    item {
                        Text(
                            text = if (postSearched) "No results found." else "Enter a keyword to search posts.",
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
            }

            1 -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = promptTag,
                            onValueChange = { promptTag = it },
                            label = { Text("AI Model") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                promptSearched = true
                                onSearchPrompts(promptTag)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Search Prompts")
                        }
                    }
                }

                items(prompts, key = { it.id }) { prompt ->
                    PromptSummaryCard(prompt)
                }
                if (prompts.isEmpty()) {
                    item {
                        Text(
                            text = if (promptSearched) "No results found." else "Enter an AI model to search prompts.",
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
            }

            2 -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Find users and their public prompts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = userEmail,
                            onValueChange = { userEmail = it },
                            label = { Text("USC Email") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                userSearched = true
                                onSearchUsers(userEmail)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Search Users")
                        }
                    }
                }

                items(users) { userResult ->
                    UserSearchCard(userResult)
                }
                if (users.isEmpty()) {
                    item {
                        Text(
                            text = if (userSearched) "No users found." else "Enter a USC email to find users.",
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSection(
    modifier: Modifier = Modifier,
    user: UserProfile,
    onUpdateProfile: (LocalDate?, String) -> Unit,
    onResetPassword: (String) -> Unit,
    onLogout: () -> Unit
) {
    var birthDate by rememberSaveable { mutableStateOf(user.birthDate) }
    var bio by rememberSaveable { mutableStateOf(user.bio) }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var showEditProfile by rememberSaveable { mutableStateOf(false) }
    var showEditPassword by rememberSaveable { mutableStateOf(false) }
    var showChangeEmail by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    birthDate?.let {
        calendar.set(it.year, it.monthValue - 1, it.dayOfMonth)
    }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                birthDate = LocalDate.of(year, month + 1, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Name: ${user.name}")
                    Text("Email: ${user.email}")
                    Text("Student ID: ${user.studentId}")
                    Text("Affiliation: ${user.department} - ${user.school}")
                }
            }
        }

        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Edit Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = { showEditProfile = !showEditProfile }) {
                            Icon(
                                if (showEditProfile) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                    }

                    AnimatedVisibility(visible = showEditProfile) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { datePickerDialog.show() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(birthDate?.toString() ?: "Select Birth Date")
                            }

                            OutlinedTextField(
                                value = bio,
                                onValueChange = { bio = it },
                                label = { Text("Bio") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp)
                            )

                            Button(
                                onClick = {
                                    onUpdateProfile(birthDate, bio.trim())
                                    message = "Profile updated"
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Changes")
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reset Password",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = { showEditPassword = !showEditPassword }) {
                            Icon(
                                if (showEditPassword) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                    }

                    AnimatedVisibility(visible = showEditPassword) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = { Text("New Password") },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation()
                            )

                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirm Password") },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation()
                            )

                            Button(
                                onClick = {
                                    if (newPassword != confirmPassword) {
                                        message = "Passwords do not match"
                                        return@Button
                                    }
                                    if (newPassword.isBlank()) {
                                        message = "Password cannot be blank"
                                        return@Button
                                    }
                                    onResetPassword(newPassword)
                                    newPassword = ""
                                    confirmPassword = ""
                                    message = "Password updated"
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Update Password")
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Change Email",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        IconButton(onClick = { showChangeEmail = !showChangeEmail }) {
                            Icon(
                                if (showChangeEmail) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                    }

                    AnimatedVisibility(visible = showChangeEmail) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = user.email,
                                onValueChange = { },
                                label = { Text("Current Email") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false
                            )
                            Text(
                                text = "Email addresses cannot be changed for security reasons. Please contact support if you need assistance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log Out")
            }
        }

        message?.let { msg ->
            item {
                Text(
                    msg,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePostCard(
    onCreatePost: (String, String, String, Boolean) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var tag by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var isDraft by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var tagExpanded by remember { mutableStateOf(false) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "💬 Create Post",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Post Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            ExposedDropdownMenuBox(
                expanded = tagExpanded,
                onExpandedChange = { tagExpanded = !tagExpanded }
            ) {
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("AI Model") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = false
                )
                ExposedDropdownMenu(
                    expanded = tagExpanded,
                    onDismissRequest = { tagExpanded = false }
                ) {
                    AI_AGENTS.forEach { agent ->
                        DropdownMenuItem(
                            text = { Text(agent) },
                            onClick = {
                                tag = agent
                                tagExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Share your experience with this AI...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                maxLines = 8
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = isDraft,
                    onCheckedChange = { isDraft = it }
                )
                Text("Save as Draft (not visible to others)")
            }

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    when {
                        title.isBlank() -> error = "Title is required"
                        tag.isBlank() -> error = "AI Model is required"
                        body.isBlank() -> error = "Post content is required"
                        else -> {
                            error = null
                            onCreatePost(title.trim(), body.trim(), tag.trim(), isDraft)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isDraft) "Save Draft" else "Publish Post")
            }
        }
    }
}

@Composable
private fun PostCard(
    post: Post,
    currentUser: UserProfile?,
    onUpdatePost: (Long, String, String, String) -> Unit,
    onDeletePost: (Long) -> Unit,
    onPublishDraft: (Long) -> Unit,
    onCreateComment: (Long, String?, String) -> Unit,
    onUpdateComment: (Long, String?, String) -> Unit,
    onVotePost: (Long, Int) -> Unit,
    onVoteComment: (Long, Int) -> Unit,
    enableCommentComposer: Boolean,
    showVoting: Boolean
) {
    var commentBody by rememberSaveable(post.id) { mutableStateOf("") }
    var commentTitle by rememberSaveable(post.id) { mutableStateOf("") }
    var showEditPost by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var editPostTitle by remember { mutableStateOf(post.title) }
    var editPostTag by remember { mutableStateOf(post.tag) }
    var editPostBody by remember { mutableStateOf(post.body) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = post.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!post.isPublished) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "DRAFT",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${post.author.name} • ${post.createdAt.formatRelative()}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (post.isEdited) {
                            Text(
                                text = "(edited)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${post.tag}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    if (currentUser?.id == post.author.id) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    editPostTitle = post.title
                                    editPostTag = post.tag
                                    editPostBody = post.body
                                    showEditPost = true
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) }
                            )
                            if (!post.isPublished) {
                                DropdownMenuItem(
                                    text = { Text("Publish") },
                                    onClick = {
                                        onPublishDraft(post.id)
                                        showMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Filled.Publish, contentDescription = null) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showDeleteDialog = true
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) }
                            )
                        }
                    }
                }
            }

            Text(
                text = post.body,
                style = MaterialTheme.typography.bodyMedium
            )

            if (showVoting) {
                VoteRow(
                    score = post.voteSummary.score,
                    currentVote = post.currentUserVote,
                    onUpvote = {
                        val newValue = if (post.currentUserVote == 1) 0 else 1
                        onVotePost(post.id, newValue)
                    },
                    onDownvote = {
                        val newValue = if (post.currentUserVote == -1) 0 else -1
                        onVotePost(post.id, newValue)
                    }
                )
            }

            if (post.comments.isNotEmpty() && post.isPublished) {
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Comments (${post.comments.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                post.comments.forEach { comment ->
                    CommentCard(
                        comment = comment,
                        currentUser = currentUser,
                        onUpdateComment = onUpdateComment,
                        onVoteComment = onVoteComment
                    )
                }
            }

            if (enableCommentComposer && currentUser != null && post.isPublished) {
                Spacer(modifier = Modifier.size(8.dp))
                OutlinedTextField(
                    value = commentTitle,
                    onValueChange = { commentTitle = it },
                    label = { Text("Comment Title (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = commentBody,
                    onValueChange = { commentBody = it },
                    label = { Text("Add a comment") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp)
                )
                Button(onClick = {
                    if (commentBody.isBlank()) return@Button
                    onCreateComment(post.id, commentTitle.takeIf { it.isNotBlank() }?.trim(), commentBody.trim())
                    commentTitle = ""
                    commentBody = ""
                }) {
                    Text("Post Comment")
                }
            }
        }
    }

    if (showEditPost) {
        EditPostDialog(
            title = editPostTitle,
            tag = editPostTag,
            body = editPostBody,
            onTitleChange = { editPostTitle = it },
            onTagChange = { editPostTag = it },
            onBodyChange = { editPostBody = it },
            onSave = {
                onUpdatePost(post.id, editPostTitle.trim(), editPostBody.trim(), editPostTag.trim())
                showEditPost = false
            },
            onDismiss = { showEditPost = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePost(post.id)
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
            title = { Text("Delete Post?") },
            text = { Text("This action cannot be undone.") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPostDialog(
    title: String,
    tag: String,
    body: String,
    onTitleChange: (String) -> Unit,
    onTagChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var tagExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                when {
                    title.isBlank() -> error = "Title is required"
                    tag.isBlank() -> error = "AI Model is required"
                    body.isBlank() -> error = "Post content is required"
                    else -> {
                        error = null
                        onSave()
                    }
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Edit Post") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = tagExpanded,
                    onExpandedChange = { tagExpanded = !tagExpanded }
                ) {
                    OutlinedTextField(
                        value = tag,
                        onValueChange = onTagChange,
                        label = { Text("AI Model") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = tagExpanded,
                        onDismissRequest = { tagExpanded = false }
                    ) {
                        AI_AGENTS.forEach { agent ->
                            DropdownMenuItem(
                                text = { Text(agent) },
                                onClick = {
                                    onTagChange(agent)
                                    tagExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = body,
                    onValueChange = onBodyChange,
                    label = { Text("Body") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp)
                )
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    )
}

@Composable
private fun CommentCard(
    comment: Comment,
    currentUser: UserProfile?,
    onUpdateComment: (Long, String?, String) -> Unit,
    onVoteComment: (Long, Int) -> Unit
) {
    var showEdit by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf(comment.title.orEmpty()) }
    var editBody by remember { mutableStateOf(comment.body) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = comment.title ?: "Comment",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${comment.author.name} • ${comment.createdAt.formatRelative()}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (comment.isEdited) {
                    Text(
                        text = "(edited)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Text(comment.body)
            VoteRow(
                score = comment.voteSummary.score,
                currentVote = comment.currentUserVote,
                onUpvote = {
                    val newValue = if (comment.currentUserVote == 1) 0 else 1
                    onVoteComment(comment.id, newValue)
                },
                onDownvote = {
                    val newValue = if (comment.currentUserVote == -1) 0 else -1
                    onVoteComment(comment.id, newValue)
                }
            )
            if (currentUser?.id == comment.author.id) {
                TextButton(onClick = {
                    editTitle = comment.title.orEmpty()
                    editBody = comment.body
                    showEdit = true
                }) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Edit")
                }
            }
        }
    }

    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateComment(comment.id, editTitle.takeIf { it.isNotBlank() }?.trim(), editBody.trim())
                    showEdit = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEdit = false }) { Text("Cancel") }
            },
            title = { Text("Edit Comment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editBody,
                        onValueChange = { editBody = it },
                        label = { Text("Comment") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                    )
                }
            }
        )
    }
}

@Composable
private fun VoteRow(
    score: Int,
    currentVote: Int?,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(onClick = onUpvote) {
            Icon(
                imageVector = Icons.Filled.ThumbUp,
                contentDescription = "Upvote",
                tint = if (currentVote == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
        Text(score.toString(), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onDownvote) {
            Icon(
                imageVector = Icons.Filled.ThumbDown,
                contentDescription = "Downvote",
                tint = if (currentVote == -1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePromptCard(
    onCreatePrompt: (String, String, String, String, Boolean) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var tag by rememberSaveable { mutableStateOf("") }
    var isPrivate by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var tagExpanded by remember { mutableStateOf(false) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "✨ Create Prompt",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Prompt Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Brief Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Prompt Text (the actual prompt to use)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                maxLines = 8
            )

            ExposedDropdownMenuBox(
                expanded = tagExpanded,
                onExpandedChange = { tagExpanded = !tagExpanded }
            ) {
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("AI Model") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = false
                )
                ExposedDropdownMenu(
                    expanded = tagExpanded,
                    onDismissRequest = { tagExpanded = false }
                ) {
                    AI_AGENTS.forEach { agent ->
                        DropdownMenuItem(
                            text = { Text(agent) },
                            onClick = {
                                tag = agent
                                tagExpanded = false
                            }
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = isPrivate,
                    onCheckedChange = { isPrivate = it }
                )
                Text("Keep Private (only you can see)")
            }

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    when {
                        title.isBlank() -> error = "Title is required"
                        content.isBlank() -> error = "Prompt text is required"
                        tag.isBlank() -> error = "AI Model is required"
                        else -> {
                            error = null
                            onCreatePrompt(title.trim(), description.trim(), content.trim(), tag.trim(), isPrivate)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isPrivate) "Save Private Prompt" else "Share Prompt")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromptCard(
    prompt: Prompt,
    canEdit: Boolean,
    onUpdatePrompt: (Long, String, String, String, String, Boolean) -> Unit,
    onDeletePrompt: (Long) -> Unit
) {
    var showEdit by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf(prompt.title) }
    var editDescription by remember { mutableStateOf(prompt.description) }
    var editContent by remember { mutableStateOf(prompt.content) }
    var editTag by remember { mutableStateOf(prompt.tag) }
    var editIsPrivate by remember { mutableStateOf(prompt.isPrivate) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = prompt.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (prompt.isPrivate) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "PRIVATE",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                    Text(
                        text = "${prompt.author.name} • ${prompt.createdAt.formatRelative()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${prompt.tag}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    if (canEdit) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    editTitle = prompt.title
                                    editDescription = prompt.description
                                    editContent = prompt.content
                                    editTag = prompt.tag
                                    editIsPrivate = prompt.isPrivate
                                    showEdit = true
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showDeleteDialog = true
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) }
                            )
                        }
                    }
                }
            }
            if (prompt.description.isNotBlank()) {
                Text(
                    text = prompt.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = prompt.content,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    if (showEdit) {
        EditPromptDialog(
            title = editTitle,
            description = editDescription,
            content = editContent,
            tag = editTag,
            isPrivate = editIsPrivate,
            onTitleChange = { editTitle = it },
            onDescriptionChange = { editDescription = it },
            onContentChange = { editContent = it },
            onTagChange = { editTag = it },
            onPrivateChange = { editIsPrivate = it },
            onSave = {
                onUpdatePrompt(prompt.id, editTitle.trim(), editDescription.trim(), editContent.trim(), editTag.trim(), editIsPrivate)
                showEdit = false
            },
            onDismiss = { showEdit = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePrompt(prompt.id)
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
            title = { Text("Delete Prompt?") },
            text = { Text("This action cannot be undone.") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPromptDialog(
    title: String,
    description: String,
    content: String,
    tag: String,
    isPrivate: Boolean,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onTagChange: (String) -> Unit,
    onPrivateChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var tagExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                when {
                    title.isBlank() -> error = "Title is required"
                    content.isBlank() -> error = "Prompt text is required"
                    tag.isBlank() -> error = "AI Model is required"
                    else -> {
                        error = null
                        onSave()
                    }
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Edit Prompt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = onContentChange,
                    label = { Text("Prompt Text") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp)
                )
                ExposedDropdownMenuBox(
                    expanded = tagExpanded,
                    onExpandedChange = { tagExpanded = !tagExpanded }
                ) {
                    OutlinedTextField(
                        value = tag,
                        onValueChange = onTagChange,
                        label = { Text("AI Model") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = tagExpanded,
                        onDismissRequest = { tagExpanded = false }
                    ) {
                        AI_AGENTS.forEach { agent ->
                            DropdownMenuItem(
                                text = { Text(agent) },
                                onClick = {
                                    onTagChange(agent)
                                    tagExpanded = false
                                }
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = isPrivate,
                        onCheckedChange = onPrivateChange
                    )
                    Text("Keep Private")
                }
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    )
}

@Composable
private fun PostSummaryCard(post: Post) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${post.author.name} • ${post.createdAt.formatRelative()} • #${post.tag}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = post.body,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Text("Score: ${post.voteSummary.score} • ${post.commentCount} comments")
        }
    }
}

@Composable
private fun PromptSummaryCard(prompt: Prompt) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = prompt.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${prompt.author.name} • #${prompt.tag}",
                style = MaterialTheme.typography.bodySmall
            )
            if (prompt.description.isNotBlank()) {
                Text(prompt.description, fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = prompt.content,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UserSearchCard(userResult: UserSearchResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = userResult.user.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${userResult.user.email} • ${userResult.user.department} - ${userResult.user.school}",
                style = MaterialTheme.typography.bodySmall
            )

            if (userResult.prompts.isNotEmpty()) {
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Public Prompts (${userResult.prompts.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                userResult.prompts.forEach { prompt ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = prompt.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "#${prompt.tag}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            if (prompt.description.isNotBlank()) {
                                Text(prompt.description, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                text = prompt.content,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "No public prompts shared yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}