import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.group1.dartbud.R

// Definer Permanent Marker font family
private val PermanentMarkerFontFamily = FontFamily(
    Font(R.font.permanentmarker_regular, FontWeight.Normal)
)

// Egendefinert typografi for appen, satt opp med håndskrift-fonten
// Permanent Marker på bodyLarge.
val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = PermanentMarkerFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    )
    // Add other text styles as needed
)
