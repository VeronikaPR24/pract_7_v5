package com.bignerdranch.android.autopark

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class RouteAdapter : ListAdapter<Route, RouteAdapter.RouteViewHolder>(RouteDiffCallback()) {
    var onItemClick: ((Route) -> Unit)? = null
    var onItemLongClick: ((Route) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route, parent, false)
        return RouteViewHolder(view)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val route = getItem(position)
        holder.bind(route)
    }

    inner class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRouteNumber: TextView = itemView.findViewById(R.id.tvRouteNumber)
        private val tvRoutePath: TextView = itemView.findViewById(R.id.tvRoutePath)
        private val tvStartPoint: TextView = itemView.findViewById(R.id.tvStartPoint)
        private val tvEndPoint: TextView = itemView.findViewById(R.id.tvEndPoint)
        private val tvDistance: TextView = itemView.findViewById(R.id.tvDistance)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvDrivers: TextView = itemView.findViewById(R.id.tvDrivers)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val route = getItem(position)
                    onItemClick?.invoke(route)
                }
            }

            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val route = getItem(position)
                    onItemLongClick?.invoke(route)
                    true
                } else {
                    false
                }
            }
        }

        fun bind(route: Route) {
            tvRouteNumber.text = route.routeNumber
            tvRoutePath.text = "${route.startPoint} → ${route.endPoint}"
            tvStartPoint.text = route.startPoint
            tvEndPoint.text = route.endPoint
            tvDistance.text = "${route.distance} км"
            tvTime.text = "${route.estimatedTime} мин"

            val driversCount = (1 + (route.distance / 5).toInt()).coerceAtMost(5)
            tvDrivers.text = "$driversCount водителя"
        }
    }
}

class RouteDiffCallback : DiffUtil.ItemCallback<Route>() {
    override fun areItemsTheSame(oldItem: Route, newItem: Route): Boolean {
        return oldItem.routeId == newItem.routeId
    }

    override fun areContentsTheSame(oldItem: Route, newItem: Route): Boolean {
        return oldItem == newItem
    }
}