package com.tvmime.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvmime.db.AppDatabase
import com.tvmime.db.entity.CategoryEntity
import com.tvmime.db.entity.ChannelEntity
import com.tvmime.db.entity.EpgProgramEntity
import com.tvmime.theme.DesignSystemTokens
import kotlinx.coroutines.flow.firstOrNull

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getInstance(this)
        setContent {
            TVMimeMobileApp(database)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TVMimeMobileApp(database: AppDatabase) {
    val bgColor = Color(DesignSystemTokens.Colors.Background)
    val crimson = Color(DesignSystemTokens.Colors.Crimson)
    
    var categories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var channels by remember { mutableStateOf<List<ChannelEntity>>(emptyList()) }
    var activePortalId by remember { mutableStateOf("mock_portal_123") }

    LaunchedEffect(Unit) {
        database.portalDao().getActivePortal().collect { portal ->
            if (portal != null) {
                activePortalId = portal.id
                database.categoryDao().getCategories(portal.id, "LIVE").collect { cats ->
                    categories = cats
                    if (selectedCategory == null && cats.isNotEmpty()) {
                        selectedCategory = cats.first().categoryId
                    }
                }
            }
        }
    }

    LaunchedEffect(selectedCategory, activePortalId) {
        if (selectedCategory != null) {
            database.channelDao().getChannelsByCategory(activePortalId, selectedCategory!!).collect { ch ->
                channels = ch
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "TVMIME",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "MOBILE",
                            color = crimson,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Cast, contentDescription = "Cast", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0C0C10))
            )
        },
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Categories Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories, key = { it.categoryId }) { category ->
                    val isSelected = category.categoryId == selectedCategory
                    Surface(
                        color = if (isSelected) crimson else Color(DesignSystemTokens.Colors.Card),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable { selectedCategory = category.categoryId }
                    ) {
                        Text(
                            text = category.categoryName,
                            color = if (isSelected) Color.White else Color.LightGray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Channels List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(channels, key = { it.id }) { channel ->
                    ChannelRow(channel, database, activePortalId)
                }
            }
        }
    }
}

@Composable
fun ChannelRow(channel: ChannelEntity, database: AppDatabase, portalId: String) {
    var nowPlaying by remember { mutableStateOf<EpgProgramEntity?>(null) }
    var upcoming by remember { mutableStateOf<List<EpgProgramEntity>>(emptyList()) }

    LaunchedEffect(channel.epgChannelId, portalId) {
        if (!channel.epgChannelId.isNullOrBlank()) {
            val epoch = System.currentTimeMillis() / 1000
            val programs = database.epgDao().getProgramsForChannel(portalId, channel.epgChannelId, epoch, 5).firstOrNull()
            if (programs != null && programs.isNotEmpty()) {
                nowPlaying = programs.firstOrNull { it.startEpoch <= epoch && it.endEpoch >= epoch } ?: programs.first()
                upcoming = programs.filter { it.id != nowPlaying?.id }
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(DesignSystemTokens.Colors.Card)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { /* Play video */ }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel Number & Name
            Column(modifier = Modifier.weight(0.35f)) {
                Text(
                    text = "${channel.num}",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = channel.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))

            // EPG Info
            Column(modifier = Modifier.weight(0.65f)) {
                if (nowPlaying != null) {
                    Text(
                        text = "NOW PLAYING",
                        color = Color(DesignSystemTokens.Colors.Crimson),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = nowPlaying!!.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val progress = remember(nowPlaying) {
                        val current = System.currentTimeMillis() / 1000
                        val total = nowPlaying!!.endEpoch - nowPlaying!!.startEpoch
                        if (total <= 0) 0f else ((current - nowPlaying!!.startEpoch).toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp).height(2.dp),
                        color = Color(DesignSystemTokens.Colors.Crimson),
                        trackColor = Color.DarkGray
                    )
                    if (upcoming.isNotEmpty()) {
                        Text(
                            text = "NEXT: ${upcoming.first().title}",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = "No EPG Data",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}
