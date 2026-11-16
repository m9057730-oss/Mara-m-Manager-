package com.example.util

import com.example.data.model.AppLanguage
import com.example.data.model.ExpenseCategory
import com.example.data.model.InventoryCategory
import com.example.data.model.OrderChannel
import com.example.data.model.PaymentMethod

data class AppStrings(
    val appTitle: String,
    val navDashboard: String,
    val navOrders: String,
    val navMenuQr: String,
    val navStockParty: String,
    val navStaffCosts: String,
    val todayProfitLoss: String,
    val netProfit: String,
    val netLoss: String,
    val todaySales: String,
    val todayCost: String,
    val todayTarget: String,
    val laborCost: String,
    val tipsToday: String,
    val tipsMonth: String,
    val liveOrders: String,
    val pendingOrders: String,
    val completedOrders: String,
    val thirdPartyOrders: String,
    val ownDeliveryLabel: String,
    val zomatoLabel: String,
    val swiggyLabel: String,
    val otherAppLabel: String,
    val tableOrderLabel: String,
    val pickupOrderLabel: String,
    val onlineOrderLabel: String,
    val lowStockAlertTitle: String,
    val quickNewOrder: String,
    val quickScanMenu: String,
    val quickQrMenu: String,
    val quickAiManager: String,
    val printKot: String,
    val printBill: String,
    val gstApplied: String,
    val gstCancelled: String,
    val toggleGst: String,
    val autoKot: String,
    val fineDineMenu: String,
    val scanMenuPhoto: String,
    val restaurantQrMenu: String,
    val addDish: String,
    val inventoryTab: String,
    val partyBookTab: String,
    val sendWhatsAppReport: String,
    val sendChefReqWhatsApp: String,
    val staffAttendanceTab: String,
    val laborMenuTab: String,
    val dailyCostsPnlTab: String,
    val aiCalculateSalary: String,
    val markAttendance: String,
    val settingsTitle: String,
    val languageLabel: String,
    val themeColorsLabel: String,
    val accentColorLabel: String,
    val textColorLabel: String,
    val pickWallpaper: String,
    val clearWallpaper: String,
    val restartApp: String,
    val signOut: String,
    val saveSettings: String,
    val dailyCostBreakdown: String,
    val electricityPerDay: String,
    val rentPerDay: String,
    val staffFoodToday: String,
    val staffSalaryToday: String,
    val gasAndOtherToday: String,
    val expiryLossToday: String,
    val addStaff: String,
    val addInventory: String,
    val addExpense: String,
    val addPartyBooking: String,
    val paymentQrCode: String,
    val paymentCard: String,
    val paymentCash: String,
    val paymentOnline: String,
    val partyName: String,
    val partyPhone: String,
    val inventoryUsed: String,
    val chefRequirement: String,
    val revenueVsCostChart: String
) {
    fun channelLabel(channel: OrderChannel): String = when (channel) {
        OrderChannel.TABLE -> tableOrderLabel
        OrderChannel.OWN_DELIVERY -> ownDeliveryLabel
        OrderChannel.PICKUP -> pickupOrderLabel
        OrderChannel.ONLINE -> onlineOrderLabel
        OrderChannel.ZOMATO -> zomatoLabel
        OrderChannel.SWIGGY -> swiggyLabel
        OrderChannel.OTHER_APP -> otherAppLabel
    }

    fun paymentLabel(method: PaymentMethod): String = when (method) {
        PaymentMethod.CASH -> paymentCash
        PaymentMethod.CARD -> paymentCard
        PaymentMethod.QR_UPI -> paymentQrCode
        PaymentMethod.ONLINE_PREPAID -> paymentOnline
    }

    fun inventoryCatLabel(cat: InventoryCategory, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> when (cat) {
            InventoryCategory.KITCHEN -> "Kitchen Items"
            InventoryCategory.PACKING -> "Packing Material"
            InventoryCategory.TANDOOR_COAL -> "Tandoor Coal"
            InventoryCategory.COLD_DRINKS -> "Cold Drinks (Coke/Pepsi)"
        }
        AppLanguage.HINDI -> when (cat) {
            InventoryCategory.KITCHEN -> "रसोई सामग्री"
            InventoryCategory.PACKING -> "पैकिंग मटेरियल"
            InventoryCategory.TANDOOR_COAL -> "तंदूर कोयला"
            InventoryCategory.COLD_DRINKS -> "कोल्ड ड्रिंक (कोक/पेप्सी)"
        }
        AppLanguage.PUNJABI -> when (cat) {
            InventoryCategory.KITCHEN -> "ਰਸੋਈ ਸਮਾਨ"
            InventoryCategory.PACKING -> "ਪੈਕਿੰਗ ਸਮਾਨ"
            InventoryCategory.TANDOOR_COAL -> "ਤੰਦੂਰ ਕੋਲਾ"
            InventoryCategory.COLD_DRINKS -> "ਕੋਲਡ ਡ੍ਰਿੰਕ (ਕੋਕ/ਪੈਪਸੀ)"
        }
    }

    fun expenseCatLabel(cat: ExpenseCategory, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> when (cat) {
            ExpenseCategory.STAFF_SALARY -> "Staff Salary"
            ExpenseCategory.SHOP_RENT -> "Shop Rent"
            ExpenseCategory.MATERIAL_GAS -> "Material & Gas"
            ExpenseCategory.EXPIRY_LOSS -> "Expiry Material"
            ExpenseCategory.ELECTRICITY -> "Electricity Bill"
            ExpenseCategory.STAFF_FOOD -> "Staff Food"
            ExpenseCategory.OTHER -> "Other Expense"
        }
        AppLanguage.HINDI -> when (cat) {
            ExpenseCategory.STAFF_SALARY -> "स्टाफ सैलरी"
            ExpenseCategory.SHOP_RENT -> "दुकान किराया"
            ExpenseCategory.MATERIAL_GAS -> "मटेरियल व गैस"
            ExpenseCategory.EXPIRY_LOSS -> "एक्सपायरी नुकसान"
            ExpenseCategory.ELECTRICITY -> "बिजली बिल"
            ExpenseCategory.STAFF_FOOD -> "स्टाफ खाना खर्च"
            ExpenseCategory.OTHER -> "अन्य खर्च"
        }
        AppLanguage.PUNJABI -> when (cat) {
            ExpenseCategory.STAFF_SALARY -> "ਸਟਾਫ਼ ਤਨਖ਼ਾਹ"
            ExpenseCategory.SHOP_RENT -> "ਦੁਕਾਨ ਕਿਰਾਇਆ"
            ExpenseCategory.MATERIAL_GAS -> "ਮਟੀਰੀਅਲ ਤੇ ਗੈਸ"
            ExpenseCategory.EXPIRY_LOSS -> "ਐਕਸਪਾਇਰੀ ਸਮਾਨ"
            ExpenseCategory.ELECTRICITY -> "ਬਿਜਲੀ ਬਿੱਲ"
            ExpenseCategory.STAFF_FOOD -> "ਸਟਾਫ਼ ਖਾਣਾ ਖਰਚ"
            ExpenseCategory.OTHER -> "ਹੋਰ ਖਰਚ"
        }
    }
}

object L10n {
    private val en = AppStrings(
        appTitle = "Mara Manager",
        navDashboard = "Home",
        navOrders = "Orders",
        navMenuQr = "Menu & QR",
        navStockParty = "Stock & Party",
        navStaffCosts = "Staff & P&L",
        todayProfitLoss = "Today's Profit & Loss",
        netProfit = "PROFIT",
        netLoss = "LOSS",
        todaySales = "Today Sales",
        todayCost = "Today Cost",
        todayTarget = "Today Target",
        laborCost = "Labor Cost",
        tipsToday = "Tips Today",
        tipsMonth = "Tips Month",
        liveOrders = "Live Orders",
        pendingOrders = "Pending Orders",
        completedOrders = "Completed",
        thirdPartyOrders = "3rd-Party Apps (Zomato / Swiggy)",
        ownDeliveryLabel = "Own Delivery",
        zomatoLabel = "Zomato",
        swiggyLabel = "Swiggy",
        otherAppLabel = "Other App",
        tableOrderLabel = "Table",
        pickupOrderLabel = "Pickup",
        onlineOrderLabel = "Online",
        lowStockAlertTitle = "Low Stock Alert!",
        quickNewOrder = "New Order",
        quickScanMenu = "Scan Menu",
        quickQrMenu = "QR Menu",
        quickAiManager = "AI Manager",
        printKot = "Print KOT",
        printBill = "Print Bill",
        gstApplied = "GST Added",
        gstCancelled = "GST Cancelled",
        toggleGst = "Toggle GST",
        autoKot = "Auto KOT",
        fineDineMenu = "Fine-Dine Menu",
        scanMenuPhoto = "Photo → Menu AI",
        restaurantQrMenu = "QR Order Menu",
        addDish = "Add Dish",
        inventoryTab = "Inventory",
        partyBookTab = "Party Book",
        sendWhatsAppReport = "WhatsApp Stock Report",
        sendChefReqWhatsApp = "Send Chef Req to WhatsApp",
        staffAttendanceTab = "Staff & Attendance",
        laborMenuTab = "Labor Cost & Menu",
        dailyCostsPnlTab = "Daily Costs & P&L",
        aiCalculateSalary = "AI Salary & Tip Calc",
        markAttendance = "Mark Attendance",
        settingsTitle = "Full App Settings",
        languageLabel = "App Language",
        themeColorsLabel = "Happy Color Themes",
        accentColorLabel = "Accent Color",
        textColorLabel = "Word / Text Color",
        pickWallpaper = "Pick Gallery Wallpaper",
        clearWallpaper = "Remove Wallpaper",
        restartApp = "Restart App",
        signOut = "Sign Out",
        saveSettings = "Save All Settings",
        dailyCostBreakdown = "Daily Operating Cost Breakdown",
        electricityPerDay = "Electricity / Day",
        rentPerDay = "Shop Rent / Day",
        staffFoodToday = "Staff Food / Day",
        staffSalaryToday = "Staff Salary Today",
        gasAndOtherToday = "Gas & Material Today",
        expiryLossToday = "Expiry Loss Today",
        addStaff = "Add Staff",
        addInventory = "Add Inventory",
        addExpense = "Add Expense",
        addPartyBooking = "New Party Booking",
        paymentQrCode = "QR Code",
        paymentCard = "Card",
        paymentCash = "Cash",
        paymentOnline = "Online Paid",
        partyName = "Party Name",
        partyPhone = "Party Number",
        inventoryUsed = "Inventory Used",
        chefRequirement = "Chef Requirement",
        revenueVsCostChart = "Today's Profit, Sales & Cost Chart"
    )

    private val hi = AppStrings(
        appTitle = "मारा मैनेजर",
        navDashboard = "डैशबोर्ड",
        navOrders = "ऑर्डर",
        navMenuQr = "मेनू व QR",
        navStockParty = "स्टॉक व पार्टी",
        navStaffCosts = "स्टाफ व P&L",
        todayProfitLoss = "आज का प्रॉफिट और लॉस",
        netProfit = "मुनाफा",
        netLoss = "नुकसान",
        todaySales = "आज की बिक्री",
        todayCost = "आज का खर्च",
        todayTarget = "आज का टारगेट",
        laborCost = "लेबर कॉस्ट",
        tipsToday = "आज की टिप",
        tipsMonth = "महीने की टिप",
        liveOrders = "लाइव ऑर्डर",
        pendingOrders = "पेंडिंग ऑर्डर",
        completedOrders = "पूरे हुए",
        thirdPartyOrders = "थर्ड-पार्टी ऐप्स (ज़ोमैटो / स्विगी)",
        ownDeliveryLabel = "खुद की डिलीवरी",
        zomatoLabel = "ज़ोमैटो",
        swiggyLabel = "स्विगी",
        otherAppLabel = "अन्य ऐप",
        tableOrderLabel = "टेबल",
        pickupOrderLabel = "पिकअप",
        onlineOrderLabel = "ऑनलाइन",
        lowStockAlertTitle = "स्टॉक कम है!",
        quickNewOrder = "नया ऑर्डर",
        quickScanMenu = "फोटो से मेनू",
        quickQrMenu = "QR मेनू",
        quickAiManager = "AI मैनेजर",
        printKot = "KOT प्रिंट",
        printBill = "बिल प्रिंट",
        gstApplied = "GST लागू",
        gstCancelled = "GST हटाया",
        toggleGst = "GST बदलें",
        autoKot = "ऑटो KOT",
        fineDineMenu = "फाइन-डाइन मेनू",
        scanMenuPhoto = "फोटो स्कैन मेनू AI",
        restaurantQrMenu = "QR ऑर्डर मेनू",
        addDish = "डिश जोड़ें",
        inventoryTab = "इन्वेंटरी",
        partyBookTab = "पार्टी बुक",
        sendWhatsAppReport = "WhatsApp स्टॉक रिपोर्ट",
        sendChefReqWhatsApp = "शेफ लिस्ट WhatsApp भेजें",
        staffAttendanceTab = "स्टाफ व हाज़िरी",
        laborMenuTab = "लेबर कॉस्ट व मेनू",
        dailyCostsPnlTab = "दैनिक खर्च व P&L",
        aiCalculateSalary = "AI सैलरी व टिप हिसाब",
        markAttendance = "हाज़िरी लगाएं",
        settingsTitle = "पूरी ऐप सेटिंग्स",
        languageLabel = "ऐप भाषा चुनें",
        themeColorsLabel = "खुशहाल रंग थीम",
        accentColorLabel = "मुख्य रंग",
        textColorLabel = "टेक्स्ट / शब्द का रंग",
        pickWallpaper = "गैलरी से वॉलपेपर लगाएं",
        clearWallpaper = "वॉलपेपर हटाएं",
        restartApp = "ऐप रीस्टार्ट करें",
        signOut = "साइन आउट",
        saveSettings = "सेटिंग्स सेव करें",
        dailyCostBreakdown = "दैनिक खर्च का पूरा हिसाब",
        electricityPerDay = "बिजली खर्च / दिन",
        rentPerDay = "किराया / दिन",
        staffFoodToday = "स्टाफ खाना / दिन",
        staffSalaryToday = "आज की स्टाफ सैलरी",
        gasAndOtherToday = "गैस व मटेरियल खर्च",
        expiryLossToday = "एक्सपायरी मटेरियल",
        addStaff = "स्टाफ जोड़ें",
        addInventory = "इन्वेंटरी जोड़ें",
        addExpense = "खर्च जोड़ें",
        addPartyBooking = "नई पार्टी बुकिंग",
        paymentQrCode = "QR कोड",
        paymentCard = "कार्ड",
        paymentCash = "कैश",
        paymentOnline = "ऑनलाइन पेड",
        partyName = "पार्टी का नाम",
        partyPhone = "पार्टी नंबर",
        inventoryUsed = "इस्तेमाल इन्वेंटरी",
        chefRequirement = "शेफ की डिमांड",
        revenueVsCostChart = "आज बिक्री बनाम खर्च चार्ट"
    )

    private val pa = AppStrings(
        appTitle = "ਮਾਰਾ ਮੈਨੇਜਰ",
        navDashboard = "ਡੈਸ਼ਬੋਰਡ",
        navOrders = "ਆਰਡਰ",
        navMenuQr = "ਮੀਨੂ ਤੇ QR",
        navStockParty = "ਸਟਾਕ ਤੇ ਪਾਰਟੀ",
        navStaffCosts = "ਸਟਾਫ਼ ਤੇ P&L",
        todayProfitLoss = "ਅੱਜ ਦਾ ਮੁਨਾਫ਼ਾ ਤੇ ਘਾਟਾ",
        netProfit = "ਮੁਨਾਫ਼ਾ",
        netLoss = "ਘਾਟਾ",
        todaySales = "ਅੱਜ ਦੀ ਵਿਕਰੀ",
        todayCost = "ਅੱਜ ਦਾ ਖਰਚ",
        todayTarget = "ਅੱਜ ਦਾ ਟਾਰਗੇਟ",
        laborCost = "ਲੇਬਰ ਖਰਚ",
        tipsToday = "ਅੱਜ ਦੀ ਟਿੱਪ",
        tipsMonth = "ਮਹੀਨੇ ਦੀ ਟਿੱਪ",
        liveOrders = "ਲਾਈਵ ਆਰਡਰ",
        pendingOrders = "ਪੈਂਡਿੰਗ ਆਰਡਰ",
        completedOrders = "ਮੁਕੰਮਲ",
        thirdPartyOrders = "3rd-ਪਾਰਟੀ ਐਪ (ਜ਼ੋਮੈਟੋ / ਸਵਿਗੀ)",
        ownDeliveryLabel = "ਆਪਣੀ ਡਿਲੀਵਰੀ",
        zomatoLabel = "ਜ਼ੋਮੈਟੋ",
        swiggyLabel = "ਸਵਿਗੀ",
        otherAppLabel = "ਹੋਰ ਐਪ",
        tableOrderLabel = "ਟੇਬਲ",
        pickupOrderLabel = "ਪਿਕਅੱਪ",
        onlineOrderLabel = "ਆਨਲਾਈਨ",
        lowStockAlertTitle = "ਸਟਾਕ ਖ਼ਤਮ ਹੋ ਰਿਹਾ ਹੈ!",
        quickNewOrder = "ਨਵਾਂ ਆਰਡਰ",
        quickScanMenu = "ਫੋਟੋ ਤੋਂ ਮੀਨੂ",
        quickQrMenu = "QR ਮੀਨੂ",
        quickAiManager = "AI ਮੈਨੇਜਰ",
        printKot = "KOT ਪ੍ਰਿੰਟ",
        printBill = "ਬਿੱਲ ਪ੍ਰਿੰਟ",
        gstApplied = "GST ਲੱਗਿਆ",
        gstCancelled = "GST ਕੈਂਸਲ",
        toggleGst = "GST ਬਦਲੋ",
        autoKot = "ਆਟੋ KOT",
        fineDineMenu = "ਫਾਈਨ-ਡਾਈਨ ਮੀਨੂ",
        scanMenuPhoto = "ਫੋਟੋ ਸਕੈਨ ਮੀਨੂ AI",
        restaurantQrMenu = "QR ਆਰਡਰ ਮੀਨੂ",
        addDish = "ਡਿਸ਼ ਸ਼ਾਮਲ ਕਰੋ",
        inventoryTab = "ਇਨਵੈਂਟਰੀ",
        partyBookTab = "ਪਾਰਟੀ ਬੁੱਕ",
        sendWhatsAppReport = "WhatsApp ਸਟਾਕ ਰਿਪੋਰਟ",
        sendChefReqWhatsApp = "ਸ਼ੈੱਫ਼ ਲਿਸਟ WhatsApp ਭੇਜੋ",
        staffAttendanceTab = "ਸਟਾਫ਼ ਤੇ ਹਾਜ਼ਰੀ",
        laborMenuTab = "ਲੇਬਰ ਖਰਚ ਤੇ ਮੀਨੂ",
        dailyCostsPnlTab = "ਰੋਜ਼ਾਨਾ ਖਰਚ ਤੇ P&L",
        aiCalculateSalary = "AI ਤਨਖ਼ਾਹ ਤੇ ਟਿੱਪ ਹਿਸਾਬ",
        markAttendance = "ਹਾਜ਼ਰੀ ਲਗਾਓ",
        settingsTitle = "ਪੂਰੀ ਐਪ ਸੈਟਿੰਗ",
        languageLabel = "ਭਾਸ਼ਾ ਚੁਣੋ",
        themeColorsLabel = "ਖ਼ੁਸ਼ਹਾਲ ਰੰਗ ਥੀਮ",
        accentColorLabel = "ਮੁੱਖ ਰੰਗ",
        textColorLabel = "ਅੱਖਰਾਂ / ਟੈਕਸਟ ਦਾ ਰੰਗ",
        pickWallpaper = "ਗੈਲਰੀ ਤੋਂ ਵਾਲਪੇਪਰ ਲਗਾਓ",
        clearWallpaper = "ਵਾਲਪੇਪਰ ਹਟਾਓ",
        restartApp = "ਐਪ ਰੀਸਟਾਰਟ ਕਰੋ",
        signOut = "ਸਾਈਨ ਆਊਟ",
        saveSettings = "ਸੈਟਿੰਗ ਸੇਵ ਕਰੋ",
        dailyCostBreakdown = "ਰੋਜ਼ਾਨਾ ਖਰਚ ਦਾ ਪੂਰਾ ਹਿਸਾਬ",
        electricityPerDay = "ਬਿਜਲੀ ਖਰਚ / ਦਿਨ",
        rentPerDay = "ਕਿਰਾਇਆ / ਦਿਨ",
        staffFoodToday = "ਸਟਾਫ਼ ਖਾਣਾ / ਦਿਨ",
        staffSalaryToday = "ਅੱਜ ਦੀ ਸਟਾਫ਼ ਤਨਖ਼ਾਹ",
        gasAndOtherToday = "ਗੈਸ ਤੇ ਸਮਾਨ ਖਰਚ",
        expiryLossToday = "ਐਕਸਪਾਇਰੀ ਸਮਾਨ ਘਾਟਾ",
        addStaff = "ਸਟਾਫ਼ ਸ਼ਾਮਲ ਕਰੋ",
        addInventory = "ਇਨਵੈਂਟਰੀ ਸ਼ਾਮਲ ਕਰੋ",
        addExpense = "ਖਰਚ ਸ਼ਾਮਲ ਕਰੋ",
        addPartyBooking = "ਨਵੀਂ ਪਾਰਟੀ ਬੁਕਿੰਗ",
        paymentQrCode = "QR ਕੋਡ",
        paymentCard = "ਕਾਰਡ",
        paymentCash = "ਕੈਸ਼",
        paymentOnline = "ਆਨਲਾਈਨ ਪੇਡ",
        partyName = "ਪਾਰਟੀ ਦਾ ਨਾਮ",
        partyPhone = "ਪਾਰਟੀ ਨੰਬਰ",
        inventoryUsed = "ਵਰਤੀ ਇਨਵੈਂਟਰੀ",
        chefRequirement = "ਸ਼ੈੱਫ਼ ਦੀ ਲੋੜ",
        revenueVsCostChart = "ਅੱਜ ਵਿਕਰੀ ਅਤੇ ਖਰਚ ਚਾਰਟ"
    )

    fun forLang(lang: AppLanguage): AppStrings = when (lang) {
        AppLanguage.ENGLISH -> en
        AppLanguage.HINDI -> hi
        AppLanguage.PUNJABI -> pa
    }
}
