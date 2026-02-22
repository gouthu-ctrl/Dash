# Dash Trip Planner - Project Status

> **Version:** 1.0.0  
> **Last Updated:** 2026-01-30  
> **Schema Version:** v5.0

---

## 📊 Executive Summary

| Category | Status |
|----------|--------|
| **Core App** | ✅ Buildable |
| **Authentication** | ✅ Implemented (Google Sign-In) |
| **Trip Management** | ✅ Implemented |
| **Itinerary Management** | ✅ Implemented |
| **Offline Support** | 🟡 Partial (Room DB ready, sync logic present) |
| **AI Features** | 🔴 Stub Only |
| **Collaboration** | 🟡 Partial (UI ready, real-time needs testing) |
| **Document Vault** | 🟡 Partial (Upload/download ready) |

---

## ✅ Features Implemented

### 1. Authentication & Profiles
- [x] Google Sign-In via Credential Manager
- [x] Supabase Auth integration
- [x] Auto profile creation on signup (trigger)
- [x] Profile model with subscription tier (FREE/PRO)
- [x] `FeatureManager` for Pro feature gating

### 2. Trip Management (CRUD)
- [x] Create trip with title, dates, timezone, notes
- [x] Custom fields (name/value pairs) via `custom_attributes`
- [x] Trip list view (HomeScreen)
- [x] Trip detail view with timeline
- [x] Edit/delete trips
- [x] Drag-drop reordering (display_order)
- [x] Trip visibility settings (private/friends/public)
- [x] Share token generation

### 3. Itinerary Management
- [x] Add itinerary items (flights, hotels, activities, etc.)
- [x] Item types: `flight`, `hotel`, `activity`, `restaurant`, `transport`, `other`
- [x] Day-by-day timeline view (`DayTimelineScreen`)
- [x] Location picker with Mapbox/Photon geocoding
- [x] Sorting by date/time
- [x] Drag-drop reordering within day
- [x] Item status workflow (draft/confirmed/proposed/archived)

### 4. Collaborative Features
- [x] Trip members model & RLS policies
- [x] Member invitation flow (by email)
- [x] Role management (owner/editor/viewer)
- [x] `MemberManagementScreen` & components
- [x] Voting on proposed items (upvote/downvote/heart)
- [x] `VotingScreen` & `VotingViewModel`
- [x] Collaboration indicators component


### 5. Offline Support Infrastructure
- [x] Room database setup (`DashDatabase`)
- [x] `TripDao` & `ItineraryDao` with dirty flag tracking
- [x] `SyncRepository` for offline-first reads
- [x] Push/pull sync logic for trips & items
- [x] Soft delete with eventual sync
- [x] `OfflineSyncIndicator` component

### 6. Document Vault
- [x] `DocumentRepository` with Supabase Storage
- [x] Upload/download files
- [x] Document categories (passport, visa, ticket, insurance, health, other)
- [x] Expiry date tracking
- [x] Signed URL generation for private access
- [x] `DocumentVaultScreen` UI
- [x] `DocumentVaultViewModel`

### 7. Location & Maps
- [x] `LocationRepository` with Mapbox Geocoding API
- [x] `LocationSearchViewModel` with debounced search
- [x] `LocationPicker` component
- [x] `MapViewScreen` (basic implementation)
- [x] Location coordinates stored on items

### 8. UI/UX Components
- [x] Material Design 3 theme
- [x] `OnboardingComponents` with tooltips
- [x] `WelcomeScreen` with carousel
- [x] `DashComponents` (cards, buttons, FABs)
- [x] `TripFormComponents` & `TripDetailComponents`
- [x] Navigation with animations
- [x] Settings screen
- [x] Profile screen

### 9. Real-time Subscriptions
- [x] Supabase Realtime enabled for `trips` & `itinerary_items`
- [x] `subscribeToTripChanges()` in TripRepository
- [x] `subscribeToItineraryChanges()` in TripRepository
- [x] `subscribeToVoteChanges()` in TripRepository

### 10. AI Trip Planning (Hybrid: Nano + Flash)
| Component | Status | Notes |
|-----------|--------|-------|
| `AIRepository` | ✅ Ready | Implements Strategy pattern (Cloud vs On-Device) |
| `AIPlannerViewModel` | ✅ Ready | Fully connected to Repository |
| `AIPlannerScreen` | ✅ Ready | Conversational UI active |
| Edge Function | ✅ Ready | `generate-plan` deployed with Gemini 1.5 Flash |
| On-Device AI | 🟡 Beta | `OnDeviceGeminiStrategy` implemented for supported devices |

**Architecture:**
- **Primary:** usage of **Gemini Nano** on supported devices (Pixel 10 Pro, etc) -> Zero Latency, privacy.
- **Fallback:** usage of **Supabase Edge Function** wrapping **Gemini 2.5 Flash** -> Low cost, universal access.

---

## 🟡 Partially Implemented (Needs Work)




### 1. Offline Mode (Itinerary Sync)
| Component | Status | Notes |
|-----------|--------|-------|
| Trip sync | ✅ Working | Push/pull implemented |
| Itinerary sync | 🟡 Basic | Per-trip refresh only |
| Map snapshots | 🔴 Missing | Need offline map tiles |
| Conflict resolution | 🔴 Missing | Last-write-wins currently |

### 2. Price Alerts
| Component | Status | Notes |
|-----------|--------|-------|
| `PriceAlertRepository` | ✅ CRUD | Supabase table ready |
| `PriceAlertsScreen` | ✅ UI Ready | Display & create alerts |
| Price fetching API | 🔴 Missing | No flight/hotel API integrated |
| Background worker | 🔴 Missing | Need WorkManager job |

### 3. Trip Chat (Real-time)
| Component | Status | Notes |
|-----------|--------|-------|
| `trip_chat_messages` table | ✅ Ready | Schema in place |
| `TripChatScreen` | 🔴 Disabled | Entry point removed |
| Real-time subscription | � Missing | Not implemented |
| Push notifications | 🔴 Missing | Need FCM setup |

### 4. Photo Location Extraction
| Component | Status | Notes |
|-----------|--------|-------|
| `PhotoLocationScreen` | ✅ Ready | Picker & UI |
| `PhotoLocationViewModel` | ✅ Ready | ML Kit integrated |
| ML Kit Text Recognition | ✅ Working | On-device OCR |
| Place name extraction | 🟡 Basic | May need NER improvement |

### 5. Sharing Features
| Component | Status | Notes |
|-----------|--------|-------|
| Share token | ✅ Implemented | UUID per trip |
| `ShareTripScreen` | ✅ UI Ready | Copy link, QR code |
| View-only mode | 🟡 Needs Testing | RLS supports it |
| PDF export | 🔴 Missing | Need PDF generation library |

---

## 🔴 Not Yet Implemented

### Core Features

| Feature | Priority | Complexity | Notes |
|---------|----------|------------|-------|
| **Home Country/Currency by IP** | High | Medium | Need IP geolocation API |
| **Booking Reminders** | High | Medium | WorkManager + notifications |
| **Calendar Sync** | High | High | Android Calendar Provider API |
| **Widget Support** | Medium | High | Android Glance API |
| **Deep Links to Airlines/Hotels** | Medium | Low | Intent handling |
| **Flight Status Alerts** | High | High | Need FlightAware/similar API |
| **Expense Tracking & Splits** | Medium | Medium | Table ready, UI needed |

### AI & Intelligence

| Feature | Priority | Complexity | Notes |
|---------|----------|------------|-------|
| **AI Trip Generation** | High | High | Edge Function + OpenAI |
| **Conversational Refinement** | High | High | Chat UI ready, backend needed |
| **Itinerary Optimization** | Medium | High | Component exists, needs algorithm |
| **Recommendations from History** | Low | High | Need ML pipeline |
| **Vector DB (Pinecone)** | Low | High | `place_knowledge` table ready |
| **AI Suggestions for Active Trips** | High | Medium | Intuitive add-to-trip flow |

### Social Features

| Feature | Priority | Complexity | Notes |
|---------|----------|------------|-------|
| **Social Proof ("Friends visited")** | Medium | Medium | `user_relationships` table ready |
| **Contact Integration** | Medium | Medium | Need Contacts permission |
| **Influencer Templates** | Low | Low | `is_template` flag exists |
| **Template Marketplace** | Low | Medium | UI stub exists |

### Document Management

| Feature | Priority | Complexity | Notes |
|---------|----------|------------|-------|
| **Encryption at Rest** | High | Medium | `is_encrypted` flag ready |
| **Booking Email Parsing** | Medium | High | `booking_inbox` table ready |
| **Auto-import from Gmail** | Low | High | Gmail API + parsing |

---

## 🔌 API Integrations Required

### Must Have (MVP)

| API | Purpose | Status | Notes |
|-----|---------|--------|-------|
| **Supabase** | Backend | ✅ Connected | Auth, DB, Storage, Realtime |
| **Mapbox Geocoding** | Location search | ✅ Connected | Token in local.properties |
| **OpenAI (GPT-4)** | AI planning | 🔴 Pending | For Edge Function |
| **IP Geolocation** | Home country/currency | 🔴 Needed | ipapi.co or similar |

### Nice to Have (Post-MVP)

| API | Purpose | Status | Cost |
|-----|---------|--------|------|
| **FlightAware** | Flight tracking | 🔴 Not started | $$$$ |
| **Amadeus** | Flight/hotel booking | 🔴 Not started | Freemium |
| **SkyScanner** | Price comparison | 🔴 Not started | Freemium |
| **Google Calendar** | Sync | 🔴 Not started | Free (OAuth) |
| **Firebase Cloud Messaging** | Push notifications | 🔴 Not started | Free |
| **Pinecone** | Vector search | 🔴 Not started | Freemium |
| **Stripe** | Subscriptions | 🔴 Not started | 2.9% + fees |

---

## 📁 Project Structure

```
app/src/main/java/com/dash/travel/
├── MainActivity.kt           # Navigation host
├── ProfileUpdateHelper.kt    # Profile sync helper
├── data/
│   ├── local/
│   │   ├── DashDatabase.kt   # Room database
│   │   ├── EntityMappers.kt  # Entity ↔ Model conversions
│   │   ├── dao/              # TripDao, ItineraryDao, RecentLocationsDao
│   │   └── entity/           # TripEntity, ItineraryItemEntity
│   ├── model/
│   │   └── SupabaseModels.kt # All Supabase table models
│   ├── models/               # Legacy models (Trip, ItineraryItem)
│   ├── remote/
│   │   └── SupabaseManager.kt # Supabase client singleton
│   ├── repository/
│   │   ├── AIRepository.kt
│   │   ├── DocumentRepository.kt
│   │   ├── LocationRepository.kt
│   │   ├── PriceAlertRepository.kt
│   │   ├── ProfileRepository.kt
│   │   ├── SyncRepository.kt
│   │   └── TripRepository.kt
│   └── search/               # Search utilities
├── di/
│   └── DiContainer.kt        # Manual DI container
├── ui/
│   ├── components/           # Reusable UI components
│   ├── onboarding/           # Onboarding flow
│   ├── screens/              # All app screens (25+)
│   ├── theme/                # Material 3 theme
│   └── viewmodel/            # ViewModels (10)
└── util/
    └── FeatureManager.kt     # Pro feature gating
```

---

## 📱 Screens Inventory

| Screen | File | Status |
|--------|------|--------|
| Welcome | `WelcomeScreen.kt` | ✅ |
| Home | `HomeScreen.kt` | ✅ |
| Add Trip | `AddTripScreen.kt` | ✅ |
| Trip Detail | `TripDetailScreen.kt` | ✅ |
| Day Timeline | `DayTimelineScreen.kt` | ✅ |
| Add Itinerary | `AddItineraryScreen.kt` | ✅ |
| Voting | `VotingScreen.kt` | ✅ |

| AI Planner | `AIPlannerScreen.kt` | 🟡 |
| Document Vault | `DocumentVaultScreen.kt` | 🟡 |
| Map View | `MapViewScreen.kt` | 🟡 |
| Price Alerts | `PriceAlertsScreen.kt` | 🟡 |
| Flight Alerts | `FlightAlertsScreen.kt` | 🟡 |
| Share Trip | `ShareTripScreen.kt` | 🟡 |
| Reminders | `RemindersScreen.kt` | 🟡 |
| Offline Manager | `OfflineManagerScreen.kt` | 🟡 |
| Photo Location | `PhotoLocationScreen.kt` | 🟡 |
| Social Proof | `SocialProofScreen.kt` | 🔴 |
| Template Marketplace | `TemplateMarketplaceScreen.kt` | 🔴 |
| Widget Customization | `WidgetCustomizationScreen.kt` | 🔴 |
| Member Management | `MemberManagementScreen.kt` | ✅ |
| Profile | `ProfileScreen.kt` | ✅ |
| Settings | `SettingsScreen.kt` | ✅ |

---

## 🏷️ Pro vs Free Features

| Feature | Free | Pro |
|---------|------|-----|
| Create Trips | 3 max | Unlimited |

| Offline Mode | ❌ | ✅ |
| Flight Alerts | ❌ | ✅ |
| AI Planning | ❌ | ✅ |
| Document Vault | 5 docs | Unlimited |

---

## 🚀 Recommended Next Steps

### Phase 1: Polish MVP (1-2 weeks)
1. [ ] Deploy `generate-plan` Edge Function with OpenAI
2. [ ] Integrate IP geolocation for home country/currency
3. [ ] Implement booking reminders (WorkManager)
4. [ ] Test real-time collaboration end-to-end
5. [ ] Add PDF export for sharing

### Phase 2: Enhance Experience (2-3 weeks)
1. [ ] Calendar sync (Android Calendar Provider)
2. [ ] Push notifications (FCM)
3. [ ] Expense tracking UI
4. [ ] Improve offline sync with conflict handling
5. [ ] Map snapshot caching

### Phase 3: Premium Features (3-4 weeks)
1. [ ] Flight status API integration
2. [ ] Price tracking with alerts
3. [ ] Social proof (contact integration)
4. [ ] Home screen widgets
5. [ ] Stripe subscription billing

---

## 📝 Schema Tables Reference

| Table | Model | Repository | Notes |
|-------|-------|------------|-------|
| `profiles` | `Profile` | `ProfileRepository` | ✅ |
| `trips` | `SupabaseTrip` | `TripRepository` | ✅ |
| `trip_members` | `TripMember` | `TripRepository` | ✅ |
| `itinerary_items` | `ItineraryItem` | `TripRepository` | ✅ |
| `itinerary_votes` | `ItineraryVote` | `TripRepository` | ✅ |
| `itinerary_attachments` | – | – | 🔴 No repo |
| `expenses` | `Expense` | – | 🔴 No repo |
| `expense_splits` | `ExpenseSplit` | – | 🔴 No repo |
| `document_vault` | `DocumentVaultItem` | `DocumentRepository` | ✅ |
| `booking_inbox` | – | – | 🔴 No repo |
| `ai_conversations` | `AIConversation` | `AIRepository` | ✅ |

| `price_alerts` | `SupabasePriceAlert` | `PriceAlertRepository` | ✅ |
| `place_knowledge` | – | – | 🔴 No repo |
| `user_relationships` | – | – | 🔴 No repo |

---

## 🔧 Dependencies

```kotlin
// Core
androidx.compose.material3
androidx.navigation.compose

// Backend
io.github.jan-tennert.supabase (Auth, PostgREST, Storage, Realtime, Functions)
io.ktor.client

// Local Storage
androidx.room

// Images
io.coil-kt.coil.compose

// ML
com.google.mlkit:text-recognition

// UI
sh.calvin.reorderable (drag-drop)
```

---

*This document should be updated as features are completed or requirements change.*
