package org.example.biteshare.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.example.biteshare.data.BiteShareRepository
import org.example.biteshare.data.PickDbRepository
import org.example.biteshare.data.PickRepository
import org.example.biteshare.data.SupabaseRepository
import org.example.biteshare.domain.Model
import org.example.biteshare.domain.PickContext
import org.example.biteshare.domain.PickMode
import org.example.biteshare.domain.PickModel
import org.example.biteshare.view.BrowseView
import org.example.biteshare.view.ChangePasswordView
import org.example.biteshare.view.DetailView
import org.example.biteshare.view.EditProfileView
import org.example.biteshare.view.FriendsListView
import org.example.biteshare.view.HelpView
import org.example.biteshare.view.HomeView
import org.example.biteshare.view.PickForMeView
import org.example.biteshare.view.PrivacyView
import org.example.biteshare.view.ProfileView
import org.example.biteshare.view.RecommendsView
import org.example.biteshare.view.ReviewView
import org.example.biteshare.view.SavedView
import org.example.biteshare.view.VoteHistoryDetailView
import org.example.biteshare.view.VoteHistoryView
import org.example.biteshare.view.VoteInvitesView
import org.example.biteshare.view.VoteWithFriendsView
import org.example.biteshare.viewmodel.BrowseViewModel
import org.example.biteshare.viewmodel.ChangePasswordViewModel
import org.example.biteshare.viewmodel.DetailViewModel
import org.example.biteshare.viewmodel.EditProfileViewModel
import org.example.biteshare.viewmodel.FriendsListViewModel
import org.example.biteshare.viewmodel.HelpViewModel
import org.example.biteshare.viewmodel.HomeViewModel
import org.example.biteshare.viewmodel.PickForMeViewModel
import org.example.biteshare.viewmodel.PrivacyViewModel
import org.example.biteshare.viewmodel.ProfileViewModel
import org.example.biteshare.viewmodel.RecommendsViewModel
import org.example.biteshare.viewmodel.ReviewViewModel
import org.example.biteshare.viewmodel.SavedViewModel
import org.example.biteshare.viewmodel.VoteHistoryDetailViewModel
import org.example.biteshare.viewmodel.VoteHistoryViewModel
import org.example.biteshare.viewmodel.VoteInvitesViewModel
import org.example.biteshare.viewmodel.VoteWithFriendsViewModel
import org.example.biteshare.location.LocationAccess
import org.example.biteshare.location.NoopLocationAccess
import org.example.biteshare.view.FriendRequestsView
import org.example.biteshare.view.ReviewsListView
import org.example.biteshare.viewmodel.FriendRequestsViewModel
import org.example.biteshare.viewmodel.ReviewsListViewModel

private enum class Tab { Home, Review, Pick, Profile }

private sealed class HomeRoute {
    data object Main : HomeRoute()
    data object Browse : HomeRoute()
    data class Detail(
        val restaurantId: String,
        val origin: String // "browse", "saved", etc.
    ) : HomeRoute()
}

private sealed class PickRoute {
    data object Main : PickRoute()
    data class GroupRecommend(val ctx: PickContext) : PickRoute()
    data class Vote(val ctx: PickContext) : PickRoute()
    data class SoloRecommend(val ctx: PickContext) : PickRoute()
    data object Invites : PickRoute()
    data class JoinVote(val sessionId: String) : PickRoute()
    data object History : PickRoute()
    data class HistoryDetail(val sessionId: String) : PickRoute()
}

private sealed class ProfileRoute {
    data object Main : ProfileRoute()
    data object Saved : ProfileRoute()
    data object Privacy : ProfileRoute()
    data object Help : ProfileRoute()
    data object EditProfile : ProfileRoute()
    data object FriendsList : ProfileRoute()
    data object ChangePassword : ProfileRoute()
    data object FriendRequests : ProfileRoute()
    data object MyReviews : ProfileRoute()

}

@Composable
fun AppRoot(
    model: Model,
    locationAccess: LocationAccess = NoopLocationAccess,
) {
    val repo: BiteShareRepository = remember(model) { SupabaseRepository(model) }
    val pickRepo: PickRepository = remember(model) {
        PickDbRepository(userIdProvider = { model.currentUser?.id })
    }
    val pickModel = remember(pickRepo) { PickModel(pickRepo) }
    val scope = rememberCoroutineScope()

    var tab by remember { mutableStateOf(Tab.Home) }
    var homeRoute by remember { mutableStateOf<HomeRoute>(HomeRoute.Main) }
    var pickRoute by remember { mutableStateOf<PickRoute>(PickRoute.Main) }
    var profileRoute by remember { mutableStateOf<ProfileRoute>(ProfileRoute.Main) }

    val homeVm = remember(repo, pickRepo) { HomeViewModel(repo, pickRepo) }
    val browseVm = remember(repo) { BrowseViewModel(repo) }
    val pickVm = remember(pickModel, locationAccess) { PickForMeViewModel(pickModel, locationAccess) }
    val recVm = remember(pickModel) { RecommendsViewModel(pickModel) }
    val historyVm = remember(pickModel) { VoteHistoryViewModel(pickModel) }
    val invitesVm = remember(pickModel) { VoteInvitesViewModel(pickModel) }
    val profileVm = remember(repo) { ProfileViewModel(repo) }
    val savedVm = remember(repo) { SavedViewModel(repo) }
    val privacyVm = remember(repo) { PrivacyViewModel(repo) }
    val helpVm = remember(repo) { HelpViewModel(repo) }
    val editProfileVm = remember(repo) { EditProfileViewModel(repo) }

    val friendsListVm = remember(repo) { FriendsListViewModel(repo) }
    val changePasswordVm = remember(repo) { ChangePasswordViewModel(repo) }
    val friendRequestsVm = remember(repo) { FriendRequestsViewModel(repo) }
    val reviewsListVm = remember(repo) { ReviewsListViewModel(repo) }
    val reviewVm = remember(model, repo) { ReviewViewModel(model, repo) }


    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.Home,
                    onClick = { tab = Tab.Home },
                    icon = { Text("🏠") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = tab == Tab.Review,
                    onClick = { tab = Tab.Review },
                    icon = { Text("✍️") },
                    label = { Text("Review") }
                )
                NavigationBarItem(
                    selected = tab == Tab.Pick,
                    onClick = {
                        tab = Tab.Pick
                    },
                    icon = { Text("🎯") },
                    label = { Text("Pick") }
                )
                NavigationBarItem(
                    selected = tab == Tab.Profile,
                    onClick = {
                        tab = Tab.Profile
                        profileRoute = ProfileRoute.Main
                        profileVm.loadProfile()
                    },
                    icon = { Text("👤") },
                    label = { Text("Profile") }
                )
            }
        }
    ) { innerPadding ->
        when (tab) {
            Tab.Home -> {
                Box(Modifier.padding(innerPadding)) {
                    when (val route = homeRoute) {
                        HomeRoute.Main -> {
                            HomeView(
                                vm = homeVm,
                                onRestaurantSearch = { query ->
                                    browseVm.clearTagFilter()
                                    if (query.isBlank()) {
                                        browseVm.clearSearch()
                                    } else {
                                        browseVm.updateSearchQuery(query)
                                    }
                                    homeRoute = HomeRoute.Browse
                                },
                                onRestaurantBrowseClick = {
                                    browseVm.clearTagFilter()
                                    browseVm.clearSearch()
                                    homeRoute = HomeRoute.Browse
                                },
                                onTagClick = { tag ->
                                    browseVm.clearSearch()
                                    browseVm.applyTagFilter(tag)
                                    homeRoute = HomeRoute.Browse
                                }
                            ).Content()
                        }

                        HomeRoute.Browse -> {
                            BrowseView(
                                vm = browseVm,
                                onBack = { homeRoute = HomeRoute.Main },
                                onRestaurantClick = { id ->
                                    homeRoute = HomeRoute.Detail(id, origin = "browse")
                                }
                            ).Content()
                        }

                        is HomeRoute.Detail -> {
                            val detailVm = remember(route.restaurantId) {
                                DetailViewModel(repo, route.restaurantId)
                            }
                            DetailView(
                                vm = detailVm,
                                onBack = {
                                    when (route.origin) {
                                        "saved" -> {
                                            tab = Tab.Profile
                                            profileRoute = ProfileRoute.Saved
                                        }
                                        "browse" -> {
                                            homeRoute = HomeRoute.Browse
                                        }
                                        else -> {
                                            homeRoute = HomeRoute.Main
                                        }
                                    }
                                }
                            ).Content()
                        }
                    }
                }
            }

            Tab.Review -> {
                Box(modifier = Modifier.padding(innerPadding)) {
                    ReviewView(reviewVm)
                }
            }

            Tab.Pick -> {
                Box(Modifier.padding(innerPadding)) {
                    when (val route = pickRoute) {
                        PickRoute.Main -> {
                            PickForMeView(
                                vm = pickVm,
                                onGo = {
                                    val context = pickVm.buildPickContext()
                                    scope.launch {
                                        val items = pickModel.recommend(context)
                                        if (context.mode == PickMode.WITH_FRIENDS) {
                                            recVm.loadItems(
                                                items = items,
                                                title = "Recommended for Your Group"
                                            )
                                            pickRoute = PickRoute.GroupRecommend(context)
                                        } else {
                                            recVm.loadItems(items = items, title = "Recommends")
                                            pickRoute = PickRoute.SoloRecommend(context)
                                        }
                                    }
                                },
                                onInvites = { pickRoute = PickRoute.Invites },
                                onHistory = { pickRoute = PickRoute.History }
                            ).Content()
                        }

                        is PickRoute.GroupRecommend -> {
                            RecommendsView(
                                vm = recVm,
                                onBack = { pickRoute = PickRoute.Main },
                                actionLabel = "Start Voting",
                                onActionClick = { pickRoute = PickRoute.Vote(route.ctx) }
                            ).Content()
                        }

                        is PickRoute.Vote -> {
                            val voteVm = remember(route.ctx, recVm.uiState.items) {
                                VoteWithFriendsViewModel(
                                    model = pickModel,
                                    context = route.ctx,
                                    candidateRestaurants = recVm.uiState.items
                                )
                            }
                            VoteWithFriendsView(
                                vm = voteVm,
                                onBack = { pickRoute = PickRoute.GroupRecommend(route.ctx) },
                                onFinish = { sessionId ->
                                    if (sessionId.isNotBlank()) {
                                        pickRoute = PickRoute.HistoryDetail(sessionId)
                                    }
                                }
                            ).Content()
                        }

                        is PickRoute.SoloRecommend -> {
                            LaunchedEffect(route.ctx, recVm.uiState.items) {
                                if (recVm.uiState.items.isEmpty()) {
                                    recVm.load(route.ctx)
                                }
                            }
                            RecommendsView(
                                vm = recVm,
                                onBack = { pickRoute = PickRoute.Main }
                            ).Content()
                        }

                        PickRoute.Invites -> {
                            LaunchedEffect(Unit) {
                                invitesVm.refresh()
                            }
                            VoteInvitesView(
                                vm = invitesVm,
                                onBack = { pickRoute = PickRoute.Main },
                                onOpenSession = { sessionId ->
                                    pickRoute = PickRoute.JoinVote(sessionId)
                                }
                            ).Content()
                        }

                        is PickRoute.JoinVote -> {
                            val joinVm = remember(route.sessionId) {
                                VoteWithFriendsViewModel(
                                    model = pickModel,
                                    sessionId = route.sessionId
                                )
                            }
                            VoteWithFriendsView(
                                vm = joinVm,
                                onBack = { pickRoute = PickRoute.Invites },
                                onFinish = { sessionId ->
                                    if (sessionId.isNotBlank()) {
                                        pickRoute = PickRoute.HistoryDetail(sessionId)
                                    }
                                }
                            ).Content()
                        }

                        PickRoute.History -> {
                            LaunchedEffect(Unit) {
                                historyVm.refresh()
                            }
                            VoteHistoryView(
                                vm = historyVm,
                                onBack = { pickRoute = PickRoute.Main },
                                onOpenSession = { id -> pickRoute = PickRoute.HistoryDetail(id) }
                            ).Content()
                        }

                        is PickRoute.HistoryDetail -> {
                            val detailVm = remember(route.sessionId) {
                                VoteHistoryDetailViewModel(pickModel, route.sessionId)
                            }
                            VoteHistoryDetailView(
                                vm = detailVm,
                                onBack = { pickRoute = PickRoute.History }
                            ).Content()
                        }
                    }
                }
            }

            Tab.Profile -> {
                Box(Modifier.padding(innerPadding)) {
                    when (profileRoute) {
                        ProfileRoute.Main -> {
                            ProfileView(
                                vm = profileVm,
                                onSavedRestaurants = { profileRoute = ProfileRoute.Saved },
                                onPrivacy = { profileRoute = ProfileRoute.Privacy },
                                onHelp = { profileRoute = ProfileRoute.Help },
                                onLogout = {},
                                onEditProfile = { profileRoute = ProfileRoute.EditProfile },
                                onFriendsList = { profileRoute = ProfileRoute.FriendsList },
                                onFriendRequests = { profileRoute = ProfileRoute.FriendRequests },
                                onMyReviews = { profileRoute = ProfileRoute.MyReviews },
                            ).Content()
                        }

                        ProfileRoute.FriendRequests -> {
                            val view = remember {
                                FriendRequestsView(
                                    vm = friendRequestsVm,
                                    onBack = {
                                        profileVm.loadProfile()
                                        friendsListVm.refresh()
                                        profileRoute = ProfileRoute.Main
                                    }
                                )
                            }
                            LaunchedEffect(Unit) {
                                friendRequestsVm.loadRequests()
                            }
                            view.Content()
                        }

                        ProfileRoute.EditProfile -> {
                            EditProfileView(
                                vm = editProfileVm,
                                onBack = {
                                    profileRoute = ProfileRoute.Main
                                    profileVm.loadProfile()
                                },
                                onChangePassword = { profileRoute = ProfileRoute.ChangePassword }

                            ).Content()
                        }
                        ProfileRoute.FriendsList -> {
                            val view = remember {
                                FriendsListView(
                                    vm = friendsListVm,
                                    onBack = { profileRoute = ProfileRoute.Main }
                                )
                            }
                            LaunchedEffect(Unit) {
                                friendsListVm.refresh()
                            }
                            view.Content()
                        }

                        ProfileRoute.Saved -> {
                            val view = remember {
                                SavedView(
                                    vm = savedVm,
                                    onBack = {
                                        profileRoute = ProfileRoute.Main
                                    },
                                    onRestaurantClick = { restaurant ->
                                        tab = Tab.Home
                                        homeRoute = HomeRoute.Detail(
                                            restaurantId = restaurant.id,
                                            origin = "saved"
                                        )
                                    }
                                )
                            }
                            LaunchedEffect(Unit) {
                                savedVm.refresh()
                            }

                            view.Content()
                        }

                        ProfileRoute.Privacy -> {
                            PrivacyView(
                                vm = privacyVm,
                                onBack = { profileRoute = ProfileRoute.Main }
                            ).Content()
                        }

                        ProfileRoute.Help -> {
                            HelpView(
                                vm = helpVm,
                                onBack = { profileRoute = ProfileRoute.Main }
                            ).Content()
                        }
                        ProfileRoute.MyReviews -> {
                            val view = remember {
                                ReviewsListView(
                                    vm = reviewsListVm,
                                    onBack = { profileRoute = ProfileRoute.Main }
                                )
                            }
                            view.Content()
                        }
                        ProfileRoute.ChangePassword -> {
                            ChangePasswordView(
                                vm = changePasswordVm,
                                onBack = { profileRoute = ProfileRoute.EditProfile }
                            ).Content()
                        }
                    }
                }
            }
        }
    }
}
