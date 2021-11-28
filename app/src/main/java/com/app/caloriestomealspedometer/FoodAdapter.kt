package com.app.caloriestomealspedometer

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import io.realm.OrderedRealmCollection
import io.realm.RealmRecyclerViewAdapter
import kotlinx.android.synthetic.main.activity_food_edit.*
import java.io.FileDescriptor


class FoodAdapter(data: OrderedRealmCollection<Food>,context:Context) :
    RealmRecyclerViewAdapter<Food, FoodAdapter.ViewHolder>(data, true) {

    //FoodListActivityのcontextからcontentResolverを取得する
    val cr = context.contentResolver

    private val pref = PreferenceManager.getDefaultSharedPreferences(context)
    private val editor = pref.edit()

    private var checkFoodId = pref.getLong("CHECK_FOOD_ID",-1)

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        val parcelFileDescriptor = cr.openFileDescriptor(uri, "r")
        val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
        val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        parcelFileDescriptor.close()
        return image
    }



    private var listener: ((Long?) -> Unit)? = null
    fun setOnItemClickListener(listener: (Long?) -> Unit) {
        this.listener = listener
    }

    init {
        setHasStableIds(true)
    }

    class ViewHolder(cell: View) : RecyclerView.ViewHolder(cell) {
        val image: ImageView = cell.findViewById(R.id.image_view)
        val name: TextView = cell.findViewById(R.id.name_view)
        val calorie: TextView = cell.findViewById(R.id.calorie_view)
        val button: RadioButton = cell.findViewById(R.id.radioButton)
        val rLayout: RelativeLayout = cell.findViewById(R.id.rvc_relativeLayout)
        val lLayout: LinearLayout = cell.findViewById(R.id.rvc_linearLayout)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodAdapter.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(
            R.layout.recycler_view_content,
            parent, false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodAdapter.ViewHolder, position: Int) {
        val food: Food? = getItem(position)

        val uri = Uri.parse(food?.imagePath)
        try {
            val bmp: Bitmap? = uri?.let { getBitmapFromUri(it) }
            holder.image.setImageBitmap(bmp)
        } catch (e: Exception) {
            e.printStackTrace()
            //画像が指定されていない場合、もしくは画像が削除/移動などでパスを読み込めない場合はデフォルトの画像を表示する
            holder.image.setImageResource(R.drawable.rice)
        }
        holder.name.text = food?.name
        holder.calorie.text = food?.calorie.toString() + "kcal"
        holder.button.isChecked = food!!.id == checkFoodId
        //各radioボタンを押したとき
        holder.button.setOnClickListener {
            checkFoodId = food.id
            editor.putLong("CHECK_FOOD_ID", food.id).apply()
            editor.putString("CHECK_FOOD_IMAGE_PATH", food.imagePath).apply()
            editor.putLong("CHECK_CALORIE", food.calorie).apply()
            editor.putString("CHECK_NAME", food.name).apply()
            editor.putString("CHECK_UNIT", food.unit).apply()
            notifyDataSetChanged()
        }
        //画像部分とテキスト部分をタップしたときにFoodEditActivityに遷移する
        holder.rLayout.setOnClickListener {
            listener?.invoke(food.id)
        }
        holder.lLayout.setOnClickListener {
            listener?.invoke(food.id)
        }
    }

    override fun getItemId(position: Int): Long {
        return getItem(position)?.id ?: 0
    }


}