package org.example.biteshare.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.biteshare.model.FakeRepository
import org.example.biteshare.model.PickContext
import org.example.biteshare.view.PickForMeView
import org.example.biteshare.view.RecommendsView
import org.example.biteshare.viewmodel.PickForMeViewModel
import org.example.biteshare.viewmodel.RecommendsViewModel
import org.example.biteshare.viewmodel.ProfileViewModel
import org.example.biteshare.viewmodel.SavedViewModel
import org.example.biteshare.view.ProfileView
import org.example.biteshare.view.SavedView
import org.example.biteshare.view.PrivacyView
import org.example.biteshare.view.HelpView
import org.example.biteshare.viewmodel.PrivacyViewModel
import org.example.biteshare.viewmodel.HelpViewModel

private enum class Tab { Home, Review, Pick, Profile }
private sealed class PickRoute {
    data object Main : PickRoute()
    data class Recommends(val ctx: PickContext) : PickRoute()
}

private sealed class ProfileRoute {
    data object Main : ProfileRoute()
    data object Saved : ProfileRoute()
    data object Privacy : ProfileRoute()
    data object Help : ProfileRoute()
}

@Composable
fun AppRoot() {
    val repo = remember { FakeRepository() }

    var tab by remember { mutableStateOf(Tab.Pick) }
    var pickRoute by remember { mutableStateOf<PickRoute>(PickRoute.Main) }
    var profileRoute by remember { mutableStateOf<ProfileRoute>(ProfileRoute.Main) }
    val pickVm = remember { PickForMeViewModel(repo) }
    val recVm = remember { RecommendsViewModel(repo) }
    val profileVm = remember { ProfileViewModel(repo) }
    val savedVm = remember { SavedViewModel(repo) }
    val privacyVm = remember { PrivacyViewModel(repo) }
    val helpVm = remember { HelpViewModel(repo) }

    LaunchedEffect(pickRoute) {
        if (pickRoute is PickRoute.Recommends) {
            recVm.load((pickRoute as PickRoute.Recommends).ctx)
        }
    }

    PickMeTheme {
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
                        onClick = { tab = Tab.Pick },
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
        ) { inner ->
            when (tab) {
                Tab.Home -> PlaceholderScreen("Home", Modifier.padding(inner))
                Tab.Review -> PlaceholderScreen("Review", Modifier.padding(inner))
                Tab.Profile -> {
                    when (profileRoute) {
                        ProfileRoute.Main -> {
                            val view = remember {
                                ProfileView(
                                    vm = profileVm,
                                    onSavedRestaurants = { profileRoute = ProfileRoute.Saved },
                                    onPrivacy = { profileRoute = ProfileRoute.Privacy },
                                    onHelp = { profileRoute = ProfileRoute.Help }
                                )
                            }
                            view.Content()
                        }
                        ProfileRoute.Saved -> {
                            val view = remember {
                                SavedView(
                                    vm = savedVm,
                                    onBack = { profileRoute = ProfileRoute.Main }
                                )
                            }
                            view.Content()
                        }
                        ProfileRoute.Privacy -> {
                            val view = remember {
                                PrivacyView(
                                    vm = privacyVm,
                                    onBack = { profileRoute = ProfileRoute.Main }
                                )
                            }
                            view.Content()
                        }
                        ProfileRoute.Help -> {
                            val view = remember {
                                HelpView(
                                    vm = helpVm,
                                    onBack = { profileRoute = ProfileRoute.Main }
                                )
                            }
                            view.Content()
                        }
                    }
                }
                Tab.Pick -> {
                    when (val r = pickRoute) {
                        PickRoute.Main -> {
                            val view = remember {
                                PickForMeView(
                                    vm = pickVm,
                                    onGo = {
                                        pickRoute = PickRoute.Recommends(pickVm.buildPickContext())
                                    }
                                )
                            }
                            view.Content()
                        }
                        is PickRoute.Recommends -> {
                            val view = remember { RecommendsView(recVm) }
                            view.Content()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(24.dp)
        )
    }
}
