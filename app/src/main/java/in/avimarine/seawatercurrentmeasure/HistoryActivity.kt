package `in`.avimarine.seawatercurrentmeasure

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import `in`.avimarine.seawatercurrentmeasure.databinding.ActivityHistoryBinding


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
    }
}
