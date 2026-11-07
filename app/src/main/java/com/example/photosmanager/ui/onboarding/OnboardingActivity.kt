package com.example.photosmanager.ui.onboarding

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photosmanager.MainActivity
import com.example.photosmanager.R
import com.example.photosmanager.data.repository.PreferencesRepository
import com.example.photosmanager.ui.theme.PHOTOSMANAGERTheme
import kotlinx.coroutines.launch

/**
 * 首次引导。版式照 ScreenshotBye 的 activity_onboarding.xml：
 * 右上角「跳过」，下面居中一行页码，标题 22sp 加粗居中，正文 14sp 靠左带行距，
 * 中间撑开把页面指示点压到底部，最后一行是「上一步 / 下一步」。
 */
class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PHOTOSMANAGERTheme {
                OnboardingScreen {
                    finish()
                    startActivity(
                        android.content.Intent(this@OnboardingActivity, MainActivity::class.java)
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val preferencesRepository = remember { PreferencesRepository(context) }
    val scope = rememberCoroutineScope()

    val titles = stringArrayResource(R.array.onb_titles)
    val bodies = stringArrayResource(R.array.onb_bodies)
    val pageCount = titles.size

    var currentPage by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // 同首页：targetSdk 36 起内容会顶到状态栏底下，本页没有 Scaffold，内边距自己补
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
    ) {
        TextButton(
            onClick = onComplete,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(stringResource(R.string.onb_skip))
        }

        Text(
            text = stringResource(R.string.onb_page_label, currentPage + 1, pageCount),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 正文长短不一，小屏上留出可滚的余地
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 20.dp)
        ) {
            Text(
                text = titles[currentPage],
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = bodies[currentPage],
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            titles.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (index == currentPage) 12.dp else 8.dp)
                        .background(
                            color = if (index == currentPage)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        ) {
            if (currentPage > 0) {
                TextButton(onClick = { currentPage-- }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.onb_prev))
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (currentPage < pageCount - 1) {
                Button(
                    onClick = { currentPage++ },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(stringResource(R.string.onb_next))
                }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            preferencesRepository.setFirstLaunch(false)
                            onComplete()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(stringResource(R.string.onb_start))
                }
            }
        }
    }
}
