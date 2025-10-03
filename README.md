# BillGenie - Android Menu Management App

A modern Android application for managing restaurant menu items with local storage capabilities.

## 📱 Features

- ✅ **Add Menu Items**: Create new menu items with name and price
- ✅ **View Menu List**: Display all menu items in a clean, organized list
- ✅ **Edit Items**: Modify existing menu items
- ✅ **Delete Items**: Remove unwanted menu items
- ✅ **Local Storage**: All data stored locally using Room database
- ✅ **Material Design**: Modern UI following Google's Material Design guidelines
- ✅ **Offline Support**: Works completely offline

## 🛠️ Technology Stack

- **Language**: Kotlin
- **UI Framework**: Android Views with Material Design Components
- **Database**: Room (SQLite wrapper)
- **Architecture**: MVVM with Repository Pattern
- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 (API 34)

## 📁 Project Structure

```
app/src/main/
├── java/com/billgenie/
│   ├── MainActivity.kt              # Main screen with menu list
│   ├── AddMenuItemActivity.kt       # Add/Edit menu item screen
│   ├── model/
│   │   └── MenuItem.kt              # Data model
│   ├── database/
│   │   ├── BillGenieDatabase.kt     # Room database
│   │   ├── MenuItemDao.kt           # Database operations
│   │   └── MenuItemRepository.kt    # Data repository
│   └── adapter/
│       └── MenuItemAdapter.kt       # RecyclerView adapter
├── res/
│   ├── layout/
│   │   ├── activity_main.xml        # Main screen layout
│   │   ├── activity_add_menu_item.xml # Add item screen layout
│   │   └── item_menu.xml            # Menu item card layout
│   ├── values/
│   │   ├── strings.xml              # App strings
│   │   ├── colors.xml               # Color definitions
│   │   └── themes.xml               # App themes
│   └── drawable/
│       └── ic_arrow_back.xml        # Back navigation icon
└── AndroidManifest.xml              # App configuration
```

## 🚀 Getting Started

### Prerequisites

1. **Android Studio** (Arctic Fox or newer)
2. **JDK 8 or higher**
3. **Android SDK** with API 24+ installed
4. **Android device/emulator** running Android 7.0+

### Installation & Setup

1. **Clone/Download** this project to your local machine
2. **Open Android Studio**
3. **Import Project**: File → Open → Select the `billGenie` folder
4. **Sync Project**: Click "Sync Now" when prompted for Gradle sync
5. **Build Project**: Build → Make Project (Ctrl+F9)

### Running the App

#### Option 1: Using Android Emulator
1. **Create AVD**: Tools → AVD Manager → Create Virtual Device
2. **Select Device**: Choose any device (e.g., Pixel 4)
3. **Select API Level**: Choose API 24 or higher
4. **Launch Emulator**: Click the play button
5. **Run App**: Click the green "Run" button in Android Studio

#### Option 2: Using Physical Device
1. **Enable Developer Options**: Settings → About Phone → Tap "Build Number" 7 times
2. **Enable USB Debugging**: Settings → Developer Options → USB Debugging
3. **Connect Device**: Use USB cable to connect to computer
4. **Run App**: Click the green "Run" button in Android Studio

### Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests
./gradlew test

# Install on connected device
./gradlew installDebug
```

## 📱 How to Use

### Adding Menu Items
1. Launch the app
2. Tap the **"+"** floating action button
3. Enter the **item name** and **price**
4. Tap **"Add Item"** to save

### Managing Menu Items
- **Edit**: Tap on any menu item to edit it
- **Delete**: Tap the delete icon (🗑️) next to any item
- **View**: All items are displayed on the main screen

## 🗄️ Database Schema

### MenuItem Table
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-increment) |
| name | TEXT | Menu item name |
| price | REAL | Item price |
| dateAdded | TEXT | Date when item was added |

## 🔧 Development

### Key Classes

- **`MenuItem`**: Data class representing a menu item
- **`MenuItemDao`**: Database access object for CRUD operations
- **`BillGenieDatabase`**: Room database configuration
- **`MenuItemRepository`**: Handles data operations and business logic
- **`MainActivity`**: Displays menu items list
- **`AddMenuItemActivity`**: Handles adding/editing menu items
- **`MenuItemAdapter`**: RecyclerView adapter for displaying menu items

### Adding New Features

1. **Model Changes**: Update `MenuItem.kt` if new fields needed
2. **Database**: Modify `MenuItemDao.kt` for new queries
3. **UI**: Update layout files in `res/layout/`
4. **Logic**: Implement in respective Activity classes

## 🎨 UI Components

- **Material Design Cards** for menu items
- **Floating Action Button** for adding items
- **TextInputLayout** for form inputs
- **RecyclerView** for efficient list display
- **Material Color Scheme** with primary/secondary colors

## 🚀 Future Enhancements

- [ ] Menu categories
- [ ] Item images
- [ ] Export/Import functionality
- [ ] Search and filter
- [ ] Price calculations
- [ ] Multi-restaurant support
- [ ] Cloud sync
- [ ] Barcode scanning

## 📝 License

This project is open source. Feel free to use and modify as needed.

## 👨‍💻 Developer

Created as part of Android development learning project.

---

**Happy Coding! 🎉**