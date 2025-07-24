package com.example.mmdemo.list.model

import android.graphics.Color
import androidx.annotation.StringRes
import androidx.room.Entity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.PrimaryKey
import com.example.mmdemo.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

interface JuiceRepository {
    val juicesStream: Flow<List<Juice>>

    fun getJuiceStream(id: Long): Flow<Juice?>
    suspend fun addJuice(juice: Juice)
    suspend fun deleteJuice(juice: Juice)
    suspend fun updateJuice(juice: Juice)
}

@Entity
data class Juice(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
    val description: String = "",
    val color: String,
    val rating: Int
)

enum class JuiceColor(val color: Int, @StringRes val label: Int) {
    Red(Color.RED, R.string.red),
    Blue(Color.BLUE, R.string.blue),
    Green(Color.GREEN, R.string.green),
    Cyan(Color.CYAN, R.string.cyan),
    Yellow(Color.YELLOW, R.string.yellow),
    Magenta(Color.MAGENTA, R.string.magenta)
}

/**
 * ViewModel to retrieve, add, edit or delete a juice entry
 * from the [JuiceRepository]'s data source.
 */
class EntryViewModel(private val juiceRepository: JuiceRepository) : ViewModel() {

    fun getJuiceStream(id: Long): Flow<Juice?> = juiceRepository.getJuiceStream(id)

    fun saveJuice(
        id: Long,
        name: String,
        description: String,
        color: String,
        rating: Int
    ) {
        val juice = Juice(id, name, description, color, rating)
        viewModelScope.launch {
            if (id > 0) {
                juiceRepository.updateJuice(juice)
            } else {
                juiceRepository.addJuice(juice)
            }
        }
    }
}
