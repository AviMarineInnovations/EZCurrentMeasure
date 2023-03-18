package `in`.avimarine.seawatercurrentmeasure

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import `in`.avimarine.androidutils.getDirString
import `in`.avimarine.androidutils.getSpeedString
import `in`.avimarine.androidutils.timeStampToDateString
import `in`.avimarine.seawatercurrentmeasure.databinding.HistoryRowItemBinding

class HistoryListAdapter(private val dataSet: List<History>) :
    RecyclerView.Adapter<HistoryListAdapter.ViewHolder>() {


    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = HistoryRowItemBinding.bind(view)

    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.history_row_item, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        with(viewHolder) {
            val c = binding.spdTextView.context
            val (magnetic, fromNotation, speedUnit) = Preferences.getPreferences(c)
            binding.dirTextView.text = getDirString(dataSet[position].dir, magnetic,fromNotation, dataSet[position].loc1, dataSet[position].loc1.time)
            binding.spdTextView.text = getSpeedString(dataSet[position].spd.convertTo(speedUnit).value, speedUnit)
            binding.timeTextView.text = timeStampToDateString(dataSet[position].loc1.time)
            binding.locationTextView.text = getShortLocation(dataSet[position].loc1)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

}