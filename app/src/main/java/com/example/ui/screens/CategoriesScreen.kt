package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.UiStrings
import com.example.model.Book
import com.example.model.CategoryItem
import com.example.ui.theme.BodoniModaFamily
import com.example.ui.theme.ManropeFamily

@Composable
fun CategoriesScreen(
  categories: List<CategoryItem>,
  books: List<Book>,
  strings: UiStrings,
  onSelectCategory: (String) -> Unit,
  onCreateCategory: (String, String) -> Unit,
  onRenameCategory: (String, String) -> Unit,
  onDeleteCategory: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showCreateDialog by remember { mutableStateOf(false) }
  var renamingCategory by remember { mutableStateOf<CategoryItem?>(null) }
  var deletingCategory by remember { mutableStateOf<CategoryItem?>(null) }

  Column(modifier = modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text(
          text = strings.navCategories,
          fontFamily = BodoniModaFamily,
          fontSize = 28.sp,
          lineHeight = 34.sp,
          fontWeight = FontWeight.Normal,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = "${categories.size} curated shelves",
          fontFamily = ManropeFamily,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      OutlinedButton(
        onClick = { showCreateDialog = true },
        shape = RoundedCornerShape(50),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.height(44.dp).testTag("create_category_button"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "New category", modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("New Category", fontSize = 12.sp)
      }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)

    LazyColumn(
      contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
    ) {
      items(categories, key = { it.id }) { category ->
        val matchingBooks = books.filter { it.category.equals(category.name, ignoreCase = true) }
        var showMenu by remember { mutableStateOf(false) }

        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clickable { onSelectCategory(category.name) }
              .padding(vertical = 18.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = category.name,
              fontFamily = BodoniModaFamily,
              fontSize = 20.sp,
              fontWeight = FontWeight.Normal,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(3.dp))
            val subText =
              if (category.subcategories.isNotEmpty()) {
                "${matchingBooks.size} title(s) · ${category.subcategories.joinToString(", ")}"
              } else {
                "${matchingBooks.size} title(s)"
              }
            Text(
              text = subText,
              fontFamily = ManropeFamily,
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Box {
            IconButton(
              onClick = { showMenu = true },
              modifier = Modifier.size(44.dp),
            ) {
              Icon(
                Icons.Filled.MoreVert,
                contentDescription = "Category options for ${category.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
              DropdownMenuItem(
                text = { Text("Rename") },
                onClick = {
                  showMenu = false
                  renamingCategory = category
                },
              )
              DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = {
                  showMenu = false
                  deletingCategory = category
                },
              )
            }
          }

          Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
          )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)
      }
    }
  }

  if (showCreateDialog) {
    var name by remember { mutableStateOf("") }
    var subcategory by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showCreateDialog = false },
      title = { Text("Create Category") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Category Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("new_category_name_input"),
          )
          OutlinedTextField(
            value = subcategory,
            onValueChange = { subcategory = it },
            label = { Text("Initial Subcategory (Optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (name.isNotBlank()) {
              onCreateCategory(name, subcategory)
              showCreateDialog = false
            }
          }
        ) {
          Text("Create")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
      },
    )
  }

  renamingCategory?.let { cat ->
    var newName by remember(cat.id) { mutableStateOf(cat.name) }
    AlertDialog(
      onDismissRequest = { renamingCategory = null },
      title = { Text("Rename Category") },
      text = {
        OutlinedTextField(
          value = newName,
          onValueChange = { newName = it },
          label = { Text("Category Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (newName.isNotBlank()) {
              onRenameCategory(cat.id, newName)
              renamingCategory = null
            }
          }
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { renamingCategory = null }) { Text("Cancel") }
      },
    )
  }

  deletingCategory?.let { cat ->
    AlertDialog(
      onDismissRequest = { deletingCategory = null },
      title = { Text("Delete '${cat.name}'?") },
      text = {
        Text("Books currently in this category will be moved to 'Uncategorized'.")
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteCategory(cat.id)
            deletingCategory = null
          }
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingCategory = null }) { Text("Cancel") }
      },
    )
  }
}
