# PyPOS Offline Mode - Technical Specification
## Version 2.0 Roadmap

---

## 1. Overview

Enable PyPOS mobile app to work fully offline with local data caching, sync when back online, and queue transactions created offline.

---

## 2. Architecture

### 2.1 Tech Stack
- **Room Database** (Android SQLite abstraction)
- **Kotlin Coroutines** + Flow for async operations  
- **WorkManager** for background sync

### 2.2 Data Flow
```
[App] <-> [Repository] <-> [Network: online]
         \-> [Local DB] <-> [Network: offline]
```

---

## 3. Database Schema

### 3.1 Entities

```java
@Entity(tableName = "items")
- id, name, sku, barcode, category_id
- unit_price, cost, quantity, min_stock_level
- image_url, is_service, is_active
- created_at, updated_at, synced_at

@Entity(tableName = "categories")  
- id, name, description, is_active

@Entity(tableName = "sales")
- id, final_amount, payment_method, created_at
- cashier_id, synced, sync_status

@Entity(tableName = "sale_items")
- id, sale_id, item_id, quantity, unit_price, subtotal

@Entity(tableName = "pending_sync")
- id, table_name, record_id, operation, data_json, created_at
```

---

## 4. Implementation Steps

### Phase 1: Database Setup (Day 1)

1. Add Room to `build.gradle`:
```gradle
implementation "androidx.room:room-runtime:2.6.1"
kapt "androidx.room:room-compiler:2.6.1"
```

2. Create Database:
```java
@Database(entities = {...}, version = 1)
public abstract class PyPosDatabase extends RoomDatabase {
    public abstract ItemDao itemDao();
    public abstract CategoryDao categoryDao();
    public abstract SaleDao saleDao();
    public abstract PendingSyncDao pendingSyncDao();
}
```

3. Create DAO interfaces with CRUD operations

---

### Phase 2: Repository Pattern (Day 1)

```java
public class ItemsRepository {
    private ItemDao itemDao;
    private ApiService api;
    
    public LiveData<List<Item>> getItems() {
        if (NetworkUtil.isOnline()) {
            // fetch from API, save to DB, return
        } else {
            // return from local DB
        }
    }
}
```

---

### Phase 3: Offline Queue (Day 2)

1. When creating sale offline:
```java
public void createSale(Sale sale) {
    if (NetworkUtil.isOnline()) {
        api.createSale(sale);
    } else {
        // save to local DB
        // add to pending_sync queue
        // notify user "Sale saved offline"
    }
}
```

2. Sync when back online:
```java
public void syncPendingRecords() {
    List<PendingSync> pending = pendingSyncDao.getAll();
    for (record : pending) {
        try {
            api.createSale(record.data);
            pendingSyncDao.delete(record);
        } catch (e) {
            // retry later
        }
    }
}
```

---

### Phase 4: Network Monitoring + UI (Day 2)

1. Show banner when offline:
```
┌─────────────────────────────────┐
│ ⚠️ Offline Mode - Changes will   │
│    sync when connected          │
└─────────────────────────────────┘
```

2. Auto-sync on connection restore

3. Manual "Sync Now" button

---

## 5. Key Files to Create

```
app/src/main/java/com/dtcteam/pypos/data/
├── database/
│   ├── PyPosDatabase.java
│   ├── converters/
│   └── dao/
│       ├── ItemDao.java
│       ├── CategoryDao.java
│       ├── SaleDao.java
│       └── PendingSyncDao.java
├── entity/
│   ├── ItemEntity.java
│   ├── CategoryEntity.java
│   ├── SaleEntity.java
│   └── PendingSyncEntity.java
├── repository/
│   ├── ItemsRepository.java
│   ├── CategoriesRepository.java
│   └── SalesRepository.java
└── sync/
    ├── SyncManager.java
    └── NetworkUtil.java
```

---

## 6. Sync Strategy

### 6.1 Conflict Resolution
- **Server wins** for inventory changes
- **Queue** sales created offline
- **Merge** is too complex for v2

### 6.2 Sync Priority
1. User login (always needs network)
2. Items/Categories (sync on open)
3. Sales (after transaction)
4. Reports (lower priority)

---

## 7. UI Changes Needed

### 7.1 Offline Banner
- Red banner at top when offline
- Show count of pending syncs

### 7.2 Sync Status Icons
- ✓ Synced (green)
- ⏳ Pending (yellow)  
- ✗ Failed (red)

### 7.3 Manual Sync Button
- In Settings screen
- "Sync Now" with last sync timestamp

---

## 8. Testing Checklist

- [ ] Create sale when online
- [ ] Turn off wifi, create sale
- [ ] Turn on wifi, verify auto-sync
- [ ] Kill app during offline sale
- [ ] Reopen app, verify sync
- [ ] Test multiple offline sales
- [ ] Test conflict (item deleted server-side)

---

## 9. Estimated Effort

- **Code:** ~600-800 new lines
- **Files:** 12-15 new files
- **Testing:** ~1 day
- **Total:** 3-4 days

---

## 10. Questions/Decisions

- [ ] Use Kotlin or stay Java?
- [ ] Cache images offline? (large)
- [ ] Auto-sync frequency? (immediate or batched)
- [ ] Delete synced sales locally? (save space)

---

*Document created: April 2026*
*For: PyPOS v2.0 Development*