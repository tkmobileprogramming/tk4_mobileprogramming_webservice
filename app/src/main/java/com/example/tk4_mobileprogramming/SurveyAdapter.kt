package com.example.tk4_mobileprogramming

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SurveyAdapter(
    private val surveys: List<Survey>,
    private val onItemClick: (Survey) -> Unit
) : RecyclerView.Adapter<SurveyAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.nameTextView)
        val ageTextView: TextView = view.findViewById(R.id.ageTextView)
        val addressTextView: TextView = view.findViewById(R.id.addressTextView)
        val symptomsTextView: TextView = view.findViewById(R.id.symptomsTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.survey_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val survey = surveys[position]
        holder.nameTextView.text = survey.name
        holder.ageTextView.text = "Usia: ${survey.age}"
        holder.addressTextView.text = "Alamat: ${survey.address}"
        holder.symptomsTextView.text = "Gejala: ${survey.symptoms}"
        holder.itemView.setOnClickListener { onItemClick(survey) }
    }

    override fun getItemCount() = surveys.size
}