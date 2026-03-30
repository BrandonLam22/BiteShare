package org.example.biteshare.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import biteshare.composeapp.generated.resources.Res
import biteshare.composeapp.generated.resources.beyayenet_home
import biteshare.composeapp.generated.resources.breakfast_home
import biteshare.composeapp.generated.resources.capo_chino_home
import biteshare.composeapp.generated.resources.chesse_burger_home
import biteshare.composeapp.generated.resources.coffee_home
import biteshare.composeapp.generated.resources.drink_home
import biteshare.composeapp.generated.resources.fast_food_home
import biteshare.composeapp.generated.resources.local_home
import biteshare.composeapp.generated.resources.milkshake_home
import biteshare.composeapp.generated.resources.pizza_home
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

fun isRemoteImageRef(ref: String): Boolean {
    val t = ref.trim()
    return t.startsWith("http://", ignoreCase = true) || t.startsWith("https://", ignoreCase = true)
}

fun imageResByKey(key: String): DrawableResource? {
    return when (key.lowercase()) {
        "pizza_home" -> Res.drawable.pizza_home
        "chesse_burger_home" -> Res.drawable.chesse_burger_home
        "coffee_home" -> Res.drawable.coffee_home
        "milkshake_home" -> Res.drawable.milkshake_home
        "capo_chino_home" -> Res.drawable.capo_chino_home
        "beyayenet_home" -> Res.drawable.beyayenet_home
        "local_home" -> Res.drawable.local_home
        "fast_food_home" -> Res.drawable.fast_food_home
        "drink_home" -> Res.drawable.drink_home
        "breakfast_home" -> Res.drawable.breakfast_home
        else -> null
    }
}

@Composable
fun RestaurantDetailImage(
    imageRef: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val key = imageRef.trim()
    when {
        isRemoteImageRef(key) -> {
            AsyncImage(
                model = key,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
            )
        }
        else -> {
            val res = imageResByKey(key)
            Box(modifier = modifier) {
                if (res != null) {
                    Image(
                        painter = painterResource(res),
                        contentDescription = contentDescription,
                        contentScale = contentScale,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("🍴", style = MaterialTheme.typography.displayLarge)
                    }
                }
            }
        }
    }
}
