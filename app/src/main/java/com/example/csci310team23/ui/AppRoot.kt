package com.example.csci310team23.ui

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Switch
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
import com.example.csci310team23.data.model.AuthMode
import com.example.csci310team23.data.model.Comment
import com.example.csci310team23.data.model.Post
import com.example.csci310team23.data.model.PostSearchType
import com.example.csci310team23.data.model.Prompt
import com.example.csci310team23.data.model.TagWatchHistory
import com.example.csci310team23.data.model.UserProfile
import com.example.csci310team23.data.model.UserWatchHistory
import com.example.csci310team23.data.model.formatRelative
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.time.LocalDate
import java.util.Calendar

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
private fun BaseContentCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
fun AppRoot(
    viewModel: AppViewModel = viewModel(
        factory = AppViewModel.provideFactory(
            AppGraph.repository,
            AppGraph.preferencesManager
        )
    )
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
    val snackbarHostState = remember { SnackbarHostState() }
    val viewModel: AppViewModel = viewModel()

    LaunchedEffect(authState.errorMessage, authState.infoMessage) {
        authState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
        authState.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
    var selectedSection by rememberSaveable { mutableStateOf(MainSection.PROMPTS) }

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
                isFeedRefreshing = uiState.isFeedRefreshing,
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
                watchedTagsHistory = uiState.watchedTagsHistory,
                watchedUsersHistory = uiState.watchedUsersHistory,
                allUsers = uiState.allUsers
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
                allPrompts = uiState.prompts,
                onCreatePrompt = { title, description, content, tag, temperature, context, memoryTokens, isPrivate ->
                    viewModel.createPrompt(
                        title,
                        description,
                        content,
                        tag,
                        temperature,
                        context,
                        memoryTokens,
                        isPrivate
                    )
                },
                onUpdatePrompt = { id, title, description, content, tag, temperature, context, memoryTokens, isPrivate ->
                    viewModel.updatePrompt(
                        id,
                        title,
                        description,
                        content,
                        tag,
                        temperature,
                        context,
                        memoryTokens,
                        isPrivate
                    )
                },
                onDeletePrompt = viewModel::deletePrompt
            )

            MainSection.SEARCH -> SearchSection(
                modifier = Modifier.padding(innerPadding),
                currentUser = currentUser,
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
    isFeedRefreshing: Boolean,
    onCreatePost: (String, String, String, Boolean) -> Unit,
    onUpdatePost: (Long, String, String, String) -> Unit,
    onDeletePost: (Long) -> Unit,
    onPublishDraft: (Long) -> Unit,
    onCreateComment: (Long, String?, String) -> Unit,
    onUpdateComment: (Long, String?, String) -> Unit,
    onVotePost: (Long, Int) -> Unit,
    onVoteComment: (Long, Int) -> Unit,
    onWatchTag: (String) -> Unit,
    onWatchUser: (String) -> Unit,
    watchedTagsHistory: List<TagWatchHistory>,
    watchedUsersHistory: List<UserWatchHistory>,
    allUsers: List<UserProfile>
) {
    var showCreatePost by rememberSaveable { mutableStateOf(false) }
    var showWatchDialog by remember { mutableStateOf(false) }
    val viewModel: AppViewModel = viewModel()

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = isFeedRefreshing),
        onRefresh = { viewModel.refreshFeed() },
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
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
                            Row {
                                IconButton(
                                    onClick = { viewModel.refreshFeed() },
                                    enabled = !isFeedRefreshing
                                ) {
                                    Icon(
                                        Icons.Filled.Refresh,
                                        "Refresh feed",
                                        tint = if (isFeedRefreshing) MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.38f
                                        )
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(onClick = { showWatchDialog = true }) {
                                    Icon(Icons.Filled.BookmarkAdd, "Manage watches")
                                }
                            }
                        }

                        Button(
                            onClick = { showCreatePost = !showCreatePost },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isFeedRefreshing
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

            if (isFeedRefreshing) {
                items(5) { index ->
                    LoadingPostCard()
                }
            } else {
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
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showWatchDialog) {
        WatchDialog(
            watchedTagsHistory = watchedTagsHistory,
            watchedUsersHistory = watchedUsersHistory,
            allUsers = allUsers,
            onWatchTag = onWatchTag,
            onWatchUser = onWatchUser,
            onDismiss = { showWatchDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchDialog(
    watchedTagsHistory: List<TagWatchHistory>,
    watchedUsersHistory: List<UserWatchHistory>,
    allUsers: List<UserProfile>,
    onWatchTag: (String) -> Unit,
    onWatchUser: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tagExpanded by remember { mutableStateOf(false) }
    var selectedTag by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        title = { Text("Manage Watched Content") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Watch AI Models", fontWeight = FontWeight.SemiBold)

                ExposedDropdownMenuBox(
                    expanded = tagExpanded,
                    onExpandedChange = { tagExpanded = !tagExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedTag,
                        onValueChange = { selectedTag = it },
                        label = { Text("Select AI Model") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true
                    )
                    ExposedDropdownMenu(
                        expanded = tagExpanded,
                        onDismissRequest = { tagExpanded = false }
                    ) {
                        AI_AGENTS.forEach { agent ->
                            DropdownMenuItem(
                                text = { Text(agent) },
                                onClick = {
                                    onWatchTag(agent)
                                    selectedTag = ""
                                    tagExpanded = false
                                }
                            )
                        }
                    }
                }

                watchedTagsHistory.forEach { tagHistory ->
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("#${tagHistory.tag}")
                            Text(
                                text = if (tagHistory.endTime == null) "Currently watching"
                                else "Stopped watching",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onWatchTag(tagHistory.tag) }) {
                            Text(if (tagHistory.endTime == null) "Stop" else "Watch Again")
                        }
                    }
                }

                Spacer(Modifier.size(8.dp))
                Text("Watch Users (by Email)", fontWeight = FontWeight.SemiBold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = userEmail,
                        onValueChange = { userEmail = it },
                        label = { Text("USC Email") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (userEmail.isNotBlank()) {
                                onWatchUser(userEmail.trim())
                                userEmail = ""
                            }
                        },
                        modifier = Modifier.padding(top = 5.dp)
                    ) {
                        Text("+")
                    }
                }
                watchedUsersHistory.forEach { userHistory ->
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(userHistory.email)
                            Text(
                                text = if (userHistory.endTime == null) "Currently watching"
                                else "Stopped watching",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onWatchUser(userHistory.email) }) {
                            Text(if (userHistory.endTime == null) "Stop" else "Watch Again")
                        }
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Trending",
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
                onDeletePost = { _ -> },
                onPublishDraft = { _ -> },
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
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun PromptSection(
    modifier: Modifier = Modifier,
    currentUser: UserProfile?,
    allPrompts: List<Prompt>,
    onCreatePrompt: (String, String, String, String, String?, String?, String?, Boolean) -> Unit,
    onUpdatePrompt: (Long, String, String, String, String, String?, String?, String?, Boolean) -> Unit,
    onDeletePrompt: (Long) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showCreatePrompt by rememberSaveable { mutableStateOf(false) }

    val myPrompts = allPrompts.filter { it.author.id == currentUser?.id }
    val publicPrompts = allPrompts.filter { !it.isPrivate }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "LLM Prompts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 0.dp)
                )

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("My Prompts") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Community") }
                    )
                }
            }
        }

        when (selectedTab) {
            0 -> {
                item {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Your personal prompt library",
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
                                onCreatePrompt = { title, description, content, tag, temp, ctx, mem, isPrivate ->
                                    onCreatePrompt(
                                        title,
                                        description,
                                        content,
                                        tag,
                                        temp,
                                        ctx,
                                        mem,
                                        isPrivate
                                    )
                                    showCreatePrompt = false
                                }
                            )
                        }
                    }
                }

                items(myPrompts, key = { it.id }) { prompt ->
                    PromptCard(
                        prompt = prompt,
                        canEdit = true,
                        onUpdatePrompt = onUpdatePrompt,
                        onDeletePrompt = onDeletePrompt
                    )
                }
                if (myPrompts.isEmpty()) {
                    item {
                        Text(
                            text = "You haven't created any prompts yet. Start building your library!",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }

            1 -> {
                items(publicPrompts, key = { it.id }) { prompt ->
                    PromptCard(
                        prompt = prompt,
                        canEdit = currentUser?.id == prompt.author.id,
                        onUpdatePrompt = onUpdatePrompt,
                        onDeletePrompt = onDeletePrompt
                    )
                }
                if (publicPrompts.isEmpty()) {
                    item {
                        Text(
                            text = "No public prompts shared yet. Be the first!",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchSection(
    modifier: Modifier = Modifier,
    currentUser: UserProfile?,
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
    var tagExpanded by remember { mutableStateOf(false) }
    var promptTag by rememberSaveable { mutableStateOf("") }
    var promptTagExpanded by remember { mutableStateOf(false) }
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 0.dp)
                )

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

                        if (selectedType == PostSearchType.TAG) {
                            ExposedDropdownMenuBox(
                                expanded = tagExpanded,
                                onExpandedChange = { tagExpanded = !tagExpanded }
                            ) {
                                OutlinedTextField(
                                    value = keyword,
                                    onValueChange = { keyword = it },
                                    label = { Text("AI Model") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = tagExpanded
                                        )
                                    },
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
                                                keyword = agent
                                                tagExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = keyword,
                                onValueChange = { keyword = it },
                                label = { Text("Search keyword") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

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
                            text = if (postSearched) "No results found." else "",
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }

            1 -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Select an AI model to search prompts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ExposedDropdownMenuBox(
                            expanded = promptTagExpanded,
                            onExpandedChange = { promptTagExpanded = !promptTagExpanded }
                        ) {
                            OutlinedTextField(
                                value = promptTag,
                                onValueChange = { promptTag = it },
                                label = { Text("AI Model") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = promptTagExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                readOnly = false
                            )
                            ExposedDropdownMenu(
                                expanded = promptTagExpanded,
                                onDismissRequest = { promptTagExpanded = false }
                            ) {
                                AI_AGENTS.forEach { agent ->
                                    DropdownMenuItem(
                                        text = { Text(agent) },
                                        onClick = {
                                            promptTag = agent
                                            promptTagExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = {
                                promptSearched = true
                                onSearchPrompts(promptTag)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Search Public Prompts")
                        }
                    }
                }

                items(prompts, key = { it.id }) { prompt ->
                    PromptSearchCard(
                        prompt = prompt,
                        isOwnPrompt = currentUser?.id == prompt.author.id
                    )
                }
                if (prompts.isEmpty()) {
                    item {
                        Text(
                            text = if (promptSearched) "No results found." else "",
                            modifier = Modifier.padding(top = 16.dp)
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
                    UserSearchCard(
                        userResult = userResult,
                        isOwnProfile = currentUser?.id == userResult.user.id
                    )
                }
                if (users.isEmpty()) {
                    item {
                        Text(
                            text = if (userSearched) "No users found." else "",
                            modifier = Modifier.padding(top = 16.dp)
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
    onLogout: () -> Unit,
    viewModel: AppViewModel = viewModel()
) {
    var birthDate by rememberSaveable { mutableStateOf(user.birthDate) }
    var bio by rememberSaveable { mutableStateOf(user.bio) }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var showEditProfile by rememberSaveable { mutableStateOf(false) }
    var showEditPassword by rememberSaveable { mutableStateOf(false) }
    var showDisplaySettings by rememberSaveable { mutableStateOf(false) }
    var showChangeEmail by rememberSaveable { mutableStateOf(false) }

    var showCommentTitles by remember(user.id) {
        mutableStateOf(viewModel.getShowCommentTitles())
    }

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
                            text = "Display Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = { showDisplaySettings = !showDisplaySettings }) {
                            Icon(
                                if (showDisplaySettings) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                    }

                    AnimatedVisibility(visible = showDisplaySettings) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Show Comment Titles")
                                Switch(
                                    checked = showCommentTitles,
                                    onCheckedChange = { newValue ->
                                        showCommentTitles = newValue
                                        viewModel.setShowCommentTitles(newValue)
                                    }
                                )
                            }
                            Text(
                                text = "When enabled, comment titles will be displayed above comment text",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                text = "Create Post",
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
    var showCommentComposer by rememberSaveable(post.id) { mutableStateOf(false) }
    var editPostTitle by remember { mutableStateOf(post.title) }
    var editPostTag by remember { mutableStateOf(post.tag) }
    var editPostBody by remember { mutableStateOf(post.body) }

    BaseContentCard {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                Spacer(modifier = Modifier.size(4.dp))

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
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                if (currentUser?.id == post.author.id) {
                    Text(
                        text = "#${post.tag}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "Options",
                            modifier = Modifier.size(20.dp)
                        )
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
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Publish,
                                        contentDescription = null
                                    )
                                }
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
                } else {
                    Text(
                        text = "#${post.tag}",
                        style = MaterialTheme.typography.labelLarge
                    )
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
                    val newValue: Int = if (post.currentUserVote == 1) 0 else 1
                    onVotePost(post.id, newValue)
                },
                onDownvote = {
                    val newValue: Int = if (post.currentUserVote == -1) 0 else -1
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
            Button(
                onClick = { showCommentComposer = !showCommentComposer },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    if (showCommentComposer) Icons.Filled.KeyboardArrowUp else Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(if (showCommentComposer) "Hide" else "Add Comment")
            }

            AnimatedVisibility(visible = showCommentComposer) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                        onCreateComment(
                            post.id,
                            commentTitle.takeIf { it.isNotBlank() }?.trim(),
                            commentBody.trim()
                        )
                        commentTitle = ""
                        commentBody = ""
                        showCommentComposer = false
                    }) {
                        Text("Post Comment")
                    }
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
    val viewModel: AppViewModel = viewModel()

    var showEdit by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf(comment.title ?: "") }
    var editBody by remember { mutableStateOf(comment.body) }

    val showCommentTitle = if (currentUser != null) {
        remember(currentUser.id) { viewModel.getShowCommentTitles() }
    } else {
        true
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (showCommentTitle) {
                        Text(
                            text = comment.title ?: "Comment",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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
                }

                if (currentUser?.id == comment.author.id) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "Options",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                editTitle = comment.title ?: ""
                                editBody = comment.body
                                showEdit = true
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) }
                        )
                    }
                }
            }

            Text(text = comment.body, style = MaterialTheme.typography.bodyMedium)
            VoteRow(
                score = comment.voteSummary.score,
                currentVote = comment.currentUserVote,
                onUpvote = {
                    val newValue: Int = if (comment.currentUserVote == 1) 0 else 1
                    onVoteComment(comment.id, newValue)
                },
                onDownvote = {
                    val newValue: Int = if (comment.currentUserVote == -1) 0 else -1
                    onVoteComment(comment.id, newValue)
                }
            )
        }
    }

    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            confirmButton = {
                TextButton(onClick = {
                    val titleToSave: String? =
                        if (editTitle.isNotBlank()) editTitle.trim() else null
                    onUpdateComment(comment.id, titleToSave, editBody.trim())
                    showEdit = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEdit = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Edit Comment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { newValue: String -> editTitle = newValue },
                        label = { Text("Title (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editBody,
                        onValueChange = { newValue: String -> editBody = newValue },
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onUpvote, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Filled.ThumbUp,
                contentDescription = "Upvote",
                modifier = Modifier.size(18.dp),
                tint = if (currentVote == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
        Text(score.toString(), style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = onDownvote, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Filled.ThumbDown,
                contentDescription = "Downvote",
                modifier = Modifier.size(18.dp),
                tint = if (currentVote == -1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePromptCard(
    onCreatePrompt: (String, String, String, String, String?, String?, String?, Boolean) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var tag by rememberSaveable { mutableStateOf("") }
    var temperature by rememberSaveable { mutableStateOf("") }
    var context by rememberSaveable { mutableStateOf("") }
    var memoryTokens by rememberSaveable { mutableStateOf("") }
    var isPrivate by rememberSaveable { mutableStateOf(false) }
    var showOptional by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var tagExpanded by remember { mutableStateOf(false) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Create Prompt",
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

            OutlinedButton(
                onClick = { showOptional = !showOptional },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    if (showOptional) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(if (showOptional) "Hide Optional Fields" else "Show Optional Fields")
            }

            AnimatedVisibility(visible = showOptional) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = temperature,
                        onValueChange = { temperature = it },
                        label = { Text("Temperature (e.g., 0.7)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = context,
                        onValueChange = { context = it },
                        label = { Text("Context") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    OutlinedTextField(
                        value = memoryTokens,
                        onValueChange = { memoryTokens = it },
                        label = { Text("Memory Tokens") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
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
                            onCreatePrompt(
                                title.trim(),
                                description.trim(),
                                content.trim(),
                                tag.trim(),
                                temperature.takeIf { it.isNotBlank() }?.trim(),
                                context.takeIf { it.isNotBlank() }?.trim(),
                                memoryTokens.takeIf { it.isNotBlank() }?.trim(),
                                isPrivate
                            )
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

@Composable
private fun PromptCard(
    prompt: Prompt,
    canEdit: Boolean,
    onUpdatePrompt: (Long, String, String, String, String, String?, String?, String?, Boolean) -> Unit,
    onDeletePrompt: (Long) -> Unit
) {
    var showEdit by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showOptional by rememberSaveable(prompt.id) { mutableStateOf(false) }

    val hasOptionalFields = !prompt.temperature.isNullOrBlank() ||
            !prompt.context.isNullOrBlank() ||
            !prompt.memoryTokens.isNullOrBlank()
    println("DEBUG: temp='${prompt.temperature}' ctx='${prompt.context}' mem='${prompt.memoryTokens}' hasOpt=$hasOptionalFields")


    BaseContentCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = prompt.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (prompt.isPrivate) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "PRIVATE",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.size(4.dp))

                Text(
                    text = "${prompt.author.name} • ${prompt.createdAt.formatRelative()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "#${prompt.tag}",
                    style = MaterialTheme.typography.labelLarge
                )
                if (canEdit) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "Options",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
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
                } else {
                    Spacer(modifier = Modifier.width(28.dp))
                }
            }
        }

        if (prompt.description.isNotBlank()) {
            Text(
                text = prompt.description,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = prompt.content,
            style = MaterialTheme.typography.bodyMedium
        )

        if (hasOptionalFields) {
            OutlinedButton(
                onClick = { showOptional = !showOptional },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    if (showOptional) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(if (showOptional) "Hide Details" else "Show Details")
            }

            AnimatedVisibility(visible = showOptional) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    prompt.temperature?.let {
                        Text("Temperature: $it", style = MaterialTheme.typography.bodySmall)
                    }
                    prompt.context?.let {
                        Text("Context: $it", style = MaterialTheme.typography.bodySmall)
                    }
                    prompt.memoryTokens?.let {
                        Text("Memory Tokens: $it", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showEdit) {
        EditPromptDialog(
            prompt = prompt,
            onSave = { title, description, content, tag, temperature, context, memoryTokens, isPrivate ->
                onUpdatePrompt(
                    prompt.id,
                    title,
                    description,
                    content,
                    tag,
                    temperature,
                    context,
                    memoryTokens,
                    isPrivate
                )
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
    prompt: Prompt,
    onSave: (String, String, String, String, String?, String?, String?, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(prompt.title) }
    var description by remember { mutableStateOf(prompt.description) }
    var content by remember { mutableStateOf(prompt.content) }
    var tag by remember { mutableStateOf(prompt.tag) }
    var temperature by remember { mutableStateOf(prompt.temperature ?: "") }
    var context by remember { mutableStateOf(prompt.context ?: "") }
    var memoryTokens by remember { mutableStateOf(prompt.memoryTokens ?: "") }
    var isPrivate by remember { mutableStateOf(prompt.isPrivate) }
    var showOptional by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var tagExpanded by remember { mutableStateOf(false) }

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
                        onSave(
                            title.trim(),
                            description.trim(),
                            content.trim(),
                            tag.trim(),
                            temperature.takeIf { it.isNotBlank() }?.trim(),
                            context.takeIf { it.isNotBlank() }?.trim(),
                            memoryTokens.takeIf { it.isNotBlank() }?.trim(),
                            isPrivate
                        )
                    }
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Edit Prompt") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 500.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Prompt Text") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                    )
                }
                item {
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
                                        tag = agent
                                        tagExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    TextButton(
                        onClick = { showOptional = !showOptional },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (showOptional) "Hide Optional Fields" else "Show Optional Fields")
                    }
                }

                if (showOptional) {
                    item {
                        OutlinedTextField(
                            value = temperature,
                            onValueChange = { temperature = it },
                            label = { Text("Temperature (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = context,
                            onValueChange = { context = it },
                            label = { Text("Context (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = memoryTokens,
                            onValueChange = { memoryTokens = it },
                            label = { Text("Memory Tokens (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = isPrivate,
                            onCheckedChange = { isPrivate = it }
                        )
                        Text("Keep Private")
                    }
                }

                error?.let {
                    item {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun PromptSearchCard(
    prompt: Prompt,
    isOwnPrompt: Boolean
) {
    var showOptional by rememberSaveable(prompt.id) { mutableStateOf(false) }
    val hasOptionalFields = !prompt.temperature.isNullOrBlank() ||
            !prompt.context.isNullOrBlank() ||
            !prompt.memoryTokens.isNullOrBlank()
    println("DEBUG: temp='${prompt.temperature}' ctx='${prompt.context}' mem='${prompt.memoryTokens}' hasOpt=$hasOptionalFields")

    BaseContentCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = prompt.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (prompt.isPrivate && isOwnPrompt) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "PRIVATE",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.size(4.dp))

                Text(
                    text = "${prompt.author.name} • ${prompt.createdAt.formatRelative()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "#${prompt.tag}",
                style = MaterialTheme.typography.labelLarge
            )
        }

        if (prompt.description.isNotBlank()) {
            Text(
                text = prompt.description,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = prompt.content,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )

        if (hasOptionalFields) {
            OutlinedButton(
                onClick = { showOptional = !showOptional },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    if (showOptional) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(if (showOptional) "Hide Details" else "Show Details")
            }

            AnimatedVisibility(visible = showOptional) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    prompt.temperature?.let {
                        Text("Temperature: $it", style = MaterialTheme.typography.bodySmall)
                    }
                    prompt.context?.let {
                        Text("Context: $it", style = MaterialTheme.typography.bodySmall)
                    }
                    prompt.memoryTokens?.let {
                        Text("Memory: $it", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PostSummaryCard(post: Post) {
    BaseContentCard {
        Text(
            text = post.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.size(4.dp))
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

@Composable
private fun UserSearchCard(
    userResult: UserSearchResult,
    isOwnProfile: Boolean
) {
    val publicPrompts = userResult.prompts.filter { !it.isPrivate }
    val privatePrompts = userResult.prompts.filter { it.isPrivate }

    BaseContentCard {
        Text(
            text = userResult.user.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = "${userResult.user.email} • ${userResult.user.department} - ${userResult.user.school}",
            style = MaterialTheme.typography.bodySmall
        )

        if (userResult.prompts.isNotEmpty()) {
            Spacer(modifier = Modifier.size(8.dp))

            if (isOwnProfile) {
                Text(
                    text = "Public Prompts (${publicPrompts.size}) • Private Prompts (${privatePrompts.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = "Public Prompts (${publicPrompts.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            userResult.prompts.forEach { prompt ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = prompt.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (prompt.isPrivate && isOwnProfile) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(
                                                text = "PRIVATE",
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(
                                                    horizontal = 4.dp,
                                                    vertical = 1.dp
                                                ),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = "#${prompt.tag}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        if (prompt.description.isNotBlank()) {
                            Text(prompt.description, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = prompt.content,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall
                        )
                        prompt.temperature?.let {
                            Text("Temp: $it", style = MaterialTheme.typography.bodySmall)
                        }
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

@Composable
private fun LoadingPostCard() {
    BaseContentCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.small
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(20.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.small
                    )
            )
        }

        Spacer(modifier = Modifier.size(8.dp))

        Box(
            modifier = Modifier
                .width(120.dp)
                .height(16.dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.small
                )
        )

        Spacer(modifier = Modifier.size(12.dp))

        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.small
                    )
            )
            Spacer(modifier = Modifier.size(4.dp))
        }

        Spacer(modifier = Modifier.size(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(20.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.small
                    )
            )
        }
    }
}
