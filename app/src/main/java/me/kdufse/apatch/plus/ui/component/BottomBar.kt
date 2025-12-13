// BottomBar.kt
package me.kdufse.apatch.plus.ui.component

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import com.ramcosta.composedestinations.utils.isRouteOnBackStackAsState
import me.kdufse.apatch.plus.ui.screen.BottomBarDestination

@Composable
fun BottomBar(navController: NavController) {
    NavigationBar {
        BottomBarDestination.entries.forEach { destination ->
            val isCurrentDestOnBackStack by navController.isRouteOnBackStackAsState(destination.direction)
            
            NavigationBarItem(
                selected = isCurrentDestOnBackStack,
                onClick = {
                    if (!isCurrentDestOnBackStack) {
                        navController.navigate(destination.direction.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.label)
                    )
                },
                label = {
                    Text(
                        text = stringResource(destination.label),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}