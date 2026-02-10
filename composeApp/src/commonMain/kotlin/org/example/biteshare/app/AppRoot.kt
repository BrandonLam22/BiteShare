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

private enum class Tab { Home, Review, Pick, Profile }
private sealed class PickRoute {
    data object Main : PickRoute()
    data class Recommends(val ctx: PickContext) : PickRoute()
}

@Composable
fun AppRoot() {
    val repo = remember { FakeRepository() }

    var tab by remember { mutableStateOf(Tab.Pick) }
    var pickRoute by remember { mutableStateOf<PickRoute>(PickRoute.Main) }

    val pickVm = remember { PickForMeViewModel(repo) }
    val recVm = remember { RecommendsViewModel(repo) }

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
                        onClick = { tab = Tab.Profile },
                        icon = { Text("👤") },
                        label = { Text("Profile") }
                    )
                }
            }
        ) { inner ->
            when (tab) {
                Tab.Home -> PlaceholderScreen("Home", Modifier.padding(inner))
                Tab.Review -> PlaceholderScreen("Review", Modifier.padding(inner))
                Tab.Profile -> PlaceholderScreen("Profile", Modifier.padding(inner))

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
