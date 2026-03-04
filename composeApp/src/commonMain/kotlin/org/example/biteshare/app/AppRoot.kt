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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import org.example.biteshare.data.FakeRepository
import org.example.biteshare.domain.Model
import org.example.biteshare.domain.PickContext
import org.example.biteshare.domain.PickMode
import org.example.biteshare.domain.PickModel
import org.example.biteshare.view.BrowseView
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
import org.example.biteshare.view.VoteWithFriendsView
import org.example.biteshare.viewmodel.BrowseViewModel
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
import org.example.biteshare.viewmodel.VoteWithFriendsViewModel

private enum class Tab { Home, Review, Pick, Profile }

private sealed class HomeRoute {
    data object Main : HomeRoute()
    data object Browse : HomeRoute()
    data class Detail(val restaurantId: String) : HomeRoute()
}

private sealed class PickRoute {
    data object Main : PickRoute()
    data class Generated(val ctx: PickContext) : PickRoute()
    data class Vote(val ctx: PickContext) : PickRoute()
    data class Recommends(val ctx: PickContext) : PickRoute()
}

private sealed class ProfileRoute {
    data object Main : ProfileRoute()
    data object Saved : ProfileRoute()
    data object Privacy : ProfileRoute()
    data object Help : ProfileRoute()
    data object EditProfile : ProfileRoute()
    data object FriendsList : ProfileRoute()
}

@Composable
fun AppRoot(model: Model) {
    val repo = remember(model) { FakeRepository(model) }
    val pickModel = remember(repo) { PickModel(repo) }

    var tab by remember { mutableStateOf(Tab.Home) }
    var homeRoute by remember { mutableStateOf<HomeRoute>(HomeRoute.Main) }
    var pickRoute by remember { mutableStateOf<PickRoute>(PickRoute.Main) }
    var profileRoute by remember { mutableStateOf<ProfileRoute>(ProfileRoute.Main) }

    val homeVm = remember(repo) { HomeViewModel(repo) }
    val browseVm = remember(repo) { BrowseViewModel(repo) }
    val pickVm = remember(pickModel) { PickForMeViewModel(pickModel) }
    val recVm = remember(pickModel) { RecommendsViewModel(pickModel) }
    val profileVm = remember(repo) { ProfileViewModel(repo) }
    val savedVm = remember(repo) { SavedViewModel(repo) }
    val privacyVm = remember(repo) { PrivacyViewModel(repo) }
    val helpVm = remember(repo) { HelpViewModel(repo) }
    val editProfileVm = remember(repo) { EditProfileViewModel(repo) }
    val reviewVm = remember(model) { ReviewViewModel(model) }
    val friendsListVm = remember(repo) { FriendsListViewModel(repo) }


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
                                onSearchClick = {
                                    browseVm.clearTagFilter()
                                    homeRoute = HomeRoute.Browse
                                },
                                onTagClick = { tag ->
                                    browseVm.applyTagFilter(tag)
                                    homeRoute = HomeRoute.Browse
                                }
                            ).Content()
                        }

                        HomeRoute.Browse -> {
                            BrowseView(
                                vm = browseVm,
                                onBack = { homeRoute = HomeRoute.Main },
                                onRestaurantClick = { id -> homeRoute = HomeRoute.Detail(id) }
                            ).Content()
                        }

                        is HomeRoute.Detail -> {
                            val detailVm = remember(route.restaurantId) {
                                DetailViewModel(repo, route.restaurantId)
                            }
                            DetailView(
                                vm = detailVm,
                                onBack = { homeRoute = HomeRoute.Browse }
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
                                    val items = pickModel.recommend(context)
                                    if (context.mode == PickMode.WITH_FRIENDS) {
                                        recVm.loadItems(
                                            items = items,
                                            title = "Recommended for Your Group"
                                        )
                                        pickRoute = PickRoute.Generated(context)
                                    } else {
                                        recVm.loadItems(items = items, title = "Recommends")
                                        pickRoute = PickRoute.Recommends(context)
                                    }
                                }
                            ).Content()
                        }

                        is PickRoute.Generated -> {
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
                                onBack = { pickRoute = PickRoute.Generated(route.ctx) }
                            ).Content()
                        }

                        is PickRoute.Recommends -> {
                            LaunchedEffect(route.ctx) {
                                recVm.load(route.ctx)
                            }
                            RecommendsView(
                                vm = recVm,
                                onBack = { pickRoute = PickRoute.Main }
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
                                onFriendsList = { profileRoute = ProfileRoute.FriendsList }
                            ).Content()
                        }

                        ProfileRoute.EditProfile -> {
                            EditProfileView(
                                vm = editProfileVm,
                                onBack = {
                                    profileRoute = ProfileRoute.Main
                                    profileVm.loadProfile()
                                }
                            ).Content()
                        }
                        ProfileRoute.FriendsList -> {
                            val view = remember {
                                FriendsListView(
                                    vm = friendsListVm,
                                    onBack = { profileRoute = ProfileRoute.Main }
                                )
                            }
                            view.Content()
                        }

                        ProfileRoute.Saved -> {
                            val view = remember {
                                SavedView(
                                    vm = savedVm,
                                    onBack = {
                                        profileRoute = ProfileRoute.Main
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
                    }
                }
            }
        }
    }
}
