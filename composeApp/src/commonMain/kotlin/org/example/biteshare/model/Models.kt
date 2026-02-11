package org.example.biteshare.model
enum class PickMode { ME_ONLY, WITH_FRIENDS }

data class Friend(
    val id: String,
    val name: String,
)

data class Restaurant(
    val id: String,
    val name: String,
    val category: String,
    val price: String,
    val eta: String,
    val rating: Double,
    val isSaved: Boolean = false,
)

data class PickContext(
    val mode: PickMode,
    val selectedFriendIds: Set<String> = emptySet(),
)

/** 主屏分类（Local, Fast Food, Drink, Breakfast） */
data class CategoryItem(
    val id: String,
    val label: String,
)

/** 热门菜品/饮品卡片：标题为菜品名，副标题为餐厅/咖啡馆名 */
data class PopularItem(
    val id: String,
    val title: String,
    val subtitle: String,
)
