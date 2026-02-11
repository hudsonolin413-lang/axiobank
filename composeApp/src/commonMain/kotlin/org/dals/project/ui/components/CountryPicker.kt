package org.dals.project.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

data class Country(
    val name: String,
    val code: String,
    val flag: String,
    val dialCode: String
)

val countries = listOf(
    Country("United States", "US", "🇺🇸", "+1"),
    Country("United Kingdom", "GB", "🇬🇧", "+44"),
    Country("Canada", "CA", "🇨🇦", "+1"),
    Country("Australia", "AU", "🇦🇺", "+61"),
    Country("Germany", "DE", "🇩🇪", "+49"),
    Country("France", "FR", "🇫🇷", "+33"),
    Country("Italy", "IT", "🇮🇹", "+39"),
    Country("Spain", "ES", "🇪🇸", "+34"),
    Country("Netherlands", "NL", "🇳🇱", "+31"),
    Country("Belgium", "BE", "🇧🇪", "+32"),
    Country("Switzerland", "CH", "🇨🇭", "+41"),
    Country("Austria", "AT", "🇦🇹", "+43"),
    Country("Sweden", "SE", "🇸🇪", "+46"),
    Country("Norway", "NO", "🇳🇴", "+47"),
    Country("Denmark", "DK", "🇩🇰", "+45"),
    Country("Finland", "FI", "🇫🇮", "+358"),
    Country("Poland", "PL", "🇵🇱", "+48"),
    Country("Czech Republic", "CZ", "🇨🇿", "+420"),
    Country("Hungary", "HU", "🇭🇺", "+36"),
    Country("Portugal", "PT", "🇵🇹", "+351"),
    Country("Greece", "GR", "🇬🇷", "+30"),
    Country("Turkey", "TR", "🇹🇷", "+90"),
    Country("Russia", "RU", "🇷🇺", "+7"),
    Country("China", "CN", "🇨🇳", "+86"),
    Country("Japan", "JP", "🇯🇵", "+81"),
    Country("South Korea", "KR", "🇰🇷", "+82"),
    Country("India", "IN", "🇮🇳", "+91"),
    Country("Singapore", "SG", "🇸🇬", "+65"),
    Country("Malaysia", "MY", "🇲🇾", "+60"),
    Country("Thailand", "TH", "🇹🇭", "+66"),
    Country("Philippines", "PH", "🇵🇭", "+63"),
    Country("Indonesia", "ID", "🇮🇩", "+62"),
    Country("Vietnam", "VN", "🇻🇳", "+84"),
    Country("Brazil", "BR", "🇧🇷", "+55"),
    Country("Argentina", "AR", "🇦🇷", "+54"),
    Country("Mexico", "MX", "🇲🇽", "+52"),
    Country("Chile", "CL", "🇨🇱", "+56"),
    Country("Colombia", "CO", "🇨🇴", "+57"),
    Country("Peru", "PE", "🇵🇪", "+51"),
    Country("South Africa", "ZA", "🇿🇦", "+27"),
    Country("Nigeria", "NG", "🇳🇬", "+234"),
    Country("Kenya", "KE", "🇰🇪", "+254"),
    Country("Egypt", "EG", "🇪🇬", "+20"),
    Country("Morocco", "MA", "🇲🇦", "+212"),
    Country("Israel", "IL", "🇮🇱", "+972"),
    Country("United Arab Emirates", "AE", "🇦🇪", "+971"),
    Country("Saudi Arabia", "SA", "🇸🇦", "+966"),
    Country("Qatar", "QA", "🇶🇦", "+974"),
    Country("Kuwait", "KW", "🇰🇼", "+965"),
    Country("Bahrain", "BH", "🇧🇭", "+973"),
    Country("Oman", "OM", "🇴🇲", "+968"),
    Country("Jordan", "JO", "🇯🇴", "+962"),
    Country("Lebanon", "LB", "🇱🇧", "+961"),
    Country("New Zealand", "NZ", "🇳🇿", "+64"),
    Country("Ireland", "IE", "🇮🇪", "+353"),
    Country("Luxembourg", "LU", "🇱🇺", "+352"),
    Country("Iceland", "IS", "🇮🇸", "+354"),
    Country("Malta", "MT", "🇲🇹", "+356"),
    Country("Cyprus", "CY", "🇨🇾", "+357"),
    Country("Estonia", "EE", "🇪🇪", "+372"),
    Country("Latvia", "LV", "🇱🇻", "+371"),
    Country("Lithuania", "LT", "🇱🇹", "+370"),
    Country("Slovenia", "SI", "🇸🇮", "+386"),
    Country("Slovakia", "SK", "🇸🇰", "+421"),
    Country("Croatia", "HR", "🇭🇷", "+385"),
    Country("Serbia", "RS", "🇷🇸", "+381"),
    Country("Montenegro", "ME", "🇲🇪", "+382"),
    Country("Bosnia and Herzegovina", "BA", "🇧🇦", "+387"),
    Country("North Macedonia", "MK", "🇲🇰", "+389"),
    Country("Albania", "AL", "🇦🇱", "+355"),
    Country("Bulgaria", "BG", "🇧🇬", "+359"),
    Country("Romania", "RO", "🇷🇴", "+40"),
    Country("Moldova", "MD", "🇲🇩", "+373"),
    Country("Ukraine", "UA", "🇺🇦", "+380"),
    Country("Belarus", "BY", "🇧🇾", "+375")
).sortedBy { it.name }

@Composable
fun CountryPicker(
    selectedCountry: Country?,
    onCountrySelected: (Country) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = if (selectedCountry != null) {
            "${selectedCountry.flag} ${selectedCountry.name} (${selectedCountry.dialCode})"
        } else {
            ""
        },
        onValueChange = { },
        label = { Text("Country") },
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                if (enabled) showDialog = true
            },
        enabled = false, // Always disabled for user input, only clickable
        readOnly = true,
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select Country",
                modifier = Modifier.clickable(enabled = enabled) {
                    if (enabled) showDialog = true
                }
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.38f
            ),
            disabledLabelColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.38f
            ),
            disabledBorderColor = if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.12f
            ),
            disabledTrailingIconColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.38f
            )
        )
    )

    if (showDialog) {
        CountryPickerDialog(
            onCountrySelected = { country ->
                onCountrySelected(country)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun CountryPickerDialog(
    onCountrySelected: (Country) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isEmpty()) {
            countries
        } else {
            countries.filter { country ->
                country.name.contains(searchQuery, ignoreCase = true) ||
                        country.code.contains(searchQuery, ignoreCase = true) ||
                        country.dialCode.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Text(
                    text = "Select Country",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search countries") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true
                )

                // Countries List
                LazyColumn {
                    items(filteredCountries) { country ->
                        CountryItem(
                            country = country,
                            onClick = { onCountrySelected(country) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CountryItem(
    country: Country,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = country.flag,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(end = 12.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = country.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = country.dialCode,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PhoneNumberInput(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    selectedCountry: Country?,
    onCountrySelected: (Country) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null
) {
    // Set default country to United States if none selected
    LaunchedEffect(Unit) {
        if (selectedCountry == null) {
            val defaultCountry = countries.find { it.code == "US" }
            if (defaultCountry != null) {
                onCountrySelected(defaultCountry)
            }
        }
    }

    Column(modifier = modifier) {
        // Country Picker
        CountryPicker(
            selectedCountry = selectedCountry,
            onCountrySelected = onCountrySelected,
            enabled = enabled
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Phone Number Input
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneNumberChange,
            label = { Text("Phone Number") },
            leadingIcon = {
                if (selectedCountry != null) {
                    Text(
                        text = "${selectedCountry.flag} ${selectedCountry.dialCode}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            isError = isError,
            supportingText = supportingText,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone
            ),
            singleLine = true,
            placeholder = {
                Text("Enter phone number")
            }
        )
    }
}