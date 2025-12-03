package com.example.csci310team23.ui

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.setValue
import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
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
import com.example.csci310team23.data.model.PostVersion
import com.example.csci310team23.data.model.PostSearchType
import com.example.csci310team23.data.model.Prompt
import com.example.csci310team23.data.model.PromptVersion
import com.example.csci310team23.data.model.TagWatchHistory
import com.example.csci310team23.data.model.UserProfile
import com.example.csci310team23.data.model.UserWatchHistory
import com.example.csci310team23.data.model.formatRelative
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.time.LocalDate
import java.time.Period
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
    "Perplexity AI",
    "Other"
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
            ProfileSetupScreen(
                user = currentUser,
                onComplete = { department, school, birthDate, bio ->
                    viewModel.completeProfile(department, school, birthDate, bio)
                },
                onLogout = viewModel::logout
            )
        }

        else -> {
            MainScreen(
                currentUser = currentUser,
                uiState = uiState,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState
            )
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
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .testTag("landing_get_started_button")
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
                    label = { Text("Sign In") },
                    modifier = Modifier.testTag("auth_sign_in_tab")
                )
                FilterChip(
                    selected = authState.mode == AuthMode.REGISTER,
                    onClick = {
                        localError = null
                        onModeChange(AuthMode.REGISTER)
                    },
                    label = { Text("Register") },
                    modifier = Modifier.testTag("auth_register_tab")
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
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .testTag("auth_email_field")
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
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .testTag("auth_password_field"),
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
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .testTag("auth_submit_button")
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
    var birthDateError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    birthDate?.let {
        calendar.set(it.year, it.monthValue - 1, it.dayOfMonth)
    }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                birthDate = selectedDate

                val age = Period.between(selectedDate, LocalDate.now()).years
                birthDateError = if (age < 18) {
                    "You must be 18 or older"
                } else {
                    null
                }
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

        Column {
            OutlinedButton(
                onClick = { datePickerDialog.show() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(birthDate?.toString() ?: "Select Birth Date")
            }
            birthDateError?.let { errorText ->
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
        }

        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
        )

        error?.let { errorText ->
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(onClick = {
            birthDateError = null
            error = null

            if (department.isBlank() || school.isBlank()) {
                error = "Affiliation information is required"
                return@Button
            }

            birthDate?.let { date ->
                val age = Period.between(date, LocalDate.now()).years
                if (age < 18) {
                    birthDateError = "You must be 18 or older"
                    return@Button
                }
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
    DISCOVER("Discover"),
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
    var selectedSection by rememberSaveable { mutableStateOf(MainSection.FEED) }

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
                        MainSection.DISCOVER -> Icons.Outlined.Whatshot
                        MainSection.PROMPTS -> Icons.Outlined.Article
                        MainSection.SEARCH -> Icons.Filled.Search
                        MainSection.PROFILE -> Icons.Filled.Person
                    }
                    NavigationBarItem(
                        selected = section == selectedSection,
                        onClick = { selectedSection = section },
                        icon = { Icon(icon, contentDescription = section.title) },
                        label = { Text(section.title) },
                        modifier = Modifier.testTag("nav_${section.name.lowercase()}")
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
                onCreatePost = { title, body, tag, isDraft, isAnonymous ->
                    viewModel.createPost(title, body, tag, isDraft, isAnonymous)
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
                allUsers = uiState.allUsers,
                postVersions = uiState.postVersions,
                onToggleBookmark = viewModel::togglePostBookmark
            )

            MainSection.DISCOVER -> DiscoverSection(
                modifier = Modifier.padding(innerPadding),
                currentUser = currentUser,
                trendingPosts = uiState.trending,
                bookmarkedPosts = uiState.bookmarkedPosts,
                bookmarkedPrompts = uiState.bookmarkedPrompts,
                postVersions = uiState.postVersions,
                promptVersions = uiState.promptVersions,
                onVotePost = viewModel::voteOnPost,
                onVoteComment = viewModel::voteOnComment,
                onPublishDraft = viewModel::publishDraft,
                onDeletePost = viewModel::deletePost,
                onUpdatePost = viewModel::updatePost,
                onUpdatePrompt = viewModel::updatePrompt,
                onDeletePrompt = viewModel::deletePrompt,
                onCreateComment = viewModel::createComment,
                onUpdateComment = viewModel::updateComment,
                onTogglePostBookmark = viewModel::togglePostBookmark,
                onTogglePromptBookmark = viewModel::togglePromptBookmark
            )

            MainSection.PROMPTS -> PromptSection(
                modifier = Modifier.padding(innerPadding),
                currentUser = currentUser,
                allPrompts = uiState.prompts,
                promptVersions = uiState.promptVersions,
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
                onUpdatePrompt = viewModel::updatePrompt,
                onDeletePrompt = viewModel::deletePrompt,
                onTogglePromptBookmark = viewModel::togglePromptBookmark
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
    onCreatePost: (String, String, String, Boolean, Boolean) -> Unit,
    onUpdatePost: (Long, String, String, String, Boolean) -> Unit,
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
    allUsers: List<UserProfile>,
    postVersions: Map<Long, List<PostVersion>>,
    onToggleBookmark: (Long) -> Unit
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("toggle_create_post_button"),
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
                            onCreatePost = { title, body, tag, isDraft, isAnonymous ->
                                onCreatePost(title, body, tag, isDraft, isAnonymous)
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
                        onToggleBookmark = onToggleBookmark,
                        versions = postVersions[post.id].orEmpty(),
                        enableCommentComposer = currentUser != null,
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
    onVoteComment: (Long, Int) -> Unit,
    onToggleBookmark: (Long) -> Unit,
    postVersions: Map<Long, List<PostVersion>>
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
                onUpdatePost = { _, _, _, _, _ -> },
                onDeletePost = { _ -> },
                onPublishDraft = { _ -> },
                onCreateComment = { _, _, _ -> },
                onUpdateComment = { _, _, _ -> },
                onVotePost = onVotePost,
                onVoteComment = onVoteComment,
                onToggleBookmark = onToggleBookmark,
                versions = postVersions[post.id].orEmpty(),
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
    promptVersions: Map<Long, List<PromptVersion>>,
    onCreatePrompt: (String, String, String, String, String?, String?, String?, Boolean) -> Unit,
    onUpdatePrompt: (Long, String, String, String, String, String?, String?, String?, Boolean) -> Unit,
    onDeletePrompt: (Long) -> Unit,
    onTogglePromptBookmark: (Long) -> Unit
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
                        modifier = Modifier.testTag("prompts_tab_my"),
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("My Prompts") }
                    )
                    Tab(
                        modifier = Modifier.testTag("prompts_tab_community"),
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("toggle_create_prompt_button")
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
                        onDeletePrompt = onDeletePrompt,
                        onToggleBookmark = onTogglePromptBookmark,
                        versions = promptVersions[prompt.id].orEmpty()
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
                        onDeletePrompt = onDeletePrompt,
                        onToggleBookmark = onTogglePromptBookmark,
                        versions = promptVersions[prompt.id].orEmpty()
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

@Composable
private fun DiscoverSection(
    modifier: Modifier = Modifier,
    currentUser: UserProfile?,
    trendingPosts: List<Post>,
    bookmarkedPosts: List<Post>,
    bookmarkedPrompts: List<Prompt>,
    postVersions: Map<Long, List<PostVersion>>,
    promptVersions: Map<Long, List<PromptVersion>>,
    onVotePost: (Long, Int) -> Unit,
    onVoteComment: (Long, Int) -> Unit,
    onPublishDraft: (Long) -> Unit,
    onDeletePost: (Long) -> Unit,
    onUpdatePost: (Long, String, String, String, Boolean) -> Unit,
    onUpdatePrompt: (Long, String, String, String, String, String?, String?, String?, Boolean) -> Unit,
    onDeletePrompt: (Long) -> Unit,
    onCreateComment: (Long, String?, String) -> Unit,
    onUpdateComment: (Long, String?, String) -> Unit,
    onTogglePostBookmark: (Long) -> Unit,
    onTogglePromptBookmark: (Long) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Discover",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Trending content and your saved library",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Trending") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Saved") }
                )
            }
        }

        when (selectedTab) {
            0 -> TrendingSection(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                posts = trendingPosts,
                currentUser = currentUser,
                onVotePost = onVotePost,
                onVoteComment = onVoteComment,
                onToggleBookmark = onTogglePostBookmark,
                postVersions = postVersions
            )

            else -> BookmarkSection(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                currentUser = currentUser,
                bookmarkedPosts = bookmarkedPosts,
                bookmarkedPrompts = bookmarkedPrompts,
                postVersions = postVersions,
                promptVersions = promptVersions,
                onVotePost = onVotePost,
                onVoteComment = onVoteComment,
                onPublishDraft = onPublishDraft,
                onDeletePost = onDeletePost,
                onUpdatePost = onUpdatePost,
                onUpdatePrompt = onUpdatePrompt,
                onDeletePrompt = onDeletePrompt,
                onCreateComment = onCreateComment,
                onUpdateComment = onUpdateComment,
                onTogglePostBookmark = onTogglePostBookmark,
                onTogglePromptBookmark = onTogglePromptBookmark
            )
        }
    }
}

@Composable
private fun BookmarkSection(
    modifier: Modifier = Modifier,
    currentUser: UserProfile?,
    bookmarkedPosts: List<Post>,
    bookmarkedPrompts: List<Prompt>,
    postVersions: Map<Long, List<PostVersion>>,
    promptVersions: Map<Long, List<PromptVersion>>,
    onVotePost: (Long, Int) -> Unit,
    onVoteComment: (Long, Int) -> Unit,
    onPublishDraft: (Long) -> Unit,
    onDeletePost: (Long) -> Unit,
    onUpdatePost: (Long, String, String, String, Boolean) -> Unit,
    onUpdatePrompt: (Long, String, String, String, String, String?, String?, String?, Boolean) -> Unit,
    onDeletePrompt: (Long) -> Unit,
    onCreateComment: (Long, String?, String) -> Unit,
    onUpdateComment: (Long, String?, String) -> Unit,
    onTogglePostBookmark: (Long) -> Unit,
    onTogglePromptBookmark: (Long) -> Unit
) {
    var tagFilter by rememberSaveable { mutableStateOf("") }
    var tagExpanded by remember { mutableStateOf(false) }
    var contentTypeFilter by rememberSaveable { mutableStateOf("All") }
    var contentTypeExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Bookmarks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Saved posts and prompts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    @OptIn(ExperimentalMaterial3Api::class)
                    ExposedDropdownMenuBox(
                        expanded = contentTypeExpanded,
                        onExpandedChange = { contentTypeExpanded = !contentTypeExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = contentTypeFilter,
                            onValueChange = { },
                            label = { Text("Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = contentTypeExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            readOnly = true
                        )
                        ExposedDropdownMenu(
                            expanded = contentTypeExpanded,
                            onDismissRequest = { contentTypeExpanded = false }
                        ) {
                            listOf("All", "Posts", "Prompts").forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        contentTypeFilter = type
                                        contentTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    @OptIn(ExperimentalMaterial3Api::class)
                    ExposedDropdownMenuBox(
                        expanded = tagExpanded,
                        onExpandedChange = { tagExpanded = !tagExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = tagFilter.ifBlank { "All models" },
                            onValueChange = { tagFilter = it },
                            label = { Text("AI Model") },
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
                            DropdownMenuItem(
                                text = { Text("All models") },
                                onClick = {
                                    tagFilter = ""
                                    tagExpanded = false
                                }
                            )
                            AI_AGENTS.forEach { agent ->
                                DropdownMenuItem(
                                    text = { Text(agent) },
                                    onClick = {
                                        tagFilter = agent
                                        tagExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        val filteredPosts = bookmarkedPosts.filter { post ->
            val matchesType = contentTypeFilter == "All" || contentTypeFilter == "Posts"
            val matchesTag = tagFilter.isBlank() || post.tag.equals(tagFilter, ignoreCase = true)
            matchesType && matchesTag
        }

        val filteredPrompts = bookmarkedPrompts.filter { prompt ->
            val matchesType = contentTypeFilter == "All" || contentTypeFilter == "Prompts"
            val matchesTag = tagFilter.isBlank() || prompt.tag.equals(tagFilter, ignoreCase = true)
            matchesType && matchesTag
        }

        if (filteredPosts.isNotEmpty() && (contentTypeFilter == "All" || contentTypeFilter == "Posts")) {
            item {
                Text(
                    text = "Posts (${filteredPosts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(filteredPosts, key = { it.id }) { post ->
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
                    onToggleBookmark = onTogglePostBookmark,
                    versions = postVersions[post.id].orEmpty(),
                    enableCommentComposer = currentUser != null,
                    showVoting = post.isPublished
                )
            }
        }

        if (filteredPrompts.isNotEmpty() && (contentTypeFilter == "All" || contentTypeFilter == "Prompts")) {
            item {
                Text(
                    text = "Prompts (${filteredPrompts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = if (filteredPosts.isNotEmpty()) 16.dp else 8.dp)
                )
            }

            items(filteredPrompts, key = { it.id }) { prompt ->
                PromptCard(
                    prompt = prompt,
                    canEdit = currentUser?.id == prompt.author.id,
                    onUpdatePrompt = onUpdatePrompt,
                    onDeletePrompt = onDeletePrompt,
                    onToggleBookmark = onTogglePromptBookmark,
                    versions = promptVersions[prompt.id].orEmpty()
                )
            }
        }

        if (filteredPosts.isEmpty() && filteredPrompts.isEmpty()) {
            item {
                Text(
                    text = if (bookmarkedPosts.isEmpty() && bookmarkedPrompts.isEmpty()) {
                        "No bookmarks yet. Tap the bookmark icon to save posts and prompts."
                    } else {
                        "No items match your filters."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 12.dp)
                )
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
    var userQuery by rememberSaveable { mutableStateOf("") }
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
                        modifier = Modifier.testTag("search_tab_posts"),
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Posts") }
                    )
                    Tab(
                        modifier = Modifier.testTag("search_tab_prompts"),
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Prompts") }
                    )
                    Tab(
                        modifier = Modifier.testTag("search_tab_users"),
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
                                        .menuAnchor()
                                        .testTag("search_post_keyword_field"),
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_post_keyword_field")
                            )
                        }

                        Button(
                            onClick = {
                                postSearched = true
                                onSearchPosts(selectedType, keyword)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_post_button")
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
                                    .menuAnchor()
                                    .testTag("search_prompt_tag_field"),
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_prompt_button")
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
                            value = userQuery,
                            onValueChange = { userQuery = it },
                            label = { Text("Name or Email") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_user_email_field")
                        )
                        Button(
                            onClick = {
                                userSearched = true
                                onSearchUsers(userQuery)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_user_button")
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
    var birthDateError by remember { mutableStateOf<String?>(null) }

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
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                birthDate = selectedDate

                val age = Period.between(selectedDate, LocalDate.now()).years
                birthDateError = if (age < 18) {
                    "You must be 18 or older"
                } else {
                    null
                }
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
                        IconButton(
                            onClick = { showEditProfile = !showEditProfile },
                            modifier = Modifier.testTag("profile_edit_toggle")
                        ) {
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
                            Column {
                                OutlinedButton(
                                    onClick = {
                                        datePickerDialog.show()
                                        birthDateError = null
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("edit_profile_birthdate_button")
                                ) {
                                    Text(birthDate?.toString() ?: "Select Birth Date")
                                }
                                birthDateError?.let { errorText ->
                                    Text(
                                        text = errorText,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = bio,
                                onValueChange = { bio = it },
                                label = { Text("Bio") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp)
                                    .testTag("edit_profile_bio_field")
                            )

                            Button(
                                onClick = {
                                    birthDateError = null
                                    message = null

                                    birthDate?.let { date ->
                                        val age = Period.between(date, LocalDate.now()).years
                                        if (age < 18) {
                                            birthDateError = "You must be 18 or older"
                                            return@Button
                                        }
                                    }

                                    onUpdateProfile(birthDate, bio.trim())
                                    message = "Profile updated"
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_profile_save_button")
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
                        IconButton(
                            onClick = { showDisplaySettings = !showDisplaySettings },
                            modifier = Modifier.testTag("profile_display_toggle")
                        ) {
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
                                    },
                                    modifier = Modifier.testTag("profile_display_show_comment_titles")
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
                        IconButton(
                            onClick = { showEditPassword = !showEditPassword },
                            modifier = Modifier.testTag("profile_password_toggle")
                        ) {
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_new_password_field"),
                                visualTransformation = PasswordVisualTransformation()
                            )

                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirm Password") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_confirm_password_field"),
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_update_password_button")
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
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("profile_status_message")
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePostCard(
    onCreatePost: (String, String, String, Boolean, Boolean) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var tag by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var isDraft by rememberSaveable { mutableStateOf(false) }
    var isAnonymous by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var tagExpanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("comment_card")
    ) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_post_title_field"),
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
                        .menuAnchor()
                        .testTag("create_post_tag_field"),
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
                    .heightIn(min = 120.dp)
                    .testTag("create_post_body_field"),
                maxLines = 8
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = isDraft,
                    onCheckedChange = { isDraft = it },
                    modifier = Modifier.testTag("create_post_draft_checkbox")
                )
                Text("Save as Draft (not visible to others)")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = isAnonymous,
                    onCheckedChange = { isAnonymous = it },
                    modifier = Modifier.testTag("create_post_anonymous_checkbox")
                )
                Text("Post anonymously (name hidden from others)")
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
                            onCreatePost(title.trim(), body.trim(), tag.trim(), isDraft, isAnonymous)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_post_submit_button")
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
    onUpdatePost: (Long, String, String, String, Boolean) -> Unit,
    onDeletePost: (Long) -> Unit,
    onPublishDraft: (Long) -> Unit,
    onCreateComment: (Long, String?, String) -> Unit,
    onUpdateComment: (Long, String?, String) -> Unit,
    onVotePost: (Long, Int) -> Unit,
    onVoteComment: (Long, Int) -> Unit,
    onToggleBookmark: (Long) -> Unit = {},
    versions: List<PostVersion> = emptyList(),
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
    var editPostIsAnonymous by remember { mutableStateOf(post.isAnonymous) }
    var showHistory by remember { mutableStateOf(false) }

    val isOwnPost = currentUser?.id == post.author.id
    val displayName = if (post.isAnonymous && !isOwnPost) "Anon" else post.author.name
    val postTimeline = remember(versions, post) {
        (versions + post.toCurrentVersionSnapshot()).sortedBy { it.createdAt }
    }
    val numberedPostVersions = remember(postTimeline) {
        postTimeline.mapIndexed { index, version ->
            VersionOption(index, "Version ${index + 1} • ${version.createdAt.formatRelative()}")
        }
    }

    var advancedHistory by remember { mutableStateOf(false) }
    var baseVersionIndex by remember(numberedPostVersions) {
        mutableIntStateOf((numberedPostVersions.size - 2).coerceAtLeast(0))
    }
    var compareVersionIndex by remember(numberedPostVersions) {
        mutableIntStateOf(numberedPostVersions.lastIndex.coerceAtLeast(0))
    }

    BaseContentCard(modifier = Modifier.testTag("post_card")) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
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
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.testTag("post_draft_badge")
                        ) {
                            Text(
                                text = "DRAFT",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    if (post.isAnonymous) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.testTag("post_anonymous_badge")
                        ) {
                            Text(
                                text = "ANON",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.size(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$displayName • ${post.createdAt.formatRelative()}",
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "#${post.tag}",
                    style = MaterialTheme.typography.labelLarge
                )
                IconButton(
                    onClick = { onToggleBookmark(post.id) },
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("post_bookmark_button")
                ) {
                    Icon(
                        if (post.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark post"
                    )
                }
                if (isOwnPost) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("post_menu_button")
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
                                editPostIsAnonymous = post.isAnonymous
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
                },
                modifier = Modifier.testTag("post_vote_row"),
                upvoteTag = "post_upvote_button",
                downvoteTag = "post_downvote_button"
            )
        }

        val totalVersions = postTimeline.size
        if (totalVersions > 0 && (versions.isNotEmpty() || post.isEdited)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showHistory = true }) {
                    Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("History ($totalVersions)")
                }
            }
        }

        if (post.comments.isNotEmpty() && post.isPublished) {
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Comments (${post.comments.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("post_comments_header")
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
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("post_comment_toggle_button")
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("comment_title_field")
                    )
                    OutlinedTextField(
                        value = commentBody,
                        onValueChange = { commentBody = it },
                        label = { Text("Add a comment") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp)
                            .testTag("comment_body_field")
                    )
                    Button(
                        onClick = {
                            if (commentBody.isBlank()) return@Button
                            onCreateComment(
                                post.id,
                                commentTitle.takeIf { it.isNotBlank() }?.trim(),
                                commentBody.trim()
                            )
                            commentTitle = ""
                            commentBody = ""
                            showCommentComposer = false
                        },
                        modifier = Modifier.testTag("comment_submit_button")
                    ) {
                        Text("Post Comment")
                    }
                }
            }
        }
    }

    if (showHistory) {
        AlertDialog(
            onDismissRequest = { showHistory = false },
            confirmButton = {
                TextButton(onClick = { showHistory = false }) { Text("Close") }
            },
            title = { Text("Post Version History") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !advancedHistory,
                            onClick = { advancedHistory = false },
                            label = { Text("Timeline") }
                        )
                        FilterChip(
                            selected = advancedHistory,
                            onClick = { advancedHistory = true },
                            label = { Text("Advanced diff") }
                        )
                    }

                    if (advancedHistory) {
                        if (numberedPostVersions.size < 2) {
                            Text(
                                text = "Make at least one edit to compare versions.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            VersionSelector(
                                label = "Base version",
                                options = numberedPostVersions,
                                selectedIndex = baseVersionIndex,
                                onSelected = { selection ->
                                    baseVersionIndex = selection.coerceIn(numberedPostVersions.indices)
                                }
                            )
                            VersionSelector(
                                label = "Compare to",
                                options = numberedPostVersions,
                                selectedIndex = compareVersionIndex,
                                onSelected = { selection ->
                                    compareVersionIndex = selection.coerceIn(numberedPostVersions.indices)
                                }
                            )

                            val baseVersion = postTimeline.getOrElse(baseVersionIndex) { postTimeline.last() }
                            val compareVersion = postTimeline.getOrElse(compareVersionIndex) { postTimeline.last() }

                            DiffBlock(
                                title = "Title",
                                before = baseVersion.title,
                                after = compareVersion.title
                            )
                            DiffBlock(
                                title = "Body",
                                before = baseVersion.body,
                                after = compareVersion.body
                            )
                            DiffBlock(
                                title = "AI Model",
                                before = "#${baseVersion.tag}",
                                after = "#${compareVersion.tag}"
                            )
                        }
                    } else {
                        val numberedTimeline = postTimeline.mapIndexed { index, version -> index + 1 to version }
                        numberedTimeline.reversed().forEach { (number, version) ->
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Version $number${if (number == numberedTimeline.size) " (current)" else ""}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${version.createdAt.formatRelative()} • #${version.tag}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (number > 1) {
                                        val prevVersion = numberedTimeline.firstOrNull { it.first == number - 1 }?.second
                                        if (prevVersion != null) {
                                            val changes = mutableListOf<String>()
                                            if (version.title != prevVersion.title) changes.add("title")
                                            if (version.body != prevVersion.body) changes.add("content")
                                            if (version.tag != prevVersion.tag) changes.add("tag")

                                            if (changes.isNotEmpty()) {
                                                Text(
                                                    text = "Changed: ${changes.joinToString(", ")}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    if (version.title != post.title) {
                                        Text(
                                            text = "Title: ${version.title}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Text(
                                        text = version.body,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    if (showEditPost) {
        EditPostDialog(
            title = editPostTitle,
            tag = editPostTag,
            body = editPostBody,
            isAnonymous = editPostIsAnonymous,
            onTitleChange = { editPostTitle = it },
            onTagChange = { editPostTag = it },
            onBodyChange = { editPostBody = it },
            onAnonymousChange = { editPostIsAnonymous = it },
            onSave = {
                onUpdatePost(
                    post.id,
                    editPostTitle.trim(),
                    editPostBody.trim(),
                    editPostTag.trim(),
                    editPostIsAnonymous
                )
                showEditPost = false
            },
            onDismiss = { showEditPost = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePost(post.id)
                        showDeleteDialog = false
                    },
                    modifier = Modifier.testTag("delete_post_confirm_button")
                ) {
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
    isAnonymous: Boolean,
    onTitleChange: (String) -> Unit,
    onTagChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onAnonymousChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var tagExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        title.isBlank() -> error = "Title is required"
                        tag.isBlank() -> error = "AI Model is required"
                        body.isBlank() -> error = "Post content is required"
                        else -> {
                            error = null
                            onSave()
                        }
                    }
                },
                modifier = Modifier.testTag("edit_post_save_button")
            ) { Text("Save") }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_post_title_field")
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
                            .testTag("edit_post_tag_field")
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
                        .testTag("edit_post_body_field")
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = isAnonymous,
                        onCheckedChange = onAnonymousChange
                    )
                    Text("Post anonymously")
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
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("comment_menu_button")
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
                },
                modifier = Modifier.testTag("comment_vote_row"),
                upvoteTag = "comment_upvote_button",
                downvoteTag = "comment_downvote_button"
            )
        }
    }

    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val titleToSave: String? =
                            if (editTitle.isNotBlank()) editTitle.trim() else null
                        onUpdateComment(comment.id, titleToSave, editBody.trim())
                        showEdit = false
                    },
                    modifier = Modifier.testTag("edit_comment_save_button")
                ) {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_comment_title_field")
                    )
                    OutlinedTextField(
                        value = editBody,
                        onValueChange = { newValue: String -> editBody = newValue },
                        label = { Text("Comment") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                            .testTag("edit_comment_body_field")
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
    onDownvote: () -> Unit,
    modifier: Modifier = Modifier,
    upvoteTag: String = "",
    downvoteTag: String = ""
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onUpvote,
            modifier = Modifier
                .size(32.dp)
                .let { base -> if (upvoteTag.isNotBlank()) base.testTag(upvoteTag) else base }
        ) {
            Icon(
                imageVector = Icons.Filled.ThumbUp,
                contentDescription = "Upvote",
                modifier = Modifier.size(18.dp),
                tint = if (currentVote == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
        Text(score.toString(), style = MaterialTheme.typography.bodyMedium)
        IconButton(
            onClick = onDownvote,
            modifier = Modifier
                .size(32.dp)
                .let { base -> if (downvoteTag.isNotBlank()) base.testTag(downvoteTag) else base }
        ) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_prompt_title_field"),
                singleLine = true
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Brief Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_prompt_description_field"),
                maxLines = 2
            )
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Prompt Text (the actual prompt to use)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .testTag("create_prompt_content_field"),
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
                        .menuAnchor()
                        .testTag("create_prompt_tag_field"),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_prompt_optional_toggle")
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_prompt_temperature_field"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = context,
                        onValueChange = { context = it },
                        label = { Text("Context") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_prompt_context_field"),
                        maxLines = 3
                    )
                    OutlinedTextField(
                        value = memoryTokens,
                        onValueChange = { memoryTokens = it },
                        label = { Text("Memory Tokens") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_prompt_memory_field"),
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
                    onCheckedChange = { isPrivate = it },
                    modifier = Modifier.testTag("create_prompt_private_checkbox")
                )
                Text("Save as Private Prompt (only you can see)")
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
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_prompt_submit_button")
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
    onDeletePrompt: (Long) -> Unit,
    onToggleBookmark: (Long) -> Unit,
    versions: List<PromptVersion> = emptyList()
) {
    var showEdit by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showOptional by rememberSaveable(prompt.id) { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    val promptTimeline = remember(versions, prompt) {
        (versions + prompt.toCurrentVersionSnapshot()).sortedBy { it.createdAt }
    }
    val numberedPromptVersions = remember(promptTimeline) {
        promptTimeline.mapIndexed { index, version ->
            VersionOption(index, "Version ${index + 1} • ${version.createdAt.formatRelative()}")
        }
    }

    var promptAdvancedHistory by remember { mutableStateOf(false) }
    var promptBaseIndex by remember(numberedPromptVersions) {
        mutableIntStateOf((numberedPromptVersions.size - 2).coerceAtLeast(0))
    }
    var promptCompareIndex by remember(numberedPromptVersions) {
        mutableIntStateOf(numberedPromptVersions.lastIndex.coerceAtLeast(0))
    }

    val hasOptionalFields = !prompt.temperature.isNullOrBlank() ||
            !prompt.context.isNullOrBlank() ||
            !prompt.memoryTokens.isNullOrBlank()


    BaseContentCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.testTag("prompt_private_badge")
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${prompt.author.name} • ${prompt.createdAt.formatRelative()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (prompt.isEdited) {
                        Text(
                            text = "(edited)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "#${prompt.tag}",
                    style = MaterialTheme.typography.labelLarge
                )
                IconButton(
                    onClick = { onToggleBookmark(prompt.id) },
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("prompt_bookmark_button")
                ) {
                    Icon(
                        if (prompt.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark prompt"
                    )
                }
                if (canEdit) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("prompt_menu_button")
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

        val totalPromptVersions = promptTimeline.size
        if (totalPromptVersions > 0 && (versions.isNotEmpty() || prompt.isEdited)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showHistory = true }) {
                    Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("History ($totalPromptVersions)")
                }
            }
        }
    }

    if (showHistory) {
        AlertDialog(
            onDismissRequest = { showHistory = false },
            confirmButton = { TextButton(onClick = { showHistory = false }) { Text("Close") } },
            title = { Text("Prompt Version History") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !promptAdvancedHistory,
                            onClick = { promptAdvancedHistory = false },
                            label = { Text("Timeline") }
                        )
                        FilterChip(
                            selected = promptAdvancedHistory,
                            onClick = { promptAdvancedHistory = true },
                            label = { Text("Advanced diff") }
                        )
                    }

                    if (promptAdvancedHistory) {
                        if (numberedPromptVersions.size < 2) {
                            Text(
                                text = "Make at least one edit to compare versions.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            VersionSelector(
                                label = "Base version",
                                options = numberedPromptVersions,
                                selectedIndex = promptBaseIndex,
                                onSelected = { selection ->
                                    promptBaseIndex = selection.coerceIn(numberedPromptVersions.indices)
                                }
                            )
                            VersionSelector(
                                label = "Compare to",
                                options = numberedPromptVersions,
                                selectedIndex = promptCompareIndex,
                                onSelected = { selection ->
                                    promptCompareIndex = selection.coerceIn(numberedPromptVersions.indices)
                                }
                            )

                            val baseVersion = promptTimeline.getOrElse(promptBaseIndex) { promptTimeline.last() }
                            val compareVersion = promptTimeline.getOrElse(promptCompareIndex) { promptTimeline.last() }

                            val basePromptVersion = if (promptBaseIndex == promptTimeline.lastIndex) {
                                prompt
                            } else {
                                null
                            }

                            val onlyVisibilityChanged = baseVersion.title == compareVersion.title &&
                                    baseVersion.description == compareVersion.description &&
                                    baseVersion.content == compareVersion.content &&
                                    baseVersion.tag == compareVersion.tag &&
                                    baseVersion.temperature == compareVersion.temperature &&
                                    baseVersion.context == compareVersion.context &&
                                    baseVersion.memoryTokens == compareVersion.memoryTokens

                            if (onlyVisibilityChanged && promptBaseIndex != promptCompareIndex) {
                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Only visibility changed",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "The prompt content remained the same. Only the privacy setting was modified between these versions.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                DiffBlock(
                                    title = "Title",
                                    before = baseVersion.title,
                                    after = compareVersion.title
                                )
                                DiffBlock(
                                    title = "Description",
                                    before = baseVersion.description.ifBlank { "(empty)" },
                                    after = compareVersion.description.ifBlank { "(empty)" }
                                )
                                DiffBlock(
                                    title = "Prompt Text",
                                    before = baseVersion.content,
                                    after = compareVersion.content
                                )
                                DiffBlock(
                                    title = "AI Model",
                                    before = "#${baseVersion.tag}",
                                    after = "#${compareVersion.tag}"
                                )

                                if (baseVersion.temperature != compareVersion.temperature) {
                                    DiffBlock(
                                        title = "Temperature",
                                        before = baseVersion.temperature ?: "(not set)",
                                        after = compareVersion.temperature ?: "(not set)"
                                    )
                                }

                                if (baseVersion.context != compareVersion.context) {
                                    DiffBlock(
                                        title = "Context",
                                        before = baseVersion.context ?: "(not set)",
                                        after = compareVersion.context ?: "(not set)"
                                    )
                                }

                                if (baseVersion.memoryTokens != compareVersion.memoryTokens) {
                                    DiffBlock(
                                        title = "Memory Tokens",
                                        before = baseVersion.memoryTokens ?: "(not set)",
                                        after = compareVersion.memoryTokens ?: "(not set)"
                                    )
                                }
                            }
                        }
                    } else {
                        val numberedTimeline = promptTimeline.mapIndexed { index, version -> index + 1 to version }
                        numberedTimeline.reversed().forEach { (number, version) ->
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Version $number${if (number == numberedTimeline.size) " (current)" else ""}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${version.createdAt.formatRelative()} • #${version.tag}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (number > 1) {
                                        val prevVersion = numberedTimeline.firstOrNull { it.first == number - 1 }?.second
                                        if (prevVersion != null) {
                                            val changes = mutableListOf<String>()
                                            if (version.title != prevVersion.title) changes.add("title")
                                            if (version.description != prevVersion.description) changes.add("description")
                                            if (version.content != prevVersion.content) changes.add("content")
                                            if (version.tag != prevVersion.tag) changes.add("tag")
                                            if (version.temperature != prevVersion.temperature) changes.add("temperature")
                                            if (version.context != prevVersion.context) changes.add("context")
                                            if (version.memoryTokens != prevVersion.memoryTokens) changes.add("memory")

                                            if (changes.isNotEmpty()) {
                                                Text(
                                                    text = "Changed: ${changes.joinToString(", ")}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    if (version.title != prompt.title) {
                                        Text(
                                            text = "Title: ${version.title}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (version.description.isNotBlank()) {
                                        Text(
                                            text = version.description,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Text(
                                        text = version.content,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
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
                TextButton(
                    onClick = {
                        onDeletePrompt(prompt.id)
                        showDeleteDialog = false
                    },
                    modifier = Modifier.testTag("delete_prompt_confirm_button")
                ) {
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
            TextButton(
                onClick = {
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
                },
                modifier = Modifier.testTag("edit_prompt_save_button")
            ) { Text("Save") }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_prompt_title_field")
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_prompt_description_field")
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
                            .testTag("edit_prompt_content_field")
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
                                .testTag("edit_prompt_tag_field")
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_prompt_temperature_field")
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = context,
                            onValueChange = { context = it },
                            label = { Text("Context (optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_prompt_context_field")
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = memoryTokens,
                            onValueChange = { memoryTokens = it },
                            label = { Text("Memory Tokens (optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_prompt_memory_field")
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
                            onCheckedChange = { isPrivate = it },
                            modifier = Modifier.testTag("edit_prompt_private_checkbox")
                        )
                        Text("Save as Private Draft")
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
    val displayName = if (post.isAnonymous) "Anon" else post.author.name
    BaseContentCard(modifier = Modifier.testTag("post_summary_card")) {
        Text(
            text = post.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = "$displayName • ${post.createdAt.formatRelative()} • #${post.tag}",
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

private data class VersionOption(val index: Int, val label: String)

private enum class DiffType { ADDED, REMOVED, UNCHANGED }

private data class DiffLine(val type: DiffType, val content: String)

private fun buildDiffLines(before: String, after: String): List<DiffLine> {
    val oldLines = before.lines()
    val newLines = after.lines()
    val lcs = Array(oldLines.size + 1) { IntArray(newLines.size + 1) }

    for (i in oldLines.indices.reversed()) {
        for (j in newLines.indices.reversed()) {
            lcs[i][j] = if (oldLines[i] == newLines[j]) {
                1 + lcs[i + 1][j + 1]
            } else {
                maxOf(lcs[i + 1][j], lcs[i][j + 1])
            }
        }
    }

    val result = mutableListOf<DiffLine>()
    var i = 0
    var j = 0
    while (i < oldLines.size && j < newLines.size) {
        when {
            oldLines[i] == newLines[j] -> {
                result.add(DiffLine(DiffType.UNCHANGED, oldLines[i]))
                i++
                j++
            }

            lcs[i + 1][j] >= lcs[i][j + 1] -> {
                result.add(DiffLine(DiffType.REMOVED, oldLines[i]))
                i++
            }

            else -> {
                result.add(DiffLine(DiffType.ADDED, newLines[j]))
                j++
            }
        }
    }

    while (i < oldLines.size) {
        result.add(DiffLine(DiffType.REMOVED, oldLines[i]))
        i++
    }

    while (j < newLines.size) {
        result.add(DiffLine(DiffType.ADDED, newLines[j]))
        j++
    }

    return result
}

@Composable
private fun DiffBlock(title: String, before: String, after: String) {
    val diffLines = remember(before, after) {
        val beforeLines = before.lines()
        val afterLines = after.lines()

        if (beforeLines.size == 1 && afterLines.size == 1) {
            buildSimpleDiff(before, after)
        } else {
            buildDiffLines(before, after)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                .horizontalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                diffLines.forEach { line ->
                    val (background, prefixColor) = when (line.type) {
                        DiffType.ADDED -> Pair(
                            Color(0xFFD4EDDA),
                            Color(0xFF22863A)
                        )
                        DiffType.REMOVED -> Pair(
                            Color(0xFFFCE8E8),
                            Color(0xFFCB2431)
                        )
                        DiffType.UNCHANGED -> Pair(
                            Color.Transparent,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    val prefix = when (line.type) {
                        DiffType.ADDED -> "+"
                        DiffType.REMOVED -> "-"
                        DiffType.UNCHANGED -> " "
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(background)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = prefix,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = prefixColor,
                            modifier = Modifier.width(12.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = line.content.ifBlank { " " },
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VersionSelector(
    label: String,
    options: List<VersionOption>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.getOrNull(selectedIndex)?.label ?: label

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelected(option.index)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun Post.toCurrentVersionSnapshot(): PostVersion = PostVersion(
    id = -1,
    postId = id,
    title = title,
    body = body,
    tag = tag,
    createdAt = updatedAt
)

private fun Prompt.toCurrentVersionSnapshot(): PromptVersion = PromptVersion(
    id = -1,
    promptId = id,
    title = title,
    description = description,
    content = content,
    tag = tag,
    temperature = temperature,
    context = context,
    memoryTokens = memoryTokens,
    createdAt = updatedAt
)

private fun buildSimpleDiff(before: String, after: String): List<DiffLine> {
    return when {
        before == after -> listOf(DiffLine(DiffType.UNCHANGED, before))
        before.isEmpty() -> listOf(DiffLine(DiffType.ADDED, after))
        after.isEmpty() -> listOf(DiffLine(DiffType.REMOVED, before))
        else -> listOf(
            DiffLine(DiffType.REMOVED, before),
            DiffLine(DiffType.ADDED, after)
        )
    }
}
