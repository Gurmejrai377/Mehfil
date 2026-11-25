package com.example.ui

import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.CallQualityMode
import com.example.data.CallType
import com.example.data.MehfilRepository
import com.example.data.Story
import com.example.data.UiState
import com.example.data.UserAccount
import com.example.ui.components.IncomingCallOverlay
import com.example.ui.components.ProfileSettingsSheet
import com.example.ui.components.StoryViewerDialog
import com.example.ui.screens.ActiveCallScreen
import com.example.ui.screens.CallsTabScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatsTabScreen
import com.example.ui.screens.ContactsTabScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PartyRoomScreen
import com.example.ui.screens.RoomsTabScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MehfilChampagne
import com.example.ui.theme.MehfilDeepPurple
import com.example.ui.theme.MehfilGold
import com.example.ui.theme.MehfilMidnightPlum
import com.example.ui.theme.MehfilPurple
import com.example.ui.theme.MehfilRoyalPurple
import com.example.viewmodel.ChatDetailViewModel
import com.example.viewmodel.MehfilViewModel
import com.example.viewmodel.PartyRoomViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

private const val ROUTE_CHATS = "chats"
private const val ROUTE_ROOMS = "rooms"
private const val ROUTE_CALLS = "calls"
private const val ROUTE_CONTACTS = "contacts"
private const val ROUTE_CHAT_DETAIL = "chat_detail/{chatId}/{peerId}/{peerName}"
private const val ROUTE_PARTY_ROOM = "party_room/{roomId}"
private const val ROUTE_ACTIVE_CALL = "active_call/{callId}/{peerId}/{peerName}/{callType}/{qualityMode}"

private data class MehfilTabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val MEHFIL_TABS = listOf(
    MehfilTabItem(
        route = ROUTE_CHATS,
        label = "Chats",
        selectedIcon = Icons.Rounded.ChatBubble,
        unselectedIcon = Icons.Outlined.ChatBubbleOutline,
        testTag = "nav_tab_chats"
    ),
    MehfilTabItem(
        route = ROUTE_ROOMS,
        label = "Rooms",
        selectedIcon = Icons.Rounded.GraphicEq,
        unselectedIcon = Icons.Outlined.GraphicEq,
        testTag = "nav_tab_rooms"
    ),
    MehfilTabItem(
        route = ROUTE_CALLS,
        label = "Calls",
        selectedIcon = Icons.Rounded.Call,
        unselectedIcon = Icons.Outlined.Call,
        testTag = "nav_tab_calls"
    ),
    MehfilTabItem(
        route = ROUTE_CONTACTS,
        label = "Contacts",
        selectedIcon = Icons.Rounded.Contacts,
        unselectedIcon = Icons.Outlined.Contacts,
        testTag = "nav_tab_contacts"
    )
)

@Composable
fun MehfilApp() {
    var showSplash by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentUser by remember {
        Firebase.auth.authStateFlow()
    }.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = CredentialManager.create(context),
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    val user = currentUser
    if (user == null) {
        LoginScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedMehfilShell(
                firebaseUser = user,
                onSignOut = {
                    signOutWithCredentialManager(
                        context = context,
                        scope = scope
                    )
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedMehfilShell(
    firebaseUser: FirebaseUser,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val callRepository = remember(context) { MehfilRepository(context.applicationContext) }

    val fallbackAccount = remember(firebaseUser.uid) {
        val rawName = firebaseUser.displayName?.trim().takeIf { !it.isNullOrBlank() }
            ?: firebaseUser.email?.substringBefore("@")
            ?: "Mehfil Member"
        val cleanHandle = rawName.lowercase().replace(Regex("[^a-z0-9]"), "").take(8).ifBlank { "member" }
        UserAccount(
            id = firebaseUser.uid,
            userId = firebaseUser.uid,
            displayName = rawName,
            email = firebaseUser.email,
            mehfilId = "mehfil_${cleanHandle}_${firebaseUser.uid.takeLast(4).lowercase()}",
            bio = "Yahan har baat ek Mehfil hai"
        )
    }

    val mehfilViewModel: MehfilViewModel = viewModel(
        key = "mehfil_${firebaseUser.uid}",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                MehfilViewModel(MehfilRepository(app.applicationContext), firebaseUser.uid)
            }
        }
    )

    val accountState by mehfilViewModel.userAccount.collectAsStateWithLifecycle()
    val discoverableProfilesState by mehfilViewModel.discoverableProfilesState.collectAsStateWithLifecycle()
    val chatsState by mehfilViewModel.chatsState.collectAsStateWithLifecycle()
    val contactsState by mehfilViewModel.contactsState.collectAsStateWithLifecycle()
    val callsState by mehfilViewModel.callsState.collectAsStateWithLifecycle()
    val incomingCall by mehfilViewModel.incomingCall.collectAsStateWithLifecycle()
    val storiesState by mehfilViewModel.storiesState.collectAsStateWithLifecycle()
    val roomsState by mehfilViewModel.partyRoomsState.collectAsStateWithLifecycle()

    val account = (accountState as? UiState.Success)?.data ?: fallbackAccount
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showTabChrome = currentRoute in setOf(ROUTE_CHATS, ROUTE_ROOMS, ROUTE_CALLS, ROUTE_CONTACTS)

    val snackbarHostState = remember { SnackbarHostState() }
    var showProfileSheet by remember { mutableStateOf(false) }
    var activeStory by remember { mutableStateOf<Story?>(null) }

    LaunchedEffect(mehfilViewModel) {
        mehfilViewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val startCallAndNavigate: (peerId: String, peerName: String, callType: CallType) -> Unit =
        { peerId, peerName, callType ->
            mehfilViewModel.startOutgoingCall(peerId, peerName, callType) { callId ->
                val quality = if (account.lowDataMode) CallQualityMode.LOW_DATA else CallQualityMode.HD
                val route = "active_call/${Uri.encode(callId)}/${Uri.encode(peerId)}/${Uri.encode(peerName)}/${callType.name}/${quality.name}"
                navController.navigate(route)
            }
        }

    val openChatAndNavigate: (chatId: String, peerId: String, peerName: String) -> Unit =
        { chatId, peerId, peerName ->
            val route = "chat_detail/${Uri.encode(chatId)}/${Uri.encode(peerId)}/${Uri.encode(peerName)}"
            navController.navigate(route)
        }

    val openRoomAndNavigate: (roomId: String) -> Unit = { roomId ->
        navController.navigate("party_room/${Uri.encode(roomId)}")
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp && showTabChrome

        Scaffold(
            containerColor = MehfilMidnightPlum,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showTabChrome && !useNavigationRail) {
                    NavigationBar(
                        containerColor = MehfilDeepPurple.copy(alpha = 0.96f),
                        tonalElevation = 8.dp,
                        modifier = Modifier.border(
                            width = 0.5.dp,
                            color = MehfilGold.copy(alpha = 0.25f)
                        )
                    ) {
                        MEHFIL_TABS.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MehfilGold,
                                    selectedTextColor = MehfilGold,
                                    unselectedIconColor = MehfilChampagne.copy(alpha = 0.65f),
                                    unselectedTextColor = MehfilChampagne.copy(alpha = 0.65f),
                                    indicatorColor = MehfilPurple.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MehfilDeepPurple
                    ) {
                        MEHFIL_TABS.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MehfilGold,
                                    selectedTextColor = MehfilGold,
                                    unselectedIconColor = MehfilChampagne.copy(alpha = 0.65f),
                                    unselectedTextColor = MehfilChampagne.copy(alpha = 0.65f),
                                    indicatorColor = MehfilRoyalPurple
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_CHATS,
                    modifier = Modifier.weight(1f).fillMaxSize()
                ) {
                    composable(ROUTE_CHATS) {
                        ChatsTabScreen(
                            currentUserId = account.userId,
                            accountState = accountState,
                            chatsState = chatsState,
                            storiesState = storiesState,
                            contactsState = contactsState,
                            onOpenChat = openChatAndNavigate,
                            onStartCall = startCallAndNavigate,
                            onOpenProfile = { showProfileSheet = true },
                            onPostStory = mehfilViewModel::postStory,
                            onOpenStory = { story ->
                                mehfilViewModel.markStoryViewed(story)
                                activeStory = story
                            },
                            onGoToContacts = {
                                navController.navigate(ROUTE_CONTACTS) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onGoToRooms = {
                                navController.navigate(ROUTE_ROOMS) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onGoToCalls = {
                                navController.navigate(ROUTE_CALLS) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }

                    composable(ROUTE_ROOMS) {
                        RoomsTabScreen(
                            roomsState = roomsState,
                            onJoinRoom = openRoomAndNavigate,
                            onCreateRoom = { name, type, topic ->
                                mehfilViewModel.createPartyRoom(name, type, topic) { roomId ->
                                    openRoomAndNavigate(roomId)
                                }
                            }
                        )
                    }

                    composable(ROUTE_CALLS) {
                        CallsTabScreen(
                            currentUserId = account.userId,
                            callsState = callsState,
                            onStartCall = startCallAndNavigate
                        )
                    }

                    composable(ROUTE_CONTACTS) {
                        ContactsTabScreen(
                            accountState = accountState,
                            contactsState = contactsState,
                            discoverableProfilesState = discoverableProfilesState,
                            onAddContact = mehfilViewModel::addContact,
                            onToggleFavorite = mehfilViewModel::toggleFavorite,
                            onDeleteContact = mehfilViewModel::deleteContact,
                            onOpenChat = { peerId, peerName ->
                                mehfilViewModel.openDirectChat(peerId, peerName) { chatId ->
                                    openChatAndNavigate(chatId, peerId, peerName)
                                }
                            },
                            onStartCall = startCallAndNavigate
                        )
                    }

                    composable(
                        route = ROUTE_PARTY_ROOM,
                        arguments = listOf(
                            navArgument("roomId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val roomId = Uri.decode(backStackEntry.arguments?.getString("roomId").orEmpty())
                        val partyRoomViewModel: PartyRoomViewModel = viewModel(
                            key = "room_${account.userId}_$roomId",
                            factory = viewModelFactory {
                                initializer {
                                    val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                                    PartyRoomViewModel(
                                        repository = MehfilRepository(app.applicationContext),
                                        roomId = roomId,
                                        currentUserId = account.userId,
                                        currentUserName = account.displayName
                                    )
                                }
                            }
                        )
                        PartyRoomScreen(
                            viewModel = partyRoomViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = ROUTE_CHAT_DETAIL,
                        arguments = listOf(
                            navArgument("chatId") { type = NavType.StringType },
                            navArgument("peerId") { type = NavType.StringType },
                            navArgument("peerName") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val chatId = Uri.decode(backStackEntry.arguments?.getString("chatId").orEmpty())
                        val peerId = Uri.decode(backStackEntry.arguments?.getString("peerId").orEmpty())
                        val peerName = Uri.decode(backStackEntry.arguments?.getString("peerName").orEmpty())

                        val chatDetailViewModel: ChatDetailViewModel = viewModel(
                            key = "chat_${account.userId}_$chatId",
                            factory = viewModelFactory {
                                initializer {
                                    val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                                    ChatDetailViewModel(
                                        repository = MehfilRepository(app.applicationContext),
                                        chatId = chatId,
                                        currentUserId = account.userId
                                    )
                                }
                            }
                        )

                        ChatDetailScreen(
                            viewModel = chatDetailViewModel,
                            currentUserId = account.userId,
                            currentUserName = account.displayName,
                            peerId = peerId,
                            peerName = peerName,
                            onBack = { navController.popBackStack() },
                            onStartCall = { callType ->
                                startCallAndNavigate(peerId, peerName, callType)
                            }
                        )
                    }

                    composable(
                        route = ROUTE_ACTIVE_CALL,
                        arguments = listOf(
                            navArgument("callId") { type = NavType.StringType },
                            navArgument("peerId") { type = NavType.StringType },
                            navArgument("peerName") { type = NavType.StringType },
                            navArgument("callType") { type = NavType.StringType },
                            navArgument("qualityMode") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val callId = Uri.decode(backStackEntry.arguments?.getString("callId").orEmpty())
                        val peerId = Uri.decode(backStackEntry.arguments?.getString("peerId").orEmpty())
                        val peerName = Uri.decode(backStackEntry.arguments?.getString("peerName").orEmpty())
                        val callType = CallType.fromRaw(backStackEntry.arguments?.getString("callType"))
                        val qualityMode = CallQualityMode.fromRaw(backStackEntry.arguments?.getString("qualityMode"))

                        ActiveCallScreen(
                            repository = callRepository,
                            currentUserId = account.userId,
                            callId = callId,
                            peerId = peerId,
                            peerName = peerName,
                            initialCallType = callType,
                            initialQualityMode = qualityMode,
                            onQualityChanged = { newQuality ->
                                mehfilViewModel.updateActiveCallQuality(callId, newQuality)
                            },
                            onEndCall = { durationSeconds, finalQuality ->
                                mehfilViewModel.finishCall(
                                    callId = callId,
                                    peerId = peerId,
                                    peerName = peerName,
                                    callType = callType,
                                    durationSeconds = durationSeconds,
                                    qualityMode = finalQuality
                                )
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }

    val ringingCall = incomingCall
    if (ringingCall != null && currentRoute?.startsWith("active_call/") != true) {
        IncomingCallOverlay(
            call = ringingCall,
            onAccept = {
                mehfilViewModel.acceptIncomingCall(ringingCall) { callId, peerId, peerName, callType, quality ->
                    val route = "active_call/${Uri.encode(callId)}/${Uri.encode(peerId)}/${Uri.encode(peerName)}/${callType.name}/${quality.name}"
                    navController.navigate(route)
                }
            },
            onDecline = {
                mehfilViewModel.declineIncomingCall(ringingCall)
            }
        )
    }

    if (showProfileSheet) {
        ProfileSettingsSheet(
            account = account,
            onSave = mehfilViewModel::updateProfileSettings,
            onSignOut = {
                showProfileSheet = false
                onSignOut()
            },
            onDismiss = { showProfileSheet = false }
        )
    }

    activeStory?.let { story ->
        StoryViewerDialog(
            story = story,
            onDismiss = { activeStory = null }
        )
    }
}
