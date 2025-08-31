// File: app/src/main/java/com/example/cloudfavorites/CategoryFragment.kt
package com.xlzhen.ifavorites

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class CategoryFragment : Fragment() {

    companion object {
        private const val ARG_PARENT_ID = "parent_id"

        fun newInstance(parentId: String): CategoryFragment {
            val fragment = CategoryFragment()
            val args = Bundle()
            args.putString(ARG_PARENT_ID, parentId)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var subfoldersRecycler: RecyclerView
    private lateinit var bookmarksRecycler: RecyclerView
    private lateinit var addSubfolderButton: Button
    private lateinit var switchViewButton: Button
    private lateinit var addBookmarkButton: Button

    private var parentId: String = ""
    private var selectedFolderId: String = ""
    private var subfolders: List<Folder> = emptyList()
    private var bookmarks: List<Bookmark> = emptyList()
    private var isGridView = true
    private var userId: String = ""
    private var auth: String = ""

    private val pickCoverLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        handleFilePick(result, true)
    }
    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        handleFilePick(result, false)
    }

    private var selectedCoverUri: Uri? = null
    private var selectedFileUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            parentId = it.getString(ARG_PARENT_ID) ?: ""
            selectedFolderId = parentId  // Initial selected is parent
        }
        userId = requireContext().getSharedPreferences("prefs", Context.MODE_PRIVATE).getString("user_id", "") ?: ""
        auth = "Bearer $userId"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_category, container, false)

        subfoldersRecycler = view.findViewById(R.id.subfolders_recycler)
        bookmarksRecycler = view.findViewById(R.id.bookmarks_recycler)
        addSubfolderButton = view.findViewById(R.id.add_subfolder)
        switchViewButton = view.findViewById(R.id.switch_view)
        addBookmarkButton = view.findViewById(R.id.add_bookmark)

        subfoldersRecycler.layoutManager = LinearLayoutManager(context)
        bookmarksRecycler.layoutManager = GridLayoutManager(context, 2)  // Initial grid

        addSubfolderButton.setOnClickListener { showAddSubfolderDialog() }
        switchViewButton.setOnClickListener { toggleViewMode() }
        addBookmarkButton.setOnClickListener { showAddBookmarkDialog() }

        loadSubfolders()
        loadBookmarks(selectedFolderId)

        return view
    }

    private fun showAddSubfolderDialog() {
        val input = EditText(context)
        AlertDialog.Builder(requireContext())
            .setTitle("新增二级菜单")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val name = input.text.toString()
                if (name.isNotEmpty()) {
                    createSubfolder(name)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun createSubfolder(name: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.createFolder(auth, CreateFolderRequest(name, parentId, userId))
                withContext(Dispatchers.Main) {
                    if (response.success) {
                        loadSubfolders()
                    } else {
                        Toast.makeText(context, response.message ?: "创建失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadSubfolders() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getSubFolders(auth, parentId)
                subfolders = response.folders ?: emptyList()
                withContext(Dispatchers.Main) {
                    subfoldersRecycler.adapter = SubfolderAdapter(subfolders) { folder ->
                        selectedFolderId = folder.id
                        loadBookmarks(selectedFolderId)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadBookmarks(folderId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getBookmarks(auth, folderId)
                bookmarks = response.bookmarks ?: emptyList()
                withContext(Dispatchers.Main) {
                    bookmarksRecycler.adapter = BookmarkAdapter(bookmarks, isGridView, RetrofitClient.BASE_URL) { bookmark ->
                        openBookmark(bookmark)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun toggleViewMode() {
        isGridView = !isGridView
        bookmarksRecycler.layoutManager = if (isGridView) GridLayoutManager(context, 2) else LinearLayoutManager(context)
        bookmarksRecycler.adapter = BookmarkAdapter(bookmarks, isGridView, RetrofitClient.BASE_URL) { bookmark ->
            openBookmark(bookmark)
        }
        switchViewButton.text = if (isGridView) "切换到列表" else "切换到宫格"
    }

    private fun showAddBookmarkDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_bookmark, null)
        val titleEdit: EditText = dialogView.findViewById(R.id.title_edit)
        val descEdit: EditText = dialogView.findViewById(R.id.desc_edit)
        val linkEdit: EditText = dialogView.findViewById(R.id.link_edit)
        val pickCoverButton: Button = dialogView.findViewById(R.id.pick_cover)
        val pickFileButton: Button = dialogView.findViewById(R.id.pick_file)

        pickCoverButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            pickCoverLauncher.launch(intent)
        }
        pickFileButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "*/*" }
            pickFileLauncher.launch(intent)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("添加收藏")
            .setView(dialogView)
            .setPositiveButton("确定") { _, _ ->
                val title = titleEdit.text.toString()
                val desc = descEdit.text.toString()
                val link = linkEdit.text.toString()
                if (title.isNotEmpty() && desc.isNotEmpty() && link.isNotEmpty()) {
                    addBookmark(title, desc, link)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun handleFilePick(result: ActivityResult, isCover: Boolean) {
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                if (isCover) selectedCoverUri = uri else selectedFileUri = uri
            }
        }
    }

    private fun addBookmark(title: String, desc: String, link: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var coverId: String? = null
                if (selectedCoverUri != null) {
                    val part = prepareFilePart("file", selectedCoverUri!!)
                    val response = RetrofitClient.instance.uploadCover(auth, part)
                    if (response.success) coverId = response.file_id
                }

                var fileId: String? = null
                if (selectedFileUri != null) {
                    val part = prepareFilePart("file", selectedFileUri!!)
                    val response = RetrofitClient.instance.uploadFile(auth, part)
                    if (response.success) fileId = response.file_id
                }

                val request = AddBookmarkRequest(title, desc, selectedFolderId, link, coverId, userId, fileId)
                val response = RetrofitClient.instance.addBookmark(auth, request)
                withContext(Dispatchers.Main) {
                    if (response.success) {
                        loadBookmarks(selectedFolderId)
                        selectedCoverUri = null
                        selectedFileUri = null
                    } else {
                        Toast.makeText(context, response.message ?: "添加失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun prepareFilePart(partName: String, uri: Uri): MultipartBody.Part {
        val file = File(requireContext().cacheDir, getFileName(uri))
        val inputStream = requireContext().contentResolver.openInputStream(uri)!!
        FileOutputStream(file).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(partName, file.name, requestFile)
    }

    private fun getFileName(uri: Uri): String {
        var name = ""
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            name = cursor.getString(nameIndex)
        }
        return name
    }

    private fun openBookmark(bookmark: Bookmark) {
        val uri = if (bookmark.filepath != null) {
            Uri.parse(RetrofitClient.BASE_URL + bookmark.filepath.replace("\\","/"))
        } else {
            Uri.parse(bookmark.link)
        }
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }
}