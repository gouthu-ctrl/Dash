package com.dash.travel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dash.travel.ui.components.DashCard
import com.dash.travel.ui.theme.*

data class SocialReview(
    val id: String,
    val authorName: String,
    val authorAvatar: String?,
    val rating: Float,
    val text: String,
    val location: String,
    val timeAgo: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialProofScreen(
    destinationName: String,
    onNavigateBack: () -> Unit
) {
    // Mock Data
    val reviews = listOf(
        SocialReview("1", "Sarah Jenkins", "https://i.pravatar.cc/150?u=sarah", 5f, "Absolutely loved the hidden cafe near the old bridge! Must visit.", "Le Petit Café", "2 days ago"),
        SocialReview("2", "Mike Ross", "https://i.pravatar.cc/150?u=mike", 4f, "Great views but skip the main line, buy tickets online.", "City Tower", "1 week ago"),
        SocialReview("3", "Jessica Pearson", "https://i.pravatar.cc/150?u=jessica", 5f, "The best sushi I've ever had. Ask for the chef's special.", "Sushi Zen", "2 weeks ago")
    )

    val friendsVisited = listOf(
        "https://i.pravatar.cc/150?u=alex",
        "https://i.pravatar.cc/150?u=sam",
        "https://i.pravatar.cc/150?u=jordan"
    )

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Friends in $destinationName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = OnBackground,
                    navigationIconContentColor = OnBackground
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Friends Hero
            item {
                DashCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${friendsVisited.size} friends have visited $destinationName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy((-12).dp)
                        ) {
                            friendsVisited.forEach { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, Surface, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { /* Ask for tips */ },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary.copy(alpha=0.1f), contentColor = Primary)
                        ) {
                            Text("Ask them for tips")
                        }
                    }
                }
            }
            
            item {
                 Text(
                    "Recent Recommendations",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            items(reviews) { review ->
                ReviewCard(review)
            }
        }
    }
}

@Composable
fun ReviewCard(review: SocialReview) {
    DashCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = review.authorAvatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(review.authorName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("visited ${review.location}", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(review.timeAgo, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(review.text, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                repeat(5) { index ->
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (index < review.rating) Color(0xFFFFD700) else Color.Gray.copy(alpha=0.3f)
                    )
                }
            }
        }
    }
}
