package com.localmarket.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

data class CategoryItem(
    val name: String,
    val icon: ImageVector
)

data class ProductItem(
    val id: String,
    val title: String,
    val category: String,
    val productType: String,
    val condition: String, // "جديد" أو "مستخدم"
    val priceIqd: Long,
    val address: String,
    val phone: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme {
                    LocalMarketApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalMarketApp() {
    val categories = remember {
        listOf(
            CategoryItem("الكل", Icons.Default.Apps),
            CategoryItem("إلكترونيات", Icons.Default.Devices),
            CategoryItem("ملابس", Icons.Default.Checkroom),
            CategoryItem("منزل ومطبخ", Icons.Default.Kitchen),
            CategoryItem("أثاث", Icons.Default.Chair),
            CategoryItem("سيارات ودراجات", Icons.Default.DirectionsCar),
            CategoryItem("أطفال", Icons.Default.ChildCare)
        )
    }

    val products = remember {
        mutableStateListOf(
            ProductItem(
                id = "1",
                title = "ثلاجة بابين حجم متوسط",
                category = "منزل ومطبخ",
                productType = "تبريد وتجميد",
                condition = "مستخدم",
                priceIqd = 285000,
                address = "الحي المركزي - قرب السوق الرئيسي",
                phone = "07701234567"
            ),
            ProductItem(
                id = "2",
                title = "سماعة لاسلكية أصلية",
                category = "إلكترونيات",
                productType = "ملحقات صوتية",
                condition = "جديد",
                priceIqd = 35000,
                address = "حي الجامعة - شارع الخدمات",
                phone = "07809876543"
            )
        )
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: مشتري، 1: بائع
    var selectedCategory by remember { mutableStateOf("الكل") }
    var selectedProduct by remember { mutableStateOf<ProductItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedTab == 0) "سوق المنطقة - المشترون" else "سوق المنطقة - نشر منتج",
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
                    icon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                    label = { Text("المشترون") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.AddBusiness, contentDescription = null) },
                    label = { Text("البائعون") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (selectedTab == 0) {
                BuyerScreen(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it },
                    products = products.filter {
                        selectedCategory == "الكل" || it.category == selectedCategory
                    },
                    onProductClick = { selectedProduct = it }
                )
            } else {
                SellerScreen(
                    categories = categories.filter { it.name != "الكل" }.map { it.name },
                    onPublishProduct = { newProduct ->
                        products.add(0, newProduct)
                        selectedCategory = "الكل"
                        selectedTab = 0
                    }
                )
            }

            selectedProduct?.let { product ->
                ProductDetailsDialog(
                    product = product,
                    onDismiss = { selectedProduct = null }
                )
            }
        }
    }
}

@Composable
fun BuyerScreen(
    categories: List<CategoryItem>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    products: List<ProductItem>,
    onProductClick: (ProductItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text(
            text = "اختر القسم بالنقر على الأيقونة:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(categories) { category ->
                val isSelected = category.name == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectCategory(category.name) },
                    label = { Text(category.name) },
                    leadingIcon = {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = category.name,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (products.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد منتجات في هذا القسم حاليًا.")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(products, key = { it.id }) { product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onProductClick(product) }
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
                                AssistChip(
                                    onClick = {},
                                    label = { Text(product.condition) }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "القسم: ${product.category} • النوع: ${product.productType}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "السعر: ${formatIqd(product.priceIqd)} د.ع",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "العنوان: ${product.address} • الهاتف: ${product.phone}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerScreen(
    categories: List<String>,
    onPublishProduct: (ProductItem) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull() ?: "إلكترونيات") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var productType by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("جديد") }
    var priceText by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "يرجى تعبئة جميع بيانات المنتج بالكامل قبل النشر:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("اسم المنتج *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = !categoryExpanded }
        ) {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("القسم *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                categories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = {
                            selectedCategory = cat
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = productType,
            onValueChange = { productType = it },
            label = { Text("نوع المنتج (مثال: هاتف، قميص، طاولة) *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text(text = "حالة المنتج *", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("جديد", "مستخدم").forEach { itemCondition ->
                FilterChip(
                    selected = condition == itemCondition,
                    onClick = { condition = itemCondition },
                    label = { Text(itemCondition) }
                )
            }
        }

        OutlinedTextField(
            value = priceText,
            onValueChange = { input -> priceText = input.filter { it.isDigit() } },
            label = { Text("السعر بالدينار العراقي (د.ع) *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("العنوان (المنطقة / الحي / أقرب نقطة دالة) *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { input -> phone = input.filter { it.isDigit() || it == '+' } },
            label = { Text("رقم الهاتف للتواصل *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }

        Button(
            onClick = {
                val parsedPrice = priceText.toLongOrNull()
                when {
                    title.isBlank() -> errorMessage = "يرجى إدخال اسم المنتج."
                    productType.isBlank() -> errorMessage = "يرجى إدخال نوع المنتج."
                    parsedPrice == null || parsedPrice <= 0L -> errorMessage = "يرجى إدخال سعر صحيح بالدينار العراقي."
                    address.isBlank() -> errorMessage = "يرجى إدخال عنوان البائع في المنطقة."
                    phone.trim().length < 7 -> errorMessage = "يرجى إدخال رقم هاتف صحيح للتواصل."
                    else -> {
                        errorMessage = null
                        onPublishProduct(
                            ProductItem(
                                id = System.currentTimeMillis().toString(),
                                title = title.trim(),
                                category = selectedCategory,
                                productType = productType.trim(),
                                condition = condition,
                                priceIqd = parsedPrice,
                                address = address.trim(),
                                phone = phone.trim()
                            )
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("نشر المنتج في السوق", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProductDetailsDialog(
    product: ProductItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(product.title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("القسم: ${product.category}")
                Text("النوع: ${product.productType}")
                Text("الحالة: ${product.condition}")
                Text(
                    text = "السعر: ${formatIqd(product.priceIqd)} د.ع",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("العنوان: ${product.address}", fontWeight = FontWeight.SemiBold)
                Text("رقم الهاتف: ${product.phone}", fontWeight = FontWeight.SemiBold)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${product.phone}")
                    }
                    context.startActivity(intent)
                }
            ) {
                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("اتصال بالبائع")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

fun formatIqd(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return formatter.format(amount)
}
