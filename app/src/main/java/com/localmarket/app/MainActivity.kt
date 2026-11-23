package com.localmarket.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddBusiness
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chair
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.util.Locale

data class MarketCategory(
    val id: String,
    val title: String,
    val icon: ImageVector
)

data class MarketProduct(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val listingType: String = "بيع",
    val condition: String = "جديد",
    val priceIqd: String = "",
    val location: String = "",
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

private fun formatIraqiWhatsAppNumber(rawPhone: String): String {
    val digitsOnly = rawPhone.filter { it.isDigit() }
    return when {
        digitsOnly.startsWith("964") -> digitsOnly
        digitsOnly.startsWith("0") -> "964" + digitsOnly.drop(1)
        digitsOnly.isNotEmpty() -> "964$digitsOnly"
        else -> ""
    }
}

private fun formatIqdPrice(rawPrice: String): String {
    val digitsOnly = rawPrice.filter { it.isDigit() }
    val amount = digitsOnly.toLongOrNull() ?: return rawPrice.trim()
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return formatter.format(amount)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        LocalMarketApp()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalMarketApp() {
    val db = remember { FirebaseFirestore.getInstance() }
    val products = remember { mutableStateListOf<MarketProduct>() }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedTab by remember { mutableStateOf(0) } // 0 = المشتري، 1 = البائع
    var selectedCategory by remember { mutableStateOf("الكل") }
    var searchQuery by remember { mutableStateOf("") }

    val categories = remember {
        listOf(
            MarketCategory("all", "الكل", Icons.Rounded.Apps),
            MarketCategory("electronics", "إلكترونيات", Icons.Rounded.Devices),
            MarketCategory("clothes", "ملابس", Icons.Rounded.Checkroom),
            MarketCategory("home", "منزل ومطبخ", Icons.Rounded.Kitchen),
            MarketCategory("furniture", "أثاث", Icons.Rounded.Chair),
            MarketCategory("vehicles", "سيارات ودراجات", Icons.Rounded.DirectionsCar),
            MarketCategory("kids", "مستلزمات أطفال", Icons.Rounded.ChildCare)
        )
    }

    DisposableEffect(Unit) {
        val registration = db.collection("products")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorMessage = "تعذر تحميل المنتجات: ${error.localizedMessage ?: "خطأ في الاتصال"}"
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    products.clear()
                    for (doc in snapshot.documents) {
                        val item = MarketProduct(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            category = doc.getString("category") ?: "إلكترونيات",
                            listingType = doc.getString("listingType") ?: "بيع",
                            condition = doc.getString("condition") ?: "جديد",
                            priceIqd = doc.getString("priceIqd") ?: "",
                            location = doc.getString("location") ?: "",
                            phone = doc.getString("phone") ?: "",
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                        if (item.title.isNotBlank()) {
                            products.add(item)
                        }
                    }
                    errorMessage = null
                }
                isLoading = false
            }

        onDispose {
            registration.remove()
        }
    }

    val normalizedQuery = searchQuery.trim().lowercase()
    val filteredProducts = products.filter { product ->
        val matchesCategory = selectedCategory == "الكل" || product.category == selectedCategory
        val matchesSearch = normalizedQuery.isEmpty() ||
            product.title.lowercase().contains(normalizedQuery) ||
            product.location.lowercase().contains(normalizedQuery) ||
            product.category.lowercase().contains(normalizedQuery)
        matchesCategory && matchesSearch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedTab == 0) "السوق المحلي - تصفح المنتجات" else "السوق المحلي - إضافة منتج",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Rounded.Storefront, contentDescription = "المشتري") },
                    label = { Text("المشتري") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Rounded.AddBusiness, contentDescription = "البائع") },
                    label = { Text("البائع") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> BuyerScreen(
                modifier = Modifier.padding(innerPadding),
                categories = categories,
                selectedCategory = selectedCategory,
                onSelectCategory = { selectedCategory = it },
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                products = filteredProducts,
                isLoading = isLoading,
                errorMessage = errorMessage
            )

            1 -> SellerScreen(
                modifier = Modifier.padding(innerPadding),
                categories = categories.filter { it.title != "الكل" },
                onPublishProduct = { newProduct, onComplete ->
                    val data = hashMapOf(
                        "title" to newProduct.title,
                        "category" to newProduct.category,
                        "listingType" to newProduct.listingType,
                        "condition" to newProduct.condition,
                        "priceIqd" to newProduct.priceIqd,
                        "location" to newProduct.location,
                        "phone" to newProduct.phone,
                        "createdAt" to System.currentTimeMillis()
                    )

                    db.collection("products")
                        .add(data)
                        .addOnSuccessListener {
                            onComplete(true, null)
                            selectedTab = 0
                        }
                        .addOnFailureListener { e ->
                            onComplete(false, e.localizedMessage ?: "فشل حفظ المنتج")
                        }
                }
            )
        }
    }
}

@Composable
fun BuyerScreen(
    modifier: Modifier = Modifier,
    categories: List<MarketCategory>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    products: List<MarketProduct>,
    isLoading: Boolean,
    errorMessage: String?
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("ابحث عن منتج أو منطقة...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "بحث"
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "مسح البحث"
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories, key = { it.id }) { category ->
                FilterChip(
                    selected = selectedCategory == category.title,
                    onClick = { onSelectCategory(category.title) },
                    label = { Text(category.title) },
                    leadingIcon = {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = category.title,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider()

        when {
            isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("جاري تحميل المنتجات...")
                }
            }

            errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            products.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "لا توجد نتائج مطابقة لبحثك" else "لا توجد منتجات معروضة في هذا القسم حاليًا",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "جرّب كلمة بحث أخرى أو امسح الفلتر" else "يمكنك الانتقال إلى تبويب البائع لإضافة أول منتج",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(products, key = { it.id }) { product ->
                        val formattedPrice = formatIqdPrice(product.priceIqd)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = product.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${product.listingType} • ${product.condition}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "القسم: ${product.category}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "السعر: $formattedPrice د.ع",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocationOn,
                                        contentDescription = "الموقع",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = product.location,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (product.phone.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val dialIntent = Intent(
                                                    Intent.ACTION_DIAL,
                                                    Uri.parse("tel:${product.phone}")
                                                )
                                                context.startActivity(dialIntent)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Call,
                                                contentDescription = "اتصال بالبائع",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("اتصال")
                                        }

                                        Button(
                                            onClick = {
                                                val waNumber = formatIraqiWhatsAppNumber(product.phone)
                                                if (waNumber.isBlank()) {
                                                    Toast.makeText(context, "رقم الهاتف غير صالح لواتساب", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                val message = Uri.encode("مرحبًا، أستفسر عن منتج: ${product.title} ($formattedPrice د.ع)")
                                                val waIntent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("https://wa.me/$waNumber?text=$message")
                                                )
                                                try {
                                                    context.startActivity(waIntent)
                                                } catch (_: ActivityNotFoundException) {
                                                    Toast.makeText(context, "تعذر فتح واتساب على الجهاز", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Chat,
                                                contentDescription = "واتساب",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("واتساب")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SellerScreen(
    modifier: Modifier = Modifier,
    categories: List<MarketCategory>,
    onPublishProduct: (MarketProduct, (Boolean, String?) -> Unit) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.title ?: "إلكترونيات") }
    var listingType by remember { mutableStateOf("بيع") }
    var condition by remember { mutableStateOf("جديد") }
    var priceIqd by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val listingTypes = listOf("بيع", "إيجار", "مقايضة")
    val conditions = listOf("جديد", "مستعمل")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "نشر منتج جديد في السوق المحلي",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("عنوان المنتج") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text(text = "اختر القسم", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories, key = { it.id }) { cat ->
                FilterChip(
                    selected = selectedCategory == cat.title,
                    onClick = { selectedCategory = cat.title },
                    label = { Text(cat.title) }
                )
            }
        }

        Text(text = "نوع العرض", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listingTypes.forEach { type ->
                FilterChip(
                    selected = listingType == type,
                    onClick = { listingType = type },
                    label = { Text(type) }
                )
            }
        }

        Text(text = "حالة المنتج", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            conditions.forEach { cond ->
                FilterChip(
                    selected = condition == cond,
                    onClick = { condition = cond },
                    label = { Text(cond) }
                )
            }
        }

        OutlinedTextField(
            value = priceIqd,
            onValueChange = { input -> priceIqd = input.filter { it.isDigit() } },
            label = { Text("السعر بالدينار العراقي (د.ع)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("المنطقة / الحي") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { input -> phone = input.filter { it.isDigit() || it == '+' } },
            label = { Text("رقم هاتف التواصل") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (statusMessage != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!isError) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = statusMessage!!,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Button(
            onClick = {
                if (title.isBlank() || priceIqd.isBlank() || location.isBlank() || phone.isBlank()) {
                    isError = true
                    statusMessage = "يرجى ملء جميع الحقول المطلوبة (العنوان، السعر، المنطقة، الهاتف)."
                    return@Button
                }

                isSubmitting = true
                isError = false
                statusMessage = null

                val product = MarketProduct(
                    title = title.trim(),
                    category = selectedCategory,
                    listingType = listingType,
                    condition = condition,
                    priceIqd = priceIqd.trim(),
                    location = location.trim(),
                    phone = phone.trim()
                )

                onPublishProduct(product) { success, err ->
                    isSubmitting = false
                    if (success) {
                        title = ""
                        priceIqd = ""
                        location = ""
                        phone = ""
                        statusMessage = "تم نشر المنتج بنجاح!"
                        isError = false
                    } else {
                        statusMessage = err ?: "حدث خطأ أثناء النشر."
                        isError = true
                    }
                }
            },
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("نشر المنتج في السوق")
            }
        }
    }
}
