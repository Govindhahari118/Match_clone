package com.match.app.ui.testimonials

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.match.app.ui.i18n.t

data class TestimonialItem(
    val name: String,
    val location: String,
    val profession: String,
    val rating: Int,
    val text: String,
    val initial: String = name.take(1).uppercase()
)

private val sampleTestimonials = listOf(
    TestimonialItem("Priya & Rahul", "Bangalore, Karnataka", "Software Engineer", 5,
        "We found each other through this app. The compatibility quiz was incredibly accurate — it matched us on values that truly matter. We got married within 6 months of connecting!"),
    TestimonialItem("Sneha & Vikram", "Chennai, Tamil Nadu", "Doctor", 5,
        "The verification process gave me confidence that profiles were genuine. The Kundli matching feature helped our families feel comfortable with the match. Highly recommended!"),
    TestimonialItem("Anjali & Arjun", "Mumbai, Maharashtra", "Business Analyst", 5,
        "I was skeptical about online matrimony, but the assisted service was a game-changer. My relationship manager understood exactly what I was looking for. Found my life partner in 3 months!"),
    TestimonialItem("Meera & Suresh", "Hyderabad, Telangana", "Teacher", 4,
        "The second marriage section gave me hope. The profiles were respectful and genuine. The virtual meet feature helped me connect safely before sharing personal details."),
    TestimonialItem("Kavitha & Rajan", "Delhi", "CA", 5,
        "What I loved most was the photo privacy feature. I could control who sees my photos, which gave me peace of mind. The biodata maker was also a great touch — made sharing with family easy."),
    TestimonialItem("Pooja & Aditya", "Pune, Maharashtra", "Marketing Manager", 5,
        "The bio generator helped me create a perfect introduction. The compatibility score was spot-on. We connected over shared hobbies and haven't looked back since!"),
    TestimonialItem("Radha & Krishna", "Jaipur, Rajasthan", "Government Officer", 4,
        "Clean interface, genuine profiles, and the community circles feature helped me find matches within my community. The daily recommendations kept me engaged."),
    TestimonialItem("Nisha & Dev", "Kolkata, West Bengal", "Architect", 5,
        "The voice message feature in chat was unique and personal. Being able to hear someone's voice before meeting made the whole process more comfortable and authentic."),
    TestimonialItem("Lakshmi & Ganesh", "Coimbatore, Tamil Nadu", "Lecturer", 5,
        "From the detailed profile sections to the horoscope matching — everything is thoughtfully designed. We matched on 32 out of 36 points in Guna Milan! Our families were thrilled."),
    TestimonialItem("Ananya & Siddharth", "Noida, UP", "Product Manager", 5,
        "The profile boost feature helped me get noticed faster. Within a week of boosting, I received 15 interests — one of which turned into my life partner. Best investment ever!")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestimonialsScreen(
    onBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("user_reviews", "User Reviews")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header stats
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⭐", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(8.dp))
                        Text(t("rating_out_of_5", "4.8 out of 5"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        Text(t("based_on_verified_reviews", "Based on 10,000+ verified reviews"), style = MaterialTheme.typography.bodySmall, color = Color(0xFF4E342E))
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            RatingStat("50K+", "Happy Couples")
                            RatingStat("4.8★", "Avg Rating")
                            RatingStat("97%", "Recommend")
                        }
                    }
                }
            }

            items(sampleTestimonials) { t ->
                TestimonialCard(t)
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun TestimonialCard(item: TestimonialItem) {
    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(item.initial, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("${item.profession} • ${item.location}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Star rating
                Row {
                    repeat(item.rating) {
                        Icon(Icons.Filled.Star, null, Modifier.size(16.dp), tint = Color(0xFFFFA000))
                    }
                    repeat(5 - item.rating) {
                        Icon(Icons.Filled.StarBorder, null, Modifier.size(16.dp), tint = Color(0xFFFFA000))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(item.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun RatingStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF4E342E))
    }
}
