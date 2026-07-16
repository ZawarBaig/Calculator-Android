package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.KeyBlue
import com.example.ui.theme.KeyControlDark
import com.example.ui.theme.KeyControlLight
import com.example.ui.theme.KeyOrange
import com.example.ui.theme.KeySlateDark
import com.example.ui.theme.KeySlateLight
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      // Manage theme override state locally (null = system auto, true = dark, false = light)
      var forceDarkMode by remember { mutableStateOf<Boolean?>(null) }
      val isDark = forceDarkMode ?: androidx.compose.foundation.isSystemInDarkTheme()

      MyApplicationTheme(darkTheme = isDark) {
        Scaffold(
          modifier = Modifier.fillMaxSize(),
          contentWindowInsets = androidx.compose.material3.ScaffoldDefaults.contentWindowInsets
        ) { innerPadding ->
          CalculatorScreen(
            forceDarkMode = forceDarkMode,
            onThemeChange = { forceDarkMode = it },
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }
}

@Composable
fun CalculatorScreen(
  forceDarkMode: Boolean?,
  onThemeChange: (Boolean?) -> Unit,
  modifier: Modifier = Modifier,
  calcViewModel: CalculatorViewModel = viewModel()
) {
  val haptic = LocalHapticFeedback.current
  var dragSum by remember { mutableStateOf(0f) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .safeDrawingPadding()
      .padding(bottom = 16.dp),
    verticalArrangement = Arrangement.SpaceBetween,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Top Bar - Custom Sliding Theme Switcher
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 8.dp),
      contentAlignment = Alignment.Center
    ) {
      ThemeSelector(
        currentOverride = forceDarkMode,
        onSelect = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onThemeChange(it)
        }
      )
    }

    // Calculator Display Section (Flexible Height)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .padding(horizontal = 24.dp, vertical = 16.dp)
        .pointerInput(Unit) {
          detectDragGestures(
            onDragStart = { dragSum = 0f },
            onDragEnd = { dragSum = 0f },
            onDragCancel = { dragSum = 0f }
          ) { change, dragAmount ->
            change.consume()
            dragSum += dragAmount.x
            // 80px horizontal swipe triggers backspace with haptic
            if (kotlin.math.abs(dragSum) > 80f) {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              calcViewModel.onBackspace()
              dragSum = 0f
            }
          }
        },
      verticalArrangement = Arrangement.Bottom,
      horizontalAlignment = Alignment.End
    ) {
      // Formula Text (Previous operations)
      Text(
        text = calcViewModel.formula,
        fontSize = 20.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        textAlign = TextAlign.End,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        fontFamily = FontFamily.SansSerif,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("display_formula")
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Primary Input/Output Display with auto-scaling font size
      val displayLength = calcViewModel.display.length
      val displayFontSize = when {
        displayLength <= 7 -> 72.sp
        displayLength <= 10 -> 52.sp
        displayLength <= 13 -> 42.sp
        else -> 32.sp
      }

      Text(
        text = calcViewModel.display,
        fontSize = displayFontSize,
        fontWeight = FontWeight.Light,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.End,
        maxLines = 1,
        fontFamily = FontFamily.SansSerif,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("display_result")
      )
    }

    // Interactive Button Grid
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 480.dp) // Optimized for tablets
        .padding(horizontal = 20.dp)
    ) {
      val keySpacing = 12.dp
      val totalSpacingWidth = keySpacing * 3
      val keyWidth = (maxWidth - totalSpacingWidth) / 4

      Column(
        verticalArrangement = Arrangement.spacedBy(keySpacing),
        modifier = Modifier.fillMaxWidth()
      ) {
        // Row 1: AC, Backspace, %, Division
        Row(
          horizontalArrangement = Arrangement.spacedBy(keySpacing),
          modifier = Modifier.fillMaxWidth()
        ) {
          KeyButton(
            text = "AC",
            onClick = { calcViewModel.onClear() },
            keyWidth = keyWidth,
            category = KeyCategory.Control,
            tag = "btn_clear"
          )
          KeyButton(
            text = "",
            icon = {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                tint = if (androidx.compose.foundation.isSystemInDarkTheme() || forceDarkMode == true) Color.White else Color(0xFF2D2F3C),
                modifier = Modifier.size(24.dp)
              )
            },
            onClick = { calcViewModel.onBackspace() },
            keyWidth = keyWidth,
            category = KeyCategory.Control,
            tag = "btn_backspace"
          )
          KeyButton(
            text = "%",
            onClick = { calcViewModel.onPercentage() },
            keyWidth = keyWidth,
            category = KeyCategory.Control,
            tag = "btn_percentage"
          )
          KeyButton(
            text = "÷",
            onClick = { calcViewModel.onOperator("÷") },
            keyWidth = keyWidth,
            category = KeyCategory.Operator,
            tag = "btn_divide"
          )
        }

        // Row 2: 7, 8, 9, Multiplication
        Row(
          horizontalArrangement = Arrangement.spacedBy(keySpacing),
          modifier = Modifier.fillMaxWidth()
        ) {
          KeyButton(text = "7", onClick = { calcViewModel.onDigit("7") }, keyWidth = keyWidth, tag = "btn_7")
          KeyButton(text = "8", onClick = { calcViewModel.onDigit("8") }, keyWidth = keyWidth, tag = "btn_8")
          KeyButton(text = "9", onClick = { calcViewModel.onDigit("9") }, keyWidth = keyWidth, tag = "btn_9")
          KeyButton(
            text = "×",
            onClick = { calcViewModel.onOperator("×") },
            keyWidth = keyWidth,
            category = KeyCategory.Operator,
            tag = "btn_multiply"
          )
        }

        // Row 3: 4, 5, 6, Subtraction
        Row(
          horizontalArrangement = Arrangement.spacedBy(keySpacing),
          modifier = Modifier.fillMaxWidth()
        ) {
          KeyButton(text = "4", onClick = { calcViewModel.onDigit("4") }, keyWidth = keyWidth, tag = "btn_4")
          KeyButton(text = "5", onClick = { calcViewModel.onDigit("5") }, keyWidth = keyWidth, tag = "btn_5")
          KeyButton(text = "6", onClick = { calcViewModel.onDigit("6") }, keyWidth = keyWidth, tag = "btn_6")
          KeyButton(
            text = "−",
            onClick = { calcViewModel.onOperator("−") },
            keyWidth = keyWidth,
            category = KeyCategory.Operator,
            tag = "btn_subtract"
          )
        }

        // Row 4: 1, 2, 3, Addition
        Row(
          horizontalArrangement = Arrangement.spacedBy(keySpacing),
          modifier = Modifier.fillMaxWidth()
        ) {
          KeyButton(text = "1", onClick = { calcViewModel.onDigit("1") }, keyWidth = keyWidth, tag = "btn_1")
          KeyButton(text = "2", onClick = { calcViewModel.onDigit("2") }, keyWidth = keyWidth, tag = "btn_2")
          KeyButton(text = "3", onClick = { calcViewModel.onDigit("3") }, keyWidth = keyWidth, tag = "btn_3")
          KeyButton(
            text = "+",
            onClick = { calcViewModel.onOperator("+") },
            keyWidth = keyWidth,
            category = KeyCategory.Operator,
            tag = "btn_add"
          )
        }

        // Row 5: +/-, 0, ., Equals
        Row(
          horizontalArrangement = Arrangement.spacedBy(keySpacing),
          modifier = Modifier.fillMaxWidth()
        ) {
          KeyButton(
            text = "±",
            onClick = { calcViewModel.onToggleSign() },
            keyWidth = keyWidth,
            category = KeyCategory.Control,
            tag = "btn_toggle_sign"
          )
          KeyButton(text = "0", onClick = { calcViewModel.onDigit("0") }, keyWidth = keyWidth, tag = "btn_0")
          KeyButton(text = ".", onClick = { calcViewModel.onDecimal() }, keyWidth = keyWidth, tag = "btn_decimal")
          KeyButton(
            text = "=",
            onClick = { calcViewModel.onEquals() },
            keyWidth = keyWidth,
            category = KeyCategory.Equals,
            tag = "btn_equals"
          )
        }
      }
    }
  }
}

enum class KeyCategory {
  Number, Control, Operator, Equals
}

@Composable
fun KeyButton(
  text: String,
  onClick: () -> Unit,
  keyWidth: Dp,
  modifier: Modifier = Modifier,
  category: KeyCategory = KeyCategory.Number,
  icon: (@Composable () -> Unit)? = null,
  tag: String = ""
) {
  val haptic = LocalHapticFeedback.current
  var isPressed by remember { mutableStateOf(false) }

  // Tactile animation scale down on press, bounce back on release
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.90f else 1.0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
    ),
    label = "keyScale"
  )

  // Determine button color based on Light/Dark themes and categories
  val isDark = MaterialTheme.colorScheme.background == com.example.ui.theme.DarkBackground
  val containerColor = when (category) {
    KeyCategory.Number -> if (isDark) KeySlateDark else KeySlateLight
    KeyCategory.Control -> if (isDark) KeyControlDark else KeyControlLight
    KeyCategory.Operator -> if (isDark) KeyControlDark else KeyControlLight
    KeyCategory.Equals -> KeyOrange
  }

  val contentColor = when (category) {
    KeyCategory.Number -> MaterialTheme.colorScheme.onBackground
    KeyCategory.Control -> MaterialTheme.colorScheme.onBackground
    KeyCategory.Operator -> KeyOrange
    KeyCategory.Equals -> Color.White
  }

  val shape = RoundedCornerShape(26.dp)

  Box(
    modifier = modifier
      .size(keyWidth)
      .scale(scale)
      .shadow(
        elevation = if (isPressed) 1.dp else 3.dp,
        shape = shape,
        clip = true
      )
      .background(containerColor)
      .pointerInput(Unit) {
        detectTapGestures(
          onPress = {
            isPressed = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            tryAwaitRelease()
            isPressed = false
          },
          onTap = {
            onClick()
          }
        )
      }
      .testTag(tag),
    contentAlignment = Alignment.Center
  ) {
    if (icon != null) {
      icon()
    } else {
      Text(
        text = text,
        fontSize = 25.sp,
        fontWeight = FontWeight.Medium,
        color = contentColor,
        fontFamily = FontFamily.SansSerif
      )
    }
  }
}

@Composable
fun ThemeSelector(
  currentOverride: Boolean?,
  onSelect: (Boolean?) -> Unit
) {
  val isDark = MaterialTheme.colorScheme.background == com.example.ui.theme.DarkBackground
  val containerBg = if (isDark) Color(0xFF1E212B) else Color(0xFFECEEF4)

  // Custom sliding animator for the theme switch pill
  // Positions: 0 = Light, 1 = Auto, 2 = Dark
  val selectedIndex = when (currentOverride) {
    false -> 0
    null -> 1
    true -> 2
  }

  val sliderOffset by animateDpAsState(
    targetValue = (selectedIndex * 54).dp,
    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow),
    label = "sliderPosition"
  )

  Box(
    modifier = Modifier
      .size(width = 176.dp, height = 44.dp)
      .clip(CircleShape)
      .background(containerBg)
      .padding(4.dp)
      .testTag("theme_selector_container")
  ) {
    // Sliding Indicator Highlight
    Box(
      modifier = Modifier
        .offset(x = sliderOffset)
        .size(width = 54.dp, height = 36.dp)
        .clip(CircleShape)
        .background(if (isDark) Color(0xFF34384B) else Color.White)
        .shadow(1.dp, CircleShape)
    )

    // Icons Row
    Row(
      modifier = Modifier.fillMaxSize(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Light Mode Button
      Box(
        modifier = Modifier
          .size(width = 54.dp, height = 36.dp)
          .clip(CircleShape)
          .pointerInput(Unit) {
            detectTapGestures { onSelect(false) }
          }
          .testTag("theme_btn_light"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Outlined.LightMode,
          contentDescription = "Light Mode",
          tint = if (selectedIndex == 0) KeyOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
          modifier = Modifier.size(19.dp)
        )
      }

      // Auto (System) Button
      Box(
        modifier = Modifier
          .size(width = 54.dp, height = 36.dp)
          .clip(CircleShape)
          .pointerInput(Unit) {
            detectTapGestures { onSelect(null) }
          }
          .testTag("theme_btn_auto"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "AUTO",
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = if (selectedIndex == 1) {
            if (isDark) Color.White else Color.Black
          } else {
            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
          }
        )
      }

      // Dark Mode Button
      Box(
        modifier = Modifier
          .size(width = 54.dp, height = 36.dp)
          .clip(CircleShape)
          .pointerInput(Unit) {
            detectTapGestures { onSelect(true) }
          }
          .testTag("theme_btn_dark"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Outlined.DarkMode,
          contentDescription = "Dark Mode",
          tint = if (selectedIndex == 2) KeyBlue else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
          modifier = Modifier.size(19.dp)
        )
      }
    }
  }
}

class CalculatorViewModel : ViewModel() {
  var display by mutableStateOf("0")
    private set
  var formula by mutableStateOf("")
    private set

  private var operand1: Double? = null
  private var activeOperator: String? = null
  private var resetOnNextInput = false

  fun onDigit(digit: String) {
    if (resetOnNextInput) {
      display = digit
      resetOnNextInput = false
    } else {
      if (display == "0") {
        display = digit
      } else {
        if (display.length < 15) { // Safe max length to fit comfortably on most mobile displays
          display += digit
        }
      }
    }
  }

  fun onDecimal() {
    if (resetOnNextInput) {
      display = "0."
      resetOnNextInput = false
    } else {
      if (!display.contains(".")) {
        display += "."
      }
    }
  }

  fun onOperator(op: String) {
    val currentValue = display.toDoubleOrNull() ?: 0.0

    if (operand1 != null && activeOperator != null && !resetOnNextInput) {
      val result = calculate(operand1!!, currentValue, activeOperator!!)
      display = formatResult(result)
      operand1 = result
    } else {
      operand1 = currentValue
    }

    activeOperator = op
    formula = "${formatResult(operand1!!)} $op"
    resetOnNextInput = true
  }

  fun onEquals() {
    val currentValue = display.toDoubleOrNull() ?: 0.0
    if (operand1 != null && activeOperator != null) {
      val result = calculate(operand1!!, currentValue, activeOperator!!)
      formula = "${formatResult(operand1!!)} $activeOperator ${formatResult(currentValue)} ="
      display = formatResult(result)
      operand1 = null
      activeOperator = null
      resetOnNextInput = true
    }
  }

  fun onClear() {
    display = "0"
    formula = ""
    operand1 = null
    activeOperator = null
    resetOnNextInput = false
  }

  fun onBackspace() {
    if (display != "0" && !resetOnNextInput) {
      if (display.length > 1) {
        display = display.substring(0, display.length - 1)
        if (display == "-") display = "0"
      } else {
        display = "0"
      }
    }
  }

  fun onToggleSign() {
    val currentValue = display.toDoubleOrNull() ?: 0.0
    if (currentValue != 0.0) {
      if (display.startsWith("-")) {
        display = display.substring(1)
      } else {
        display = "-$display"
      }
    }
  }

  fun onPercentage() {
    val currentValue = display.toDoubleOrNull() ?: 0.0
    val result = currentValue / 100.0
    display = formatResult(result)
    resetOnNextInput = true
  }

  private fun calculate(op1: Double, op2: Double, operator: String): Double {
    return when (operator) {
      "+" -> op1 + op2
      "−" -> op1 - op2
      "×" -> op1 * op2
      "÷" -> {
        if (op2 == 0.0) Double.NaN else op1 / op2
      }
      else -> op2
    }
  }

  private fun formatResult(value: Double): String {
    if (value.isNaN()) return "Error"
    if (value.isInfinite()) return "Overflow"

    // Print integer representation if it's whole
    if (value % 1.0 == 0.0 && value <= Long.MAX_VALUE && value >= Long.MIN_VALUE) {
      return value.toLong().toString()
    }

    // Dynamic formatting with clean decimal separators, avoids trailing scientific zeroes
    val df = java.text.DecimalFormat("#.########", java.text.DecimalFormatSymbols(java.util.Locale.US))
    return df.format(value)
  }
}
