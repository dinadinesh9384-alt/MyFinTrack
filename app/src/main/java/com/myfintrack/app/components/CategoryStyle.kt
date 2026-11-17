package com.myfintrack.app.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.myfintrack.app.model.Category
import com.myfintrack.app.theme.*

val Category.icon: ImageVector
    get() = when (this) {
        Category.FOOD -> Icons.Filled.Restaurant
        Category.GROCERIES -> Icons.Filled.LocalGroceryStore
        Category.TRANSPORT -> Icons.Filled.DirectionsBus
        Category.FUEL -> Icons.Filled.LocalGasStation
        Category.SHOPPING -> Icons.Filled.LocalMall
        Category.BILLS -> Icons.Filled.Receipt
        Category.RENT -> Icons.Filled.Home
        Category.HEALTH -> Icons.Filled.LocalHospital
        Category.EDUCATION -> Icons.Filled.School
        Category.ENTERTAINMENT -> Icons.Filled.Movie
        Category.TRAVEL -> Icons.Filled.Flight
        Category.PERSONAL_CARE -> Icons.Filled.Spa
        Category.FRIENDS_FAMILY -> Icons.Filled.Group
        Category.OTHER_EXPENSE -> Icons.Filled.Category
        Category.SALARY -> Icons.Filled.Work
        Category.BUSINESS -> Icons.Filled.Store
        Category.FREELANCE -> Icons.Filled.Laptop
        Category.INVESTMENT -> Icons.AutoMirrored.Filled.TrendingUp
        Category.GIFT -> Icons.Filled.CardGiftcard
        Category.REFUND -> Icons.Filled.Replay
        Category.OTHER_INCOME -> Icons.Filled.Category
        Category.TRANSFER -> Icons.Filled.SwapHoriz
    }

val Category.color: Color
    get() = when (this) {
        Category.FOOD -> CategoryFood
        Category.GROCERIES -> CategoryGroceries
        Category.TRANSPORT -> CategoryTransport
        Category.FUEL -> CategoryFuel
        Category.SHOPPING -> CategoryShopping
        Category.BILLS -> CategoryBills
        Category.RENT -> CategoryRent
        Category.HEALTH -> CategoryHealth
        Category.EDUCATION -> CategoryEducation
        Category.ENTERTAINMENT -> CategoryEntertainment
        Category.TRAVEL -> CategoryTravel
        Category.PERSONAL_CARE -> CategoryPersonalCare
        Category.FRIENDS_FAMILY -> CategoryFriends
        Category.OTHER_EXPENSE -> CategoryOthers
        Category.SALARY, Category.BUSINESS, Category.FREELANCE,
        Category.INVESTMENT, Category.GIFT, Category.REFUND,
        Category.OTHER_INCOME -> CategoryIncome
        Category.TRANSFER -> FinTransfer
    }
