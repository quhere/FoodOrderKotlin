package com.pro.foodorder.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pro.foodorder.adapter.CartAdapter.CartViewHolder
import com.pro.foodorder.constant.Constant
import com.pro.foodorder.databinding.ItemCartBinding
import com.pro.foodorder.model.Food
import com.pro.foodorder.utils.GlideUtils.loadUrl

class CartAdapter(private val mListFoods: MutableList<Food>?,
                  private val iClickListener: IClickListener) : RecyclerView.Adapter<CartViewHolder>() {

    interface IClickListener {
        fun clickDeteteFood(food: Food?, position: Int)
        fun updateItemFood(food: Food?, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val itemCartBinding = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(itemCartBinding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val food = mListFoods!![position]
        loadUrl(food.image, holder.mItemCartBinding.imgFoodCart)
        holder.mItemCartBinding.tvFoodNameCart.text = food.name
        var strFoodPriceCart: String? = "" + food.price + Constant.CURRENCY
        if (food.sale > 0) {
            strFoodPriceCart = "" + food.realPrice + Constant.CURRENCY
        }
        holder.mItemCartBinding.tvFoodPriceCart.text = strFoodPriceCart
        holder.mItemCartBinding.tvCount.text = java.lang.String.valueOf(food.count)
        holder.mItemCartBinding.tvSubtract.setOnClickListener {
            val strCount = holder.mItemCartBinding.tvCount.text.toString()
            val count = strCount.toInt()
            if (count <= 1) {
                return@setOnClickListener
            }
            val newCount = count - 1
            holder.mItemCartBinding.tvCount.text = newCount.toString()
            val totalPrice = food.realPrice * newCount
            food.count = newCount
            food.totalPrice = totalPrice
            iClickListener.updateItemFood(food, holder.adapterPosition)
        }
        holder.mItemCartBinding.tvAdd.setOnClickListener {
            val newCount = holder.mItemCartBinding.tvCount.text.toString().toInt() + 1
            holder.mItemCartBinding.tvCount.text = newCount.toString()
            val totalPrice = food.realPrice * newCount
            food.count = newCount
            food.totalPrice = totalPrice
            iClickListener.updateItemFood(food, holder.adapterPosition)
        }
        holder.mItemCartBinding.tvDelete.setOnClickListener { iClickListener.clickDeteteFood(food, holder.adapterPosition) }
    }

    override fun getItemCount(): Int {
        return mListFoods?.size ?: 0
    }

    class CartViewHolder(val mItemCartBinding: ItemCartBinding) : RecyclerView.ViewHolder(mItemCartBinding.root)
}