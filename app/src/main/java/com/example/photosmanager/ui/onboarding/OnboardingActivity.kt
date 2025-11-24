package com.example.photosmanager.ui.onboarding

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.photosmanager.MainActivity
import com.example.photosmanager.data.repository.PreferencesRepository
import com.example.photosmanager.ui.theme.PHOTOSMANAGERTheme
import kotlinx.coroutines.launch

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PHOTOSMANAGERTheme {
                OnboardingScreen {
                    finish()
                    startActivity(android.content.Intent(this@OnboardingActivity, MainActivity::class.java))
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferencesRepository = remember { PreferencesRepository(context) }
    val scope = rememberCoroutineScope()

    var currentPage by remember { mutableIntStateOf(0) }

    val pages = listOf(
        OnboardingPage(
            title = "欢迎使用 PhotosManager",
            description = "帮助您在碎片时间里轻松管理手机相册中的无用照片"
        ),
        OnboardingPage(
            title = "智能照片管理",
            description = "每次亮屏时随机展示照片，快速决定保留或删除"
        ),
        OnboardingPage(
            title = "批量处理",
            description = "查看附近照片，批量处理相似的无用照片"
        ),
        OnboardingPage(
            title = "开始使用",
            description = "请在设置中选择要管理的相册，然后启用服务"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = pages[currentPage].title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = pages[currentPage].description,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentPage > 0) {
                TextButton(onClick = { currentPage-- }) {
                    Text("上一步")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            if (currentPage < pages.size - 1) {
                Button(onClick = { currentPage++ }) {
                    Text("下一步")
                }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            preferencesRepository.setFirstLaunch(false)
                            onComplete()
                        }
                    }
                ) {
                    Text("开始使用")
                }
            }
        }

        // 页面指示器
        Row(
            modifier = Modifier.padding(top = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            pages.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(if (index == currentPage) 12.dp else 8.dp)
                        .background(
                            color = if (index == currentPage)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
            }
        }
    }
}

data class OnboardingPage(
    val title: String,
    val description: String
)

