package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.example.localization.UiStrings
import com.example.model.Book
import com.example.model.CategoryItem
import com.example.ui.theme.PlayfairDisplayFamily

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
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = strings.navCategories,
        fontFamily = PlayfairDisplayFamily,
        style = MaterialTheme.typography.headlineMedium,
      )
      OutlinedButton(
        onClick = { showCreateDialog = true },
        shape = RoundedCornerShape(50),
        modifier = Modifier.height(48.dp).testTag("create_category_button"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "New category", modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("New")
      }
    }

    LazyColumn(
      contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      items(categories, key = { it.id }) { category ->
        val matchingBooks = books.filter { it.category.equals(category.name, ignoreCase = true) }
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth().clickable { onSelectCategory(category.name) },
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.Folder,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
              )
              val subText =
                if (category.subcategories.isNotEmpty()) {
                  "${matchingBooks.size} book(s) • ${category.subcategories.joinToString(", ")}"
                } else {
                  "${matchingBooks.size} book(s)"
                }
              Text(
                text = subText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            IconButton(
              onClick = { renamingCategory = category },
              modifier = Modifier.size(48.dp),
            ) {
              Icon(Icons.Filled.Edit, contentDescription = "Rename ${category.name}")
            }
            IconButton(
              onClick = { deletingCategory = category },
              modifier = Modifier.size(48.dp),
            ) {
              Icon(
                Icons.Filled.DeleteOutline,
                contentDescription = "Delete ${category.name}",
                tint = MaterialTheme.colorScheme.error,
              )
            }
          }
        }
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
