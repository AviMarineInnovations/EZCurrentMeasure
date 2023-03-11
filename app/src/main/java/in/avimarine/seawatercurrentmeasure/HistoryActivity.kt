package `in`.avimarine.seawatercurrentmeasure

import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import `in`.avimarine.seawatercurrentmeasure.databinding.ActivityHistoryBinding
import org.json.JSONArray


class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val historyAdapter = HistoryListAdapter(HistoryDataSource.getHistoryList(this))
        val recyclerView: RecyclerView = findViewById(R.id.history_recycler_view)
        recyclerView.adapter = historyAdapter
        val dividerItemDecoration = DividerItemDecoration(
            recyclerView.context,
            resources.configuration.orientation
        )
        recyclerView.addItemDecoration(dividerItemDecoration)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    fun getFormattedHistory():String{
        val (magnetic, fromNotation, speedUnit) = Preferences.getPreferences(this)
        val ja = JSONArray(Preferences.getHistory(this))
        var ret = ""
        for (i in 0 until ja.length()) {
            val entry =  ja.getJSONObject(i)
            val l = Location("")
            l.longitude = entry.getDouble("Lon2")
            l.latitude = entry.getDouble("Lat2")
            val time2 = entry.getLong("time2")
            ret += timeStamptoDateString(time2) + ", " + getDirString(entry.getDouble("direction"),false,fromNotation,l,time2) + "T, " +  getSpeedString(entry.getDouble("speed"),speedUnit) + "\n"
        }
        return ret
    }

}
